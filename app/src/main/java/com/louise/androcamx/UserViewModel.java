package com.louise.androcamx;

import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentReference;

public class UserViewModel extends ViewModel {

    private DocumentReference userRef;

    public DocumentReference getUserRef() {
        return userRef;
    }

    public void setUserRef(DocumentReference userRef) {
        this.userRef = userRef;
    }
}
