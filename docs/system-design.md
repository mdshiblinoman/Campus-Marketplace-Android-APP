# 8. System Design

The system design describes the structure, components, data flow, user interactions, and security mechanisms of the Campus Marketplace application. The system is designed as a mobile client-cloud application where students use an Android application and Firebase provides authentication, database, storage, and backend service support.

## 8.1 System Users

The Campus Marketplace has three main types of users: Buyer, Seller, and Administrator.

### Buyer

A buyer is a student who uses the marketplace to find products posted by other students.

A buyer can:

- Register and log in using a university email.
- Verify the account email address.
- Browse available products.
- Search for products by keyword.
- Filter products by category and price.
- Sort product listings.
- View product details.
- Add products to the wishlist.
- Contact sellers through chat.
- Receive message and product notifications.
- Report suspicious or inappropriate listings.
- Block sellers when needed.

### Seller

A seller is a student who can post products for sale. A seller can perform all buyer functions and can also manage product listings.

A seller can:

- Add products.
- Upload product images.
- Edit product information.
- Delete their own listings.
- Mark products as Available, Reserved, or Sold.
- Communicate with potential buyers through chat.
- Receive notifications about messages, product status, and admin actions.
- Receive seller ratings and reviews after completed transactions.

### Administrator

An administrator is responsible for keeping the marketplace safe, organized, and suitable for a university environment.

An administrator can:

- Access the admin dashboard.
- Manage users.
- Block or unblock suspicious users.
- Monitor product listings.
- Review newly submitted products.
- Approve or reject products.
- Review reports.
- Remove inappropriate, fake, or suspicious listings.
- Take action against reported sellers.
- Send important admin notifications.
- Monitor marketplace activity.

## 8.2 Main System Components

The proposed system contains the following main components:

1. Android Mobile Application
2. Firebase Authentication
3. Cloud Firestore
4. Firebase Realtime Database
5. Firebase Storage
6. Firebase Cloud Functions
7. Admin Management System

### Android Mobile Application

The Android application provides the user interface for buyers, sellers, and administrators. In this project, the application is developed using:

- Kotlin
- Jetpack Compose
- Android Studio
- Firebase SDK

The application contains screens and features for:

- Login
- Registration
- Forgot password
- Email verification
- Home
- Product listing
- Product details
- Add product
- Edit product
- My Products
- Wishlist
- Buyer-seller chat
- Notifications
- Profile
- Reports
- Admin dashboard

### Firebase Authentication

Firebase Authentication manages:

- User registration
- Login
- Logout
- Email verification
- Password recovery
- Current session management

The system requires a verified university email before a user can access the main marketplace features.

### Cloud Firestore

Cloud Firestore stores the main marketplace data:

- Users
- Products
- Chats
- Messages
- Wishlists
- Reports
- Seller reviews
- Notifications
- User blocks

Firestore real-time listeners are used for products, chats, notifications, reports, admin data, and blocking status.

### Firebase Realtime Database

Firebase Realtime Database stores user profile data used for fast user lookup and login access checks. It helps the app quickly confirm whether a user profile exists and whether the account has been disabled by an administrator.

### Firebase Storage

Firebase Storage stores uploaded media files:

- Product images
- Profile pictures

### Firebase Cloud Functions

Firebase Cloud Functions can be used for future backend operations such as:

- Push notifications
- Automated moderation
- Scheduled cleanup
- Background marketplace tasks
- Server-side validation

In the current student-project version, most operations are handled directly through the Android app using Firebase SDKs.

### Admin Management System

The Admin Management System allows administrators to control marketplace quality and safety. It includes:

- Admin dashboard
- User management
- Product management
- Product approval
- Report management
- Listing removal
- User blocking and unblocking
- Admin notifications

## 8.3 Overall System Architecture

The overall system architecture shows how users interact with the Android application and how the application communicates with Firebase services.

```text
                    +----------------------+
                    |        Users         |
                    | Buyer / Seller/Admin |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    | Android Application  |
                    | Kotlin + Compose     |
                    +----------------------+
                    | Login / Registration |
                    | Marketplace          |
                    | Search and Filter    |
                    | Product Management   |
                    | Wishlist             |
                    | Chat                 |
                    | Notifications        |
                    | Profile              |
                    | Admin Functions      |
                    +----------+-----------+
                               |
                         HTTPS / Internet
                               |
        +----------------------+----------------------+
        |                      |                      |
        v                      v                      v
+---------------+      +---------------+      +---------------+
| Firebase      |      | Cloud         |      | Firebase      |
| Authentication|      | Firestore     |      | Storage       |
+---------------+      +---------------+      +---------------+
| Login         |      | Users         |      | Product       |
| Registration  |      | Products      |      | Images        |
| Verification  |      | Chats         |      | Profile       |
| Password Reset|      | Messages      |      | Pictures      |
| Logout        |      | Wishlist      |      |               |
|               |      | Reports       |      |               |
|               |      | Reviews       |      |               |
|               |      | Notifications |      |               |
|               |      | User Blocks   |      |               |
+---------------+      +---------------+      +---------------+
                               |
                               v
                    +----------------------+
                    | Firebase Realtime    |
                    | Database             |
                    +----------------------+
                    | User Profile Lookup  |
                    | Disabled User Check  |
                    +----------------------+
```

