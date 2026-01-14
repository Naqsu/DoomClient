package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.util.RenderUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;

import java.awt.*;

public class ChestESP extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Full", "Full", "Box", "Outline");
    private final BooleanSetting chests = new BooleanSetting("Chests", this, true);
    private final BooleanSetting enderChests = new BooleanSetting("Ender Chests", this, true);
    private final BooleanSetting trappedChests = new BooleanSetting("Trapped", this, true);

    public ChestESP() {
        super("ChestESP", 0, Category.RENDER);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(chests);
        Client.INSTANCE.settingsManager.rSetting(enderChests);
        Client.INSTANCE.settingsManager.rSetting(trappedChests);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        for (TileEntity tile : mc.theWorld.loadedTileEntityList) {
            if (shouldRender(tile)) {

                // Ustalanie koloru w zaleznosci od typu
                Color color = getColor(tile);

                // Pobieranie pozycji wzgledem kamery
                double x = tile.getPos().getX() - mc.getRenderManager().viewerPosX;
                double y = tile.getPos().getY() - mc.getRenderManager().viewerPosY;
                double z = tile.getPos().getZ() - mc.getRenderManager().viewerPosZ;

                // Standardowy wymiar skrzynki (lekko mniejszy niz blok, zeby nie migalo)
                // Minecraftowe skrzynki nie zajmuja calego bloku (0.0625 marginesu)
                AxisAlignedBB bb = new AxisAlignedBB(
                        x + 0.0625, y, z + 0.0625,
                        x + 0.9375, y + 0.875, z + 0.9375
                );

                // Skrzynki Endu i Trapped sa takie same wymiarowo
                if (tile instanceof TileEntityEnderChest) {
                    // Ender Chest jest odrobine mniejszy/inny w modelu, ale BB jest ok.
                }

                // Renderowanie zgodnie z trybem
                boolean drawBox = mode.is("Box") || mode.is("Full");
                boolean drawOutline = mode.is("Outline") || mode.is("Full");

                if (drawBox) {
                    // Wypelnienie (lekko przezroczyste, alpha 60)
                    int argb = new Color(color.getRed(), color.getGreen(), color.getBlue(), 60).getRGB();
                    RenderUtil.drawFilledBox(bb, argb);
                }

                if (drawOutline) {
                    // Obrys (pelny kolor)
                    int argb = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255).getRGB();
                    RenderUtil.drawBoundingBox(bb, 1.5f, argb);
                }
            }
        }
    }

    private boolean shouldRender(TileEntity tile) {
        if (tile instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) tile;
            if (chest.getChestType() == 1) { // 1 = Trapped Chest
                return trappedChests.isEnabled();
            }
            return chests.isEnabled();
        }
        if (tile instanceof TileEntityEnderChest) {
            return enderChests.isEnabled();
        }
        return false;
    }

    private Color getColor(TileEntity tile) {
        if (tile instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) tile;
            if (chest.getChestType() == 1) {
                return new Color(255, 50, 50); // Czerwony dla Trapped
            }
            return new Color(255, 180, 0); // Złoty/Pomarańczowy dla Zwykłych
        }
        if (tile instanceof TileEntityEnderChest) {
            return new Color(180, 50, 255); // Fioletowy dla Ender
        }
        return Color.WHITE;
    }
}