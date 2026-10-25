package com.linguacraft.client;

import com.linguacraft.net.ResultPayload;
import com.linguacraft.net.StartChallengePayload;

/** Entry points for packets arriving on the client (kept separate so the server never loads client classes). */
public final class ClientPayloads {
    private ClientPayloads() {}

    public static void onStart(StartChallengePayload p) {
        ClientChallenge.start(p);
    }

    public static void onResult(ResultPayload r) {
        ClientChallenge.onResult(r);
    }
}
