#  Roamly: Travel Planning Mobile Application 
A collaborative Android travel-planning application designed to help users organise their trips, 
manage packing lists, plan itineraries and access useful travel information from one place.
________________
## Roamly System Overview
The application combines travel-planning functionality such as holiday management, itinerary planning, 
currency information, packing-list management and application settings into one centralised platform.

The purpose of Roamly is to reduce the need for users to rely on multiple applications when preparing for 
a trip. By providing related travel-planning functionality within one application, users can organise their
travel information and preparation activities from a single mobile interface.

The application is developed as an Android application using Kotlin and Android Studio, with a separate
REST API developed using Node.js, TypeScript and Express. The backend communicates with an Azure SQL 
database through Prisma ORM, while the backend is deployed using Microsoft Azure services.
________________________________________
## Intended Users
Roamly is intended for individuals who are planning, preparing for, or managing trips i.e. Travellers

Travellers are the primary users of Roamly. They can use the application to manage information associated
with their holidays and prepare for upcoming trips.

The current application allows travellers to access functionality including:  <br/>
•	Viewing and managing holidays <br/>
•	Managing itinerary information   <br/>
•	Accessing currency functionality  <br/>
•	Creating and managing packing lists  <br/>
•	Adding, editing and deleting packing-list items  <br/>
•	Managing application settings  <br/>
•	Updating their display name  <br/>

The application is designed to provide these features through a simple mobile interface so that users can 
access their travel information while preparing for or undertaking a trip.
________________________________________
## Overview of Part 2
The man objective was to develop the core functionality of the mobile application and integrate the
Android client with the project's REST API and cloud-based database.

The current implementation includes the Dashboard, Itinerary, Currency, Packing and Settings functionality.
The Packing section allows users to create, edit and delete packing lists, as well as create, edit and
delete individual items within each list.

The Settings section provides users with options such as managing their display name and selecting a
preferred language. The Settings functionality has also been structured so that additional preferences can 
be incorporated into later development.

Part 2 also focuses on connecting the Android application to the backend API. Retrofit is used to make HTTP
requests from the Android application, while the backend processes these requests and communicates with the
Azure SQL database through Prisma.

Some functionality is intentionally excluded from the current Part 2 implementation because it forms part of
the planned Part 3 scope.

The following functionality will be implemented during Part 3:  <br/>
•	Notifications and Reminders  <br/>
•	Functional language switching   <br/>
•	Biometric authentication  <br/>

(The language options have been incorporated into the application's Settings structure, but the actual 
application wide language switching functionality will be completed during Part 3.)
________________________________________
## System Architecture
Roamly follows a three-tier client-server architecture, consisting of the Android application, REST API 
and database layers. 

The Android application communicates with the REST API using Retrofit. The API processes requests and 
communicates with the Azure SQL database through Prisma ORM.

<img width="950" height="466" alt="image" src="https://github.com/user-attachments/assets/de5519fa-693a-4ada-b1d4-2dfc1cf87d79" />

________________________________________
## Architectural Design Overview 
The Android application acts as the presentation layer and contains the Dashboard, Itinerary, Currency, 
Packing and Settings features.

The Node.js and Express REST API acts as the application layer and handles requests between the Android 
application and database.

The Azure SQL database acts as the data layer and stores persistent application information, including 
users, holidays, itineraries, packing lists, packing items and settings.

Separating these responsibilities supports maintainability and allows the different layers to be developed 
independently (Gillis, 2024).
____________________
## Android Application Structure
The Android application is developed using Kotlin, Android Studio and XML-based layouts.

The project also  uses Fragments to separate the major application features rather than implementing the complete application within a single Activity.

The main feature structure includes:  <br/>
•	`ui/itinerary/` – contains the itinerary-related functionality.  <br/>
•	`ui/packing/` – contains the Packing List and Packing Item functionality.   <br/>
•	`ui/settings/` – contains the application Settings functionality.  <br/>
•	`PackingFragment` – manages the user's packing lists.  <br/>
•	`PackingItemsFragment` – manages the items belonging to a selected packing list.  <br/>
•	`PackingListAdapter` – displays packing lists within the RecyclerView.  <br/>
•	`PackingItemsAdapter` – displays individual packing items.  <br/>
•	`Dialog layouts` – provide interfaces for creating, editing and deleting packing lists and items.  <br/>
•	`API-related classes` – handle communication between the Android application and REST API.  <br/>

The separation of these responsibilities improves maintainability because functionality relating to different application features does not have to be implemented inside a single class.

___________________
## Backend and API Integration

The backend is developed using Node.js, TypeScript and Express and provides the REST API used by the
Android application.

