package com.nvidia.developer.opengl.utils;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

public final class Holder<T> {
    private T value;

    public Holder(){}

    public Holder(T value) { this.value = value;}

    public T set(T value) {
        T old = this.value;
        this.value = value;
        return old;
    }

    public T get() { return value;}
    public boolean isNull() { return value == null;}
}
