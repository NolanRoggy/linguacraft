package com.linguacraft.client;

import com.linguacraft.LinguaCraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client entry point: adds the in-game config screen (Mods -> LinguaCraft -> Config) where the default language can be switched. */
@Mod(value = LinguaCraft.MODID, dist = Dist.CLIENT)
public class LinguaCraftClient {
    public LinguaCraftClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
