package me.testblocks.util;

public class ObjectHolder<T> {
    private T value;

    public ObjectHolder() {}

    public ObjectHolder(T value) {
        this.value = value;
    }

    public T value() {
        return value;
    }

    public void bindValue(T value) {
        this.value = value;
    }
}
