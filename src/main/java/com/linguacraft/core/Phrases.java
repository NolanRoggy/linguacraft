package com.linguacraft.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Short phrases asked at random moments or when you do something in the game. */
public final class Phrases {
    public enum Category {
        RANDOM,    // any time
        EAT,       // after eating
        KILL,      // after killing a hostile mob
        SLEEP,     // after waking up
        HURT,      // after taking damage
        DIMENSION  // after changing dimension
    }

    private static final Map<Language, Map<Category, List<Word>>> PHRASES = new EnumMap<>(Language.class);

    private static void es(Category c, String text, String english) {
        PHRASES.get(Language.SPANISH).get(c).add(Word.es(text, english));
    }

    private static void ja(Category c, String text, String kana, String romaji, String english) {
        PHRASES.get(Language.JAPANESE).get(c).add(Word.ja(text, kana, romaji, english));
    }

    static {
        for (Language l : Language.values()) {
            Map<Category, List<Word>> m = new EnumMap<>(Category.class);
            for (Category c : Category.values()) m.put(c, new ArrayList<>());
            PHRASES.put(l, m);
        }

        // ---------- Spanish ----------
        es(Category.RANDOM, "Hola, ¿cómo estás?", "Hello, how are you?");
        es(Category.RANDOM, "Buenos días", "Good morning");
        es(Category.RANDOM, "Me llamo Steve", "My name is Steve");
        es(Category.RANDOM, "¿Dónde está la mina?", "Where is the mine?");
        es(Category.RANDOM, "Necesito ayuda", "I need help");
        es(Category.RANDOM, "Muchas gracias", "Thank you very much");
        es(Category.RANDOM, "No entiendo", "I don't understand");
        es(Category.RANDOM, "¿Cuánto cuesta?", "How much does it cost?");
        es(Category.RANDOM, "Tengo hambre", "I am hungry");
        es(Category.RANDOM, "Me gusta construir", "I like to build");

        es(Category.EAT, "Tengo hambre", "I am hungry");
        es(Category.EAT, "Está muy rico", "It is very tasty");
        es(Category.EAT, "Me gusta el pan", "I like bread");
        es(Category.EAT, "Quiero más comida", "I want more food");

        es(Category.KILL, "Cuidado con el monstruo", "Watch out for the monster");
        es(Category.KILL, "Hay un zombi", "There is a zombie");
        es(Category.KILL, "Tengo miedo", "I am afraid");
        es(Category.KILL, "Soy muy fuerte", "I am very strong");

        es(Category.SLEEP, "Buenos días", "Good morning");
        es(Category.SLEEP, "Dormí muy bien", "I slept very well");
        es(Category.SLEEP, "Tengo sueño", "I am sleepy");
        es(Category.SLEEP, "Es de día", "It is daytime");

        es(Category.HURT, "Me duele", "It hurts");
        es(Category.HURT, "Necesito ayuda", "I need help");
        es(Category.HURT, "Tengo poca vida", "I have low health");
        es(Category.HURT, "Ay, qué dolor", "Ouch, what pain");

        es(Category.DIMENSION, "¿Dónde estoy?", "Where am I?");
        es(Category.DIMENSION, "Hace mucho calor", "It is very hot");
        es(Category.DIMENSION, "Este lugar es extraño", "This place is strange");
        es(Category.DIMENSION, "Quiero volver a casa", "I want to go home");

        // ---------- Japanese ----------
        ja(Category.RANDOM, "こんにちは", "こんにちは", "konnichiwa", "Hello");
        ja(Category.RANDOM, "おはようございます", "おはようございます", "ohayou gozaimasu", "Good morning");
        ja(Category.RANDOM, "私の名前はスティーブです", "わたしのなまえはすてぃーぶです", "watashi no namae wa sutiibu desu", "My name is Steve");
        ja(Category.RANDOM, "ありがとうございます", "ありがとうございます", "arigatou gozaimasu", "Thank you very much");
        ja(Category.RANDOM, "わかりません", "わかりません", "wakarimasen", "I don't understand");
        ja(Category.RANDOM, "助けてください", "たすけてください", "tasukete kudasai", "Please help me");
        ja(Category.RANDOM, "いくらですか", "いくらですか", "ikura desu ka", "How much is it?");
        ja(Category.RANDOM, "お腹が空きました", "おなかがすきました", "onaka ga sukimashita", "I am hungry");
        ja(Category.RANDOM, "建てるのが好きです", "たてるのがすきです", "tateru no ga suki desu", "I like to build");
        ja(Category.RANDOM, "鉱山はどこですか", "こうざんはどこですか", "kouzan wa doko desu ka", "Where is the mine?");

        ja(Category.EAT, "いただきます", "いただきます", "itadakimasu", "Let's eat (said before a meal)");
        ja(Category.EAT, "おいしい", "おいしい", "oishii", "Delicious");
        ja(Category.EAT, "お腹が空きました", "おなかがすきました", "onaka ga sukimashita", "I am hungry");
        ja(Category.EAT, "ごちそうさまでした", "ごちそうさまでした", "gochisousama deshita", "Thank you for the meal");

        ja(Category.KILL, "気をつけて", "きをつけて", "ki o tsukete", "Be careful");
        ja(Category.KILL, "ゾンビがいます", "ぞんびがいます", "zonbi ga imasu", "There is a zombie");
        ja(Category.KILL, "怖いです", "こわいです", "kowai desu", "I'm scared");
        ja(Category.KILL, "私は強いです", "わたしはつよいです", "watashi wa tsuyoi desu", "I am strong");

        ja(Category.SLEEP, "おはよう", "おはよう", "ohayou", "Good morning");
        ja(Category.SLEEP, "よく寝ました", "よくねました", "yoku nemashita", "I slept well");
        ja(Category.SLEEP, "眠いです", "ねむいです", "nemui desu", "I am sleepy");
        ja(Category.SLEEP, "朝です", "あさです", "asa desu", "It is morning");

        ja(Category.HURT, "痛い", "いたい", "itai", "Ouch / It hurts");
        ja(Category.HURT, "助けて", "たすけて", "tasukete", "Help");
        ja(Category.HURT, "大丈夫ですか", "だいじょうぶですか", "daijoubu desu ka", "Are you okay?");
        ja(Category.HURT, "体力が少ないです", "たいりょくがすくないです", "tairyoku ga sukunai desu", "My health is low");

        ja(Category.DIMENSION, "ここはどこですか", "ここはどこですか", "koko wa doko desu ka", "Where is this place?");
        ja(Category.DIMENSION, "とても暑いです", "とてもあついです", "totemo atsui desu", "It is very hot");
        ja(Category.DIMENSION, "変な場所です", "へんなばしょです", "hen na basho desu", "It is a strange place");
        ja(Category.DIMENSION, "家に帰りたいです", "いえにかえりたいです", "ie ni kaeritai desu", "I want to go home");
    }

    private Phrases() {}

    public static Word pick(Language lang, Category cat, Random rng) {
        List<Word> list = PHRASES.get(lang).get(cat);
        if (list.isEmpty()) list = PHRASES.get(lang).get(Category.RANDOM);
        return list.get(rng.nextInt(list.size()));
    }

    public static int count(Language lang) {
        int n = 0;
        for (List<Word> l : PHRASES.get(lang).values()) n += l.size();
        return n;
    }
}
