package mcvmcomputers.item;

import mcvmcomputers.entities.EntityCRTScreen;
import mcvmcomputers.entities.EntityFlatScreen;
import mcvmcomputers.entities.EntityKeyboard;
import mcvmcomputers.entities.EntityMouse;
import mcvmcomputers.entities.EntityWallTV;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration of all mod items and creative tabs.
 *
 * <p>Uses the NeoForge {@link DeferredRegister}: {@link #init(IEventBus)} is called
 * from {@link mcvmcomputers.MainMod} and registers components (motherboards, RAM,
 * CPUs, GPU), peripherals (screens, keyboard, mouse), PC cases, the ordering tablet
 * and three creative tabs. The static {@code Item} fields are populated on
 * registration and used throughout the mod.</p>
 */
/**
 * Declares and registers every item in the mod (PC parts, peripherals, screens,
 * the ordering tablet and packages) and builds the three creative-tab item groups.
 */
public class ItemList {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "mcvmcomputers");
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "mcvmcomputers");

    public static Item PC_CASE_SIDEPANEL;
    public static Item ITEM_MOTHERBOARD;
    public static Item ITEM_MOTHERBOARD64;
    public static Item ITEM_FLATSCREEN;
    public static Item ITEM_WALLTV;
    public static Item ITEM_CRTSCREEN;
    public static Item ITEM_HARDDRIVE;
    public static Item ITEM_KEYBOARD;
    public static Item ITEM_MOUSE;
    public static Item ITEM_RAM64M;
    public static Item ITEM_RAM128M;
    public static Item ITEM_RAM256M;
    public static Item ITEM_RAM512M;
    public static Item ITEM_RAM1G;
    public static Item ITEM_RAM2G;
    public static Item ITEM_RAM4G;
    public static Item ITEM_CPU2;
    public static Item ITEM_CPU4;
    public static Item ITEM_CPU6;
    public static Item ITEM_GPU;
    public static Item ITEM_TABLET;
    public static Item ITEM_PACKAGE;
    public static Item PC_CASE;
    public static Item PC_CASE_NO_PANEL;
    public static Item PC_CASE_ONLY_PANEL;
    public static Item PC_CASE_GLASS_PANEL;

    /**
     * Returns {@code true} if the item can be "placed" in the world as an entity
     * (case, side panel, keyboard, mouse, screens). Used by the placement preview
     * logic in {@code GameloopMixin}.
     */
    public static boolean isPlacableItem(Item item) {
        return item == PC_CASE
            || item == PC_CASE_SIDEPANEL
            || item == ITEM_KEYBOARD
            || item == ITEM_MOUSE
            || item == ITEM_CRTSCREEN
            || item == ITEM_FLATSCREEN
            || item == ITEM_WALLTV;
    }

    public static void init(IEventBus modEventBus) {
        ITEMS.register("pc_case_sidepanel", () -> PC_CASE_SIDEPANEL = new ItemPCCaseSidepanel(new Item.Properties()));
        ITEMS.register("pc_case", () -> PC_CASE = new ItemPCCase(new Item.Properties()));
        ITEMS.register("motherboard", () -> ITEM_MOTHERBOARD = new OrderableItem(new Item.Properties(), 4));
        ITEMS.register("motherboard64", () -> ITEM_MOTHERBOARD64 = new OrderableItem(new Item.Properties(), 8));
        ITEMS.register("walltv", () -> ITEM_WALLTV = new PlacableOrderableItem(new Item.Properties(), EntityWallTV.class, SoundEvents.METAL_PLACE, 14, true));
        ITEMS.register("flatscreen", () -> ITEM_FLATSCREEN = new PlacableOrderableItem(new Item.Properties(), EntityFlatScreen.class, SoundEvents.METAL_PLACE, 10));
        ITEMS.register("crtscreen", () -> ITEM_CRTSCREEN = new PlacableOrderableItem(new Item.Properties(), EntityCRTScreen.class, SoundEvents.METAL_PLACE, 10));
        ITEMS.register("harddrive", () -> ITEM_HARDDRIVE = new ItemHarddrive(new Item.Properties()));
        ITEMS.register("keyboard", () -> ITEM_KEYBOARD = new PlacableOrderableItem(new Item.Properties(), EntityKeyboard.class, SoundEvents.METAL_PLACE, 4));
        ITEMS.register("mouse", () -> ITEM_MOUSE = new PlacableOrderableItem(new Item.Properties(), EntityMouse.class, SoundEvents.METAL_PLACE, 4));
        ITEMS.register("ram64m", () -> ITEM_RAM64M = new OrderableItem(new Item.Properties(), 2));
        ITEMS.register("ram128m", () -> ITEM_RAM128M = new OrderableItem(new Item.Properties(), 2));
        ITEMS.register("ram256m", () -> ITEM_RAM256M = new OrderableItem(new Item.Properties(), 3));
        ITEMS.register("ram512m", () -> ITEM_RAM512M = new OrderableItem(new Item.Properties(), 4));
        ITEMS.register("ram1g", () -> ITEM_RAM1G = new OrderableItem(new Item.Properties(), 6));
        ITEMS.register("ram2g", () -> ITEM_RAM2G = new OrderableItem(new Item.Properties(), 8));
        ITEMS.register("ram4g", () -> ITEM_RAM4G = new OrderableItem(new Item.Properties(), 14));
        ITEMS.register("cpu_divided_by_2", () -> ITEM_CPU2 = new OrderableItem(new Item.Properties(), 10));
        ITEMS.register("cpu_divided_by_4", () -> ITEM_CPU4 = new OrderableItem(new Item.Properties(), 8));
        ITEMS.register("cpu_divided_by_6", () -> ITEM_CPU6 = new OrderableItem(new Item.Properties(), 6));
        ITEMS.register("gpu", () -> ITEM_GPU = new OrderableItem(new Item.Properties(), 12));
        ITEMS.register("ordering_tablet", () -> ITEM_TABLET = new ItemOrderingTablet(new Item.Properties().stacksTo(1)));
        ITEMS.register("package", () -> ITEM_PACKAGE = new ItemPackage(new Item.Properties().rarity(Rarity.EPIC)));
        ITEMS.register("pc_case_no_panel", () -> PC_CASE_NO_PANEL = new Item(new Item.Properties().rarity(Rarity.EPIC)));
        ITEMS.register("pc_case_only_panel", () -> PC_CASE_ONLY_PANEL = new Item(new Item.Properties().rarity(Rarity.EPIC)));
        ITEMS.register("pc_case_only_glass_sidepanel", () -> PC_CASE_GLASS_PANEL = new Item(new Item.Properties().rarity(Rarity.EPIC)));

        CREATIVE_TABS.register("parts", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mcvmcomputers.parts"))
                .icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
                .displayItems((params, output) -> {
                    output.accept(PC_CASE_SIDEPANEL);
                    output.accept(PC_CASE);
                    output.accept(ITEM_MOTHERBOARD);
                    output.accept(ITEM_MOTHERBOARD64);
                    output.accept(ITEM_HARDDRIVE);
                    output.accept(ITEM_RAM64M);
                    output.accept(ITEM_RAM128M);
                    output.accept(ITEM_RAM256M);
                    output.accept(ITEM_RAM512M);
                    output.accept(ITEM_RAM1G);
                    output.accept(ITEM_RAM2G);
                    output.accept(ITEM_RAM4G);
                    output.accept(ITEM_CPU2);
                    output.accept(ITEM_CPU4);
                    output.accept(ITEM_CPU6);
                    output.accept(ITEM_GPU);
                }).build());
        CREATIVE_TABS.register("peripherals", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mcvmcomputers.peripherals"))
                .icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
                .displayItems((params, output) -> {
                    output.accept(ITEM_FLATSCREEN);
                    output.accept(ITEM_WALLTV);
                    output.accept(ITEM_CRTSCREEN);
                    output.accept(ITEM_KEYBOARD);
                    output.accept(ITEM_MOUSE);
                }).build());
        CREATIVE_TABS.register("others", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mcvmcomputers.others"))
                .icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
                .displayItems((params, output) -> {
                    output.accept(ITEM_TABLET);
                }).build());

        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
