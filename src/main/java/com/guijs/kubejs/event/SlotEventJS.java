package com.guijs.kubejs.event;

import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.player.PlayerEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

@Info("Context passed to slot callbacks (onTake / onChange) of custom GuiJS GUIs.")
public class SlotEventJS extends PlayerEventJS {
    private final ServerPlayer player;
    private final ItemStack stack;
    @Nullable
    private final String slotId;
    private final int slotIndex;
    private final int containerIndex;
    @Nullable
    private final String guiId;
    @Nullable
    private final BlockPos pos;

    public SlotEventJS(ServerPlayer player, ItemStack stack, @Nullable String slotId, int slotIndex, int containerIndex, @Nullable String guiId, @Nullable BlockPos pos) {
        this.player = player;
        this.stack = stack;
        this.slotId = slotId;
        this.slotIndex = slotIndex;
        this.containerIndex = containerIndex;
        this.guiId = guiId;
        this.pos = pos;
    }

    @Override
    public ServerPlayer getEntity() {
        return player;
    }

    @Info("The player interacting with the slot.")
    public ServerPlayer getPlayer() {
        return player;
    }

    @Info("onTake: the stack that was taken; onChange: the stack now in the slot.")
    public ItemStack getStack() {
        return stack;
    }

    @Nullable
    @Info("The slot id given in the builder (null when not set).")
    public String getSlotId() {
        return slotId;
    }

    @Info("Ordinal of the slot inside the menu.")
    public int getSlotIndex() {
        return slotIndex;
    }

    @Info("Index of the slot inside its backing container (same as given to gui.slot(...)).")
    public int getContainerIndex() {
        return containerIndex;
    }

    @Nullable
    @Info("The GUI id the slot belongs to.")
    public String getGuiId() {
        return guiId;
    }

    @Nullable
    @Info("The anchored block position when the GUI was opened from a block entity.")
    public BlockPos getPos() {
        return pos;
    }
}
