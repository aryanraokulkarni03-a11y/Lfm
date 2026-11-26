package com.campus.lostfound.domain.model;

public record UserProfile(long userId, String fullName, String degree, String branch, String registrationNumber,
                          String semester, String batch, String section, String hostelName, String roomNumber,
                          String phone, String gender) {
    public UserProfile {
        fullName = fullName == null ? "" : fullName;
        degree = degree == null ? "" : degree;
        branch = branch == null ? "" : branch;
        registrationNumber = registrationNumber == null ? "" : registrationNumber;
        semester = semester == null ? "" : semester;
        batch = batch == null ? "" : batch;
        section = section == null ? "" : section;
        hostelName = hostelName == null ? "" : hostelName;
        roomNumber = roomNumber == null ? "" : roomNumber;
        phone = phone == null ? "" : phone;
        gender = gender == null ? "" : gender;
    }

    public String universityDetails() { return degree + " • " + branch + " • Reg. No. " + registrationNumber + " • Semester " + semester + " • Batch " + batch + " • Section " + section; }
}
