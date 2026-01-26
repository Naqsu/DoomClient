package doom.module.impl.combat.killaura;

import doom.event.impl.EventUpdate;
import doom.module.impl.combat.Killaura;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public abstract class AuraMode {
    protected Minecraft mc = Minecraft.getMinecraft();
    protected Killaura parent;
    protected String name;

    public AuraMode(String name, Killaura parent) {
        this.name = name;
        this.parent = parent;
    }

    public abstract void onUpdate(EventUpdate event);
    public abstract void onDisable();
    public abstract void onEnable();

    protected void attack(EntityLivingBase target) {
        // --- FIX GRIM POST ANIMATION ---
        // Nawet jeśli AutoBlock jest wyłączony, gracz może ręcznie trzymać PPM (jedzenie, łuk, manualny blok).
        // Grim flaguje "Post Animation", jeśli wyślesz pakiet ataku (C02) będąc w stanie "Using Item".
        // Ten kod sprawdza, czy używasz przedmiotu i wysyła pakiet "puszczenia" przed atakiem.

        if (mc.thePlayer.isUsingItem()) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
            ));
        }

        // Swing (C0A) - musi być PRZED atakiem na 1.8.9 dla Grima
        mc.thePlayer.swingItem();

        // Atak (C02)
        mc.playerController.attackEntity(mc.thePlayer, target);
    }
}