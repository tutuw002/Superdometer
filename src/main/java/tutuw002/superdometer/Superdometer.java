package tutuw002.superdometer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


@Mod(Superdometer.MODID)
public final class Superdometer 
{
    public static final String MODID = "superdometer";
	private static final Logger LOGGER = LogManager.getLogger();

    public Superdometer() 
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.debug("registered");
	}

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) 
    {
        //add your common setup stuff here
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) 
    {
        MinecraftForge.EVENT_BUS.register(new SuperdometerHud());
    }
}

