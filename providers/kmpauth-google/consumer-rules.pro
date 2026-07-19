# Consumer R8/ProGuard rules shipped with kmpauth-google.
#
# Credential Manager loads its Google Play services provider reflectively, so
# R8 strips it in release builds unless it is kept. Without this, Google
# Sign-In silently does nothing in a minified build while working fine in debug.
# See https://developer.android.com/identity/sign-in/credential-manager
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# The Google ID token credential is constructed from a Bundle by name.
-keep class com.google.android.libraries.identity.googleid.** { *; }
