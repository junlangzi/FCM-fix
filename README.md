# FCM fix

**Giữ kết nối thông báo đẩy (FCM) ổn định trên Android nội địa**

`FCM fix` là ứng dụng Android (Kotlin) giúp giảm trễ hoặc mất thông báo trên các máy OPPO, Realme, OnePlus (ColorOS / OxygenOS), Xiaomi (MIUI / HyperOS), Vivo / iQOO (OriginOS) và các ROM tương tự — nơi Google Play Services và kết nối FCM thường bị hệ thống đóng băng hoặc ngắt ngầm khi tắt màn hình.

---

## Vấn đề cần giải quyết

Trên nhiều máy Android nội địa, thông báo từ Zalo, Messenger, Telegram, app ngân hàng… hay đến muộn hàng chục phút, hoặc chỉ hiện khi mở lại màn hình. Nguyên nhân phổ biến:

1. **NAT timeout nhà mạng di động**  
   Phiên NAT trên 4G/5G thường chỉ giữ khoảng **3–5 phút** không có dữ liệu. Google Play Services mặc định heartbeat FCM có thể dài hơn nhiều, khiến socket bị “cắt im lặng” (silent drop). Push từ server Google bị chặn cho đến khi GMS tự thức lại.

2. **Doze Mode & trình diệt nền OEM**  
   ColorOS, HyperOS, OriginOS… đóng băng tiến trình nền, hạn chế đánh thức app và ngắt kết nối mạng khi màn hình tắt.

3. **Whitelist chưa đủ**  
   Nếu người dùng chưa cho phép tự khởi động / bỏ tối ưu pin cho **Google Play Services** và app nhận thông báo, mọi biện pháp keep-alive đều kém hiệu quả.

`FCM fix` không root, chạy dưới dạng **Foreground Service** + **Notification Listener**, chủ động duy trì kết nối mạng và “đánh thức” GMS theo chu kỳ hợp lý.

---

## Cách hoạt động (tóm tắt kỹ thuật)

| Thành phần | Vai trò |
|------------|---------|
| **Foreground Service** | Giữ process sống, hiện thông báo dịch vụ (có thể ẩn icon status bar) |
| **Notification Listener Service** | Neo app với `system_server`, giảm khả năng bị OEM đóng băng |
| **GMS Heartbeat** | Gửi broadcast `GTALK_HEARTBEAT` / `MCS_HEARTBEAT` tới `com.google.android.gms` |
| **Socket ping (UDP DNS)** | Kết nối ngắn tới resolver công cộng (1.1.1.1, 8.8.8.8…) để làm mới bảng NAT |
| **AlarmManager (exact)** | Lên lịch nhịp keep-alive kể cả khi Doze |
| **Network callback** | Khi đổi Wi‑Fi ↔ 4G, kích hoạt heartbeat ngay |
| **BootReceiver** | Tự khởi động lại service sau khi mở máy (nếu bật) |

Chu kỳ **thích ứng (Auto)**:

- Mạng **di động**: khoảng **3–4 phút** (chống NAT drop)
- **Wi‑Fi**: khoảng **8–15 phút** (tiết kiệm pin hơn)
- Pin yếu / Power Save hệ thống: tự nới chu kỳ

Người dùng cũng có thể chọn cố định **1–10 phút** hoặc **Custom**.

---

## Tính năng chính

### Vận hành
- Ba chế độ: **Luôn chạy** · **Chỉ khi tắt màn hình** · **Tắt chương trình**
- Chu kỳ ping: Auto / cố định / tùy chỉnh
- Tạm dừng · Restart · Tắt từ thông báo dịch vụ
- Tự khởi động sau reboot (tùy chọn)
- Ẩn icon trên thanh trạng thái (channel mức thấp nhất mà Foreground Service cho phép)

