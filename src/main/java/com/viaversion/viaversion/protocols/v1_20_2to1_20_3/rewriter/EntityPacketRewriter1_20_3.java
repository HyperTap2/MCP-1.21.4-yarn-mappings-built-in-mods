/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2025 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.viaversion.viaversion.protocols.v1_20_2to1_20_3.rewriter;

import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_3;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundPacket1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.Protocol1_20_2To1_20_3;
import com.viaversion.viaversion.rewriter.EntityRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.Key;

public final class EntityPacketRewriter1_20_3 extends EntityRewriter<ClientboundPacket1_20_2, Protocol1_20_2To1_20_3> {
   public EntityPacketRewriter1_20_3(Protocol1_20_2To1_20_3 protocol) {
      super(protocol);
   }

   @Override
   public void registerPackets() {
      this.registerTrackerWithData1_19(ClientboundPackets1_20_2.ADD_ENTITY, EntityTypes1_20_3.FALLING_BLOCK);
      this.registerSetEntityData(ClientboundPackets1_20_2.SET_ENTITY_DATA, Types1_20_2.ENTITY_DATA_LIST, Types1_20_3.ENTITY_DATA_LIST);
      this.registerRemoveEntities(ClientboundPackets1_20_2.REMOVE_ENTITIES);
      this.protocol.registerClientbound(ClientboundConfigurationPackets1_20_2.REGISTRY_DATA, new PacketHandlers() {
         @Override
         protected void register() {
            this.map(Types.COMPOUND_TAG);
            this.handler(EntityPacketRewriter1_20_3.this.configurationDimensionDataHandler());
            this.handler(EntityPacketRewriter1_20_3.this.configurationBiomeSizeTracker());
         }
      });
      this.protocol.registerClientbound(ClientboundPackets1_20_2.LOGIN, new PacketHandlers() {
         @Override
         public void register() {
            this.map(Types.INT);
            this.map(Types.BOOLEAN);
            this.map(Types.STRING_ARRAY);
            this.map(Types.VAR_INT);
            this.map(Types.VAR_INT);
            this.map(Types.VAR_INT);
            this.map(Types.BOOLEAN);
            this.map(Types.BOOLEAN);
            this.map(Types.BOOLEAN);
            this.map(Types.STRING);
            this.map(Types.STRING);
            this.handler(EntityPacketRewriter1_20_3.this.worldDataTrackerHandlerByKey());
            this.handler(EntityPacketRewriter1_20_3.this::sendChunksSentGameEvent);
         }
      });
      this.protocol.registerClientbound(ClientboundPackets1_20_2.RESPAWN, new PacketHandlers() {
         @Override
         public void register() {
            this.map(Types.STRING);
            this.map(Types.STRING);
            this.handler(EntityPacketRewriter1_20_3.this.worldDataTrackerHandlerByKey());
            this.handler(EntityPacketRewriter1_20_3.this::sendChunksSentGameEvent);
         }
      });
      this.protocol.registerClientbound(ClientboundPackets1_20_2.INITIALIZE_BORDER, this::sendChunksSentGameEvent);
   }

   private void sendChunksSentGameEvent(PacketWrapper wrapper) {
   }

   @Override
   protected void registerRewrites() {
      this.filter().handler((event, data) -> {
         EntityDataType type = data.dataType();
         if (type == Types1_20_2.ENTITY_DATA_TYPES.componentType) {
            data.setTypeAndValue(Types1_20_3.ENTITY_DATA_TYPES.componentType, ComponentUtil.jsonToTag(data.value()));
         } else if (type == Types1_20_2.ENTITY_DATA_TYPES.optionalComponentType) {
            data.setTypeAndValue(Types1_20_3.ENTITY_DATA_TYPES.optionalComponentType, ComponentUtil.jsonToTag(data.value()));
         } else {
            data.setDataType(Types1_20_3.ENTITY_DATA_TYPES.byId(type.typeId()));
         }
      });
      this.filter().dataType(Types1_20_3.ENTITY_DATA_TYPES.particleType).handler((event, data) -> {
         Particle particle = data.value();
         ParticleMappings particleMappings = this.protocol.getMappingData().getParticleMappings();
         if (particle.id() == particleMappings.id("vibration")) {
            String resourceLocation = particle.<String>removeArgument(0).getValue();
            if (Key.stripMinecraftNamespace(resourceLocation).equals("block")) {
               particle.add(0, Types.VAR_INT, 0);
            } else {
               particle.add(0, Types.VAR_INT, 1);
            }
         }
      });
      this.registerEntityDataTypeHandler(
         Types1_20_3.ENTITY_DATA_TYPES.itemType,
         Types1_20_3.ENTITY_DATA_TYPES.blockStateType,
         Types1_20_3.ENTITY_DATA_TYPES.optionalBlockStateType,
         Types1_20_3.ENTITY_DATA_TYPES.particleType,
         null
      );
      this.registerBlockStateHandler(EntityTypes1_20_3.ABSTRACT_MINECART, 11);
   }

   @Override
   public void onMappingDataLoaded() {
      this.mapTypes();
   }

   @Override
   public EntityType typeFromId(int type) {
      return EntityTypes1_20_3.getTypeFromId(type);
   }
}
