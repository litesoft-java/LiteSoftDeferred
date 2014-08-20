package org.litesoft.deferred.shared;

public interface DeferredRunner {
    /**
     * Schedules the Runnable to "run" on or after the elapsed time.
     *
     * @param pDelayMillis how long to wait before the "run", in
     *                     milliseconds
     */
    void schedule( int pDelayMillis );

    /**
     * Schedules the Runnable to "run" on or after the elapsed time repeatedly.
     *
     * @param pPeriodMillis how long to wait before the "run", in
     *                      milliseconds, initially & between each repetition
     */
    void scheduleRepeating( int pPeriodMillis );

    /**
     * Cancels the pending "run"(s).
     */
    void cancel();
}
