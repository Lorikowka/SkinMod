package com.example.skinmod;

import com.example.skinmod.command.SetTextureCommand;
import com.example.skinmod.network.ModNetwork;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SkinMod.MODID)
public class SkinMod {

    public static final String MODID = "skinmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public SkinMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        ServerSkinManager.load();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SetTextureCommand.register(event.getDispatcher());
    }
}
