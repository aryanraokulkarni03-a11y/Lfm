package com.campus.lostfound.service;

import com.campus.lostfound.domain.enums.ItemStatus;
import com.campus.lostfound.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemStatusServiceTest {
    private final ItemStatusService service = new ItemStatusService();

    @Test
    void allowsTheNormalReturnPath() {
        assertEquals(ItemStatus.CLAIM_PENDING, service.transition(ItemStatus.OPEN, ItemStatus.CLAIM_PENDING));
        assertEquals(ItemStatus.CLAIM_APPROVED, service.transition(ItemStatus.CLAIM_PENDING, ItemStatus.CLAIM_APPROVED));
        assertEquals(ItemStatus.RETURNED, service.transition(ItemStatus.CLAIM_APPROVED, ItemStatus.RETURNED));
        assertEquals(ItemStatus.CLOSED, service.transition(ItemStatus.RETURNED, ItemStatus.CLOSED));
    }

    @Test
    void rejectsUnsafeTransitions() {
        assertThrows(InvalidStatusTransitionException.class,
                () -> service.transition(ItemStatus.OPEN, ItemStatus.RETURNED));
        assertThrows(InvalidStatusTransitionException.class,
                () -> service.transition(ItemStatus.ARCHIVED, ItemStatus.OPEN));
    }
}
