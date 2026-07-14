package com.viaversion.viafabricplus.features.networking.legacy_chat_signature;

import com.mojang.authlib.yggdrasil.response.KeyPairResponse.KeyPair;
import java.nio.ByteBuffer;

public record KeyPairResponse1_19_0(KeyPair keyPair, ByteBuffer publicKeySignatureV2, ByteBuffer publicKeySignature, String expiresAt, String refreshedAfter) {
}
