# TitanFit Backend — E-Commerce Core API Engine

> Hệ thống máy chủ RESTful API dành cho nền tảng thương mại điện tử TitanFit — Chuyên cung cấp trang phục thể thao cao cấp và thực phẩm bổ sung dinh dưỡng thể hình.

---

## Công Nghệ Sử Dụng (Tech Stack)

- **Ngôn ngữ & Nền tảng:** Java 21 (LTS) & Spring Boot 4.0.5
- **ORM & Data Access:** Spring Data JPA, Hibernate ORM
- **Cơ sở dữ liệu:** MySQL 8.x
- **Quản lý Migration:** Flyway (`src/main/resources/db/migration`)
- **Bảo mật & Phân quyền:** Spring Security 6 & Stateless JSON Web Token (JWT)
- **Bộ nhớ đệm & Blacklist:** Redis (Spring Data Redis)
- **Kiểm soát lưu lượng (Rate Limiting):** Bucket4j
- **Tài liệu API:** SpringDoc OpenAPI 3 (Swagger UI)
- **Tiện ích:** Lombok, Maven Wrapper (`./mvnw`)

---

## Yêu Cầu Hệ Thống (Prerequisites)

Trước khi chạy dự án, hãy đảm bảo máy tính của bạn đã cài đặt:

1. **Java Development Kit (JDK):** Phiên bản **21** trở lên.
   ```bash
   java -version
   ```
2. **Cơ sở dữ liệu MySQL:** Phiên bản **8.0+** đang chạy ở cổng mặc định `3306`.
   ```bash
   mysql --version
   ```
3. **Redis Server:** Phiên bản **6.0+** đang chạy ở cổng mặc định `6379` (dùng để lưu Refresh Token, Token Blacklist và Rate Limiting).
   ```bash
   redis-cli ping
   ```
4. **Maven:** Không bắt buộc cài riêng (dự án đã tích hợp sẵn `./mvnw`).

---

## Hướng Dẫn Cài Đặt & Chạy Dự Án

### Bước 1: Khởi Tạo Cơ Sở Dữ Liệu MySQL

Đăng nhập vào MySQL CLI hoặc công cụ quản trị (MySQL Workbench, DBeaver, Navicat, phpMyAdmin) và tạo database:

```sql
CREATE DATABASE titanfit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

### Bước 2: Thiết Lập Biến Môi Trường (.env)

Sao chép file cấu hình mẫu `.env.example` thành file `.env` ngay trong thư mục `be/`:

```bash
cp .env.example .env
```

Mở file `.env` và cập nhật thông tin môi trường của bạn:

```env
# Application Profile & Seed
SPRING_PROFILES_ACTIVE=dev
APP_SEED_ENABLED=true

# Database Configuration
MYSQL_HOST=localhost
DB_USERNAME=root
DB_PASSWORD=Mat_khau_mysql_cua_ban

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT & Cookie Security (Tạo bằng lệnh: openssl rand -base64 32)
JWT_SECRET=Thay_bang_chuoi_base64_32_bytes_cua_ban
ACCESS_TOKEN_EXP=1800
REFRESH_TOKEN_EXP=604800
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Initial Admin Account (Bắt buộc mật khẩu mạnh >= 8 ký tự khi chạy production)
INIT_ADMIN_EMAIL=admin@titanfit.vn
INIT_ADMIN_PASSWORD=Mat_khau_admin_bao_mat_cao
INIT_ADMIN_PHONE=0900000001

