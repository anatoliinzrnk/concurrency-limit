package com.netflix.concurrency.limits;

/**
 * Contract for an algorithm that calculates a concurrency limit based on 
 * rtt measurements
 */
public interface Limit {
    /**
     * Details of the current sample window
     */
    interface SampleWindow {
        /**
         * @return Candidate RTT in the sample window. This is traditionally the minimum rtt.
         */
        long getCandidateRttNanos();
        
        /**
         * @return Maximum number of inflight observed during the sample window
         */
        long getMaxInFlight();
        
        /**
         * @return Number of observed RTTs in the sample window
         */
        long getSampleCount();
        
        /**
         * @return True if there was a timeout
         */
        boolean didDrop();
    }
    
    /**
     * @return Current estimated limit
     */
    int getLimit();
    
    /**
     * Update the concurrency limit using a new rtt sample
     * 
     * @param rtt Minimum RTT sample for the last window
     * @param maxInFlight Maximum number of inflight requests observed in the sampling window
     */
    void update(SampleWindow sample);
}
