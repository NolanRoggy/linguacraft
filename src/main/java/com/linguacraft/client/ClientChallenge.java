package com.linguacraft.client;

import com.linguacraft.core.Language;
import com.linguacraft.core.Similarity;
import com.linguacraft.core.Word;
import com.linguacraft.net.ReportPayload;
import com.linguacraft.net.ResultPayload;
import com.linguacraft.net.StartChallengePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/** Client-side state of the current challenge. Read by {@link ClientHud}. */
public final class ClientChallenge {
    public enum State { IDLE, PREPARING, LISTENING, PASSED, FAILED, ERROR }

    // ---- read by the HUD ----
    public static volatile State state = State.IDLE;
    public static volatile StartChallengePayload data;
    public static volatile int percent;
    public static volatile String heard = "";
    public static volatile String status = "";
    public static volatile String banner = "";
    public static volatile long listenStartMs;
    public static volatile long bannerUntilMs;

    private static final Map<Language, Model> MODELS = new EnumMap<>(Language.class);
    private static final SpeechListener LISTENER = new SpeechListener();
    private static volatile int best;
    private static volatile boolean reported;
    private static volatile int generation; // invalidates callbacks from a previous challenge

    private ClientChallenge() {}

    public static void start(StartChallengePayload p) {
        stopListening();
        final int gen = ++generation;
        data = p;
        percent = 0;
        best = 0;
        heard = "";
        banner = "";
        reported = false;
        status = "Preparing...";
        state = State.PREPARING;

        Thread t = new Thread(() -> prepareAndListen(p, gen), "LinguaCraft-Prepare");
        t.setDaemon(true);
        t.start();
    }

    private static void prepareAndListen(StartChallengePayload p, int gen) {
        try {
            Model model;
            synchronized (MODELS) {
                model = MODELS.get(p.language());
                if (model == null) {
                    LibVosk.setLogLevel(LogLevel.WARNINGS);
                    Path dir = ModelManager.ensure(p.language(), s -> {
                        if (gen == generation) status = s;
                    });
                    if (gen == generation) status = "Loading speech model...";
                    model = new Model(dir.toString());
                    MODELS.put(p.language(), model);
                }
            }
            if (gen != generation) return;

            final Word target = p.word();
            listenStartMs = System.currentTimeMillis();
            state = State.LISTENING;
            status = "Listening...";
            LISTENER.start(model, text -> {
                if (gen != generation) return;
                heard = text;
                int pct = Similarity.percent(text, target, p.language());
                percent = pct;
                if (pct > best) best = pct;
                if (best >= p.thresholdPercent()) report(gen, best);
            }, err -> {
                if (gen != generation) return;
                fail(gen, err);
            });
        } catch (Throwable t) {
            fail(gen, "Speech setup failed: " + t.getMessage());
        }
    }

    private static void fail(int gen, String message) {
        if (gen != generation) return;
        state = State.ERROR;
        status = message;
        reported = true;
        send(new ReportPayload(data.id(), -1)); // tell the server: skip, no penalty
        bannerUntilMs = System.currentTimeMillis() + 6000;
        banner = message;
    }

    private static synchronized void report(int gen, int value) {
        if (gen != generation || reported) return;
        reported = true;
        send(new ReportPayload(data.id(), value));
    }

    private static void send(ReportPayload payload) {
        Minecraft.getInstance().execute(() -> PacketDistributor.sendToServer(payload));
    }

    /** Called every client tick: handles the countdown. */
    public static void tick() {
        if (state == State.LISTENING && !reported) {
            StartChallengePayload d = data;
            long elapsed = System.currentTimeMillis() - listenStartMs;
            if (d != null && elapsed > d.limitTicks() * 50L) report(generation, best);
        }
        if ((state == State.PASSED || state == State.FAILED || state == State.ERROR)
                && System.currentTimeMillis() > bannerUntilMs) {
            state = State.IDLE;
            data = null;
        }
    }

    public static void onResult(ResultPayload r) {
        StartChallengePayload d = data;
        if (d == null || d.id() != r.id()) return;
        stopListening();
        generation++;
        banner = r.message();
        bannerUntilMs = System.currentTimeMillis() + 4000;
        state = r.passed() ? State.PASSED : State.FAILED;
    }

    private static void stopListening() {
        LISTENER.stop();
    }

    public static boolean active() {
        return state != State.IDLE && data != null;
    }
}
