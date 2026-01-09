package doom.module.impl.render;

import doom.module.Module;
import doom.ui.clickgui.ClickGui; // <--- Importujemy nasze nowe okno
import org.lwjgl.input.Keyboard;

public class ClickGuiModule extends Module {

    public ClickGuiModule() {
        // Nazwa, Klawisz domyślny (Prawy Shift), Kategoria
        super("ClickGui", Keyboard.KEY_RSHIFT, Category.RENDER);
    }

    @Override
    public void onEnable() {
        // Null check (żeby nie wywaliło gry, jak nie ma gracza)
        if (mc.thePlayer != null && mc.theWorld != null) {
            // Wyświetlamy nasze nowe GUI
            mc.displayGuiScreen(new ClickGui());
        }

        // Wyłączamy moduł natychmiast po odpaleniu GUI.
        // Dzięki temu, gdy zamkniesz GUI (Esc), moduł będzie "odznaczony"
        // i będziesz mógł go włączyć znowu.
        this.toggle();
    }
}