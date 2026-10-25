package com.linguacraft.net;

import com.linguacraft.LinguaCraft;
import com.linguacraft.core.Language;
import com.linguacraft.core.Word;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: "say this now". */
public record StartChallengePayload(
        int id,
        boolean itemChallenge,
        Language language,
        String text,
        String reading,
        String english,
        String kana,
        String label,
        int limitTicks,
        int thresholdPercent
) implements CustomPacketPayload {

    public static final Type<StartChallengePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LinguaCraft.MODID, "start_challenge"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartChallengePayload> CODEC =
            StreamCodec.ofMember(StartChallengePayload::write, StartChallengePayload::read);

    public Word word() {
        return new Word(text, reading, english, kana);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeBoolean(itemChallenge);
        buf.writeVarInt(language.ordinal());
        buf.writeUtf(text);
        buf.writeUtf(reading);
        buf.writeUtf(english);
        buf.writeUtf(kana);
        buf.writeUtf(label);
        buf.writeVarInt(limitTicks);
        buf.writeVarInt(thresholdPercent);
    }

    private static StartChallengePayload read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        boolean item = buf.readBoolean();
        Language lang = Language.values()[buf.readVarInt()];
        String text = buf.readUtf();
        String reading = buf.readUtf();
        String english = buf.readUtf();
        String kana = buf.readUtf();
        String label = buf.readUtf();
        int limit = buf.readVarInt();
        int threshold = buf.readVarInt();
        return new StartChallengePayload(id, item, lang, text, reading, english, kana, label, limit, threshold);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
