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
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.blocks.LogicCableBlock;
import net.oktawia.faststone.client.FaststoneClientEvents;
import net.oktawia.faststone.entities.LogicCableBlockEntity;
import net.oktawia.faststone.logic.LogicCableColor;
import net.oktawia.faststone.logic.parts.LogicCablePartType;
import org.joml.Matrix4f;

public class LogicCableBlockEntityRenderer implements BlockEntityRenderer<LogicCableBlockEntity> {

    private static final float UNIT = 1.0F / 16.0F;

    private static final float SURFACE_OFFSET = 0.006F;
    private static final int MAX_SURFACE_ALPHA = 125;
    private static final float MIN_VISIBLE_STRENGTH = 0.015F;

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

        poseStack.pushPose();

        renderParts(cable, state, poseStack, buffer, packedLight);

        float strength = cable.getVisualSignalStrength();

        if (strength > MIN_VISIBLE_STRENGTH) {
            renderCableGlow(state, poseStack, buffer, strength);
        }

        poseStack.popPose();
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
                        FaststoneClientEvents.LOGIC_INPUT_PART_MODEL,
                        poseStack,
                        buffer,
                        packedLight
                );
            }

            if (part == LogicCablePartType.OUTPUT) {
                renderPartModel(
                        state,
                        side,
                        FaststoneClientEvents.LOGIC_OUTPUT_PART_MODEL,
                        poseStack,
                        buffer,
                        packedLight
                );
            }
        }
    }

    private void renderPartModel(
            BlockState state,
            Direction side,
            net.minecraft.resources.ResourceLocation modelLocation,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ModelManager modelManager = minecraft.getModelManager();
        BakedModel model = modelManager.getModel(modelLocation);

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

    private void renderCableGlow(
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffer,
            float strength
    ) {
        boolean[][][] voxels = buildCableVoxels(state);

        LogicCableColor color = state.getValue(LogicCableBlock.COLOR);
        int rgb = color.getRgb();

        int r = brighten((rgb >> 16) & 255, strength);
        int g = brighten((rgb >> 8) & 255, strength);
        int b = brighten(rgb & 255, strength);

        int alpha = Math.round(MAX_SURFACE_ALPHA * strength);

        if (alpha <= 0) {
            return;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        renderVoxelSurface(
                voxels,
                consumer,
                matrix,
                r,
                g,
                b,
                alpha,
                SURFACE_OFFSET
        );
    }

    private boolean[][][] buildCableVoxels(BlockState state) {
        boolean[][][] voxels = new boolean[16][16][16];

        fillBox(voxels, 6, 6, 6, 10, 10, 10);

        if (state.getValue(LogicCableBlock.NORTH)) {
            fillBox(voxels, 6, 6, 0, 10, 10, 6);
        }

        if (state.getValue(LogicCableBlock.SOUTH)) {
            fillBox(voxels, 6, 6, 10, 10, 10, 16);
        }

        if (state.getValue(LogicCableBlock.WEST)) {
            fillBox(voxels, 0, 6, 6, 6, 10, 10);
        }

        if (state.getValue(LogicCableBlock.EAST)) {
            fillBox(voxels, 10, 6, 6, 16, 10, 10);
        }

        if (state.getValue(LogicCableBlock.UP)) {
            fillBox(voxels, 6, 10, 6, 10, 16, 10);
        }

        if (state.getValue(LogicCableBlock.DOWN)) {
            fillBox(voxels, 6, 0, 6, 10, 6, 10);
        }

        return voxels;
    }

    private void fillBox(
            boolean[][][] voxels,
            int fromX,
            int fromY,
            int fromZ,
            int toX,
            int toY,
            int toZ
    ) {
        for (int x = fromX; x < toX; x++) {
            for (int y = fromY; y < toY; y++) {
                for (int z = fromZ; z < toZ; z++) {
                    voxels[x][y][z] = true;
                }
            }
        }
    }

    private void renderVoxelSurface(
            boolean[][][] voxels,
            VertexConsumer consumer,
            Matrix4f matrix,
            int r,
            int g,
            int b,
            int alpha,
            float offset
    ) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    if (!voxels[x][y][z]) {
                        continue;
                    }

                    for (Direction direction : Direction.values()) {
                        if (isExposed(voxels, x, y, z, direction)) {
                            renderVoxelFace(
                                    consumer,
                                    matrix,
                                    x,
                                    y,
                                    z,
                                    direction,
                                    r,
                                    g,
                                    b,
                                    alpha,
                                    offset
                            );
                        }
                    }
                }
            }
        }
    }

    private boolean isExposed(
            boolean[][][] voxels,
            int x,
            int y,
            int z,
            Direction direction
    ) {
        int nx = x + direction.getStepX();
        int ny = y + direction.getStepY();
        int nz = z + direction.getStepZ();

        if (nx < 0 || nx >= 16) {
            return true;
        }

        if (ny < 0 || ny >= 16) {
            return true;
        }

        if (nz < 0 || nz >= 16) {
            return true;
        }

        return !voxels[nx][ny][nz];
    }

    private void renderVoxelFace(
            VertexConsumer consumer,
            Matrix4f matrix,
            int x,
            int y,
            int z,
            Direction direction,
            int r,
            int g,
            int b,
            int alpha,
            float offset
    ) {
        float minX = x * UNIT;
        float minY = y * UNIT;
        float minZ = z * UNIT;

        float maxX = minX + UNIT;
        float maxY = minY + UNIT;
        float maxZ = minZ + UNIT;

        float ox = direction.getStepX() * offset;
        float oy = direction.getStepY() * offset;
        float oz = direction.getStepZ() * offset;

        switch (direction) {
            case NORTH -> quadDoubleSided(
                    consumer,
                    matrix,
                    minX + ox, minY + oy, minZ + oz,
                    minX + ox, maxY + oy, minZ + oz,
                    maxX + ox, maxY + oy, minZ + oz,
                    maxX + ox, minY + oy, minZ + oz,
                    r,
                    g,
                    b,
                    alpha
            );

            case SOUTH -> quadDoubleSided(
                    consumer,
                    matrix,
                    minX + ox, minY + oy, maxZ + oz,
                    maxX + ox, minY + oy, maxZ + oz,
                    maxX + ox, maxY + oy, maxZ + oz,
                    minX + ox, maxY + oy, maxZ + oz,
                    r,
                    g,
                    b,
                    alpha
            );

            case WEST -> quadDoubleSided(
                    consumer,
                    matrix,
                    minX + ox, minY + oy, minZ + oz,
                    minX + ox, minY + oy, maxZ + oz,
                    minX + ox, maxY + oy, maxZ + oz,
                    minX + ox, maxY + oy, minZ + oz,
                    r,
                    g,
                    b,
                    alpha
            );

            case EAST -> quadDoubleSided(
                    consumer,
                    matrix,
                    maxX + ox, minY + oy, minZ + oz,
                    maxX + ox, maxY + oy, minZ + oz,
                    maxX + ox, maxY + oy, maxZ + oz,
                    maxX + ox, minY + oy, maxZ + oz,
                    r,
                    g,
                    b,
                    alpha
            );

            case UP -> quadDoubleSided(
                    consumer,
                    matrix,
                    minX + ox, maxY + oy, minZ + oz,
                    minX + ox, maxY + oy, maxZ + oz,
                    maxX + ox, maxY + oy, maxZ + oz,
                    maxX + ox, maxY + oy, minZ + oz,
                    r,
                    g,
                    b,
                    alpha
            );

            case DOWN -> quadDoubleSided(
                    consumer,
                    matrix,
                    minX + ox, minY + oy, minZ + oz,
                    maxX + ox, minY + oy, minZ + oz,
                    maxX + ox, minY + oy, maxZ + oz,
                    minX + ox, minY + oy, maxZ + oz,
                    r,
                    g,
                    b,
                    alpha
            );
        }
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
                x1, y1, z1,
                x2, y2, z2,
                x3, y3, z3,
                x4, y4, z4,
                r,
                g,
                b,
                alpha
        );

        quad(
                consumer,
                matrix,
                x4, y4, z4,
                x3, y3, z3,
                x2, y2, z2,
                x1, y1, z1,
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
}