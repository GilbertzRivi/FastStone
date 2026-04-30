package net.oktawia.faststone.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.faststone.defs.regs.BlockRegistrar;
import net.oktawia.faststone.defs.regs.ItemRegistrar;
import net.oktawia.faststone.entities.LogicCableBlockEntity;
import net.oktawia.faststone.items.LogicCableBlockItem;
import net.oktawia.faststone.logic.LogicCableColor;
import net.oktawia.faststone.logic.interfaces.LogicConnectable;
import net.oktawia.faststone.logic.network.LogicNetworkGraph;
import net.oktawia.faststone.logic.parts.LogicCablePartType;
import net.oktawia.faststone.logic.parts.LogicPartModelShapes;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class LogicCableBlock extends Block implements EntityBlock, LogicConnectable {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    public static final EnumProperty<LogicCableColor> COLOR =
            EnumProperty.create("color", LogicCableColor.class);

    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);

    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6),
            Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16),
            Direction.WEST,  Block.box(0, 6, 6, 6, 10, 10),
            Direction.EAST,  Block.box(10, 6, 6, 16, 10, 10),
            Direction.UP,    Block.box(6, 10, 6, 10, 16, 10),
            Direction.DOWN,  Block.box(6, 0, 6, 10, 6, 10)
    );

    public LogicCableBlock(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(COLOR, LogicCableColor.RED)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, COLOR);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogicCableBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockGetter level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        LogicCableColor color = getColorFromPlacedItem(ctx);

        BlockState state = this.defaultBlockState()
                .setValue(COLOR, color);

        for (Direction dir : Direction.values()) {
            state = state.setValue(prop(dir), canConnectTo(state, level, pos, dir));
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
                canConnectTo(state, level, currentPos, direction)
        );

        if (newState != state && level instanceof Level realLevel && !realLevel.isClientSide) {
            LogicNetworkGraph.scheduleRebuildAround(realLevel, currentPos);
        }

        return newState;
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
        if (!(level.getBlockEntity(pos) instanceof LogicCableBlockEntity cable)) {
            return InteractionResult.PASS;
        }

        Direction partSide = getClickedPartSide(cable, pos, hit.getLocation());

        if (partSide == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (!player.isShiftKeyDown() && !held.isEmpty()) {
            return InteractionResult.PASS;
        }

        LogicCablePartType removed = cable.getPart(partSide);

        if (removed == LogicCablePartType.NONE) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            cable.removePart(partSide);
            refreshSideAfterPartChange(level, pos, partSide);

            if (!player.getAbilities().instabuild) {
                popResource(level, pos, getPartItemStack(removed));
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void refreshSideAfterPartChange(Level level, BlockPos pos, Direction side) {
        BlockState oldState = level.getBlockState(pos);

        if (!(oldState.getBlock() instanceof LogicCableBlock)) {
            return;
        }

        BlockState newState = oldState.setValue(
                prop(side),
                canConnectTo(oldState, level, pos, side)
        );

        if (!newState.equals(oldState)) {
            level.setBlock(pos, newState, Block.UPDATE_ALL);
        } else {
            level.sendBlockUpdated(pos, oldState, oldState, Block.UPDATE_ALL);
        }

        BlockState finalState = level.getBlockState(pos);
        Block block = finalState.getBlock();

        forceNeighborShapeUpdate(level, pos, side);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);

            level.updateNeighborsAt(neighborPos, block);
            level.neighborChanged(neighborPos, block, pos);

            if (dir != side) {
                forceNeighborShapeUpdate(level, pos, dir);
            }
        }

        level.updateNeighborsAt(pos, block);
        level.updateNeighbourForOutputSignal(pos, block);

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

    private static LogicCableColor getColorFromPlacedItem(BlockPlaceContext ctx) {
        Item item = ctx.getItemInHand().getItem();

        if (item instanceof LogicCableBlockItem cableItem) {
            return cableItem.getColor();
        }

        return LogicCableColor.RED;
    }

    private boolean canConnectTo(
            BlockState selfState,
            BlockGetter level,
            BlockPos pos,
            Direction dir
    ) {
        if (hasPartAt(level, pos, dir)) {
            return false;
        }

        BlockPos otherPos = pos.relative(dir);
        BlockState otherState = level.getBlockState(otherPos);

        LogicCableColor selfColor = selfState.getValue(COLOR);

        if (otherState.getBlock() instanceof LogicBusBlock) {
            return true;
        }

        if (otherState.getBlock() instanceof LogicCableBlock) {
            return otherState.hasProperty(COLOR)
                    && otherState.getValue(COLOR) == selfColor
                    && !hasPartAt(level, otherPos, dir.getOpposite());
        }

        if (otherState.getBlock() instanceof LogicConnectable connectable) {
            return connectable.canConnectLogic(
                    otherState,
                    level,
                    otherPos,
                    dir.getOpposite(),
                    selfColor
            );
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
        return state.hasProperty(COLOR)
                && state.getValue(COLOR) == cableColor
                && !hasPartAt(level, pos, side);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            @Nullable Direction direction
    ) {
        if (!(level.getBlockEntity(pos) instanceof LogicCableBlockEntity cable)) {
            return false;
        }

        if (direction == null) {
            for (Direction side : Direction.values()) {
                if (isRedstonePart(cable.getPart(side))) {
                    return true;
                }
            }

            return false;
        }

        Direction physicalSide = direction.getOpposite();

        return isRedstonePart(cable.getPart(physicalSide));
    }

    @Override
    public int getSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        if (!(level.getBlockEntity(pos) instanceof LogicCableBlockEntity cable)) {
            return 0;
        }

        Direction physicalSide = direction.getOpposite();

        if (cable.getPart(physicalSide) != LogicCablePartType.OUTPUT) {
            return 0;
        }

        return cable.isRedstoneOutputPowered(physicalSide) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        return getSignal(state, level, pos, direction);
    }

    private boolean isRedstonePart(LogicCablePartType part) {
        return part == LogicCablePartType.INPUT
                || part == LogicCablePartType.OUTPUT;
    }

    @Override
    public ItemStack getCloneItemStack(
            BlockState state,
            HitResult target,
            BlockGetter level,
            BlockPos pos,
            Player player
    ) {
        LogicCableColor color = state.hasProperty(COLOR)
                ? state.getValue(COLOR)
                : LogicCableColor.RED;

        RegistryObject<Item> item = BlockRegistrar.getLogicCableItem(color);

        if (item == null) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item.get());
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return buildShape(state, level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return buildShape(state, level, pos);
    }

    private VoxelShape buildShape(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape shape = CORE;

        for (Direction dir : Direction.values()) {
            if (state.getValue(prop(dir))) {
                shape = Shapes.or(shape, ARMS.get(dir));
            }
        }

        if (level.getBlockEntity(pos) instanceof LogicCableBlockEntity cable) {
            for (Direction dir : Direction.values()) {
                LogicCablePartType part = cable.getPart(dir);

                if (part != LogicCablePartType.NONE) {
                    shape = Shapes.or(shape, LogicPartModelShapes.getShape(part, dir));
                }
            }
        }

        return shape;
    }

    public static boolean hasPartAt(BlockGetter level, BlockPos pos, Direction side) {
        if (!(level.getBlockEntity(pos) instanceof LogicCableBlockEntity cable)) {
            return false;
        }

        return cable.hasPart(side);
    }

    private Direction getClickedPartSide(
            LogicCableBlockEntity cable,
            BlockPos pos,
            Vec3 hitLocation
    ) {
        double x = hitLocation.x - pos.getX();
        double y = hitLocation.y - pos.getY();
        double z = hitLocation.z - pos.getZ();

        for (Direction side : Direction.values()) {
            LogicCablePartType part = cable.getPart(side);

            if (part == LogicCablePartType.NONE) {
                continue;
            }

            if (LogicPartModelShapes.contains(part, side, x, y, z)) {
                return side;
            }
        }

        return null;
    }

    private ItemStack getPartItemStack(LogicCablePartType type) {
        return switch (type) {
            case INPUT -> new ItemStack(ItemRegistrar.LOGIC_INPUT_PART.get());
            case OUTPUT -> new ItemStack(ItemRegistrar.LOGIC_OUTPUT_PART.get());
            case NONE -> ItemStack.EMPTY;
        };
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