# Demo Buyer Account (Chỉ hoạt động ở profile dev và khi APP_SEED_ENABLED=true)
INIT_BUYER_EMAIL=hoang.nguyen@example.com
INIT_BUYER_PASSWORD=123456
INIT_BUYER_PHONE=0912345678
```

> **Lưu ý bảo mật & Môi trường:**
> - **Cơ chế Token & Phòng chống CSRF**: API được bảo vệ nghiêm ngặt bằng Header `Authorization: Bearer <access_token>`. Cookie HttpOnly chỉ dùng lưu trữ và cấp lại Refresh Token trên endpoint `/api/auth`. Cơ chế này loại bỏ hoàn toàn nguy cơ tấn công CSRF.
> - **Cookie Security (`COOKIE_SECURE`)**: Đặt `false` trong môi trường phát triển HTTP và bắt buộc `true` trên Production có HTTPS để cấm truyền cookie qua kết nối không mã hóa.
> - **Thời hạn Token**: `ACCESS_TOKEN_EXP` (mặc định 1800 giây = 30 phút), `REFRESH_TOKEN_EXP` (mặc định 604800 giây = 7 ngày). Khi logout, Access Token được đưa vào Redis blacklist và Refresh Token bị thu hồi ngay lập tức.
> - **Cơ chế Refresh Token Rotation**: Gọi `POST /api/auth/refresh` để cấp cặp token mới an toàn, token cũ tự hủy.
> - **Khóa `JWT_SECRET`**: Bắt buộc phải là chuỗi Base64 với độ dài sau giải mã tối thiểu 32 bytes (256 bits).
>   - Sinh khóa trên Linux/macOS/Git Bash: `openssl rand -base64 32`
>   - Trên Windows PowerShell: `[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Max 256 }))`
> - **Cấu hình `APP_SEED_ENABLED`**: Khi `true` (mặc định ở profile `dev`), hệ thống sẽ nạp danh mục, sản phẩm mẫu và tài khoản buyer demo. Ở production, seeder dữ liệu mẫu tự động bị vô hiệu hóa (`false`).
> - **Bảo mật `INIT_ADMIN_PASSWORD`**: Ở production (`SPRING_PROFILES_ACTIVE=prod`), hệ thống sẽ từ chối khởi động (fail-fast) nếu mật khẩu admin bị thiếu, ngắn hơn 8 ký tự hoặc chứa các từ khóa yếu mặc định (`admin123`, `123456`, `password`, `CHANGE_ME`).
> - Spring Boot tự động nạp biến từ file `.env` qua cấu hình `spring.config.import`.

---

### Bước 3: Khởi Chạy Ứng Dụng

Bạn có thể chạy dự án bằng 1 trong 2 cách sau:

#### Cách 1: Chạy trực tiếp qua Maven Wrapper (Khuyên dùng cho Development)
```bash
# Trên Linux/macOS:
./mvnw spring-boot:run

# Trên Windows (Command Prompt hoặc PowerShell):
mvnw.cmd spring-boot:run
```

#### Cách 2: Đóng gói và chạy file JAR độc lập (Production build)
```bash
# 1. Đóng gói ứng dụng:
./mvnw clean package -DskipTests

# 2. Khởi chạy file JAR vừa sinh ra:
java -jar target/be-0.0.1-SNAPSHOT.jar
```

Khi màn hình terminal hiển thị dòng thông báo sau là ứng dụng đã khởi động thành công:
```text
Started BeApplication in X.XXX seconds (process running for X.XXX)
```

Server backend lắng nghe tại cổng mặc định: `http://localhost:8080`.

---

## Cơ Chế Khởi Tạo Dữ Liệu & Migration

Khi khởi động server lần đầu tiên, hệ thống sẽ tự động thực hiện:

1. **Flyway Migration:** Tự động tạo bảng dữ liệu chuẩn hóa (`users`, `categories`, `products`, `product_variants`, `orders`, `order_item`, `reviews`). Bảng `categories` được migration nạp sẵn 2 danh mục cốt lõi (`do-the-thao` và `thuc-pham-bo-sung`).
2. **Khởi tạo tài khoản mẫu (`DataInitializer`):**
   - **Quản trị viên (Admin):**
     - Email: `admin@titanfit.vn` (hoặc theo cấu hình `INIT_ADMIN_EMAIL`)
     - Quyền: `ROLE_ADMIN` (truy cập quản lý đơn hàng, kho sản phẩm, thống kê hệ thống)
   - **Khách hàng mẫu (Customer - chỉ hoạt động ở profile `dev` khi `APP_SEED_ENABLED=true`):**
     - Email: `hoang.nguyen@example.com` (hoặc theo cấu hình `INIT_BUYER_EMAIL`)
     - Quyền: `ROLE_CUSTOMER`
