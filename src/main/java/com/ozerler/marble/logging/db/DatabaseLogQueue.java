package com.ozerler.marble.logging.db;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DatabaseLogQueue {

    public static final int DEFAULT_CAPACITY = 10_000;
    private static final int HIGH_WATER_MARK = (int) (DEFAULT_CAPACITY * 0.90);

    private static volatile DatabaseLogQueue instance;

    private final BlockingQueue<DatabaseLogEntry> queue;
    private final AtomicLong droppedCount = new AtomicLong(0);

    public DatabaseLogQueue() {
        this.queue = new ArrayBlockingQueue<>(DEFAULT_CAPACITY);
        instance = this;
    }

    public static DatabaseLogQueue getInstance() {
        DatabaseLogQueue current = instance;
        if (current == null) {
            synchronized (DatabaseLogQueue.class) {
                if (instance == null) {
                    instance = new DatabaseLogQueue();
                }
                current = instance;
            }
        }
        return current;
    }

    public boolean enqueue(DatabaseLogEntry entry) {
        if (entry == null) {
            return false;
        }

        int currentSize = queue.size();

        // If above high-water mark, shed debug/trace events
        if (currentSize >= HIGH_WATER_MARK) {
            String level = entry.level();
            if ("DEBUG".equalsIgnoreCase(level) || "TRACE".equalsIgnoreCase(level)) {
                droppedCount.incrementAndGet();
                return false;
            }
        }

        boolean accepted = queue.offer(entry);
        if (!accepted) {
            droppedCount.incrementAndGet();
        }
        return accepted;
    }

    public int drainTo(List<DatabaseLogEntry> batch, int maxElements) {
        return queue.drainTo(batch, maxElements);
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public long getDroppedCount() {
        return droppedCount.get();
    }
}
