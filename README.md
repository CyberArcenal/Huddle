# 📱 Huddle Android App
> A modern, feature‑rich social networking mobile client built with Jetpack Compose.

[`https://kotlinlang.org/`](https://kotlinlang.org/)  
[`https://developer.android.com/jetpack/compose`](https://developer.android.com/jetpack/compose)  
[`https://opensource.org/licenses/Apache-2.0`](https://opensource.org/licenses/Apache-2.0)

**Huddle** is designed for fast and seamless social interaction. Using the latest Android technologies, it provides an intuitive interface for profiles, stories, messaging, and group engagements.

---

## 🌟 Key Features

### 👤 User & Social
- **Dynamic Profiles** – Full customization of profile photos, cover photos, and bio.
- **Follow System** – Real‑time follow/unfollow functionality with mutual follower tracking.
- **Secure Auth** – JWT‑based authentication with refresh token logic and 2FA support.

### 📸 Content Engagement
- **Infinite Feed** – Smooth scrolling of posts with support for likes, comments, and reactions.
- **Native Stories** – Instagram‑style stories viewer with analytics and **Highlights** creation.
- **Reels & Video** – Short‑form video playback optimized for mobile data.

### 💬 Community & Messaging
- **Real‑time Chat** – Direct and group messaging with read receipts.
- **Groups & Events** – Discover and join communities; RSVP to upcoming events.
- **Smart Notifications** – Instant alerts for all social activities.

---

## 🛠 Tech Stack (Developer Friendly)

This project follows **Modern Android Development (MAD)** practices:

* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Declarative UI)
* **Architecture**: MVVM (Model‑View‑ViewModel) with modular managers
* **Async/Streams**: Kotlin Coroutines & Flow
* **Networking**: [Retrofit](https://square.github.io/retrofit/) & OkHttp (JWT Auth with Refresh Tokens)
* **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
* **Paging**: Paging 3 library for efficient list loading

---

## 🏗 Project Structure

```text
Huddle-Android/
├── app/src/main/java/com/cyberarcenal/huddle/
│   ├── ui/             # UI Layer (Screens, Components, ViewModels)
│   ├── data/           # Data Layer (Repositories, DTOs, Enums)
│   ├── network/        # API Configuration & Auth Management
│   └── utils/          # Extensions, Formatters, & Helpers
└── build.gradle.kts    # Gradle build configuration
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio **Hedgehog** or newer
- JDK 17
- Android SDK 26 (Android 8.0) or higher

### Setup Instructions
1. **Clone the repo:**
   ```bash
   git clone https://github.com/CyberArcenal/Huddle-Android.git
   ```
2. **API Configuration:**  
   Open `local.properties` and add your base URL:
   ```properties
   BASE_URL="https://your-huddle-api.com/"
   ```
3. **Build & Run:**  
   Sync Gradle and click the **Run** button in Android Studio.

---

## 🤝 Contributing
Contributions are welcome!
1. **Fork** the project
2. Create a **Feature Branch** (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. Open a **Pull Request**

---

## 📞 Support
Questions or feedback?
- Open a [GitHub Issue](https://github.com/CyberArcenal/Huddle/issues)
- Email: `dev@cyberarcenal.com`

**Built with ❤️ by the Huddle Team.**