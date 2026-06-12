package mcvmcomputers.item;

import net.minecraft.item.Item;

/**
 * Base class for any item that can be bought from the ordering tablet. Carries
 * the item's price in iron ingots.
 */
public class OrderableItem extends Item{
	private final int price;

	public OrderableItem(Settings settings, int price) {
		super(settings);
		this.price = price;
	}


	public int getPrice() {
		return price;
	}

}
