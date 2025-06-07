# PennyPal — Smart Budgeting App

**PennyPal** is a fully featured Android budgeting application built using Kotlin, Jetpack Compose, and Firebase Firestore. It allows users to manage their income, track expenses, view budget goals, analyse financial health, and earn rewards for good budgeting habits.

This application was created as part of a practical coursework submission, demonstrating mastery of app architecture, Firebase integration, Compose UI design, and multi-feature mobile development.

---

## 📄 Table of Contents

* [Demo Video](#-demo-video)
* [Features](#-features)
* [Bonus Features](#-bonus-features)
* [Technologies Used](#-technologies-used)
* [UI Design](#-ui-design)
* [Firebase Integration](#-firebase-integration)
* [Testing & GitHub Actions](#-testing--github-actions)
* [Build & Deployment](#-build--deployment)
* [Installation](#-installation)
* [Screenshots](#-screenshots)
* [Credits](#-credits)

---

## 🎥 Demo Video

Click here to view the full walkthrough video of PennyPal: 

* App demonstration on real Android phone
* Voiceover explaining each screen
* Shows live Firestore updates and all major features

---

## 💳 Features

| Feature                       | Description                                                                      |
| ----------------------------- | -------------------------------------------------------------------------------- |
| **Login/Signup**              | Secure user login with Firebase Firestore-based username & password              |
| **Category Management**       | Create and manage expense categories                                             |
| **Add Expenses**              | Log expenses with amount, category, date, start & end time, and photo upload     |
| **View Expense History**      | Filterable by day/week/month/year, includes full expense details & photo preview |
| **Income Tracking**           | Add income entries and calculate total balance dynamically                       |
| **Budget Goals**              | Set monthly minimum and maximum spending goals, stored in Firebase               |
| **Graph Analysis**            | View custom bar graph of spending per category with min/max goal overlays        |
| **Goal Performance Feedback** | Visual progress bar showing budget goal adherence over time                      |
| **Currency Support**          | Multi-currency feature with offline converter                                    |
| **Error Handling**            | Input validation, user-friendly errors, and crash-free UI                        |

---

## 🏰 Bonus Features

| Bonus Feature         | Description                                                                                 |
| --------------------- | ------------------------------------------------------------------------------------------- |
| **🏆 Gamification**   | Users earn badges for smart spending habits (e.g. staying under max goal for 3 months)      |
| **🌐 Multi-Currency** | Users can select their preferred currency and all amounts are converted locally without API |

---

## 📈 Technologies Used

* **Language**: Kotlin 2.0.21
* **Framework**: Jetpack Compose + Material Design 3
* **Database**: Firebase Firestore (NoSQL, real-time)
* **Storage**: Firebase Storage for photos
* **Local Storage**: SharedPreferences for session + currency selection
* **Version Control**: Git + GitHub
* **CI/CD**: GitHub Actions

---

## 📺 UI Design

* Figma-based design
* Yellow, blue and white pastel colour scheme
* Rounded cards, curved headers, clear typography
* Fully responsive and accessibility-friendly
* Custom bar graph with dynamic axis and goal overlays

---

## 💡 Firebase Integration

* All data stored under: `users/{username}/...`
* Collections:

  * `expenses`
  * `incomes`
  * `goals`
  * `categories`
  * `badges`
* Real-time syncing and UI updates with `LaunchedEffect`
* Offline-safe storage where applicable

---

## 🌟 Testing & GitHub Actions

* GitHub Actions used for automated build validation
* Unit tests on ViewModel logic (budget calculation, goal progress)
* Firebase Firestore tested using mocked data in preview mode

---

## 🚀 Build & Deployment

* Built APK:
* Works fully on real Android devices
* No emulator required for showcase

---

## ↓ Installation

1. Clone this repository

```bash
git clone 
```

2. Open in Android Studio (2024.2.2 Ladybug Feature Drop)
3. Connect Firebase project using `google-services.json`
4. Build APK or run on device

---

## 📸 Screenshots

*Include screenshots of:*

* HomeScreen
* Category & Expense Entry
* Graph with min/max overlays
* Badge reward screen
* Currency selection dropdown

---

## ✍️ Credits

Developed by: Paayal Rakesh and Keagan Charl Shaw

---

## 📃 Documentation

* Research & design docs included in repo under `/docs`
* Code comments and logging provided throughout
* View `/README.md` and `/.github/workflows/` for CI/CD

---

