package com.campus.lostfound.repository.api;

import com.campus.lostfound.domain.enums.ItemStatus;
import com.campus.lostfound.domain.enums.ReportType;
import com.campus.lostfound.domain.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {
    Item save(Item item);
    List<Item> search(String keyword, String category, ReportType type, ItemStatus status, boolean includeArchived);
    Optional<Item> findById(long id);
    void updateStatus(long id, ItemStatus status);
    void archive(long id);
}
