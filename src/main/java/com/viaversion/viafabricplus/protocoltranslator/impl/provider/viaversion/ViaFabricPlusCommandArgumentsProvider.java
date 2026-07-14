package com.viaversion.viafabricplus.protocoltranslator.impl.provider.viaversion;

import com.viaversion.viaversion.api.minecraft.signature.SignableCommandArgumentsProvider;
import com.viaversion.viaversion.util.Pair;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.argument.SignedArgumentList;

public final class ViaFabricPlusCommandArgumentsProvider extends SignableCommandArgumentsProvider {
   public List<Pair<String, String>> getSignableArguments(String command) {
      ClientPlayNetworkHandler network = MinecraftClient.getInstance().getNetworkHandler();
      return network != null
         ? SignedArgumentList.of(network.getCommandDispatcher().parse(command, network.getCommandSource()))
            .arguments()
            .stream()
            .map(function -> new Pair<String, String>(function.getNodeName(), function.value()))
            .toList()
         : Collections.emptyList();
   }
}
