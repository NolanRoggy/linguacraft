package com.linguacraft.server;

import com.linguacraft.Config;
import com.linguacraft.core.Language;
import com.linguacraft.core.Phrases;
import com.linguacraft.core.Vocab;
import com.linguacraft.core.Word;
import com.linguacraft.net.ResultPayload;
import com.linguacraft.net.StartChallengePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Server-side brain: decides when to ask something, tracks the one active challenge per player,
 * and applies the result (unlock or delete).
 */
public final class ChallengeManager {

    /** itemPath is non-null only for item challenges (the item that gets deleted on failure). */
    private record Challenge(int id, Language lang, Word word, Item item, String itemPath, long deadlineTick) {
        boolean isItem() {
            return item != null;
        }
    }

    private record PendingItem(Item item, String itemPath, Word word, Language lang) {}

    private static final Random RNG = new Random();
    private static final Map<UUID, Challenge> ACTIVE = new HashMap<>();
    private static final Map<UUID, Deque<PendingItem>> QUEUE = new HashMap<>();
    private static final Map<UUID, Long> NEXT_RANDOM_TICK = new HashMap<>();
    private static final Map<UUID, Long> LAST_PHRASE_END_TICK = new HashMap<>();
    private static int nextId = 1;

    /** Grace on top of the time limit so a slow model download / network lag doesn't instantly fail you. */
    private static final int GRACE_TICKS = 20 * 8;

    private ChallengeManager() {}

    // ------------------------------------------------------------------ item challenges

    /** Called when the player crafts / smelts / first picks up an item. */
    public static void onItemObtained(ServerPlayer p, ItemStack stack) {
        if (stack.isEmpty() || p.isSpectator()) return;
        Language lang = PlayerData.language(p);
        Item item = stack.getItem();
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        Word word = Vocab.get(lang, path);
        if (word == null) return;                       // no vocabulary for this item
        if (PlayerData.isKnown(p, lang, path)) return;  // already unlocked

        UUID id = p.getUUID();
        Challenge cur = ACTIVE.get(id);
        if (cur != null && path.equals(cur.itemPath())) return;
        Deque<PendingItem> q = QUEUE.computeIfAbsent(id, k -> new ArrayDeque<>());
        for (PendingItem pi : q) if (pi.itemPath().equals(path)) return;

        PendingItem pending = new PendingItem(item, path, word, lang);
        if (cur == null) startItem(p, pending);
        else q.add(pending);
    }

    private static void startItem(ServerPlayer p, PendingItem pi) {
        int limit = Config.ITEM_TIME_LIMIT_SECONDS.get() * 20;
        begin(p, new Challenge(nextId++, pi.lang(), pi.word(), pi.item(), pi.itemPath(),
                        p.serverLevel().getGameTime() + limit + GRACE_TICKS),
                true, limit, "Say it in " + pi.lang().displayName + " or it is deleted!");
    }

    // ------------------------------------------------------------------ phrase challenges

    /** Ask a phrase for a category if nothing else is going on. Returns true if one was started. */
    public static boolean askPhrase(ServerPlayer p, Phrases.Category cat, boolean ignoreCooldown) {
        if (!Config.ASK_PHRASES.get() || p.isSpectator()) return false;
        UUID id = p.getUUID();
        if (ACTIVE.containsKey(id)) return false;
        long now = p.serverLevel().getGameTime();
        Long last = LAST_PHRASE_END_TICK.get(id);
        if (!ignoreCooldown && last != null && now - last < Config.MIN_GAP_SECONDS.get() * 20L) return false;

        Language lang = PlayerData.language(p);
        Word word = Phrases.pick(lang, cat, RNG);
        startWord(p, lang, word, "Say this phrase in " + lang.displayName);
        return true;
    }

