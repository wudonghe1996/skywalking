package org.apache.skywalking.oap.server.receiver.arthas.handler;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.arthas.v3.*;
import org.apache.skywalking.oap.server.core.storage.IDayuDAO;
import org.apache.skywalking.oap.server.receiver.arthas.CommandQueue;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.apache.skywalking.oap.server.receiver.arthas.entity.JadDTO;
import org.apache.skywalking.oap.server.receiver.arthas.entity.ThreadStackDTO;
import org.apache.skywalking.oap.server.receiver.arthas.provider.ArthasProvider;

@Slf4j
public class GrpcArthasHandler extends ArthasServiceGrpc.ArthasServiceImplBase implements GRPCHandler {

    private final Gson gson = new Gson();

    @Override
    public void getCommand(final ArthasRequest request, final StreamObserver<ArthasResponse> responseObserver) {
        ArthasResponse.Builder builder = ArthasResponse.newBuilder().setCommand(Command.NONE);
        CommandQueue.consumeCommand(request.getServiceName(), request.getInstanceName())
                .ifPresent(arthasCommand -> {
                    builder.setCommand(arthasCommand.getCommand());
                    builder.setProfileTaskId(arthasCommand.getProfileTaskId());
                });
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendArthasData(final ArthasDataRequest request, final StreamObserver<ArthasResponse> responseObserver) {
        ArthasResponse.Builder builder = ArthasResponse.newBuilder();
        IDayuDAO dayuDao = ArthasProvider.getDayuDao();
        request.getArthasSamplingDataList().forEach(dayuDao::saveArthasData);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getRealTimeCommand(final ArthasRequest request, final StreamObserver<RealTimeResponse> responseObserver) {
        RealTimeResponse.Builder response = RealTimeResponse.newBuilder();
        RealTimeData.Builder builder = RealTimeData.newBuilder();

        CommandQueue.consumeRealTimeCommand(request.getServiceName(), request.getInstanceName())
                .ifPresent(realTimeQueueData -> {
                    realTimeQueueData.forEach(x -> {
                        builder.setCommand(x.getCommand()).setRealTimeCommand(x.getRealTimeCommand());
                        response.addRealTimeData(builder.build());
                    });
                });
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendRealTimeData(final RealTimeRequest request, final StreamObserver<RealTimeResponse> responseObserver) {
        String key = CommandQueue.getKey(request.getServiceName(), request.getInstanceName());
        RealTimeCommand realTimeCommand = request.getRealTimeCommand();
        switch (realTimeCommand){
            case CPU_CODE_STACK:
                ThreadStackDTO.ThreadInfo cpuCodeStack = gson.fromJson(request.getData(), ThreadStackDTO.ThreadInfo.class);
                RestArthasHandler.CPU_CODE_STACK_RESPONSE_DATA.put(key, cpuCodeStack);
                RestArthasHandler.CPU_CODE_STACK_COUNT_DOWN_LATCH.get(key).countDown();
                break;
            case JAD:
                JadDTO jadDTO = gson.fromJson(request.getData(), JadDTO.class);
                RestArthasHandler.JAD_RESPONSE_DATA.put(key, jadDTO);
                RestArthasHandler.JAD_COUNT_DOWN_LATCH.get(key).countDown();
                break;
            case FLAME_DIAGRAM_DATA:
                String flameDiagramData = request.getData();
                RestArthasHandler.FLAME_DIAGRAM_RESPONSE_DATA.put(key, flameDiagramData);
                RestArthasHandler.FLAME_DIAGRAM_COUNT_DOWN_LATCH.get(key).countDown();
                break;
        }
        responseObserver.onNext(null);
        responseObserver.onCompleted();
    }

}