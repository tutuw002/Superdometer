package tutuw002.superdometer;

// import com.mojang.blaze3d.systems.RenderSystem;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SuperdometerHud extends Screen {
    public SuperdometerHud() {
        super(new StringTextComponent("SpeedHud"));
    }

    protected static final Logger LOGGER = LogManager.getLogger();
    private Minecraft client;
    private FontRenderer fontRenderer;

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        this.client = Minecraft.getInstance();
        this.fontRenderer = client.fontRenderer;

        Vec3d playerPosVec = client.player.getPositionVec();
        double travelledX = playerPosVec.x - client.player.prevPosX;
        double travelledZ = playerPosVec.z - client.player.prevPosZ;
        double currentSpeed = MathHelper.sqrt(travelledX * travelledX + travelledZ * travelledZ);

        String currentSpeedText = String.format("%.2f blocks/sec", currentSpeed / 0.05F);

        int width = this.fontRenderer.getStringWidth(currentSpeedText);
        int height = this.fontRenderer.FONT_HEIGHT;
        int paddingX = 2;
        int paddingY = 2;
        int marginX = 4;
        int marginY = 4;
        int left = 0 + marginX;
        int top = 0 + marginY;
        int realHeight = height + paddingY * 2 - 1;

        top += client.getMainWindow().getScaledHeight() - marginY * 2 - realHeight;

        left += paddingX;
        top += paddingY;

        fontRenderer.drawString(currentSpeedText, left, top, 14737632);

        return;
    }

    /* @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        draw(event);
    } */
}