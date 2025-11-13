package com.example.gallery_service.service;

import com.example.gallery_service.dto.GalleryResponse;
import com.example.gallery_service.entity.TravelPhoto;
import com.example.gallery_service.repository.TravelPhotoRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TravelPhotoService {

    @Autowired
    private TravelPhotoRepository travelPhotoRepository;

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    public TravelPhoto uploadPhoto(UUID userId, UUID galleryId, MultipartFile file,
            String caption, String location, LocalDateTime takenAt) throws Exception {

        // Tạo tên file duy nhất theo cấu trúc user_id/gallery_id/uuid.jpg
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = userId + "/" + galleryId + "/" + UUID.randomUUID() + fileExtension;

        // Đảm bảo bucket tồn tại
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // Upload file lên MinIO
        InputStream inputStream = file.getInputStream();
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        // Tạo URL file (presigned URL)
        String fileUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .method(Method.GET)
                        .build());

        // Lưu metadata vào database
        TravelPhoto photo = new TravelPhoto();
        photo.setUserId(userId);
        photo.setTripId(galleryId); // Set galleryId as tripId
        photo.setFileUrl(fileUrl);
        photo.setCaption(caption);
        photo.setLocation(location);
        photo.setTakenAt(takenAt);
        photo.setUploadedAt(LocalDateTime.now());

        return travelPhotoRepository.save(photo);
    }

    public List<TravelPhoto> getPhotosByUserId(UUID userId) {
        return travelPhotoRepository.findByUserId(userId);
    }

    public List<TravelPhoto> getPhotosByUserAndTrip(UUID userId, UUID tripId) {
        return travelPhotoRepository.findByUserIdAndTripId(userId, tripId);
    }

    public TravelPhoto getPhotoById(UUID photoId) {
        return travelPhotoRepository.findById(photoId).orElse(null);
    }

    public List<TravelPhoto> getPhotosByGalleryId(UUID galleryId) {
        return travelPhotoRepository.findByTripId(galleryId);
    }

    public List<UUID> getGalleryIdsByUserId(UUID userId) {
        return travelPhotoRepository.findDistinctGalleryIdsByUserId(userId);
    }

    public List<GalleryResponse> getGalleriesWithThumbnailByUserId(UUID userId) {
        List<UUID> galleryIds = travelPhotoRepository.findDistinctGalleryIdsByUserId(userId);
        List<GalleryResponse> galleries = new java.util.ArrayList<>();

        for (UUID galleryId : galleryIds) {
            List<TravelPhoto> photos = travelPhotoRepository.findFirstPhotoByUserAndGallery(userId, galleryId);
            String thumbnailUrl = photos.isEmpty() ? null : photos.get(0).getFileUrl();

            List<TravelPhoto> allPhotos = travelPhotoRepository.findByUserIdAndTripId(userId, galleryId);
            int photoCount = allPhotos.size();
            int totalLikes = allPhotos.stream().mapToInt(TravelPhoto::getLikeCount).sum();

            galleries.add(new GalleryResponse(galleryId, thumbnailUrl, photoCount, totalLikes));
        }

        return galleries;
    }

    public void deletePhoto(UUID photoId) {
        // Get photo info to extract file path from URL
        TravelPhoto photo = travelPhotoRepository.findById(photoId).orElse(null);
        if (photo != null && photo.getFileUrl() != null) {
            try {
                // Extract object name from presigned URL
                String objectName = extractObjectNameFromUrl(photo.getFileUrl());

                if (objectName != null) {
                    // Delete from MinIO
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectName)
                                    .build());
                    System.out.println("Deleted from MinIO: " + objectName);
                }
            } catch (Exception e) {
                System.err.println("Error deleting from MinIO: " + e.getMessage());
                e.printStackTrace(); // Log error but continue with DB deletion
            }
        }
        // Delete from database
        travelPhotoRepository.deleteById(photoId);
    }

    @Transactional
    public void deleteGalleryByTripId(UUID tripId) {
        // Get all photos in this trip
        List<TravelPhoto> photos = travelPhotoRepository.findByTripId(tripId);

        // Delete each file from MinIO
        for (TravelPhoto photo : photos) {
            if (photo.getFileUrl() != null) {
                try {
                    // Extract object name from presigned URL
                    // Format: http://host:port/bucket/object?params
                    String fileUrl = photo.getFileUrl();
                    String objectName = extractObjectNameFromUrl(fileUrl);

                    if (objectName != null) {
                        // Delete from MinIO
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(objectName)
                                        .build());
                        System.out.println("Deleted from MinIO: " + objectName);
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting from MinIO: " + e.getMessage());
                    e.printStackTrace(); // Log error but continue
                }
            }
        }

        // Delete all from database
        travelPhotoRepository.deleteByTripId(tripId);
    }

    private String extractObjectNameFromUrl(String url) {
        try {
            // Presigned URL format: http://host:port/bucket/object?X-Amz-...
            // Extract everything between bucket/ and ?
            int bucketIndex = url.indexOf("/" + bucketName + "/");
            if (bucketIndex == -1)
                return null;

            int startIndex = bucketIndex + bucketName.length() + 2; // +2 for two slashes
            int endIndex = url.indexOf("?", startIndex);

            if (endIndex == -1) {
                return url.substring(startIndex);
            } else {
                return url.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            System.err.println("Error extracting object name from URL: " + url);
            e.printStackTrace();
            return null;
        }
    }
}