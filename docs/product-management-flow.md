# Step 6: Design the Product Management Flow

Product management is one of the core functions of the Campus Marketplace system. It allows sellers to add, edit, delete, and update the status of their product listings.

## Product Add Flow

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
Store Product Information in Cloud Firestore
  |
  v
Pending Admin Review
  |
  v
Admin Approval
  |
  v
Product Published
  |
  v
Buyer Can View Product
```

## Product Information

Each product listing contains the following information:

- Product ID
- Seller ID
- Product title
- Description
- Category
- Price
- Condition
- Product images
- Approval status
- Availability status
- Upload date
- Updated date
- Location
- Contact preference

## Product Status

The system uses two types of product status.

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

## Add Product Process

When a seller adds a product:

1. The seller opens the My Products screen.
2. The seller selects Add Product.
3. The seller enters product details such as title, description, category, condition, price, location, and contact preference.
4. The seller uploads one or more product images.
5. The application validates the product information.
6. Product images are uploaded to Firebase Storage.
7. Image download URLs are saved with the product record.
8. Product information is stored in Cloud Firestore.
9. The product is marked as Pending for admin review.
10. After admin approval, the product becomes published.

## Edit Product Process

When a seller edits a product:

1. The seller opens My Products.
2. The seller selects the edit option for a listing.
3. The seller updates product information or images.
4. The application validates the updated data.
5. New images are uploaded to Firebase Storage if selected.
6. Updated product data is saved in Cloud Firestore.
7. The product is sent back to Pending review so the admin can approve the changes.

## Delete Product Process

When a seller deletes a product:

1. The seller selects the delete option.
2. The app shows a confirmation dialog.
3. If the seller confirms, the listing is removed from Firestore.
4. The product no longer appears in browsing or in the seller's product list.

## Mark Product as Sold

When a seller sells a product:

1. The seller opens My Products.
2. The seller selects Mark as Sold.
3. The product availability status changes to Sold.
4. The product no longer appears as an available item in normal browsing.
5. Wishlist users may receive a product status notification.

## Reserve Product

When a seller wants to hold a product for a buyer:

1. The seller selects Mark as Reserved.
2. The product availability status changes to Reserved.
3. The product stops appearing in normal browsing.
4. The seller can later change it back to Available or mark it as Sold.

## Admin Product Review

The admin can:

- View all listings.
- Approve pending products.
- Reject inappropriate or incomplete products.
- Remove fake or suspicious listings.
- Block suspicious sellers.

## Summary

The product management flow ensures that sellers can manage listings easily while administrators maintain marketplace quality. Firebase Storage handles product images, Cloud Firestore stores product records, and approval plus availability status controls whether buyers can view and contact sellers about a product.
