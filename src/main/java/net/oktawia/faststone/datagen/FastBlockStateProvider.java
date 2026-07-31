package net.oktawia.faststone.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.faststone.Faststone;
import net.oktawia.faststone.blocks.gates.LogicGateBlock;
import net.oktawia.faststone.defs.regs.BlockRegistrar;

public class FastBlockStateProvider extends BlockStateProvider {

    public FastBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Faststone.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (var block : BlockRegistrar.getBlocks()) {
            if (
                    block == BlockRegistrar.LOGIC_CABLE.get()
                    || block == BlockRegistrar.LOGIC_BUS.get()
                    || block instanceof LogicGateBlock
            ) {
                continue;
            }

            DirectionProperty facingProperty = getFacingProperty(block);

            if (facingProperty != null) {
                facingCubeWithFront(block, facingProperty);
            } else {
                simpleBlockWithItem(block);
            }
        }
    }

    private void simpleBlockWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }

    private DirectionProperty getFacingProperty(Block block) {
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            if (property instanceof DirectionProperty directionProperty
                    && property.getName().equals("facing")) {
                return directionProperty;
            }
        }

        return null;
    }

    private void facingCubeWithFront(Block block, DirectionProperty facingProperty) {
        ModelFile model = cubeWithFrontTexture(block);

        getVariantBuilder(block)
                .partialState()
                .with(facingProperty, Direction.NORTH)
                .modelForState()
                .modelFile(model)
                .addModel()

                .partialState()
                .with(facingProperty, Direction.SOUTH)
                .modelForState()
                .modelFile(model)
                .rotationY(180)
                .addModel()

                .partialState()
                .with(facingProperty, Direction.EAST)
                .modelForState()
                .modelFile(model)
                .rotationY(90)
                .addModel()

                .partialState()
                .with(facingProperty, Direction.WEST)
                .modelForState()
                .modelFile(model)
                .rotationY(270)
                .addModel()

                .partialState()
                .with(facingProperty, Direction.UP)
                .modelForState()
                .modelFile(model)
                .rotationX(270)
                .addModel()

                .partialState()
                .with(facingProperty, Direction.DOWN)
                .modelForState()
                .modelFile(model)
                .rotationX(90)
                .addModel();

        simpleBlockItem(block, model);
    }

    private BlockModelBuilder cubeWithFrontTexture(Block block) {
        String name = ForgeRegistries.BLOCKS.getKey(block).getPath();

        return models()
                .cube(
                        name,
                        modLoc("block/" + name),
                        modLoc("block/" + name),
                        modLoc("block/" + name + "_front"),
                        modLoc("block/" + name),
                        modLoc("block/" + name),
                        modLoc("block/" + name)
                )
                .texture("particle", modLoc("block/" + name));
    }
}