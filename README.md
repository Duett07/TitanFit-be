# 🏋️‍♂️ TitanFit Backend — E-Commerce Core API Engine

> Hệ thống máy chủ RESTful API dành cho nền tảng thương mại điện tử **TitanFit** — Chuyên cung cấp trang phục thể thao cao cấp và thực phẩm bổ sung dinh dưỡng thể hình.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

- **Ngôn ngữ & Nền tảng:** Java 21 (LTS) & Spring Boot 4.0.5
- **ORM & Data Access:** Spring Data JPA, Hibernate ORM
- **Cơ sở dữ liệu:** MySQL 8.x
- **Quản lý Migration:** Flyway (`src/main/resources/db/migration`)
- **Bảo mật & Phân quyền:** Spring Security 6 & Stateless JSON Web Token (JWT)
- **Tài liệu API:** SpringDoc OpenAPI 3 (Swagger UI)
- **Tiện ích:** Lombok, Maven Wrapper (`./mvnw`)

---

## 📋 Yêu Cầu Hệ Thống (Prerequisites)

Trước khi chạy dự án, hãy đảm bảo máy tính của bạn đã cài đặt:

1. **Java Development Kit (JDK):** Phiên bản **21** trở lên.
   ```bash
   java -version
   ```
2. **Cơ sở dữ liệu MySQL:** Phiên bản **8.0+** đang chạy ở cổng mặc định `3306`.
   ```bash
   mysql --version
   ```
3. **Maven:** Không bắt buộc cài riêng (dự án đã tích hợp sẵn `./mvnw`).

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Dự Án

### Bước 1: Khởi Tạo Cơ Sở Dữ Liệu MySQL

Đăng nhập vào MySQL CLI hoặc công cụ quản trị (MySQL Workbench, DBeaver, Navicat, phpMyAdmin) và tạo database:

```sql
CREATE DATABASE titanfit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

### Bước 2: Thiết Lập Biến Môi Trường (`.env`)

Sao chép file cấu hình mẫu `.env.example` thành file `.env` ngay trong thư mục này:

```bash
cp .env.example .env
```

Mở file `.env` và cập nhật thông tin tài khoản MySQL của máy bạn:

```env
# Application Profile & Seed
SPRING_PROFILES_ACTIVE=dev
APP_SEED_ENABLED=true

# Database Configuration
MYSQL_HOST=localhost
DB_USERNAME=root
DB_PASSWORD=Mật_khẩu_mysql_của_bạn

# JWT Security (Tạo bằng lệnh: openssl rand -base64 32)
JWT_SECRET=Thay_bằng_chuỗi_base64_32_bytes_của_bạn
JWT_EXPIRATION=2592000000
ACCESS_TOKEN_EXP=1800
REFRESH_TOKEN_EXP=604800

# Initial Admin Account (Bắt buộc mật khẩu mạnh >= 8 ký tự khi chạy production)
INIT_ADMIN_EMAIL=admin@titanfit.vn
INIT_ADMIN_PASSWORD=Mật_khẩu_admin_bảo_mật_cao
INIT_ADMIN_PHONE=0900000001

