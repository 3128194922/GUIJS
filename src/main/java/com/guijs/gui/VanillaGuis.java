package com.guijs.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Builds providers for all vanilla menu types. Menus validated by
 * {@link net.minecraft.world.inventory.ContainerLevelAccess} use {@link OpenAccess}
 * so they can be opened "floating" without a real block; container menus get a
 * transient {@link SimpleContainer}.
 *
 * <p>Binding to a real block (pos) is handled by {@link GuiOpener}, which prefers
 * the block entity's own {@link net.minecraft.world.MenuProvider} for full
 * functionality; this class is the fallback for floating GUIs.</p>
 */
public final class VanillaGuis {
    private VanillaGuis() {
    }

    public static final List<String> NAMES = List.of(
            "chest", "crafting", "furnace", "blast_furnace", "smoker",
            "anvil", "smithing", "enchanting_table", "brewing_stand",
            "dispenser", "hopper", "shulker_box", "beacon", "lectern",
            "loom", "stonecutter", "grindstone", "cartography_table", "merchant"
    );

    public static boolean isVanillaGui(String name) {
        return name != null && NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    public static Component defaultTitle(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "chest" -> Component.translatable("container.chest");
            case "crafting" -> Component.translatable("container.crafting");
            case "furnace" -> Component.translatable("container.furnace");
            case "blast_furnace" -> Component.translatable("container.blast_furnace");
            case "smoker" -> Component.translatable("container.smoker");
            case "anvil" -> Component.translatable("container.repair");
            case "smithing" -> Component.translatable("container.upgrade");
            case "enchanting_table" -> Component.translatable("container.enchant");
            case "brewing_stand" -> Component.translatable("container.brewing");
            case "dispenser" -> Component.translatable("container.dispenser");
            case "hopper" -> Component.translatable("container.hopper");
            case "shulker_box" -> Component.translatable("container.shulkerBox");
            case "beacon" -> Component.translatable("container.beacon");
            case "lectern" -> Component.translatable("container.lectern");
            case "loom" -> Component.translatable("container.loom");
            case "stonecutter" -> Component.translatable("container.stonecutter");
            case "grindstone" -> Component.translatable("container.grindstone_title");
            case "cartography_table" -> Component.translatable("container.cartography_table");
            case "merchant" -> Component.translatable("merchant.trades");
            default -> Component.literal(name);
        };
    }

