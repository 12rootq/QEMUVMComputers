package mcvmcomputers.item;

import java.util.Arrays;
import java.util.List;

import mcvmcomputers.entities.EntityCRTScreen;
import mcvmcomputers.entities.EntityFlatScreen;
import mcvmcomputers.entities.EntityKeyboard;
import mcvmcomputers.entities.EntityMouse;
import mcvmcomputers.entities.EntityWallTV;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ItemList {

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

	public static final ItemGroup MOD_ITEM_GROUP_PARTS = Registry.register(
			Registries.ITEM_GROUP,
			new Identifier("mcvmcomputers", "parts"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
					.displayName(Text.translatable("itemGroup.mcvmcomputers.parts"))
					.entries((context, entries) -> {
						entries.add(PC_CASE);
						entries.add(PC_CASE_SIDEPANEL);
						entries.add(ITEM_MOTHERBOARD);
						entries.add(ITEM_MOTHERBOARD64);
						entries.add(ITEM_HARDDRIVE);
						entries.add(ITEM_RAM64M);
						entries.add(ITEM_RAM128M);
						entries.add(ITEM_RAM256M);
						entries.add(ITEM_RAM512M);
						entries.add(ITEM_RAM1G);
						entries.add(ITEM_RAM2G);
						entries.add(ITEM_RAM4G);
						entries.add(ITEM_CPU2);
						entries.add(ITEM_CPU4);
						entries.add(ITEM_CPU6);
						entries.add(ITEM_GPU);
					})
					.build()
	);

	public static final ItemGroup MOD_ITEM_GROUP_PERIPHERALS = Registry.register(
			Registries.ITEM_GROUP,
			new Identifier("mcvmcomputers", "peripherals"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
					.displayName(Text.translatable("itemGroup.mcvmcomputers.peripherals"))
					.entries((context, entries) -> {
						entries.add(ITEM_FLATSCREEN);
						entries.add(ITEM_WALLTV);
						entries.add(ITEM_CRTSCREEN);
						entries.add(ITEM_KEYBOARD);
						entries.add(ITEM_MOUSE);
					})
					.build()
	);

	public static final ItemGroup MOD_ITEM_GROUP_OTHERS = Registry.register(
			Registries.ITEM_GROUP,
			new Identifier("mcvmcomputers", "others"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
					.displayName(Text.translatable("itemGroup.mcvmcomputers.others"))
					.entries((context, entries) -> {
						entries.add(ITEM_TABLET);
						entries.add(ITEM_PACKAGE);
					})
					.build()
	);

	public static final List<Item> PLACABLE_ITEMS = Arrays.asList(
			PC_CASE,
			PC_CASE_SIDEPANEL,
			ITEM_KEYBOARD,
			ITEM_MOUSE,
			ITEM_CRTSCREEN,
			ITEM_FLATSCREEN,
			ITEM_WALLTV
	);

	public static void init() {
		registerItem("pc_case_sidepanel", PC_CASE_SIDEPANEL);
		registerItem("pc_case", PC_CASE);
		registerItem("motherboard", ITEM_MOTHERBOARD);
		registerItem("motherboard64", ITEM_MOTHERBOARD64);
		registerItem("walltv", ITEM_WALLTV);
		registerItem("flatscreen", ITEM_FLATSCREEN);
		registerItem("crtscreen", ITEM_CRTSCREEN);
		registerItem("harddrive", ITEM_HARDDRIVE);
		registerItem("keyboard", ITEM_KEYBOARD);
		registerItem("mouse", ITEM_MOUSE);
		registerItem("ram64m", ITEM_RAM64M);
		registerItem("ram128m", ITEM_RAM128M);
		registerItem("ram256m", ITEM_RAM256M);
		registerItem("ram512m", ITEM_RAM512M);
		registerItem("ram1g", ITEM_RAM1G);
		registerItem("ram2g", ITEM_RAM2G);
		registerItem("ram4g", ITEM_RAM4G);
		registerItem("cpu_divided_by_2", ITEM_CPU2);
		registerItem("cpu_divided_by_4", ITEM_CPU4);
		registerItem("cpu_divided_by_6", ITEM_CPU6);
		registerItem("gpu", ITEM_GPU);
		registerItem("ordering_tablet", ITEM_TABLET);
		registerItem("package", ITEM_PACKAGE);

		// Visual items - used for rendering only!
		registerItem("pc_case_no_panel", PC_CASE_NO_PANEL);
		registerItem("pc_case_only_panel", PC_CASE_ONLY_PANEL);
		registerItem("pc_case_only_glass_sidepanel", PC_CASE_GLASS_PANEL);
	}

	private static Item registerItem(String id, Item it) {
		Registry.register(Registries.ITEM, new Identifier("mcvmcomputers", id), it);
		return it;
	}
}