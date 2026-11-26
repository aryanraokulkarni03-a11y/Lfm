package com.campus.lostfound.domain.model;

import com.campus.lostfound.domain.enums.ItemStatus;
import com.campus.lostfound.domain.enums.ReportType;

import java.time.LocalDate;
import java.util.List;

public record Item(long id, long reporterId, String reporterEmail, ReportType reportType, String title,
                   String description, String category, String location, LocalDate eventDate,
                   String identifyingMarks, String contactDetails, ItemStatus status, List<String> imagePaths) {
    public Item {
        imagePaths = imagePaths == null ? List.of() : List.copyOf(imagePaths);
    }
}