    /** Builds a floating vanilla menu provider; {@code pos} may refine validation context. */
    public static SimpleMenuProvider createProvider(String name, Level level, Inventory inventory, BlockPos pos, Component title) {
        String key = name.toLowerCase(Locale.ROOT);
        boolean bound = pos != null;
        BlockPos target = bound ? pos : inventory.player.blockPosition();

        MenuConstructor constructor = switch (key) {
            case "chest" -> (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            case "crafting" -> (id, inv, p) -> new CraftingMenu(id, inv, access(level, target, bound, CraftingTableBlock.class));
            case "furnace" -> (id, inv, p) -> new FurnaceMenu(id, inv, new SimpleContainer(3), new SimpleContainerData(4));
            case "blast_furnace" -> (id, inv, p) -> new BlastFurnaceMenu(id, inv, new SimpleContainer(3), new SimpleContainerData(4));
            case "smoker" -> (id, inv, p) -> new SmokerMenu(id, inv, new SimpleContainer(3), new SimpleContainerData(4));
            case "anvil" -> (id, inv, p) -> new AnvilMenu(id, inv, access(level, target, bound, AnvilBlock.class));
            case "smithing" -> (id, inv, p) -> new SmithingMenu(id, inv, access(level, target, bound, SmithingTableBlock.class));
            case "enchanting_table" -> (id, inv, p) -> new EnchantmentMenu(id, inv, access(level, target, bound, EnchantmentTableBlock.class));
            case "brewing_stand" -> (id, inv, p) -> new BrewingStandMenu(id, inv, new SimpleContainer(5), new SimpleContainerData(2));
            case "dispenser" -> (id, inv, p) -> new DispenserMenu(id, inv, new SimpleContainer(9));
            case "hopper" -> (id, inv, p) -> new HopperMenu(id, inv, new SimpleContainer(5));
            case "shulker_box" -> (id, inv, p) -> new ShulkerBoxMenu(id, inv, new SimpleContainer(27));
            case "beacon" -> (id, inv, p) -> new BeaconMenu(id, new SimpleContainer(1), new SimpleContainerData(3), access(level, target, bound, BeaconBlock.class));
            case "lectern" -> (id, inv, p) -> new LecternMenu(id, new SimpleContainer(1), new SimpleContainerData(1));
            case "loom" -> (id, inv, p) -> new LoomMenu(id, inv, access(level, target, bound, LoomBlock.class));
            case "stonecutter" -> (id, inv, p) -> new StonecutterMenu(id, inv, access(level, target, bound, StonecutterBlock.class));
            case "grindstone" -> (id, inv, p) -> new GrindstoneMenu(id, inv, access(level, target, bound, GrindstoneBlock.class));
            case "cartography_table" -> (id, inv, p) -> new CartographyTableMenu(id, inv, access(level, target, bound, CartographyTableBlock.class));
            case "merchant" -> (id, inv, p) -> new MerchantMenu(id, inv, merchant(level, pos, bound, inv));
            default -> null;
        };
        return constructor == null ? null : new SimpleMenuProvider(constructor, title);
    }

    /** True if pos holds the block class this vanilla menu normally validates against. */
    public static boolean blockMatches(String name, Level level, BlockPos pos) {
        if (pos == null) {
            return false;
        }
        Class<? extends net.minecraft.world.level.block.Block> cls = switch (name.toLowerCase(Locale.ROOT)) {
            case "crafting" -> CraftingTableBlock.class;
            case "furnace" -> FurnaceBlock.class;
            case "blast_furnace" -> BlastFurnaceBlock.class;
            case "smoker" -> SmokerBlock.class;
            case "anvil" -> AnvilBlock.class;
            case "smithing" -> SmithingTableBlock.class;
            case "enchanting_table" -> EnchantmentTableBlock.class;
            case "brewing_stand" -> BrewingStandBlock.class;
            case "shulker_box" -> ShulkerBoxBlock.class;
            case "beacon" -> BeaconBlock.class;
            case "lectern" -> LecternBlock.class;
            case "loom" -> LoomBlock.class;
            case "stonecutter" -> StonecutterBlock.class;
            case "grindstone" -> GrindstoneBlock.class;
            case "cartography_table" -> CartographyTableBlock.class;
            default -> null;
        };
        return cls != null && cls.isInstance(level.getBlockState(pos).getBlock());
    }

    private static net.minecraft.world.inventory.ContainerLevelAccess access(Level level, BlockPos pos, boolean bound, Class<? extends net.minecraft.world.level.block.Block> blockClass) {
        boolean real = bound && blockClass.isInstance(level.getBlockState(pos).getBlock());
        return new OpenAccess(level, pos, !real);
    }

    private static Merchant merchant(Level level, BlockPos pos, boolean bound, Inventory inventory) {
        if (bound) {
            AbstractVillager nearest = level.getEntitiesOfClass(AbstractVillager.class,
                            AABB.ofSize(pos.getCenter(), 8.0, 4.0, 8.0), AbstractVillager::isAlive).stream()
                    .min(Comparator.comparingDouble(v -> v.distanceToSqr(pos.getCenter())))
                    .orElse(null);
            if (nearest != null) {
                return nearest;
            }
        }
        return new Merchant() {
            @Override public void setTradingPlayer(net.minecraft.world.entity.player.Player player) {
            }

            @Override public net.minecraft.world.entity.player.Player getTradingPlayer() {
                return inventory.player;
            }

            @Override public MerchantOffers getOffers() {
                return new MerchantOffers();
            }

            @Override public void overrideOffers(MerchantOffers offers) {
            }

            @Override public void notifyTrade(MerchantOffer offer) {
            }

            @Override public void notifyTradeUpdated(ItemStack stack) {
            }

            @Override public int getVillagerXp() {
                return 0;
            }

            @Override public void overrideXp(int xp) {
            }

            @Override public boolean showProgressBar() {
                return false;
            }

            @Override public net.minecraft.sounds.SoundEvent getNotifyTradeSound() {
                return SoundEvents.VILLAGER_YES;
            }

            @Override public boolean isClientSide() {
                return level.isClientSide();
            }
        };
    }

    /** Real chest menu with an explicit row count (1-6), backed by a transient container. */
    public static SimpleMenuProvider chestRows(int rows, Component title) {
        int r = Mth.clamp(rows, 1, 6);
        var container = new SimpleContainer(9 * r);
        return new SimpleMenuProvider((id, inv, p) -> new ChestMenu(menuRows(r), id, inv, container, r), title);
    }

    private static MenuType<ChestMenu> menuRows(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }
}
