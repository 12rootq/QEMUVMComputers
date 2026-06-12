package mcvmcomputers.item;

import java.util.Arrays;
import java.util.List;

import mcvmcomputers.entities.EntityCRTScreen;
import mcvmcomputers.entities.EntityFlatScreen;
import mcvmcomputers.entities.EntityKeyboard;
import mcvmcomputers.entities.EntityMouse;
import mcvmcomputers.entities.EntityWallTV;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Declares every mod item (PC parts, peripherals, screens, ordering tablet and
 * packages). Registration happens via Forge's RegisterEvent; creative tab groups
 * are built in registerGroups().
 */
public class ItemList {
    // ----- items -----
    public static final OrderableItem PC_CASE_SIDEPANEL = new ItemPCCaseSidepanel(new Settings());
    public static final OrderableItem ITEM_MOTHERBOARD = new OrderableItem(new Settings(), 4);
    public static final OrderableItem ITEM_MOTHERBOARD64 = new OrderableItem(new Settings(), 8);
    public static final OrderableItem ITEM_FLATSCREEN = new PlacableOrderableItem(new Settings(), EntityFlatScreen.class, SoundEvents.BLOCK_METAL_PLACE, 10);
    public static final OrderableItem ITEM_WALLTV = new PlacableOrderableItem(new Settings(), EntityWallTV.class, SoundEvents.BLOCK_METAL_PLACE, 14, true);
    public static final OrderableItem ITEM_CRTSCREEN = new PlacableOrderableItem(new Settings(), EntityCRTScreen.class, SoundEvents.BLOCK_METAL_PLACE, 10);
    public static final OrderableItem ITEM_HARDDRIVE = new ItemHarddrive(new Settings());
    public static final OrderableItem ITEM_KEYBOARD = new PlacableOrderableItem(new Settings(), EntityKeyboard.class, SoundEvents.BLOCK_METAL_PLACE, 4);
    public static final OrderableItem ITEM_MOUSE = new PlacableOrderableItem(new Settings(), EntityMouse.class, SoundEvents.BLOCK_METAL_PLACE, 4);
    public static final OrderableItem ITEM_RAM64M = new OrderableItem(new Settings(), 2);
    public static final OrderableItem ITEM_RAM128M = new OrderableItem(new Settings(), 2);
    public static final OrderableItem ITEM_RAM256M = new OrderableItem(new Settings(), 3);
    public static final OrderableItem ITEM_RAM512M = new OrderableItem(new Settings(), 4);
    public static final OrderableItem ITEM_RAM1G = new OrderableItem(new Settings(), 6);
    public static final OrderableItem ITEM_RAM2G = new OrderableItem(new Settings(), 8);
    public static final OrderableItem ITEM_RAM4G = new OrderableItem(new Settings(), 14);
    public static final OrderableItem ITEM_CPU2 = new OrderableItem(new Settings(), 10);
    public static final OrderableItem ITEM_CPU4 = new OrderableItem(new Settings(), 8);
    public static final OrderableItem ITEM_CPU6 = new OrderableItem(new Settings(), 6);
    public static final OrderableItem ITEM_GPU = new OrderableItem(new Settings(), 12);
    public static final Item ITEM_TABLET = new ItemOrderingTablet(new Settings().maxCount(1));
    public static final Item ITEM_PACKAGE = new ItemPackage(new Settings().rarity(Rarity.EPIC));
    public static final OrderableItem PC_CASE = new ItemPCCase(new Settings());
    public static final Item PC_CASE_NO_PANEL = new Item(new Settings().rarity(Rarity.EPIC));
    public static final Item PC_CASE_ONLY_PANEL = new Item(new Settings().rarity(Rarity.EPIC));
    public static final Item PC_CASE_GLASS_PANEL = new Item(new Settings().rarity(Rarity.EPIC));
    public static final OrderableItem ITEM_MOUSE_PAD = new OrderableItem(new Settings(), 3);

    public static final List<Item> PLACABLE_ITEMS = Arrays.asList(PC_CASE, PC_CASE_SIDEPANEL, ITEM_KEYBOARD, ITEM_MOUSE, ITEM_CRTSCREEN, ITEM_FLATSCREEN, ITEM_WALLTV);

    // ----- item groups (vanilla builder, no Fabric API) -----
    public static ItemGroup MOD_ITEM_GROUP_PARTS;
    public static ItemGroup MOD_ITEM_GROUP_PERIPHERALS;
    public static ItemGroup MOD_ITEM_GROUP_OTHERS;

