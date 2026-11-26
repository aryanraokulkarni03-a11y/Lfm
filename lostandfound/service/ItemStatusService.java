package com.campus.lostfound.service;

import com.campus.lostfound.domain.enums.ItemStatus;
import com.campus.lostfound.exception.InvalidStatusTransitionException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ItemStatusService {
    private static final Map<ItemStatus, Set<ItemStatus>> ALLOWED = Map.of(
            ItemStatus.OPEN, EnumSet.of(ItemStatus.CLAIM_PENDING, ItemStatus.ARCHIVED),
            ItemStatus.CLAIM_PENDING, EnumSet.of(ItemStatus.OPEN, ItemStatus.CLAIM_APPROVED, ItemStatus.ARCHIVED),
            ItemStatus.CLAIM_APPROVED, EnumSet.of(ItemStatus.RETURNED, ItemStatus.ARCHIVED),
            ItemStatus.RETURNED, EnumSet.of(ItemStatus.CLOSED, ItemStatus.ARCHIVED),
            ItemStatus.CLOSED, EnumSet.of(ItemStatus.ARCHIVED),
            ItemStatus.ARCHIVED, EnumSet.noneOf(ItemStatus.class)
    );

    public ItemStatus transition(ItemStatus current, ItemStatus next) {
        if (current == null || next == null) throw new InvalidStatusTransitionException("Item status is required");
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(next)) {
            throw new InvalidStatusTransitionException("Cannot move item from " + current + " to " + next);
        }
        return next;
    }
}
