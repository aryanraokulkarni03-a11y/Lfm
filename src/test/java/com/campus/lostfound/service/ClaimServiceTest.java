package com.campus.lostfound.service;

import com.campus.lostfound.domain.enums.ClaimStatus;
import com.campus.lostfound.domain.model.Claim;
import com.campus.lostfound.repository.api.ClaimRepository;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ClaimServiceTest {
    @Test void rejectsSecondActiveClaim() {
        var repo = new FakeClaims(); repo.active = true; var service = new ClaimService(repo);
        var claim = new Claim(0, 2, "Phone", 3, "student@srmap.edu.in", "Mine", "Scratch", "Bought last year", "", null, ClaimStatus.PENDING, null, null);
        assertThrows(RuntimeException.class, () -> service.submit(claim));
    }
    private static final class FakeClaims implements ClaimRepository { boolean active; public Claim save(Claim c){return c;} public List<Claim> findByClaimant(long id){return List.of();} public List<Claim> findPending(){return List.of();} public List<Claim> findAll(){return List.of();} public boolean hasActiveClaim(long id){return active;} public void review(long a,long b,ClaimStatus c,String d){} }
}
