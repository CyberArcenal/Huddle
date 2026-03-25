# 📱 Huddle Android App
> A modern, feature-rich social networking mobile client built with Jetpack Compose.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Modular-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Ang **Huddle** ay idinisenyo para sa mabilis at seamless na social interaction. Gamit ang pinakabagong Android technologies, nagbibigay ito ng intuitive na interface para sa profiles, stories, messaging, at group engagements.

---

## 🌟 Key Features

### 👤 User & Social
- **Dynamic Profiles** – Full customization ng profile photos, cover photos, at bio.
- **Follow System** – Real-time follow/unfollow functionality na may mutual follower tracking.
- **Secure Auth** – JWT-based authentication na may refresh token logic at 2FA support.

### 📸 Content Engagement
- **Infinite Feed** – Smooth scrolling ng posts na may support para sa likes, comments, at reactions.
- **Native Stories** – Instagram-style stories viewer na may analytics at **Highlights** creation.
- **Reels & Video** – Short-form video playback na optimized para sa mobile data.

### 💬 Community & Messaging
- **Real-time Chat** – Direct at group messaging na may read receipts.
- **Groups & Events** – Tuklasin at sumali sa mga communities; mag-RSVP sa mga upcoming events.
- **Smart Notifications** – Instant alerts para sa lahat ng social activities.

---

## 🛠 Tech Stack (Developer Friendly)

Ang project na ito ay sumusunod sa **Modern Android Development (MAD)** practices:

*   **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Declarative UI)
*   **Architecture**: MVVM (Model-View-ViewModel) with modular managers.
*   **Async/Streams**: Kotlin Coroutines & Flow
*   **Networking**: [Retrofit](https://square.github.io/retrofit/) & OkHttp (JWT Auth with Refresh Tokens)
*   **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
*   **Paging**: Paging 3 library para sa efficient list loading.

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
- Android Studio **Hedgehog** or newer.
- JDK 17.
- Android SDK 26 (Android 8.0) pataas.

### Setup Instructions
1.  **Clone the repo:**
    ```bash
    git clone https://github.com/CyberArcenal/Huddle-Android.git
    ```
2.  **API Configuration:**
    Buksan ang `local.properties` at ilagay ang iyong base URL:
    ```properties
    BASE_URL="https://your-huddle-api.com/"
    ```
3.  **Build & Run:**
    I-sync ang Gradle at i-click ang **Run** button sa Android Studio.

---

## 🤝 Contributing
Welcome ang kahit anong contributions! 
1. **Fork** ang project.
2. Gawa ng **Feature Branch** (`git checkout -b feature/AmazingFeature`).
3. **Commit** ang changes (`git commit -m 'Add some AmazingFeature'`).
4. **Push** sa branch (`git push origin feature/AmazingFeature`).
5. Mag-open ng **Pull Request**.

---

## 📞 Support
May tanong o feedback? 
- Mag-open ng [GitHub Issue](https://github.com/CyberArcenal/Huddle-Android/issues).
- Email: `dev@cyberarcenal.com`

**Built with ❤️ by the Huddle Team.**
