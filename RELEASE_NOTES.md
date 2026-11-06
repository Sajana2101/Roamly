# Roamly Final POE Release Notes

## Release Information

The release information below identifies the final Android build documented by these notes. The Git repository remains the authoritative source for the final committed code, while this document focuses on what changed functionally and technically after the Part 2 prototype.

| Item | Details |
|---|---|
| Application | Roamly Travel Planning Mobile Application |
| Release | Final POE / Part 3 |
| Application Version | `1.0` |
| Version Code | `1` |
| Release Date | 6 November 2026 |
| Platform | Native Android |
| Primary Language | Kotlin |
| Backend | Node.js, TypeScript and Express |
| Cloud Platform | Microsoft Azure |
| Final Status | Final POE submission release |

________________________________________

## 1. Purpose of The Release Notes

These release notes document the functional and technical changes introduced to Roamly after the Part 2 prototype.

Part 2 established the core travel-planning functionality and the initial three-tier client-server architecture. The Final POE builds on that foundation rather than replacing it.

The final phase introduces the advanced requirements expected for the completed application, strengthens cloud and persistence behaviour and adds the evidence needed to demonstrate that the individual technologies work together.

These notes should be read together with:

- The main `README.md`
- `docs/TESTING.md`
- The final demonstration video
- The signed production APK

________________________________________

## 2. Part 2 Prototype Baseline

The Part 2 prototype provides the baseline used to describe the changes in this release.

At the end of Part 2, Roamly already included:

- Dashboard / Home functionality
- Holiday management
- Itinerary functionality
- Currency functionality
- Packing-list management
- Packing-item management
- Application Settings
- Google-based authentication
- Android-to-backend communication using Retrofit
- A Node.js, TypeScript and Express REST API
- Azure SQL Database persistence through Prisma ORM
- A three-tier client-server structure

The Part 2 README also identified functionality that would be completed later, particularly notifications/reminders, application-wide language switching and biometric authentication.

Because the Part 2 prototype achieved full marks, the Final POE did not need to correct a failed prototype. The release instead extends the successful baseline with the additional advanced and production-readiness requirements.

________________________________________

## 3. Final POE Release Summary

The Final POE introduces or significantly expands the following areas:

- Native Android biometric authentication
- DataStore-based Settings persistence
- Authenticated cloud-backed Settings synchronisation
- Saved Home Currency integration with the Currency Converter
- Room-based offline persistence
- Automatic synchronisation after reconnection
- Firebase Cloud Messaging push notifications
- Azure Blob Storage for Travel Documents
- Azure Table Storage for NoSQL travel notes
- English, Afrikaans, isiZulu and isiXhosa localisation
- Expanded authenticated REST API integration
- User-Defined Feature 4: Travel Management Toolkit
- User-Defined Feature 5: Automated Travel Reminders and Countdowns
- Automated and physical-device testing
- GitHub Actions build/test verification
- Signed release APK and Play Store promotional assets

The sections below explain what changed in each area and why the change matters to the final application.

________________________________________

# 4. Detailed Functional Updates

## 4.1 Authentication and Security

### Google Single Sign-On

Google Sign-In remains the primary external authentication mechanism.

The Final POE strengthens the authentication flow by integrating the Google identity with the custom Roamly backend. After Google authentication, the identity token is validated before a Roamly session is established.

This keeps the external identity step separate from Roamly's own application session and allows protected backend data to remain associated with the authenticated user.

### Biometric Authentication - Added in Final POE

Native Android biometric authentication was introduced as an additional secure access mechanism.

Roamly invokes the Android biometric framework rather than implementing a custom fingerprint interface. Biometric templates remain under operating-system control and are not stored by the application.

The final physical-device evidence confirms that Android displays the protected fingerprint prompt over the Roamly Unlock screen.

**Release impact:**

- Adds a second secure access mechanism
- Uses Android's protected biometric framework
- Avoids storing biometric templates inside Roamly
- Retains Google Sign-In as an alternative authentication path

________________________________________

## 4.2 Settings and User Preference Persistence

### DataStore Persistence - Added in Final POE

