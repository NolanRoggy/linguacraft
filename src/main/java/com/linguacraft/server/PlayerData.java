package com.linguacraft.server;

import com.linguacraft.Config;
import com.linguacraft.core.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Per-player settings and progress, stored in the player's persistent data. */
public final class PlayerData {
    public static final String KEY = "linguacraft";
    private static final String LANG = "lang";
    private static final String PRACTICE = "practice";

    private PlayerData() {}

    public static CompoundTag root(ServerPlayer p) {
        CompoundTag persistent = p.getPersistentData();
        if (!persistent.contains(KEY, Tag.TAG_COMPOUND)) persistent.put(KEY, new CompoundTag());
        return persistent.getCompound(KEY);
    }

    public static Language language(ServerPlayer p) {
        CompoundTag t = root(p);
        Language l = t.contains(LANG) ? Language.parse(t.getString(LANG)) : null;
        return l != null ? l : Config.DEFAULT_LANGUAGE.get();
    }

    public static void setLanguage(ServerPlayer p, Language l) {
        root(p).putString(LANG, l.name());
    }

    public static boolean practice(ServerPlayer p) {
        CompoundTag t = root(p);
        return t.contains(PRACTICE) ? t.getBoolean(PRACTICE) : Config.PRACTICE_MODE.get();
    }

    public static void setPractice(ServerPlayer p, boolean v) {
        root(p).putBoolean(PRACTICE, v);
    }

    private static String listKey(Language l) {
        return "known_" + l.code;
    }

    public static List<String> known(ServerPlayer p, Language l) {
        ListTag list = root(p).getList(listKey(l), Tag.TAG_STRING);
        List<String> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    public static boolean isKnown(ServerPlayer p, Language l, String itemPath) {
        return known(p, l).contains(itemPath);
    }

    public static void markKnown(ServerPlayer p, Language l, String itemPath) {
        if (isKnown(p, l, itemPath)) return;
        CompoundTag t = root(p);
        ListTag list = t.getList(listKey(l), Tag.TAG_STRING);
        list.add(StringTag.valueOf(itemPath));
        t.put(listKey(l), list);
    }

    /** Copy progress to the new player entity after death / returning from the End. */
    public static void copy(ServerPlayer from, ServerPlayer to) {
        CompoundTag src = from.getPersistentData();
        if (src.contains(KEY, Tag.TAG_COMPOUND)) {
            to.getPersistentData().put(KEY, src.getCompound(KEY).copy());
        }
    }
}
