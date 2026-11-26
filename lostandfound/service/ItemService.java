package com.campus.lostfound.service;

import com.campus.lostfound.domain.enums.ItemStatus;
import com.campus.lostfound.domain.enums.ReportType;
import com.campus.lostfound.domain.model.Item;
import com.campus.lostfound.exception.ValidationException;
import com.campus.lostfound.repository.api.ItemImageRepository;
import com.campus.lostfound.repository.api.ItemRepository;

import java.util.List;
import java.util.Optional;

public final class ItemService {
    private final ItemRepository items;
    private final ItemImageRepository images;
    private final ItemStatusService statuses = new ItemStatusService();

    public ItemService(ItemRepository items, ItemImageRepository images) {
        this.items = items;
        this.images = images;
    }

    public Item report(Item item) {
        validate(item);
        Item saved = items.save(item);
        images.save(saved.id(), saved.imagePaths());
        return saved;
    }

    public List<Item> search(String keyword, String category, ReportType type, ItemStatus status, boolean includeArchived) {
        return items.search(keyword, category, type, status, includeArchived);
    }

    public Optional<Item> find(long id) { return items.findById(id); }

    public void transition(Item item, ItemStatus next) {
        statuses.transition(item.status(), next);
        items.updateStatus(item.id(), next);
    }

    public void archive(long id) { items.archive(id); }

    private void validate(Item item) {
        if (item.title() == null || item.title().isBlank()) throw new ValidationException("Title is required");
        if (item.description() == null || item.description().isBlank()) throw new ValidationException("Description is required");
        if (item.category() == null || item.category().isBlank()) throw new ValidationException("Category is required");
        if (item.location() == null || item.location().isBlank()) throw new ValidationException("Location is required");
        if (item.eventDate() == null) throw new ValidationException("Date is required");
        if (item.identifyingMarks() == null || item.identifyingMarks().isBlank()) throw new ValidationException("Identifying details are required");
        if (item.contactDetails() == null || item.contactDetails().isBlank()) throw new ValidationException("Contact details are required");
    }
}