## 8.4 Application Layers

The system can be logically divided into three layers:

1. Presentation Layer
2. Application Logic Layer
3. Data Layer

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
| Notifications               |
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

### Presentation Layer

The Presentation Layer contains the Android user interface. It displays information to users and collects user input.

Examples:

- Login Screen
- Registration Screen
- Forgot Password Screen
- Home Screen
- Product Details Screen
- Add Product Screen
- My Products Screen
- Wishlist Screen
- Chat Screen
- Notifications Screen
- Profile Screen
- Admin Dashboard

### Application Logic Layer

The Application Logic Layer handles business rules and connects the user interface with Firebase services. In this project, this layer is mainly handled by ViewModel classes:

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
- Product creation, update, deletion, and status changes
- Product approval state
- Search and filtering logic
- Wishlist management
- Chat operations
- Report submission
- Seller rating and review logic
- User blocking
- Notification creation and reading
- Admin moderation actions

### Data Layer

The Data Layer manages communication with Firebase services.

It includes:

- Firebase Authentication
- Cloud Firestore
- Firebase Realtime Database
- Firebase Storage

## 8.5 Authentication Flow

The authentication flow describes how a student enters and accesses the system.

```text
Start
  |
  v
Open Application
  |
  v
Login / Registration
  |
  v
Register?
  |
  +-------------------+
  |                   |
 Yes                  No
  |                   |
  v                   v
Create Account       Login
  |                   |
  v                   v
Save Student         Validate
Profile Data         Credentials
  |                   |
  v                   v
Send Email           Check Profile,
Verification         Disabled Status,
  |                   and Verification
  v                   |
User Verifies        v
Email               Valid?
  |                   |
  +---------+---------+
            |
            v
           Home
```

During registration, the user provides full name, university email, student ID, department, phone number, and password. The app validates the inputs, creates a Firebase Authentication account, stores profile data, and sends a verification email.

During login, the app checks that:

- Credentials are valid.
- The Firebase user profile exists.
- The account is not disabled.
- The university email is verified.

The Forgot Password feature uses Firebase Authentication to send a password reset email.

## 8.6 Product Management Flow

Product management allows sellers to add, edit, delete, reserve, and mark products as sold.

```text
Seller
  |
  v
Add Product
  |
  v
Enter Product Information
  |
  v
Upload Images
  |
  v
Validate Information
  |
  v
Upload Images to Firebase Storage
  |
  v
Store Product in Cloud Firestore
  |
  v
Pending Admin Review
  |
  v
Admin Approves
  |
  v
Product Published
  |
  v
Buyer Can View Product
```

Product information includes:

- Product ID
- Seller ID
- Product title
- Description
- Category
- Price
- Condition
- Images
- Location
- Contact preference
- Approval status
- Availability status
- Upload date
- Updated date

The system uses two product status types.

Approval status:

- Pending
- Approved
- Rejected

Availability status:

- Available
- Reserved
- Sold
- Removed

A product appears in normal browsing only when it is approved and available.

## 8.7 Product Browsing and Search Flow

Users can browse and discover products from the Home screen.

```text
User
  |
  v
Home Screen
  |
  v
Browse Products
  |
  v
Search / Filter / Sort
  |
  v
Apply Product Rules
  |
  v
Display Matching Products
  |
  v
Product Details
```

The system provides:

- Keyword search
- Category filtering
- Minimum and maximum price filtering
- Sorting by newest, oldest, lowest price, and highest price
- Recently added products
- Recommended products based on viewed and wishlisted categories

Products from blocked users, or products owned by users who blocked the current user, are hidden from normal browsing.

## 8.8 Wishlist System

The wishlist allows users to save products they are interested in.

```text
User
  |
  v
Product Details
  |
  v
Add to Wishlist
  |
  v
Check Existing Wishlist
  |
  v
Save Product ID
  |
  v
Cloud Firestore
  |
  v
Wishlist Screen
```

