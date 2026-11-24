package com.guijs.gui.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Full client-side layout of a custom GUI, defined from KubeJS via
 * {@link GuiBuilder} and synced inside the open-screen payload.
 */
public class GuiUIDescription {
    public static final int DEFAULT_WIDTH = 176;
    public static final int DEFAULT_HEIGHT = 166;

    public Component title = Component.empty();
    public int width = DEFAULT_WIDTH;
    public int height = DEFAULT_HEIGHT;
    /** Background texture; when null the built-in default panel is drawn. */
    @Nullable
    public ResourceLocation background;
    public int backgroundTextureWidth = 256;
    public int backgroundTextureHeight = 256;
    public final List<GuiElement> elements = new ArrayList<>();

    public void write(FriendlyByteBuf buf) {
        buf.writeComponent(title);
        buf.writeVarInt(width);
        buf.writeVarInt(height);
        buf.writeBoolean(background != null);
        if (background != null) {
            buf.writeResourceLocation(background);
            buf.writeVarInt(backgroundTextureWidth);
            buf.writeVarInt(backgroundTextureHeight);
        }
        buf.writeVarInt(elements.size());
        for (GuiElement e : elements) {
            e.write(buf);
        }
    }

    public static GuiUIDescription read(FriendlyByteBuf buf) {
        GuiUIDescription d = new GuiUIDescription();
        d.title = buf.readComponent();
        d.width = buf.readVarInt();
        d.height = buf.readVarInt();
        if (buf.readBoolean()) {
            d.background = buf.readResourceLocation();
            d.backgroundTextureWidth = buf.readVarInt();
            d.backgroundTextureHeight = buf.readVarInt();
        }
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            d.elements.add(GuiElement.read(buf));
        }
        return d;
    }

    /**
     * Resolves build-time anchors into absolute pixels. Call once after the
     * builder finished; the synced layout contains only absolute coordinates
     * so the client renderer stays dumb and cheap.
     */
    public void resolve() {
        for (GuiElement e : elements) {
            int w = e.effectiveWidth();
            int h = e.effectiveHeight();
            switch (e.anchorX) {
                case GuiElement.ANCHOR_CENTER -> e.x = (width - w) / 2 + e.x;
                case GuiElement.ANCHOR_END -> e.x = width - w + e.x;
                default -> {
                }
            }
            switch (e.anchorY) {
                case GuiElement.ANCHOR_CENTER -> e.y = (height - h) / 2 + e.y;
                case GuiElement.ANCHOR_END -> e.y = height - h + e.y;
                default -> {
                }
            }
            if (e.x < 0 || e.y < 0 || e.x + w > width || e.y + h > height) {
                com.guijs.GuiJSMod.LOGGER.warn(
                        "[GUIJS] Element '{}' (type {}) lies outside the GUI panel: pos=({},{}) size={}x{} panel={}x{}",
                        e.id == null ? "?" : e.id, e.type, e.x, e.y, w, h, width, height);
            }
        }
        checkOverlaps();
    }

    /**
     * Logs a warning for every pair of overlapping solid elements (slots,
     * buttons, progress bars, images, inventory blocks). Text labels are
     * skipped because their estimated width is unreliable.
     */
    private void checkOverlaps() {
        for (int i = 0; i < elements.size(); i++) {
            GuiElement a = elements.get(i);
            if (!isSolid(a)) continue;
            int aw = a.effectiveWidth();
            int ah = a.effectiveHeight();
            for (int j = i + 1; j < elements.size(); j++) {
                GuiElement b = elements.get(j);
                if (!isSolid(b)) continue;
                int bw = b.effectiveWidth();
                int bh = b.effectiveHeight();
                if (a.x < b.x + bw && b.x < a.x + aw && a.y < b.y + bh && b.y < a.y + ah) {
                    com.guijs.GuiJSMod.LOGGER.warn(
                            "[GUIJS] Overlapping elements in GUI panel: {}({},{},{},{}) intersects {}({},{},{},{})",
                            nameOf(a), a.x, a.y, aw, ah, nameOf(b), b.x, b.y, bw, bh);
                }
            }
        }
    }

    private static boolean isSolid(GuiElement e) {
        return switch (e.type) {
            case SLOT, PLAYER_INVENTORY, BUTTON, PROGRESS, IMAGE -> true;
            default -> false;
        };
    }

    private static String nameOf(GuiElement e) {
        return (e.id != null ? e.id : e.type.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** Largest backing-container slot index referenced by SLOT elements, or -1. */
    public int maxSlotIndex() {
        int max = -1;
        for (GuiElement e : elements) {
            if (e.type == GuiElementType.SLOT && e.playerIndex < 0) {
                max = Math.max(max, e.index);
            }
        }
        return max;
    }

    /** Number of ContainerData values needed (max PROGRESS data index + 1). */
    public int dataCount() {
        int max = -1;
        for (GuiElement e : elements) {
            if (e.type == GuiElementType.PROGRESS) {
                max = Math.max(max, e.index);
            }
        }
        return max + 1;
    }

    /** Number of BUTTON elements (also used as clickMenuButton id space). */
    public int buttonCount() {
        int n = 0;
        for (GuiElement e : elements) {
            if (e.type == GuiElementType.BUTTON) {
                n++;
            }
        }
        return n;
    }
}
