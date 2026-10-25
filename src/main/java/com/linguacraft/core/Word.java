package com.linguacraft.core;

import java.util.ArrayList;
import java.util.List;

/**
 * One thing to say.
 *
 * @param text    what is displayed and what the speech recognizer should produce (Spanish text, or Japanese script)
 * @param reading how to pronounce it for display (same as text for Spanish, romaji for Japanese)
 * @param english the English meaning
 * @param kana    Japanese only: pure kana spelling, also accepted (empty for Spanish)
 */
public record Word(String text, String reading, String english, String kana) {

    public static Word es(String text, String english) {
        return new Word(text, text, english, "");
    }

    public static Word ja(String text, String kana, String romaji, String english) {
        return new Word(text, romaji, english, kana);
    }

    /** Every written form a correct answer may match. */
    public List<String> accepted() {
        List<String> out = new ArrayList<>(2);
        out.add(text);
        if (kana != null && !kana.isEmpty()) out.add(kana);
        return out;
    }
}
