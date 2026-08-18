package com.sigma.blockgrabber;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockGrabberMod implements ModInitializer {
    private static final String BLOCK_ID = "block_id";

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND || world.isClient || !player.isSneaking()) return ActionResult.PASS;
            ItemStack stack = player.getMainHandStack();
            if (!stack.isOf(Items.STICK)) return ActionResult.PASS;

            BlockPos pos = hit.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (state.isAir() || state.isOf(Blocks.BEDROCK)) return ActionResult.PASS;

            var nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
            nbt.putString(BLOCK_ID, Registries.BLOCK.getId(state.getBlock()).toString());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            return ActionResult.SUCCESS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != Hand.MAIN_HAND || world.isClient || !player.isSneaking()) return ActionResult.PASS;
            ItemStack stack = player.getMainHandStack();
            if (!stack.isOf(Items.STICK)) return ActionResult.PASS;

            var data = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (data == null) return ActionResult.PASS;
            var nbt = data.copyNbt();
            if (!nbt.contains(BLOCK_ID)) return ActionResult.PASS;

            Identifier id = Identifier.tryParse(nbt.getString(BLOCK_ID).orElse(""));
            if (id == null || !Registries.BLOCK.containsId(id)) return ActionResult.PASS;

            BlockState state = Registries.BLOCK.get(id).getDefaultState();
            var spawnPos = player.getEyePos().add(player.getRotationVector().multiply(1.25));
            FallingBlockEntity entity = FallingBlockEntity.spawnFromBlock(world,
                    BlockPos.ofFloored(spawnPos), state);
            entity.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
            entity.setVelocity(player.getRotationVector().multiply(0.9));
            stack.remove(DataComponentTypes.CUSTOM_DATA);
            return ActionResult.SUCCESS;
        });
    }
}
