package com.guijs.kubejs;

import com.guijs.GuiJSMod;
import com.guijs.gui.GuiOpener;
import com.guijs.gui.VanillaGuis;
import com.guijs.gui.custom.GuiBuilder;
import com.guijs.gui.custom.GuiRegistry;
import com.guijs.kubejs.event.GuiJSEvents;
import com.guijs.kubejs.event.GuiMenuOpenedEventJS;
import dev.latvian.mods.kubejs.core.ServerPlayerKJS;
import dev.latvian.mods.kubejs.gui.chest.ChestMenuData;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * The {@code GuiJS} binding for KubeJS server scripts.
 *
 * <pre>{@code
 * GuiJS.openVanilla(player, 'crafting')                       // floating crafting table
 * GuiJS.openVanilla(player, 'furnace', [x, y, z])             // bound to a real furnace
 * GuiJS.openChest(player, '我的箱子', 4)                       // 4-row chest
 * GuiJS.open(player, 'farmersdelight:cooking_pot', [x, y, z]) // any mod GUI by menu id
 * GuiJS.open(player, 'cgm:attachments')                       // context-free mod GUI
 * }</pre>
 */
public class GuiJSBinding {

    // ------------------------------------------------------------------
    // Vanilla GUIs (short names: chest, crafting, furnace, ...)
    // ------------------------------------------------------------------

    @Info("""
            Open a vanilla GUI for the player by short name.
            Supported names: chest, crafting, furnace, blast_furnace, smoker, anvil, smithing, enchanting_table, brewing_stand, dispenser, hopper, shulker_box, beacon, lectern, loom, stonecutter, grindstone, cartography_table, merchant.
            Without a position the GUI floats and does not validate against a real block.
            """)
    public void openVanilla(ServerPlayer player, String name) {
        openVanilla(player, name, null, null);
    }

    @Info("Open a vanilla GUI bound to the block position (full functionality when the block exists).")
    public void openVanilla(ServerPlayer player, String name, BlockPos pos) {
        openVanilla(player, name, null, pos);
    }

    @Info("Open a vanilla GUI with a custom title.")
    public void openVanilla(ServerPlayer player, String name, Component title) {
        openVanilla(player, name, title, null);
    }

    @Info("Open a vanilla GUI with a custom title, bound to the block position.")
    public void openVanilla(ServerPlayer player, String name, Component title, @Nullable BlockPos pos) {
        GuiOpener.openVanilla(player, name, title == null ? com.guijs.gui.VanillaGuis.defaultTitle(name) : title, pos);
        postOpened(player, "minecraft:" + name, pos);
    }

    // ------------------------------------------------------------------
    // Chests with custom row count / KubeJS callback chest GUI
    // ------------------------------------------------------------------

    @Info("Open a chest GUI with a custom title and 1-6 rows, backed by a transient inventory.")
    public void openChest(ServerPlayer player, Component title, int rows) {
        GuiOpener.openChest(player, title, rows);
        postOpened(player, "minecraft:chest", null);
    }

    @Info("""
            Open a custom callback-driven chest GUI (KubeJS built-in CustomChestMenu).
            The builder consumer receives the ChestMenuData to fill slots and register click handlers.
            """)
    public void openChestGUI(ServerPlayer player, Component title, int rows, Consumer<ChestMenuData> builder) {
        if (player instanceof ServerPlayerKJS kjs) {
            kjs.kjs$openChestGUI(title, rows, builder);
            postOpened(player, "kubejs:chest_gui", null);
        }
    }

    // ------------------------------------------------------------------
    // Any registered menu type (vanilla or any installed mod)
    // ------------------------------------------------------------------

    @Info("Open any registered menu by id ('modid:path') without extra context.")
    public void open(ServerPlayer player, String id) {
        openImpl(player, id, null, null, null);
    }

    @Info("Open any registered menu by id, bound to the block position (full functionality when its block entity provides the menu).")
    public void open(ServerPlayer player, String id, BlockPos pos) {
        openImpl(player, id, null, pos, null);
    }

    @Info("Open any registered menu by id with a payload writer (FriendlyByteBuf consumer) for menus that read custom network data.")
    public void open(ServerPlayer player, String id, Consumer<FriendlyByteBuf> payload) {
        openImpl(player, id, null, null, payload);
    }

