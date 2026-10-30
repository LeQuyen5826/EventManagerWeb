# Hệ thống quản lý sự kiện (Spring Boot)

Dự án môn **Kỹ năng lập trình nâng cao**, cài đặt đúng các công nghệ trong báo cáo mục 3.2 – 3.5.

| Mục báo cáo | Công nghệ | Nằm ở đâu trong code |
|---|---|---|
| 3.2a IoC/DI, Bean | `@RestController @Service @Repository @Component @Configuration/@Bean` | toàn bộ project |
| 3.2b Constructor Injection | `@RequiredArgsConstructor` + field `final` | `CheckInService`, `RegistrationService`... |
| 3.2c AOP | `@Aspect @Around` ghi log + thời gian | `aspect/ServiceLoggingAspect` |
| 3.3a REST API | Controller, ResponseEntity, phân trang | `controller/*` |
| 3.3b JPA/Hibernate | `@ManyToOne/@OneToMany/@OneToOne/@ManyToMany`, `@Lock`, `@EntityGraph`, `@Query` | `entity/*`, `repository/*` |
| 3.4a Validation | `@Valid`, `@NotBlank`, `@Min`, `@Future`... | `dto/*` |
| 3.4b Exception Handling | `@RestControllerAdvice`, `ApiError` | `exception/*` |
| 3.4c Transaction | `@Transactional`, rollback, `@TransactionalEventListener(AFTER_COMMIT)` | `RegistrationService.cancel`, `event/*` |
| 3.5 Security | BCrypt, JWT, Filter, Role/Authority, 401/403 JSON | `security/*`, `config/SecurityConfig` |

## Chạy

Yêu cầu: **JDK 25+**, **Maven 3.9+** (hoặc mở bằng IntelliJ, chuột phải `pom.xml` -> Add as Maven Project).

```bash
mvn spring-boot:run                                   # CSDL H2 in-memory, chạy ngay
mvn spring-boot:run -Dspring-boot.run.profiles=mysql  # dùng MySQL (sửa DB_USER, DB_PASSWORD)
mvn test                                              # unit test Mockito
```

- Swagger UI: http://localhost:8080/swagger-ui.html (bấm **Authorize**, dán token từ `/api/auth/login`)
- Gửi email thật: thêm profile `smtp` và đặt biến môi trường `MAIL_USER`, `MAIL_PASSWORD`. Mặc định chỉ ghi log email.
- Đổi khóa JWT bằng biến môi trường `JWT_SECRET` (>= 32 ký tự).

## Tài khoản demo (tự tạo khi khởi động)

| Vai trò | Email | Mật khẩu |
|---|---|---|
| ADMIN | admin@example.com | Admin@123 |
| ORGANIZER | organizer@example.com | Abc@12345 |
| ATTENDEE | attendee@example.com | Abc@12345 |
| STAFF | staff@example.com | Abc@12345 |

Dữ liệu mẫu: sự kiện #1 "Hội thảo AI trong giáo dục" (sức chứa 150, đang mở đăng ký) và sự kiện #2 "Workshop Spring Boot" đang diễn ra
(sức chứa 2) kèm một vé mẫu có mã QR `DEMO-QR-TOKEN-0001` để thử check-in.

## Kịch bản test bằng Postman/Swagger (để chụp ảnh cho báo cáo)

1. `POST /api/auth/login` `{"email":"attendee@example.com","password":"Abc@12345"}` -> 200, lấy `accessToken`.
2. `GET /api/me/profile` không gửi token -> **401**.
3. ATTENDEE gọi `GET /api/admin/users` -> **403**; đăng nhập ADMIN gọi lại -> **200**.
4. ORGANIZER `POST /api/events` với `title` rỗng, `capacity` 0 -> **400** có `errors` theo từng trường.
5. ORGANIZER tạo sự kiện hợp lệ -> **201** + header `Location`, trạng thái `DRAFT`; `POST /api/events/{id}/publish` để công bố.
6. ATTENDEE `POST /api/events/1/registrations` `{"studentId":"BIT240000","organization":"24IT2"}` -> **201**, có `qrToken`; gọi lần 2 -> **409**.
7. ATTENDEE `DELETE /api/registrations/{id}` -> **204** (BR05: phải trước giờ bắt đầu >= 24 giờ).
8. STAFF `POST /api/checkin/scan` `{"qrToken":"DEMO-QR-TOKEN-0001"}` -> **200**; quét lại -> **409**.
9. ORGANIZER `GET /api/events/2/report` -> thống kê; xem console để thấy log của Aspect: `CheckInService.scan -> OK (21 ms)`.

## Cấu hình đáng chú ý (`application.yml`)

- `app.registration.waitlist-enabled`: `true` hết chỗ thì vào danh sách chờ (201, `WAITLISTED`); `false` hết chỗ trả **409**.
- `app.checkin.enforce-window`: bật/tắt kiểm tra khung giờ check-in (BR06).