The Settings implementation was expanded from the prototype to use Android DataStore for lightweight preference persistence.

The final Settings repository manages preferences including:

- Preferred application language
- Home currency
- Travel-reminder preference

The repository also supports migration from the earlier Part 2 preference file. This allows the implementation to move to the more suitable DataStore approach without treating the Final POE as an unrelated replacement of the prototype.

### Cloud-Backed Settings Synchronisation

Settings are synchronised through the authenticated `/api/settings` endpoint.

This provides a local-first approach: supported preferences are saved on the device and can then be associated with the authenticated Roamly account through the API.

### Currency Converter Integration

The saved Home Currency is no longer isolated inside Settings.

When the Currency Converter opens, the selected Home Currency is automatically applied as the source currency. This creates direct cross-feature use of a stored user preference.

### Settings Test Coverage

Automated tests were added for DataStore persistence and language mapping.

The Settings-specific suite completed:

- 8 tests
- 0 failures
- 0 ignored tests
- 100% successful execution

________________________________________

## 4.3 Offline Mode and Local Persistence

### Room Offline Storage

Room is used as the structured local persistence layer for supported offline functionality.

This is particularly relevant to a travel application because connectivity may be weak, expensive or unavailable while a user is travelling.

The final Packing test demonstrated that a list and its items could be created while the device was offline and that the packed state remained available locally.

### Automatic Reconnection Synchronisation

WorkManager coordinates supported pending synchronisation after connectivity is restored.

The final test used `Offline Test Packing` so that the locally created list and item states could be matched directly with Azure SQL after reconnection.

Azure SQL contained:

- `Offline Test Packing`
- `Offline Test Passport`
- `Offline Test Hotel Accommodation`

The packed/unpacked state also matched the earlier offline test.

**Release impact:**

The final architecture is therefore no longer limited to direct cloud access. Supported user actions can pass through local Room persistence and be reconciled with the cloud later.

________________________________________

## 4.4 Real-Time Notifications

### Firebase Cloud Messaging - Added in Final POE

Firebase Cloud Messaging is integrated to provide real-time push-notification capability.

The final Android project includes `RoamlyMessagingService`, which receives Firebase notification/data payloads and displays the appropriate Android notification.

The physical-device test received **Roamly - Test Notification** with the expected Roamly message in the Android notification shade.

This provides direct delivery evidence rather than only configuration evidence.

________________________________________

## 4.5 Multi-Language Support

The Part 1 design planned support for multiple South African languages.

The completed Final POE contains four language resource sets:

- English
- Afrikaans
- isiZulu
- isiXhosa

The Android resource directories are:

```text
values/
values-af-rZA/
values-zu/
values-xh/
```

Language names are mapped to the codes `en`, `af`, `zu` and `xh`.

The selected language is applied through `AppCompatDelegate.setApplicationLocales(...)` and the preference is retained through the Settings implementation.

Final physical-device evidence verifies both:

- Successful switching to Afrikaans
- Afrikaans remaining active after the application was reopened

**Release impact:**

The language selector is therefore no longer only a stored preference from the prototype. It is connected to the actual Android locale resources and changes the visible application interface.

________________________________________

## 4.6 Travel Documents and Azure Blob Storage

### Cloud File Storage - Added in Final POE

Travel-document handling was expanded so that file content is stored in Azure Blob Storage while structured document metadata remains available through the backend and relational data layer.

The final test uploaded:

- Document Name: `Boarding Pass`
- Document Type: `Flight`
- File: `Morocco Trip - Boarding Pass.pdf`
- Notes: `Boarding Pass for outbound flight`

Roamly displayed the uploaded Boarding Pass successfully.

Azure SQL contained the matching TravelDocument metadata and Azure Blob Storage contained the corresponding PDF object in the `travel-documents` container.

The Blob evidence also shows the PDF content type, object size and server-encrypted state.

**Release impact:**

This separates large file content from relational application data while still allowing the Travel Documents feature to present both as one user-facing record.

________________________________________

## 4.7 NoSQL Integration

### Azure Table Storage - Added in Final POE

