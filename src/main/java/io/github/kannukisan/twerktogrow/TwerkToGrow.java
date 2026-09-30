package io.github.kannukisan.twerktogrow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class TwerkToGrow implements ModInitializer {
    private static final int RADIUS = 3;
    private static final int VERTICAL_RADIUS = 3;
    private static final int CROUCHES_PER_GROWTH = 3;
    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });
    }

    private static void tickPlayer(ServerPlayer player) {
        PlayerState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
        boolean crouching = player.isCrouching();

        if (crouching && !state.wasCrouching) {
            state.crouches++;
            if (state.crouches >= CROUCHES_PER_GROWTH) {
                state.crouches = 0;
                growNearby(player);
            }
        }
        state.wasCrouching = crouching;
    }

    private static void growNearby(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos center = player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-RADIUS, -VERTICAL_RADIUS, -RADIUS),
            center.offset(RADIUS, VERTICAL_RADIUS, RADIUS)
        )) {
            BlockState blockState = level.getBlockState(pos);
            if (!(blockState.getBlock() instanceof BonemealableBlock growable)) {
                continue;
            }
            if (!growable.isValidBonemealTarget(level, pos, blockState)) {
                continue;
            }
            if (level.random.nextFloat() > 0.35F) {
                continue;
            }
            if (growable.isBonemealSuccess(level, level.random, pos, blockState)) {
                growable.performBonemeal(level, level.random, pos, blockState);
                level.levelEvent(1505, pos, 0);
            }
        }
    }

    private static final class PlayerState {
        private boolean wasCrouching;
        private int crouches;
    }
}
