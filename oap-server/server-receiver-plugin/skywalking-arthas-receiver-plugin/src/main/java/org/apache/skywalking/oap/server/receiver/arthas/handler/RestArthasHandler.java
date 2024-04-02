package org.apache.skywalking.oap.server.receiver.arthas.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.annotation.Path;
import com.linecorp.armeria.server.annotation.Post;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.network.arthas.v3.ArthasRequest;
import org.apache.skywalking.apm.network.arthas.v3.Command;
import org.apache.skywalking.apm.network.arthas.v3.RealTimeCommand;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.analysis.manual.arthas.ArthasConstant;
import org.apache.skywalking.oap.server.core.analysis.manual.arthas.FlameDiagramSamplingStatus;
import org.apache.skywalking.oap.server.core.query.DayuQueryService;
import org.apache.skywalking.oap.server.core.storage.IDayuDAO;
import org.apache.skywalking.oap.server.core.storage.model.arthas.FlameDiagramList;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.receiver.arthas.CommandQueue;
import org.apache.skywalking.oap.server.receiver.arthas.entity.FlameDiagramRequest;
import org.apache.skywalking.oap.server.receiver.arthas.entity.JadDTO;
import org.apache.skywalking.oap.server.receiver.arthas.entity.RealTimeRequest;
import org.apache.skywalking.oap.server.receiver.arthas.entity.ThreadStackDTO;
import org.apache.skywalking.oap.server.receiver.arthas.provider.ArthasProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
public class RestArthasHandler {

    public static final Map<String, ThreadStackDTO.ThreadInfo> CPU_CODE_STACK_RESPONSE_DATA = new ConcurrentHashMap<>();
    public static final Map<String, CountDownLatch> CPU_CODE_STACK_COUNT_DOWN_LATCH = new ConcurrentHashMap<>();
    public static final Map<String, JadDTO> JAD_RESPONSE_DATA = new ConcurrentHashMap<>();
    public static final Map<String, CountDownLatch> JAD_COUNT_DOWN_LATCH = new ConcurrentHashMap<>();
    public static final Map<String, String> FLAME_DIAGRAM_RESPONSE_DATA = new ConcurrentHashMap<>();
    public static final Map<String, CountDownLatch> FLAME_DIAGRAM_COUNT_DOWN_LATCH = new ConcurrentHashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(20);
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
        Integer profileTaskId = request.getProfileTaskId();
        String filePath = request.getFilePath();
        Integer flameDiagramTotal = queryService.getFlameDiagramTotal(profileTaskId);
        filePath = filePath + "_" + flameDiagramTotal;

        CommandQueue.produceRealTimeCommand(request.getServiceName(), request.getInstanceName(), RealTimeCommand.FLAME_DIAGRAM_SAMPLING, filePath);
        String flameDiagramId = UUID.randomUUID().toString();
        IDayuDAO dayuDao = ArthasProvider.getDayuDao();
        dayuDao.saveFlameDiagramData(profileTaskId, flameDiagramId, ArthasConstant.DEFAULT_FLAME_DIAGRAM_DATA, FlameDiagramSamplingStatus.SAMPLING);

        submitSamplingTask(profileTaskId, flameDiagramId, request.getServiceName(), request.getInstanceName(), filePath);

        return successResponse(true);
    }

    private void submitSamplingTask(Integer profileTaskId, String flameDiagramId, String serviceName, String instanceName, String filePath) {
        executorService.execute(() -> {
            while (true) {
                try {
                    String key = CommandQueue.getKey(serviceName, instanceName);
                    CommandQueue.produceRealTimeCommand(serviceName, instanceName, RealTimeCommand.FLAME_DIAGRAM_DATA, filePath);
                    FLAME_DIAGRAM_COUNT_DOWN_LATCH.put(key, new CountDownLatch(1));
                    FLAME_DIAGRAM_COUNT_DOWN_LATCH.get(key).await();
                    String result = FLAME_DIAGRAM_RESPONSE_DATA.get(key);
                    FLAME_DIAGRAM_RESPONSE_DATA.remove(key);
                    if (StringUtils.isNotEmpty(result)) {
                        IDayuDAO dayuDao = ArthasProvider.getDayuDao();
                        dayuDao.updateFlameDiagramData(profileTaskId, flameDiagramId, result, FlameDiagramSamplingStatus.FINISH);
                        return;
                    }
                } catch (Exception e) {
                    log.error("get sampling data task error");
                    e.printStackTrace();
                }
            }
        });
    }

    @Post
    @Path("/api/arthas/getFlameDiagram")
    public HttpResponse getFlameDiagram(final FlameDiagramRequest request) throws JsonProcessingException {
        Integer profileTaskId = request.getProfileTaskId();
        String flameDiagramId = request.getFlameDiagramId();
        String result = queryService.getFlameDiagram(profileTaskId, flameDiagramId);
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

}