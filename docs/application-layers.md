# Step 4: Design the Application Layers

The Campus Marketplace system can be logically divided into three main layers:

1. Presentation Layer
2. Application Logic Layer
3. Data Layer

This separation makes the system easier to understand, maintain, and extend.

```text
+-----------------------------+
|      Presentation Layer     |
|        Android UI           |
|   Kotlin + Jetpack Compose  |
+--------------+--------------+
               |
               v
+-----------------------------+
|     Application Logic       |
| Authentication              |
| Product Management          |
| Search / Filter             |
| Wishlist / Chat / Reports   |
| Admin Moderation            |
+--------------+--------------+
               |
               v
+-----------------------------+
|         Data Layer          |
| Firebase Authentication     |
| Cloud Firestore             |
| Firebase Realtime Database  |
| Firebase Storage            |
+-----------------------------+
```

## 1. Presentation Layer

The Presentation Layer contains the Android user interface. It is responsible for displaying information to users and collecting user input.

In this project, the Presentation Layer is built using:

- Kotlin
- Jetpack Compose
- Material Design components

Examples of screens in this layer:

- Login Screen
- Registration Screen
- Forgot Password Screen
- Home Screen
- Product Listing Screen
- Product Details Screen
- Add Product Screen
- Edit Product Screen
- My Products Screen
- Wishlist Screen
- Chat Screen
- Notifications Screen
- Profile Screen
- Admin Dashboard

The Presentation Layer communicates with ViewModels to display data and trigger user actions.

## 2. Application Logic Layer

The Application Logic Layer contains the main business logic of the application. It connects the user interface with Firebase services and controls how user actions are processed.

In this project, the logic layer is mainly handled by ViewModel classes.

Examples:

- `AuthViewModel`
- `ProductViewModel`
- `ChatViewModel`
- `NotificationViewModel`
- `ProfileViewModel`
- `AdminViewModel`

This layer handles:

- Authentication validation
- Registration and login logic
- Email verification checks
- Password reset requests
- Product creation
- Product update
- Product deletion
- Product approval status
- Product availability status
- Search and filtering logic
- Wishlist management
- Buyer-seller chat operations
- Notification creation and reading
- Report submission
- Seller rating and review logic
- Admin moderation actions
- User blocking and unblocking

## 3. Data Layer

The Data Layer manages communication with Firebase services and stores application data.

It includes:

- Firebase Authentication
- Cloud Firestore
- Firebase Realtime Database
- Firebase Storage

Firebase Authentication stores and manages user login credentials.

Cloud Firestore stores:

- Users
- Products
- Chats
- Messages
- Wishlists
- Notifications
- Reports
- Seller reviews

Firebase Realtime Database stores user profile data for fast user lookup and compatibility with profile/chat features.

Firebase Storage stores:

- Product images
- Profile pictures

## Layer Communication

The layers communicate in one direction:

```text
User
  |
  v
Presentation Layer
  |
  v
Application Logic Layer
  |
  v
Data Layer
```

When Firebase returns data, the response flows back through the ViewModel and updates the UI:

```text
Data Layer
  |
  v
Application Logic Layer
  |
  v
Presentation Layer
  |
  v
User
```

## Example: Product Search

1. The user enters a search keyword on the Home screen.
2. The Presentation Layer sends the input to the filtering logic.
3. The Application Logic Layer filters products by title, category, condition, location, seller, and other fields.
4. The filtered list is shown on the Home screen.

## Example: Add Product

1. The seller enters product details and selects images.
2. The Presentation Layer sends the data to `ProductViewModel`.
3. The Application Logic Layer validates and prepares the product data.
4. Firebase Storage stores the images.
5. Cloud Firestore stores the product information and image URLs.
6. The UI updates after the product is saved.

## Summary

The three-layer structure helps separate the user interface, business rules, and Firebase data operations. This makes the Campus Marketplace easier to develop, debug, and expand with future features such as AI recommendations or Firebase Cloud Functions.
