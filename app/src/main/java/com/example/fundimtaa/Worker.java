package com.example.fundimtaa;

import com.google.firebase.Timestamp;

public class Worker {
    private String workerId;
    private String name;
    private String phoneNumber;
    private String location;
    private String dateOfApplication;
    private String experience;
    private Timestamp timestamp;
    private double rating; // Add rating field
    private String review; // Add review field
    private int assignedJobs;
    private int experienceScore; // New field
    private boolean recommended;
    private boolean iscompleted;
    private boolean isCanceled;

    private boolean isAvailable;

    // Constructor
    public Worker(String workerId, String name, String phoneNumber, String location, String dateOfApplication, String experience, Timestamp timestamp,boolean isCanceled) {
        this.workerId = workerId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.location = location;
        this.dateOfApplication = dateOfApplication;
        this.experience = experience;
        this.timestamp = timestamp;
        this.rating = 0; // Default value
        this.review = ""; // Default value
        this.assignedJobs = 0;
        this.experienceScore = 0;
        this.iscompleted = iscompleted;
        this.isCanceled = isCanceled;
        this.isAvailable = true;

    }

    // Getters and setters
    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDateOfApplication() {
        return dateOfApplication;
    }

    public void setDateOfApplication(String dateOfApplication) {
        this.dateOfApplication = dateOfApplication;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public int getAssignedJobs() {
        return assignedJobs;
    }

    public void setAssignedJobs(int assignedJobs) {
        this.assignedJobs = assignedJobs;
    }

    public int getExperienceScore() {
        return experienceScore;
    }

    public void setExperienceScore(int experienceScore) {
        this.experienceScore = experienceScore;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public boolean isIscompleted() {
        return iscompleted;
    }

    public void setIscompleted(boolean iscompleted) {
        this.iscompleted = iscompleted;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void setCanceled(boolean canceled) {
        isCanceled = canceled;
    }
    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
