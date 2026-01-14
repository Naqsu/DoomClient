package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender2D;
import doom.module.Module;
import doom.settings.impl.NumberSetting;
import doom.ui.notification.Notification;
import doom.ui.notification.NotificationManager;
import net.minecraft.client.gui.ScaledResolution;

public class NotificationsMod extends Module {

    // Ustawienie czasu trwania (możesz sobie zmienić w GUI)
    public static NumberSetting duration;

    public NotificationsMod() {
        super("Notifications", 0, Category.RENDER);
        this.setToggled(true); // Domyślnie włączone

        duration = new NumberSetting("Duration (s)", this, 2.5, 1.0, 5.0, 0.5);
        Client.INSTANCE.settingsManager.rSetting(duration);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (NotificationManager.getNotifications().isEmpty()) return;

        ScaledResolution sr = new ScaledResolution(mc);

        // 1. Obliczamy wysokość wszystkich powiadomień razem
        float heightSum = 0;
        for (Notification n : NotificationManager.getNotifications()) {
            heightSum += n.getHeight() + 5; // Wysokość + odstęp
        }

        // 2. Ustalamy punkt startowy
        // Zamiast "zabetonować" dół, mówimy:
        // "Zacznij rysować na takiej wysokości, żeby ostatnie powiadomienie dotykało dołu ekranu"
        float currentY = sr.getScaledHeight() - 20 - heightSum;

        // 3. Rysujemy z góry na dół
        for (Notification n : NotificationManager.getNotifications()) {

            // Renderujemy
            n.render(currentY);

            // Przesuwamy się w DÓŁ (dodajemy Y) dla następnego elementu
            currentY += (n.getHeight() + 5);

            if (n.shouldDelete()) {
                NotificationManager.getNotifications().remove(n);
            }
        }
    }
}