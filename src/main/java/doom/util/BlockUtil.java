package doom.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.util.List;

public class BlockUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isAirOrLiquid(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block instanceof BlockAir || block instanceof BlockLiquid;
    }

    public static BlockPos getAimBlockPos() {
        // Logika szukania bloku pod nogami dla Scaffolda
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ);

        if ((mc.gameSettings.keyBindJump.isKeyDown() || !mc.thePlayer.onGround)
                && mc.thePlayer.moveForward == 0.0f
                && mc.thePlayer.moveStrafing == 0.0f
                && !isAirOrLiquid(playerPos.add(0, -1, 0))) {
            return playerPos.add(0, -1, 0);
        }
        return playerPos; // Uproszczone na potrzeby startu
    }

    // Pobiera listę bloków wokół gracza (do Scaffolda)
    public static BlockData getBlockData(BlockPos pos, List<Block> invalidBlocks) {
        if (!invalidBlocks.contains(mc.theWorld.getBlockState(pos).getBlock())) return null;

        EnumFacing[] facings = EnumFacing.values();

        for (EnumFacing facing : facings) {
            BlockPos neighbor = pos.offset(facing);
            if (!invalidBlocks.contains(mc.theWorld.getBlockState(neighbor).getBlock())) {
                return new BlockData(neighbor, facing.getOpposite());
            }
        }

        // Extended search (diagonals)
        BlockPos[] diagonals = new BlockPos[]{
                pos.add(-1, 0, 0), pos.add(1, 0, 0), pos.add(0, 0, -1), pos.add(0, 0, 1)
        };
        for (BlockPos diagonal : diagonals) {
            if (!invalidBlocks.contains(mc.theWorld.getBlockState(diagonal.down()).getBlock())) {
                return new BlockData(diagonal.down(), EnumFacing.UP);
            }
        }

        return null;
    }

    public static class BlockData {
        public final BlockPos pos;
        public final EnumFacing face;
        public BlockData(BlockPos pos, EnumFacing face) {
            this.pos = pos;
            this.face = face;
        }
    }

    public static Vec3 getVec3(BlockData data) {
        BlockPos pos = data.pos;
        EnumFacing face = data.face;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        x += face.getFrontOffsetX() * 0.5;
        y += face.getFrontOffsetY() * 0.5;
        z += face.getFrontOffsetZ() * 0.5;
        return new Vec3(x, y, z);
    }
}