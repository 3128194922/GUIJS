package com.guijs.gui.custom;

import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Builder exposed to KubeJS startup scripts through
 * {@code GuiJS.createGui('my_gui', gui => { ... })}.
 */
public class GuiBuilder {
    public final GuiUIDescription description = new GuiUIDescription();

    /** Current anchor state applied to subsequently added elements. */
    private int anchorX = GuiElement.ANCHOR_START;
    private int anchorY = GuiElement.ANCHOR_START;
    /** Most recently added element - the target of the chained slot rule methods. */
    @Nullable
    private GuiElement last;

    /**
     * Sets the anchor mode for all subsequently added elements.
     * horizontal: 'left' | 'center' | 'right'; vertical: 'top' | 'middle' | 'bottom'.
     * The given x/y then act as an offset from the anchored position, so a layout
     * adapts automatically to different GUI sizes.
     */
    @Info("""
            Set the anchor for subsequently added elements: anchor('center', 'top') etc.
            horizontal: 'left'|'center'|'right', vertical: 'top'|'middle'|'bottom'.
            Afterwards x/y are offsets from the anchored position (e.g. x=0 with 'center'
            means perfectly centered). Call anchor('left','top') to return to absolute mode.
            """)
    public GuiBuilder anchor(String horizontal, String vertical) {
        anchorX = parseAnchor(horizontal);
        anchorY = parseAnchor(vertical);
        return this;
    }

    private static int parseAnchor(String s) {
        return switch (s == null ? "" : s.toLowerCase(java.util.Locale.ROOT)) {
            case "center", "middle" -> GuiElement.ANCHOR_CENTER;
            case "right", "bottom", "end" -> GuiElement.ANCHOR_END;
            default -> GuiElement.ANCHOR_START;
        };
    }

    private GuiElement add(GuiElement e) {
        e.anchorX = anchorX;
        e.anchorY = anchorY;
        description.elements.add(e);
        last = e;
        return e;
    }

    // ------------------------------------------------------------------
    // Chained slot rules (apply to the most recently added slot element)
    // ------------------------------------------------------------------

    /**
     * Restricts what can be placed into the last added slot. Accepts an
     * ingredient ({@code 'minecraft:iron_ore'}, {@code Item.of(...).nbt({...})},
     * tag strings, ...) or a predicate function {@code stack => boolean}.
     * Ingredients match NBT when the ingredient specifies it.
     */
    @Info("""
            Restrict what may be placed into the previously added slot (chain after gui.slot/grid).
            Accepts an ingredient (e.g. 'minecraft:iron_ore', an IngredientJS with NBT, a tag '#forge:ingots/iron')
            or a predicate function (stack) => boolean. NBT is matched when the ingredient specifies it.
            """)
    public GuiBuilder filter(Object ingredientOrPredicate) {
        if (last != null && last.type == GuiElementType.SLOT) {
            last.filter = ingredientOrPredicate;
        }
        return this;
    }

    @Info("Makes the previously added slot a ghost slot: clicking stores a 1-count sample of the held item (for configuration), nothing is taken from the cursor.")
    public GuiBuilder ghost() {
        if (last != null && last.type == GuiElementType.SLOT) {
            last.ghost = true;
        }
        return this;
    }

    @Info("Prevents taking items out of the previously added slot (insert-only).")
    public GuiBuilder locked() {
        if (last != null && last.type == GuiElementType.SLOT) {
            last.locked = true;
        }
        return this;
    }

    @Info("Limits how many items of one stack the previously added slot accepts (chain after gui.slot).")
    public GuiBuilder maxStackSize(int limit) {
        if (last != null && last.type == GuiElementType.SLOT) {
            last.maxStackSize = limit;
        }
        return this;
    }

    @Info("""
            JS callback fired when a player takes a stack out of the previously added slot:
            (event) => { event.player, event.stack, event.slotId, event.containerIndex, event.guiId, event.pos }
            """)
    public GuiBuilder onTake(Consumer<com.guijs.kubejs.event.SlotEventJS> callback) {
        if (last != null && last.type == GuiElementType.SLOT) {
            last.jsOnTake = callback;
        }
        return this;
    }

    @Info("""
            JS callback fired when the content of the previously added slot changes through interaction:
            (event) => { event.player, event.stack, event.slotId, event.containerIndex, event.guiId, event.pos }
            """)
    public GuiBuilder onChange(Consumer<com.guijs.kubejs.event.SlotEventJS> callback) {
        if (last != null && last.type == GuiElementType.SLOT) {
            last.jsOnChange = callback;
        }
        return this;
    }

    @Info("GUI title (shown in the window header).")
    public GuiBuilder title(Object title) {
        description.title = title instanceof Component c ? c : Component.literal(String.valueOf(title));
        return this;
    }

    @Info("Full GUI size in pixels (default 176x166, the vanilla container size).")
    public GuiBuilder size(int width, int height) {
        description.width = width;
        description.height = height;
        return this;
    }

    @Info("""
            Background texture to blit, e.g. 'kubejs:textures/gui/my_gui.png'.
            The texture region [0,0,width,height] of the given texture size is stretched to the GUI.
            Omit to use the built-in default panel texture.
            """)
    public GuiBuilder background(String texture) {
        return background(texture, 256, 256);
    }

    @Info("Background texture with an explicit texture size (region [0,0,width,height] of that texture).")
    public GuiBuilder background(String texture, int textureWidth, int textureHeight) {
        ResourceLocation rl = ResourceLocation.tryParse(texture);
        if (rl != null) {
            description.background = rl;
            description.backgroundTextureWidth = textureWidth;
            description.backgroundTextureHeight = textureHeight;
        }
        return this;
    }