    @Info("Open any registered menu by id with a custom title and a block position.")
    public void open(ServerPlayer player, String id, Component title, @Nullable BlockPos pos) {
        openImpl(player, id, title, pos, null);
    }

    @Info("Open any registered menu by id with a custom title and a payload writer (FriendlyByteBuf consumer).")
    public void open(ServerPlayer player, String id, Component title, @Nullable Consumer<FriendlyByteBuf> payload) {
        openImpl(player, id, title, null, payload);
    }

    // ------------------------------------------------------------------
    // Custom script-defined GUIs (fully custom layout)
    // ------------------------------------------------------------------

    @Info("""
            Register a fully custom GUI layout (call from startup_scripts).
            The builder supports: title, size, background, slot (block entity container), playerSlot,
            playerInventory, button, label, progress, image.
            Open with GuiJS.openCustom(player, guiId[, pos]) or bind to blocks with GuiJS.bindBlock.
            """)
    public void createGui(String guiId, Consumer<GuiBuilder> builder) {
        GuiBuilder b = new GuiBuilder();
        builder.accept(b);
        // Resolve anchors (center/right/bottom) into absolute pixels once, here,
        // and warn about elements that end up outside the panel.
        b.description.resolve();
        GuiRegistry.register(guiId, b.description);
    }

    @Info("""
            Bind a block id to a custom GUI id (call from startup_scripts):
            right-clicking the block opens the GUI instead of the default behavior.
            The block entity should have an inventory attachment (info.inventory(width, height))
            for the GUI slots to store items persistently.
            """)
    public void bindBlock(String blockId, String guiId) {
        GuiRegistry.bindBlock(blockId, guiId);
    }

    @Info("Open a custom GUI registered with GuiJS.createGui. Without a position the container slots have no persistent storage.")
    public void openCustom(ServerPlayer player, String guiId) {
        openCustom(player, guiId, null);
    }

    @Info("Open a custom GUI registered with GuiJS.createGui, anchored to a block entity: container slots bind to its inventory.")
    public void openCustom(ServerPlayer player, String guiId, @Nullable BlockPos anchorPos) {
        GuiOpener.openCustom(player, guiId, anchorPos);
        postOpened(player, GuiJSMod.MOD_ID + ":custom:" + guiId, anchorPos);
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    @Info("List all registered menu type ids ('modid:path'), sorted alphabetically.")
    public List<String> listMenus() {
        return GuiOpener.listMenus();
    }

    @Info("List registered menu type ids whose id contains the keyword (case-insensitive).")
    public List<String> findMenus(String keyword) {
        return GuiOpener.findMenus(keyword);
    }

    @Info("Check whether a menu type id is registered.")
    public boolean hasMenu(String id) {
        return GuiOpener.hasMenu(id);
    }

    @Info("""
            Diagnostic: returns the resolved layout of a custom GUI (all element types and
            absolute pixel rectangles). Use it from a command or console.info to verify the
            layout that is actually registered - overlapping/missing elements become obvious.
            """)
    public String getLayout(String guiId) {
        com.guijs.gui.custom.GuiUIDescription d = GuiRegistry.get(guiId);
        if (d == null) {
            return "GUI '" + guiId + "' is not registered";
        }
        StringBuilder sb = new StringBuilder()
                .append(guiId).append(' ').append(d.width).append('x').append(d.height).append(':');
        for (var e : d.elements) {
            sb.append(" [").append(e.type).append(" x=").append(e.x).append(" y=").append(e.y)
                    .append(' ').append(e.effectiveWidth()).append('x').append(e.effectiveHeight());
            if (e.id != null) {
                sb.append(" id=").append(e.id);
            }
            sb.append(']');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------

    private static void openImpl(ServerPlayer player, String id, @Nullable Component title, @Nullable BlockPos pos, @Nullable Consumer<FriendlyByteBuf> payload) {
        GuiOpener.openMenu(player, id, title, pos, payload);
        postOpened(player, id, pos);
    }

    private static void postOpened(ServerPlayer player, String menuId, @Nullable BlockPos pos) {
        if (GuiJSEvents.OPENED.hasListeners()) {
            GuiJSEvents.OPENED.post(ScriptType.SERVER, new GuiMenuOpenedEventJS(player, menuId, pos));
        }
    }
}
