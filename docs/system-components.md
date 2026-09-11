# Step 2: Identify the Main System Components

The next step in designing the Campus Marketplace system is to identify the major components of the application and explain the responsibility of each component.

The proposed system contains the following main components:

1. Android Mobile Application
2. Firebase Authentication
3. Cloud Firestore
4. Firebase Storage
5. Firebase Cloud Functions
6. Admin Management System

## 1. Android Mobile Application

The Android mobile application provides the user interface for buyers, sellers, and administrators. In this project, the application is developed using:

- Kotlin
- Jetpack Compose
- Android Studio
- Firebase SDK

The Android application contains screens and features for:

- Login
- Registration
- Forgot password
- Email verification flow
- Home page
- Product listing
- Recently added products
- Recommended products
- Product search
- Category filter
- Price filter
- Sorting
- Product details
- Add product
- Edit product
- My Products
- Wishlist
- Buyer-seller chat
- Notifications
- Profile
- Report listing
- Admin dashboard

## 2. Firebase Authentication

Firebase Authentication manages user identity and secure access to the application.

It is used for:

- User registration
- Login
- Logout
- Email verification
- Password recovery
- Current user session management

The system requires users to verify their university email before accessing the main marketplace features.

## 3. Cloud Firestore

Cloud Firestore is used as the main database for marketplace data.

It stores:

- User records
- Product listings
- Product approval status
- Product availability status
- Chats
- Chat messages
- Wishlists
- Notifications
- Reports
- Seller reviews and ratings

Firestore real-time listeners are used to update products, chats, notifications, reports, and admin data without requiring manual refresh.

## 4. Firebase Storage

Firebase Storage is used to store media files uploaded by users.

It stores:

- Product images
- Profile pictures

The app uploads images to Firebase Storage and saves the image download URLs in Firestore or Realtime Database.

## 5. Firebase Cloud Functions

Firebase Cloud Functions can be used for future backend automation.

Possible uses include:

- Sending push notifications with Firebase Cloud Messaging
- Running background moderation tasks
- Automatically updating marketplace statistics
- Handling admin-triggered backend operations
- Cleaning up related data when products or users are removed

For the current student-project version, most operations are handled directly from the Android app using Firebase SDKs.

## 6. Admin Management System

The Admin Management System allows administrators to monitor and control marketplace activity.

It includes:

- Admin dashboard
- User management
- Product management
- Product approval
- Report management
- User blocking and unblocking
- Listing removal
- Marketplace activity monitoring
- Admin notifications

The admin system helps keep the marketplace safe, organized, and suitable for a university environment.

## Summary

The Campus Marketplace system combines an Android mobile app with Firebase services. The Android app provides the interface, Firebase Authentication manages users, Firestore stores marketplace data, Firebase Storage stores images, Cloud Functions can support future automation, and the Admin Management System provides moderation and control.
