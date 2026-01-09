package doom.ui.notification;

import java.awt.Color;

public enum NotificationType {
    INFO(new Color(255, 255, 255), "Info"),
    SUCCESS(new Color(100, 255, 100), "Success"),
    WARNING(new Color(255, 210, 100), "Warning"),
    ERROR(new Color(255, 100, 100), "Error");

    private final Color color;
    private final String name;

    NotificationType(Color color, String name) {
        this.color = color;
        this.name = name;
    }

    public int getColor() {
        return color.getRGB();
    }

    public String getName() {
        return name;
    }
}