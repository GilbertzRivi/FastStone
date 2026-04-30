package net.oktawia.faststone.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.oktawia.faststone.logic.LogicCableColor;
import net.oktawia.faststone.logic.network.LogicNetworkGraph;
import net.oktawia.faststone.logic.interfaces.LogicConnectable;

import java.util.Map;

public class LogicBusBlock extends Block implements LogicConnectable {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);

    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5),
            Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16),
            Direction.WEST,  Block.box(0, 5, 5, 5, 11, 11),
            Direction.EAST,  Block.box(11, 5, 5, 16, 11, 11),
            Direction.UP,    Block.box(5, 11, 5, 11, 16, 11),
            Direction.DOWN,  Block.box(5, 0, 5, 11, 5, 11)
    );

    public LogicBusBlock(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockGetter level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        BlockState state = this.defaultBlockState();

        for (Direction dir : Direction.values()) {
            state = state.setValue(prop(dir), canConnectTo(level, pos, dir));
        }

        return state;
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
        BlockState newState = state.setValue(
                prop(direction),
                canConnectTo(level, currentPos, direction)
        );

        if (newState != state && level instanceof Level realLevel && !realLevel.isClientSide) {
            LogicNetworkGraph.scheduleRebuildAround(realLevel, currentPos);
        }

        return newState;
    }

    private boolean canConnectTo(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos otherPos = pos.relative(dir);
        BlockState otherState = level.getBlockState(otherPos);
        Block otherBlock = otherState.getBlock();

        if (otherBlock instanceof LogicBusBlock) {
            return true;
        }

        if (otherBlock instanceof LogicCableBlock) {
            return !LogicCableBlock.hasPartAt(level, otherPos, dir.getOpposite());
        }

        return false;
    }

    @Override
    public boolean canConnectLogic(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction side,
            LogicCableColor cableColor
    ) {
        return true;
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
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return buildShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return buildShape(state);
    }

    private VoxelShape buildShape(BlockState state) {
        VoxelShape shape = CORE;

        for (Direction dir : Direction.values()) {
            if (state.getValue(prop(dir))) {
                shape = Shapes.or(shape, ARMS.get(dir));
            }
        }

        return shape;
    }

    public static BooleanProperty prop(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }
}