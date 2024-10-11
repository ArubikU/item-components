package com.boyonk.itemcomponents.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.boyonk.itemcomponents.BaseComponentSetter;
import com.boyonk.itemcomponents.ItemComponents;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements BaseComponentSetter {

	@Shadow
	@Final
	PatchedDataComponentMap components;

	@Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/core/component/PatchedDataComponentMap;)V", at = @At("RETURN"))
	void itemcomponents$storeStack(ItemLike item, int count, PatchedDataComponentMap components, CallbackInfo ci) {
		ItemComponents.store((ItemStack) (Object) this);
	}

	@Override
	public void itemcomponents$setBaseComponents(DataComponentMap baseComponents) {
		((BaseComponentSetter) (Object) this.components).itemcomponents$setBaseComponents(baseComponents);
	}

}
