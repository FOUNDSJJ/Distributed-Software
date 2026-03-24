package com.example.order.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OrderIdGenerator {

    private static final long EPOCH_MILLIS = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    private static final long MACHINE_ID = 1L;
    private static final long SEQUENCE_MASK = (1L << 12) - 1;

    private final AtomicLong sequence = new AtomicLong(0L);
    private volatile long lastTimestamp = -1L;

    public synchronized long nextId() {
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp < lastTimestamp) {
            currentTimestamp = lastTimestamp;
        }

        if (currentTimestamp == lastTimestamp) {
            long nextSequence = (sequence.incrementAndGet()) & SEQUENCE_MASK;
            if (nextSequence == 0L) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence.set(0L);
        }

        lastTimestamp = currentTimestamp;
        return ((currentTimestamp - EPOCH_MILLIS) << 22)
                | (MACHINE_ID << 12)
                | (sequence.get() & SEQUENCE_MASK);
    }

    private long waitNextMillis(long currentTimestamp) {
        long now = System.currentTimeMillis();
        while (now <= currentTimestamp) {
            now = System.currentTimeMillis();
        }
        sequence.set(0L);
        return now;
    }
}
