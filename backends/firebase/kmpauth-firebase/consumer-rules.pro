# KMPAuth firebase backend is discovered via ServiceLoader; keep the service
# implementation and its no-arg constructor, or minified builds silently
# lose backend auto-registration.
-keep class com.mmk.kmpauth.firebase.backend.FirebaseAuthBackendService { <init>(); }
