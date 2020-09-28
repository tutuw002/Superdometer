package tutuw002.superdometer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class SuperdometerHud extends Screen 
{
    protected static final Logger LOGGER = LogManager.getLogger();
    private Minecraft client;
    private FontRenderer fontRenderer;

    //add these into a settings or something
    private int fontColor;
    private Coordinate margin;
    private Align align;

    public SuperdometerHud() 
    {
        super(new StringTextComponent("SpeedHud"));
        client = Minecraft.getInstance();
        fontRenderer = client.fontRenderer;
        fontColor = Integer.parseInt("FFFFFF", 16);
        margin = new Coordinate(4, 4);
        align = Align.BOTTOMRIGHT;

        LOGGER.debug("hud created");
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) 
    {
        String speedText = formatSpeed(getSpeed((Entity) client.player));
        Coordinate speedPos = getTextPos(speedText, align);

        drawString(fontRenderer, speedText, speedPos.X, speedPos.Y, fontColor);
    }

    private double getSpeed(Entity e)
    {
        return e.getDistanceSq(e.prevPosX, e.prevPosY, e.prevPosZ);
    }

    private String formatSpeed(double s)
    {
        return String.format("%.2f blocks/sec", s / 0.05F);
    }

    private Coordinate getTextPos(String t, Align a)
    {
        MainWindow mainWindow = client.getMainWindow();
        Coordinate screen = new Coordinate(mainWindow.getScaledWidth(),  mainWindow.getScaledHeight());
        Coordinate text = new Coordinate(fontRenderer.getStringWidth(t), fontRenderer.FONT_HEIGHT);

        switch(a)
        {//this probably can be much cleverer i too lazy think, u make
            case TOPRIGHT:
            return new Coordinate(screen.X - margin.X - text.X, 0 + margin.Y);
            case BOTTOMLEFT:
            return new Coordinate(0 + margin.X, screen.Y - margin.Y - text.Y);
            case BOTTOMRIGHT:
            return new Coordinate(screen.X - margin.X - text.X, screen.Y - margin.Y - text.Y);
            default: //TOPLEFT
            return new Coordinate(0 + margin.X, 0 + margin.Y);
          }
    }

    enum Align
    {
        TOPLEFT,
        TOPRIGHT,
        BOTTOMLEFT,
        BOTTOMRIGHT
    }

    public class Coordinate
    {
        public int X;
        public int Y;

        public Coordinate(int x, int y)
        {
            X = x;
            Y = y;
        }
    }
}