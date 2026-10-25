package com.linguacraft.net;

import com.linguacraft.LinguaCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: the best accuracy the microphone reached for challenge {@code id}.
 * {@code percent == -1} means "I could not listen" (no mic, model failed to load): the server cancels with no penalty.
 */
public record ReportPayload(int id, int percent) implements CustomPacketPayload {

    public static final Type<ReportPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LinguaCraft.MODID, "report"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReportPayload> CODEC =
            StreamCodec.ofMember(
                    (ReportPayload p, RegistryFriendlyByteBuf buf) -> {
                        buf.writeVarInt(p.id);
                        buf.writeInt(p.percent);
                    },
                    buf -> new ReportPayload(buf.readVarInt(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
