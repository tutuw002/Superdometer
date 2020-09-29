package tutuw002.superdometer;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class KeyHandler
{
    public KeyBinding cycleAlign;
    public KeyBinding cycleUnit;

    public KeyHandler()
    {
        register();
    }

    @SubscribeEvent
    public void register()    
    {
        cycleAlign = new KeyBinding("superdometer.key.cyclealign", GLFW.GLFW_KEY_P, "key.categories.superdometer");
        cycleUnit = new KeyBinding("superdometer.key.cycleunit", GLFW.GLFW_KEY_O, "key.categories.superdometer");

        ClientRegistry.registerKeyBinding(cycleAlign);
        ClientRegistry.registerKeyBinding(cycleUnit);
    }
}