# Hệ thống quản lý sự kiện (Nhóm 7 - 24IT2, ĐH CMC)

Backend Java 17 · Spring Boot 3.3 · Spring Data JPA · Spring Security + JWT · ZXing (QR) · Apache POI · Swagger.

## Chạy nhanh (profile dev: H2 in-memory, email chỉ ghi log)
```
mvn spring-boot:run
```
- Swagger UI: http://localhost:8080/swagger-ui.html (nút Authorize → dán JWT)
- H2 console: http://localhost:8080/h2-console (jdbc:h2:mem:eventdb, user `sa`)
- Tài khoản demo: `admin@example.com / Admin@12345`; `organizer@example.com`, `staff@example.com`, `attendee@example.com` / `Demo@12345`

## Chạy với MySQL + SMTP (profile prod)
Tạo DB `event_db`, rồi đặt biến môi trường: `DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET (>=32 ký tự), ADMIN_PASSWORD, MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD`
```
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Kịch bản thử nhanh
1. `POST /api/auth/login` (organizer) → lấy token
2. `POST /api/events` (categoryId=1, venueId=1) → `POST /api/events/{id}/publish`
3. Đăng nhập attendee → `POST /api/events/{id}/registrations` (cần trong khoảng registrationOpenAt..CloseAt)
4. `GET /api/registrations/{id}/qr` lấy ảnh QR; organizer `POST /api/events/{id}/staff` gán staff
5. Staff `POST /api/checkin/scan {"qrToken": "..."}` (trong khung giờ BR06: từ 60 phút trước giờ bắt đầu)
6. `GET /api/events/{id}/report`, `/report/export` (Excel)

## Chưa làm (ngoài phạm vi code mẫu)
Quên mật khẩu qua email (FR03 phần reset), kích hoạt email (FR01), Google OAuth (FR08), duyệt từng đơn đăng ký (FR34), nhắc lịch tự động (FR49), gửi thông báo thủ công (FR50), xuất PDF (FR55), .ics (FR22), đánh giá sự kiện (FR57), nhân bản sự kiện (FR17).

## Giao diện web (frontend)
Đã tích hợp sẵn trong `src/main/resources/static` (HTML/CSS/JS thuần, không cần cài Node). Chạy `mvn spring-boot:run` rồi mở **http://localhost:8080/**.
- Khách: xem/tìm sự kiện, chi tiết + lịch trình · Người tham gia: đăng ký, hủy, xem mã QR (Vé của tôi), thông báo, hồ sơ
- Người tổ chức: tạo/sửa/công bố/hủy sự kiện, lịch trình, danh sách đăng ký, phân công nhân viên, báo cáo + xuất Excel
- Nhân viên: trang Check-in (quét QR bằng camera trên Chrome/Edge hoặc dán mã, check-in thủ công, số liệu thời gian thực)
- Admin: dashboard, quản lý người dùng/vai trò/khóa, duyệt sự kiện, danh mục + địa điểm, nhật ký
Lưu ý: `SecurityConfig` đã được thêm `permitAll` cho `/`, `/css/**`, `/js/**` để trình duyệt tải được giao diện.
