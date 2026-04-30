package net.oktawia.faststone.items;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.oktawia.faststone.blocks.LogicCableBlock;
import net.oktawia.faststone.entities.LogicCableBlockEntity;
import net.oktawia.faststone.logic.parts.LogicCablePartType;

@Getter
public class LogicCablePartItem extends Item {

    private static final double PX = 1.0D / 16.0D;

    private final LogicCablePartType partType;

    public LogicCablePartItem(Properties properties, LogicCablePartType partType) {
        super(properties);
        this.partType = partType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof LogicCableBlock cableBlock)) {
            return InteractionResult.PASS;
        }

        if (!(level.getBlockEntity(pos) instanceof LogicCableBlockEntity cable)) {
            return InteractionResult.PASS;
        }

        Direction side = getTargetCableSide(state, pos, context.getClickLocation());

        if (side == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            boolean changed = cable.setPart(side, partType);

            if (changed) {
                cableBlock.refreshSideAfterPartChange(level, pos, side);

                if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private Direction getTargetCableSide(BlockState state, BlockPos pos, Vec3 hitLocation) {
        double x = hitLocation.x - pos.getX();
        double y = hitLocation.y - pos.getY();
        double z = hitLocation.z - pos.getZ();

        Direction armSide = getClickedArmSide(state, x, y, z);

        if (armSide != null) {
            return armSide;
        }

        return getClickedCoreSide(x, y, z);
    }

    private Direction getClickedArmSide(BlockState state, double x, double y, double z) {
        if (state.getValue(LogicCableBlock.NORTH)
                && inside(x, y, z, 6, 6, 0, 10, 10, 6)) {
            return Direction.NORTH;
        }

        if (state.getValue(LogicCableBlock.SOUTH)
                && inside(x, y, z, 6, 6, 10, 10, 10, 16)) {
            return Direction.SOUTH;
        }

        if (state.getValue(LogicCableBlock.WEST)
                && inside(x, y, z, 0, 6, 6, 6, 10, 10)) {
            return Direction.WEST;
        }

        if (state.getValue(LogicCableBlock.EAST)
                && inside(x, y, z, 10, 6, 6, 16, 10, 10)) {
            return Direction.EAST;
        }

        if (state.getValue(LogicCableBlock.UP)
                && inside(x, y, z, 6, 10, 6, 10, 16, 10)) {
            return Direction.UP;
        }

        if (state.getValue(LogicCableBlock.DOWN)
                && inside(x, y, z, 6, 0, 6, 10, 6, 10)) {
            return Direction.DOWN;
        }

        return null;
    }

    private Direction getClickedCoreSide(double x, double y, double z) {
        double dx = x - 0.5D;
        double dy = y - 0.5D;
        double dz = z - 0.5D;

        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);

        if (ax >= ay && ax >= az) {
            return dx >= 0.0D ? Direction.EAST : Direction.WEST;
        }

        if (ay >= ax && ay >= az) {
            return dy >= 0.0D ? Direction.UP : Direction.DOWN;
        }

        return dz >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private boolean inside(
            double x,
            double y,
            double z,
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ
    ) {
        double minX = fromX * PX;
        double minY = fromY * PX;
        double minZ = fromZ * PX;

        double maxX = toX * PX;
        double maxY = toY * PX;
        double maxZ = toZ * PX;

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}