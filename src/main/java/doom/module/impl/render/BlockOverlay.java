package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.module.Category;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.util.ColorUtil;
import doom.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

import java.awt.Color;

public class BlockOverlay extends Module {

    private final BooleanSetting outline = new BooleanSetting("Outline", this, true);
    private final BooleanSetting fill = new BooleanSetting("Fill", this, true);
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", this, true);

    public BlockOverlay() {
        super("BlockOverlay", 0, Category.RENDER);
        Client.INSTANCE.settingsManager.rSetting(outline);
        Client.INSTANCE.settingsManager.rSetting(fill);
        Client.INSTANCE.settingsManager.rSetting(rainbow);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        // Sprawdzamy na co patrzy gracz (musi to być blok)
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            Block block = mc.theWorld.getBlockState(pos).getBlock();

            // Ignorujemy powietrze (na wszelki wypadek)
            if (block instanceof BlockAir) return;

            // Pobieramy bounding box bloku (żeby działało np. na schodkach czy płotkach poprawnie)
            AxisAlignedBB bb = block.getSelectedBoundingBox(mc.theWorld, pos);
            if (bb == null) return;

            // Konwersja koordynatów świata na koordynaty renderowania
            double viewerX = mc.getRenderManager().viewerPosX;
            double viewerY = mc.getRenderManager().viewerPosY;
            double viewerZ = mc.getRenderManager().viewerPosZ;

            // Przesuwamy boxa względem kamery
            AxisAlignedBB renderBB = new AxisAlignedBB(
                    bb.minX - viewerX, bb.minY - viewerY, bb.minZ - viewerZ,
                    bb.maxX - viewerX, bb.maxY - viewerY, bb.maxZ - viewerZ
            );

            // Kolor
            Color c = rainbow.isEnabled()
                    ? new Color(ColorUtil.getRainbow(4.0f, 0.7f, 1.0f, 0))
                    : new Color(220, 20, 20);

            // Rysowanie WYPEŁNIENIA
            if (fill.isEnabled()) {
                // Alpha 50/255 (półprzezroczyste)
                int fillColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), 60).getRGB();
                RenderUtil.drawFilledBox(renderBB, fillColor);
            }

            // Rysowanie OBRYSU
            if (outline.isEnabled()) {
                // Alpha 255 (pełne)
                int outlineColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255).getRGB();
                RenderUtil.drawBoundingBox(renderBB, 2.0f, outlineColor);
            }
        }
    }
}