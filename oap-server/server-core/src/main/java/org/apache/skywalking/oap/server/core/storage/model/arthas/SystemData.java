package org.apache.skywalking.oap.server.core.storage.model.arthas;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.skywalking.oap.server.core.query.type.KeyValue;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class SystemData {
    private String jvmInfo;
    private String sysEnv;
    private String sysProp;
    private String vmOption;
}