Roamly uses **Azure Table Storage** for the Final POE NoSQL requirement.

The backend exposes authenticated `/api/travel-notes` functionality and stores notes in the `RoamlyTravelNotes` table.

Stored note data includes:

- Title
- Content
- Category
- Tags
- Created timestamp
- Updated timestamp

The authenticated user is represented through the storage key structure so that the backend can work with the correct user's data.

The final Azure evidence shows a `POE NoSQL Test` entity with its content, category, tags and timestamps.

**Release impact:**

The final application therefore uses both relational and NoSQL cloud storage for different data models rather than forcing all data into Azure SQL.

________________________________________

## 4.8 REST API and Cloud Data Integration

The custom REST API remains the main communication layer between Android and the cloud.

The Final POE expands the backend's responsibilities because it now coordinates a broader set of services and authenticated data flows.

The backend supports areas including:

- Authentication validation
- User-specific authorisation
- Holiday and itinerary operations
- Packing and synchronisation
- Reminders
- Settings
- Travel Document metadata
- Azure Blob integration
- Azure Table Storage travel notes

A final API round-trip test used the Cape Town Final POE holiday.

The holiday was visible in Roamly and a matching Azure SQL record was found with the expected Cape Town / South Africa values and 13–20 November 2026 dates.

This provides stronger integration evidence than an app-only screenshot because it verifies the cloud persistence behind the Android interface.

________________________________________

## 4.9 Holiday and Itinerary Functionality

The core holiday and itinerary functionality established during the prototype remains part of the final application.

Holiday records can include information such as:

- Trip name
- Trip classification
- Destination
- Start and end dates
- Accommodation details
- Flight information
- Status and countdown information

The itinerary area continues the original design principle of organising trip information chronologically by day.

The final multilingual evidence also demonstrates that the itinerary interface participates in the localisation implementation rather than existing as a separate English-only section.

________________________________________

## 4.10 Packing Functionality

Packing functionality remains a major part of the Travel Management Toolkit.

The final application supports:

- Multiple packing lists
- Packing items
- Editing and deletion
- Packed/unpacked state
- Automatic packing progress
- Offline local persistence
- Reconnection synchronisation

Compared with Part 2, the main Final POE improvement is that Packing is no longer dependent only on an active cloud connection.

________________________________________

## 4.11 Reminders and Countdown Functionality

Notifications and reminders were identified as later work in the Part 2 README.

The final application includes reminder management and holiday countdown information.

Reminder data can include:

- Reminder title
- Associated holiday
- Date
- Time
- Description
- Enabled/disabled state

The final `Online Flight Check-in` test reminder was created successfully and remained associated with the correct holiday.

Holiday cards also calculate countdown information from future trip dates, such as the `31 days to go` value shown in the Final POE evidence.

Firebase Cloud Messaging complements these local reminder/countdown features by adding real-time push-notification capability.

________________________________________

# 5. User-Defined Feature Updates

The Part 1 research identified several custom travel-planning ideas.

The lecturer later clarified that the team did not need to develop two entirely new features where suitable custom functionality had already been implemented. The important requirement was to clearly identify which completed functionality would be treated as Features 4 and 5.

## 5.1 User-Defined Feature 4 - Travel Management Toolkit

**Final POE designation:** User-Defined Feature 4

The Travel Management Toolkit combines practical trip-preparation functionality into one broader feature group.

It includes:

- Packing-list creation
- Packing-item management
- Packed/unpacked state
- Packing-progress calculation
- Travel checklist functionality
- Travel-document management
- Azure Blob-backed document storage
- Room-backed offline Packing
- Reconnection synchronisation

This grouping reflects the original design objective of reducing reliance on separate travel-preparation tools.

Compared with the Part 2 prototype, Feature 4 is strengthened by offline support, cloud file storage and formal integration evidence.

## 5.2 User-Defined Feature 5 - Automated Travel Reminders and Countdowns

**Final POE designation:** User-Defined Feature 5

This feature provides time-sensitive travel support.

It includes:

- Reminder creation
- Reminder editing and deletion
- Enabled/disabled reminder state
- Reminder association with holidays
- Date and time scheduling
- Automatic trip countdowns
- Notification integration

