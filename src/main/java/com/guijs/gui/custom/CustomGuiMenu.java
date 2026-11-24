package com.guijs.gui.custom;

import com.guijs.GuiJSMod;
import com.guijs.kubejs.event.ButtonClickedEventJS;
import com.guijs.kubejs.event.GuiJSEvents;
import com.guijs.kubejs.event.SlotEventJS;
import dev.latvian.mods.kubejs.block.entity.BlockEntityJS;
import dev.latvian.mods.kubejs.block.entity.InventoryAttachment;
import dev.latvian.mods.kubejs.item.ingredient.IngredientJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Fully script-defined container menu ({@code guijs:custom}).
 *
 * <p>Slots are laid out from the {@link GuiUIDescription} built in startup
 * scripts; backing-container slots are bound to the block entity inventory
 * (KubeJS {@code info.inventory(...)} attachment) when opened from a block.
 * Progress elements read the vanilla-synchronized {@link ContainerData};
 * button clicks arrive through the vanilla {@code clickMenuButton} path and
 * are forwarded to the {@code GuiJSEvents.button} script event.</p>
 */
public class CustomGuiMenu extends AbstractContainerMenu {
    public static final int MAX_PROGRESS = 10_000;

    public final GuiUIDescription description;
    /** Backing container: BE inventory on the server, sync placeholder on the client. */
    public final Container backing;
    public final ContainerData data;
    @Nullable
    public final String guiId;
    @Nullable
    public final BlockPos anchorPos;
    @Nullable
    private final BlockState anchorState;
    private final Inventory playerInventory;
    /**
     * Block entity owning the backing inventory (server side). Its script
     * {@code data} tag feeds progress values: keys {@code guijs0..guijsN}
     * (0..10000) map to ContainerData indexes 0..N.
     */
    @Nullable
    private final BlockEntityJS sourceBE;

    private int mainSlotsEnd;
    private int playerSlotsStart;

    /** Server-side construction (script-driven). */
    public CustomGuiMenu(MenuType<CustomGuiMenu> type, int containerId, Inventory playerInv,
                         GuiUIDescription description, @Nullable Container backing,
                         @Nullable ContainerData data, @Nullable String guiId, @Nullable BlockPos anchorPos,
                         @Nullable BlockState anchorState) {
        super(type, containerId);
        this.description = description;
        this.backing = backing != null ? backing : new SimpleContainer(Math.max(description.maxSlotIndex() + 1, 0));
        this.data = data != null ? data : new SimpleContainerData(description.dataCount());
        this.guiId = guiId;
        this.anchorPos = anchorPos;
        this.anchorState = anchorState;
        this.playerInventory = playerInv;
        this.sourceBE = this.backing instanceof InventoryAttachment ia ? ia.blockEntity : null;

        buildSlots(playerInv);
        addDataSlots(this.data);
    }

    /** Client-side network factory ({@code IMenuTypeExtension.create}). */
    public static CustomGuiMenu clientCreate(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        return new CustomGuiMenu(com.guijs.GuiJSMod.CUSTOM_MENU.get(), containerId, playerInv,
                GuiUIDescription.read(buf), null, null, null, null, null);
    }

