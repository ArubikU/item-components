package com.boyonk.itemcomponents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.boyonk.itemcomponents.ItemComponents;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;

@Mixin(Item.class)
public class ItemMixin {

	@ModifyReturnValue(method = "components", at = @At("RETURN"))
	public DataComponentMap itemcomponents$getComponents(DataComponentMap original) {
		return ItemComponents.MANAGER.getMap((Item) (Object) this, original);

	}

	@ModifyExpressionValue(method = "getDefaultMaxStackSize", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Item;components:Lnet/minecraft/core/component/DataComponentMap;"))
	public DataComponentMap itemcomponents$getComponentsForMaxCount(DataComponentMap original) {
		return ItemComponents.MANAGER.getMap((Item) (Object) this, original);
	}
}