    /** Review of an item word the player already unlocked, otherwise a normal random phrase. */
    public static boolean askRandom(ServerPlayer p, boolean ignoreCooldown) {
        if (!Config.ASK_PHRASES.get() || ACTIVE.containsKey(p.getUUID())) return false;
        Language lang = PlayerData.language(p);
        List<String> known = new ArrayList<>(PlayerData.known(p, lang));
        if (!known.isEmpty() && RNG.nextDouble() < Config.REVIEW_CHANCE.get()) {
            Word w = Vocab.randomOf(lang, known, RNG);
            if (w != null) {
                long now = p.serverLevel().getGameTime();
                Long last = LAST_PHRASE_END_TICK.get(p.getUUID());
                if (!ignoreCooldown && last != null && now - last < Config.MIN_GAP_SECONDS.get() * 20L) return false;
                startWord(p, lang, w, "Review: say it in " + lang.displayName);
                return true;
            }
        }
        return askPhrase(p, Phrases.Category.RANDOM, ignoreCooldown);
    }

    private static void startWord(ServerPlayer p, Language lang, Word word, String label) {
        int limit = Config.PHRASE_TIME_LIMIT_SECONDS.get() * 20;
        begin(p, new Challenge(nextId++, lang, word, null, null, p.serverLevel().getGameTime() + limit + GRACE_TICKS),
                false, limit, label);
    }

    // ------------------------------------------------------------------ lifecycle

    private static void begin(ServerPlayer p, Challenge c, boolean item, int limitTicks, String label) {
        ACTIVE.put(p.getUUID(), c);
        PacketDistributor.sendToPlayer(p, new StartChallengePayload(
                c.id(), item, c.lang(), c.word().text(), c.word().reading(), c.word().english(),
                c.word().kana(), label, limitTicks, Config.THRESHOLD_PERCENT.get()));
    }

    /** The client reports the best accuracy it heard. */
    public static void report(ServerPlayer p, int id, int percent) {
        Challenge c = ACTIVE.get(p.getUUID());
        if (c == null || c.id() != id) return;
        ACTIVE.remove(p.getUUID());

        if (percent < 0) { // client could not listen at all
            send(p, id, false, "Could not listen (no microphone or speech model). Challenge skipped, nothing lost.");
            finished(p, false);
            return;
        }
        if (percent >= Config.THRESHOLD_PERCENT.get()) pass(p, c, percent);
        else fail(p, c, percent);
    }

    private static void pass(ServerPlayer p, Challenge c, int percent) {
        if (c.isItem()) {
            PlayerData.markKnown(p, c.lang(), c.itemPath());
            String msg = "Unlocked: " + c.word().text() + " (" + percent + "%)";
            send(p, c.id(), true, msg);
            p.sendSystemMessage(Component.literal("✔ " + msg + "  = " + c.word().english()).withStyle(ChatFormatting.GREEN));
        } else {
            String msg = "Correct! (" + percent + "%)";
            send(p, c.id(), true, msg);
            p.sendSystemMessage(Component.literal("✔ " + c.word().text() + " = " + c.word().english()
                    + " (" + percent + "%)").withStyle(ChatFormatting.GREEN));
        }
        finished(p, !c.isItem());
    }

    private static void fail(ServerPlayer p, Challenge c, int percent) {
        boolean practice = PlayerData.practice(p);
        if (c.isItem()) {
            boolean delete = Config.DELETE_ITEMS.get() && !practice;
            if (delete) deleteItem(p, c.item());
            String msg = delete ? "Deleted: " + c.word().english() + " (" + percent + "%)"
                                : "Not quite (" + percent + "%) - practice mode, nothing deleted";
            send(p, c.id(), false, msg);
            p.sendSystemMessage(Component.literal("✘ " + msg + "  Correct: " + c.word().text()
                    + (c.word().reading().equals(c.word().text()) ? "" : " (" + c.word().reading() + ")"))
                    .withStyle(ChatFormatting.RED));
        } else {
            if (!practice) applyPhrasePenalty(p);
            String msg = "Not quite (" + percent + "%)";
            send(p, c.id(), false, msg);
            p.sendSystemMessage(Component.literal("✘ " + msg + "  Correct: " + c.word().text()
                    + (c.word().reading().equals(c.word().text()) ? "" : " (" + c.word().reading() + ")")
                    + " = " + c.word().english()).withStyle(ChatFormatting.RED));
        }
        finished(p, !c.isItem());
    }

