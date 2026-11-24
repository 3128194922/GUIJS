package com.guijs.client;

import com.guijs.GuiJSMod;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side registration: binds {@code guijs:custom} to {@link GuiJSScreen}
 * so the custom menu type opened through Forge's OpenContainer path renders
 * with the script-defined layout.
 */
@Mod.EventBusSubscriber(modid = GuiJSMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GuiJSClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(GuiJSMod.CUSTOM_MENU.get(), GuiJSScreen::new));
    }
}
