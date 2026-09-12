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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FlyBrainMod implements ModInitializer {
    private static final String FLY_TAG = "fruit_fly_connectome";
    private static final Map<UUID, NeuralState> BRAINS = new LinkedHashMap<>();
    private static final Map<UUID, Integer> DAMAGE_COUNTS = new HashMap<>();
    private static final Set<UUID> DANCING = new HashSet<>();
    private static boolean soundFly;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!isFly(entity) || damageTaken <= 0) return;

            NeuralState state = BRAINS.computeIfAbsent(entity.getUuid(), id -> new NeuralState());
            state.stimulus = Math.max(state.stimulus, Math.min(1.0f, damageTaken / 2.0f));
            state.activateNociception();

            int hits = DAMAGE_COUNTS.merge(entity.getUuid(), 1, Integer::sum);
            if (soundFly && entity.getEntityWorld() instanceof ServerWorld world) {
                float pitch = Math.min(2.0f, 0.85f + hits * 0.06f);
                float volume = Math.min(1.5f, 0.25f + hits * 0.05f);
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.ENTITY_BAT_HURT, entity.getSoundCategory(), volume, pitch);
            }

            if (entity.getEntityWorld() instanceof ServerWorld world) {
                Text message = Text.literal("[FlyBrain] simulated nociception stimulus -> " + state.activeSummary());
                world.getPlayers().forEach(player -> player.sendMessage(message, false));
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            BRAINS.entrySet().removeIf(entry -> {
                NeuralState state = entry.getValue();
                state.tick();
                return state.stimulus == 0;
            });

            for (Entity entity : world.iterateEntities()) {
                if (!(entity instanceof BatEntity fly) || !isFly(fly) || !DANCING.contains(fly.getUuid())) continue;
                long t = world.getTime() + fly.getId() * 7L;
                double side = Math.sin(t * 0.45) * 0.08;
                double up = Math.cos(t * 0.70) * 0.055;
                double forward = Math.cos(t * 0.30) * 0.035;
                fly.setVelocity(new Vec3d(forward, up, side));
                fly.setYaw((float) (Math.sin(t * 0.18) * 70.0));
                fly.setPitch((float) (Math.cos(t * 0.25) * 25.0));
                fly.velocityDirty = true;
            }
        });
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("spawnfly")
                .executes(ctx -> spawnFly(ctx.getSource())));

        dispatcher.register(CommandManager.literal("flybrain")
                .executes(ctx -> reportBrains(ctx.getSource())));

        dispatcher.register(CommandManager.literal("soundfly")
                .executes(ctx -> {
                    soundFly = !soundFly;
                    ctx.getSource().sendFeedback(() -> Text.literal("SoundFly " + (soundFly ? "enabled" : "disabled") + "."), true);
                    return 1;
                }));

        dispatcher.register(CommandManager.literal("dancemode")
                .executes(ctx -> toggleDance(ctx.getSource())));
    }

    private static int spawnFly(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        BlockPos pos = BlockPos.ofFloored(source.getPosition()).up();
        BatEntity fly = EntityType.BAT.spawn(world, pos, SpawnReason.COMMAND);
        if (fly == null) {
            source.sendError(Text.literal("Could not spawn the simulated fly."));
            return 0;
        }

        fly.setCustomName(Text.literal("Drosophila melanogaster • Connectome"));
        fly.setCustomNameVisible(true);
        fly.addCommandTag(FLY_TAG);
        fly.setNoGravity(true);
        BRAINS.put(fly.getUuid(), new NeuralState());
        source.sendFeedback(() -> Text.literal("Spawned simulated Drosophila."), true);
        return 1;
    }

    private static int toggleDance(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        Vec3d origin = source.getPosition();
        BatEntity nearest = null;
        double best = 16.0 * 16.0;

        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof BatEntity bat && isFly(bat)) {
                double distance = bat.squaredDistanceTo(origin);
                if (distance < best) {
                    best = distance;
                    nearest = bat;
                }
            }
        }

        if (nearest == null) {
            source.sendError(Text.literal("No simulated fly within 16 blocks."));
            return 0;
        }

        UUID id = nearest.getUuid();
        if (DANCING.add(id)) {
            nearest.setNoGravity(true);
            source.sendFeedback(() -> Text.literal("Dance mode enabled for the nearest simulated fly."), true);
        } else {
            DANCING.remove(id);
            source.sendFeedback(() -> Text.literal("Dance mode disabled."), true);
        }
        return 1;
    }

    private static int reportBrains(ServerCommandSource source) {
        if (BRAINS.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No simulated fly brains are active."), false);
            return 0;
        }

        for (NeuralState state : BRAINS.values()) {
            source.sendFeedback(() -> Text.literal("Fly brain: stimulus=" + String.format("%.2f", state.stimulus)
                    + " active=" + state.activeSummary()), false);
        }
        return BRAINS.size();
    }

    private static boolean isFly(Entity entity) {
        return entity instanceof BatEntity && entity.getCommandTags().contains(FLY_TAG);
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
            for (Map.Entry<String, Float> entry : neurons.entrySet()) {
                if (entry.getValue() > 0.05f) {
                    active.add(entry.getKey() + "=" + String.format("%.2f", entry.getValue()));
                }
            }
            return active.isEmpty() ? "none" : String.join(", ", active);
        }
    }
}
