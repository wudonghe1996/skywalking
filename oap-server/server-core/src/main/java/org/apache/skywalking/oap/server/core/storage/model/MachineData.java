package org.apache.skywalking.oap.server.core.storage.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MachineData {

    private Double systemCpuUsed;
    private Double processCpuUsed;
    private Double machineMemoryUsed;
    private Integer cpuCore;
    private Integer memoryTotal;
    private Integer processCount;
    private Integer threadCount;
    private Long netRecv;
    private Long netSent;
    private Long time;
}
