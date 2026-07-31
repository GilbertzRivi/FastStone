package net.oktawia.faststone.logic.parts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.oktawia.faststone.Faststone;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class LogicPartModelShapes {

    private static final double EPSILON = 0.0001D;

    private static final Map<LogicCablePartType, Map<Direction, PartShape>> SHAPES =
            new EnumMap<>(LogicCablePartType.class);

    static {
        SHAPES.put(
                LogicCablePartType.INPUT,
                buildDirectionalShapes(loadModelBoxes("logic_input_part"))
        );

        SHAPES.put(
                LogicCablePartType.OUTPUT,
                buildDirectionalShapes(loadModelBoxes("logic_output_part"))
        );

        SHAPES.put(
                LogicCablePartType.DISPLAY,
                buildDirectionalShapes(loadModelBoxes("logic_display_part"))
        );
    }

    private LogicPartModelShapes() {
    }

    public static VoxelShape getShape(LogicCablePartType type, Direction side) {
        if (type == LogicCablePartType.NONE) {
            return Shapes.empty();
        }

        Map<Direction, PartShape> directionalShapes = SHAPES.get(type);

        if (directionalShapes == null) {
            return Shapes.empty();
        }

        PartShape shape = directionalShapes.get(side);

        if (shape == null) {
            return Shapes.empty();
        }

        return shape.voxelShape();
    }

    public static boolean contains(
            LogicCablePartType type,
            Direction side,
            double localX,
            double localY,
            double localZ
    ) {
        if (type == LogicCablePartType.NONE) {
            return false;
        }

        Map<Direction, PartShape> directionalShapes = SHAPES.get(type);

        if (directionalShapes == null) {
            return false;
        }

        PartShape shape = directionalShapes.get(side);

        if (shape == null) {
            return false;
        }

        double x = localX * 16.0D;
        double y = localY * 16.0D;
        double z = localZ * 16.0D;

        for (Box box : shape.boxes()) {
            if (box.contains(x, y, z)) {
                return true;
            }
        }

        return false;
    }

    private static Map<Direction, PartShape> buildDirectionalShapes(List<Box> northBoxes) {
        Map<Direction, PartShape> result = new EnumMap<>(Direction.class);

        for (Direction side : Direction.values()) {
            List<Box> rotatedBoxes = new ArrayList<>();

            for (Box box : northBoxes) {
                rotatedBoxes.add(rotateBox(box, side));
            }

            result.put(side, new PartShape(rotatedBoxes, buildVoxelShape(rotatedBoxes)));
        }

        return result;
    }

    private static VoxelShape buildVoxelShape(List<Box> boxes) {
        VoxelShape shape = Shapes.empty();

        for (Box box : boxes) {
            shape = Shapes.or(
                    shape,
                    Block.box(
                            box.minX(),
                            box.minY(),
                            box.minZ(),
                            box.maxX(),
                            box.maxY(),
                            box.maxZ()
                    )
            );
        }

        return shape.optimize();
    }

    private static List<Box> loadModelBoxes(String modelName) {
        String path = "assets/"
                + Faststone.MODID
                + "/models/block/part/"
                + modelName
                + ".json";

        try (InputStream stream = LogicPartModelShapes.class
                .getClassLoader()
                .getResourceAsStream(path)) {

            if (stream == null) {
                return fallbackBoxes();
            }

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray elements = root.getAsJsonArray("elements");

                if (elements == null || elements.isEmpty()) {
                    return fallbackBoxes();
                }

                List<Box> boxes = new ArrayList<>();

                for (JsonElement element : elements) {
                    JsonObject object = element.getAsJsonObject();

                    JsonArray from = object.getAsJsonArray("from");
                    JsonArray to = object.getAsJsonArray("to");

                    if (from == null || to == null) {
                        continue;
                    }

                    boxes.add(new Box(
                            from.get(0).getAsDouble(),
                            from.get(1).getAsDouble(),
                            from.get(2).getAsDouble(),
                            to.get(0).getAsDouble(),
                            to.get(1).getAsDouble(),
                            to.get(2).getAsDouble()
                    ).normalized());
                }

                if (boxes.isEmpty()) {
                    return fallbackBoxes();
                }

                return boxes;
            }
        } catch (Exception ignored) {
            return fallbackBoxes();
        }
    }

    private static List<Box> fallbackBoxes() {
        return List.of(new Box(6, 6, 2, 10, 10, 6));
    }

    private static Box rotateBox(Box box, Direction side) {
        double[][] corners = {
                {box.minX(), box.minY(), box.minZ()},
                {box.minX(), box.minY(), box.maxZ()},
                {box.minX(), box.maxY(), box.minZ()},
                {box.minX(), box.maxY(), box.maxZ()},
                {box.maxX(), box.minY(), box.minZ()},
                {box.maxX(), box.minY(), box.maxZ()},
                {box.maxX(), box.maxY(), box.minZ()},
                {box.maxX(), box.maxY(), box.maxZ()}
        };

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;

        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (double[] corner : corners) {
            double[] rotated = rotatePoint(side, corner[0], corner[1], corner[2]);

            minX = Math.min(minX, rotated[0]);
            minY = Math.min(minY, rotated[1]);
            minZ = Math.min(minZ, rotated[2]);

            maxX = Math.max(maxX, rotated[0]);
            maxY = Math.max(maxY, rotated[1]);
            maxZ = Math.max(maxZ, rotated[2]);
        }

        return new Box(minX, minY, minZ, maxX, maxY, maxZ).normalized();
    }

    private static double[] rotatePoint(Direction side, double x, double y, double z) {
        return switch (side) {
            case NORTH -> new double[]{x, y, z};
            case SOUTH -> new double[]{16.0D - x, y, 16.0D - z};

            case EAST -> new double[]{16.0D - z, y, x};
            case WEST -> new double[]{z, y, 16.0D - x};

            case UP -> new double[]{x, 16.0D - z, y};
            case DOWN -> new double[]{x, z, 16.0D - y};
        };
    }

    private record PartShape(List<Box> boxes, VoxelShape voxelShape) {
    }

    private record Box(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        private Box normalized() {
            return new Box(
                    Math.min(minX, maxX),
                    Math.min(minY, maxY),
                    Math.min(minZ, maxZ),
                    Math.max(minX, maxX),
                    Math.max(minY, maxY),
                    Math.max(minZ, maxZ)
            );
        }

        private boolean contains(double x, double y, double z) {
            return x >= minX - EPSILON
                    && x <= maxX + EPSILON
                    && y >= minY - EPSILON
                    && y <= maxY + EPSILON
                    && z >= minZ - EPSILON
                    && z <= maxZ + EPSILON;
        }
    }
}