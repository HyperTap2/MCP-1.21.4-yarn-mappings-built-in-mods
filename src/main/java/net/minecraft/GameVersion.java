package net.minecraft;

import java.util.Date;
import net.minecraft.resource.ResourceType;

public interface GameVersion {
   SaveVersion getSaveVersion();

   String getId();

   String getName();

   int getProtocolVersion();

   int getResourceVersion(ResourceType type);

   Date getBuildTime();

   boolean isStable();
}
