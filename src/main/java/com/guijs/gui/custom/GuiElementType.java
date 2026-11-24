package com.guijs.gui.custom;

/**
 * UI element kinds supported by the custom GUI framework.
 */
public enum GuiElementType {
    /** Item slot bound to the backing container (or player inventory when playerIndex >= 0). */
    SLOT(0),
    /** Player inventory block (3 rows + hotbar) placed at x/y. */
    PLAYER_INVENTORY(1),
    /** Clickable button; clicks are routed back to GuiJSEvents.button via clickMenuButton. */
    BUTTON(2),
    /** Static text label. */
    LABEL(3),
    /** Progress bar driven by a ContainerData index (0..10000 scaled, rendered as fill). */
    PROGRESS(4),
    /** Decorative texture block. */
    IMAGE(5),
    /** Dynamic text label rendering "prefix + ContainerData[index]" (live updated). */
    DATA_LABEL(6);

    public static final GuiElementType[] VALUES = values();

    public final int id;

    GuiElementType(int id) {
        this.id = id;
    }

    public static GuiElementType byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : LABEL;
    }
}
