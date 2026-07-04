package com.example.invoice;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {
    private static FirebaseHelper instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserUid() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public void login(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void signup(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void loginWithCredential(com.google.firebase.auth.AuthCredential credential, OnCompleteListener<AuthResult> listener) {
        auth.signInWithCredential(credential).addOnCompleteListener(listener);
    }

    public void logout() {
        auth.signOut();
    }

    public void sendPasswordResetEmail(String email, OnCompleteListener<Void> listener) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener(listener);
    }

    // Firestore - Save Business/User Profile (Backward Compatible Overload)
    public void saveUserProfile(String name, String email, String businessName, String taxId, String phone, String address, String logoBase64, OnCompleteListener<Void> listener) {
        saveUserProfile(name, email, "", businessName, taxId, phone, address, logoBase64, listener);
    }

    // Firestore - Save Full Business & Personal User Profile
    public void saveUserProfile(String name, String email, String personalPhone, String businessName, String taxId, String phone, String address, String logoBase64, OnCompleteListener<Void> listener) {
        String uid = getCurrentUserUid();
        if (uid == null) return;

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("email", email);
        profile.put("personalPhone", personalPhone);
        profile.put("businessName", businessName);
        profile.put("taxId", taxId);
        profile.put("businessPhone", phone);
        profile.put("businessAddress", address);
        profile.put("logoBase64", logoBase64);
        profile.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid).set(profile).addOnCompleteListener(listener);
    }

    // Firestore - Get User/Business Profile
    public void getUserProfile(OnCompleteListener<DocumentSnapshot> listener) {
        String uid = getCurrentUserUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnCompleteListener(listener);
    }

    // Firestore - Save Invoice
    public void saveInvoice(Invoice invoice, OnCompleteListener<DocumentReference> listener) {
        String uid = getCurrentUserUid();
        if (uid == null) return;

        db.collection("users").document(uid).collection("invoices").add(invoice).addOnCompleteListener(listener);
    }

    // Firestore - Get Invoices (ordered by creation time descending)
    public void getInvoices(OnCompleteListener<QuerySnapshot> listener) {
        String uid = getCurrentUserUid();
        if (uid == null) return;

        db.collection("users").document(uid).collection("invoices")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(listener);
    }

    // Firestore - Update Invoice Status
    public void updateInvoiceStatus(String invoiceId, String status, OnCompleteListener<Void> listener) {
        String uid = getCurrentUserUid();
        if (uid == null || invoiceId == null) return;

        db.collection("users").document(uid).collection("invoices").document(invoiceId)
                .update("status", status)
                .addOnCompleteListener(listener);
    }
}
