package com.parking.justpark;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignUp extends AppCompatActivity {

    EditText name, phone, email, password;
    AppCompatImageButton show_hide;
    AppCompatButton signup_btn, signup_with_google;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z.]+\\.+[a-z.]+";
    String phonePattern = "^[0][689][0-9]{8}$";
    ProgressDialog progressDialog;
    TextView haveaccount;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore database;

    static boolean hide = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);



        name = findViewById(R.id.Name);
        phone = findViewById(R.id.Phone);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        show_hide = findViewById(R.id.show_hide);
        signup_btn = findViewById(R.id.signup);
        signup_with_google = findViewById(R.id.signupwithgoogle);
        haveaccount = findViewById(R.id.haveanaccount);

        progressDialog = new ProgressDialog(this);


        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        database = FirebaseFirestore.getInstance();

        signup_with_google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUp.this, GoogleAuthen.class));
            }
        });

        haveaccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUp.this, Login.class));
            }
        });

        show_hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(hide){
                    hide = false;
                    show_hide.setImageResource(R.drawable.ic_hide);
                    password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                }else{
                    hide = true;
                    show_hide.setImageResource(R.drawable.ic_show);
                    password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        signup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkData();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null){
            updateUI(currentUser);
        }
    }


    public void updateUI(FirebaseUser muser){
        progressDialog.dismiss();

        Toast.makeText(this, "You Signed up successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(SignUp.this, MainActivity.class));
    }


    private void checkData(){

            String name_user = name.getText().toString();
            String email_user = email.getText().toString();
            String phone_user = phone.getText().toString();
            String password_user = password.getText().toString();

            if(name_user.isEmpty()){
                name.setError("Full name is empty. Please enter your full name");
                //Toast.makeText(SignUp.this, getString(R.string.name_empty), Toast.LENGTH_SHORT).show();
            }else if(!phone_user.matches(phonePattern)){
                phone.setError("Phone may be empty or phone pattern invalid. must be 10 digits, start with 0 and follow by 6, 8, or 9");
            }else if(password_user.isEmpty() || password_user.length() < 8){
                password.setError("Password may be empty or password pattern invalid, more than 8 character");
            }else if(!email_user.matches(emailPattern)){
                email.setError("Email may be empty or email pattern invalid. example {justpark@gmail.com}");
            }else {
                progressDialog.setMessage("Please wait While Registration...");
                progressDialog.setTitle("Registration");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                Map<String, Object> userdb = new HashMap<>();
                userdb.put("name", name_user);
                userdb.put("phone", phone_user);
                userdb.put("reserve", false);


                auth.createUserWithEmailAndPassword(email_user, password_user).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            progressDialog.dismiss();
                            user = auth.getCurrentUser();

                            DocumentReference doc = database.collection("users").document(user.getUid());



                            database.collection("users").document(user.getUid())
                                    .set(userdb)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.d(TAG, "Data successfully written");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error writing document", e);
                                        }
                                    });

                            startActivity(new Intent(SignUp.this, MainActivity.class));
                            Toast.makeText(SignUp.this, "Successful", Toast.LENGTH_SHORT).show();
                        }else{
                            progressDialog.dismiss();
                            Toast.makeText(SignUp.this, ""+ Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        }





}