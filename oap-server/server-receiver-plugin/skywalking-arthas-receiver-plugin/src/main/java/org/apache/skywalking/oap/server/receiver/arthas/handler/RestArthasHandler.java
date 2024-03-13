package org.apache.skywalking.oap.server.receiver.arthas.handler;

import com.linecorp.armeria.server.annotation.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.arthas.v3.CommandRequest;
import org.apache.skywalking.apm.network.arthas.v3.SendRequest;
import org.apache.skywalking.apm.network.management.v3.InstanceProperties;
import org.apache.skywalking.oap.server.receiver.arthas.CommandQueue;
import org.apache.skywalking.apm.network.arthas.v3.Command;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class RestArthasHandler {

    public static final Map<String, String> FLAME_DIAGRAM_RESPONSE_DATA = new ConcurrentHashMap<>();
    protected static CountDownLatch COUNT_DOWN_LATCH;

    @Post
    @Path("/api/arthas/start")
    public void arthasStart(final CommandRequest request) {
        CommandQueue.produceCommand(request.getServiceName(), request.getInstanceName(), Command.START);
    }

    @Post
    @Path("/api/arthas/stop")
    public void arthasStop(final CommandRequest request) {
        CommandQueue.produceCommand(request.getServiceName(), request.getInstanceName(), Command.STOP);
    }

    @Post
    @Path("/api/arthas/getFlameDiagram")
    public String arthasGetFlameDiagram(final GetFlameRequest request) {
        String result;
        try {
            CommandQueue.produceFlameDiagram(request.getServiceName(), request.getInstanceName(), request.getFilePath());
            String key = request.getServiceName() + "-" + request.getInstanceName();
            COUNT_DOWN_LATCH = new CountDownLatch(1);
            COUNT_DOWN_LATCH.await();
            result = FLAME_DIAGRAM_RESPONSE_DATA.get(key);
            FLAME_DIAGRAM_RESPONSE_DATA.remove(key);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Data
    private static class GetFlameRequest {
        private String serviceName;
        private String instanceName;
        private String filePath;
    }
}
