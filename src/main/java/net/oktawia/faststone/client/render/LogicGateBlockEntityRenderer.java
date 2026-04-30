package net.oktawia.faststone.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.blocks.gates.LogicGateBlock;
import net.oktawia.faststone.entities.gates.LogicGateBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class LogicGateBlockEntityRenderer implements BlockEntityRenderer<LogicGateBlockEntity> {

    private static final float UNIT = 1.0F / 16.0F;

    private static final net.minecraft.resources.ResourceLocation PORT_TEXTURE =
            Faststone.makeId("block/gate/logic_gate_port");

    public LogicGateBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            LogicGateBlockEntity gate,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = gate.getBlockState();

        if (!(state.getBlock() instanceof LogicGateBlock)) {
            return;
        }

        BlockGetter level = gate.getLevel();

        if (level == null) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(PORT_TEXTURE);

        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());

        poseStack.pushPose();

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        for (Direction side : Direction.values()) {
            if (!gate.hasVisiblePort(side)) {
                continue;
            }

            boolean connected = LogicGateBlock.isSideConnectedToCable(
                    level,
                    gate.getBlockPos(),
                    side
            );

            int rgb = gate.getPortRenderColor(side);

            int r = (rgb >> 16) & 255;
            int g = (rgb >> 8) & 255;
            int b = rgb & 255;

            renderPort(
                    side,
                    connected,
                    consumer,
                    matrix,
                    normal,
                    sprite,
                    packedLight,
                    r,
                    g,
                    b
            );
        }

        poseStack.popPose();
    }

    private void renderPort(
            Direction side,
            boolean connected,
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            TextureAtlasSprite sprite,
            int packedLight,
            int r,
            int g,
            int b
    ) {
        switch (side) {
            case NORTH -> {
                if (connected) {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 6, 6, 0, 10, 10, 5, r, g, b);
                } else {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 6, 6, 4, 10, 10, 5, r, g, b);
                }
            }

            case SOUTH -> {
                if (connected) {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 6, 6, 11, 10, 10, 16, r, g, b);
                } else {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 6, 6, 11, 10, 10, 12, r, g, b);
                }
            }

            case WEST -> {
                if (connected) {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 0, 6, 6, 5, 10, 10, r, g, b);
                } else {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 4, 6, 6, 5, 10, 10, r, g, b);
                }
            }

            case EAST -> {
                if (connected) {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 11, 6, 6, 16, 10, 10, r, g, b);
                } else {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 11, 6, 6, 12, 10, 10, r, g, b);
                }
            }

            case UP -> {
                if (connected) {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 6, 11, 6, 10, 16, 10, r, g, b);
                } else {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 6, 11, 6, 10, 12, 10, r, g, b);
                }
            }

            case DOWN -> {
                if (connected) {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 6, 0, 6, 10, 5, 10, r, g, b);
                } else {
                    renderBox16(consumer, matrix, normal, sprite, packedLight, 6, 4, 6, 10, 5, 10, r, g, b);
                }
            }
        }
    }

    private void renderBox16(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            TextureAtlasSprite sprite,
            int packedLight,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ,
            int r,
            int g,
            int b
    ) {
        float minX = fromX * UNIT;
        float minY = fromY * UNIT;
        float minZ = fromZ * UNIT;

        float maxX = toX * UNIT;
        float maxY = toY * UNIT;
        float maxZ = toZ * UNIT;

        renderNorthFace(consumer, matrix, normal, sprite, packedLight, minX, minY, minZ, maxX, maxY, r, g, b);
        renderSouthFace(consumer, matrix, normal, sprite, packedLight, minX, minY, maxZ, maxX, maxY, r, g, b);
        renderWestFace(consumer, matrix, normal, sprite, packedLight, minX, minY, minZ, maxY, maxZ, r, g, b);
        renderEastFace(consumer, matrix, normal, sprite, packedLight, maxX, minY, minZ, maxY, maxZ, r, g, b);
        renderUpFace(consumer, matrix, normal, sprite, packedLight, minX, maxY, minZ, maxX, maxZ, r, g, b);
        renderDownFace(consumer, matrix, normal, sprite, packedLight, minX, minY, minZ, maxX, maxZ, r, g, b);
    }

    private void renderNorthFace(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            TextureAtlasSprite sprite,
            int packedLight,
            float minX,
            float minY,
            float z,
            float maxX,
            float maxY,
            int r,
            int g,
            int b
    ) {
        quad(
                consumer,
                matrix,
                normal,
                sprite,
                packedLight,
                Direction.NORTH,
                minX, minY, z,
                minX, maxY, z,
                maxX, maxY, z,
                maxX, minY, z,
                r,
                g,
                b
        );
    }

    private void renderSouthFace(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            TextureAtlasSprite sprite,
            int packedLight,
            float minX,
            float minY,
            float z,
            float maxX,
            float maxY,
            int r,
            int g,
            int b
    ) {
        quad(
                consumer,
                matrix,
                normal,
                sprite,
                packedLight,
                Direction.SOUTH,
                minX, minY, z,
                maxX, minY, z,
                maxX, maxY, z,
                minX, maxY, z,
                r,
                g,
                b
        );
    }

    private void renderWestFace(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            TextureAtlasSprite sprite,
            int packedLight,
            float x,
            float minY,
            float minZ,
            float maxY,
            float maxZ,
            int r,
            int g,
            int b
    ) {
        quad(
                consumer,
                matrix,
                normal,
                sprite,
                packedLight,
                Direction.WEST,
                x, minY, minZ,
                x, minY, maxZ,
                x, maxY, maxZ,
                x, maxY, minZ,
                r,
                g,
                b
        );
    }

    private void renderEastFace(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            TextureAtlasSprite sprite,
            int packedLight,
            float x,
            float minY,
            float minZ,
            float maxY,
            float maxZ,
            int r,
            int g,
            int b
    ) {
        quad(
                consumer,
                matrix,
                normal,
                sprite,
                packedLight,
                Direction.EAST,
                x, minY, minZ,
                x, maxY, minZ,
                x, maxY, maxZ,
                x, minY, maxZ,
                r,
                g,
                b
        );
    }

    private void renderUpFace(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            TextureAtlasSprite sprite,
            int packedLight,
            float minX,
            float y,
            float minZ,
            float maxX,
            float maxZ,
            int r,
            int g,
            int b
    ) {
        quad(
                consumer,
                matrix,
                normal,
                sprite,
                packedLight,
                Direction.UP,
                minX, y, minZ,
                minX, y, maxZ,
                maxX, y, maxZ,
                maxX, y, minZ,
                r,
                g,
                b
        );
    }

    private void renderDownFace(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            TextureAtlasSprite sprite,
            int packedLight,
            float minX,
            float y,
            float minZ,
            float maxX,
            float maxZ,
            int r,
            int g,
            int b
    ) {
        quad(
                consumer,
                matrix,
                normal,
                sprite,
                packedLight,
                Direction.DOWN,
                minX, y, minZ,
                maxX, y, minZ,
                maxX, y, maxZ,
                minX, y, maxZ,
                r,
                g,
                b
        );
    }

    private void quad(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            TextureAtlasSprite sprite,
            int packedLight,
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
            float z4,
            int r,
            int g,
            int b
    ) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float nx = direction.getStepX();
        float ny = direction.getStepY();
        float nz = direction.getStepZ();

        vertex(consumer, matrix, normal, x1, y1, z1, u0, v0, nx, ny, nz, packedLight, r, g, b);
        vertex(consumer, matrix, normal, x2, y2, z2, u1, v0, nx, ny, nz, packedLight, r, g, b);
        vertex(consumer, matrix, normal, x3, y3, z3, u1, v1, nx, ny, nz, packedLight, r, g, b);
        vertex(consumer, matrix, normal, x4, y4, z4, u0, v1, nx, ny, nz, packedLight, r, g, b);
    }

    private void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            float x,
            float y,
            float z,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            int packedLight,
            int r,
            int g,
            int b
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(r, g, b, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}