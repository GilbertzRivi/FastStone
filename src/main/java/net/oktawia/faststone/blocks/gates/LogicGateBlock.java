package net.oktawia.faststone.blocks.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.oktawia.faststone.blocks.LogicCableBlock;
import net.oktawia.faststone.entities.gates.LogicGateBlockEntity;
import net.oktawia.faststone.items.LogicCableBlockItem;
import net.oktawia.faststone.logic.LogicCableColor;
import net.oktawia.faststone.logic.interfaces.LogicConnectable;
import net.oktawia.faststone.logic.interfaces.LogicGateConfigurable;
import net.oktawia.faststone.logic.network.LogicNetworkGraph;

import java.util.Map;

public abstract class LogicGateBlock extends Block implements EntityBlock, LogicConnectable {

    public static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);

    private static final Map<Direction, VoxelShape> STUBS = Map.of(
            Direction.NORTH, Block.box(6, 6, 4, 10, 10, 5),
            Direction.SOUTH, Block.box(6, 6, 11, 10, 10, 12),
            Direction.WEST,  Block.box(4, 6, 6, 5, 10, 10),
            Direction.EAST,  Block.box(11, 6, 6, 12, 10, 10),
            Direction.UP,    Block.box(6, 11, 6, 10, 12, 10),
            Direction.DOWN,  Block.box(6, 4, 6, 10, 5, 10)
    );

    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, Block.box(6, 6, 0, 10, 10, 5),
            Direction.SOUTH, Block.box(6, 6, 11, 10, 10, 16),
            Direction.WEST,  Block.box(0, 6, 6, 5, 10, 10),
            Direction.EAST,  Block.box(11, 6, 6, 16, 10, 10),
            Direction.UP,    Block.box(6, 11, 6, 10, 16, 10),
            Direction.DOWN,  Block.box(6, 0, 6, 10, 5, 10)
    );

    private final int coreColor;

    public LogicGateBlock(BlockBehaviour.Properties properties, int coreColor) {
        super(properties.noOcclusion());
        this.coreColor = coreColor;
    }

    public int getCoreColor() {
        return coreColor;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState();
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos currentPos,
            BlockPos neighborPos
    ) {
        if (level instanceof Level realLevel && !realLevel.isClientSide) {
            if (realLevel.getBlockEntity(currentPos) instanceof LogicGateBlockEntity gate) {
                gate.onGateNeighborChanged(direction);
            }

            LogicNetworkGraph.scheduleRebuildAround(realLevel, currentPos);
        }

        return state;
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide && oldState.getBlock() != state.getBlock()) {
            LogicNetworkGraph.scheduleRebuildAround(level, pos);
        }
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            LogicNetworkGraph.scheduleRebuildAround(level, pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof LogicGateBlockEntity gate)) {
            return InteractionResult.PASS;
        }

        Direction side = getTargetSide(gate, level, pos, hit.getLocation(), hit.getDirection());
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown() && gate instanceof LogicGateConfigurable configurable) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(
                        serverPlayer,
                        configurable,
                        buffer -> buffer.writeBlockPos(pos)
                );
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.getItem() instanceof LogicCableBlockItem cableItem) {
            if (!level.isClientSide && gate.hasVisiblePort(side)) {
                gate.setPortColor(side, cableItem.getColor());
                refreshSideAfterPortChange(level, pos, side);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!stack.isEmpty() && !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            gate.cyclePortState(side);
            refreshSideAfterPortChange(level, pos, side);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void refreshSideAfterPortChange(Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        forceNeighborShapeUpdate(level, pos, side);

        BlockPos neighborPos = pos.relative(side);

        level.updateNeighborsAt(pos, block);
        level.updateNeighborsAt(neighborPos, block);
        level.neighborChanged(neighborPos, block, pos);

        if (!level.isClientSide) {
            LogicNetworkGraph.scheduleRebuildAround(level, pos);
        }
    }

    private void forceNeighborShapeUpdate(Level level, BlockPos pos, Direction side) {
        BlockPos neighborPos = pos.relative(side);

        BlockState selfState = level.getBlockState(pos);
        BlockState neighborState = level.getBlockState(neighborPos);

        BlockState updatedNeighborState = neighborState.updateShape(
                side.getOpposite(),
                selfState,
                level,
                neighborPos,
                pos
        );

        if (!updatedNeighborState.equals(neighborState)) {
            level.setBlock(neighborPos, updatedNeighborState, Block.UPDATE_ALL);
        } else {
            level.sendBlockUpdated(neighborPos, neighborState, neighborState, Block.UPDATE_ALL);
            neighborState.neighborChanged(level, neighborPos, selfState.getBlock(), pos, false);
        }
    }

    private Direction getTargetSide(
            LogicGateBlockEntity gate,
            BlockGetter level,
            BlockPos pos,
            Vec3 hitLocation,
            Direction fallback
    ) {
        Direction portSide = getClickedVisiblePortSide(gate, level, pos, hitLocation);

        if (portSide != null) {
            return portSide;
        }

        double x = hitLocation.x - pos.getX();
        double y = hitLocation.y - pos.getY();
        double z = hitLocation.z - pos.getZ();

        if (inside(x, y, z, 5, 5, 5, 11, 11, 11)) {
            return getCoreSide(x, y, z);
        }

        return fallback;
    }

    public static Direction getClickedVisiblePortSide(
            LogicGateBlockEntity gate,
            BlockGetter level,
            BlockPos pos,
            Vec3 hitLocation
    ) {
        double x = hitLocation.x - pos.getX();
        double y = hitLocation.y - pos.getY();
        double z = hitLocation.z - pos.getZ();

        for (Direction side : Direction.values()) {
            if (!gate.hasVisiblePort(side)) {
                continue;
            }

            boolean connected = isSideConnectedToCable(level, pos, side);

            if (insidePortShape(side, connected, x, y, z)) {
                return side;
            }
        }

        return null;
    }

    private static boolean insidePortShape(
            Direction side,
            boolean connected,
            double x,
            double y,
            double z
    ) {
        return switch (side) {
            case NORTH -> connected
                    ? inside(x, y, z, 6, 6, 0, 10, 10, 5)
                    : inside(x, y, z, 6, 6, 4, 10, 10, 5);

            case SOUTH -> connected
                    ? inside(x, y, z, 6, 6, 11, 10, 10, 16)
                    : inside(x, y, z, 6, 6, 11, 10, 10, 12);

            case WEST -> connected
                    ? inside(x, y, z, 0, 6, 6, 5, 10, 10)
                    : inside(x, y, z, 4, 6, 6, 5, 10, 10);

            case EAST -> connected
                    ? inside(x, y, z, 11, 6, 6, 16, 10, 10)
                    : inside(x, y, z, 11, 6, 6, 12, 10, 10);

            case UP -> connected
                    ? inside(x, y, z, 6, 11, 6, 10, 16, 10)
                    : inside(x, y, z, 6, 11, 6, 10, 12, 10);

            case DOWN -> connected
                    ? inside(x, y, z, 6, 0, 6, 10, 5, 10)
                    : inside(x, y, z, 6, 4, 6, 10, 5, 10);
        };
    }

    private Direction getCoreSide(double x, double y, double z) {
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

    private static boolean inside(
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
        double px = 1.0D / 16.0D;

        return x >= fromX * px && x <= toX * px
                && y >= fromY * px && y <= toY * px
                && z >= fromZ * px && z <= toZ * px;
    }

    @Override
    public boolean canConnectLogic(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction side,
            LogicCableColor cableColor
    ) {
        if (!(level.getBlockEntity(pos) instanceof LogicGateBlockEntity gate)) {
            return false;
        }

        return gate.canCableConnect(side, cableColor);
    }

    public static boolean isSideConnectedToCable(
            BlockGetter level,
            BlockPos pos,
            Direction side
    ) {
        BlockPos cablePos = pos.relative(side);
        BlockState cableState = level.getBlockState(cablePos);

        if (!(cableState.getBlock() instanceof LogicCableBlock)) {
            return false;
        }

        BooleanProperty prop = LogicCableBlock.prop(side.getOpposite());

        return cableState.hasProperty(prop) && cableState.getValue(prop);
    }

    public static LogicCableColor getRenderColorForSide(
            BlockGetter level,
            BlockPos pos,
            Direction side
    ) {
        BlockPos cablePos = pos.relative(side);
        BlockState cableState = level.getBlockState(cablePos);

        if (cableState.getBlock() instanceof LogicCableBlock
                && cableState.hasProperty(LogicCableBlock.COLOR)) {
            return cableState.getValue(LogicCableBlock.COLOR);
        }

        if (level.getBlockEntity(pos) instanceof LogicGateBlockEntity gate) {
            return gate.getPortColor(side);
        }

        return LogicCableColor.RED;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return buildShape(level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return buildShape(level, pos);
    }

    private VoxelShape buildShape(BlockGetter level, BlockPos pos) {
        VoxelShape shape = CORE;

        if (!(level.getBlockEntity(pos) instanceof LogicGateBlockEntity gate)) {
            return shape;
        }

        for (Direction direction : Direction.values()) {
            if (!gate.hasVisiblePort(direction)) {
                continue;
            }

            if (isSideConnectedToCable(level, pos, direction)) {
                shape = Shapes.or(shape, ARMS.get(direction));
            } else {
                shape = Shapes.or(shape, STUBS.get(direction));
            }
        }

        return shape;
    }
}