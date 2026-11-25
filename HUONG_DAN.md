# Danh bạ số – Hướng dẫn build và cài đặt

Phát triển bởi Quốc Tuấn · Phiên bản 1.1.2

## Cấu trúc mã nguồn

```
DanhBaSo/
├─ app/build.gradle.kts           ← cấu hình app (tên gói, phiên bản, thư viện)
└─ app/src/main/
   ├─ AndroidManifest.xml         ← quyền của app (KHÔNG có quyền Internet)
   └─ java/com/quoctuan/danhbaso/
      ├─ MainActivity.kt          ← màn hình gốc, thanh dưới, menu ☰, chuyển màn
      ├─ data/
      │  ├─ ContactsRepo.kt       ← đọc/ghi danh bạ chung (Gmail, máy)
      │  ├─ CallLogRepo.kt        ← đọc nhật ký cuộc gọi, gộp mỗi số 1 dòng
      │  ├─ SimRepo.kt            ← đọc/ghi số trong SIM
      │  ├─ BackupCrypto.kt       ← mã hóa AES-256-GCM + PBKDF2 600.000 vòng
      │  ├─ BackupManager.kt      ← tạo file .dbat và khôi phục vào SIM + Gmail
      │  ├─ AppData.kt, Models.kt, Prefs.kt
      ├─ ui/                      ← các màn hình (theo bản thiết kế 12 ảnh)
      └─ util/
         ├─ Actions.kt            ← Gọi, SMS, Zalo, Chặn số, mở GitHub
         └─ PhoneUtil.kt          ← chuẩn hóa số điện thoại, bỏ dấu tiếng Việt
```

## Bước 1 – Mở project
1. Giải nén file zip vào thư mục riêng, ví dụ `C:\DuAn\DanhBaSo` (KHÔNG để trong `C:\Android`).
2. Mở Android Studio → **File → Open** → chọn thư mục `DanhBaSo` → **OK**.
3. Nếu hỏi *Trust project* → bấm **Trust Project**.
4. Chờ thanh dưới cùng chạy xong **Gradle sync** (lần đầu cần Internet, có thể mất 5–15 phút).

## Bước 2 – Build file APK
**Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
Xong bấm **locate** trong thông báo, file nằm ở `app\build\outputs\apk\debug\app-debug.apk`.

## Bước 3 – Cài lên điện thoại
Cách A: chép `app-debug.apk` sang điện thoại (Zalo, cáp, Drive…) → mở file → cho phép cài từ nguồn này → Cài đặt.
Cách B: cắm cáp, bật Gỡ lỗi USB, bấm nút ▶ Run trong Android Studio.

## Bước 4 – Lần đầu mở app
- Bấm **Cho phép** các quyền: Danh bạ, Nhật ký cuộc gọi, Điện thoại.
- Nếu quyền **Nhật ký cuộc gọi bị khóa xám**: Cài đặt → Ứng dụng → Danh bạ số → nút ⋮ góc trên → **Cho phép cài đặt hạn chế**, rồi bật lại quyền.
- Kiểm tra app không có quyền Internet: Cài đặt → Ứng dụng → Danh bạ số → Quyền.

## Trước khi phát hành lên GitHub
1. Mở `util/Actions.kt`, sửa `GITHUB_RELEASES_URL` thành địa chỉ repo thật.
2. Ngày phát hành tự lấy theo ngày build, không cần sửa.
3. Tạo file khóa ký: **Build → Generate Signed App Bundle / APK → APK → Create new…**
   GIỮ KỸ file `.jks` và mật khẩu. Mất file này thì không cập nhật đè được nữa.
   (File .jks đã được chặn trong .gitignore, không bị đẩy lên GitHub.)
4. Mỗi bản mới: tăng `versionCode` và `versionName` trong `app/build.gradle.kts`.

## Lưu ý
- Lưu vào SIM dùng chuẩn Android 12+; một số máy (nhất là ColorOS) có thể không cho ghi SIM. Khi đó app báo lỗi và vẫn lưu đủ bên Gmail.
- Nút **Chặn số** mở danh sách chặn của điện thoại (dùng chung với app gốc); số đã được chép sẵn, chỉ cần dán.

## Bản 1.0.1 – Build bản chạy mượt (release)
Bản debug chạy chậm hơn nhiều. Để app mượt:
1. Menu **Build → Select Build Variant…** → ở cột *Active Build Variant* đổi `debug` thành **release**.
2. **Build → Generate App Bundles or APKs → Generate APKs**.
3. File nằm ở `app\build\outputs\apk\release\app-release.apk`.
Bản release tạm ký bằng khóa debug nên cài đè lên bản cũ được, không cần gỡ.
