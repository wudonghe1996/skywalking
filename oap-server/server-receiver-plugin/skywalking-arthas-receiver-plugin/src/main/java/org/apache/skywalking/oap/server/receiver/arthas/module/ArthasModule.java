package org.apache.skywalking.oap.server.receiver.arthas.module;

import org.apache.skywalking.oap.server.library.module.ModuleDefine;

public class ArthasModule extends ModuleDefine {

    public static final String NAME = "arthas-controller";

    public ArthasModule() {
        super(NAME);
    }

    @Override
    public Class<?>[] services() {
        return new Class[0];
    }

}
