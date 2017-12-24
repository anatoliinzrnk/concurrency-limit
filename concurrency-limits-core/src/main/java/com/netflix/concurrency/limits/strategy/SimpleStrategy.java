package com.netflix.concurrency.limits.strategy;

import com.netflix.concurrency.limits.MetricIds;
import com.netflix.concurrency.limits.MetricRegistry;
import com.netflix.concurrency.limits.MetricRegistry.Metric;
import com.netflix.concurrency.limits.Strategy;
import com.netflix.concurrency.limits.internal.EmptyMetricRegistry;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simplest strategy for enforcing a concurrency limit that has a single counter
 * for tracking total usage.
 */
public final class SimpleStrategy<T> implements Strategy<T> {

    private final AtomicInteger busy = new AtomicInteger();
    private volatile int limit = 1;
    private final Metric inflightMetric;
    
    public SimpleStrategy() {
        this(EmptyMetricRegistry.INSTANCE);
    }
    
    public SimpleStrategy(MetricRegistry registry) {
        this.inflightMetric = registry.metric(MetricIds.INFLIGHT_METRIC_ID);
        registry.guage(MetricIds.LIMIT_METRIC_ID, this::getLimit);
    }
    
    @Override
    public Optional<Runnable> tryAcquire(T context) {
        if (busy.get() >= limit) {
            return Optional.empty();
        }
        
        int inflight = busy.incrementAndGet();
        inflightMetric.add(inflight);
        return Optional.of(busy::decrementAndGet);
    }
    
    @Override
    public void setLimit(int limit) {
        if (limit < 1) {
            limit = 1;
        }
        this.limit = limit;
    }
    
    // Visible for testing
    int getLimit() {
        return limit;
    }
    
    int getBusyCount() {
        return busy.get();
    }

    @Override
    public String toString() {
        return "SimpleStrategy [busy=" + busy.get() + ", limit=" + limit + "]";
    }
}
