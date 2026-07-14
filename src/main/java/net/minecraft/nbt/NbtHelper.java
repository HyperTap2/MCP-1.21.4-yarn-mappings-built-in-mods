package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.visitor.NbtOrderedStringFormatter;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class NbtHelper {
   private static final Comparator<NbtList> BLOCK_POS_COMPARATOR = Comparator.<NbtList>comparingInt(nbt -> nbt.getInt(1))
      .thenComparingInt(nbt -> nbt.getInt(0))
      .thenComparingInt(nbt -> nbt.getInt(2));
   private static final Comparator<NbtList> ENTITY_POS_COMPARATOR = Comparator.<NbtList>comparingDouble(nbt -> nbt.getDouble(1))
      .thenComparingDouble(nbt -> nbt.getDouble(0))
      .thenComparingDouble(nbt -> nbt.getDouble(2));
   public static final String DATA_KEY = "data";
   private static final char LEFT_CURLY_BRACKET = '{';
   private static final char RIGHT_CURLY_BRACKET = '}';
   private static final String COMMA = ",";
   private static final char COLON = ':';
   private static final Splitter COMMA_SPLITTER = Splitter.on(",");
   private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int field_33229 = 2;
   private static final int field_33230 = -1;

   private NbtHelper() {
   }

   @VisibleForTesting
   public static boolean matches(@Nullable NbtElement standard, @Nullable NbtElement subject, boolean ignoreListOrder) {
      if (standard == subject) {
         return true;
      }

      if (standard == null) {
         return true;
      }

      if (subject == null) {
         return false;
      }

      if (!standard.getClass().equals(subject.getClass())) {
         return false;
      }

      if (standard instanceof NbtCompound nbtCompound) {
         NbtCompound nbtCompound2 = (NbtCompound)subject;
         if (nbtCompound2.getSize() < nbtCompound.getSize()) {
            return false;
         }

         for (String string : nbtCompound.getKeys()) {
            NbtElement nbtElement = nbtCompound.get(string);
            if (!matches(nbtElement, nbtCompound2.get(string), ignoreListOrder)) {
               return false;
            }
         }

         return true;
      } else if (standard instanceof NbtList nbtList && ignoreListOrder) {
         NbtList nbtList2 = (NbtList)subject;
         if (nbtList.isEmpty()) {
            return nbtList2.isEmpty();
         }

         if (nbtList2.size() < nbtList.size()) {
            return false;
         }

         for (NbtElement nbtElement2 : nbtList) {
            boolean bl = false;

            for (NbtElement nbtElement3 : nbtList2) {
               if (matches(nbtElement2, nbtElement3, ignoreListOrder)) {
                  bl = true;
                  break;
               }
            }

            if (!bl) {
               return false;
            }
         }

         return true;
      } else {
         return standard.equals(subject);
      }
   }

   public static NbtIntArray fromUuid(UUID uuid) {
      return new NbtIntArray(Uuids.toIntArray(uuid));
   }

   public static UUID toUuid(NbtElement element) {
      if (element.getNbtType() != NbtIntArray.TYPE) {
         throw new IllegalArgumentException(
            "Expected UUID-Tag to be of type " + NbtIntArray.TYPE.getCrashReportName() + ", but found " + element.getNbtType().getCrashReportName() + "."
         );
      } else {
         int[] is = ((NbtIntArray)element).getIntArray();
         if (is.length != 4) {
            throw new IllegalArgumentException("Expected UUID-Array to be of length 4, but found " + is.length + ".");
         } else {
            return Uuids.toUuid(is);
         }
      }
   }

   public static Optional<BlockPos> toBlockPos(NbtCompound nbt, String key) {
      int[] is = nbt.getIntArray(key);
      return is.length == 3 ? Optional.of(new BlockPos(is[0], is[1], is[2])) : Optional.empty();
   }

   public static NbtElement fromBlockPos(BlockPos pos) {
      return new NbtIntArray(new int[]{pos.getX(), pos.getY(), pos.getZ()});
   }

   public static BlockState toBlockState(RegistryEntryLookup<Block> blockLookup, NbtCompound nbt) {
      if (!nbt.contains("Name", 8)) {
         return Blocks.AIR.getDefaultState();
      }

      Identifier identifier = Identifier.of(nbt.getString("Name"));
      Optional<? extends RegistryEntry<Block>> optional = blockLookup.getOptional(RegistryKey.of(RegistryKeys.BLOCK, identifier));
      if (optional.isEmpty()) {
         return Blocks.AIR.getDefaultState();
      }

      Block block = optional.get().value();
      BlockState blockState = block.getDefaultState();
      if (nbt.contains("Properties", 10)) {
         NbtCompound nbtCompound = nbt.getCompound("Properties");
         StateManager<Block, BlockState> stateManager = block.getStateManager();

         for (String string : nbtCompound.getKeys()) {
            Property<?> property = stateManager.getProperty(string);
            if (property != null) {
               blockState = withProperty(blockState, property, string, nbtCompound, nbt);
            }
         }
      }

      return blockState;
   }

   private static <S extends State<?, S>, T extends Comparable<T>> S withProperty(
      S state, Property<T> property, String key, NbtCompound properties, NbtCompound root
   ) {
      Optional<T> optional = property.parse(properties.getString(key));
      if (optional.isPresent()) {
         return state.with(property, optional.get());
      }

      LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", new Object[]{key, properties.getString(key), root});
      return state;
   }

   public static NbtCompound fromBlockState(BlockState state) {
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putString("Name", Registries.BLOCK.getId(state.getBlock()).toString());
      Map<Property<?>, Comparable<?>> map = state.getEntries();
      if (!map.isEmpty()) {
         NbtCompound nbtCompound2 = new NbtCompound();

         for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
            Property<?> property = entry.getKey();
            nbtCompound2.putString(property.getName(), nameValue(property, entry.getValue()));
         }

         nbtCompound.put("Properties", nbtCompound2);
      }

      return nbtCompound;
   }

   public static NbtCompound fromFluidState(FluidState state) {
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putString("Name", Registries.FLUID.getId(state.getFluid()).toString());
      Map<Property<?>, Comparable<?>> map = state.getEntries();
      if (!map.isEmpty()) {
         NbtCompound nbtCompound2 = new NbtCompound();

         for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
            Property<?> property = entry.getKey();
            nbtCompound2.putString(property.getName(), nameValue(property, entry.getValue()));
         }

         nbtCompound.put("Properties", nbtCompound2);
      }

      return nbtCompound;
   }

   private static <T extends Comparable<T>> String nameValue(Property<T> property, Comparable<?> value) {
      return property.name((T)value);
   }

   public static String toFormattedString(NbtElement nbt) {
      return toFormattedString(nbt, false);
   }

   public static String toFormattedString(NbtElement nbt, boolean withArrayContents) {
      return appendFormattedString(new StringBuilder(), nbt, 0, withArrayContents).toString();
   }

   public static StringBuilder appendFormattedString(StringBuilder stringBuilder, NbtElement nbt, int depth, boolean withArrayContents) {
      switch (nbt.getType()) {
         case 0:
            break;
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 8:
            stringBuilder.append(nbt);
            break;
         case 7:
            NbtByteArray nbtByteArray = (NbtByteArray)nbt;
            byte[] bs = nbtByteArray.getByteArray();
            int i = bs.length;
            appendIndent(depth, stringBuilder).append("byte[").append(i).append("] {\n");
            if (withArrayContents) {
               appendIndent(depth + 1, stringBuilder);

               for (int j = 0; j < bs.length; j++) {
                  if (j != 0) {
                     stringBuilder.append(',');
                  }

                  if (j % 16 == 0 && j / 16 > 0) {
                     stringBuilder.append('\n');
                     if (j < bs.length) {
                        appendIndent(depth + 1, stringBuilder);
                     }
                  } else if (j != 0) {
                     stringBuilder.append(' ');
                  }

                  stringBuilder.append(String.format(Locale.ROOT, "0x%02X", bs[j] & 255));
               }
            } else {
               appendIndent(depth + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            }

            stringBuilder.append('\n');
            appendIndent(depth, stringBuilder).append('}');
            break;
         case 9:
            NbtList nbtList = (NbtList)nbt;
            int k = nbtList.size();
            int b = nbtList.getHeldType();
            String string = b == 0 ? "undefined" : NbtTypes.byId(b).getCommandFeedbackName();
            appendIndent(depth, stringBuilder).append("list<").append(string).append(">[").append(k).append("] [");
            if (k != 0) {
               stringBuilder.append('\n');
            }

            for (int l = 0; l < k; l++) {
               if (l != 0) {
                  stringBuilder.append(",\n");
               }

               appendIndent(depth + 1, stringBuilder);
               appendFormattedString(stringBuilder, nbtList.get(l), depth + 1, withArrayContents);
            }

            if (k != 0) {
               stringBuilder.append('\n');
            }

            appendIndent(depth, stringBuilder).append(']');
            break;
         case 10:
            NbtCompound nbtCompound = (NbtCompound)nbt;
            List<String> list = Lists.newArrayList(nbtCompound.getKeys());
            Collections.sort(list);
            appendIndent(depth, stringBuilder).append('{');
            if (stringBuilder.length() - stringBuilder.lastIndexOf("\n") > 2 * (depth + 1)) {
               stringBuilder.append('\n');
               appendIndent(depth + 1, stringBuilder);
            }

            int ored = list.stream().mapToInt(String::length).max().orElse(0);
            String s = Strings.repeat(" ", ored);

            for (int l = 0; l < list.size(); l++) {
               if (l != 0) {
                  stringBuilder.append(",\n");
               }

               String string2 = list.get(l);
               appendIndent(depth + 1, stringBuilder)
                  .append('"')
                  .append(string2)
                  .append('"')
                  .append(s, 0, s.length() - string2.length())
                  .append(": ");
               appendFormattedString(stringBuilder, nbtCompound.get(string2), depth + 1, withArrayContents);
            }

            if (!list.isEmpty()) {
               stringBuilder.append('\n');
            }

            appendIndent(depth, stringBuilder).append('}');
            break;
         case 11:
            NbtIntArray nbtIntArray = (NbtIntArray)nbt;
            int[] is = nbtIntArray.getIntArray();
            int z = 0;

            for (int m : is) {
               z = Math.max(z, String.format(Locale.ROOT, "%X", m).length());
            }

            int j = is.length;
            appendIndent(depth, stringBuilder).append("int[").append(j).append("] {\n");
            if (withArrayContents) {
               appendIndent(depth + 1, stringBuilder);

               for (int l = 0; l < is.length; l++) {
                  if (l != 0) {
                     stringBuilder.append(',');
                  }

                  if (l % 16 == 0 && l / 16 > 0) {
                     stringBuilder.append('\n');
                     if (l < is.length) {
                        appendIndent(depth + 1, stringBuilder);
                     }
                  } else if (l != 0) {
                     stringBuilder.append(' ');
                  }

                  stringBuilder.append(String.format(Locale.ROOT, "0x%0" + z + "X", is[l]));
               }
            } else {
               appendIndent(depth + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            }

            stringBuilder.append('\n');
            appendIndent(depth, stringBuilder).append('}');
            break;
         case 12:
            NbtLongArray nbtLongArray = (NbtLongArray)nbt;
            long[] ls = nbtLongArray.getLongArray();
            long n = 0L;

            for (long o : ls) {
               n = Math.max(n, String.format(Locale.ROOT, "%X", o).length());
            }

            long p = ls.length;
            appendIndent(depth, stringBuilder).append("long[").append(p).append("] {\n");
            if (withArrayContents) {
               appendIndent(depth + 1, stringBuilder);

               for (int m = 0; m < ls.length; m++) {
                  if (m != 0) {
                     stringBuilder.append(',');
                  }

                  if (m % 16 == 0 && m / 16 > 0) {
                     stringBuilder.append('\n');
                     if (m < ls.length) {
                        appendIndent(depth + 1, stringBuilder);
                     }
                  } else if (m != 0) {
                     stringBuilder.append(' ');
                  }

                  stringBuilder.append(String.format(Locale.ROOT, "0x%0" + n + "X", ls[m]));
               }
            } else {
               appendIndent(depth + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            }

            stringBuilder.append('\n');
            appendIndent(depth, stringBuilder).append('}');
            break;
         default:
            stringBuilder.append("<UNKNOWN :(>");
      }

      return stringBuilder;
   }

   private static StringBuilder appendIndent(int depth, StringBuilder stringBuilder) {
      int i = stringBuilder.lastIndexOf("\n") + 1;
      int j = stringBuilder.length() - i;

      for (int k = 0; k < 2 * depth - j; k++) {
         stringBuilder.append(' ');
      }

      return stringBuilder;
   }

   public static Text toPrettyPrintedText(NbtElement element) {
      return new NbtTextFormatter("").apply(element);
   }

   public static String toNbtProviderString(NbtCompound compound) {
      return new NbtOrderedStringFormatter().apply(toNbtProviderFormat(compound));
   }

   public static NbtCompound fromNbtProviderString(String string) throws CommandSyntaxException {
      return fromNbtProviderFormat(StringNbtReader.parse(string));
   }

   @VisibleForTesting
   static NbtCompound toNbtProviderFormat(NbtCompound compound) {
      boolean bl = compound.contains("palettes", 9);
      NbtList nbtList;
      if (bl) {
         nbtList = compound.getList("palettes", 9).getList(0);
      } else {
         nbtList = compound.getList("palette", 10);
      }

      NbtList nbtList2 = nbtList.stream()
         .map(NbtCompound.class::cast)
         .map(NbtHelper::toNbtProviderFormattedPalette)
         .map(NbtString::of)
         .collect(Collectors.toCollection(NbtList::new));
      compound.put("palette", nbtList2);
      if (bl) {
         NbtList nbtList3 = new NbtList();
         NbtList nbtList4 = compound.getList("palettes", 9);
         nbtList4.stream().map(NbtList.class::cast).forEach(nbt -> {
            NbtCompound nbtCompound = new NbtCompound();

            for (int i = 0; i < nbt.size(); i++) {
               nbtCompound.putString(nbtList2.getString(i), toNbtProviderFormattedPalette(nbt.getCompound(i)));
            }

            nbtList3.add(nbtCompound);
         });
         compound.put("palettes", nbtList3);
      }

      if (compound.contains("entities", 9)) {
         NbtList nbtList3 = compound.getList("entities", 10);
         NbtList nbtList4 = nbtList3.stream()
            .map(NbtCompound.class::cast)
            .sorted(Comparator.comparing(nbt -> nbt.getList("pos", 6), ENTITY_POS_COMPARATOR))
            .collect(Collectors.toCollection(NbtList::new));
         compound.put("entities", nbtList4);
      }

      NbtList nbtList3 = compound.getList("blocks", 10)
         .stream()
         .map(NbtCompound.class::cast)
         .sorted(Comparator.comparing(nbt -> nbt.getList("pos", 3), BLOCK_POS_COMPARATOR))
         .peek(nbt -> nbt.putString("state", nbtList2.getString(nbt.getInt("state"))))
         .collect(Collectors.toCollection(NbtList::new));
      compound.put("data", nbtList3);
      compound.remove("blocks");
      return compound;
   }

   @VisibleForTesting
   static NbtCompound fromNbtProviderFormat(NbtCompound compound) {
      NbtList nbtList = compound.getList("palette", 8);
      Map<String, NbtElement> map = nbtList.stream()
         .map(NbtString.class::cast)
         .map(NbtString::asString)
         .collect(ImmutableMap.toImmutableMap(Function.identity(), NbtHelper::fromNbtProviderFormattedPalette));
      if (compound.contains("palettes", 9)) {
         compound.put(
            "palettes",
            compound.getList("palettes", 10)
               .stream()
               .map(NbtCompound.class::cast)
               .map(
                  nbt -> map.keySet()
                     .stream()
                     .map(nbt::getString)
                     .map(NbtHelper::fromNbtProviderFormattedPalette)
                     .collect(Collectors.toCollection(NbtList::new))
               )
               .collect(Collectors.toCollection(NbtList::new))
         );
         compound.remove("palette");
      } else {
         compound.put("palette", map.values().stream().collect(Collectors.toCollection(NbtList::new)));
      }

      if (compound.contains("data", 9)) {
         Object2IntMap<String> object2IntMap = new Object2IntOpenHashMap();
         object2IntMap.defaultReturnValue(-1);

         for (int i = 0; i < nbtList.size(); i++) {
            object2IntMap.put(nbtList.getString(i), i);
         }

         NbtList nbtList2 = compound.getList("data", 10);

         for (int j = 0; j < nbtList2.size(); j++) {
            NbtCompound nbtCompound = nbtList2.getCompound(j);
            String string = nbtCompound.getString("state");
            int k = object2IntMap.getInt(string);
            if (k == -1) {
               throw new IllegalStateException("Entry " + string + " missing from palette");
            }

            nbtCompound.putInt("state", k);
         }

         compound.put("blocks", nbtList2);
         compound.remove("data");
      }

      return compound;
   }

   @VisibleForTesting
   static String toNbtProviderFormattedPalette(NbtCompound compound) {
      StringBuilder stringBuilder = new StringBuilder(compound.getString("Name"));
      if (compound.contains("Properties", 10)) {
         NbtCompound nbtCompound = compound.getCompound("Properties");
         String string = nbtCompound.getKeys().stream().sorted().map(key -> key + ":" + nbtCompound.get(key).asString()).collect(Collectors.joining(","));
         stringBuilder.append('{').append(string).append('}');
      }

      return stringBuilder.toString();
   }

   @VisibleForTesting
   static NbtCompound fromNbtProviderFormattedPalette(String string) {
      NbtCompound nbtCompound = new NbtCompound();
      int i = string.indexOf(123);
      String string2;
      if (i >= 0) {
         string2 = string.substring(0, i);
         NbtCompound nbtCompound2 = new NbtCompound();
         if (i + 2 <= string.length()) {
            String string3 = string.substring(i + 1, string.indexOf(125, i));
            COMMA_SPLITTER.split(string3).forEach(property -> {
               List<String> list = COLON_SPLITTER.splitToList(property);
               if (list.size() == 2) {
                  nbtCompound2.putString(list.get(0), list.get(1));
               } else {
                  LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", string);
               }
            });
            nbtCompound.put("Properties", nbtCompound2);
         }
      } else {
         string2 = string;
      }

      nbtCompound.putString("Name", string2);
      return nbtCompound;
   }

   public static NbtCompound putDataVersion(NbtCompound nbt) {
      int i = SharedConstants.getGameVersion().getSaveVersion().getId();
      return putDataVersion(nbt, i);
   }

   public static NbtCompound putDataVersion(NbtCompound nbt, int dataVersion) {
      nbt.putInt("DataVersion", dataVersion);
      return nbt;
   }

   public static int getDataVersion(NbtCompound nbt, int fallback) {
      return nbt.contains("DataVersion", 99) ? nbt.getInt("DataVersion") : fallback;
   }
}
