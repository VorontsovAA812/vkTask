package com.vorontsov.task;

import com.vorontsov.task.CountResponse;
import com.vorontsov.task.Empty;
import com.vorontsov.task.Entry;
import com.vorontsov.task.RangeRequest;
import com.vorontsov.task.StorageServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.tarantool.TarantoolClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class StorageServiceImpl extends StorageServiceGrpc.StorageServiceImplBase {

    private final TarantoolClient tarantoolClient;

    @Override
    public void count(Empty request, StreamObserver<CountResponse> responseObserver) {
        log.info("Запрос Count к Tarantool");
        try {
            List<?> result = tarantoolClient.syncOps().eval("return box.space.KV:count()");
            long count = 0;
            if (result != null && !result.isEmpty()) {
                count = ((Number) result.get(0)).longValue();
            }

            CountResponse response = CountResponse.newBuilder()
                    .setCount(count)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка Count", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Ошибка БД: " + e.getMessage())
                    .asException());
        }
    }

    @Override
    public void range(RangeRequest request, StreamObserver<Entry> responseObserver) {
        log.info("Запрос Range: {} - {}", request.getKeySince(), request.getKeyTo());
        try {
            List<?> result = tarantoolClient.syncOps().select("KV", "primary", Collections.emptyList(), 0, 1000, 0);
            if (result != null) {
                for (Object row : result) {
                    List<?> tuple = (List<?>) row;
                    String key = tuple.get(0).toString();
                    String value = tuple.get(1).toString();

                    if (key.compareTo(request.getKeySince()) >= 0 && key.compareTo(request.getKeyTo()) <= 0) {
                        responseObserver.onNext(Entry.newBuilder()
                                .setKey(key)
                                .setValue(value)
                                .build());
                    }
                }
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка Range", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Ошибка БД: " + e.getMessage())
                    .asException());
        }
    }
}