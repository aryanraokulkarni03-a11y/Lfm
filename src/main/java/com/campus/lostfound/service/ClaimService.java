package com.campus.lostfound.service;

import com.campus.lostfound.domain.enums.ClaimStatus;
import com.campus.lostfound.domain.enums.ItemStatus;
import com.campus.lostfound.domain.model.Claim;
import com.campus.lostfound.exception.ValidationException;
import com.campus.lostfound.repository.api.ClaimRepository;

import java.util.List;

public final class ClaimService {
    private final ClaimRepository claims;

    public ClaimService(ClaimRepository claims) { this.claims = claims; }

    public Claim submit(Claim claim) {
        if (claim.ownershipExplanation().isBlank() || claim.identifyingDetails().isBlank()
                || claim.possessionInformation().isBlank()) {
            throw new ValidationException("Ownership explanation, identifying details, and possession information are required");
        }
        if (claims.hasActiveClaim(claim.itemId())) throw new ValidationException("This item already has an active claim");
        return claims.save(claim);
    }

    public List<Claim> mine(long userId) { return claims.findByClaimant(userId); }
    public List<Claim> pending() { return claims.findPending(); }
    public List<Claim> all() { return claims.findAll(); }
    public void approve(long claimId, long adminId, String reason) { claims.review(claimId, adminId, ClaimStatus.APPROVED, reason); }
    public void reject(long claimId, long adminId, String reason) { claims.review(claimId, adminId, ClaimStatus.REJECTED, reason); }
}
