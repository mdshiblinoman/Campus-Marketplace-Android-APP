# Step 5: Design the Authentication Flow

The authentication flow describes how a student enters and accesses the Campus Marketplace system.

The system uses Firebase Authentication and account-status checks to control access to the marketplace.

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
Save Profile         Check Account
Data                  Status
  |                   |
  +---------+---------+
            |
            v
           Home
```

## Registration Flow

When a new student creates an account:

1. The student opens the application.
2. The student selects the registration option.
3. The student enters required information:
   - Full name
   - University email
   - Student ID
   - Department
   - Phone number
   - Password
4. The application validates the input.
5. Firebase Authentication creates the account.
6. The student profile is saved in Firebase.
7. The student can sign in with the newly created account.

## Login Flow

When an existing student logs in:

1. The student enters university email and password.
2. Firebase Authentication validates the credentials.
3. The system checks whether the user profile exists.
4. The system checks whether the account is blocked or disabled.
5. If all checks pass, the user is redirected to the Home screen.

## Forgot Password Flow

If a student forgets the password:

1. The student selects Forgot Password.
2. The student enters the university email address.
3. Firebase Authentication sends a password reset email.
4. The student opens the email and creates a new password.
5. The student returns to the app and logs in again.

## Account Access Rules

The user can access the marketplace only if:

- The email and password are valid.
- The user profile exists in Firebase.
- The account is not disabled by an administrator.

## Summary

The authentication flow protects the marketplace by validating credentials, profile existence, and account status before granting access to buying, selling, chatting, reporting, and wishlist features.
