package com.guijs.kubejs.event;

import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.player.PlayerEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

@Info("Fired when a button of a custom GuiJS GUI is clicked (server side).")
public class ButtonClickedEventJS extends PlayerEventJS {
    private final ServerPlayer player;
    @Nullable
    private final String guiId;
    @Nullable
    private final String buttonId;
    private final int elementOrdinal;
    @Nullable
    private final BlockPos pos;

    public ButtonClickedEventJS(ServerPlayer player, @Nullable String guiId, @Nullable String buttonId, int elementOrdinal, @Nullable BlockPos pos) {
        this.player = player;
        this.guiId = guiId;
        this.buttonId = buttonId;
        this.elementOrdinal = elementOrdinal;
        this.pos = pos;
    }

    @Override
    public ServerPlayer getEntity() {
        return player;
    }

    @Info("The player that clicked the button.")
    public ServerPlayer getPlayer() {
        return player;
    }

    @Nullable
    @Info("The GUI id the button belongs to ('null' when opened without a registered GUI id).")
    public String getGuiId() {
        return guiId;
    }

    @Nullable
    @Info("The button id given in GuiBuilder.button(...).")
    public String getButtonId() {
        return buttonId;
    }

    @Info("Ordinal of the button element inside the GUI description (fallback id).")
    public int getElementOrdinal() {
        return elementOrdinal;
    }

    @Nullable
    @Info("The anchored block position when the GUI was opened from a block entity.")
    public BlockPos getPos() {
        return pos;
    }
}
