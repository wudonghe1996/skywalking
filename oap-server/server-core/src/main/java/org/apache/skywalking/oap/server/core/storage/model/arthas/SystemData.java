package org.apache.skywalking.oap.server.core.storage.model.arthas;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SystemData {
    private String jvmInfo;
    private String sysEnv;
    private String sysProp;
    private String vmOption;
}