When a user removes a product from the wishlist, the wishlist document is deleted from Firestore. Wishlist users may receive notifications when a saved product is updated or marked as sold.

## 8.9 Chat System

The chat system allows buyers and sellers to communicate about a product.

```text
Buyer
  |
  v
Product Details
  |
  v
Chat with Seller
  |
  v
Create / Open Chat
  |
  v
Send Message
  |
  v
Cloud Firestore
  |
  v
Real-Time Update
  |
  v
Seller Receives Message
```

Each message contains:

- Message ID
- Sender ID
- Receiver ID
- Message text
- Timestamp
- Read status
- Message status

Firestore real-time listeners update the conversation without requiring manual refresh. The app also creates message notifications for recipients.

If either user has blocked the other user, the chat composer is disabled and new messages cannot be sent.

## 8.10 Reporting System

Users can report suspicious or inappropriate listings.

```text
User
  |
  v
Product Details
  |
  v
Report Product
  |
  v
Select Reason
  |
  v
Submit Report
  |
  v
Cloud Firestore
  |
  v
Admin Dashboard
  |
  v
Admin Reviews Report
  |
  v
Take Action
```

Possible report reasons include:

- Fake product
- Scam
- Incorrect information
- Inappropriate content
- Duplicate listing
- Suspicious seller
- Other

Administrators can review, resolve, or dismiss reports. They can also remove the reported listing or block the reported seller.

## 8.11 Admin System

The administrator uses a dashboard to monitor and manage the marketplace.

```text
                 Admin
                   |
                   v
            Admin Dashboard
                   |
       +-----------+-----------+
       |           |           |
       v           v           v
     Users      Listings     Reports
       |           |           |
       v           v           v
    Manage      Review       Review
    Access      Products     Issues
       |           |           |
       +-----------+-----------+
                   |
                   v
              Take Action
```

The admin dashboard includes:

- Activity metrics
- Registered user counts
- Active and blocked user counts
- Active, sold, and removed listing counts
- Pending approval counts
- Pending report counts

The administrator can:

- View users.
- Block or unblock users.
- View listings.
- Approve or reject pending products.
- Remove products.
- View reports.
- Mark reports as reviewed or resolved.
- Remove reported listings.
- Block reported sellers.
- Send notifications to all users.

## 8.12 Database Design

The database uses Cloud Firestore for marketplace data and Firebase Realtime Database for user profile lookup and account access checks.

### Users

```text
users
  |
  +-- userId
      |
      +-- fullName
      +-- email
      +-- studentId
      +-- department
      +-- mobile
      +-- role
      +-- disabled
      +-- registrationDate
      +-- lastLogin
      +-- profileImageUrl
```

### Products

```text
products
  |
  +-- productId
      |
      +-- id
      +-- ownerId
      +-- name
      +-- description
      +-- category
      +-- price
      +-- condition
      +-- imageUrl
      +-- imageUrls
      +-- location
      +-- contactPreference
      +-- approvalStatus
      +-- availabilityStatus
      +-- isSold
      +-- createdAt
      +-- updatedAt
      +-- reviewedAt
      +-- reviewedBy
      +-- publishedAt
      +-- rejectionReason
```

### Chats and Messages

```text
chats
  |
  +-- chatId
      |
      +-- participantIds
      +-- buyerId
      +-- sellerId
      +-- productId
      +-- productTitle
      +-- lastMessage
      +-- lastMessageTimestamp
      +-- lastSenderId
      +-- unreadCount
      +-- createdAt
      +-- updatedAt
      |
      +-- messages
          |
          +-- messageId
              |
              +-- senderId
              +-- receiverId
              +-- content
              +-- timestamp
              +-- isRead
              +-- status
              +-- readAt
```

### Wishlist

```text
wishlist
  |
  +-- wishlistId
      |
      +-- userId
      +-- productId
      +-- createdAt
```

### Reports

```text
reports
  |
  +-- reportId
      |
      +-- id
      +-- productId
      +-- productTitle
      +-- sellerId
      +-- reporterId
      +-- reason
      +-- timestamp
      +-- status
      +-- adminAction
      +-- reviewedAt
      +-- reviewedBy
```

### Seller Reviews

```text
seller_reviews
  |
  +-- reviewId
      |
      +-- id
      +-- sellerId
      +-- reviewerId
      +-- productId
      +-- productTitle
      +-- rating
      +-- comment
      +-- createdAt
      +-- updatedAt
```

### Notifications

```text
notifications
  |
  +-- notificationId
      |
      +-- id
      +-- recipientId
      +-- title
      +-- message
      +-- type
      +-- relatedId
      +-- relatedTitle
      +-- createdBy
      +-- createdAt
      +-- read
```

