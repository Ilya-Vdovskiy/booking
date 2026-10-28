# LumaStay — hotel booking demo for Android

Native Android application in **Java 17** for the hotel booking variant of Laboratory Work 02. The app reproduces the C# LumaStay form, splash screen, draft storage, validation, permission flow, local notification, and calendar handoff.

## Open and run

1. Open this folder (`lab-2`) as a project in Android Studio.
2. Let Gradle sync. The project uses Android Gradle Plugin 8.9.2, Gradle 8.13, a version catalog, `compileSdk`/`targetSdk` 35 and `minSdk` 24.
3. Select a connected phone with USB debugging enabled and run the **app** configuration. Alternatively, run `gradlew.bat :app:assembleDebug` and install `app/build/outputs/apk/debug/app-debug.apk`.
4. If Android Studio picks JDK 24 for Gradle and sync fails, set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** to JDK 21 or 17. Java source compatibility remains 17.

No server or account is needed. Booking is a local demonstration; no reservation is sent to a hotel.
The Java port uses application ID `ru.lumastay.java`, so it can coexist with the original C# installation on the same phone.

## What to try

- Tap either date field to select a check-in/check-out range. A valid stay starts today or later, and checkout follows check-in.
- Enter guest details, 10 passport digits, a Russian phone number, email, 1–8 guests and a room category. Wishes are optional.
- Use **Проверить поля** to display errors. Errors also appear when a field loses focus and disappear when corrected. **Подготовить заявку** becomes available only for a valid form.
- Close and reopen the app to verify the draft survives. Confirm a booking to clear it.
- On Android 13+, use **Включить уведомления** to see the explanation and system permission prompt. Denying permission still allows the booking and calendar flow.
- After confirmation, choose **В календарь** to open a prefilled all-day event. Save it in your calendar app. If none is installed, the app shows a message.

The technical rationale is in [docs/Пояснительная_записка.md](docs/Пояснительная_записка.md). Run unit checks with `gradlew.bat :app:testDebugUnitTest`. On Windows, Gradle's JUnit worker may fail to locate test classes when the project path contains Cyrillic; temporarily map this same folder to an ASCII drive letter with `subst X: "<absolute path to lab-2>"`, run `X:\gradlew.bat :app:testDebugUnitTest`, then remove the mapping with `subst X: /D`.
