package org.apache.skywalking.oap.server.receiver.arthas.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.annotation.Path;
import com.linecorp.armeria.server.annotation.Post;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.network.arthas.v3.ArthasRequest;
import org.apache.skywalking.apm.network.arthas.v3.Command;
import org.apache.skywalking.apm.network.arthas.v3.RealTimeCommand;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.query.DayuQueryService;
import org.apache.skywalking.oap.server.core.storage.IDayuDAO;
import org.apache.skywalking.oap.server.core.storage.model.arthas.FlameDiagramList;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.receiver.arthas.CommandQueue;
import org.apache.skywalking.oap.server.receiver.arthas.entity.JadDTO;
import org.apache.skywalking.oap.server.receiver.arthas.entity.ThreadStackDTO;
import org.apache.skywalking.oap.server.receiver.arthas.provider.ArthasProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class RestArthasHandler {

    public static final Map<String, ThreadStackDTO.ThreadInfo> CPU_CODE_STACK_RESPONSE_DATA = new ConcurrentHashMap<>();
    public static final Map<String, CountDownLatch> CPU_CODE_STACK_COUNT_DOWN_LATCH = new ConcurrentHashMap<>();
    public static final Map<String, JadDTO> JAD_RESPONSE_DATA = new ConcurrentHashMap<>();
    public static final Map<String, CountDownLatch> JAD_COUNT_DOWN_LATCH = new ConcurrentHashMap<>();
    public static final Map<String, String> FLAME_DIAGRAM_RESPONSE_DATA = new ConcurrentHashMap<>();
    public static final Map<String, CountDownLatch> FLAME_DIAGRAM_COUNT_DOWN_LATCH = new ConcurrentHashMap<>();

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private DayuQueryService queryService;

    public RestArthasHandler(ModuleManager moduleManager) {
        this.queryService = moduleManager.find(CoreModule.NAME).provider().getService(DayuQueryService.class);
    }

    @Post
    @Path("/api/arthas/start")
    public HttpResponse arthasStart(final ArthasRequest request) throws JsonProcessingException {
        CommandQueue.produceCommand(request.getServiceName(), request.getInstanceName(), request.getProfileTaskId(), Command.START);
        return successResponse("");
    }

    @Post
    @Path("/api/arthas/stop")
    public HttpResponse arthasStop(final ArthasRequest request) throws JsonProcessingException {
        CommandQueue.produceCommand(request.getServiceName(), request.getInstanceName(), request.getProfileTaskId(), Command.STOP);
        return successResponse("");
    }

    @Post
    @Path("/api/arthas/getCpuCodeStack")
    public HttpResponse getCpuCodeStack(final RealTimeRequest request) throws JsonProcessingException {
        ThreadStackDTO.ThreadInfo threadInfo = new ThreadStackDTO.ThreadInfo();
        try {
            String key = CommandQueue.getKey(request.getServiceName(), request.getInstanceName());
            CommandQueue.produceRealTimeCommand(request.getServiceName(), request.getInstanceName(), RealTimeCommand.CPU_CODE_STACK, request.getCommand());
            CPU_CODE_STACK_COUNT_DOWN_LATCH.put(key, new CountDownLatch(1));
            CPU_CODE_STACK_COUNT_DOWN_LATCH.get(key).await();
            threadInfo = CPU_CODE_STACK_RESPONSE_DATA.get(key);
            CPU_CODE_STACK_RESPONSE_DATA.remove(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return successResponse(threadInfo);
    }

    @Post
    @Path("/api/arthas/getJadData")
    public HttpResponse getJadData(final RealTimeRequest request) throws JsonProcessingException {
        JadDTO jadDTO = new JadDTO();
        try {
            String key = CommandQueue.getKey(request.getServiceName(), request.getInstanceName());
            CommandQueue.produceRealTimeCommand(request.getServiceName(), request.getInstanceName(), RealTimeCommand.JAD, request.getCommand());
            JAD_COUNT_DOWN_LATCH.put(key, new CountDownLatch(1));
            JAD_COUNT_DOWN_LATCH.get(key).await();
            jadDTO = JAD_RESPONSE_DATA.get(key);
            JAD_RESPONSE_DATA.remove(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return successResponse(jadDTO);
    }

    @Post
    @Path("/api/arthas/samplingFlameDiagram")
    public HttpResponse samplingFlameDiagram(final FlameDiagramRequest request) throws JsonProcessingException {
        CommandQueue.produceRealTimeCommand(request.getServiceName(), request.getInstanceName(), RealTimeCommand.FLAME_DIAGRAM_SAMPLING, request.getFilePath());
        return successResponse(true);
    }

    @Post
    @Path("/api/arthas/getFlameDiagram")
    public HttpResponse getFlameDiagram(final FlameDiagramRequest request) throws JsonProcessingException {
        String result;
        try {
            Integer profileTaskId = request.getProfileTaskId();
            String flameDiagramId = request.getFlameDiagramId();
            String flameDiagram = queryService.getFlameDiagram(profileTaskId, flameDiagramId);
            if (StringUtils.isEmpty(flameDiagram)) {
                String key = CommandQueue.getKey(request.getServiceName(), request.getInstanceName());
                CommandQueue.produceRealTimeCommand(request.getServiceName(), request.getInstanceName(), RealTimeCommand.FLAME_DIAGRAM_DATA, request.getFilePath());
                FLAME_DIAGRAM_COUNT_DOWN_LATCH.put(key, new CountDownLatch(1));
                FLAME_DIAGRAM_COUNT_DOWN_LATCH.get(key).await();
                result = FLAME_DIAGRAM_RESPONSE_DATA.get(key);
                FLAME_DIAGRAM_RESPONSE_DATA.remove(key);

                // save data to es
                IDayuDAO dayuDao = ArthasProvider.getDayuDao();
                dayuDao.saveFlameDiagramData(profileTaskId, result);
            } else {
                result = flameDiagram;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return successResponse(result);
    }

    @Post
    @Path("/api/arthas/getFlameDiagramList")
    public HttpResponse getFlameDiagramList(final FlameDiagramRequest request) throws JsonProcessingException {
        Integer profileTaskId = request.getProfileTaskId();
        List<FlameDiagramList> flameDiagramList = queryService.getFlameDiagramList(profileTaskId);
        return successResponse(flameDiagramList);
    }

    private HttpResponse successResponse(Object data) throws JsonProcessingException {
        return HttpResponse.of(ResponseHeaders.builder(HttpStatus.OK)
                .contentType(MediaType.JSON_UTF_8)
                .build(), HttpData.ofUtf8(MAPPER.writeValueAsString(data)));
    }

    @Data
    private static class FlameDiagramRequest {
        private String serviceName;
        private String instanceName;
        private String filePath;
        private Integer profileTaskId;
        private String flameDiagramId;
    }

    @Data
    private static class RealTimeRequest {
        private String serviceName;
        private String instanceName;
        private String command;
    }
}