    private static void send(ServerPlayer p, int id, boolean passed, String message) {
        PacketDistributor.sendToPlayer(p, new ResultPayload(id, passed, message));
    }

    private static void finished(ServerPlayer p, boolean wasPhrase) {
        UUID id = p.getUUID();
        if (wasPhrase) LAST_PHRASE_END_TICK.put(id, p.serverLevel().getGameTime());
        Deque<PendingItem> q = QUEUE.get(id);
        if (q != null && !q.isEmpty() && !ACTIVE.containsKey(id)) {
            startItem(p, q.poll());
        }
    }

    // ------------------------------------------------------------------ per-tick work

    public static void tick(ServerPlayer p) {
        UUID id = p.getUUID();
        long now = p.serverLevel().getGameTime();

        Challenge c = ACTIVE.get(id);
        if (c != null && now > c.deadlineTick()) { // client never reported in time
            ACTIVE.remove(id);
            fail(p, c, 0);
            return;
        }

        if (c == null && Config.ASK_PHRASES.get()) {
            Long next = NEXT_RANDOM_TICK.get(id);
            if (next == null) {
                NEXT_RANDOM_TICK.put(id, now + nextRandomDelay());
            } else if (now >= next) {
                NEXT_RANDOM_TICK.put(id, now + nextRandomDelay());
                askRandom(p, false);
            }
        }
    }

    private static long nextRandomDelay() {
        long avg = Config.RANDOM_PHRASE_MINUTES.get() * 60L * 20L;
        return (long) (avg * (0.5 + RNG.nextDouble())); // +-50%
    }

    public static boolean isBusy(ServerPlayer p) {
        return ACTIVE.containsKey(p.getUUID());
    }

    /** Drop everything for a player (logout, /lingua skip). No penalty. */
    public static void cancel(ServerPlayer p, boolean notifyClient) {
        Challenge c = ACTIVE.remove(p.getUUID());
        QUEUE.remove(p.getUUID());
        if (c != null && notifyClient) send(p, c.id(), true, "Skipped");
    }

    public static void forget(UUID id) {
        ACTIVE.remove(id);
        QUEUE.remove(id);
        NEXT_RANDOM_TICK.remove(id);
        LAST_PHRASE_END_TICK.remove(id);
    }

    // ------------------------------------------------------------------ penalties

    /** Removes every stack of this item from the inventory, the crafting grid and the cursor. */
    static void deleteItem(ServerPlayer p, Item item) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) inv.setItem(i, ItemStack.EMPTY);
        }
        if (p.containerMenu.getCarried().is(item)) p.containerMenu.setCarried(ItemStack.EMPTY);
        for (Slot s : p.containerMenu.slots) {
            if ((s.container instanceof Inventory || s.container instanceof CraftingContainer) && s.getItem().is(item)) {
                s.set(ItemStack.EMPTY);
            }
        }
        p.inventoryMenu.broadcastChanges();
        p.containerMenu.broadcastChanges();
    }

    private static void applyPhrasePenalty(ServerPlayer p) {
        switch (Config.PHRASE_PENALTY.get()) {
            case NONE -> {}
            case SLOWNESS -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 15, 1));
            case DELETE_RANDOM_ITEM -> {
                Inventory inv = p.getInventory();
                List<Integer> filled = new ArrayList<>();
                for (int i = 0; i < 36; i++) if (!inv.getItem(i).isEmpty()) filled.add(i);
                if (!filled.isEmpty()) {
                    int slot = filled.get(RNG.nextInt(filled.size()));
                    p.sendSystemMessage(Component.literal("Lost: " + inv.getItem(slot).getHoverName().getString())
                            .withStyle(ChatFormatting.DARK_RED));
                    inv.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
    }
}