The feature supports the original research idea of helping users remember important travel tasks and dates without manually tracking them elsewhere.

## 5.3 Additional Custom Functionality Retained

Feature 4 and Feature 5 are the two features explicitly designated for the final rubric, but they are not the only custom functionality in Roamly.

The final application also retains:

- Centralised holiday management
- Accommodation and flight information
- Day-to-day itinerary planning
- Currency conversion
- Personalised Settings
- User-owned cloud data
- Trip classification

These features remain part of the broader Roamly value proposition.

________________________________________

# 6. Architecture and Infrastructure Changes

## 6.1 Part 2 Architecture

The Part 2 prototype primarily followed:

```text
Android Application
        |
        v
Custom REST API
        |
        v
Azure SQL Database
```

Retrofit handled Android-to-API communication and Prisma ORM provided the backend data-access layer.

## 6.2 Final POE Architecture

The final architecture extends the prototype with local persistence, synchronisation, cloud file storage and NoSQL.

```text
Android User Interface
        |
        v
ViewModels / Repositories
        |
        |----> DataStore
        |       User Preferences
        |
        |----> Room
        |       Offline Data
        |
        v
Retrofit / WorkManager
        |
        v
Custom Roamly REST API
        |
        |----> Azure SQL Database
        |
        |----> Azure Blob Storage
        |
        `----> Azure Table Storage

