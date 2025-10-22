package com.example.gallery_service.dto;

import java.util.UUID;

public class GalleryResponse {
    private UUID galleryId;
    private String thumbnailUrl;
    private int photoCount;
    private int totalLikes;

    public GalleryResponse() {
    }

    public GalleryResponse(UUID galleryId, String thumbnailUrl, int photoCount, int totalLikes) {
        this.galleryId = galleryId;
        this.thumbnailUrl = thumbnailUrl;
        this.photoCount = photoCount;
        this.totalLikes = totalLikes;
    }

    public UUID getGalleryId() {
        return galleryId;
    }

    public void setGalleryId(UUID galleryId) {
        this.galleryId = galleryId;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public int getPhotoCount() {
        return photoCount;
    }

    public void setPhotoCount(int photoCount) {
        this.photoCount = photoCount;
    }

    public int getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(int totalLikes) {
        this.totalLikes = totalLikes;
    }
}
