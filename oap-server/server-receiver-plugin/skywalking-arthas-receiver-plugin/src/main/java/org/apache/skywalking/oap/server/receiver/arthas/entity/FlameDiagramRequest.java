package org.apache.skywalking.oap.server.receiver.arthas.entity;

import lombok.Data;

@Data
public class FlameDiagramRequest {

    private String serviceName;
    private String instanceName;
    private String filePath;
    private Integer profileTaskId;
    private String flameDiagramId;

}
