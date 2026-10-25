package com.linguacraft.net;

import com.linguacraft.LinguaCraft;
import com.linguacraft.client.ClientPayloads;
import com.linguacraft.server.ChallengeManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = LinguaCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Net {
    private Net() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");

        r.playToClient(StartChallengePayload.TYPE, StartChallengePayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientPayloads.onStart(payload)));

        r.playToClient(ResultPayload.TYPE, ResultPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientPayloads.onResult(payload)));

        r.playToServer(ReportPayload.TYPE, ReportPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        ChallengeManager.report(sp, payload.id(), payload.percent());
                    }
                }));
    }
}
