package com.linguacraft.core;

import java.text.Normalizer;
import java.util.Locale;

/** Scores how close a spoken (recognized) string is to the target word. */
public final class Similarity {
    private Similarity() {}

    /** 0..100 */
    public static int percent(String spoken, Word target, Language lang) {
        String s = normalize(spoken, lang);
        if (s.isEmpty()) return 0;
        int best = 0;
        for (String form : target.accepted()) {
            String t = normalize(form, lang);
            if (t.isEmpty()) continue;
            int dist = levenshtein(s, t);
            int max = Math.max(s.length(), t.length());
            int pct = (int) Math.round(100.0 * (max - dist) / max);
            best = Math.max(best, pct);
        }
        return Math.max(0, Math.min(100, best));
    }

    /** Lowercase, drop accents (Latin) or fold katakana to hiragana (Japanese), keep letters/digits only. */
    public static String normalize(String in, Language lang) {
        if (in == null) return "";
        String s = in.toLowerCase(Locale.ROOT);
        if (lang.latin) {
            s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        } else {
            s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!lang.latin && c >= 0x30A1 && c <= 0x30F6) c = (char) (c - 0x60); // katakana -> hiragana
            if (Character.isLetterOrDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[b.length()];
    }
}
