package org.apache.skywalking.oap.server.core.storage.model.arthas;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.skywalking.oap.server.core.analysis.manual.arthas.FlameDiagramSamplingStatus;

import java.util.Date;

@Data
@Accessors(chain = true)
public class FlameDiagramList {
    private String id;
    private Date createTime;

    private FlameDiagramSamplingStatus status;
}
