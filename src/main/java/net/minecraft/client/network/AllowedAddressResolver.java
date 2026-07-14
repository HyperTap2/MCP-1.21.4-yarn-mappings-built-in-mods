package net.minecraft.client.network;

import com.google.common.annotations.VisibleForTesting;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Optional;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;

public class AllowedAddressResolver {
   public static final AllowedAddressResolver DEFAULT = new AllowedAddressResolver(
      AddressResolver.DEFAULT, RedirectResolver.createSrv(), BlockListChecker.create()
   );
   private final AddressResolver addressResolver;
   private final RedirectResolver redirectResolver;
   private final BlockListChecker blockListChecker;

   @VisibleForTesting
   AllowedAddressResolver(AddressResolver addressResolver, RedirectResolver redirectResolver, BlockListChecker blockListChecker) {
      this.addressResolver = addressResolver;
      this.redirectResolver = redirectResolver;
      this.blockListChecker = blockListChecker;
   }

   public Optional<Address> resolve(ServerAddress address) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)
         || ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
         return this.addressResolver.resolve(address);
      }
      Optional<Address> optional = this.addressResolver.resolve(address);
      if ((!optional.isPresent() || this.blockListChecker.isAllowed(optional.get())) && this.blockListChecker.isAllowed(address)) {
         Optional<ServerAddress> optional2 = this.redirectResolver.lookupRedirect(address);
         if (optional2.isPresent()) {
            optional = this.addressResolver.resolve(optional2.get()).filter(this.blockListChecker::isAllowed);
         }

         return optional;
      } else {
         return Optional.empty();
      }
   }

   Optional<ServerAddress> lookupRedirect(ServerAddress address) {
      return this.redirectResolver.lookupRedirect(address);
   }
}
