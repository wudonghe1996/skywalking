package org.apache.skywalking.oap.server.receiver.arthas.entity;

import lombok.Data;

@Data
public class RealTimeRequest {

    private String serviceName;
    private String instanceName;
    private String command;

}
