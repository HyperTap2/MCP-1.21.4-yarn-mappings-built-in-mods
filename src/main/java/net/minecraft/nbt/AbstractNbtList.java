package net.minecraft.nbt;

import java.util.AbstractList;

public abstract class AbstractNbtList<T extends NbtElement> extends AbstractList<T> implements NbtElement {
   public abstract T set(int i, T nbtElement);

   public abstract void add(int i, T nbtElement);

   public abstract T remove(int i);

   public abstract boolean setElement(int index, NbtElement element);

   public abstract boolean addElement(int index, NbtElement element);

   public abstract byte getHeldType();
}
