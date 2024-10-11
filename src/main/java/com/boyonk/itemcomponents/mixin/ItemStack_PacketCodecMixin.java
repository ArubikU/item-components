package com.boyonk.itemcomponents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.boyonk.itemcomponents.ItemComponents;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStack.class)
public class ItemStack_PacketCodecMixin {

    @Inject(method = "encode(Lnet/minecraft/network/RegistryFriendlyByteBuf;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/component/PatchedDataComponentMap;asPatch()Lnet/minecraft/core/component/DataComponentPatch;"), cancellable = true)
    private DataComponentPatch defaultitemcomponents$sendExtraChanges(RegistryFriendlyByteBuf buf, ItemStack stack,
            CallbackInfo ci) {
        DataComponentPatch original = getOriginalDataComponentPatch(stack);
        DataComponentPatch base = ItemComponents.MANAGER.getChanges(stack.getItem());

        if (base.isEmpty()) {
            return original; // No extra changes
        }
        if (original.isEmpty()) {
            return base;
        }

        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        combineChanges(original, base, builder);
        ci.cancel(); // Cancel original
        buf.writeVarInt(builder.build().size());
        return builder.build();
    }

    private DataComponentPatch getOriginalDataComponentPatch(ItemStack stack) {
        // Placeholder method to retrieve original component changes
        return DataComponentPatch.EMPTY; // Replace with actual retrieval logic if needed
    }

    private void combineChanges(DataComponentPatch original, DataComponentPatch base,
            DataComponentPatch.Builder builder) {
        // Combine original and base changes
        original.split().added().forEach(builder::set);
        original.split().removed().forEach(builder::remove);
        base.split().added().forEach(builder::set);
        base.split().removed().forEach(builder::remove);
    }
}
