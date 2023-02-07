package com.parking.justpark;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.budiyev.android.codescanner.ErrorCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.Result;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.util.Locale;

public class Countdown_time extends AppCompatActivity {
    private static final long START_TIME_MILLIS = 3600000;
    private static final int CAMERA_REQUEST_CODE = 101;


    TextView time,floor,parking_slot, licence_plate;
    CircularProgressBar progressBar;
    AppCompatImageButton back;
    AppCompatButton btn_scan_qr, cancel_booking;

    private FirebaseDatabase database;

    private CodeScanner codeScanner;
    private CountDownTimer countDownTimer;
    private boolean timeRunning;
    private long timeLeft;
    private long endTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown_time);

        time = findViewById(R.id.countdown);
        progressBar = findViewById(R.id.progress_timer);
        floor = findViewById(R.id.floor);
        parking_slot = findViewById(R.id.parking_slot);
        back = findViewById(R.id.back);
        btn_scan_qr = findViewById(R.id.btn_scan_qr);
        licence_plate = findViewById(R.id.license_plate);
        cancel_booking = findViewById(R.id.cancel);

        database = FirebaseDatabase.getInstance();

        floor.setText(getIntent().getStringExtra("floor"));
        parking_slot.setText(getIntent().getStringExtra("parking_lot"));
        licence_plate.setText(getIntent().getStringExtra("license_plate"));

        setReserveStatus(true);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Countdown_time.this, ChooseSpot.class));
            }
        });

        btn_scan_qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.activity_scan_qr);
                setPermissionAndOpenCamera();
                AppCompatImageButton backtomain = findViewById(R.id.back_to_count);
                backtomain.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong("timeLeft", timeLeft);
                        editor.putLong("endTime", endTime);
                        editor.putBoolean("timeRunning", timeRunning);

                        editor.apply();

                        startActivity(new Intent(Countdown_time.this,Countdown_time.class));
                    }
                });
            }
        });

        cancel_booking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

    }

    private void setReserveStatus(boolean status){
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("hasReserve", status);
        editor.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("timeLeft", timeLeft);
        editor.putLong("endTime", endTime);
        editor.putBoolean("timeRunning", timeRunning);

        editor.apply();

        if(countDownTimer != null){
            countDownTimer.cancel();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        endTime = prefs.getLong("endTime", 0);
        timeLeft = prefs.getLong("timeLeft", START_TIME_MILLIS);
        System.out.println(timeLeft);
        timeRunning = prefs.getBoolean("timeRunning", false);

        updateCountDown();

        if(timeRunning)
            timeLeft = endTime - System.currentTimeMillis();

        if(timeLeft < 0){
            timeLeft = 0;
            updateCountDown();
        }else {
            startTimer();
        }

    }

    private void showDialog(){
        final Dialog dialog = new Dialog(Countdown_time.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.cancel_dialog);

        final AppCompatButton yes = dialog.findViewById(R.id.yes);
        final AppCompatButton no = dialog.findViewById(R.id.no);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                timeLeft = START_TIME_MILLIS;
                endTime = 0;
                timeRunning = false;

                editor.remove("hasReserve");

                editor.apply();

                if(countDownTimer != null){
                    countDownTimer.cancel();
                }

                System.out.println(prefs.getLong("endTime",0));


                DatabaseReference slotRef = database.getReference(String.format("Floor/%s",floor.getText().toString()));
                slotRef.child(parking_slot.getText().toString()).setValue(null);

                startActivity(new Intent(Countdown_time.this, ChooseSpot.class));
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }



    private void startTimer(){
        endTime = System.currentTimeMillis() + timeLeft;

        countDownTimer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                progressBar.setProgressWithAnimation(START_TIME_MILLIS - timeLeft);
                updateCountDown();
            }

            @Override
            public void onFinish() {
                progressBar.setProgressWithAnimation(START_TIME_MILLIS);
                timeRunning = false;
                timeLeft = START_TIME_MILLIS;
                endTime = 0;

                DatabaseReference slotRef = database.getReference(String.format("Floor/%s/%s",floor.getText().toString(),parking_slot.getText().toString()));
                slotRef.setValue(null);

                setReserveStatus(false);

               // startActivity(new Intent(Countdown_time.this, ChooseSpot.class));
            }
        }.start();

        timeRunning = true;
    }

    private void updateCountDown(){
        int minute = (int) (timeLeft / 1000) / 60;
        int seconds = (int) (timeLeft / 1000) % 60;

        String timeFormat = String.format(Locale.getDefault(),"%02d : %02d",minute,seconds);

        time.setText(timeFormat);
    }

    private void Scanner(){
        CodeScannerView scan_view = findViewById(R.id.scanner_qr);
        codeScanner = new CodeScanner(this, scan_view);
        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(result.getText().equalsIgnoreCase("Arrived at slot")){
                            Intent sendData = new Intent(Countdown_time.this, CountUp_time.class);
                            sendData.putExtra("floor", floor.getText().toString());
                            sendData.putExtra("license_plate", licence_plate.getText().toString());
                            sendData.putExtra("parking_slot", parking_slot.getText().toString());

                            startActivity(sendData);
                        }
                    }
                });
            }
        });

        codeScanner.setErrorCallback(new ErrorCallback() {
            @Override
            public void onError(@NonNull Throwable thrown) {
                Toast.makeText(Countdown_time.this, "Camera Error: "+thrown.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        codeScanner.startPreview();

    }

    private void setPermissionAndOpenCamera(){
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (permission != PackageManager.PERMISSION_GRANTED){
            createRequest();
        }else{
            Scanner();
        }
    }

    private void createRequest(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Scanner();
            }else{
                Toast.makeText(this, "Need the camera permission", Toast.LENGTH_SHORT).show();
            }
        }
    }
}