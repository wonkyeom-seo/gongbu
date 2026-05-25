# Foreground Service Declaration

App name: 순공  
Package name: `kr.kro.gongbu33`  
Version name: `alpa01`

## Manifest Usage

The app declares:

- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- `android:foregroundServiceType="specialUse"`

Service:

```text
kr.kro.gongbu33.StudyTimerService
```

## Play Console Declaration Text

Use case:

```text
User-started study timer with persistent notification controls.
```

Detailed explanation:

```text
The app provides a study timer that the user explicitly starts from inside the app. Once started, the timer must continue measuring study time while the app is in the background or the screen is off. A persistent notification shows the current timer state and provides user-visible controls to pause, resume, save, or reset the timer.

Foreground service is required because the timer is an ongoing, user-initiated task that must remain accurate and visible to the user. Without the foreground service, the timer controls would disappear in the background and the user could lose the active study session.

The service does not upload data, track location, play media, record audio, or perform hidden background work. It only maintains the active timer state and notification controls until the user saves or resets the session.
```

User impact if delayed or interrupted:

```text
If the foreground service is stopped, the active study timer may stop updating in the background and the notification controls may disappear. This can cause the user to lose accurate study time and pause duration for the current session.
```

Demo video checklist:

- Start a timer from the measurement screen.
- Put the app in the background.
- Show the persistent notification.
- Use notification controls: pause, resume, save, reset.
- Reopen the app and show that saved records appear in the viewer/calendar.

## Current App Behavior

- The service starts only after the user presses the start button.
- The notification is ongoing while the timer is active.
- The user can stop the active service by saving or resetting the timer.
- Study data is stored locally on the device.
