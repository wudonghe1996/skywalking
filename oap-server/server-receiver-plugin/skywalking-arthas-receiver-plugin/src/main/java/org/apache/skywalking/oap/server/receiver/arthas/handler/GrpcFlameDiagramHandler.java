package org.apache.skywalking.oap.server.receiver.arthas.handler;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.arthas.v3.*;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.apache.skywalking.oap.server.receiver.arthas.CommandQueue;

@Slf4j
public class GrpcFlameDiagramHandler extends FlameDiagramGrpc.FlameDiagramImplBase implements GRPCHandler {

    @Override
    public void getFlameDiagramPath(final Request request, final StreamObserver<Response> responseObserver) {
        Response.Builder response = Response.newBuilder();
        CommandQueue.consumeFlameDiagram(request.getServiceName(), request.getInstanceName())
                .ifPresent(response::setFilePath);

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendFlameDiagramData(SendRequest request,
                                     StreamObserver<Response> responseObserver) {
        String flameDiagramData = request.getFlameDiagramData();
        String serviceName = request.getServiceName();
        String instanceName = request.getInstanceName();
        String key = serviceName + "-" + instanceName;

        RestArthasHandler.FLAME_DIAGRAM_RESPONSE_DATA.put(key, flameDiagramData);

        responseObserver.onNext(null);
        responseObserver.onCompleted();
        RestArthasHandler.COUNT_DOWN_LATCH.countDown();
    }

}