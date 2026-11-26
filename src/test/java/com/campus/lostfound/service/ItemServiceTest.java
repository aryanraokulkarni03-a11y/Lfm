package com.campus.lostfound.service;

import com.campus.lostfound.domain.enums.*;
import com.campus.lostfound.domain.model.Item;
import com.campus.lostfound.repository.api.ItemImageRepository;
import com.campus.lostfound.repository.api.ItemRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ItemServiceTest {
    @Test void reportsValidItemAndCopiesImagesThroughRepository() {
        var repo = new FakeItems(); var images = new FakeImages(); var service = new ItemService(repo, images);
        var item = service.report(new Item(0, 4, "student@srmap.edu.in", ReportType.LOST, "Blue wallet", "Leather wallet", "Accessories", "Library", LocalDate.now(), "Sticker inside", "Desk", ItemStatus.OPEN, List.of("wallet.png")));
        assertEquals(7, item.id()); assertEquals(List.of("wallet.png"), images.paths); assertEquals("Blue wallet", repo.saved.title());
    }
    private static final class FakeItems implements ItemRepository { Item saved; public Item save(Item i) { saved = new Item(7, i.reporterId(), i.reporterEmail(), i.reportType(), i.title(), i.description(), i.category(), i.location(), i.eventDate(), i.identifyingMarks(), i.contactDetails(), ItemStatus.OPEN, i.imagePaths()); return saved; } public List<Item> search(String a,String b,ReportType c,ItemStatus d,boolean e){return List.of();} public Optional<Item> findById(long id){return Optional.ofNullable(saved);} public void updateStatus(long id,ItemStatus s){} public void archive(long id){} }
    private static final class FakeImages implements ItemImageRepository { List<String> paths; public void save(long id,List<String> p){paths=p;} }
}
