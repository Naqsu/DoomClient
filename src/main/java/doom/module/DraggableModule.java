package doom.module;

import doom.event.EventTarget;
import doom.event.impl.EventRender2D;
import net.minecraft.client.gui.ScaledResolution;

public abstract class DraggableModule extends Module {

    // Zapamiętujemy ostatni rozmiar ekranu, żeby wykryć zmianę
    private int lastScreenWidth = -1;
    private int lastScreenHeight = -1;

    public DraggableModule(String name, Category category) {
        super(name, 0, category);
    }

    public abstract float getWidth();
    public abstract float getHeight();
    public abstract void render(float x, float y);

    @EventTarget
    public void onRender2D(EventRender2D event) {
        this.width = getWidth();
        this.height = getHeight();

        ScaledResolution sr = new ScaledResolution(mc);
        int currentScreenWidth = sr.getScaledWidth();
        int currentScreenHeight = sr.getScaledHeight();

        // 1. ANCHOR FIX (Obsługa zmiany rozmiaru okna w trakcie gry)
        if (lastScreenWidth != -1 && lastScreenHeight != -1 &&
                (lastScreenWidth != currentScreenWidth || lastScreenHeight != currentScreenHeight)) {

            // Jeśli okno się POWIĘKSZYŁO, to chcemy przesunąć elementy przyklejone do prawej/dołu
            // Ale jeśli okno jest MAŁE (start gry), to nie chcemy ich "zgniatać"

            if (this.x + this.width / 2 > lastScreenWidth / 2) {
                float diff = lastScreenWidth - this.x;
                this.x = currentScreenWidth - diff;
            }

            if (this.y + this.height / 2 > lastScreenHeight / 2) {
                float diff = lastScreenHeight - this.y;
                this.y = currentScreenHeight - diff;
            }
        }

        // Zapisujemy rozmiar do następnej klatki
        lastScreenWidth = currentScreenWidth;
        lastScreenHeight = currentScreenHeight;

        // 2. STARTUP FIX (To naprawia Twój problem!)
        // Jeśli okno jest bardzo małe (start gry), NIE wykonuj Clampingu (zabezpieczenia).
        // Pozwól elementowi być "poza ekranem" przez chwilę, aż okno się zmaksymalizuje.
        if (currentScreenWidth < 900 || currentScreenHeight < 400) {
            render(this.x, this.y);
            return;
        }

        // 3. CLAMP (Zabezpieczenie przed ucieczką poza ekran - działa tylko na dużym oknie)
        if (this.x < 0) this.x = 0;
        if (this.y < 0) this.y = 0;
        if (this.x > currentScreenWidth - this.width) this.x = currentScreenWidth - this.width;
        if (this.y > currentScreenHeight - this.height) this.y = currentScreenHeight - this.height;

        render(this.x, this.y);
    }
}