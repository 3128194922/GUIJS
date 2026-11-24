package com.guijs.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Built-in payload templates for menu types whose network buffer is not a
 * plain leading BlockPos (discovered from the installed mod sources).
 * Scripts can always override with their own payload writer.
 */
public final class PayloadTemplates {
    private PayloadTemplates() {
    }

    /** Three-argument payload writer (player, anchor pos, buffer). */
    @FunctionalInterface
    public interface PayloadWriter {
        void write(ServerPlayer player, BlockPos pos, FriendlyByteBuf buf);
    }

    private static final Map<String, PayloadWriter> SPECIAL = new HashMap<>();

    static {
        // Create 6.0: schematic table / schematicannon / toolbox read (BlockPos + NBT update tag)
        for (String id : new String[]{"create:schematic_table", "create:schematicannon", "create:toolbox"}) {
            SPECIAL.put(id, PayloadTemplates::createPosNbt);
        }
        // Create 6.0: stock keeper request menu reads (boolean showLockOption, boolean isLocked, BlockPos)
        SPECIAL.put("create:stock_keeper_request", (player, pos, buf) -> {
            buf.writeBoolean(false);
            buf.writeBoolean(false);
            buf.writeBlockPos(pos);
        });
        // Travelers Backpack: (int screenID, int entityId); 2 = wearable screen, -1 = own backpack
        SPECIAL.put("travelersbackpack:backpack_item", (player, pos, buf) -> {
            buf.writeInt(2);
            buf.writeInt(-1);
        });
    }

    private static void createPosNbt(ServerPlayer player, BlockPos pos, FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        BlockEntity be = player.level().getBlockEntity(pos);
        CompoundTag tag = be != null ? be.getUpdateTag() : new CompoundTag();
        buf.writeNbt(tag);
    }

    public static boolean has(String menuId) {
        return SPECIAL.containsKey(menuId);
    }

    /**
     * Writes the template payload for the given menu id; returns false when no
     * template exists (caller falls back to the generic BlockPos payload).
     */
    public static boolean write(String menuId, ServerPlayer player, @Nullable BlockPos pos, FriendlyByteBuf buf) {
        PayloadWriter template = SPECIAL.get(menuId);
        if (template == null) {
            return false;
        }
        template.write(player, pos, buf);
        return true;
    }
}
