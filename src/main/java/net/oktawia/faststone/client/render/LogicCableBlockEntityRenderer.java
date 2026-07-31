package net.oktawia.faststone.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.blocks.LogicCableBlock;
import net.oktawia.faststone.client.FaststoneClientEvents;
import net.oktawia.faststone.client.LogicDisplayClientState;
import net.oktawia.faststone.entities.LogicCableBlockEntity;
import net.oktawia.faststone.logic.LogicCableColor;
import net.oktawia.faststone.logic.parts.LogicCablePartType;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogicCableBlockEntityRenderer implements BlockEntityRenderer<LogicCableBlockEntity> {

    public static volatile boolean cableGlowEnabled = true;

    private static final float UNIT = 1.0F / 16.0F;

    private static final float SURFACE_OFFSET = 0.006F;
    private static final int MAX_SURFACE_ALPHA = 125;
    private static final float MIN_VISIBLE_STRENGTH = 0.015F;

    private static final int NORTH_BIT = 1 << Direction.NORTH.ordinal();
    private static final int SOUTH_BIT = 1 << Direction.SOUTH.ordinal();
    private static final int WEST_BIT = 1 << Direction.WEST.ordinal();
    private static final int EAST_BIT = 1 << Direction.EAST.ordinal();
    private static final int UP_BIT = 1 << Direction.UP.ordinal();
    private static final int DOWN_BIT = 1 << Direction.DOWN.ordinal();

    private static final GlowMesh[] GLOW_MESH_CACHE = new GlowMesh[64];

    private static Level cachedStrengthLevel = null;
    private static long cachedStrengthGameTime = Long.MIN_VALUE;
    private static final Map<BlockPos, Float> VISUAL_STRENGTH_CACHE = new HashMap<>();

    private ModelManager cachedModelManager;
    private BakedModel inputPartModel;
    private BakedModel outputPartModel;
    private BakedModel displayPartModel;

    public LogicCableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            LogicCableBlockEntity cable,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = cable.getBlockState();

        if (!(state.getBlock() instanceof LogicCableBlock)) {
            return;
        }

        boolean hasParts = hasAnyPart(cable);
        float strength = cableGlowEnabled ? getVisualStrengthCached(cable) : 0.0F;
        boolean hasGlow = strength > MIN_VISIBLE_STRENGTH;

        if (!hasParts && !hasGlow) {
            return;
        }

        poseStack.pushPose();

        if (hasParts) {
            renderParts(cable, state, poseStack, buffer, packedLight);
        }

        if (hasGlow) {
            renderCableGlowCached(state, poseStack, buffer, strength);
        }

        poseStack.popPose();
    }

    private boolean hasAnyPart(LogicCableBlockEntity cable) {
        for (Direction side : Direction.values()) {
            if (cable.getPart(side) != LogicCablePartType.NONE) {
                return true;
            }
        }

        return false;
    }

    private float getVisualStrengthCached(LogicCableBlockEntity cable) {
        Level level = cable.getLevel();

        if (level == null) {
            return 0.0F;
        }

        long gameTime = level.getGameTime();

        if (cachedStrengthLevel != level || cachedStrengthGameTime != gameTime) {
            cachedStrengthLevel = level;
            cachedStrengthGameTime = gameTime;
            VISUAL_STRENGTH_CACHE.clear();
        }

        if (cable.isMaster()) {
            return cable.getVisualSignalStrength();
        }

        BlockPos masterPos = cable.getMasterPos();

        if (masterPos == null) {
            return 0.0F;
        }

        Float cached = VISUAL_STRENGTH_CACHE.get(masterPos);

        if (cached != null) {
            return cached;
        }

        float strength = 0.0F;
        BlockEntity be = level.getBlockEntity(masterPos);

        if (be instanceof LogicCableBlockEntity master && master.isMaster()) {
            strength = master.getVisualSignalStrength();
        }

        VISUAL_STRENGTH_CACHE.put(masterPos.immutable(), strength);
        return strength;
    }

    private void renderParts(
            LogicCableBlockEntity cable,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        for (Direction side : Direction.values()) {
            LogicCablePartType part = cable.getPart(side);

            if (part == LogicCablePartType.NONE) {
                continue;
            }

            if (part == LogicCablePartType.INPUT) {
                renderPartModel(
                        state,
                        side,
                        getInputPartModel(),
                        poseStack,
                        buffer,
                        packedLight
                );
                continue;
            }

            if (part == LogicCablePartType.OUTPUT) {
                renderPartModel(
                        state,
                        side,
                        getOutputPartModel(),
                        poseStack,
                        buffer,
                        packedLight
                );
                continue;
            }

            if (part == LogicCablePartType.DISPLAY) {
                renderPartModel(
                        state,
                        side,
                        getDisplayPartModel(),
                        poseStack,
                        buffer,
                        packedLight
                );

                float displayStrength = LogicDisplayClientState.getDisplayStrength(
                        cable.getBlockPos(),
                        side,
                        cable.getDisplayMode(side)
                );

                if (displayStrength > 0.0f) {
                    renderDisplayGlow(state, side, poseStack, buffer, displayStrength);
                }
            }
        }
    }

    private BakedModel getInputPartModel() {
        refreshModelCacheIfNeeded();

        if (inputPartModel == null) {
            inputPartModel = Minecraft.getInstance()
                    .getModelManager()
                    .getModel(FaststoneClientEvents.LOGIC_INPUT_PART_MODEL);
        }

        return inputPartModel;
    }

    private BakedModel getOutputPartModel() {
        refreshModelCacheIfNeeded();

        if (outputPartModel == null) {
            outputPartModel = Minecraft.getInstance()
                    .getModelManager()
                    .getModel(FaststoneClientEvents.LOGIC_OUTPUT_PART_MODEL);
        }

        return outputPartModel;
    }

    private BakedModel getDisplayPartModel() {
        refreshModelCacheIfNeeded();

        if (displayPartModel == null) {
            displayPartModel = Minecraft.getInstance()
                    .getModelManager()
                    .getModel(FaststoneClientEvents.LOGIC_DISPLAY_PART_MODEL);
        }

        return displayPartModel;
    }

    private void refreshModelCacheIfNeeded() {
        ModelManager modelManager = Minecraft.getInstance().getModelManager();

        if (cachedModelManager == modelManager) {
            return;
        }

        cachedModelManager = modelManager;
        inputPartModel = null;
        outputPartModel = null;
        displayPartModel = null;
    }

    private void renderPartModel(
            BlockState state,
            Direction side,
            BakedModel model,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

        poseStack.pushPose();

        rotateNorthModelToSide(poseStack, side);

        minecraft.getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        consumer,
                        state,
                        model,
                        1.0F,
                        1.0F,
                        1.0F,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY
                );

        poseStack.popPose();
    }

    private void rotateNorthModelToSide(PoseStack poseStack, Direction side) {
        poseStack.translate(0.5D, 0.5D, 0.5D);

        switch (side) {
            case NORTH -> {
            }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(270.0F));
        }

        poseStack.translate(-0.5D, -0.5D, -0.5D);
    }

    private void renderDisplayGlow(
            BlockState state,
            Direction side,
            PoseStack poseStack,
            MultiBufferSource buffer,
            float strength
    ) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        LogicCableColor color = state.hasProperty(LogicCableBlock.COLOR)
                ? state.getValue(LogicCableBlock.COLOR)
                : LogicCableColor.RED;

        int rgb = color.getRgb();

        int r = brighten((rgb >> 16) & 255, strength);
        int g = brighten((rgb >> 8) & 255, strength);
        int b = brighten(rgb & 255, strength);
        int alpha = Math.round(180 * strength);

        float min = 0.0F;
        float max = 16.0F * UNIT;
        float low = 0.0F;
        float high = 16.0F * UNIT;

        float offset = 0.009F;
        float ox = side.getStepX() * offset;
        float oy = side.getStepY() * offset;
        float oz = side.getStepZ() * offset;

        switch (side) {
            case NORTH -> quadDoubleSided(
                    consumer,
                    matrix,
                    min + ox, min + oy, low + oz,
                    min + ox, max + oy, low + oz,
                    max + ox, max + oy, low + oz,
                    max + ox, min + oy, low + oz,
                    r,
                    g,
                    b,
                    alpha
            );

            case SOUTH -> quadDoubleSided(
                    consumer,
                    matrix,
                    min + ox, min + oy, high + oz,
                    max + ox, min + oy, high + oz,
                    max + ox, max + oy, high + oz,
                    min + ox, max + oy, high + oz,
                    r,
                    g,
                    b,
                    alpha
            );

            case WEST -> quadDoubleSided(
                    consumer,
                    matrix,
                    low + ox, min + oy, min + oz,
                    low + ox, min + oy, max + oz,
                    low + ox, max + oy, max + oz,
                    low + ox, max + oy, min + oz,
                    r,
                    g,
                    b,
                    alpha
            );

            case EAST -> quadDoubleSided(
                    consumer,
                    matrix,
                    high + ox, min + oy, min + oz,
                    high + ox, max + oy, min + oz,
                    high + ox, max + oy, max + oz,
                    high + ox, min + oy, max + oz,
                    r,
                    g,
                    b,
                    alpha
            );

            case UP -> quadDoubleSided(
                    consumer,
                    matrix,
                    min + ox, high + oy, min + oz,
                    min + ox, high + oy, max + oz,
                    max + ox, high + oy, max + oz,
                    max + ox, high + oy, min + oz,
                    r,
                    g,
                    b,
                    alpha
            );

            case DOWN -> quadDoubleSided(
                    consumer,
                    matrix,
                    min + ox, low + oy, min + oz,
                    max + ox, low + oy, min + oz,
                    max + ox, low + oy, max + oz,
                    min + ox, low + oy, max + oz,
                    r,
                    g,
                    b,
                    alpha
            );
        }
    }

    private void renderCableGlowCached(
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffer,
            float strength
    ) {
        LogicCableColor color = state.getValue(LogicCableBlock.COLOR);
        int rgb = color.getRgb();

        int r = brighten((rgb >> 16) & 255, strength);
        int g = brighten((rgb >> 8) & 255, strength);
        int b = brighten(rgb & 255, strength);

        int alpha = Math.round(MAX_SURFACE_ALPHA * strength);

        if (alpha <= 0) {
            return;
        }

        int mask = connectionMask(state);
        GlowMesh mesh = getGlowMesh(mask);

        if (mesh.quads().isEmpty()) {
            return;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        for (GlowQuad quad : mesh.quads()) {
            renderCachedQuad(
                    consumer,
                    matrix,
                    quad,
                    r,
                    g,
                    b,
                    alpha
            );
        }
    }

    private int connectionMask(BlockState state) {
        int mask = 0;

        if (state.getValue(LogicCableBlock.NORTH)) {
            mask |= NORTH_BIT;
        }

        if (state.getValue(LogicCableBlock.SOUTH)) {
            mask |= SOUTH_BIT;
        }

        if (state.getValue(LogicCableBlock.WEST)) {
            mask |= WEST_BIT;
        }

        if (state.getValue(LogicCableBlock.EAST)) {
            mask |= EAST_BIT;
        }

        if (state.getValue(LogicCableBlock.UP)) {
            mask |= UP_BIT;
        }

        if (state.getValue(LogicCableBlock.DOWN)) {
            mask |= DOWN_BIT;
        }

        return mask;
    }

    private static GlowMesh getGlowMesh(int mask) {
        int index = mask & 63;
        GlowMesh cached = GLOW_MESH_CACHE[index];

        if (cached != null) {
            return cached;
        }

        GlowMesh generated = buildGlowMesh(index);
        GLOW_MESH_CACHE[index] = generated;
        return generated;
    }

    private static GlowMesh buildGlowMesh(int mask) {
        List<GlowQuad> quads = new ArrayList<>();

        boolean north = (mask & NORTH_BIT) != 0;
        boolean south = (mask & SOUTH_BIT) != 0;
        boolean west = (mask & WEST_BIT) != 0;
        boolean east = (mask & EAST_BIT) != 0;
        boolean up = (mask & UP_BIT) != 0;
        boolean down = (mask & DOWN_BIT) != 0;

        addBoxSurface(
                quads,
                6,
                6,
                6,
                10,
                10,
                10,
                !north,
                !south,
                !west,
                !east,
                !up,
                !down
        );

        if (north) {
            addBoxSurface(
                    quads,
                    6,
                    6,
                    0,
                    10,
                    10,
                    6,
                    true,
                    false,
                    true,
                    true,
                    true,
                    true
            );
        }

        if (south) {
            addBoxSurface(
                    quads,
                    6,
                    6,
                    10,
                    10,
                    10,
                    16,
                    false,
                    true,
                    true,
                    true,
                    true,
                    true
            );
        }

        if (west) {
            addBoxSurface(
                    quads,
                    0,
                    6,
                    6,
                    6,
                    10,
                    10,
                    true,
                    true,
                    true,
                    false,
                    true,
                    true
            );
        }

        if (east) {
            addBoxSurface(
                    quads,
                    10,
                    6,
                    6,
                    16,
                    10,
                    10,
                    true,
                    true,
                    false,
                    true,
                    true,
                    true
            );
        }

        if (up) {
            addBoxSurface(
                    quads,
                    6,
                    10,
                    6,
                    10,
                    16,
                    10,
                    true,
                    true,
                    true,
                    true,
                    true,
                    false
            );
        }

        if (down) {
            addBoxSurface(
                    quads,
                    6,
                    0,
                    6,
                    10,
                    6,
                    10,
                    true,
                    true,
                    true,
                    true,
                    false,
                    true
            );
        }

        return new GlowMesh(List.copyOf(quads));
    }

    private static void addBoxSurface(
            List<GlowQuad> quads,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ,
            boolean renderNorth,
            boolean renderSouth,
            boolean renderWest,
            boolean renderEast,
            boolean renderUp,
            boolean renderDown
    ) {
        float minX = fromX * UNIT;
        float minY = fromY * UNIT;
        float minZ = fromZ * UNIT;

        float maxX = toX * UNIT;
        float maxY = toY * UNIT;
        float maxZ = toZ * UNIT;

        if (renderNorth) {
            addQuad(
                    quads,
                    Direction.NORTH,
                    minX,
                    minY,
                    minZ,
                    minX,
                    maxY,
                    minZ,
                    maxX,
                    maxY,
                    minZ,
                    maxX,
                    minY,
                    minZ
            );
        }

        if (renderSouth) {
            addQuad(
                    quads,
                    Direction.SOUTH,
                    minX,
                    minY,
                    maxZ,
                    maxX,
                    minY,
                    maxZ,
                    maxX,
                    maxY,
                    maxZ,
                    minX,
                    maxY,
                    maxZ
            );
        }

        if (renderWest) {
            addQuad(
                    quads,
                    Direction.WEST,
                    minX,
                    minY,
                    minZ,
                    minX,
                    minY,
                    maxZ,
                    minX,
                    maxY,
                    maxZ,
                    minX,
                    maxY,
                    minZ
            );
        }

        if (renderEast) {
            addQuad(
                    quads,
                    Direction.EAST,
                    maxX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ,
                    maxX,
                    minY,
                    maxZ
            );
        }

        if (renderUp) {
            addQuad(
                    quads,
                    Direction.UP,
                    minX,
                    maxY,
                    minZ,
                    minX,
                    maxY,
                    maxZ,
                    maxX,
                    maxY,
                    maxZ,
                    maxX,
                    maxY,
                    minZ
            );
        }

        if (renderDown) {
            addQuad(
                    quads,
                    Direction.DOWN,
                    minX,
                    minY,
                    minZ,
                    maxX,
                    minY,
                    minZ,
                    maxX,
                    minY,
                    maxZ,
                    minX,
                    minY,
                    maxZ
            );
        }
    }

    private static void addQuad(
            List<GlowQuad> quads,
            Direction direction,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4
    ) {
        float ox = direction.getStepX() * SURFACE_OFFSET;
        float oy = direction.getStepY() * SURFACE_OFFSET;
        float oz = direction.getStepZ() * SURFACE_OFFSET;

        GlowQuad front = new GlowQuad(
                x1 + ox,
                y1 + oy,
                z1 + oz,
                x2 + ox,
                y2 + oy,
                z2 + oz,
                x3 + ox,
                y3 + oy,
                z3 + oz,
                x4 + ox,
                y4 + oy,
                z4 + oz
        );

        GlowQuad back = new GlowQuad(
                x4 + ox,
                y4 + oy,
                z4 + oz,
                x3 + ox,
                y3 + oy,
                z3 + oz,
                x2 + ox,
                y2 + oy,
                z2 + oz,
                x1 + ox,
                y1 + oy,
                z1 + oz
        );

        quads.add(front);
        quads.add(back);
    }

    private void renderCachedQuad(
            VertexConsumer consumer,
            Matrix4f matrix,
            GlowQuad quad,
            int r,
            int g,
            int b,
            int alpha
    ) {
        vertex(consumer, matrix, quad.x1(), quad.y1(), quad.z1(), r, g, b, alpha);
        vertex(consumer, matrix, quad.x2(), quad.y2(), quad.z2(), r, g, b, alpha);
        vertex(consumer, matrix, quad.x3(), quad.y3(), quad.z3(), r, g, b, alpha);
        vertex(consumer, matrix, quad.x4(), quad.y4(), quad.z4(), r, g, b, alpha);
    }

    private void quadDoubleSided(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            int r,
            int g,
            int b,
            int alpha
    ) {
        quad(
                consumer,
                matrix,
                x1,
                y1,
                z1,
                x2,
                y2,
                z2,
                x3,
                y3,
                z3,
                x4,
                y4,
                z4,
                r,
                g,
                b,
                alpha
        );

        quad(
                consumer,
                matrix,
                x4,
                y4,
                z4,
                x3,
                y3,
                z3,
                x2,
                y2,
                z2,
                x1,
                y1,
                z1,
                r,
                g,
                b,
                alpha
        );
    }

    private void quad(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            int r,
            int g,
            int b,
            int alpha
    ) {
        vertex(consumer, matrix, x1, y1, z1, r, g, b, alpha);
        vertex(consumer, matrix, x2, y2, z2, r, g, b, alpha);
        vertex(consumer, matrix, x3, y3, z3, r, g, b, alpha);
        vertex(consumer, matrix, x4, y4, z4, r, g, b, alpha);
    }

    private void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            int r,
            int g,
            int b,
            int alpha
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(r, g, b, alpha)
                .endVertex();
    }

    private int brighten(int value, float strength) {
        int boost = Math.round(25.0F * strength);

        if (value == 0) {
            return boost;
        }

        return Math.min(255, value + boost);
    }

    private record GlowMesh(List<GlowQuad> quads) {
    }

    private record GlowQuad(
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4
    ) {
    }
}