### User Blocks

```text
user_blocks
  |
  +-- blockerId_blockedUserId
      |
      +-- id
      +-- blockerId
      +-- blockedUserId
      +-- createdAt
```

## 8.13 Entity Relationships

The major relationships between entities are:

```text
USER
 |
 +-------------< PRODUCT
 |                  |
 |                  +-------------< REPORT
 |                  |
 |                  +-------------< WISHLIST
 |                  |
 |                  +-------------< SELLER_REVIEW
 |
 +-------------< CHAT >------------- USER
 |                  |
 |                  +-------------< MESSAGE
 |
 +-------------< NOTIFICATION
 |
 +-------------< USER_BLOCK >-------- USER
```

Relationship explanation:

- A user can create many products.
- A user can save many products in the wishlist.
- A product can receive many reports.
- A buyer and seller can communicate through chats.
- A chat can contain many messages.
- A seller can receive many reviews.
- A user can receive many notifications.
- A user can block many other users.

## 8.14 Security and Authorization

Security is important because the system is intended for verified university users.

The system uses:

- Firebase Authentication
- University email verification
- Account disabled checks
- Role-based authorization
- Input validation
- Ownership checks
- Firestore Security Rules
- Firebase Storage Rules
- User blocking

Important security rules:

- A user can access the marketplace only after email verification.
- A disabled user cannot log in to the marketplace.
- A seller can edit or delete only their own products.
- A buyer cannot report their own listing.
- A user cannot review themself.
- A user cannot message another user if either user has blocked the other.
- Only administrators can approve, reject, or remove products.
- Only administrators can block or unblock accounts at the admin level.
- Product images and profile pictures should be uploaded only by authenticated users.

Example ownership rule:

```text
Seller A
   |
   v
Own Product
   |
   v
Edit / Delete: Allowed

Seller A
   |
   v
Seller B's Product
   |
   v
Edit / Delete: Not Allowed
```

## 8.15 Complete System Flow

The complete system flow can be represented as follows:

```text
                         USER
                           |
                           v
                   Login / Registration
                           |
                           v
                   Email Verification
                           |
                           v
                         HOME
                           |
        +------------------+------------------+
        |                  |                  |
        v                  v                  v
     Browse            Add Product         Profile
        |                  |                  |
        v                  v                  v
     Search            Upload Images      Edit Profile
        |                  |                  |
        v                  v                  v
     Filter           Firebase Storage    Profile Picture
        |                  |
        v                  v
    Product       Cloud Firestore
    Details              |
        |                v
   +----+-----------+ Pending Review
   |    |           |     |
   v    v           v     v
 Chat Wishlist    Report Admin Approval
   |                  |     |
   v                  v     v
Seller           Admin Dashboard
   |
   v
Transaction
   |
   v
Mark as Sold
   |
   v
Seller Review
```

## 8.16 System Design Diagrams

The final project report should include the following diagrams:

### Diagram 1: System Architecture Diagram

Shows:

```text
Android App
  |
  +-- Firebase Authentication
  +-- Cloud Firestore
  +-- Firebase Realtime Database
  +-- Firebase Storage
  +-- Firebase Cloud Functions
```

### Diagram 2: Use Case Diagram

Shows the interaction between:

```text
Buyer
Seller
Administrator
```

and their respective functions.

### Diagram 3: ER Diagram

Shows relationships between:

```text
User
Product
Chat
Message
Wishlist
Report
Review
Notification
User Block
```

### Diagram 4: Data Flow Diagram

Shows how data moves between:

```text
User -> Application -> Firebase -> Database / Storage -> Application -> User
```

### Diagram 5: Activity Diagram

Can show processes such as:

```text
Login
Add Product
Browse Product
Chat
Report Product
Admin Approval
Mark as Sold
```

## 8.17 Final System Design Structure

The final System Design section of the project report contains:

- 8.1 System Users
- 8.2 Main System Components
- 8.3 Overall System Architecture
- 8.4 Application Layers
- 8.5 Authentication Flow
- 8.6 Product Management Flow
- 8.7 Product Browsing and Search Flow
- 8.8 Wishlist System
- 8.9 Chat System
- 8.10 Reporting System
- 8.11 Admin System
- 8.12 Database Design
- 8.13 Entity Relationships
- 8.14 Security and Authorization
- 8.15 Complete System Flow
- 8.16 System Design Diagrams

This system design provides a clear blueprint for the Campus Marketplace application using Kotlin, Jetpack Compose, and Firebase.
