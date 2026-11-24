package com.guijs.gui;

import com.guijs.GuiJSMod;
import com.guijs.gui.custom.CustomGuiMenu;
import com.guijs.gui.custom.GuiRegistry;
import com.guijs.gui.custom.GuiUIDescription;
import dev.latvian.mods.kubejs.block.entity.BlockEntityJS;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Core engine that opens GUIs from scripts.
 *
 * <p>Resolution order for {@code modid:path} menu ids:</p>
 * <ol>
 *   <li>When a block position is given and its block entity is a
 *       {@link MenuProvider}, open that (full functionality, real data).</li>
 *   <li>Otherwise build the menu from the registered {@link MenuType}
 *       ({@code type.create(id, inv, buf)}): the payload buffer is written by
 *       the script, or defaults to a BlockPos when a position was given, or is
 *       empty. A dry-run create() is performed first so malformed payloads fail
 *       with a clear error instead of breaking the open packet.</li>
 * </ol>
 */
public final class GuiOpener {
    private GuiOpener() {
    }

    /** Thrown to scripts with a clear reason when a GUI cannot be opened. */
    public static class GuiOpenException extends RuntimeException {
        public GuiOpenException(String message) {
            super(message);
        }

        public GuiOpenException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Opens a vanilla GUI by short name ("crafting", "furnace", ...). */
    public static void openVanilla(ServerPlayer player, String name, Component title, BlockPos pos) {
        if (!VanillaGuis.isVanillaGui(name)) {
            throw new GuiOpenException("Unknown vanilla GUI '" + name + "'. Supported: " + VanillaGuis.NAMES);
        }
        if (pos != null) {
            MenuProvider bound = boundBlockEntity(player, pos);
            // Only native vanilla block entities take the bound path: their menu
            // providers need no payload buffer (furnace, chest, barrel, ...).
            if (bound != null && bound.getClass().getName().startsWith("net.minecraft.world.level.block.entity.")) {
                player.openMenu(bound);
                return;
            }
        }
        MenuProvider provider = VanillaGuis.createProvider(name, player.level(), player.getInventory(), pos, title);
        if (provider == null) {
            throw new GuiOpenException("Unsupported vanilla GUI '" + name + "'");
        }
        player.openMenu(provider);
    }

    /** Chest with explicit rows (1-6) and a transient inventory. */
    public static void openChest(ServerPlayer player, Component title, int rows) {
        player.openMenu(VanillaGuis.chestRows(rows, title));
    }

    /** Opens any registered menu type. */
    public static void openMenu(ServerPlayer player, String id, Component title, BlockPos pos, Consumer<FriendlyByteBuf> payloadWriter) {
        MenuType<?> type = lookup(id);

        // 1) Prefer the real block entity: it builds its own menu with live data.
        //    Menus with a dedicated payload template skip this path - the template
        //    defines the correct buffer format for the network factory.
        if (pos != null && !PayloadTemplates.has(id)) {
            MenuProvider bound = boundBlockEntity(player, pos);
            if (bound != null) {
                if (isVanillaType(id)) {
                    player.openMenu(bound);
                } else {
                    // Modded block entities read their menu data from the open payload
                    // (usually the BlockPos); openMenu alone would send an empty buffer.
                    NetworkHooks.openScreen(player, bound, buf -> writeAutoPayload(buf, player, id, pos, payloadWriter));
                }
                return;
            }
        }

        if (title == null) {
            title = Component.literal(prettyName(id));
        }

        // 2) Build from the registered menu type with a payload buffer.
        MenuProvider provider = new SimpleMenuProvider((windowId, inv, p) ->
                type.create(windowId, inv, payloadBuffer(player, id, pos, payloadWriter)), title);

        // Dry-run: fail fast with a readable error instead of corrupting the open packet.
        try {
            AbstractContainerMenu probe = type.create(0, player.getInventory(), payloadBuffer(player, id, pos, payloadWriter));
            if (probe == null) {
                throw new GuiOpenException("Menu '" + id + "' factory returned null");
            }
        } catch (GuiOpenException e) {
            throw e;
        } catch (Exception e) {
            throw new GuiOpenException("Cannot open '" + id + "': " + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + (pos == null && payloadWriter == null
                    ? " (this menu likely needs a block position or a custom payload - pass the position of a real block, or a payload writer)"
                    : ""), e);
        }

        if (isVanillaType(id)) {
            player.openMenu(provider);
        } else {
            // Modded menu types are opened through the Forge network path so the
            // client screen factory receives the same payload buffer.
            NetworkHooks.openScreen(player, provider, buf -> writeAutoPayload(buf, player, id, pos, payloadWriter));
        }
    }

