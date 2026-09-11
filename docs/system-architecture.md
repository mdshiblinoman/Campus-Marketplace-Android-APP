# Step 3: Design the Overall System Architecture

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
                    | Search & Filter      |
                    | Product Management   |
                    | Wishlist             |
                    | Chat                 |
                    | Profile              |
                    | Admin Functions      |
                    +----------+-----------+
                               |
                         HTTPS / Internet
                               |
             +-----------------+-----------------+
             |                 |                 |
             v                 v                 v
     +---------------+  +---------------+  +---------------+
     |   Firebase    |  | Cloud         |  |   Firebase    |
     | Authentication|  | Firestore     |  |   Storage     |
     +---------------+  +---------------+  +---------------+
     | Login         |  | Users         |  | Product       |
     | Registration  |  | Products      |  | Images        |
     | Verification  |  | Chats         |  | Profile       |
     | Password      |  | Messages      |  | Pictures      |
     | Reset         |  | Wishlist      |  |               |
     | Logout        |  | Reports       |  |               |
     |               |  | Reviews       |  |               |
     |               |  | Notifications |  |               |
     +---------------+  +---------------+  +---------------+
                               |
                               v
                    +----------------------+
                    | Firebase Cloud       |
                    | Functions (Optional) |
                    +----------------------+
```

## Architecture Description

The Campus Marketplace system uses a client-server architecture. The Android application acts as the client, and Firebase provides backend services for authentication, database storage, media storage, and optional backend automation.

Users interact with the Android application based on their role:

- Buyers browse products, search listings, use the wishlist, chat with sellers, and report suspicious listings.
- Sellers manage product listings, upload images, update product status, and communicate with buyers.
- Administrators manage users, listings, reports, approvals, and marketplace safety.

## Firebase Services

Firebase Authentication handles secure user access:

- Registration
- Login
- Logout
- Email verification
- Password reset

Cloud Firestore stores the main marketplace data:

- Users
- Products
- Product approval status
- Product availability status
- Chats
- Messages
- Wishlists
- Reports
- Seller reviews
- Notifications

Firebase Storage stores uploaded files:

- Product images
- Profile pictures

Firebase Cloud Functions are optional and can be used later for backend automation:

- Push notifications
- Automated moderation
- Data cleanup
- Background marketplace tasks

## Data Flow

The basic data flow of the system is:

```text
User
  |
  v
Android Application
  |
  v
Firebase Services
  |
  v
Database / Storage
  |
  v
Firebase Response
  |
  v
Android Application
  |
  v
User
```

## Example Data Flow: Add Product

When a seller adds a product:

1. The seller enters product information in the Android application.
2. Product images are uploaded to Firebase Storage.
3. Image URLs are saved with product details in Cloud Firestore.
4. The product is stored with an approval status and availability status.
5. The admin can review and approve the product.
6. After approval, the product appears in normal browsing.

## Example Data Flow: Buyer-Seller Chat

When a buyer sends a message:

1. The buyer opens a product and starts a chat with the seller.
2. The message is saved in Cloud Firestore.
3. A Firestore real-time listener updates the chat screen.
4. A notification is created for the seller.
5. The seller can read and reply to the message.

## Summary

The architecture connects the Android application with Firebase services through the internet. Firebase Authentication manages users, Firestore stores marketplace data, Firebase Storage stores images, and optional Cloud Functions can support future backend automation.
