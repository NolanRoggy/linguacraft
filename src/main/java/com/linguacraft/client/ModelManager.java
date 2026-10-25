package com.linguacraft.client;

import com.linguacraft.core.Language;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Downloads (once) and unpacks the offline Vosk speech model for a language into config/linguacraft/models. */
public final class ModelManager {
    private ModelManager() {}

    public static Path modelsDir() {
        return FMLPaths.CONFIGDIR.get().resolve("linguacraft").resolve("models");
    }

    /** @return the folder containing the unpacked model, downloading it first if needed. Blocks; call off-thread. */
    public static Path ensure(Language lang, Consumer<String> status) throws IOException, InterruptedException {
        Path root = modelsDir();
        Path target = root.resolve(lang.modelName);
        if (Files.isDirectory(target.resolve("am")) || Files.isDirectory(target.resolve("conf"))) return target;

        Files.createDirectories(root);
        Path zip = root.resolve(lang.modelName + ".zip.part");

        status.accept("Downloading " + lang.displayName + " speech model...");
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpResponse<InputStream> resp = client.send(
                HttpRequest.newBuilder(URI.create(lang.modelUrl())).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) throw new IOException("Model download failed: HTTP " + resp.statusCode());

        long total = resp.headers().firstValueAsLong("content-length").orElse(-1);
        try (InputStream in = resp.body(); var out = Files.newOutputStream(zip)) {
            byte[] buf = new byte[64 * 1024];
            long done = 0;
            int n;
            int lastPct = -1;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                done += n;
                if (total > 0) {
                    int pct = (int) (100 * done / total);
                    if (pct != lastPct) {
                        lastPct = pct;
                        status.accept("Downloading " + lang.displayName + " speech model... " + pct + "%");
                    }
                }
            }
        }

        status.accept("Unpacking speech model...");
        Path tmp = root.resolve(lang.modelName + ".unpack");
        deleteRecursive(tmp);
        Files.createDirectories(tmp);
        unzip(zip, tmp);
        Files.deleteIfExists(zip);

        // The zip contains one top-level folder named like the model.
        Path inner = tmp.resolve(lang.modelName);
        if (!Files.isDirectory(inner)) inner = tmp;
        deleteRecursive(target);
        Files.move(inner, target, StandardCopyOption.REPLACE_EXISTING);
        deleteRecursive(tmp);
        return target;
    }

    private static void unzip(Path zip, Path dest) throws IOException {
        Path destReal = dest.toAbsolutePath().normalize();
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                Path out = destReal.resolve(e.getName()).normalize();
                if (!out.startsWith(destReal)) throw new IOException("Bad zip entry: " + e.getName()); // zip-slip guard
                if (e.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zin, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteRecursive(Path p) throws IOException {
        if (!Files.exists(p)) return;
        try (var walk = Files.walk(p)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(x -> {
                try {
                    Files.delete(x);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
