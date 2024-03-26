package org.apache.skywalking.oap.server.receiver.arthas.handler;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.arthas.v3.*;
import org.apache.skywalking.oap.server.receiver.arthas.CommandQueue;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class GrpcArthasHandler extends ArthasCommandServiceGrpc.ArthasCommandServiceImplBase implements GRPCHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcArthasHandler.class);

    @Override
    public void getCommand(final ArthasRequest request, final StreamObserver<ArthasResponse> responseObserver) {
        ArthasResponse.Builder builder = ArthasResponse.newBuilder().setCommand(Command.NONE);
        CommandQueue.consumeCommand(request.getServiceName(), request.getInstanceName())
                .ifPresent(command -> {
                    LOGGER.info(
                            "consume {} command for service {}, instance {}", command, request.getServiceName(),
                            request.getInstanceName()
                    );
                    builder.setCommand(command);
                    builder.setProfileTaskId(1);
                });
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendArthasData(final ArthasDataRequest request, final StreamObserver<ArthasResponse> responseObserver) {
        ArthasResponse.Builder builder = ArthasResponse.newBuilder();
        System.out.println(request);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


}