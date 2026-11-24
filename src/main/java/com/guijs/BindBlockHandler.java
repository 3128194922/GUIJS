package com.guijs;

import com.guijs.gui.GuiOpener;
import com.guijs.gui.custom.GuiRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Auto-opens custom GUIs bound to blocks with {@code GuiJS.bindBlock}:
 * right-clicking a bound block opens its GUI instead of the default behavior.
 */
@Mod.EventBusSubscriber(modid = GuiJSMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BindBlockHandler {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var key = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
        String guiId = GuiRegistry.guiForBlock(key.toString());
        if (guiId == null) {
            return;
        }
        event.setCanceled(true);
        GuiOpener.openCustom(player, guiId, event.getPos());
    }
}