Supporting Services:
- Google / Firebase Authentication
- Firebase Cloud Messaging
- Android Biometric Framework
```

The architecture change is important because the Final POE no longer treats every piece of data as a direct Android-to-SQL request.

Different technologies are used for different responsibilities:

- DataStore for preferences
- Room for local structured data
- WorkManager for supported background synchronisation
- Azure SQL for relational cloud data
- Azure Blob Storage for uploaded files
- Azure Table Storage for NoSQL notes

________________________________________

# 7. DevOps, Testing and Quality Improvements

## Automated Testing

The final application includes automated verification in addition to manual feature testing.

Settings-specific tests cover:

- DataStore persistence
- Language-name / language-code mapping

The Settings suite completed:

- 8 tests
- 0 failures
- 0 ignored tests
- 100% success

## GitHub Actions

The final `.github/workflows/android-tests.yml` workflow runs when code is pushed to `main` or when a pull request targets `main`.

The workflow:

1. Checks out the repository
2. Sets up Java 17
3. Sets up Gradle
4. Makes the Gradle wrapper executable
5. Runs `./gradlew testDebugUnitTest`
6. Runs `./gradlew assembleDebug`

The latest final `main` run completed successfully with both **Run Android unit tests** and **Build Android debug APK** passing.

This strengthens the final project because test/build verification is repeated in GitHub's hosted environment rather than relying only on one developer's local machine.

## Physical-Device Testing

The final application was tested on:

- Samsung Galaxy S22 (`SM-S901E/DS`)
- Android 16

Physical-device evidence covers areas including authentication, biometrics, Settings, currency, Packing, offline operation, reminders, Travel Documents, Firebase notifications and multilingual functionality.

## Final Integrated Regression Review

The final integrated review focused on whether the late-stage merges introduced a critical regression into already verified functionality.

The latest merged `main` branch passed the GitHub Actions unit-test and Android build workflow, while final physical-device evidence confirmed the biometric and multilingual integrations introduced near the end of the project.

No critical regression was identified in the documented Final POE areas.

Detailed testing is maintained in:

[View the Final POE Testing Documentation](docs/TESTING.md)

________________________________________

# 8. User Experience and Interface Updates

The final application retains the navigation structure established during the earlier design work:

- Home / Dashboard
- Itinerary
- Add Holiday
- Packing
- Documents
- Secondary access to Currency, Reminders and Settings

The Final POE adds or strengthens interface states for:

- Biometric authentication
- Persistent Settings
- Notification delivery
- Offline operation
- Travel Document upload
- Packing progress
- Reminder state
- Trip countdowns
- Multilingual interfaces

The final evidence was captured on physical Android hardware rather than relying only on emulator screenshots.

This is especially relevant to features such as biometrics, Firebase notifications and persisted language behaviour because those areas depend on real device behaviour.

________________________________________

# 9. Lecturer Feedback and Iteration

The Final POE incorporates the feedback received during the earlier project phases.

## Part 1

The Part 1 submission feedback included:

- Referencing issues
- Missing table and figure captions
- Limited discussion of specific application strengths and weaknesses
- Limited discussion of UI strengths and weaknesses
- Innovative features needed stronger explanation
- Offline mode with synchronisation was not explained
- Some planning/design visuals were unnecessarily detailed

The final documentation responds by explaining the innovative/custom features more explicitly, documenting offline mode and synchronisation in detail and using descriptive figure captions throughout the Final POE evidence.

The final architecture and testing sections also provide clearer technical explanation of how the completed application differs from the earlier design-stage proposal.

## Part 2

The Part 2 prototype achieved full marks and received the written feedback:

*"Well done!"*

Because the Part 2 prototype successfully met the requirements for that phase, the lecturer did not prescribe corrective technical changes.

The Final POE therefore focused on adding the final advanced requirements rather than reworking a failed prototype.

________________________________________

# 10. Production and Publication Readiness

The Final POE includes preparation for final Android delivery and Play Store readiness.

Final production evidence includes:

- Application version `1.0`
- Version code `1`
- Signed `app-release.apk`
- Final application icon
- Promotional Play Store screenshots
- Physical-device verification
- Final successful GitHub Actions build
- Technical README
- Formal release notes
- Detailed testing documentation
- AI Usage Report

Promotional screenshots are stored under:

```text
docs/play-store/
```

________________________________________

# 11. Change Summary - Part 2 to Final POE

The table below provides a concise comparison after the detailed release explanations above.

| Area | Part 2 Prototype | Final POE |
|---|---|---|
| Authentication | Google authentication foundation | Backend-validated Google Sign-In plus native biometric authentication |
| Settings | Basic display/language preference structure | DataStore persistence, cloud sync, language, home currency and reminders |
| Currency | Currency functionality | Saved Home Currency applied automatically |
| Offline support | Primarily cloud-connected | Room local persistence |
| Synchronisation | Direct cloud communication | WorkManager reconnection synchronisation |
| Notifications | Planned for later phase | Reminder functionality plus FCM push delivery |
| Languages | Language selection structure | English, Afrikaans, isiZulu and isiXhosa application resources |
| Travel Documents | Planned / limited | Android upload, Azure SQL metadata and Azure Blob file storage |
| NoSQL | Not part of prototype architecture | Azure Table Storage travel notes |
| Packing | CRUD packing lists/items | Offline-capable Packing with cloud synchronisation |
| Reminders | Planned | Reminder management, state and trip countdowns |
| Feature 4 | Existing travel-management functionality | Explicit Travel Management Toolkit designation |
| Feature 5 | Reminder/countdown concept | Explicit Automated Travel Reminders and Countdowns designation |
| Testing | Prototype verification | Automated, integration, cloud, offline and physical-device evidence |
| CI/CD | Unit-test workflow foundation | Automated unit tests plus Android debug APK build |
| Publication | Prototype | Signed APK and Play Store promotional assets |

________________________________________

# 12. Final Submission Check

At this stage, the technical implementation and evidence covered by these release notes have been finalised.

The remaining submission-specific item that still requires insertion is:

- Final unlisted demonstration video URL

The AI Usage Report and PDF export of the testing documentation are maintained as separate submission artefacts rather than release-note content.

________________________________________

# 13. Final Release Statement

The Final POE release represents Roamly's progression from research, planning and design in Part 1, through the successful Part 2 working prototype, into an integrated Android travel-management application.

The final release retains the core prototype functionality while extending it through native biometric security, persistent Settings, offline capability, automatic synchronisation, Azure Blob Storage, Azure Table Storage, Firebase notifications, multilingual support, expanded testing and publication-readiness evidence.

These changes support the original Roamly objective of providing travellers with one central application for holidays, itineraries, packing, documents, reminders, currency preferences and related travel information.
