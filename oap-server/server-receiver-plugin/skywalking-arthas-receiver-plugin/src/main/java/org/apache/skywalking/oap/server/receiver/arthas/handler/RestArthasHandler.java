package org.apache.skywalking.oap.server.receiver.arthas.handler;

import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.Path;
import com.linecorp.armeria.server.annotation.Post;
import lombok.extern.slf4j.Slf4j;
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
    public void arthasStart(
            @Param("serviceName") String serviceName,
            @Param("instanceName") String instanceName) {
        CommandQueue.produceCommand(serviceName, instanceName, Command.START);
    }

    @Post
    @Path("/api/arthas/stop")
    public void arthasStop(
            @Param("serviceName") String serviceName,
            @Param("instanceName") String instanceName) {
        CommandQueue.produceCommand(serviceName, instanceName, Command.STOP);
    }

    @Post
    @Path("/api/arthas/getFlameDiagram")
    public String arthasGetFlameDiagram(@Param("serviceName") String serviceName, 
                                        @Param("instanceName") String instanceName, 
                                        @Param("filePath") String filePath) {
        String result;
        try {
            CommandQueue.produceFlameDiagram(serviceName, instanceName, filePath);
            String key = serviceName + "-" + instanceName;
            COUNT_DOWN_LATCH = new CountDownLatch(1);
            COUNT_DOWN_LATCH.await();
            result = FLAME_DIAGRAM_RESPONSE_DATA.get(key);
            FLAME_DIAGRAM_RESPONSE_DATA.remove(key);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

}
