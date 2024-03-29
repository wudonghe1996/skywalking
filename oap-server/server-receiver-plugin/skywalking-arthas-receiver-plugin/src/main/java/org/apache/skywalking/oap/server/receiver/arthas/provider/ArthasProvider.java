package org.apache.skywalking.oap.server.receiver.arthas.provider;

import com.linecorp.armeria.common.HttpMethod;
import org.apache.skywalking.oap.server.core.server.HTTPHandlerRegister;
import org.apache.skywalking.oap.server.core.storage.IDayuDAO;
import org.apache.skywalking.oap.server.core.storage.StorageDAO;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.apache.skywalking.oap.server.library.module.ServiceNotProvidedException;
import org.apache.skywalking.oap.server.library.server.http.HTTPServer;

import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.server.GRPCHandlerRegister;
import org.apache.skywalking.oap.server.receiver.arthas.handler.GrpcArthasHandler;
import org.apache.skywalking.oap.server.receiver.arthas.handler.RestArthasHandler;
import org.apache.skywalking.oap.server.receiver.arthas.module.ArthasModule;

import java.util.Collections;

public class ArthasProvider extends ModuleProvider {

    private HTTPServer httpServer;
    private ArthasHttpConfig config;
    private static IDayuDAO DAYU_DAO;

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return ArthasModule.class;
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

        HTTPHandlerRegister httpHandlerRegister = getManager().find(CoreModule.NAME)
                .provider()
                .getService(HTTPHandlerRegister.class);
        httpHandlerRegister.addHandler(new RestArthasHandler(), Collections.singletonList(HttpMethod.POST));

        StorageDAO storageDAO = getManager().find(StorageModule.NAME).provider().getService(StorageDAO.class);
        DAYU_DAO = storageDAO.newDayuDao();
    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException {

    }

    @Override
    public String[] requiredModules() {
        return new String[0];
    }

    public static IDayuDAO getDayuDao() {
        return DAYU_DAO;
    }
}
