package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

import java.awt.*;

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
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            Block block = mc.theWorld.getBlockState(pos).getBlock();

            if (block instanceof BlockAir) return;

            mc.getRenderManager();
            double viewerX = mc.getRenderManager().viewerPosX;
            double viewerY = mc.getRenderManager().viewerPosY;
            double viewerZ = mc.getRenderManager().viewerPosZ;

            // Pobieramy bounding box i dopasowujemy go do renderowania
            AxisAlignedBB box = block.getSelectedBoundingBox(mc.theWorld, pos)
                    .expand(0.002, 0.002, 0.002) // Lekkie powiększenie, żeby nie migało (Z-Fighting)
                    .offset(-viewerX, -viewerY, -viewerZ);

            Color c = rainbow.isEnabled()
                    ? new Color(ColorUtil.getRainbow(4.0f, 0.7f, 1.0f, 0))
                    : new Color(220, 20, 20);

            // Setup OpenGL
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false); // Ważne: pozwala widzieć przez blok, ale nie psuje głębi

            // 1. WYPEŁNIENIE
            if (fill.isEnabled()) {
                GlStateManager.color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 0.3f); // Alpha 0.3
                drawBox(box);
            }

            // 2. OBRYS
            if (outline.isEnabled()) {
                GlStateManager.color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 1.0f); // Alpha 1.0
                GL11.glLineWidth(2.0f);
                RenderGlobal.drawSelectionBoundingBox(box);
            }

            // Cleanup OpenGL
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    // Prosta metoda do rysowania pudełka
    private void drawBox(AxisAlignedBB bb) {
        GL11.glBegin(GL11.GL_QUADS);
        // Dół
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        // Góra
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        // Przód
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        // Tył
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        // Lewo
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        // Prawo
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glEnd();
    }
}