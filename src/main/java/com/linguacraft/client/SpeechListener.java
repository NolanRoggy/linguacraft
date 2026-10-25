package com.linguacraft.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.function.Consumer;

/** Records from the default microphone and feeds Vosk. Reports live (partial) text so the meter moves while you talk. */
public final class SpeechListener {
    private static final float SAMPLE_RATE = 16000f;

    private volatile boolean running;
    private Thread thread;

    /**
     * @param onText  called on the listener thread with every partial / final transcript
     * @param onError called once if the microphone or recognizer cannot be used
     */
    public void start(Model model, Consumer<String> onText, Consumer<String> onError) {
        stop();
        running = true;
        thread = new Thread(() -> run(model, onText, onError), "LinguaCraft-Speech");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            try {
                t.join(1500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void run(Model model, Consumer<String> onText, Consumer<String> onError) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            onError.accept("No microphone found.");
            return;
        }
        try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
             Recognizer rec = new Recognizer(model, SAMPLE_RATE)) {
            line.open(format);
            line.start();
            byte[] buf = new byte[4096];
            while (running) {
                int n = line.read(buf, 0, buf.length);
                if (n <= 0) continue;
                String json = rec.acceptWaveForm(buf, n) ? rec.getResult() : rec.getPartialResult();
                String text = extract(json);
                if (!text.isEmpty()) onText.accept(text);
            }
            line.stop();
        } catch (Throwable t) {
            onError.accept("Microphone error: " + t.getMessage());
        }
    }

    private static String extract(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("text")) return o.get("text").getAsString();
            if (o.has("partial")) return o.get("partial").getAsString();
        } catch (Exception ignored) {
        }
        return "";
    }
}