# Demo Buyer Account (Chỉ hoạt động ở profile dev và khi APP_SEED_ENABLED=true)
INIT_BUYER_EMAIL=hoang.nguyen@example.com
INIT_BUYER_PASSWORD=123456
INIT_BUYER_PHONE=0912345678
```

> **Lưu ý bảo mật & Môi trường:**
> - Khóa `JWT_SECRET` bắt buộc phải là chuỗi Base64 với độ dài sau giải mã tối thiểu 32 bytes (256 bits).
>   - Sinh khóa trên Linux/macOS/Git Bash: `openssl rand -base64 32`
>   - Trên Windows PowerShell: `[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Max 256 }))`
> - `APP_SEED_ENABLED`: Khi `true` (mặc định ở profile `dev`), hệ thống sẽ nạp danh mục, sản phẩm mẫu và tài khoản buyer demo. Ở production, seeder dữ liệu mẫu tự động bị vô hiệu hóa (`false`).
> - `INIT_ADMIN_PASSWORD`: Ở production (`SPRING_PROFILES_ACTIVE=prod`), hệ thống sẽ **từ chối khởi động (fail-fast)** nếu mật khẩu admin bị thiếu, ngắn hơn 8 ký tự hoặc chứa các từ khóa yếu mặc định (`admin123`, `123456`, `password`, `CHANGE_ME`).
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

## 🌱 Cơ Chế Tự Động Nạp Dữ Liệu (Auto Data Seeding)

Khi khởi động server lần đầu tiên, hệ thống sẽ tự động thực hiện 3 công việc:

1. **Flyway Migration:** Tự động tạo bảng dữ liệu chuẩn hóa (`users`, `categories`, `products`, `product_variants`, `orders`, `order_items`, `reviews`).
2. **Khởi tạo tài khoản mẫu (`DataInitializer`):**
   - **Quản trị viên (Admin):**
     - Email: `admin@titanfit.vn`
     - Mật khẩu: `admin123`
     - Quyền: `ROLE_ADMIN` (truy cập quản lý đơn hàng, kho sản phẩm)
   - **Khách hàng mẫu (Customer):**
     - Email: `hoang.nguyen@example.com`
     - Mật khẩu: `123456`
     - Quyền: `ROLE_CUSTOMER`
3. **Nạp 10 Danh mục & 20 Sản phẩm thực tế (`CategorySeeder` & `ProductSeeder`):**
   - **10 Đồ thể thao & Phụ kiện:** Áo thun Pro-Combat, Quần tập gym 2 lớp, Áo Dry-Fit, Giày chạy bộ Running Pro, Bình lắc Shaker Inox 700ml, Đai lưng Lever Belt 10mm, Quấn cổ tay trợ lực, Găng tay chống chai, Bộ dây kháng lực 150 LBS, Con lăn tập bụng bánh đôi.
   - **10 Thực phẩm bổ sung:** Whey Gold Standard 100%, Mutant Iso Surge 5lbs, Rule 1 Protein Isolate, OstroVit Creatine Monohydrate 500g, Pre-workout Cellucor C4, Pre-workout ABE Applied Nutrition, Scivation Xtend BCAA, Sữa tăng cân Mutant Mass XXXtreme 12 Lbs, Dầu cá Now Foods Ultra Omega-3, Vitamin tổng hợp Mutant Multi.
   - **Chất lượng hình ảnh:** Toàn bộ ảnh đều liên kết tới Unsplash với thông số `fit=crop&w=800&h=800` chuẩn vuông tỉ lệ 1:1, không bị co méo hay vỡ khung giao diện.

---

## 📖 Tài Liệu API & Swagger UI

Sau khi server khởi động, bạn có thể tương tác và thử nghiệm toàn bộ API trực tiếp qua giao diện Swagger UI:

- **Swagger UI Interactive:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Schema (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 📡 Danh Sách Các Endpoint Chính

### 1. Xác thực & Tài khoản (`/api/auth`)
| Phương thức | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `POST` | `/api/auth/register` | Đăng ký tài khoản khách hàng mới | Công khai |
| `POST` | `/api/auth/login` | Đăng nhập nhận JWT Token | Công khai |
| `GET` | `/api/auth/me` | Lấy thông tin tài khoản hiện tại | Đã đăng nhập |

### 2. Danh mục sản phẩm (`/api/categories`)
| Phương thức | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `GET` | `/api/categories` | Lấy danh sách toàn bộ danh mục | Công khai |
| `GET` | `/api/categories/{id}` | Lấy chi tiết danh mục theo ID | Công khai |

### 3. Sản phẩm (`/api/products`)
| Phương thức | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `GET` | `/api/products` | Danh sách sản phẩm (có phân trang, lọc danh mục, tìm kiếm) | Công khai |
| `GET` | `/api/products/{id}` | Lấy chi tiết sản phẩm theo UUID | Công khai |
| `GET` | `/api/products/slug/{slug}` | Lấy chi tiết sản phẩm kèm biến thể theo slug thân thiện | Công khai |

### 4. Đơn hàng (`/api/orders`)
| Phương thức | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `POST` | `/api/orders` | Tạo đơn hàng mới | Đã đăng nhập |
| `GET` | `/api/orders/my-orders` | Xem lịch sử đơn hàng của tôi | Đã đăng nhập |
| `GET` | `/api/orders/{orderCode}` | Tra cứu trạng thái đơn hàng theo mã đơn | Công khai |

### 5. Dành cho Quản trị viên (`/api/admin`)
| Phương thức | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `GET` | `/api/admin/products` | Danh sách toàn bộ sản phẩm (kèm ẩn/hiện) | `ROLE_ADMIN` |
| `POST` | `/api/admin/products` | Thêm mới sản phẩm kèm các biến thể SKU | `ROLE_ADMIN` |
| `PUT` | `/api/admin/products/{id}` | Cập nhật thông tin sản phẩm và biến thể | `ROLE_ADMIN` |
| `DELETE` | `/api/admin/products/{id}` | Xóa sản phẩm khỏi hệ thống | `ROLE_ADMIN` |
| `GET` | `/api/admin/orders` | Quản lý danh sách đơn hàng toàn hệ thống | `ROLE_ADMIN` |
| `PATCH`| `/api/admin/orders/{id}/status` | Cập nhật trạng thái đơn (PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED) | `ROLE_ADMIN` |

---

## 🌐 Cấu Hình CORS & Tích Hợp Frontend

Hệ thống backend đã được thiết lập CORS sẵn sàng tích hợp với ứng dụng Frontend (Next.js/React) tại:
- `http://localhost:3000` (Hỗ trợ đầy đủ `credentials`, header `Authorization` và các phương thức `GET, POST, PUT, DELETE, PATCH, OPTIONS`).

---

## ❓ Xử Lý Sự Cố Thường Gặp (Troubleshooting)

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