Retrofit sends HTTP requests from the Android application to the API. The backend processes these
requests and uses Prisma ORM to communicate with Azure SQL.

### The general data flow is:
Android Application → Retrofit → REST API → Prisma ORM → Azure SQL

The backend is hosted using Azure App Service
______________________
## Version Control
We used Git and GitHub for version control and collaborative development. The team followed a branch-based workflow to 
allow members to work on different features without directly changing the main version of the application.

Each team member worked on their own feature branch when developing their assigned functionality. Changes were committed 
regularly to the relevant branch with descriptive commit messages. Once a feature was completed and reviewed, the changes were 
merged into the `main` branch.

The team's workflow can be summarised as:
1. Create/Switch to an individual feature branch for the assigned functionality.
2. Develop and test the feature locally.
3. Commit changes to the individual branch.
4. Push the branch to GitHub so that the work is backed up and available to the team.
5. Review and merge completed work into `main`.
6. Use the updated `main` branch as the final shared version of the project.

This approach allowed each team member to work independently while reducing the risk of interfering with each other's code. The `main` branch was used to maintain the final integrated version of the Roamly application.
_____________
## GitHub Actions Automation

GitHub Actions were used to automatically run Android unit tests when changes were made to the repository. 
The workflow is defined in `.github/workflows/android-tests.yml.`

The workflow runs automatically when: <br/>
  •	Code is pushed to the main branch. <br/>
  •	A pull request is opened or updated with main as the target branch.
  
The workflow uses an Ubuntu runner and performs the following steps:
  1.	Checks out the repository.
  2.	Sets up Java 17.
  3.	Sets up Gradle.
  4.	Makes the Gradle wrapper executable on the Linux runner.
  5.	Runs the Android unit test command: `./gradlew testDebugUnitTest`
     
The results are displayed in the GitHub Actions tab, providing automatic pass/fail feedback when changes are tested. This helps 
the team identify unit-test failures after code changes (Kerr, 2026).

The current GitHub Actions workflow only runs Android unit tests and does not run physical-device or instrumented tests. 
Physical-device testing was performed separately using: `.\gradlew connectedDebugAndroidTest`

This workflow provides a basic continuous integration process for Roamly by automatically checking the project's unit tests
before changes are integrated into the main branch.
________________________________________
## Architectural Design Considerations

The three-tier architecture separates the presentation, application and data responsibilities, supporting maintainability and
independent development of the different layers (Gillis, 2024).

The Android application provides the user interface, while the REST API handles application logic and database communication. 
This prevents the Android application from communicating directly with the database and keeps the application layers separated 
(Bernard, 2025).

Prisma ORM provides the database access layer between the REST API and Azure SQL. Azure App Service hosts the backend, while 
Azure SQL provides persistent data storage.

This architecture provides a structure that can be expanded as additional Roamly functionality is introduced
_______________________
## Known Limitations
### Azure App Service Cold Start
Roamly's backend is hosted using the Azure App Service Free F1 tier.
Due to the limitations of this hosting tier, the backend can become idle after a period of inactivity. 

When a request is made after the service has been idle, the application may require additional time to start again, resulting in 
increased response times. As a result, the first login attempt may occasionally fail or time out, while a subsequent attempt is
successful once the backend service has become active again (Microsoft, 2026). 

This behaviour is related to the Azure App Service hosting environment and does not indicate a failure in the Google Single Sign-On (SSO) or database functionality.

The backend has been tested end-to-end, and once the service is active, requests can be processed normally.
___________________
## Demonstration Video
A YouTube video demonstration of the application is available below:
[…….]

_______________
## References
Bernard, N. B., 2025 . Understanding the 3-tier architecture. [Online] 
Available at: https://code-garage.com/en/blog/understanding-3-tier-architecture
[Accessed 21 September 2026 ].

Gillis, A. S., 2024. What is a 3-tier application architecture?. [Online] 
Available at: https://www.techtarget.com/it-infrastructure/definition/What-is-a-3-tier-application-architecture
[Accessed 21 September 2026 ].

IBM, 2021. What is three-tier architecture?. [Online] 
Available at: https://www.ibm.com/think/topics/three-tier-architecture
[Accessed 21 September 2026 ].

Kerr, K., 2026 . GitHub for Beginners: Getting started with GitHub Actions. [Online] 
Available at: https://github.blog/developer-skills/github/github-for-beginners-getting-started-with-github-actions/
[Accessed 21 September 2026 ].

Microsoft, 2026. Configure an App Service app. [Online] 
Available at: https://learn.microsoft.com/en-us/azure/app-service/configure-common?tabs=portal
[Accessed 21 September 2026 ].



