package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.command.argument.AngleArgumentType;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SpawnPointCommand {
   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("spawnpoint")
                  .requires(source -> source.hasPermissionLevel(2)))
               .executes(
                  context -> execute(
                     (ServerCommandSource)context.getSource(),
                     Collections.singleton(((ServerCommandSource)context.getSource()).getPlayerOrThrow()),
                     BlockPos.ofFloored(((ServerCommandSource)context.getSource()).getPosition()),
                     0.0F
                  )
               ))
            .then(
               ((RequiredArgumentBuilder)CommandManager.argument("targets", EntityArgumentType.players())
                     .executes(
                        context -> execute(
                           (ServerCommandSource)context.getSource(),
                           EntityArgumentType.getPlayers(context, "targets"),
                           BlockPos.ofFloored(((ServerCommandSource)context.getSource()).getPosition()),
                           0.0F
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                           .executes(
                              context -> execute(
                                 (ServerCommandSource)context.getSource(),
                                 EntityArgumentType.getPlayers(context, "targets"),
                                 BlockPosArgumentType.getValidBlockPos(context, "pos"),
                                 0.0F
                              )
                           ))
                        .then(
                           CommandManager.argument("angle", AngleArgumentType.angle())
                              .executes(
                                 context -> execute(
                                    (ServerCommandSource)context.getSource(),
                                    EntityArgumentType.getPlayers(context, "targets"),
                                    BlockPosArgumentType.getValidBlockPos(context, "pos"),
                                    AngleArgumentType.getAngle(context, "angle")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int execute(ServerCommandSource source, Collection<ServerPlayerEntity> targets, BlockPos pos, float angle) {
      RegistryKey<World> registryKey = source.getWorld().getRegistryKey();

      for (ServerPlayerEntity serverPlayerEntity : targets) {
         serverPlayerEntity.setSpawnPoint(registryKey, pos, angle, true, false);
      }

      String string = registryKey.getValue().toString();
      if (targets.size() == 1) {
         source.sendFeedback(
            () -> Text.translatable(
               "commands.spawnpoint.success.single", pos.getX(), pos.getY(), pos.getZ(), angle, string, targets.iterator().next().getDisplayName()
            ),
            true
         );
      } else {
         source.sendFeedback(
            () -> Text.translatable("commands.spawnpoint.success.multiple", pos.getX(), pos.getY(), pos.getZ(), angle, string, targets.size()), true
         );
      }

      return targets.size();
   }
}
