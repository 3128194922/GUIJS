package com.guijs.block;

import com.guijs.GuiJSMod;
import dev.latvian.mods.kubejs.block.entity.BlockEntityJS;
import dev.latvian.mods.kubejs.block.entity.InventoryAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Vanilla-style container behavior for KubeJS block entities: when a block
 * with an {@code info.inventory(...)} attachment is broken (player or
 * explosion), its contents drop like a vanilla chest instead of vanishing.
 */
@Mod.EventBusSubscriber(modid = GuiJSMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BEDropHandler {
    private BEDropHandler() {
    }

    /** LOWEST so protection mods (world guard etc.) can cancel the break first. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        dropContents(level, event.getPos());
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        List<BlockPos> blocks = event.getAffectedBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            dropContents(level, blocks.get(i));
        }
    }

    private static void dropContents(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BlockEntityJS kbe && kbe.inventory instanceof InventoryAttachment attachment) {
            if (!attachment.isEmpty()) {
                Containers.dropContents(level, pos, attachment);
            }
            attachment.clearContent();
        }
    }
}
