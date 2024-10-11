package com.boyonk.itemcomponents;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.item.ItemStack;

public class ItemComponents {

	public static final String NAMESPACE = "item_components";
	public static final Logger LOGGER = LoggerFactory.getLogger("Item Components");

	public static final ItemComponentsManager MANAGER = new ItemComponentsManager();

	private static final Set<ItemStack> WEAK_STACKS = Collections
			.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	public static void store(ItemStack stack) {
		WEAK_STACKS.add(stack);
	}

	public static void forEachStack(Consumer<ItemStack> action) {
		synchronized (WEAK_STACKS) {
			WEAK_STACKS.forEach(action);
		}
	}

}
