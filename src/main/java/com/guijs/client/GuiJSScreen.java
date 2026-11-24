package com.guijs.client;

import com.guijs.gui.custom.CustomGuiMenu;
import com.guijs.gui.custom.GuiElement;
import com.guijs.gui.custom.GuiElementType;
import com.guijs.gui.custom.GuiUIDescription;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nullable;

/**
 * Client screen for {@code guijs:custom}. Renders the script-defined layout:
 * background texture, buttons (click -> server via vanilla clickMenuButton),
 * labels, progress bars (vanilla ContainerData sync) and decorative images.
 * Item slots are rendered by the standard container screen machinery.
 */
public class GuiJSScreen extends AbstractContainerScreen<CustomGuiMenu> {
    private static final int PANEL_BG = 0xFFC6C6C6;
    private static final int PANEL_BORDER = 0xFF555555;
    private static final int PANEL_HIGHLIGHT = 0xFFFFFFFF;
    private static final int PANEL_SHADOW = 0xFF8B8B8B;
    private static final int BUTTON_COLOR = 0xFF8B8B8B;
    private static final int BUTTON_HOVER = 0xFFA8A8A8;
    private static final int BAR_BACK = 0xFF3A3A3A;
    private static final int BAR_FILL = 0xFF54B435;

    @Nullable
    private int[] buttonOrdinals;
    private boolean hasInventoryBlock;

    public GuiJSScreen(CustomGuiMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        GuiUIDescription desc = menu.description;
        this.imageWidth = desc.width;
        this.imageHeight = desc.height;
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        // Default label position must match a real PLAYER_INVENTORY element,
        // otherwise the "Inventory" caption would render at a wrong spot.
        this.inventoryLabelX = 8;
        this.inventoryLabelY = desc.height - 94;

        int buttons = desc.buttonCount();
        this.buttonOrdinals = new int[buttons];
        int bi = 0;
        for (int i = 0; i < desc.elements.size(); i++) {
            GuiElement e = desc.elements.get(i);
            if (e.type == GuiElementType.BUTTON) {
                buttonOrdinals[bi++] = i;
            } else if (e.type == GuiElementType.PLAYER_INVENTORY) {
                hasInventoryBlock = true;
                this.inventoryLabelX = e.x;
                this.inventoryLabelY = e.y - 11;
            }
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        GuiUIDescription desc = menu.description;
        int left = leftPos;
        int top = topPos;

        if (desc.background != null) {
            gfx.blit(desc.background, left, top, imageWidth, imageHeight,
                    0, 0, imageWidth, imageHeight, desc.backgroundTextureWidth, desc.backgroundTextureHeight);
        } else {
            drawDefaultPanel(gfx, left, top, imageWidth, imageHeight);
        }

        for (int ordinal = 0; ordinal < desc.elements.size(); ordinal++) {
            GuiElement e = desc.elements.get(ordinal);
            switch (e.type) {
                case BUTTON -> drawButton(gfx, e, mouseX, mouseY);
                case PROGRESS -> drawProgress(gfx, e);
                case IMAGE -> {
                    ResourceLocation tex = ResourceLocation.tryParse(e.text);
                    if (tex != null) {
                        gfx.blit(tex, left + e.x, top + e.y, e.w, e.h, 0, 0, e.w, e.h, 256, 256);
                    }
                }
                default -> {
                }
            }
        }
    }

    /** Clean vanilla-style panel: 1px dark border, top/left highlight, bottom/right shadow. */
    private void drawDefaultPanel(GuiGraphics gfx, int left, int top, int w, int h) {
        gfx.fill(left, top, left + w, top + h, PANEL_BORDER);
        gfx.fill(left + 1, top + 1, left + w - 1, top + h - 1, PANEL_BG);
        gfx.fill(left + 1, top + 1, left + w - 1, top + 2, PANEL_HIGHLIGHT);
        gfx.fill(left + 1, top + 1, left + 2, top + h - 1, PANEL_HIGHLIGHT);
        gfx.fill(left + 1, top + h - 2, left + w - 1, top + h - 1, PANEL_SHADOW);
        gfx.fill(left + w - 2, top + 1, left + w - 1, top + h - 1, PANEL_SHADOW);
    }

    private void drawButton(GuiGraphics gfx, GuiElement e, int mouseX, int mouseY) {
        int x = leftPos + e.x;
        int y = topPos + e.y;
        boolean hover = mouseX >= x && mouseY >= y && mouseX < x + e.w && mouseY < y + e.h;
        gfx.fill(x, y, x + e.w, y + e.h, hover ? BUTTON_HOVER : BUTTON_COLOR);
        gfx.fill(x, y, x + e.w, y + 1, 0xFFFFFFFF);
        gfx.fill(x, y, x + 1, y + e.h, 0xFFFFFFFF);
        gfx.fill(x, y + e.h - 1, x + e.w, y + e.h, PANEL_BORDER);
        gfx.fill(x + e.w - 1, y, x + e.w, y + e.h, PANEL_BORDER);

        if (e.text != null && !e.text.isEmpty()) {
            var font = this.font;
            int tw = font.width(e.text);
            gfx.drawString(font, e.text, x + (e.w - tw) / 2, y + (e.h - 8) / 2, 0x404040, false);
        }
    }

    private void drawProgress(GuiGraphics gfx, GuiElement e) {
        int x = leftPos + e.x;
        int y = topPos + e.y;
        gfx.fill(x, y, x + e.w, y + e.h, BAR_BACK);
        float ratio = Math.min(1.0F, menu.progress(e.index) / (float) CustomGuiMenu.MAX_PROGRESS);
        int fill = Math.round(e.w * ratio);
        if (fill > 0) {
            gfx.fill(x + 1, y + 1, x + 1 + fill - 1, y + e.h - 1, BAR_FILL);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        GuiUIDescription desc = menu.description;
        gfx.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        for (GuiElement e : desc.elements) {
            if (e.type == GuiElementType.LABEL && e.text != null) {
                gfx.drawString(font, e.text, e.x, e.y, e.color, false);
            } else if (e.type == GuiElementType.DATA_LABEL) {
                String prefix = e.text == null ? "" : e.text;
                gfx.drawString(font, prefix + menu.progress(e.index), e.x, e.y, e.color, false);
            }
        }
        // Only draw the "Inventory" caption when the layout actually contains the
        // player inventory block - otherwise it would float at a wrong position.
        if (hasInventoryBlock) {
            gfx.drawString(font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        GuiUIDescription desc = menu.description;
        if (buttonOrdinals != null && minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
            for (int ordinal : buttonOrdinals) {
                GuiElement e = desc.elements.get(ordinal);
                int x = leftPos + e.x;
                int y = topPos + e.y;
                if (mouseX >= x && mouseY >= y && mouseX < x + e.w && mouseY < y + e.h) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ordinal);
                    minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 1.0F);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
