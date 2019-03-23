package com.netflix.concurrency.limits.limit;

import junit.framework.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class AIMDLimitTest {
    @Test
    public void testDefault() {
        AIMDLimit limiter = AIMDLimit.newBuilder().initialLimit(10).build();
        Assert.assertEquals(10, limiter.getLimit());
    }
    
    @Test
    public void increaseOnSuccess() {
        AIMDLimit limiter = AIMDLimit.newBuilder().initialLimit(20).build();
        limiter.onSample(0, TimeUnit.MILLISECONDS.toNanos(1), 10, false);
        Assert.assertEquals(21, limiter.getLimit());
    }

    @Test
    public void decreaseOnDrops() {
        AIMDLimit limiter = AIMDLimit.newBuilder().initialLimit(30).build();
        limiter.onSample(0, 0, 0, true);
        Assert.assertEquals(27, limiter.getLimit());
    }
}
