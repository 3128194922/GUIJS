package com.guijs.gui.custom;

import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;

/**
 * One UI element of a custom GUI. A single mutable class is used for all
 * element kinds - the fields that matter depend on {@link #type}:
 * <ul>
 *   <li>SLOT: x, y, index (backing container index or player inventory index
 *       when playerIndex >= 0)</li>
 *   <li>PLAYER_INVENTORY: x, y</li>
 *   <li>BUTTON: id, x, y, w, h, text</li>
 *   <li>LABEL: x, y, text, color</li>
 *   <li>PROGRESS: id, x, y, w, h, index (ContainerData index)</li>
 *   <li>IMAGE: x, y, w, h, text (texture)</li>
 * </ul>
 */
public class GuiElement {
    /** Anchor modes (build-time only, resolved to absolute pixels before sync). */
    public static final int ANCHOR_START = 0;    // left / top
    public static final int ANCHOR_CENTER = 1;   // horizontally centered / vertically centered
    public static final int ANCHOR_END = 2;      // right / bottom

    public GuiElementType type = GuiElementType.LABEL;
    @Nullable
    public String id;
    /** Container slot index (SLOT) or ContainerData index (PROGRESS). */
    public int index;
    /** Player inventory index for SLOT elements taken from the player inventory. */
    public int playerIndex = -1;
    public int x, y, w, h;
    @Nullable
    public String text;
    public int color = 0x404040;
    /** Build-time anchors: x/y are offsets relative to the anchored position. */
    public int anchorX = ANCHOR_START;
    public int anchorY = ANCHOR_START;

    // ---- Slot rules & callbacks (server-side, build-time only, never synced) ----
    /**
     * Slot placement filter: an {@code dev.latvian.mods.kubejs.item.ingredient.IngredientJS}
     * compatible object (supports NBT matching) or a {@code Predicate<ItemStack>}.
     */
    @Nullable
    public Object filter;
    /** Ghost slot: clicking stores a 1-count sample of the held item instead of transferring. */
    public boolean ghost;
    /** Custom stack limit for the slot (0 = default). */
    public int maxStackSize;
    /** When true the slot content cannot be taken out. */
    public boolean locked;
    /** JS callback fired when a player takes the stack out of the slot. */
    @Nullable
    public transient Object jsOnTake;
    /** JS callback fired when the slot content changes through player interaction. */
    @Nullable
    public transient Object jsOnChange;
    /** JS callback fired when the button is clicked (button elements). */
    @Nullable
    public transient Object jsOnClick;

    /** Nominal element size used by the anchor resolver (labels estimate 6px per char). */
    public int effectiveWidth() {
        return switch (type) {
            case BUTTON, PROGRESS, IMAGE -> w;
            case SLOT -> 18;
            case PLAYER_INVENTORY -> 162;
            case DATA_LABEL -> (text == null ? 0 : text.length() * 6) + 28;
            case LABEL -> text == null ? 0 : text.length() * 6;
        };
    }

    public int effectiveHeight() {
        return switch (type) {
            case BUTTON, PROGRESS, IMAGE -> h;
            case SLOT -> 18;
            case PLAYER_INVENTORY -> 76;
            case LABEL, DATA_LABEL -> 9;
        };
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByte(type.id);
        buf.writeBoolean(id != null);
        if (id != null) {
            buf.writeUtf(id);
        }
        buf.writeVarInt(index);
        buf.writeVarInt(playerIndex);
        buf.writeVarInt(x);
        buf.writeVarInt(y);
        buf.writeVarInt(w);
        buf.writeVarInt(h);
        buf.writeBoolean(text != null);
        if (text != null) {
            buf.writeUtf(text);
        }
        buf.writeInt(color);
    }

    public static GuiElement read(FriendlyByteBuf buf) {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.byId(buf.readByte());
        e.id = buf.readBoolean() ? buf.readUtf() : null;
        e.index = buf.readVarInt();
        e.playerIndex = buf.readVarInt();
        e.x = buf.readVarInt();
        e.y = buf.readVarInt();
        e.w = buf.readVarInt();
        e.h = buf.readVarInt();
        e.text = buf.readBoolean() ? buf.readUtf() : null;
        e.color = buf.readInt();
        return e;
    }
}
