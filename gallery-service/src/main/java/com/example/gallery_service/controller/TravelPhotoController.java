package com.example.gallery_service.controller;

import com.example.gallery_service.dto.GalleryResponse;
import com.example.gallery_service.entity.TravelPhoto;
import com.example.gallery_service.service.TravelPhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gallery")
public class TravelPhotoController {

    @Autowired
    private TravelPhotoService travelPhotoService;

    @PostMapping("/upload")
    public ResponseEntity<TravelPhoto> uploadPhoto(
            @RequestParam("userId") UUID userId,
            @RequestParam("galleryId") UUID galleryId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "takenAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime takenAt) {

        try {
            TravelPhoto photo = travelPhotoService.uploadPhoto(userId, galleryId, file, caption, location, takenAt);
            return ResponseEntity.ok(photo);
        } catch (Exception e) {
            e.printStackTrace(); // Add logging
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}/galleries")
    public ResponseEntity<List<GalleryResponse>> getGalleriesByUser(@PathVariable UUID userId) {
        List<GalleryResponse> galleries = travelPhotoService.getGalleriesWithThumbnailByUserId(userId);
        return ResponseEntity.ok(galleries);
    }

    @GetMapping("/user/{userId}/trip/{tripId}")
    public ResponseEntity<List<TravelPhoto>> getPhotosByUserAndTrip(
            @PathVariable UUID userId, @PathVariable UUID tripId) {
        List<TravelPhoto> photos = travelPhotoService.getPhotosByUserAndTrip(userId, tripId);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/gallery/{galleryId}")
    public ResponseEntity<List<TravelPhoto>> getPhotosByGallery(@PathVariable UUID galleryId) {
        List<TravelPhoto> photos = travelPhotoService.getPhotosByGalleryId(galleryId);
        return ResponseEntity.ok(photos);
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID photoId) {
        travelPhotoService.deletePhoto(photoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/trip/{tripId}")
    public ResponseEntity<Void> deleteGalleryByTripId(@PathVariable UUID tripId) {
        travelPhotoService.deleteGalleryByTripId(tripId);
        return ResponseEntity.noContent().build();
    }
}