    @Info("Item slot bound to the backing container (block entity inventory), at GUI-local pixel position.")
    public GuiBuilder slot(int containerIndex, int x, int y) {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.SLOT;
        e.index = containerIndex;
        e.playerIndex = -1;
        e.x = x;
        e.y = y;
        add(e);
        return this;
    }

    /**
     * Block of container slots as a grid: startIndex..startIndex+cols*rows-1,
     * spaced 18px apart (standard slot size). Honors the current anchor.
     */
    @Info("Grid of container slots: startIndex, then cols x rows, top-left pixel position; slots are placed 18px apart. The whole grid block honors the current anchor.")
    public GuiBuilder grid(int startIndex, int cols, int rows, int x, int y) {
        int gridW = cols * 18;
        int gridH = rows * 18;
        int originX = x;
        int originY = y;
        switch (anchorX) {
            case GuiElement.ANCHOR_CENTER -> originX = (description.width - gridW) / 2 + x;
            case GuiElement.ANCHOR_END -> originX = description.width - gridW + x;
            default -> {
            }
        }
        switch (anchorY) {
            case GuiElement.ANCHOR_CENTER -> originY = (description.height - gridH) / 2 + y;
            case GuiElement.ANCHOR_END -> originY = description.height - gridH + y;
            default -> {
            }
        }
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                GuiElement e = new GuiElement();
                e.type = GuiElementType.SLOT;
                e.index = startIndex + row * cols + col;
                e.playerIndex = -1;
                e.x = originX + col * 18;
                e.y = originY + row * 18;
                description.elements.add(e);
            }
        }
        return this;
    }

    @Info("Item slot taken from the player inventory (main inventory index 9..35 or hotbar 0..8).")
    public GuiBuilder playerSlot(int playerIndex, int x, int y) {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.SLOT;
        e.index = playerIndex;
        e.playerIndex = playerIndex;
        e.x = x;
        e.y = y;
        add(e);
        return this;
    }

    @Info("Standard 3x9 player inventory plus hotbar at an explicit position (vanilla: 8, 84).")
    public GuiBuilder playerInventory(int x, int y) {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.PLAYER_INVENTORY;
        e.x = x;
        e.y = y;
        add(e);
        return this;
    }

    @Info("Standard 3x9 player inventory plus hotbar, automatically centered at the bottom of the GUI (recommended).")
    public GuiBuilder playerInventory() {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.PLAYER_INVENTORY;
        e.x = 0;
        e.y = -6;
        e.anchorX = GuiElement.ANCHOR_CENTER;
        e.anchorY = GuiElement.ANCHOR_END;
        add(e);
        return this;
    }

    @Info("""
            Clickable button. Clicks are sent to the server and fire the GuiJSEvents.button
            script event with (player, guiId, buttonId = the given id).
            """)
    public GuiBuilder button(String id, int x, int y, int w, int h, @Nullable String label) {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.BUTTON;
        e.id = id;
        e.x = x;
        e.y = y;
        e.w = w;
        e.h = h;
        e.text = label;
        add(e);
        return this;
    }

    @Info("""
            Clickable button with its own JS callback, fired when the button is clicked:
            (event) => { event.player, event.buttonId, event.guiId, event.pos }
            The global GUIJSEvents.button event still fires afterwards.
            """)
    public GuiBuilder button(String id, int x, int y, int w, int h, @Nullable String label, Consumer<com.guijs.kubejs.event.ButtonClickedEventJS> onClick) {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.BUTTON;
        e.id = id;
        e.x = x;
        e.y = y;
        e.w = w;
        e.h = h;
        e.text = label;
        e.jsOnClick = onClick;
        add(e);
        return this;
    }

    @Info("Dynamic text label rendering 'prefix + value of ContainerData[index]' (updates live; write the value from block entity tick via data['guijs'+index]).")
    public GuiBuilder dataLabel(int x, int y, int dataIndex, @Nullable String prefix) {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.DATA_LABEL;
        e.x = x;
        e.y = y;
        e.index = dataIndex;
        e.text = prefix;
        add(e);
        return this;
    }

    @Info("Static text label at the given position with an ARGB/RGB color (default 0x404040).")
    public GuiBuilder label(int x, int y, String text, int color) {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.LABEL;
        e.x = x;
        e.y = y;
        e.text = text;
        e.color = color;
        add(e);
        return this;
    }

    @Info("Static text label with the default color.")
    public GuiBuilder label(int x, int y, String text) {
        return label(x, y, text, 0x404040);
    }

    @Info("""
            Progress bar filled from the ContainerData value with the given index.
            The value range is 0..10000 (0% to 100%). Rendered as a horizontal fill.
            """)
    public GuiBuilder progress(String id, int x, int y, int w, int h, int dataIndex) {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.PROGRESS;
        e.id = id;
        e.x = x;
        e.y = y;
        e.w = w;
        e.h = h;
        e.index = dataIndex;
        add(e);
        return this;
    }

    @Info("Decorative texture block (a region [0,0,w,h] of the given texture drawn at x/y).")
    public GuiBuilder image(String texture, int x, int y, int w, int h) {
        GuiElement e = new GuiElement();
        e.type = GuiElementType.IMAGE;
        e.x = x;
        e.y = y;
        e.w = w;
        e.h = h;
        e.text = texture;
        add(e);
        return this;
    }
}
