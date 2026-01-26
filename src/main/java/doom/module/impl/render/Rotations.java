package doom.module.impl.render;

import doom.event.EventManager;
import doom.event.EventTarget;
import doom.event.impl.EventRenderModel;
import doom.module.Module;
import doom.util.RotationUtil;

public class Rotations extends Module {

    // Przechowujemy oryginalne wartości, żeby nie zgubić ich między eventami
    private float realPitch = 0;
    private float realPrevPitch = 0;
    private float realHeadYaw = 0;
    private float realPrevHeadYaw = 0;
    private boolean isHooked = false;

    public Rotations() {
        super("Rotations", 0, Category.RENDER);
        this.toggled = true;
        this.hidden = true;
        EventManager.register(this);
    }

    @EventTarget
    public void onRenderModel(EventRenderModel event) {
        if (event.getEntity() != mc.thePlayer || !RotationUtil.shouldUseCustomPitch) return;

        // FAZA 1: PRZED RENDEROWANIEM (Podmiana na fejka)
        // Sprawdzamy czy event nie ma ustawionego taska (czyli czy to początek renderowania)
        // Ponieważ Twój EventRenderModel jest prosty, musimy to obsłużyć sprytnie.

        // Zapisujemy prawdziwe wartości
        realPitch = mc.thePlayer.rotationPitch;
        realPrevPitch = mc.thePlayer.prevRotationPitch;

        // --- FIX DLA SKAKANIA GŁOWY ---
        // Jeśli RenderPitch jest zbyt różny od prawdziwego, interpolujemy go jeszcze raz tutaj
        // Ale SingleMode robi to dobrze, więc po prostu podmieniamy.

        mc.thePlayer.rotationPitch = RotationUtil.renderPitch;
        mc.thePlayer.prevRotationPitch = RotationUtil.renderPitch; // Ważne! Prev też musi być podmieniony

        isHooked = true;

        // FAZA 2: PO RENDEROWANIU (Przywracanie)
        event.setPostRenderTask(() -> {
            if (isHooked && mc.thePlayer != null) {
                // Przywracamy dokładnie te wartości, które były przed chwilą
                mc.thePlayer.rotationPitch = realPitch;
                mc.thePlayer.prevRotationPitch = realPrevPitch;
                isHooked = false;
            }
        });
    }
}