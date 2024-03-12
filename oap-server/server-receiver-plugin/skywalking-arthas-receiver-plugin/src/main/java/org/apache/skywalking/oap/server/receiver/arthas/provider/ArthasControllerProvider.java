package org.apache.skywalking.oap.server.receiver.arthas.provider;

import com.linecorp.armeria.common.HttpMethod;
import org.apache.skywalking.oap.server.core.server.HTTPHandlerRegister;
import org.apache.skywalking.oap.server.library.server.http.HTTPServer;
import org.apache.skywalking.oap.server.library.server.http.HTTPServerConfig;
import org.apache.skywalking.oap.server.receiver.arthas.handler.*;
import org.apache.skywalking.oap.server.library.module.*;

import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.server.GRPCHandlerRegister;
import org.apache.skywalking.oap.server.receiver.arthas.module.ArthasControllerModule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArthasControllerProvider extends ModuleProvider {

    private HTTPServer httpServer;
    private ArthasHttpConfig config;

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return ArthasControllerModule.class;
    }

    @Override
    public ConfigCreator newConfigCreator() {
        return new ConfigCreator<ArthasHttpConfig>() {
            @Override
            public Class type() {
                return ArthasHttpConfig.class;
            }

            @Override
            public void onInitialized(final ArthasHttpConfig initialized) {
                config = initialized;
            }
        };
    }

    @Override
    public void prepare() throws ServiceNotProvidedException {

    }

    @Override
    public void start() throws ServiceNotProvidedException, ModuleStartException {
        GRPCHandlerRegister grpcService = getManager().find(CoreModule.NAME)
                .provider()
                .getService(GRPCHandlerRegister.class);
        grpcService.addHandler(new GrpcArthasHandler());
        grpcService.addHandler(new GrpcFlameDiagramHandler());

        HTTPHandlerRegister httpHandlerRegister = getManager().find(CoreModule.NAME)
                .provider()
                .getService(HTTPHandlerRegister.class);
        httpHandlerRegister.addHandler(new RestArthasHandler(), Collections.singletonList(HttpMethod.POST));
    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException {

    }

    @Override
    public String[] requiredModules() {
        return new String[0];
    }
}
