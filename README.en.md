<img width="1280" height="960" alt="image" src="https://github.com/user-attachments/assets/67eef54a-6aea-486c-9a43-550387652aa8" />

# B-Barq ⚡

> [!NOTE]
> <div dir="ltr" style="text-align: left;">
> 
> [برای مشاهده توضیحات به فارسی کلیک کنید](README.md)
> 
> </div>
B-Barq is an Android app that monitors power outage schedules for multiple places from the official BargheMan.com source and reminds you ahead of time — so your phone never runs out of charge and your work stays safe.

## How It Works
1. **Sign in with your phone number** (OTP verification via BargheMan.com).
2. **Add one or more places**, each with its own Bill ID (شناسه قبض).
3. **Grant the required permissions.**
4. **Start the foreground service** to begin monitoring.

The service checks the outage source every hour, displays the latest schedules for all your places in a persistent notification, and sends reminders before each outage based on the offsets you configure per place.

## Features
* Monitor **multiple places** at once, each with its own outage schedule.
* Hourly monitoring of official outage schedules.
* Persistent, expandable notification showing outages sorted by urgency, with failed places listed separately.
* **Configurable reminder offsets** per place (e.g. 15 min, 30 min, 1 hour before).
* Phone-based sign-in with OTP.
* Material Design 3 UI built with Jetpack Compose.

## Requirements
* Android 7.0+
* Internet access
* A valid phone number for sign-in

## Installation
1. Download the latest APK from [Releases](https://github.com/alijafari-gd/B-Barq/releases).
2. Install it on your device.
3. Sign in, add your place(s), grant permissions, and start the service.

## Privacy
B-Barq uses your phone number only for sign-in authentication, and your Bill ID(s) only to fetch outage schedules. **No personal data is collected or shared.**

## Contribution
Contributions are welcome! If you have ideas, improvements, or bug fixes, feel free to:
* Fork the repository
* Create a new branch
* Submit a pull request

## TODO (Upcoming Features)
* Automatic Bill ID detection based on the user's location
* Widget support for at-a-glance outage status

## License
MIT License

### Made with love (and frequent power cuts) by [AJ](https://github.com/alijafari-gd/). Inspired by his autistic friend.
