package me.pepperbell.continuity.client.properties;

import java.util.Properties;

import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;

public class ConnectingCtmProperties extends BasicConnectingCtmProperties {
	protected boolean innerSeams = false;

	public ConnectingCtmProperties(Properties properties, Identifier resourceId, ResourcePack pack, int packPriority, ResourceManager resourceManager, String method) {
		super(properties, resourceId, pack, packPriority, resourceManager, method);
	}

	@Override
	public void init() {
		super.init();
		parseInnerSeams();
	}

	protected void parseInnerSeams() {
		String innerSeamsStr = properties.getProperty("innerSeams");
		if (innerSeamsStr == null) {
			return;
		}

		innerSeams = Boolean.parseBoolean(innerSeamsStr.trim());
	}

	public boolean getInnerSeams() {
		return innerSeams;
	}
}
