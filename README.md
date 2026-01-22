# Purin Car 🚗

### Maintenance & Service Tracker for Cars

<div align="center">
  <img width="1920" height="1080" alt="thumbnail" src="https://github.com/user-attachments/assets/9074d786-90ec-465e-810e-c5e4db352a31" />

</div>

<br />

Purin Car is an Android Jetpack Compose application built to extend the lifespan of my car (2025 Toyota Camry) to hopefully **300,000+ miles**. 
It tracks maintenance schedules based on mileage or time intervals whichever comes first ensuring NO service is overlooked.

## Features

**Smartcar API:**
- Uses Smartcar API to connect with your car brand to automatically update PurinCar's odometer each open

**Track Critical Services**
Monitor the status of essential maintenance items, including:
- Engine Oil (5000 miles or 6 months)
- Air Filters (15000 miles or 1 year)
- Engine Coolant (30000 miles or 2 years)
- Brake Fluid (30000 miles or 2 years)
- Battery Fan (30000 miles or 3 years)
- Transmission Fluid (60000 miles or 3 years)
- Spark Plugs (100000 miles or 5 years)

**Save Files**
- **Import/Export:** Backup or transfer your service records using CSV files.

## Setup and Installation
1. Create a [SmartCar account](https://smartcar.com/)
   1. Create a new Application
   2. Copy your Client ID and Client Secret
   3. In your Smartcar App settings, add the following Redirect URI:
      - sc<YOUR_CLIENT_ID>://exchange
2. Configure the Project
   1. Clone this repository and open it in Android Studio
   2. Create a file named local.properties in the root directory of the project
   3. Add your API keys to the file:
      - SMARTCAR_CLIENT_ID=your_client_id_here
      - SMARTCAR_CLIENT_SECRET=your_client_secret_here
   5. Sync Gradle and build the app on your device

## Resources

The maintenance intervals (mileage and dates) used in this app are based on recommendations from **The Car Care Nut**, a YouTuber specializing in Toyota and Lexus maintenance.
