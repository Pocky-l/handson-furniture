package com.pockyl.handson_furniture.station;

/**
 * Changes of the furniture that clients need not be sent at once: Refurbished's generator marks itself changed on every tick
 * it runs, as its energy goes down by one. Sending the whole block each tick for that would cost a packet a tick per generator;
 * clients count the energy down themselves and are sent the block when its fuel slot changes.
 */
public final class QuietChanges {
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private QuietChanges() {
    }

    public static void begin() {
        DEPTH.get()[0]++;
    }

    public static void end() {
        DEPTH.get()[0]--;
    }

    /** Whether a change now is one of those: the furniture is still saved, only no update is sent. */
    public static boolean isQuiet() {
        return DEPTH.get()[0] > 0;
    }
}
