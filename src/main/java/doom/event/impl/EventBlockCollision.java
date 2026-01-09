package doom.event.impl;

import doom.event.Event;
import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

public class EventBlockCollision extends Event {
    private Block block;
    private BlockPos pos;
    private AxisAlignedBB boundingBox;

    public EventBlockCollision(Block block, BlockPos pos, AxisAlignedBB boundingBox) {
        this.block = block;
        this.pos = pos;
        this.boundingBox = boundingBox;
    }

    public Block getBlock() { return block; }
    public BlockPos getPos() { return pos; }

    public AxisAlignedBB getBoundingBox() { return boundingBox; }

    // To pozwala nam ustawić pudełko kolizji na NULL (brak kolizji) lub inne
    public void setBoundingBox(AxisAlignedBB boundingBox) {
        this.boundingBox = boundingBox;
    }
}