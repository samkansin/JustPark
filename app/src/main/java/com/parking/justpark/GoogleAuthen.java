package com.parking.justpark;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GoogleAuthen extends AppCompatActivity {

    ProgressDialog progressDialog;

    private GoogleSignInClient google_client;
    private static final int REQUEST_CODE = 101;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore dbStore;
    private String GoogleAuth = "Sign up";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if(extras != null)
            GoogleAuth = extras.getString("GoogleAuth");
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Google "+GoogleAuth+"...");
        progressDialog.setTitle("System");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        GoogleSignInOptions google_options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        google_client = GoogleSignIn.getClient(this, google_options);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        dbStore = FirebaseFirestore.getInstance();

        startActivityForResult(google_client.getSignInIntent(), REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try{
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            }catch(ApiException e){
                progressDialog.dismiss();
                Toast.makeText(this, "Google "+GoogleAuth+" failed", Toast.LENGTH_SHORT).show();
                finish();

            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken){
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            progressDialog.dismiss();
                            FirebaseUser user_ = auth.getCurrentUser();
                            updateUI(user_);
                        }else {
                            progressDialog.dismiss();
                            finish();
                        }
                    }
                });
    }


    public void updateUI(FirebaseUser muser){
        progressDialog.dismiss();
        Map<String,String> userData = new HashMap<>();
        userData.put("name", muser.getDisplayName());
        userData.put("phone", muser.getPhoneNumber());
        dbStore.collection("users").document(muser.getUid())
                .set(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "Data successfully written");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
        startActivity(new Intent(GoogleAuthen.this, MainActivity.class));
    }


}