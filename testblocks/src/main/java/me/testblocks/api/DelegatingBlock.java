package me.testblocks.api;

/**
 * Basic interface for our delegating block.
 */
public interface DelegatingBlock {
    Object getBehavior();
    void setBehavior(Object behavior);
}
