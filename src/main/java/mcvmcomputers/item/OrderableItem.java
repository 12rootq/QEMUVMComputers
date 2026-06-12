package mcvmcomputers.item;

import net.minecraft.world.item.Item;

public class OrderableItem extends Item{
	private final int price;

	public OrderableItem(Item.Properties settings, int price) {
		super(settings);
		this.price = price;
	}


	public int getPrice() {
		return price;
	}

}
