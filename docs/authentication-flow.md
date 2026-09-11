# Step 5: Design the Authentication Flow

The authentication flow describes how a student enters and accesses the Campus Marketplace system.

The system uses Firebase Authentication and university email verification to increase trust and prevent unauthorized users from joining the marketplace.

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
Send Email           Check Account
Verification         Status
  |                   |
  v                   v
User Verifies        Valid + Verified?
Email
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
7. A verification email is sent to the student's university email address.
8. The student must verify the email before logging in.

## Login Flow

When an existing student logs in:

1. The student enters university email and password.
2. Firebase Authentication validates the credentials.
3. The system checks whether the user profile exists.
4. The system checks whether the account is blocked or disabled.
5. The system checks whether the email is verified.
6. If all checks pass, the user is redirected to the Home screen.

If the email is not verified, the app sends a new verification link and asks the user to verify the university email before signing in.

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
- The university email is verified.

## Summary

The authentication flow protects the marketplace by requiring verified university email accounts. This helps ensure that only valid students can access buying, selling, chatting, reporting, and wishlist features.
