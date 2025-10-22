package com.example.gallery_service.repository;

import com.example.gallery_service.entity.TravelPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TravelPhotoRepository extends JpaRepository<TravelPhoto, UUID> {

    List<TravelPhoto> findByUserId(UUID userId);

    List<TravelPhoto> findByUserIdAndTripId(UUID userId, UUID tripId);

    List<TravelPhoto> findByUserIdAndIsPublic(UUID userId, Boolean isPublic);

    List<TravelPhoto> findByTripId(UUID galleryId);

    @Query("SELECT DISTINCT t.tripId FROM TravelPhoto t WHERE t.userId = :userId AND t.tripId IS NOT NULL")
    List<UUID> findDistinctGalleryIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT t FROM TravelPhoto t WHERE t.userId = :userId AND t.tripId = :tripId ORDER BY t.uploadedAt ASC")
    List<TravelPhoto> findFirstPhotoByUserAndGallery(@Param("userId") UUID userId, @Param("tripId") UUID tripId);
}