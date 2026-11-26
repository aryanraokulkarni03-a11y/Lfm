package com.campus.lostfound.repository.api;

import java.util.List;

public interface ItemImageRepository {
    void save(long itemId, List<String> paths);
}