### Giám sát
- Thống kê **Ping thành công / thất bại** trong ngày
- Ước tính **mAh** và **% pin** app đã dùng so với **100% dung lượng pin máy**
- Nhật ký real-time (3 giờ chi tiết, các giờ trước gộp và mở rộng)
- Test ping thủ công
- Danh sách IP/DNS tùy chỉnh (xoay vòng / ngẫu nhiên / cố định)

### Hỗ trợ người dùng
- Checklist whitelist theo **hãng máy** (deep-link Auto-start, pin, GMS…)
- Giao diện sáng / tối / theo hệ thống
- Reset thống kê theo giờ–phút do người dùng chọn

---

## Yêu cầu hệ thống

| Hạng mục | Chi tiết |
|----------|----------|
| Android | **8.0 (API 26)** trở lên |
| Package | `com.fcmguard.app` |
| Phiên bản | **1.1.3** |
| Root | **Không cần** |
| Google Play Services | Nên có và được whitelist |

---

## Hướng dẫn cài đặt & sử dụng

1. Cài APK (cấp quyền **Thông báo** nếu máy hỏi).
2. Mở app → bật **Truy cập thông báo** (Notification Listener).
3. Bật **Bỏ qua tối ưu pin** cho FCM fix.
4. Vào **Whitelist** và làm lần lượt:
   - Tự khởi động (Auto-start) cho **FCM fix** và **Google Play Services**
   - Không hạn chế pin / nền cho hai app trên  
   (tên mục menu khác nhau tùy ColorOS / HyperOS / OriginOS)
5. (Android 12+) Cho phép **báo thức chính xác** (Exact alarms) nếu hệ thống hỏi.
6. Chọn chế độ chạy:
   - **Chỉ tắt màn hình**: tiết kiệm pin, phù hợp hầu hết người dùng
   - **Luôn chạy**: khi cần nhận notify cả lúc đang dùng máy
7. Nên bật **chu kỳ Auto** trên mạng di động.

Hiệu quả thực tế **phụ thuộc rất lớn** vào việc whitelist đúng. App chỉ hỗ trợ keep-alive; không thể vượt qua mọi chính sách OEM nếu GMS vẫn bị đóng băng cứng.

---

## Giới hạn & lưu ý

- **Không root**: không hook Freezer / ELSA / `services.jar` như một số tool cần root.  
- **Foreground Service** bắt buộc có notification (có thể ẩn icon, không tắt hẳn theo chính sách Android).  
- Heartbeat quá dày tốn pin và có thể bị hệ thống nghi ngờ — mặc định Auto đã cân bằng.  
- Một số nhà mạng / APN đặc biệt có NAT timeout rất ngắn; có thể cần chu kỳ 2–3 phút.  
- App **không đọc nội dung** thông báo của bạn; Notification Listener chỉ dùng để neo process.

---

## Xây dựng từ mã nguồn

```text
Android Studio Hedgehog trở lên · JDK 17 · compileSdk 35
```

```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/`

Thư viện chính: Jetpack Compose, Material 3, Room, DataStore, Coroutines, AlarmManager, ConnectivityManager.

---

## Cấu trúc module (rút gọn)

```text
app/src/main/java/com/fcmguard/app/
├── service/          # FcmGuardService, NotificationListener
├── receiver/         # Boot, Heartbeat alarm, notification actions
├── helper/           # Heartbeat, Network, Battery, Whitelist, OEM
├── data/             # DataStore preferences, Room logs
└── ui/               # Compose screens (Dashboard, Settings, Stats, Whitelist)
```

---

## Giấy phép & trách nhiệm

Phần mềm được cung cấp nhằm **hỗ trợ trải nghiệm thông báo** trên thiết bị cá nhân.  
Tác giả không chịu trách nhiệm nếu OEM thay đổi chính sách nền hoặc nhà mạng thay đổi hành vi NAT.  
Hãy dùng có trách nhiệm, không lạm dụng keep-alive gây hao pin không cần thiết.

---

**FCM fix** — giữ kênh thông báo thông suốt hơn trên Android nội địa.
