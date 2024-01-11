package io.collective;

import java.time.Clock;
import java.time.Instant;

public class SimpleAgedCache {

    private final Clock clock;
    private ExpirableEntry[] cache;
    private int size;

    public SimpleAgedCache(Clock clock) {
        this.clock = clock;
        this.cache = new ExpirableEntry[10];
        this.size = 0;
    }

    public SimpleAgedCache() {
        this(Clock.systemDefaultZone());
    }

    public void put(Object key, Object value, int retentionInMillis) {
        Instant expirationTime = clock.instant().plusMillis(retentionInMillis);

        // Remove expired entries before adding a new one
        removeExpiredEntries();

        ensureCapacity();
        cache[size++] = new ExpirableEntry(key, value, expirationTime);
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        // Remove expired entries before checking the size
        removeExpiredEntries();
        return size;
    }

    public Object get(Object key) {
        return getExpired(key);
    }

    public Object getExpired(Object key) {
        Instant currentTime = clock.instant();
        int newSize = 0;

        // Copy non-expired entries to a new array
        for (int i = 0; i < size; i++) {
            ExpirableEntry entry = cache[i];
            if (entry.hasExpired(currentTime)) {
                continue;
            }
            cache[newSize++] = entry;
        }

        // Clear remaining entries after newSize
        for (int i = newSize; i < size; i++) {
            cache[i] = null;
        }

        int originalSize = size;
        size = newSize; // Update the size

        // Find the non-expired entry
        for (int i = 0; i < size; i++) {
            ExpirableEntry entry = cache[i];
            if (entry.key().equals(key)) {
                return entry.value();
            }
        }

        // If the size did not change, the entry was not found, return null
        if (originalSize == size) {
            return null;
        }

        return getExpired(key); // Recursive call to handle concurrent modifications
    }

    private void ensureCapacity() {
        if (size == cache.length) {
            ExpirableEntry[] newCache = new ExpirableEntry[size * 2];
            System.arraycopy(cache, 0, newCache, 0, size);
            cache = newCache;
        }
    }

    private void removeExpiredEntries() {
        Instant currentTime = clock.instant();
        int newSize = 0;

        // Copy non-expired entries to a new array
        for (int i = 0; i < size; i++) {
            ExpirableEntry entry = cache[i];
            if (entry.hasExpired(currentTime)) {
                continue;
            }
            cache[newSize++] = entry;
        }

        // Clear remaining entries after newSize
        for (int i = newSize; i < size; i++) {
            cache[i] = null;
        }

        size = newSize; // Update the size
    }

    private record ExpirableEntry(Object key, Object value, Instant expirationTime) {

        public boolean hasExpired(Instant currentTime) {
            return currentTime.isAfter(expirationTime);
        }
    }
}