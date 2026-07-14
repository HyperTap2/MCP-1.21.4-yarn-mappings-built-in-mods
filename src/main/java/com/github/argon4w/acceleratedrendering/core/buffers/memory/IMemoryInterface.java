package com.github.argon4w.acceleratedrendering.core.buffers.memory;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

public interface IMemoryInterface {

    void putByte(long address, byte value);

    void putByte(long address, int index, byte value);

    void putShort(long address, short value);

    void putShort(long address, int index, short value);

    void putInt(long address, int value);

    void putInt(long address, int index, int value);

    void putFloat(long address, float value);

    void putFloat(long address, int index, float value);

    void putNormal(long address, float value);

    void putNormal(long address, int index, float value);

    void putMatrix4f(long address, Matrix4f value);

    void putMatrix4f(long address, int index, Matrix4f value);

    void putMatrix3f(long address, Matrix3f value);

    void putMatrix3f(long address, int index, Matrix3f value);

    IMemoryInterface at(int index);
}
