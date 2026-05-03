package me.testblocks.api;

public interface DelegatingBlockState {
    Object blockState();
    void setBlockState(Object state);
}
