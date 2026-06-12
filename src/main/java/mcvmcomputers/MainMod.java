package mcvmcomputers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mcvmcomputers.entities.EntityList;
import mcvmcomputers.entities.EntityPC;
import mcvmcomputers.item.ItemList;
import mcvmcomputers.sound.SoundList;
import mcvmcomputers.networking.PacketList;
import mcvmcomputers.utils.TabletOrder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.Optional;

/**
 * Main mod entry point (annotated with {@link Mod}, id "mcvmcomputers").
 *
 * <p>The constructor is invoked by NeoForge during mod loading and is responsible
 * for registering all content: items ({@link ItemList}), entities ({@link EntityList}),
 * sounds ({@link SoundList}) and network packets ({@link PacketList}). It also
 * dynamically adds the tablet crafting recipe when the server starts.</p>
 *
 * <p>The {@link Runnable} fields ({@code hardDriveClick}, {@code focus}, etc.) are
 * hooks from the server/common logic into the client: their implementations are
 * supplied by {@code ClientMod}, so common code can open GUIs and play sounds
 * without depending directly on client-only classes.</p>
 */
@Mod("mcvmcomputers")
/**
 * NeoForge mod entry point. Configures the deferred registers for items,
 * entities, sounds and creative tabs, registers server-side packet handlers
 * and holds the global state shared between sides (active orders and computers).
 */
public class MainMod {
    /** Active tablet orders keyed by player UUID (server side). */
    public static Map<UUID, TabletOrder> orders;
    /** Registered PC entities keyed by owner UUID. */
    public static Map<UUID, EntityPC> computers;

    // Client callbacks. Empty by default; the real implementations are installed
    // by ClientMod when the renderers are initialized.
    public static Runnable hardDriveClick = () -> {};
    public static Runnable deliveryChestSound = () -> {};
    public static Runnable focus = () -> {};
    public static Runnable pcOpenGui = () -> {};

    public MainMod(IEventBus modEventBus) {
        orders = new HashMap<>();
        computers = new HashMap<>();
        // Register the mod's content on the mod event bus.
        ItemList.init(modEventBus);
        EntityList.init(modEventBus);
        SoundList.init(modEventBus);

        // Register network packets (client<->server) on channel version "1".
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar("1");
            PacketList.registerPackets(registrar);
        });

        // On server start, add the tablet crafting recipe (3x3) to the RecipeManager.
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            var recipes = event.getServer().getRecipeManager();
            var ingredients = net.minecraft.core.NonNullList.<Ingredient>create();
            ingredients.add(Ingredient.of(Items.OBSIDIAN));
            ingredients.add(Ingredient.of(Items.DAYLIGHT_DETECTOR));
            ingredients.add(Ingredient.of(Items.OBSIDIAN));
            ingredients.add(Ingredient.of(Items.CLOCK));
            ingredients.add(Ingredient.of(Items.GLASS_PANE));
            ingredients.add(Ingredient.of(Items.COMPASS));
            ingredients.add(Ingredient.of(Items.OBSIDIAN));
            ingredients.add(Ingredient.of(Items.STONE_BUTTON));
            ingredients.add(Ingredient.of(Items.OBSIDIAN));
            ShapedRecipePattern pattern = new ShapedRecipePattern(3, 3, ingredients, Optional.empty());
            ShapedRecipe recipe = new ShapedRecipe("", CraftingBookCategory.MISC, pattern,
                new net.minecraft.world.item.ItemStack(ItemList.ITEM_TABLET, 1));
            var all = new java.util.ArrayList<>(recipes.getRecipes());
            all.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("mcvmcomputers", "tablet_recipe"), recipe));
            recipes.replaceRecipes(all);
        });
    }
}
