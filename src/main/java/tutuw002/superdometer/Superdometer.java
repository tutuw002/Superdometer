package tutuw002.superdometer;

// import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


@Mod("superdometer")
public class Superdometer {
    // private static SuperdometerKeyBinding keyBinding;
    public static SuperdometerHud superdometerHud = new SuperdometerHud();
    protected static final Logger LOGGER = LogManager.getLogger("Squeedometer");
    
    /* @SubscribeEvent
    public void onRegister(FMLCommonSetupEvent event) {
        
    } */

    @SubscribeEvent
    public void postInit(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new SuperdometerHud());
    }

    
}

