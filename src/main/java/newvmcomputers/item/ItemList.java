package newvmcomputers.item;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import newvmcomputers.entities.EntityCRTScreen;
import newvmcomputers.entities.EntityFlatScreen;
import newvmcomputers.entities.EntityKeyboard;
import newvmcomputers.entities.EntityMouse;
import newvmcomputers.entities.EntityWallTV;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemList {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "newvmcomputers");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "newvmcomputers");

	public static final RegistryObject<Item> PC_CASE_SIDEPANEL = ITEMS.register("pc_case_sidepanel", () -> new ItemPCCaseSidepanel(new Item.Properties()));
	public static final RegistryObject<Item> ITEM_MOTHERBOARD = ITEMS.register("motherboard", () -> new OrderableItem(new Item.Properties(), 4));
	public static final RegistryObject<Item> ITEM_MOTHERBOARD64 = ITEMS.register("motherboard64", () -> new OrderableItem(new Item.Properties(), 8));
	public static final RegistryObject<Item> ITEM_FLATSCREEN = ITEMS.register("flatscreen", () -> new PlacableOrderableItem(new Item.Properties(), EntityFlatScreen.class, SoundEvents.METAL_PLACE, 10));
	public static final RegistryObject<Item> ITEM_WALLTV = ITEMS.register("walltv", () -> new PlacableOrderableItem(new Item.Properties(), EntityWallTV.class, SoundEvents.METAL_PLACE, 14, true));
	public static final RegistryObject<Item> ITEM_CRTSCREEN = ITEMS.register("crtscreen", () -> new PlacableOrderableItem(new Item.Properties(), EntityCRTScreen.class, SoundEvents.METAL_PLACE, 10));
	public static final RegistryObject<Item> ITEM_HARDDRIVE = ITEMS.register("harddrive", () -> new ItemHarddrive(new Item.Properties()));
	public static final RegistryObject<Item> ITEM_KEYBOARD = ITEMS.register("keyboard", () -> new PlacableOrderableItem(new Item.Properties(), EntityKeyboard.class, SoundEvents.METAL_PLACE, 4));
	public static final RegistryObject<Item> ITEM_MOUSE = ITEMS.register("mouse", () -> new PlacableOrderableItem(new Item.Properties(), EntityMouse.class, SoundEvents.METAL_PLACE, 4));
	public static final RegistryObject<Item> ITEM_RAM64M = ITEMS.register("ram64m", () -> new OrderableItem(new Item.Properties(), 2));
	public static final RegistryObject<Item> ITEM_RAM128M = ITEMS.register("ram128m", () -> new OrderableItem(new Item.Properties(), 2));
	public static final RegistryObject<Item> ITEM_RAM256M = ITEMS.register("ram256m", () -> new OrderableItem(new Item.Properties(), 3));
	public static final RegistryObject<Item> ITEM_RAM512M = ITEMS.register("ram512m", () -> new OrderableItem(new Item.Properties(), 4));
	public static final RegistryObject<Item> ITEM_RAM1G = ITEMS.register("ram1g", () -> new OrderableItem(new Item.Properties(), 6));
	public static final RegistryObject<Item> ITEM_RAM2G = ITEMS.register("ram2g", () -> new OrderableItem(new Item.Properties(), 8));
	public static final RegistryObject<Item> ITEM_RAM4G = ITEMS.register("ram4g", () -> new OrderableItem(new Item.Properties(), 14));
	public static final RegistryObject<Item> ITEM_CPU2 = ITEMS.register("cpu_divided_by_2", () -> new OrderableItem(new Item.Properties(), 10));
	public static final RegistryObject<Item> ITEM_CPU4 = ITEMS.register("cpu_divided_by_4", () -> new OrderableItem(new Item.Properties(), 8));
	public static final RegistryObject<Item> ITEM_CPU6 = ITEMS.register("cpu_divided_by_6", () -> new OrderableItem(new Item.Properties(), 6));
	public static final RegistryObject<Item> ITEM_GPU = ITEMS.register("gpu", () -> new OrderableItem(new Item.Properties(), 12));
	public static final RegistryObject<Item> ITEM_TABLET = ITEMS.register("ordering_tablet", () -> new ItemOrderingTablet(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> ITEM_PACKAGE = ITEMS.register("package", () -> new ItemPackage(new Item.Properties().rarity(Rarity.EPIC)));
	public static final RegistryObject<Item> PC_CASE = ITEMS.register("pc_case", () -> new ItemPCCase(new Item.Properties()));
	public static final RegistryObject<Item> PC_CASE_NO_PANEL = ITEMS.register("pc_case_no_panel", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
	public static final RegistryObject<Item> PC_CASE_ONLY_PANEL = ITEMS.register("pc_case_only_panel", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
	public static final RegistryObject<Item> PC_CASE_GLASS_PANEL = ITEMS.register("pc_case_only_glass_sidepanel", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));

	public static final RegistryObject<CreativeModeTab> MOD_ITEM_GROUP_PARTS = CREATIVE_MODE_TABS.register("parts",
			() -> CreativeModeTab.builder()
					.icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
					.title(Component.translatable("itemGroup.newvmcomputers.parts"))
					.displayItems((parameters, output) -> {
						output.accept(PC_CASE.get());
						output.accept(PC_CASE_SIDEPANEL.get());
						output.accept(ITEM_MOTHERBOARD.get());
						output.accept(ITEM_MOTHERBOARD64.get());
						output.accept(ITEM_HARDDRIVE.get());
						output.accept(ITEM_RAM64M.get());
						output.accept(ITEM_RAM128M.get());
						output.accept(ITEM_RAM256M.get());
						output.accept(ITEM_RAM512M.get());
						output.accept(ITEM_RAM1G.get());
						output.accept(ITEM_RAM2G.get());
						output.accept(ITEM_RAM4G.get());
						output.accept(ITEM_CPU2.get());
						output.accept(ITEM_CPU4.get());
						output.accept(ITEM_CPU6.get());
						output.accept(ITEM_GPU.get());
					}).build());

	public static final RegistryObject<CreativeModeTab> MOD_ITEM_GROUP_PERIPHERALS = CREATIVE_MODE_TABS.register("peripherals",
			() -> CreativeModeTab.builder()
					.icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
					.title(Component.translatable("itemGroup.newvmcomputers.peripherals"))
					.displayItems((parameters, output) -> {
						output.accept(ITEM_FLATSCREEN.get());
						output.accept(ITEM_WALLTV.get());
						output.accept(ITEM_CRTSCREEN.get());
						output.accept(ITEM_KEYBOARD.get());
						output.accept(ITEM_MOUSE.get());
					}).build());

	public static final RegistryObject<CreativeModeTab> MOD_ITEM_GROUP_OTHERS = CREATIVE_MODE_TABS.register("others",
			() -> CreativeModeTab.builder()
					.icon(() -> new ItemStack(Blocks.WHITE_STAINED_GLASS))
					.title(Component.translatable("itemGroup.newvmcomputers.others"))
					.displayItems((parameters, output) -> {
						output.accept(ITEM_TABLET.get());
						output.accept(ITEM_PACKAGE.get());
					}).build());

	public static final Supplier<List<Item>> PLACABLE_ITEMS = () -> Arrays.asList(
			PC_CASE.get(),
			PC_CASE_SIDEPANEL.get(),
			ITEM_KEYBOARD.get(),
			ITEM_MOUSE.get(),
			ITEM_CRTSCREEN.get(),
			ITEM_FLATSCREEN.get(),
			ITEM_WALLTV.get()
	);

	public static void init(net.minecraftforge.eventbus.api.IEventBus bus) {
		ITEMS.register(bus);
		CREATIVE_MODE_TABS.register(bus);
	}
}