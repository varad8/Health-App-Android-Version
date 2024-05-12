# Health App

The Health App is your comprehensive solution for efficient health record management. This app is designed to simplify the process of handling health records by securely managing them in the cloud. Here are the key features and requirements of the Health App:

## Admin Credential Setup

To set up admin credentials for your Firestore database, follow these steps:

1. Create a collection named `adminUser` in your Firestore database.

2. Add a document to the `adminUser` collection with the necessary admin credentials.

   Example document structure:

   ![Firestore Health App](https://github.com/varad8/Health-App-Android-Version/blob/main/firestore%20health%20app.png)


## Requirements

- Android Studio (Giraffe version or Latest).
- Firebase Account

## Features of the App

1. **Online Appointment Booking of Nearest Doctor:**

   - Users can conveniently book appointments with nearby doctors through the app.

2. **Doctor Search:**

   - Users can search for doctors based on specific needs, such as searching for an eye doctor. The app provides a list of doctors specializing in the specified area.

3. **Notification:**

   - Users receive notifications for appointment reminders, prescription updates, and other relevant information.

4. **Prescription Management:**

   - Users can save and view prescriptions within the app for easy access and reference.

5. **User Authentication:**
   - Register and login functionalities for both end-users (patients) and doctors.

## Features According to App Users

### Admin

- **View Recent Doctor Requests:**

  - Admin can view recent requests from doctors to join the platform.

- **Doctor Approval:**
  - Admin can approve or disapprove doctor requests.

### Doctor

- **Appointment Management:**

  - View upcoming or today's appointments.

- **Prescription Issuing:**

  - Issue prescriptions for patients during appointments.

- **Notification Sending:**

  - Send notifications to specific users.

- **History Viewing:**

  - View prescription history, appointment history, and notification history.

- **Chat Functionality:**
  - Respond to patient messages through the in-app chat.

### User (Patient)

- **Account Creation:**

  - Users can create an account by registering within the app.

- **Doctor Discovery:**

  - View nearby doctors within a 10km radius.

- **Appointment Booking:**

  - View doctor details and book appointments.

- **Prescription Viewing:**

  - View generated prescriptions.

- **Notification and Appointment History:**

  - View notification and appointment history for reference.

- **Chat with Doctors:**

  - Users can send chat messages to specific doctors.

- **Location Tracking:**
  - Users can track the location of doctors using Google Maps integration.

## Technology Stack

- **Frontend:** Android (Java/Kotlin)
- **Backend:** Firebase (Authentication, Realtime Database, Cloud Messaging)

## Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/your-username/health-app.git
   cd health-app
   ```

2. Open the project in Android Studio and build/run the app.

## Usage

1. Register or log in to access the app's features.
2. Book appointments, view prescriptions, and manage health records seamlessly.

## Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

## License

This project is licensed under the [MIT License](LICENSE).
