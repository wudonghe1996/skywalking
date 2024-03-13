package org.apache.skywalking.oap.server.core.storage.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MachineDataLine {

    private Integer cpuCore;
    private Integer memoryTotal;
    private Integer processCount;
    private Integer threadCount;
    private Double processCpuAvg;
    private Double processCpuMax;
    private Double systemCpuAvg;
    private Double systemCpuMax;
    private Double machineMemoryAvg;
    private Double machineMemoryMax;
}
