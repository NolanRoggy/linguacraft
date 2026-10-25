package com.linguacraft.core;

import java.util.Locale;

/** Supported languages. Pure Java, no Minecraft imports, so it is safe on both sides. */
public enum Language {
    SPANISH("es", "Spanish", true, "vosk-model-small-es-0.42"),
    JAPANESE("ja", "Japanese", false, "vosk-model-small-ja-0.22");

    public final String code;
    public final String displayName;
    /** True for Latin-script languages (accents get stripped when scoring). */
    public final boolean latin;
    public final String modelName;

    Language(String code, String displayName, boolean latin, String modelName) {
        this.code = code;
        this.displayName = displayName;
        this.latin = latin;
        this.modelName = modelName;
    }

    public String modelUrl() {
        return "https://alphacephei.com/vosk/models/" + modelName + ".zip";
    }

    /** Accepts "spanish", "es", "japanese", "ja" (any case). Returns null if unknown. */
    public static Language parse(String s) {
        if (s == null) return null;
        String v = s.trim().toLowerCase(Locale.ROOT);
        for (Language l : values()) {
            if (l.code.equals(v) || l.name().toLowerCase(Locale.ROOT).equals(v)) return l;
        }
        return null;
    }
}