    public static void registerItems(RegisterEvent.RegisterHelper<Item> helper) {
        reg(helper, "pc_case_sidepanel", PC_CASE_SIDEPANEL);
        reg(helper, "pc_case", PC_CASE);
        reg(helper, "motherboard", ITEM_MOTHERBOARD);
        reg(helper, "motherboard64", ITEM_MOTHERBOARD64);
        reg(helper, "walltv", ITEM_WALLTV);
        reg(helper, "flatscreen", ITEM_FLATSCREEN);
        reg(helper, "crtscreen", ITEM_CRTSCREEN);
        reg(helper, "harddrive", ITEM_HARDDRIVE);
        reg(helper, "keyboard", ITEM_KEYBOARD);
        reg(helper, "mouse", ITEM_MOUSE);
        reg(helper, "ram64m", ITEM_RAM64M);
        reg(helper, "ram128m", ITEM_RAM128M);
        reg(helper, "ram256m", ITEM_RAM256M);
        reg(helper, "ram512m", ITEM_RAM512M);
        reg(helper, "ram1g", ITEM_RAM1G);
        reg(helper, "ram2g", ITEM_RAM2G);
        reg(helper, "ram4g", ITEM_RAM4G);
        reg(helper, "cpu_divided_by_2", ITEM_CPU2);
        reg(helper, "cpu_divided_by_4", ITEM_CPU4);
        reg(helper, "cpu_divided_by_6", ITEM_CPU6);
        reg(helper, "gpu", ITEM_GPU);
        reg(helper, "ordering_tablet", ITEM_TABLET);
        reg(helper, "package", ITEM_PACKAGE);
        reg(helper, "pc_case_no_panel", PC_CASE_NO_PANEL);
        reg(helper, "pc_case_only_panel", PC_CASE_ONLY_PANEL);
        reg(helper, "pc_case_only_glass_sidepanel", PC_CASE_GLASS_PANEL);
        reg(helper, "mouse_pad", ITEM_MOUSE_PAD);
    }

    public static void registerGroups(RegisterEvent.RegisterHelper<ItemGroup> helper) {
        // Creative tabs — vanilla ItemGroup.Builder (same Yarn API, no Fabric wrapper needed)
        MOD_ITEM_GROUP_PARTS = ItemGroup.create(ItemGroup.Row.TOP, 0)
            .displayName(Text.translatable("itemGroup.mcvmcomputers.parts"))
            .icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
            .entries((ctx, e) -> {
                e.add(PC_CASE_SIDEPANEL); e.add(PC_CASE);
                e.add(ITEM_MOTHERBOARD); e.add(ITEM_MOTHERBOARD64);
                e.add(ITEM_HARDDRIVE);
                e.add(ITEM_RAM64M); e.add(ITEM_RAM128M); e.add(ITEM_RAM256M);
                e.add(ITEM_RAM512M); e.add(ITEM_RAM1G); e.add(ITEM_RAM2G); e.add(ITEM_RAM4G);
                e.add(ITEM_CPU2); e.add(ITEM_CPU4); e.add(ITEM_CPU6);
                e.add(ITEM_GPU);
            })
            .build();
        helper.register(new Identifier("mcvmcomputers", "parts"), MOD_ITEM_GROUP_PARTS);

        MOD_ITEM_GROUP_PERIPHERALS = ItemGroup.create(ItemGroup.Row.TOP, 1)
            .displayName(Text.translatable("itemGroup.mcvmcomputers.peripherals"))
            .icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
            .entries((ctx, e) -> {
                e.add(ITEM_FLATSCREEN); e.add(ITEM_WALLTV); e.add(ITEM_CRTSCREEN);
                e.add(ITEM_KEYBOARD); e.add(ITEM_MOUSE);
            })
            .build();
        helper.register(new Identifier("mcvmcomputers", "peripherals"), MOD_ITEM_GROUP_PERIPHERALS);

        MOD_ITEM_GROUP_OTHERS = ItemGroup.create(ItemGroup.Row.TOP, 2)
            .displayName(Text.translatable("itemGroup.mcvmcomputers.others"))
            .icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
            .entries((ctx, e) -> e.add(ITEM_TABLET))
            .build();
        helper.register(new Identifier("mcvmcomputers", "others"), MOD_ITEM_GROUP_OTHERS);
    }

    private static void reg(RegisterEvent.RegisterHelper<Item> helper, String id, Item item) {
        helper.register(new Identifier("mcvmcomputers", id), item);
    }
}
