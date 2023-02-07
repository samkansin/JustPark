package com.parking.justpark;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    EditText email, password;
    AppCompatImageButton show_hide;
    AppCompatButton  sign_in_with_google, sign_in;
    TextView donthave;

    private FirebaseAuth auth;

    static boolean hide = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        show_hide = findViewById(R.id.show_hide);
        sign_in_with_google = findViewById(R.id.signinwithgoogle);
        sign_in = findViewById(R.id.signin);
        donthave = findViewById(R.id.donthaveaccount);

        auth = FirebaseAuth.getInstance();

        sign_in_with_google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this, GoogleAuthen.class).putExtra("GoogleAuth","Sign in"));
            }
        });

        donthave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this, SignUp.class));
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

        sign_in.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                checkdata();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null) {
            currentUser = auth.getCurrentUser();
            updateUI(currentUser);
        }
    }

    private void checkdata(){
        String email_user = email.getText().toString();
        String password_user = password.getText().toString();

        if(email_user.isEmpty()){
            email.setError("Email is empty");
        }else if(password_user.isEmpty()){
            password.setError("Password is empty");
        }else {
            auth.signInWithEmailAndPassword(email_user, password_user)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(Login.this, "Sign In: Successful", Toast.LENGTH_SHORT).show();
                                FirebaseUser user = auth.getCurrentUser();
                                updateUI(user);
                            }else {
                                Toast.makeText(Login.this, "Sign In: failure, Please check your email and password", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

    }

    public void updateUI(FirebaseUser user){
        startActivity(new Intent(Login.this, MainActivity.class));
    }
}