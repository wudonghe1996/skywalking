package org.apache.skywalking.oap.server.core.storage.model.arthas;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CpuCharts {

    private String dataSamplingTime;

    private Double cpuData;
}
