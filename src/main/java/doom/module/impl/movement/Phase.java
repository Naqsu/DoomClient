package doom.module.impl.movement;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventBlockCollision;
import doom.event.impl.EventUpdate;
import doom.event.impl.EventPacket;
import doom.module.Category;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.MoveUtil;
import doom.util.TimeHelper;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class Phase extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Collision", "Collision", "Skip", "Grim V-Clip", "Vanilla", "Spider");
    private final NumberSetting distance = new NumberSetting("Distance", this, 2.0, 0.1, 5.0, 0.1);
    private final BooleanSetting sneakOnly = new BooleanSetting("Sneak Only", this, false);
    private final BooleanSetting motionStop = new BooleanSetting("Stop Motion", this, true);

    private final TimeHelper timer = new TimeHelper();

    public Phase() {
        super("Phase", 0, Category.MOVEMENT);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(distance);
        Client.INSTANCE.settingsManager.rSetting(sneakOnly);
        Client.INSTANCE.settingsManager.rSetting(motionStop);
    }

    @Override
    public void onDisable() {
        mc.thePlayer.noClip = false;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (sneakOnly.isEnabled() && !mc.thePlayer.isSneaking()) return;

        switch (mode.getMode()) {
            case "Vanilla":
                if (mc.thePlayer.isCollidedHorizontally) {
                    double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
                    double x = -Math.sin(yaw) * distance.getValue();
                    double z = Math.cos(yaw) * distance.getValue();
                    mc.thePlayer.setPosition(mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z);
                }
                break;

            case "Skip":
                // Tryb NCP - przeskakuje małe odległości
                if (mc.thePlayer.isCollidedHorizontally && timer.hasReached(150)) {
                    double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
                    double dist = 0.2; // Małe kroki są bezpieczniejsze
                    for (int i = 0; i < 5; i++) { // 5 pakietów po 0.2 = 1 blok
                        double x = -Math.sin(yaw) * dist * (i + 1);
                        double z = Math.cos(yaw) * dist * (i + 1);
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                                mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z, true
                        ));
                    }
                    mc.thePlayer.setPosition(
                            mc.thePlayer.posX - Math.sin(yaw) * 1.0,
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ + Math.cos(yaw) * 1.0
                    );
                    timer.reset();
                }
                break;

            case "Grim V-Clip":
                // Grim pozwala na spadanie, ale sprawdza kolizje.
                // Ten tryb teleportuje w dół przez podłogę.
                if (mc.gameSettings.keyBindSneak.isKeyDown() && timer.hasReached(500)) {
                    // Grim V-Clip bypass (teleportacja bardzo głęboko w dół, często myli checki falling)
                    // Wymaga "powietrza" pod podłogą (np. jaskinie, piętro niżej)
                    double clip = -3.0; // 3 bloki w dół (standardowa wysokość piętra)

                    // Sending fake falling packets
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + clip, mc.thePlayer.posZ, true));
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + clip, mc.thePlayer.posZ);

                    timer.reset();
                }
                break;

            case "Spider":
                // Wchodzi w sufit
                if (mc.thePlayer.isCollidedVertically && isInsideBlock()) {
                    mc.thePlayer.motionY = 0.1; // Wypycha w górę
                    mc.thePlayer.onGround = true; // Oszukuje, że stoimy
                }
                break;
        }
    }

    @EventTarget
    public void onCollision(EventBlockCollision event) {
        if (!mode.is("Collision") && !mode.is("Spider")) return;
        if (sneakOnly.isEnabled() && !mc.thePlayer.isSneaking()) return;

        // Warunek: Jesteśmy w bloku i chcemy przez niego przejść
        if (event.getBoundingBox() != null && event.getBoundingBox().maxY > mc.thePlayer.getEntityBoundingBox().minY) {

            // W trybie Collision ustawiamy boxa na null -> brak kolizji fizycznej
            if (isInsideBlock() || mc.thePlayer.isCollidedHorizontally) {
                event.setBoundingBox(null);

                // Zatrzymujemy ruch, żeby nie przelecieć przez świat
                if (motionStop.isEnabled()) {
                    mc.thePlayer.motionY = 0;
                    if (mc.gameSettings.keyBindJump.isKeyDown()) mc.thePlayer.motionY = 0.5;
                    if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.thePlayer.motionY = -0.5;
                }
            }
        }
    }

    // Sprawdza czy głowa jest w bloku (pomocnicze)
    private boolean isInsideBlock() {
        for (int x = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minX); x < MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().maxX) + 1; ++x) {
            for (int y = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY); y < MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().maxY) + 1; ++y) {
                for (int z = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minZ); z < MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().maxZ) + 1; ++z) {
                    net.minecraft.block.Block block = mc.theWorld.getBlockState(new net.minecraft.util.BlockPos(x, y, z)).getBlock();
                    net.minecraft.util.AxisAlignedBB boundingBox;
                    if (block != null && !(block instanceof net.minecraft.block.BlockAir) && (boundingBox = block.getCollisionBoundingBox(mc.theWorld, new net.minecraft.util.BlockPos(x, y, z), mc.theWorld.getBlockState(new net.minecraft.util.BlockPos(x, y, z)))) != null && mc.thePlayer.getEntityBoundingBox().intersectsWith(boundingBox)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}