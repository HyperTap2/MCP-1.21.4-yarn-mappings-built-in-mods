package net.minecraft.client.network;

import java.util.Map;
import net.minecraft.util.Identifier;

public record CookieStorage(Map<Identifier, byte[]> cookies) {
}