    private void buildSlots(Inventory playerInv) {
        mainSlotsEnd = 0;
        playerSlotsStart = Integer.MAX_VALUE;

        // 1) Backing container slots first: they occupy [0, mainSlotsEnd).
        for (GuiElement e : description.elements) {
            if (e.type == GuiElementType.SLOT && e.playerIndex < 0) {
                addSlot(new GuiSlot(backing, Math.min(e.index, Math.max(backing.getContainerSize() - 1, 0)), e.x, e.y, e));
            }
        }
        mainSlotsEnd = slots.size();

        // 2) Visible player slots from the layout (PLAYER_INVENTORY block and single player slots).
        boolean hasInventoryBlock = false;
        java.util.Set<Integer> usedPlayerIndexes = new java.util.HashSet<>();
        for (GuiElement e : description.elements) {
            if (e.type == GuiElementType.PLAYER_INVENTORY) {
                hasInventoryBlock = true;
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 9; col++) {
                        usedPlayerIndexes.add(9 + row * 9 + col);
                        addSlot(new Slot(playerInv, 9 + row * 9 + col, e.x + col * 18, e.y + row * 18));
                    }
                }
                for (int col = 0; col < 9; col++) {
                    usedPlayerIndexes.add(col);
                    addSlot(new Slot(playerInv, col, e.x + col * 18, e.y + 58));
                }
            }
        }
        for (GuiElement e : description.elements) {
            if (e.type == GuiElementType.SLOT && e.playerIndex >= 0 && !usedPlayerIndexes.contains(e.index)) {
                usedPlayerIndexes.add(e.index);
                addSlot(new Slot(playerInv, e.index, e.x, e.y));
            }
        }

        // 3) Fallback: when the layout has no inventory block, append the full player
        //    inventory off-screen so shift-click transfers still work.
        if (!hasInventoryBlock) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int idx = 9 + row * 9 + col;
                    if (!usedPlayerIndexes.contains(idx)) {
                        addSlot(new Slot(playerInv, idx, -1000, 0));
                    }
                }
            }
            for (int col = 0; col < 9; col++) {
                if (!usedPlayerIndexes.contains(col)) {
                    addSlot(new Slot(playerInv, col, -1000, 0));
                }
            }
        }

        // Player slots occupy [mainSlotsEnd, slots.size()).
        playerSlotsStart = mainSlotsEnd;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        boolean fromPlayer = index >= playerSlotsStart;
        if (fromPlayer) {
            if (mainSlotsEnd <= 0 || !moveItemStackTo(stack, 0, mainSlotsEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, playerSlotsStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (anchorPos == null || anchorState == null) {
            return true;
        }
        Level level = player.level();
        if (!level.getBlockState(anchorPos).is(anchorState.getBlock())) {
            return false;
        }
        return player.distanceToSqr(anchorPos.getX() + 0.5, anchorPos.getY() + 0.5, anchorPos.getZ() + 0.5) <= 64.0;
    }

    /** Vanilla button-click path: direct JS callback plus the global GuiJSEvents.button event. */
    @Override
    public boolean clickMenuButton(Player player, int elementOrdinal) {
        if (elementOrdinal < 0 || elementOrdinal >= description.elements.size()) {
            return true;
        }
        GuiElement element = description.elements.get(elementOrdinal);
        if (element.type != GuiElementType.BUTTON) {
            return true;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (element.jsOnClick instanceof Consumer<?> consumer) {
                @SuppressWarnings("unchecked")
                Consumer<ButtonClickedEventJS> callback = (Consumer<ButtonClickedEventJS>) consumer;
                callback.accept(new ButtonClickedEventJS(serverPlayer, guiId, element.id, elementOrdinal, anchorPos));
            }
            if (GuiJSEvents.BUTTON.hasListeners()) {
                GuiJSEvents.BUTTON.post(ScriptType.SERVER,
                        new ButtonClickedEventJS(serverPlayer, guiId, element.id, elementOrdinal, anchorPos));
            }
        }
        return true;
    }

    /** Ghost slots: clicking stores a 1-count sample of the held item instead of transferring. */
    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof GuiSlot gs && gs.element.ghost) {
            ItemStack carried = getCarried();
            if (!carried.isEmpty() && !gs.mayPlace(carried)) {
                return; // placement filter (supports NBT matching) rejects the held item
            }
            gs.set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    private void fireSlotCallback(@Nullable Object callback, ItemStack stack, GuiSlot slot) {
        if (callback == null || !(playerInventory.player instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return;
        }
        if (callback instanceof Consumer<?> consumer) {
            @SuppressWarnings("unchecked")
            Consumer<SlotEventJS> c = (Consumer<SlotEventJS>) consumer;
            c.accept(new SlotEventJS(sp, stack, slot.element.id, slots.indexOf(slot), slot.containerIndex, guiId, anchorPos));
        }
    }

    /** Progress value (0..10000) for the given ContainerData index. */
    public int progress(int dataIndex) {
        int count = data.getCount();
        return dataIndex < count ? data.get(dataIndex) : 0;
    }

    /** Pulls progress values from the block entity script data before syncing. */
    @Override
    public void broadcastChanges() {
        if (sourceBE != null) {
            CompoundTag d = sourceBE.data;
            for (int i = 0; i < data.getCount(); i++) {
                data.set(i, d.getInt("guijs" + i));
            }
        }
        super.broadcastChanges();
    }

    /**
     * Slot with script-defined behavior: placement filter (ingredient / NBT-aware
     * predicate), lock, stack limit, ghost mode and take/change callbacks.
     */
    public class GuiSlot extends Slot {
        public final GuiElement element;
        public final int containerIndex;

        GuiSlot(Container container, int containerIndex, int x, int y, GuiElement element) {
            super(container, containerIndex, x, y);
            this.containerIndex = containerIndex;
            this.element = element;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Container-level filter (info.inventory(..., inputFilter)) applies to all paths.
            if (container instanceof InventoryAttachment attachment
                    && attachment.inputFilter != null
                    && !attachment.inputFilter.test(stack)) {
                return false;
            }
            Object f = element.filter;
            if (f instanceof Predicate<?> predicate) {
                @SuppressWarnings("unchecked")
                Predicate<ItemStack> test = (Predicate<ItemStack>) predicate;
                return test.test(stack);
            }
            if (f != null) {
                return IngredientJS.of(f).test(stack);
            }
            return true;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !element.locked && super.mayPickup(player);
        }

        @Override
        public int getMaxStackSize() {
            return element.maxStackSize > 0 ? element.maxStackSize : super.getMaxStackSize();
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            fireSlotCallback(element.jsOnTake, stack, this);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            fireSlotCallback(element.jsOnChange, getItem(), this);
        }
    }
}
