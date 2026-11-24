package com.guijs;

import com.guijs.gui.custom.CustomGuiMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * GUIJS - KubeJS addon that lets scripts open GUIs:
 * vanilla menus (chest, crafting table, furnace, ...), any menu type
 * registered by any installed mod (block-bound, payload-based or
 * context-free), plus fully custom script-defined GUIs
 * ({@code guijs:custom}).
 */
@Mod(GuiJSMod.MOD_ID)
public class GuiJSMod {
    public static final String MOD_ID = "guijs";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MOD_ID);

    /** Script-defined custom GUI menu (slot layout, buttons, progress bars). */
    public static final RegistryObject<MenuType<CustomGuiMenu>> CUSTOM_MENU =
            MENUS.register("custom", () -> IForgeMenuType.create(CustomGuiMenu::clientCreate));

    public GuiJSMod(FMLJavaModLoadingContext context) {
        MENUS.register(context.getModEventBus());
        LOGGER.info("[GUIJS] Loaded. Use the GuiJS binding in KubeJS scripts.");
    }
}
