package org.apache.skywalking.oap.server.receiver.arthas.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.annotation.Path;
import com.linecorp.armeria.server.annotation.Post;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.arthas.v3.ArthasRequest;
import org.apache.skywalking.oap.server.receiver.arthas.CommandQueue;
import org.apache.skywalking.apm.network.arthas.v3.Command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class RestArthasHandler {

    public static final Map<String, String> FLAME_DIAGRAM_RESPONSE_DATA = new ConcurrentHashMap<>();
    protected static CountDownLatch COUNT_DOWN_LATCH;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Post
    @Path("/api/arthas/start")
    public HttpResponse arthasStart(final ArthasRequest request) throws JsonProcessingException {
        CommandQueue.produceCommand(request.getServiceName(), request.getInstanceName(), Command.START);
        return successResponse("");
    }

    @Post
    @Path("/api/arthas/stop")
    public HttpResponse arthasStop(final ArthasRequest request) throws JsonProcessingException {
        CommandQueue.produceCommand(request.getServiceName(), request.getInstanceName(), Command.STOP);
        return successResponse("");
    }

    @Post
    @Path("/api/arthas/getFlameDiagram")
    public HttpResponse arthasGetFlameDiagram(final GetFlameRequest request) throws JsonProcessingException {
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
        return successResponse(result);
    }

    private HttpResponse successResponse(Object data) throws JsonProcessingException {
        return HttpResponse.of(ResponseHeaders.builder(HttpStatus.OK)
                .contentType(MediaType.JSON_UTF_8)
                .build(), HttpData.ofUtf8(MAPPER.writeValueAsString(data)));
    }

    @Data
    private static class GetFlameRequest {
        private String serviceName;
        private String instanceName;
        private String filePath;
    }
}