/**
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.concurrency.limits.limit.window;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class ImmutablePercentileSampleWindow implements SampleWindow {
    private final long minRtt;
    private final int maxInFlight;
    private final boolean didDrop;
    private final Set<ObservedRtt> observedRtts;
    private final double percentile;

    ImmutablePercentileSampleWindow(double percentile) {
        this.minRtt = Long.MAX_VALUE;
        this.maxInFlight = 0;
        this.didDrop = false;
        this.observedRtts = new HashSet<>();
        this.percentile = percentile;
    }

    ImmutablePercentileSampleWindow(
            long minRtt,
            int maxInFlight,
            boolean didDrop,
            Set<ObservedRtt> observedRtts,
            double percentile
    ) {
        this.minRtt = minRtt;
        this.maxInFlight = maxInFlight;
        this.didDrop = didDrop;
        this.observedRtts = observedRtts;
        this.percentile = percentile;
    }

    @Override
    public ImmutablePercentileSampleWindow addSample(long rtt, long seqId, int inflight) {
        observedRtts.add(new ObservedRtt(rtt, seqId));
        return new ImmutablePercentileSampleWindow(
                Math.min(minRtt, rtt),
                Math.max(inflight, this.maxInFlight),
                didDrop,
                observedRtts,
                percentile
        );
    }

    @Override
    public ImmutablePercentileSampleWindow addDroppedSample(int inflight) {
        return new ImmutablePercentileSampleWindow(
                minRtt,
                Math.max(inflight, this.maxInFlight),
                true,
                observedRtts,
                percentile
        );
    }

    @Override
    public long getCandidateRttNanos() {
        return minRtt;
    }

    @Override
    public long getTrackedRttNanos() {
        List<Long> listOfSortedRtts = observedRtts.stream()
                .map(o -> o.rtt)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        int rttIndex = (int) Math.round(listOfSortedRtts.size() * percentile);
        int zeroBasedRttIndex = rttIndex - 1;
        return listOfSortedRtts.get(zeroBasedRttIndex);
    }

    @Override
    public int getMaxInFlight() {
        return maxInFlight;
    }

    @Override
    public int getSampleCount() {
        return observedRtts.size();
    }

    @Override
    public boolean didDrop() {
        return didDrop;
    }

    @Override
    public String toString() {
        return "ImmutablePercentileSampleWindow ["
                + "minRtt=" + TimeUnit.NANOSECONDS.toMicros(minRtt) / 1000.0
                + ", p" + percentile + " rtt=" + TimeUnit.NANOSECONDS.toMicros(getTrackedRttNanos()) / 1000.0
                + ", maxInFlight=" + maxInFlight
                + ", sampleCount=" + observedRtts.size()
                + ", didDrop=" + didDrop + "]";
    }

    private static class ObservedRtt {
        final long rtt;
        final long seqId;

        ObservedRtt(long rtt, long seqId) {
            this.rtt = rtt;
            this.seqId = seqId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ObservedRtt that = (ObservedRtt) o;
            return seqId == that.seqId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(seqId);
        }
    }
}
