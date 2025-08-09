# Firebase Setup for Bubble Timer Android App

## Overview
This document outlines the steps required to set up Firebase Cloud Messaging (FCM) for push notifications in the Bubble Timer Android app.

## Prerequisites
- Google Firebase Console access
- Android Studio or similar IDE
- Bubble Timer Android project

## Setup Steps

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or select existing project
3. Enter project name: "Bubble Timer"
4. Follow the setup wizard

### 2. Add Android App to Firebase
1. In Firebase Console, click "Add app" and select Android
2. Enter package name: `io.jhoyt.bubbletimer`
3. Enter app nickname: "Bubble Timer"
4. Download the `google-services.json` file
5. Replace the placeholder `google-services.json` in the `app/` directory

### 3. Enable Cloud Messaging
1. In Firebase Console, go to "Messaging" section
2. Enable Cloud Messaging for your project
3. Note the Server Key (will be needed for AWS SNS setup)

### 4. Configure Notification Channels
The app automatically creates notification channels for:
- **Timer Invitations**: High priority notifications for timer sharing invitations

### 5. Test FCM Integration
1. Build and run the app
2. Check logs for FCM token registration
3. Verify token is sent to backend via `NotificationManager`

## Backend Integration
The FCM Server Key from step 3 should be configured in AWS SNS Platform Application as documented in:
- `bubble-timer-backend/docs/SNS_PLATFORM_APPLICATION_SETUP.md`

## Troubleshooting

### Common Issues
1. **Build fails with "google-services.json not found"**
   - Ensure `google-services.json` is in the `app/` directory
   - Verify the package name matches in the JSON file

2. **FCM token not generated**
   - Check internet connectivity
   - Verify Google Play Services is installed
   - Check Firebase project configuration

3. **Notifications not received**
   - Verify notification permissions are granted
   - Check notification channel creation
   - Ensure backend SNS configuration is correct

### Log Tags
Use these log tags for debugging:
- `BubbleTimerFCM`: Firebase Messaging Service
- `NotificationManager`: Token registration with backend
- `MainActivity`: FCM token initialization

## Security Notes
- Never commit the actual `google-services.json` to version control
- Use environment-specific Firebase projects for development/production
- The FCM Server Key should be stored securely in AWS Parameter Store
