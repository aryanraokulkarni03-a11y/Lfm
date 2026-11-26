package com.campus.lostfound.domain.model;

import com.campus.lostfound.domain.enums.ClaimStatus;

import java.time.Instant;

public record Claim(long id, long itemId, String itemTitle, long claimantId, String claimantEmail,
                    String ownershipExplanation, String identifyingDetails, String possessionInformation,
                    String additionalNotes, String evidencePath, ClaimStatus status, String reviewReason,
                    Instant createdAt) { }
