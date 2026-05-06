Foraging App

A native Android app for logging, saving, and mapping fruit trees and useful foraging spots.

Features

- Add and edit tree logs with name, location, notes, tree type, harvest season, photo, and GPS coordinates.
- View saved trees in a searchable list.
- Favorite important trees for quick scanning.
- Open an interactive MapLibre map and long-press to pin a tree.
- Store data locally with Room.

Tech Stack

- Kotlin
- Android Views and ViewBinding
- Room database
- LiveData and ViewModel
- MapLibre Android SDK
- Google Play Services Location
- Glide
- Material Components

Publish Readiness Checklist

1. Build with JDK 17.
2. Use a release signing key stored outside the repository.
3. Update app icon and screenshots for the Play Store listing.
4. Test location, camera, gallery, add/edit/delete, favorite, search, and map flows on a real Android device.
5. Keep database migrations explicit so saved tree logs are not lost.
6. Confirm map tile attribution and usage terms.
7. Add a privacy policy explaining local storage, photos, and location usage.

Foraging Safety Note

This app helps users remember locations. It does not identify plants as safe to eat. Users should confirm every plant, forage legally, avoid polluted areas, and respect private property.
