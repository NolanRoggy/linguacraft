package com.linguacraft.net;

import com.linguacraft.LinguaCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: how the challenge ended (drives the green "Unlocked!" / red "Deleted!" banner). */
public record ResultPayload(int id, boolean passed, String message) implements CustomPacketPayload {

    public static final Type<ResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LinguaCraft.MODID, "result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResultPayload> CODEC =
            StreamCodec.ofMember(
                    (ResultPayload p, RegistryFriendlyByteBuf buf) -> {
                        buf.writeVarInt(p.id);
                        buf.writeBoolean(p.passed);
                        buf.writeUtf(p.message);
                    },
                    buf -> new ResultPayload(buf.readVarInt(), buf.readBoolean(), buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
