# Gallery Service API

Service quản lý kho ảnh du lịch với tích hợp PostgreSQL và MinIO.

## Cấu trúc lưu trữ

- **MinIO**: File ảnh được lưu theo cấu trúc `user_id/gallery_id/uuid.ext`
- **PostgreSQL**: Metadata của ảnh được lưu trong bảng `travel_photos`

## API Endpoints

**Upload ảnh:**

```bash
curl -X POST http://localhost:8084/api/gallery/upload \
  -H "Authorization: Bearer your-jwt-token" \
  -F "galleryId=550e8400-e29b-41d4-a716-446655440000" \
  -F "file=@photo.jpg" \
  -F "caption=Núi Phú Sĩ" \
  -F "location=Japan"
```

**Response:**

```json
{
  "id": "uuid",
  "userId": "uuid",
  "tripId": "uuid",
  "fileUrl": "presigned-url",
  "caption": "Mô tả",
  "location": "Địa điểm",
  "likeCount": 0,
  "takenAt": "2025-10-22T10:00:00",
  "uploadedAt": "2025-10-22T10:00:00",
  "isPublic": false
}
```

### 2. Lấy danh sách ảnh theo user

```
GET /api/gallery/user/{userId}
```

### 3. Lấy danh sách ảnh theo gallery

```
GET /api/gallery/gallery/{galleryId}
```

### 4. Lấy chi tiết ảnh

```
GET /api/gallery/{photoId}
```

### 5. Xóa ảnh

```
DELETE /api/gallery/{photoId}
```

### 6. Lấy danh sách gallery theo user

```
GET /api/gallery/user/{userId}/galleries
```

**Response:**

```json
["gallery-uuid-1", "gallery-uuid-2", "gallery-uuid-3"]
```

## Cấu hình môi trường

Đảm bảo các biến môi trường trong file `.env`:

```properties
# Database
DB_URL=jdbc:postgresql://localhost:5432/gallery_service
DB_USERNAME=your_username
DB_PASSWORD=your_password
DB_DRIVER=org.postgresql.Driver
DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=gallery
```

## Chạy service

```bash
# Development mode
mvn spring-boot:run

# Hoặc với Docker
docker-compose up gallery-service
```

## Cấu trúc Database

```sql
CREATE TABLE travel_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    trip_id UUID,
    file_url TEXT NOT NULL,
    thumbnail_url TEXT,
    caption TEXT,
    location VARCHAR(255),
    like_count INT DEFAULT 0,
    taken_at TIMESTAMP,
    uploaded_at TIMESTAMP DEFAULT NOW(),
    is_public BOOLEAN DEFAULT FALSE
);
```