    /**
     * Opens a custom script-defined GUI ({@code GuiJS.createGui}).
     * When anchored to a block entity with a KubeJS inventory attachment,
     * container slots are bound to it (persisted, real storage).
     */
    public static void openCustom(ServerPlayer player, String guiId, @Nullable BlockPos anchorPos) {
        GuiUIDescription desc = GuiRegistry.get(guiId);
        if (desc == null) {
            throw new GuiOpenException("Custom GUI '" + guiId + "' is not registered - define it in startup scripts with GuiJS.createGui('" + guiId + "', ...)");
        }

        Container backing = null;
        BlockState anchorState = null;
        if (anchorPos != null) {
            BlockEntity be = player.level().getBlockEntity(anchorPos);
            if (be instanceof BlockEntityJS kbe && kbe.inventory != null && kbe.inventory instanceof Container c) {
                backing = c;
                anchorState = player.level().getBlockState(anchorPos);
            }
        }

        final Container backingF = backing;
        final BlockState anchorStateF = anchorState;
        final BlockPos anchorF = anchorPos;
        ContainerData data = new SimpleContainerData(desc.dataCount());
        MenuProvider provider = new SimpleMenuProvider(
                (windowId, inv, p) -> new CustomGuiMenu(GuiJSMod.CUSTOM_MENU.get(), windowId, inv,
                        desc, backingF, data, guiId, anchorF, anchorStateF),
                desc.title);
        NetworkHooks.openScreen(player, provider, desc::write);
    }

    /** All registered menu type ids, sorted. */
    public static List<String> listMenus() {
        List<String> out = new ArrayList<>();
        for (ResourceLocation key : BuiltInRegistries.MENU.keySet()) {
            out.add(key.toString());
        }
        out.sort(String::compareTo);
        return out;
    }

    /** All registered menu type ids whose path or namespace contains the keyword. */
    public static List<String> findMenus(String keyword) {
        String k = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String id : listMenus()) {
            if (id.contains(k)) {
                out.add(id);
            }
        }
        return out;
    }

    public static MenuType<?> lookup(String id) {
        ResourceLocation rl = parse(id);
        if (rl == null) {
            throw new GuiOpenException("Invalid menu id '" + id + "' (expected 'modid:path')");
        }
        MenuType<?> type = BuiltInRegistries.MENU.get(rl);
        if (type == null) {
            List<String> similar = findMenus(rl.getPath());
            throw new GuiOpenException("Menu type '" + id + "' is not registered. Similar: " + similar.subList(0, Math.min(5, similar.size())));
        }
        return type;
    }

    public static boolean hasMenu(String id) {
        ResourceLocation rl = parse(id);
        return rl != null && BuiltInRegistries.MENU.get(rl) != null;
    }

    private static MenuProvider boundBlockEntity(ServerPlayer player, BlockPos pos) {
        BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof MenuProvider mp ? mp : null;
    }

    private static FriendlyByteBuf payloadBuffer(ServerPlayer player, String menuId, BlockPos pos, Consumer<FriendlyByteBuf> payloadWriter) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        writeAutoPayload(buf, player, menuId, pos, payloadWriter);
        return buf;
    }

    /**
     * Payload decision order: script-provided writer, built-in template
     * (special-format menus), generic BlockPos when a position was given,
     * otherwise empty.
     */
    private static void writeAutoPayload(FriendlyByteBuf buf, ServerPlayer player, String menuId, BlockPos pos, Consumer<FriendlyByteBuf> payloadWriter) {
        if (payloadWriter != null) {
            payloadWriter.accept(buf);
        } else if (PayloadTemplates.write(menuId, player, pos, buf)) {
            // template handled it
        } else if (pos != null) {
            buf.writeBlockPos(pos);
        }
    }

    private static boolean isVanillaType(String id) {
        ResourceLocation rl = parse(id);
        return rl != null && rl.getNamespace().equals("minecraft");
    }

    private static ResourceLocation parse(String id) {
        if (id == null || id.isEmpty() || !id.contains(":")) {
            return null;
        }
        return ResourceLocation.tryParse(id);
    }

    private static String prettyName(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
