package com.campus.lostfound.repository.api;

import com.campus.lostfound.domain.enums.ClaimStatus;
import com.campus.lostfound.domain.model.Claim;

import java.util.List;

public interface ClaimRepository {
    Claim save(Claim claim);
    List<Claim> findByClaimant(long claimantId);
    List<Claim> findPending();
    List<Claim> findAll();
    boolean hasActiveClaim(long itemId);
    void review(long claimId, long reviewerId, ClaimStatus status, String reason);
}
