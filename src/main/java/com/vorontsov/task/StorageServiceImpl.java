package com.vorontsov.task;

import com.vorontsov.task.repo.TarantoolRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class StorageServiceImpl extends StorageServiceGrpc.StorageServiceImplBase {

    private final TarantoolRepository repository;

    @Override
    public void count(Empty request, StreamObserver<CountResponse> responseObserver) {
        try {
            long count = repository.count();
            responseObserver.onNext(CountResponse.newBuilder().setCount(count).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка Count", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void range(RangeRequest request, StreamObserver<Entry> responseObserver) {
        try {
            String since = request.getKeySince();
            String to = request.getKeyTo();

            List<?> callResult = repository.selectRange(since, 100);
            if (callResult == null || callResult.isEmpty()) {
                responseObserver.onCompleted();
                return;
            }
            List<?> rows = (List<?>) callResult.get(0);
            log.info("Range call found {} items", rows.size());

            for (Object row : rows) {
                List<?> tuple = (List<?>) row;
                if (tuple.size() < 2) continue;

                String k = convertToString(tuple.get(0));
                String v = convertToString(tuple.get(1));

                if (!to.isEmpty() && k.compareTo(to) > 0) {
                    break;
                }
                responseObserver.onNext(Entry.newBuilder().setKey(k).setValue(v).build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Range error", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void put(Entry request, StreamObserver<Empty> responseObserver) {
        try {
            String key = request.getKey();
            String value = request.getValue();

            Object valueToSave;
            if (value == null || value.isEmpty()) {
                valueToSave = null; // Для msgpack.NULL
            } else {
                valueToSave = value.getBytes(StandardCharsets.UTF_8);
            }

            repository.put(key, valueToSave);

            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка Put", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Ошибка при сохранении: " + e.getMessage())
                    .asException());
        }
    }

    @Override
    public void get(KeyRequest request, StreamObserver<Entry> responseObserver) {
        try {
            List<?> res = repository.get(request.getKey());
            if (res != null && !res.isEmpty()) {
                List<?> tuple = (List<?>) res.get(0);
                String val = tuple.size() > 1 ? convertToString(tuple.get(1)) : "";

                responseObserver.onNext(Entry.newBuilder()
                        .setKey(request.getKey())
                        .setValue(val)
                        .build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Ключ не найден").asException());
            }
        } catch (Exception e) {
            log.error("Ошибка Get", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void delete(KeyRequest request, StreamObserver<Empty> responseObserver) {
        try {
            repository.delete(request.getKey());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка Delete", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    private String convertToString(Object obj) {
        if (obj == null) return "";
        if (obj instanceof byte[]) {
            return new String((byte[]) obj, java.nio.charset.StandardCharsets.UTF_8);
        }
        return obj.toString();
    }
}