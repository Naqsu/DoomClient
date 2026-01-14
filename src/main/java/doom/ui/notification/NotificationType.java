package doom.ui.notification;

import java.awt.*;

public enum NotificationType {
    INFO(new Color(200, 200, 255), "Info", "i"),
    SUCCESS(new Color(80, 255, 80), "Success", "v"), // Jasny zielony
    WARNING(new Color(255, 200, 50), "Warning", "!"), // Złoty
    ERROR(new Color(255, 60, 60), "Error", "x");      // Czerwony Doom

    private final Color color;
    private final String name;
    private final String icon;

    NotificationType(Color color, String name, String icon) {
        this.color = color;
        this.name = name;
        this.icon = icon;
    }

    public int getColor() {
        return color.getRGB();
    }

    public Color getColorObj() {
        return color;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }
}