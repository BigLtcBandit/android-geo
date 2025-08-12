# KickGEO App

This Android application tracks the user's location at regular intervals and sends it to a server. It is designed to be battery-efficient and reliable, running as a background service.

## Key Features

- **Background Location Tracking**: Tracks location every 15 minutes using `WorkManager` for reliability and battery efficiency.
- **Foreground Service**: Runs as a foreground service to ensure continuous operation, even when the app is closed.
- **Unique User ID**: Generates a unique ID for each user to distinguish location data on the server.
- **REST API Integration**: Sends location data to a server via a REST API using Retrofit.
- **Modern Android Practices**: Built with modern Android libraries and practices, including:
  - `WorkManager` for background tasks.
  - `Retrofit` for type-safe HTTP requests.
  - View Binding (implicitly used with `findViewById`).
- **Permission Handling**: Properly requests necessary location permissions from the user.
- **Reboot Persistence**: Automatically resumes tracking after the device is rebooted.

## Project Structure

The project is organized into the following key files:

- **`MainActivity.java`**: The main screen of the app, responsible for handling user interaction and permissions.
- **`LocationWorker.java`**: A `WorkManager` worker that fetches the location and sends it to the server.
- **`LocationRepository.java`**: Manages the logic for sending location data to the server.
- **`ApiService.java`**: Defines the Retrofit API endpoints.
- **`UserPreferences.java`**: Manages user-specific data, like the user ID, using `SharedPreferences`.
- **`build.gradle`**: Contains the project's dependencies and build configurations.
- **`AndroidManifest.xml`**: Defines the app's components and required permissions.

## Setup and Compilation Instructions

To build and run this project, you will need [Android Studio](https://developer.android.com/studio).

### 1. Clone the Repository

Clone this repository to your local machine:

```bash
git clone <repository-url>
```

### 2. Open in Android Studio

1.  Open Android Studio.
2.  Click on "Open" and navigate to the cloned repository's directory.
3.  Select the `LocationTracker` folder and click "OK".
4.  Android Studio will automatically sync the project and download the required dependencies.

### 3. Configure the Server URL

Before running the app, you need to specify the URL of your server where the location data will be sent.

1.  Open the file `app/src/main/java/com/locationtracker/ApiService.java`.
2.  Find the `BASE_URL` constant and replace the placeholder with your actual server URL:

    ```java
    class ApiClient {
        private static final String BASE_URL = "https://kickis.fun/"; // Replace with your server URL
        // ...
    }
    ```

### 4. Build and Run the App

1.  Connect an Android device to your computer or start an Android emulator.
2.  Click the "Run" button (green play icon) in Android Studio.
3.  The app will be installed and launched on your device/emulator.

### 5. Network Security Configuration

For development purposes, the app is configured to allow cleartext (HTTP) traffic to your specified server domain. This is done in `app/src/main/res/xml/network_security_config.xml`.

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">kickis.fun</domain> <!-- Make sure this matches your server URL -->
    </domain-config>
</network-security-config>
```

**Note**: For production, it is highly recommended to use HTTPS for all network communication.

## Server-Side Implementation

A simple Node.js server using Express and PostgreSQL is provided as an example to receive and store the location data.

### Prerequisites

- [Node.js](https://nodejs.org/)
- [PostgreSQL](https://www.postgresql.org/)

### Server Code (`server.js`)

```javascript
const express = require('express');
const { Pool } = require('pg');
const app = express();

app.use(express.json());

// PostgreSQL connection
const pool = new Pool({
    user: 'your_username',
    host: 'localhost',
    database: 'kickgeo',
    password: 'your_password',
    port: 5432,
});

// Create table if not exists
const createTable = async () => {
    const query = `
        CREATE TABLE IF NOT EXISTS user_locations (
            id SERIAL PRIMARY KEY,
            user_id VARCHAR(255) NOT NULL,
            latitude DOUBLE PRECISION NOT NULL,
            longitude DOUBLE PRECISION NOT NULL,
            accuracy REAL,
            timestamp BIGINT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    `;

    try {
        await pool.query(query);
        console.log('Table created or already exists');
    } catch (err) {
        console.error('Error creating table:', err);
    }
};

// API endpoint to receive location data
app.post('/api/location', async (req, res) => {
    const { userId, latitude, longitude, timestamp, accuracy } = req.body;

    try {
        const query = `
            INSERT INTO user_locations (user_id, latitude, longitude, accuracy, timestamp)
            VALUES ($1, $2, $3, $4, $5)
        `;

        await pool.query(query, [userId, latitude, longitude, accuracy, timestamp]);

        res.json({ success: true, message: 'Location saved successfully' });
    } catch (err) {
        console.error('Error saving location:', err);
        res.status(500).json({ success: false, message: 'Failed to save location' });
    }
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
    createTable();
});
```

### Running the Server

1.  Set up your PostgreSQL database and update the connection details in the `Pool` constructor.
2.  Install the required Node.js packages:
    ```bash
    npm install express pg
    ```
3.  Run the server:
    ```bash
    node server.js
    ```

The server will then be ready to accept location data from the Android app.
