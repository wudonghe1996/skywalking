package org.apache.skywalking.oap.server.receiver.arthas.handler;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.arthas.v3.*;
import org.apache.skywalking.oap.server.core.storage.IDayuDAO;
import org.apache.skywalking.oap.server.receiver.arthas.CommandQueue;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.apache.skywalking.oap.server.receiver.arthas.provider.ArthasProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class GrpcArthasHandler extends ArthasCommandServiceGrpc.ArthasCommandServiceImplBase implements GRPCHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcArthasHandler.class);

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


}