package doom.module;

import doom.event.EventManager;
import net.minecraft.client.Minecraft;

public class Module {
    public String name;
    public int key;
    public boolean toggled;
    public Category category;
    public boolean hidden = false;
    public String suffix = "";
    // --- NOWE POLA DLA HUD ---
    public float x = 10, y = 10; // Pozycja
    public float width = 0, height = 0; // Wymiary
    // -------------------------

    public Minecraft mc = Minecraft.getMinecraft();

    public Module(String name, int key, Category category) {
        this.name = name;
        this.key = key;
        this.category = category;
        this.toggled = false;
    }

    // Metoda sprawdzająca, czy myszka jest nad modułem (do Edytora)
    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public String getSuffix() { return suffix; }

    public void toggle() {
        toggled = !toggled;
        if (toggled) {
            onEnable();
            EventManager.register(this);
            // DODAJ TO:
            doom.ui.notification.NotificationManager.show("Module", getName() + " enabled", doom.ui.notification.NotificationType.SUCCESS);
        } else {
            onDisable();
            EventManager.unregister(this);
            // DODAJ TO:
            doom.ui.notification.NotificationManager.show("Module", getName() + " disabled", doom.ui.notification.NotificationType.ERROR);
        }
    }

    // Obsługa włączania przez kod (np. config)
    public void setToggled(boolean state) {
        this.toggled = state;
        if (this.toggled) {
            onEnable();
            EventManager.register(this);
            // Opcjonalnie tutaj też możesz dodać, ale toggle() jest częściej używane przez gracza
        } else {
            onDisable();
            EventManager.unregister(this);
        }
    }

    public void onEnable() {}
    public void onDisable() {}

    public String getName() { return name; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public boolean isToggled() { return toggled; }
    public Category getCategory() { return category; }

    public enum Category {
        COMBAT, MOVEMENT, PLAYER, RENDER, MISC
    }
}