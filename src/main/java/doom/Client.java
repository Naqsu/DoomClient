package doom;

import doom.command.CommandManager;
import doom.config.ConfigManager;
import doom.event.EventManager;
import doom.module.Module;
import doom.settings.SettingsManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import java.awt.Toolkit;
import java.awt.Dimension;
import doom.module.ModuleManager;
import org.lwjgl.opengl.Display;

public class Client {
    // To jest Singleton - jedna instancja na całą grę
    public static Client INSTANCE = new Client();

    public String name = "Doom Client";
    public String version = "v1.0";

    public CommandManager commandManager;
    public ModuleManager moduleManager;
    public SettingsManager settingsManager;
    public ConfigManager configManager;

    // Ta metoda odpali się przy starcie gry
    public void startup() {
        System.out.println("Starting " + name + " " + version + "...");
        Display.setTitle(name + " " + version); // Zmienia nazwę okna
        EventManager.register(this);
        settingsManager = new SettingsManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        Module hud = moduleManager.getModule(doom.module.impl.render.HUD.class);
        if (hud != null) {
            hud.setToggled(true);
        }
        this.configManager = new ConfigManager();
        this.configManager.load();
    }

    public void shutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            configManager.save();
        }));
        System.out.println("Closing...");
    }
    public static void addChatMessage(String message) {
        if (net.minecraft.client.Minecraft.getMinecraft().thePlayer != null) {
            net.minecraft.client.Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new net.minecraft.util.ChatComponentText("§8[§cDoom§8] §7" + message)
            );
        }
    }
}