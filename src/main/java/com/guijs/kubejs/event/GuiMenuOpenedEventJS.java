package com.guijs.kubejs.event;

import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.player.PlayerEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

@Info("Fired after a GUI was opened through the GuiJS binding.")
public class GuiMenuOpenedEventJS extends PlayerEventJS {
    private final ServerPlayer player;
    private final String menuId;
    @Nullable
    private final BlockPos pos;

    public GuiMenuOpenedEventJS(ServerPlayer player, String menuId, @Nullable BlockPos pos) {
        this.player = player;
        this.menuId = menuId;
        this.pos = pos;
    }

    @Override
    public ServerPlayer getEntity() {
        return player;
    }

    @Info("The player that opened the GUI.")
    public ServerPlayer getPlayer() {
        return player;
    }

    @Info("The menu id that was opened, e.g. 'minecraft:crafting' or 'farmersdelight:cooking_pot'.")
    public String getMenuId() {
        return menuId;
    }

    @Nullable
    @Info("The bound block position, if the GUI was opened with one.")
    public BlockPos getPos() {
        return pos;
    }
}
