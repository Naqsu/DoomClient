package doom.module.impl.render;

import doom.module.Category;
import doom.module.DraggableModule;
import doom.util.MoveUtil;
import doom.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import java.awt.Color;

public class InfoHUD extends DraggableModule {

    public InfoHUD() {
        super("InfoHUD", Category.RENDER);
        this.hidden = true;
        this.x = 2;
        this.y = 200;
    }

    @Override
    public float getWidth() {
        return mc.fontRendererObj.getStringWidth(getInfoString()) + 8;
    }

    @Override
    public float getHeight() {
        return 16;
    }

    @Override
    public void render(float x, float y) {
        String text = getInfoString();
        RenderUtil.drawRoundedRect(x, y, getWidth(), getHeight(), 4, new Color(20, 20, 20, 200).getRGB());
        mc.fontRendererObj.drawStringWithShadow(text, x + 4, y + 4, -1);
    }

    private String getInfoString() {
        // 1. FPS
        String fps = "\u00A7fFPS: \u00A77" + Minecraft.getDebugFPS();

        // 2. BPS - PROFESJONALNE OBLICZENIA

        // A. Obliczamy aktualną prędkość gracza
        double xDist = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
        double zDist = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
        double currentBPS = Math.hypot(xDist, zDist) * 20.0; // Prędkość w blokach na sekundę

        double baseSprintBPS = MoveUtil.getBaseMoveSpeed() * 20.0;

        // C. Logika kolorów (Porównanie)
        String colorCode;

        if (currentBPS < 1.0) {
            // Prawie stoi
            colorCode = "\u00A78";
        } else if (currentBPS < baseSprintBPS * 0.9) {
            // Wolniej niż sprint (chodzenie, skradanie, blokowanie)
            colorCode = "\u00A77";
        } else if (currentBPS <= baseSprintBPS * 1.05) {
            // Normalny sprint (z marginesem błędu 5%)
            colorCode = "\u00A7f";
        } else if (currentBPS <= baseSprintBPS * 1.5) {
            // Szybciej niż sprint (BunnyHop, Ice, LowHop) - LEGITNE PRZYSPIESZENIE
            colorCode = "\u00A7a";
        } else {
            // Znacznie szybciej (Prawdopodobnie Speedhack/Timer/DamageBoost) - RYZYKOWNE
            colorCode = "\u00A7c";
        }

        // Formatowanie do 2 miejsc po przecinku dla precyzji
        String bpsText = String.format("\u00A7fBPS: %s%.2f", colorCode, currentBPS);

        // 3. XYZ
        String xyz = String.format("\u00A7fXYZ: \u00A77%.0f %.0f %.0f", mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

        return fps + " \u00A78| " + bpsText + " \u00A78| " + xyz;
    }
}