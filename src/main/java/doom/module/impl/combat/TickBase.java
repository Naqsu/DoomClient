package doom.module.impl.combat;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPacket;
import doom.event.impl.EventRender2D;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import doom.util.TimeHelper;
import doom.util.TimerUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class TickBase extends Module {

    // --- USTAWIENIA ---
    public ModeSetting mode = new ModeSetting("Mode", this, "Balance", "Balance", "Timer", "Freeze");
    public NumberSetting ticks = new NumberSetting("Ticks", this, 10, 1, 40, 1); // Ile czasu cofnąć
    public BooleanSetting autoDisable = new BooleanSetting("Auto Disable", this, true); // Wyłącz po użyciu
    public BooleanSetting freeze = new BooleanSetting("Freeze Player", this, true); // Zatrzymaj postać podczas ładowania

    // --- ZMIENNE ---
    private int balance = 0; // Licznik "zaoszczędzonego" czasu
    private boolean isShifting = false; // Czy aktualnie przyspieszamy
    private final TimeHelper timer = new TimeHelper();

    public TickBase() {
        super("TickBase", Keyboard.KEY_NONE, Category.COMBAT);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(ticks);
        Client.INSTANCE.settingsManager.rSetting(autoDisable);
        Client.INSTANCE.settingsManager.rSetting(freeze);
    }

    @Override
    public void onEnable() {
        // Po włączeniu: Jeśli mamy balans -> zużyj go (przyspiesz)
        // Jeśli nie mamy -> zacznij ładować (zatrzymaj pakiety)

        if (balance > 0) {
            isShifting = true;
            TimerUtil.setTimerSpeed(getTimerSpeed()); // Przyspiesz grę
        } else {
            isShifting = false; // Będziemy ładować w onUpdate/onPacket
        }
    }

    @Override
    public void onDisable() {
        TimerUtil.reset();
        isShifting = false;
        // Jeśli wyłączamy ręcznie, resetujemy balans (tracimy pakiety)
        // balance = 0; // Opcjonalnie, lepiej zostawić na później
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        this.setSuffix(mode.getMode() + " [" + balance + "]");

        if (isShifting) {
            // --- FAZA ROZŁADOWANIA (SPEED) ---
            if (balance > 0) {
                balance--; // Zużywamy 1 tick balansu na 1 tick gry (ale gra działa szybciej)
            } else {
                // Koniec balansu -> Wróć do normy
                isShifting = false;
                TimerUtil.reset();
                if (autoDisable.isEnabled()) this.toggle();
            }
        } else {
            // --- FAZA ŁADOWANIA (FREEZE / LAG) ---
            // Jeśli moduł jest włączony, ale nie shiftujemy -> ładujemy balans
            if (balance < ticks.getValue()) {
                if (freeze.isEnabled()) {
                    mc.thePlayer.motionX = 0;
                    mc.thePlayer.motionY = 0;
                    mc.thePlayer.motionZ = 0;
                }
                // Pakiety są zatrzymywane w onPacket, tutaj tylko czekamy
            } else {
                // Naładowano -> Automatycznie odpal shift
                isShifting = true;
                TimerUtil.setTimerSpeed(getTimerSpeed());
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getDirection() == EventPacket.Direction.SEND) {
            if (event.getPacket() instanceof C03PacketPlayer) {
                // Jeśli nie shiftujemy (czyli ładujemy), zatrzymujemy pakiety ruchu
                if (!isShifting && this.isToggled() && balance < ticks.getValue()) {
                    event.setCancelled(true);
                    balance++; // Zyskujemy 1 tick balansu
                }
            }
        }
    }

    // Obliczanie prędkości Timera przy rozładowaniu
    // Im więcej ticków do nadrobienia, tym szybciej musimy to zrobić, ale bez przesady
    // Grim wykrywa timer > 2.0, więc lepiej użyć np. 1.5 przez dłuższy czas
    private float getTimerSpeed() {
        if (mode.is("Timer")) return 2.0f; // Szybki strzał
        return 1.2f + (balance / 100.0f); // Dynamicznie (np. 1.3 - 1.5)
    }

    // --- WIZUALIZACJA ---
    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (balance == 0 && !isShifting) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float width = 80;
        float height = 12;
        float x = sr.getScaledWidth() / 2.0f - width / 2.0f;
        float y = sr.getScaledHeight() / 2.0f + 30;

        RenderUtil.drawRoundedRect(x, y, width, height, 4, new Color(0, 0, 0, 150).getRGB());

        float progress = (float) balance / (float) ticks.getValue();
        if (progress > 1) progress = 1;

        int color = isShifting ? new Color(255, 50, 50).getRGB() : new Color(50, 255, 50).getRGB();

        RenderUtil.drawRoundedRect(x, y + height - 2, width * progress, 2, 1, color);

        String text = isShifting ? "Discharging..." : "Charging (" + balance + ")";
        FontManager.r20.drawStringWithShadow(text, x + width / 2 - FontManager.r20.getStringWidth(text) / 2, y + 2, -1);
    }
}