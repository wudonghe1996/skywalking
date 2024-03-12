package org.apache.skywalking.oap.server.receiver.dayu.dto;

import lombok.Data;

@Data
public class TraceServiceDTO {

    private String serviceName;

    private String instanceName;
}
