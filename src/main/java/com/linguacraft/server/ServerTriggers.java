package com.linguacraft.server;

import com.linguacraft.Config;
import com.linguacraft.LinguaCraft;
import com.linguacraft.core.Phrases;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.concurrent.ThreadLocalRandom;

/** Game events that start challenges. */
@EventBusSubscriber(modid = LinguaCraft.MODID)
public final class ServerTriggers {
    private ServerTriggers() {}

    private static boolean roll() {
        return ThreadLocalRandom.current().nextInt(100) < Config.ACTION_TRIGGER_CHANCE_PERCENT.get();
    }

    // ---- item words (the "delete it if you can't say it" mechanic) ----

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ChallengeManager.onItemObtained(p, e.getCrafting());
    }

    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ChallengeManager.onItemObtained(p, e.getSmelting());
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post e) {
        if (e.getPlayer() instanceof ServerPlayer p) ChallengeManager.onItemObtained(p, e.getOriginalStack());
    }

    // ---- phrases tied to actions ----

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getItem().has(DataComponents.FOOD) && roll()) {
            ChallengeManager.askPhrase(p, Phrases.Category.EAT, false);
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (e.getEntity() instanceof Enemy && e.getSource().getEntity() instanceof ServerPlayer p && roll()) {
            ChallengeManager.askPhrase(p, Phrases.Category.KILL, false);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getNewDamage() > 0 && roll()) {
            ChallengeManager.askPhrase(p, Phrases.Category.HURT, false);
        }
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ChallengeManager.askPhrase(p, Phrases.Category.SLEEP, false);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ChallengeManager.askPhrase(p, Phrases.Category.DIMENSION, false);
    }

    // ---- housekeeping ----

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) ChallengeManager.tick(p);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (e.getEntity() instanceof ServerPlayer neu && e.getOriginal() instanceof ServerPlayer old) {
            PlayerData.copy(old, neu);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        ChallengeManager.forget(e.getEntity().getUUID());
    }
}
