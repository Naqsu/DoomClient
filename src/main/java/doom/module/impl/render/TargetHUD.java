package doom.module.impl.render;

import doom.Client;
import doom.module.DraggableModule;
import doom.module.impl.combat.Killaura;
import doom.ui.font.FontManager;
import doom.util.ColorUtil;
import doom.util.RenderUtil;
import doom.util.StencilUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;

public class TargetHUD extends DraggableModule {

    // Zmienne do animacji
    private double healthBarWidth = 0;
    private double healthAnim = 0;
    private float hurtPercent = 0; // Do efektu czerwonego tła

    public TargetHUD() {
        super("TargetHUD", Category.RENDER);
        this.x = 400;
        this.y = 300;
    }

    @Override
    public float getWidth() {
        return 160;
    }

    @Override
    public float getHeight() {
        return 55;
    }

    @Override
    public void render(float x, float y) {
        // Tryb edycji (pokazuje Fake Target)
        if (mc.currentScreen instanceof doom.ui.hudeditor.GuiHudEditor) {
            drawTarget(x, y, mc.thePlayer);
            return;
        }

        // Pobieramy cel z Killaury
        EntityLivingBase target = null;
        Killaura killaura = Client.INSTANCE.moduleManager.getModule(Killaura.class);

        if (killaura != null && killaura.target != null) {
            target = killaura.target;
        } else if (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) {
            // Opcjonalnie: pokazuj siebie gdy piszesz na czacie (do testów)
            target = mc.thePlayer;
        }

        if (target != null && !target.isDead) {
            drawTarget(x, y, target);
        } else {
            // Reset animacji gdy brak celu
            healthBarWidth = 0;
            hurtPercent = 0;
        }
    }

    private void drawTarget(float x, float y, EntityLivingBase target) {
        float width = getWidth();
        float height = getHeight();

        // 1. BLUR TŁA
        // Rysujemy blur tylko pod spodem
        HUD hud = Client.INSTANCE.moduleManager.getModule(HUD.class);
        if (hud.blur.isEnabled()) {
            RenderUtil.drawBlur(x, y, width, height, 15);
        }

        // 2. OBLICZANIE KOLORU TŁA (Damage Flash)
        // Jeśli cel dostaje obrażenia, tło robi się czerwonawe
        if (target.hurtTime > 0) {
            hurtPercent = RenderUtil.lerp(hurtPercent, 1.0f, 0.1f);
        } else {
            hurtPercent = RenderUtil.lerp(hurtPercent, 0.0f, 0.1f);
        }

        int baseBg = new Color(20, 20, 25, 180).getRGB();
        int hurtBg = new Color(100, 0, 0, 180).getRGB();
        int finalBg = interpolateColor(baseBg, hurtBg, hurtPercent);

        // 3. RYSOWANIE TŁA
        RenderUtil.drawRoundedRect(x, y, width, height, 8, finalBg);

        // Subtelny Glow dookoła
        RenderUtil.drawGlow(x, y, width, height, 10, new Color(0, 0, 0, 100).getRGB());

        // 4. AVATAR 3D Z MASKOWANIEM (STENCIL)
        // Chcemy, żeby model gracza był ucięty w ładnym kółku/kwadracie po lewej

        StencilUtil.initStencilToWrite();
        // Rysujemy kształt maski (zaokrąglony kwadrat po lewej)
        RenderUtil.drawRoundedRect(x + 5, y + 5, 45, 45, 6, -1);

        StencilUtil.readStencilBuffer(1);

        // Rysujemy model wewnątrz maski
        float scale = 22;
        // Fix rotacji (żeby patrzył na nas, albo w stronę gdzie patrzy)
        //float yawOffset = target.prevRotationYawHead + (target.rotationYawHead - target.prevRotationYawHead) * mc.timer.renderPartialTicks;

        GlStateManager.color(1, 1, 1, 1);
        GuiInventory.drawEntityOnScreen((int)(x + 28), (int)(y + 42), (int)scale, x + 28 - mouseX(), y + 25 - mouseY(), target);

        StencilUtil.uninitStencilBuffer(); // Koniec maskowania

        // 5. DANE (Nazwa, HP)
        float textX = x + 55;

        // Nazwa
        FontManager.b20.drawStringWithShadow(target.getName(), textX, y + 8, -1);

        // HP (Liczbowo)
        DecimalFormat df = new DecimalFormat("##.#");
        String hpText = df.format(target.getHealth());
        // Kolor zależny od HP
        int hpColor = getHealthColor(target.getHealth(), target.getMaxHealth());
        FontManager.r18.drawStringWithShadow(hpText + " HP", textX, y + 22, -1);

        // 6. PASEK ŻYCIA (Animowany Gradient)
        float barX = textX;
        float barY = y + 38;
        float barWidth = width - 65;
        float barHeight = 8;

        // Tło paska (ciemne)
        RenderUtil.drawRoundedRect(barX, barY, barWidth, barHeight, 4, new Color(40, 40, 40, 200).getRGB());

        // Obliczanie szerokości
        double hpPercentage = Math.min(target.getHealth() / target.getMaxHealth(), 1.0);
        double targetWidth = barWidth * hpPercentage;

        // Płynna animacja (Lerp)
        healthBarWidth = RenderUtil.lerp(healthBarWidth, targetWidth, 0.15); // Szybkość animacji

        // Zabezpieczenie przed wyjściem poza ramkę (gdyby animacja przeskoczyła)
        if (healthBarWidth > barWidth) healthBarWidth = barWidth;
        if (healthBarWidth < 0) healthBarWidth = 0;

        // Gradient (Zielony -> Żółty -> Czerwony)
        int color1 = getHealthColor(target.getHealth(), target.getMaxHealth());
        int color2 = new Color(color1).darker().getRGB();

        // Rysowanie paska z gradientem
        // Używamy Scissora, żeby przyciąć gradient do zaokrąglenia
        // Ale prościej: drawRoundedGradientRect
        RenderUtil.drawRoundedGradientRect(barX, barY, (float)healthBarWidth, barHeight, 4, color1, color2);

        // Glow paska (daje efekt neonu)
        RenderUtil.drawGlow(barX, barY, (float)healthBarWidth, barHeight, 5, new Color(color1).getRGB());
    }

    // --- HELPERY ---

    private int getHealthColor(float health, float maxHealth) {
        float percentage = Math.max(0, Math.min(health, maxHealth) / maxHealth);
        // HSB Color: 0.33 (Zielony) -> 0.0 (Czerwony)
        return Color.getHSBColor(percentage * 0.33f, 0.9f, 1.0f).getRGB();
    }

    private int interpolateColor(int start, int end, float fraction) {
        if(fraction > 1) fraction = 1; if(fraction < 0) fraction = 0;
        int a1 = (start >> 24) & 0xFF, r1 = (start >> 16) & 0xFF, g1 = (start >> 8) & 0xFF, b1 = start & 0xFF;
        int a2 = (end >> 24) & 0xFF, r2 = (end >> 16) & 0xFF, g2 = (end >> 8) & 0xFF, b2 = end & 0xFF;
        int a = (int)(a1+(a2-a1)*fraction), r = (int)(r1+(r2-r1)*fraction), g = (int)(g1+(g2-g1)*fraction), b = (int)(b1+(b2-b1)*fraction);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    // Pomocnicze do obracania modelu za myszką (opcjonalne, tutaj statyczne 0)
    private int mouseX() { return 0; }
    private int mouseY() { return 0; }
}