3. **Nạp dữ liệu mẫu phát triển (`CategorySeeder` & `ProductSeeder`):**
   - Chỉ hoạt động khi `APP_SEED_ENABLED=true` (profile `dev`).
   - `CategorySeeder` nạp bổ sung 10 danh mục chi tiết (Whey Protein, BCAA, Pre-Workout, Phụ kiện gym, Quần áo thể thao, Mass Gainer, Creatine, Vitamin, Shaker, Snack Healthy).
   - `ProductSeeder` nạp 20 sản phẩm thực tế kèm các biến thể SKU (kích cỡ, hương vị, trọng lượng).
   - Toàn bộ ảnh đều liên kết tới Unsplash với thông số `fit=crop&w=800&h=800` chuẩn vuông tỉ lệ 1:1.

---

## Tài Liệu API & Swagger UI

Sau khi server khởi động (ở môi trường Development), bạn có thể tương tác và thử nghiệm toàn bộ API trực tiếp qua giao diện Swagger UI:

- **Swagger UI Interactive:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Schema (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> **Lưu ý bảo mật:** Swagger/OpenAPI được bật mặc định ở môi trường `dev`. Trên môi trường `prod`, Swagger tự động bị tắt nhằm chống rò rỉ cấu trúc hệ thống (`springdoc.api-docs.enabled=false`) và được bảo vệ nhiều tầng (yêu cầu quyền `ROLE_ADMIN` nếu được kích hoạt qua biến môi trường `SPRINGDOC_ENABLED=true`).

---

## Danh Sách Các Endpoint Chính

### 1. Xác thực & Tài khoản (`/api/auth`)
| Phương thức | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `POST` | `/api/auth/register` | Đăng ký tài khoản khách hàng mới | Công khai (có Rate Limiting) |
| `POST` | `/api/auth/login` | Đăng nhập nhận JWT Access Token và Refresh Token | Công khai (có Rate Limiting) |
| `POST` | `/api/auth/refresh` | Làm mới cặp token khi Access Token hết hạn | Công khai / Cookie Refresh Token |
| `POST` | `/api/auth/logout` | Đăng xuất, đưa Access Token vào blacklist và thu hồi Refresh Token | Đã đăng nhập |
| `GET` | `/api/auth/me` | Lấy thông tin tài khoản hiện tại | Đã đăng nhập |

### 2. Danh mục sản phẩm (`/api/categories`)
| Phương thức | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `GET` | `/api/categories` | Lấy danh sách toàn bộ danh mục | Công khai |

### 3. Sản phẩm (`/api/products`)
| Phương thức | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `GET` | `/api/products` | Danh sách sản phẩm (có phân trang, tìm kiếm từ khóa, lọc danh mục, khoảng giá, sắp xếp) | Công khai |
| `GET` | `/api/products/{slug}` | Lấy chi tiết sản phẩm kèm danh sách biến thể SKU theo slug thân thiện | Công khai |

### 4. Đơn hàng (`/api/orders`)
| Phương thức | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `POST` | `/api/orders` | Tạo đơn hàng mới | Đã đăng nhập |
| `POST` | `/api/orders/track` | Tra cứu trạng thái đơn hàng công khai (yêu cầu khớp chính xác mã đơn và số điện thoại) | Công khai |
| `GET` | `/api/orders/my-orders` | Xem lịch sử đơn hàng của tôi (hỗ trợ phân trang `page`, `size`, lọc `status`) | Đã đăng nhập |
| `GET` | `/api/orders/{orderCode}` | Lấy chi tiết đơn hàng theo mã đơn | Đã đăng nhập (chỉ chủ đơn hàng hoặc `ROLE_ADMIN`) |

### 5. Dành cho Quản trị viên (`/api/admin`)
| Phương thức | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `GET` | `/api/admin/stats` | Thống kê số lượng đơn hàng, doanh thu, sản phẩm và khách hàng | `ROLE_ADMIN` |
| `GET` | `/api/admin/products` | Danh sách toàn bộ sản phẩm (hỗ trợ phân trang, tìm kiếm, lọc danh mục, trạng thái ẩn/hiện) | `ROLE_ADMIN` |
| `POST` | `/api/admin/products` | Thêm mới sản phẩm kèm các biến thể SKU | `ROLE_ADMIN` |
| `PUT` | `/api/admin/products/{id}` | Cập nhật thông tin sản phẩm và biến thể | `ROLE_ADMIN` |
| `PATCH`| `/api/admin/products/{id}/toggle-active` | Thay đổi trạng thái hiển thị ẩn/hiện sản phẩm | `ROLE_ADMIN` |
| `DELETE` | `/api/admin/products/{id}` | Xóa sản phẩm khỏi hệ thống | `ROLE_ADMIN` |
| `GET` | `/api/admin/orders` | Quản lý danh sách đơn hàng toàn hệ thống (hỗ trợ phân trang `page`, `size`, lọc `status`, `paymentStatus`, `startDate`, `endDate`, `search`) | `ROLE_ADMIN` |
| `PATCH`| `/api/admin/orders/{id}/status` | Cập nhật trạng thái đơn (PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED) | `ROLE_ADMIN` |
| `PATCH`| `/api/admin/orders/{id}/payment-status` | Cập nhật trạng thái thanh toán (UNPAID, PAID, REFUNDED) | `ROLE_ADMIN` |

---

## Cấu Hình CORS & Tích Hợp Frontend

Hệ thống backend hỗ trợ cấu hình danh sách Frontend Origin động thông qua biến môi trường `CORS_ALLOWED_ORIGINS` (hoặc cấu hình `app.cors.allowed-origins` trong properties):
- **Môi trường Development:** Mặc định cho phép `http://localhost:3000`, `http://localhost:5173`, `http://127.0.0.1:3000`, `http://127.0.0.1:5173`.
- **Môi trường Production:** Đọc danh sách origin được chỉ định qua biến `CORS_ALLOWED_ORIGINS` (ví dụ: `https://titanfit.vn,https://admin.titanfit.vn`).
- Hỗ trợ đầy đủ `credentials` (cookie & auth header), thu hẹp headers an toàn (`Authorization`, `Content-Type`, `Accept`, `Origin`, `X-Requested-With`, `Access-Control-Request-*`), expose `Retry-After` (phục vụ Rate Limiting), và tự động cache preflight request 1 giờ.

---

## Xử Lý Sự Cố Thường Gặp (Troubleshooting)

1. **Lỗi kết nối MySQL (`Communications link failure`):**
   - Đảm bảo MySQL server đang bật: `sudo systemctl status mysql` (Linux) hoặc kiểm tra qua Services (Windows).
   - Kiểm tra `DB_USERNAME` và `DB_PASSWORD` trong file `.env` đã trùng khớp với tài khoản MySQL chưa.
   - Đảm bảo đã chạy câu lệnh `CREATE DATABASE titanfit;`.

2. **Lỗi cổng 8080 bị chiếm dụng (`Port 8080 was already in use`):**
   - Tìm và tắt tiến trình đang chiếm cổng:
     ```bash
     # Trên Linux:
     lsof -i :8080
     kill -9 <PID>
     ```
   - Hoặc đổi cổng khác trong file `application.properties`: `server.port=8081`.

3. **Lỗi `Access Denied` khi gọi API Admin:**
   - Đảm bảo trong Header request có gửi kèm Token hợp lệ:
     ```http
     Authorization: Bearer <access_token_cua_admin>
     ```
