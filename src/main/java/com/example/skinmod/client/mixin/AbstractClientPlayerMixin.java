package com.example.skinmod.client.mixin;

import com.example.skinmod.client.CustomTextureManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    private void skinmod$overrideSkinTexture(CallbackInfoReturnable<ResourceLocation> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        UUID playerUUID = self.getGameProfile().getId();

        if (CustomTextureManager.hasCustomSkin(playerUUID)) {
            ResourceLocation custom = CustomTextureManager.getCustomSkin(playerUUID);
            cir.setReturnValue(custom);
        }
    }

    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
    private void skinmod$overrideModelName(CallbackInfoReturnable<String> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        UUID playerUUID = self.getGameProfile().getId();

        String customModel = CustomTextureManager.PLAYER_MODELS.get(playerUUID);
        if (customModel != null) {
            cir.setReturnValue(customModel);
        }
    }
}
