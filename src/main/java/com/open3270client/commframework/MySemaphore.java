/*
 * Open3270Client - Java / Spring Boot port of Open3270
 * Original C# library: https://github.com/Open3270/Open3270
 * Original authors: Mike Warriner, Francois Botha
 * Original license: MIT
 *
 * Java port copyright (c) 2026 Ivanlopezmolina
 * Released under the MIT License.
 */
package com.open3270client.commframework;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A counting semaphore with a configurable initial count and a hard maximum.
 * Supports {@link #reset()} to restore the semaphore to its initial count
 * — equivalent to the C# {@code MySemaphore} in {@code CommFramework/Semaphore.cs}.
 */
public class MySemaphore {

    private final int initialCount;
    private final int maxCount;
    private int count;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notZero = lock.newCondition();

    public MySemaphore(int maxCount) {
        this(maxCount, maxCount);
    }

    public MySemaphore(int initialCount, int maxCount) {
        if (initialCount < 0)
            throw new IllegalArgumentException("initialCount must be >= 0");
        if (maxCount < 1)
            throw new IllegalArgumentException("maxCount must be >= 1");
        if (initialCount > maxCount)
            throw new IllegalArgumentException("initialCount must be <= maxCount");
        this.initialCount = initialCount;
        this.maxCount = maxCount;
        this.count = initialCount;
    }

    /** Returns the current available count. */
    public int getCount() {
        lock.lock();
        try { return count; } finally { lock.unlock(); }
    }

    /** Returns the maximum count set at construction. */
    public int getMaxCount() { return maxCount; }

    /**
     * Resets the semaphore to its initial count and wakes all waiting threads.
     */
    public void reset() {
        lock.lock();
        try {
            count = initialCount;
            notZero.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Acquires one slot, blocking indefinitely until available.
     *
     * @return {@code true} (always — blocks until successful)
     */
    public boolean acquire() {
        return acquire(-1);
    }

    /**
     * Acquires one slot, waiting at most {@code timeoutMs} milliseconds.
     *
     * @param timeoutMs timeout in milliseconds; use {@code -1} for indefinite wait
     * @return {@code true} if the slot was acquired; {@code false} on timeout
     */
    public boolean acquire(int timeoutMs) {
        lock.lock();
        try {
            while (count == 0) {
                try {
                    if (timeoutMs < 0) {
                        notZero.await();
                    } else {
                        if (!notZero.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                            return false;
                        }
                    }
                } catch (InterruptedException e) {
                    // Regenerate signal to avoid starving other waiters, then rethrow
                    notZero.signal();
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Semaphore.acquire() interrupted", e);
                }
            }
            count--;
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases {@code releaseCount} slots and wakes waiting threads.
     *
     * @param releaseCount number of slots to release (must be >= 1)
     */
    public void release(int releaseCount) {
        if (releaseCount < 1)
            throw new IllegalArgumentException("releaseCount must be >= 1");
        lock.lock();
        try {
            if (count + releaseCount > maxCount)
                throw new IllegalStateException("Release would exceed maxCount");
            count += releaseCount;
            notZero.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** Releases one slot. */
    public void release() {
        release(1);
    }
}
