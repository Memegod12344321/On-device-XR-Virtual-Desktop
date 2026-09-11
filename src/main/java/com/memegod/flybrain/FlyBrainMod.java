package com.memegod.flybrain;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FlyBrainMod implements ModInitializer {
    private static final Map<UUID, NeuralState> BRAINS = new LinkedHashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!isFly(entity)) return true;
            NeuralState state = BRAINS.computeIfAbsent(entity.getUuid(), id -> new NeuralState());
            state.stimulus = Math.max(state.stimulus, Math.min(1.0f, amount / 2.0f));
            state.activateNociception();
            ServerWorld world = (ServerWorld) entity.getWorld();
            world.getServer().getPlayerList().broadcastSystemMessage(
                    Text.literal("[FlyBrain] nociception-like stimulus -> " + state.activeSummary()), false);
            return false; // simulation only: the fly takes no actual Minecraft damage
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> BRAINS.entrySet().removeIf(entry -> {
            Entity entity = server.overworld().getEntity(entry.getKey());
            if (entity == null || !entity.isAlive()) return true;
            entry.getValue().tick();
            return false;
        }));
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("spawnfly").requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> spawnFly(ctx.getSource())));
        dispatcher.register(CommandManager.literal("flybrain").requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> reportBrains(ctx.getSource())));
    }

    private static int spawnFly(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        BlockPos pos = BlockPos.containing(source.getPosition()).up();
        BatEntity fly = EntityType.BAT.spawn(world, pos, SpawnReason.COMMAND);
        if (fly == null) {
            source.sendError(Text.literal("Could not spawn the simulated fly."));
            return 0;
        }
        fly.setCustomName(Text.literal("Drosophila melanogaster • Connectome"));
        fly.setCustomNameVisible(true);
        fly.addCommandTag("fruit_fly_connectome");
        BRAINS.put(fly.getUuid(), new NeuralState());
        source.sendSuccess(() -> Text.literal("Spawned simulated Drosophila. Hit it to trigger the neural model."), true);
        return 1;
    }

    private static int reportBrains(ServerCommandSource source) {
        if (BRAINS.isEmpty()) {
            source.sendSuccess(() -> Text.literal("No simulated fly brains are active."), false);
            return 0;
        }
        for (NeuralState state : BRAINS.values()) {
            source.sendSuccess(() -> Text.literal("Fly brain: stimulus=" + String.format("%.2f", state.stimulus)
                    + " active=" + state.activeSummary()), false);
        }
        return BRAINS.size();
    }

    private static boolean isFly(Entity entity) {
        return entity instanceof BatEntity && entity.getCommandTags().contains("fruit_fly_connectome");
    }

    private static final class NeuralState {
        private float stimulus;
        private final Map<String, Float> neurons = new LinkedHashMap<>();

        void activateNociception() {
            neurons.put("nociceptive sensory", stimulus);
            neurons.put("mechanosensory relay", stimulus * 0.85f);
            neurons.put("central complex", stimulus * 0.65f);
            neurons.put("escape motor", stimulus * 0.95f);
            neurons.put("descending modulation", stimulus * 0.55f);
        }

        void tick() {
            stimulus *= 0.94f;
            if (stimulus < 0.01f) stimulus = 0;
            if (stimulus == 0) neurons.clear();
        }

        String activeSummary() {
            if (neurons.isEmpty()) return "none";
            List<String> active = new ArrayList<>();
            for (Map.Entry<String, Float> e : neurons.entrySet()) {
                if (e.getValue() > 0.05f) active.add(e.getKey() + "=" + String.format("%.2f", e.getValue()));
            }
            return active.isEmpty() ? "none" : String.join(", ", active);
        }
    }
}
