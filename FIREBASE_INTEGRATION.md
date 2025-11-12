# Firebase Integration Guide

This document explains how the Phambili Ma Africa mobile app integrates with Firebase to sync data with the website in real-time.

## Overview

The mobile app connects to the same Firebase database as the website, allowing for real-time synchronization of data between both platforms.

### Real-Time Sync Features:

- ✅ Admin adds service on website → **App shows it instantly**
- ✅ Admin deletes service on website → **App removes it instantly**
- ✅ User books on app → **Admin dashboard shows it instantly**
- ✅ Admin updates booking status on website → **App reflects it instantly**

## Firebase Project Details

- **Project ID**: `phambili-ma-africa-9c4ca` 
- **Project Name**: Phambili Ma Africa
- **Database**: Cloud Firestore
- **Storage**: Firebase Storage
- **Auth**: Firebase Authentication

## Implementation Details

### 1. Firebase Configuration

The app is already configured with Firebase:

- `google-services.json` is located in the app module directory
- Firebase dependencies are added to the build.gradle files
- Firebase is initialized in the `PhambiliApplication` class

### 2. Data Models

The app uses the following data models that match the Firestore collections:

- `Service`: Cleaning services offered
- `Booking`: Customer bookings
- `Customer`: User profiles
- `Product`: Cleaning products (optional)

### 3. Repository Pattern

Firebase interactions are implemented using the Repository pattern:

- `ServicesRepository`: Handles services data
- `BookingRepository`: Handles booking operations
- `AuthRepository`: Handles authentication
- `CustomerRepository`: Handles customer data
- `ProductRepository`: Handles product data

### 4. ViewModels

ViewModels use Kotlin Coroutines and Flow to handle asynchronous operations:

- `ServicesViewModel`: Manages services data
- `BookingViewModel`: Manages booking operations
- `AuthViewModel`: Manages authentication

### 5. Real-Time Updates

Real-time updates are implemented using Firestore listeners:

```kotlin
fun getAvailableServices(): Flow<List<Service>> = callbackFlow {
    val listener = servicesCollection
        .whereEqualTo("Is_Available", true)
        .addSnapshotListener { snapshot, error ->
            // Process updates in real-time
            val services = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Service::class.java)?.copy(ID = doc.id)
            } ?: emptyList()
            
            trySend(services)
        }
    
    awaitClose { listener.remove() }
}
```

### 6. Offline Persistence

Offline persistence is enabled for the app:

```kotlin
val settings = FirebaseFirestoreSettings.Builder()
    .setPersistenceEnabled(true)
    .build()

firestore.firestoreSettings = settings
```

### 7. Push Notifications

Push notifications are handled by `MyFirebaseMessagingService`:

- Booking status updates
- New service notifications
- General notifications

## Testing Real-Time Sync

1. **Test Service Sync:**
   - Open admin dashboard on website
   - Add a new service
   - Open your app → Service should appear instantly

2. **Test Booking Sync:**
   - Submit booking from app
   - Check admin dashboard on website → Booking should appear instantly

3. **Test Status Updates:**
   - Admin updates booking status on website
   - Check app booking history → Status should update instantly

## Troubleshooting

If you encounter any issues:

1. Check Firebase Console for errors
2. Check Android Logcat for errors (filter: "FirebaseFirestore")
3. Verify `google-services.json` is in correct location
4. Ensure Firebase dependencies are synced

## Security Rules

The Firebase Security Rules are already configured to work for both web and app:

- Anyone can read services
- Only admins can create/update/delete services
- Users can create bookings
- Users can read their own bookings
- Only admins can update booking status
- Users can read/update their own customer profile

## Performance Tips

1. Limit query results when possible:
   ```kotlin
   bookingsCollection
       .whereEqualTo("Customer_ID", userId)
       .orderBy("Date", Query.Direction.DESCENDING)
       .limit(20)  // Only get last 20 bookings
   ```

2. Use indexes for complex queries (Firebase will prompt you to create them when needed)

3. Enable offline persistence (already implemented)
