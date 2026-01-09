package doom.command;

import net.minecraft.client.Minecraft;

public abstract class Command {
    public String name, description, syntax;
    public Minecraft mc = Minecraft.getMinecraft();

    public Command(String name, String description, String syntax) {
        this.name = name;
        this.description = description;
        this.syntax = syntax;
    }

    public abstract void onChat(String[] args);

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSyntax() { return syntax; }
}