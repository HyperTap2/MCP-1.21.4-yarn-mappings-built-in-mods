package net.minecraft.client.realms.dto;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.minecraft.client.realms.RealmsSerializable;

public class PingResult extends ValueObject implements RealmsSerializable {
   @SerializedName("pingResults")
   public List<RegionPingResult> pingResults = Lists.newArrayList();
   @SerializedName("worldIds")
   public List<Long> worldIds = Lists.newArrayList();
}
