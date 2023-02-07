package com.parking.justpark;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CarInfo extends AppCompatActivity {

    TextView floor, parking_slot;
    AppCompatAutoCompleteTextView dropdown_province;
    AppCompatButton reserve;
    AppCompatImageButton back;
    TextInputLayout text_license_plate, text_dropdown_province;
    EditText license_plate;

    private FirebaseFirestore dbStore;
    private FirebaseDatabase db;
    private FirebaseAuth auth;



    Handler mainHandler = new Handler();
    ProgressDialog progressDialog;
    static ArrayList<String> provinces = new ArrayList<>();
    private static String prov = "";


    ArrayAdapter<String> adapterItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_info);

        floor = findViewById(R.id.floor);
        parking_slot = findViewById(R.id.parking_slot);
        back = findViewById(R.id.back);
        dropdown_province = findViewById(R.id.dropdown_province);
        license_plate = findViewById(R.id.license_plate);
        text_license_plate = findViewById(R.id.text_license_plate);
        text_dropdown_province = findViewById(R.id.text_provinces);
        reserve = findViewById(R.id.reserve);


        dbStore = FirebaseFirestore.getInstance();
        db  = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        floor.setText(getIntent().getStringExtra("floor"));
        parking_slot.setText(getIntent().getStringExtra("parking_lot"));

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CarInfo.this, ChooseSpot.class));
            }
        });

        license_plate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus){
                    text_license_plate.setBoxBackgroundColor(0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        text_license_plate.setBoxStrokeColor(getColor(R.color.yellow));
                    }

                }else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        text_license_plate.setBoxStrokeColor(getColor(R.color.lightGray));
                    }
                }
            }
        });

        dropdown_province.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus){
                    text_dropdown_province.setBoxBackgroundColor(0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        text_dropdown_province.setBoxStrokeColor(getColor(R.color.yellow));
                    }

                }else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        text_dropdown_province
                                .setBoxStrokeColor(getColor(R.color.lightGray));
                    }
                }
            }
        });

        reserve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!license_plate.getText().toString().isEmpty() && !prov.isEmpty()){
                    showDialog();
                }else Toast.makeText(CarInfo.this, "License plate or Province are empty", Toast.LENGTH_SHORT).show();
            }
        });


        setProvinces();
        new FethData().start();
    }

    private void setProvinces(){

        System.out.println(provinces);
        adapterItems = new ArrayAdapter<String>(CarInfo.this, R.layout.province_list, provinces);
        dropdown_province.setAdapter(adapterItems);
        dropdown_province.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                prov = parent.getItemAtPosition(position).toString();
                Toast.makeText(CarInfo.this, "Province " + prov, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void showDialog(){
        final Dialog dialog = new Dialog(CarInfo.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.custom_dialog);

        final TextView description = dialog.findViewById(R.id.description);
        final AppCompatButton confirm = dialog.findViewById(R.id.confirm);
        final AppCompatButton cancel = dialog.findViewById(R.id.cancel);

        description.setText(getString(R.string.descript) + " ");
        description.setText(Html.fromHtml(String.format("%s <font color=#000><b>%s %s</b></font>", getString(R.string.descript), license_plate.getText().toString(), prov)));

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
            }
        });

        DatabaseReference slotRef = db.getReference(String.format("Floor/%s/%s",floor.getText().toString(),parking_slot.getText().toString()));

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slotRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if(task.isSuccessful()) {
                            if(task.getResult().getValue() == null) {
                                slotRef.setValue(getIntent().getIntExtra("IndexSlot", 0));

                                DocumentReference hisRef = dbStore.collection("users").document(Objects.requireNonNull(auth.getUid()));

                                Instant instant = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    instant = Instant.now();
                                }

                                Map<String, Object> History = new HashMap<>();
                                History.put("dateTime", instant);
                                History.put("license_plate", license_plate.getText().toString());
                                History.put("provinces", prov);

                                Intent sendData = new Intent(CarInfo.this, Countdown_time.class);
                                sendData.putExtra("floor", floor.getText().toString());
                                sendData.putExtra("parking_lot", parking_slot.getText().toString());
                                sendData.putExtra("license_plate", license_plate.getText().toString() + " " + prov);

                                AggregateQuery count = hisRef.collection("History").count();

                                count.get(AggregateSource.SERVER).addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        AggregateQuerySnapshot snapshot = task1.getResult();
                                        hisRef.collection("History").document(String.valueOf(snapshot.getCount()))
                                                .set(History).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Log.d(TAG, "successfully written");
                                                startActivity(sendData);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w(TAG, "Error writing document", e);
                                            }
                                        });


                                    }
                                });


                            }
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    class FethData extends Thread{

        String data = "";

        @Override
        public void run() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog = new ProgressDialog(CarInfo.this);
                    progressDialog.setMessage("Loading data...");
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                }
            });

            try{
                URL url = new URL("https://opend.data.go.th/govspending/bbgfmisprovince?api-key=kuSWS6bStEXwdE7xqY6LwLEHQs5WOeNX");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while((line = bufferedReader.readLine()) != null){
                    data = data + line;
                }
                if(!data.isEmpty()){
                    JSONObject jsonObject = new JSONObject(data);
                    JSONArray province = jsonObject.getJSONArray("result");
                    for(int i = 0; i < province.length(); i++){
                        provinces.add(province.getJSONObject(i).getString("prov_name"));
                    }
                }

            }catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(progressDialog.isShowing())
                        progressDialog.dismiss();
                }
            });
        }

    }

}

