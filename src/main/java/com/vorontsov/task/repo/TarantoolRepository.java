package com.vorontsov.task.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.tarantool.TarantoolClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TarantoolRepository {
    private final TarantoolClient client;

    private static final String SPACE_NAME = "KV";
    private static final String INDEX_NAME = "primary";

    public void put(String key, Object value) {
        client.syncOps().replace(SPACE_NAME, Arrays.asList(key, value));
    }
    public List<?> get(String key) {
        return client.syncOps().select(SPACE_NAME, INDEX_NAME, Collections.singletonList(key), 0, 1, 0);
    }

    public void delete(String key) {
        client.syncOps().delete(
                SPACE_NAME,
                Collections.singletonList(key)
        );
    }

    public List<?> selectRange(String keySince, int limit) {
        Object searchKey = (keySince == null || keySince.isEmpty()) ? null : keySince;
        return client.syncOps().call("get_range", searchKey, limit);
    }


    public long count() {
        List<?> res = client.syncOps().eval("return box.space." + SPACE_NAME + ":count()");
        return res != null && !res.isEmpty() ? ((Number) res.get(0)).longValue() : 0;
    }
}