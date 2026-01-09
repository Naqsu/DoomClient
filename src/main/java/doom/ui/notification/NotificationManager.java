package doom.ui.notification;

import doom.Client;
import doom.module.impl.render.NotificationsMod;

import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {

    // Lista musi być dostępna dla modułu (dlatego getter)
    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    public static void show(String title, String message, NotificationType type) {
        long time = 2500;
        NotificationsMod mod = (NotificationsMod) Client.INSTANCE.moduleManager.getModule(NotificationsMod.class);

        if (mod != null) {
            if (!mod.isToggled()) return;
            time = (long) (mod.duration.getValue() * 1000);
        }

        // --- POPRAWKA: LIMIT POWIADOMIEŃ ---
        // Jeśli jest ich za dużo, usuwamy najstarsze (pierwsze na liście)
        // Dzięki temu "winda" nie pojedzie do nieba.
        if (notifications.size() > 8) {
            notifications.remove(0);
        }
        // -----------------------------------

        notifications.add(new Notification(title, message, type, time));
    }

    // Getter dla modułu
    public static CopyOnWriteArrayList<Notification> getNotifications() {
        return notifications;
    }
}