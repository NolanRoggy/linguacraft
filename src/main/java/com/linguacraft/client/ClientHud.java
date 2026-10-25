package com.linguacraft.client;

import com.linguacraft.net.StartChallengePayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** The on-screen panel: word to say, live accuracy bar, countdown, and the final banner. */
public final class ClientHud {
    private ClientHud() {}

    private static final int PANEL_W = 280;
    private static final int PANEL_BG = 0xC0101010;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFB0B0B0;
    private static final int GREEN = 0xFF55FF55;
    private static final int YELLOW = 0xFFFFD84A;
    private static final int RED = 0xFFFF5555;
    private static final int BAR_BG = 0xFF303030;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        StartChallengePayload d = ClientChallenge.data;
        if (!ClientChallenge.active() || d == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        Font font = mc.font;

        int cx = g.guiWidth() / 2;
        int left = cx - PANEL_W / 2;
        int top = 8;
        int h = 92;
        g.fill(left, top, left + PANEL_W, top + h, PANEL_BG);
        g.renderOutline(left, top, PANEL_W, h, 0xFF606060);

        // label
        g.drawCenteredString(font, d.label(), cx, top + 6, GRAY);

        // the word, drawn at 2x (shrinks for long phrases so it still fits)
        float scale = d.text().length() > 14 ? 1.0f : 2.0f;
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1f);
        g.drawCenteredString(font, d.text(), (int) (cx / scale), (int) ((top + 18) / scale), WHITE);
        g.pose().popPose();

        // pronunciation + meaning
        int infoY = top + 18 + (int) (9 * scale) + 4;
        String info = d.reading().equals(d.text()) ? d.english() : d.reading() + "  -  " + d.english();
        g.drawCenteredString(font, info, cx, infoY, YELLOW);

        ClientChallenge.State st = ClientChallenge.state;
        int barY = top + 62;

        if (st == ClientChallenge.State.PREPARING || st == ClientChallenge.State.ERROR) {
            g.drawCenteredString(font, ClientChallenge.status, cx, barY + 4, st == ClientChallenge.State.ERROR ? RED : GRAY);
            return;
        }

        // accuracy bar
        int barX = left + 10;
        int barW = PANEL_W - 20;
        int pct = ClientChallenge.percent;
        g.fill(barX, barY, barX + barW, barY + 10, BAR_BG);
        int fillW = barW * pct / 100;
        int color = pct >= d.thresholdPercent() ? GREEN : (pct >= 50 ? YELLOW : RED);
        g.fill(barX, barY, barX + fillW, barY + 10, color);
        int thrX = barX + barW * d.thresholdPercent() / 100;
        g.fill(thrX, barY - 2, thrX + 1, barY + 12, WHITE); // threshold marker
        g.drawCenteredString(font, pct + "%", cx, barY + 1, 0xFF000000 | 0x202020);

        // status line
        String line;
        int lineColor = GRAY;
        if (st == ClientChallenge.State.LISTENING) {
            int dots = (int) ((System.currentTimeMillis() / 400) % 4);
            line = "Listening" + ".".repeat(dots);
            String heard = ClientChallenge.heard;
            if (!heard.isEmpty()) line += "   heard: " + heard;
        } else {
            line = ClientChallenge.banner;
            lineColor = st == ClientChallenge.State.PASSED ? GREEN : RED;
        }
        g.drawCenteredString(font, font.plainSubstrByWidth(line, PANEL_W - 12), cx, barY + 16, lineColor);

        // countdown bar
        if (st == ClientChallenge.State.LISTENING) {
            long elapsed = System.currentTimeMillis() - ClientChallenge.listenStartMs;
            float left01 = Math.max(0f, 1f - elapsed / (float) (d.limitTicks() * 50L));
            g.fill(barX, top + h - 8, barX + barW, top + h - 5, BAR_BG);
            g.fill(barX, top + h - 8, barX + (int) (barW * left01), top + h - 5, left01 > 0.3f ? WHITE : RED);
        }
    }
}
