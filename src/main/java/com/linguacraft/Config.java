package com.linguacraft;

import com.linguacraft.core.Language;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public enum PhrasePenalty { NONE, SLOWNESS, DELETE_RANDOM_ITEM }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<Language> DEFAULT_LANGUAGE;
    public static final ModConfigSpec.IntValue THRESHOLD_PERCENT;
    public static final ModConfigSpec.BooleanValue DELETE_ITEMS;
    public static final ModConfigSpec.BooleanValue PRACTICE_MODE;
    public static final ModConfigSpec.IntValue ITEM_TIME_LIMIT_SECONDS;
    public static final ModConfigSpec.IntValue PHRASE_TIME_LIMIT_SECONDS;
    public static final ModConfigSpec.BooleanValue ASK_PHRASES;
    public static final ModConfigSpec.IntValue RANDOM_PHRASE_MINUTES;
    public static final ModConfigSpec.IntValue MIN_GAP_SECONDS;
    public static final ModConfigSpec.IntValue ACTION_TRIGGER_CHANCE_PERCENT;
    public static final ModConfigSpec.DoubleValue REVIEW_CHANCE;
    public static final ModConfigSpec.EnumValue<PhrasePenalty> PHRASE_PENALTY;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("language");
        DEFAULT_LANGUAGE = b.comment("Language new players start with. Players can change theirs with /lingua language <spanish|japanese>.")
                .defineEnum("defaultLanguage", Language.SPANISH);
        b.pop();

        b.push("items");
        DELETE_ITEMS = b.comment("If false, failing an item word only shows a message (nothing is deleted).")
                .define("deleteItemsOnFail", true);
        PRACTICE_MODE = b.comment("Default practice mode: nothing is ever deleted or penalized. Players can toggle with /lingua practice.")
                .define("practiceModeDefault", false);
        THRESHOLD_PERCENT = b.comment("Accuracy needed to unlock an item / pass a phrase (the original stream used about 93).")
                .defineInRange("thresholdPercent", 90, 50, 100);
        ITEM_TIME_LIMIT_SECONDS = b.comment("Seconds you get to say an item's name.")
                .defineInRange("itemTimeLimitSeconds", 12, 3, 120);
        b.pop();

        b.push("phrases");
        ASK_PHRASES = b.comment("Ask random phrases and phrases tied to actions (eating, killing, sleeping, getting hurt, changing dimension).")
                .define("askPhrases", true);
        PHRASE_TIME_LIMIT_SECONDS = b.comment("Seconds you get to say a phrase.")
                .defineInRange("phraseTimeLimitSeconds", 15, 3, 120);
        RANDOM_PHRASE_MINUTES = b.comment("Average minutes between random phrase / review challenges (actual gap varies by +-50%).")
                .defineInRange("randomPhraseMinutes", 5, 1, 120);
        ACTION_TRIGGER_CHANCE_PERCENT = b.comment("Chance (percent) that an action like eating or killing a mob triggers a phrase.")
                .defineInRange("actionTriggerChancePercent", 35, 0, 100);
        MIN_GAP_SECONDS = b.comment("Minimum seconds between phrase challenges, so you are not spammed.")
                .defineInRange("minGapSeconds", 20, 0, 600);
        REVIEW_CHANCE = b.comment("Chance that a random challenge is a review of an item word you already unlocked (spaced repetition).")
                .defineInRange("reviewChance", 0.3, 0.0, 1.0);
        PHRASE_PENALTY = b.comment("What happens when you fail a phrase (items always follow deleteItemsOnFail).")
                .defineEnum("phrasePenalty", PhrasePenalty.SLOWNESS);
        b.pop();

        SPEC = b.build();
    }

    private Config() {}
}
