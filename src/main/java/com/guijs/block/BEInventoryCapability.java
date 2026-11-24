package com.guijs.block;

import com.guijs.GuiJSMod;
import dev.latvian.mods.kubejs.block.entity.BlockEntityJS;
import dev.latvian.mods.kubejs.block.entity.InventoryAttachment;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bridges KubeJS block entity inventories to the Forge capability system:
 * every {@code BlockEntityJS} with an {@code info.inventory(...)} attachment
 * automatically exposes an {@code IItemHandler}, making it fully compatible
 * with vanilla hoppers (Forge patches route them through capabilities) and
 * modded pipes/conduits.
 */
@Mod.EventBusSubscriber(modid = GuiJSMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BEInventoryCapability {
    private static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(GuiJSMod.MOD_ID, "inventory");

    private BEInventoryCapability() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof BlockEntityJS kbe
                && kbe.inventory instanceof InventoryAttachment attachment) {
            event.addCapability(KEY, new Provider(attachment));
        }
    }

    private static class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final LazyOptional<IItemHandler> handler;

        Provider(InventoryAttachment attachment) {
            this.handler = LazyOptional.of(() -> new FilteredInvWrapper(attachment));
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) {
                return handler.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return new CompoundTag(); // inventory data is persisted by the block entity itself
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
        }
    }

    /**
     * Container wrapper that respects the {@code inputFilter} of the inventory
     * attachment (raw container insertion would otherwise bypass it).
     */
    private static class FilteredInvWrapper extends InvWrapper {
        private final InventoryAttachment attachment;

        FilteredInvWrapper(InventoryAttachment attachment) {
            super(attachment);
            this.attachment = attachment;
        }

        private boolean allowed(ItemStack stack) {
            return attachment.inputFilter == null || attachment.inputFilter.test(stack);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!allowed(stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return allowed(stack) && super.isItemValid(slot, stack);
        }
    }
}
