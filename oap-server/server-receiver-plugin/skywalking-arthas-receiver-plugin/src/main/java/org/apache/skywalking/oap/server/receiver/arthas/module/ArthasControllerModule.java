package org.apache.skywalking.oap.server.receiver.arthas.module;

import org.apache.skywalking.oap.server.library.module.ModuleDefine;

public class ArthasControllerModule extends ModuleDefine {

    public static final String NAME = "arthas-controller";

    public ArthasControllerModule() {
        super(NAME);
    }

    @Override
    public Class<?>[] services() {
        return new Class[0];
    }

}
