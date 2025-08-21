Foraging App

A simple Android app for tracking and mapping fruit trees and foraging spots. Built with Kotlin, SQLite, and MapLibre.

Features

- Add and save tree logs with:
  - Tree name
  - Location name
  - Optional image
- View all saved logs in a list
- Interactive map (MapLibre) with custom markers
- Simple local database (SQLite)
- Fully offline functionality

Tech Stack

- Kotlin
- Android SDK
- SQLite (via `SQLiteOpenHelper`)
- MapLibre Maps SDK (Mapbox-style)
- RecyclerView
- Material Design (basic)


Installation

1. Clone or download the repo
2. Open in **Android Studio**
3. Make sure you have the following:
   - Internet permission
   - Location permission
4. Run on device or emulator

Future Improvements

- Switch to Room DB
- Save precise map coordinates
- Add photo gallery per log
- Export logs to file
- Cloud sync (Firebase or Dropbox)
- Add marker editing/removal

Author

Built by a software development student as a portfolio project.  
Feel free to fork or contribute!