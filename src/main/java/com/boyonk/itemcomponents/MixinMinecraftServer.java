package com.boyonk.itemcomponents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void onServerStart(CallbackInfo info) {
        ItemComponents.LOGGER.info("Server started, initializing components...");
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void onServerStop(CallbackInfo info) {
        ItemComponents.LOGGER.info("Server stopping, cleaning up components...");
        ItemComponents.MANAGER.clear();
    }

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void onResourcesReload(CallbackInfo ci) {
        ItemComponentsManager manager = new ItemComponentsManager();
        manager.reload(MinecraftServer.getServer().getResourceManager());
        ItemComponents.LOGGER.info("Resources reloaded, components loaded.");
    }
}