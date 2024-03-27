package org.apache.skywalking.oap.server.core.storage.model.arthas;

import lombok.Builder;
import lombok.Data;
import org.apache.skywalking.apm.network.arthas.v3.MemoryData;

@Data
@Builder
public class MemCharts {

    private String dataSamplingTime;

    private MemChartsData memData;

    @Data
    public static class MemChartsData {
        private Double heapMax;
        private Double heapUsed;
        private Double edenSpaceMax;
        private Double edenSpaceUsed;
        private Double survivorSpaceMax;
        private Double survivorSpaceUsed;
        private Double oldGenMax;
        private Double oldGenUsed;
        private Double nonHeapMax;
        private Double nonHeapUsed;
        private Double codeCacheMax;
        private Double codeCacheUsed;
        private Double metaSpaceMax;
        private Double metaSpaceUsed;
        private Double compressedClassSpaceMax;
        private Double compressedClassSpaceUsed;
    }
}
