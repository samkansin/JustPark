package com.parking.justpark;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class CountUp_time extends AppCompatActivity {

    TextView floor,parking_slot,license_plate, price_view,time;
    AppCompatButton payment;
    CircularProgressBar progress_timer;


    private Timer timer;
    private TimerTask timerTask;
    private boolean timeRunning = false;
    private long countTime = 0;
    private long endTime;

    private FirebaseFirestore dbStore;
    private FirebaseDatabase database;
    private int price,counter;

    PaymentSheet paymentSheet;

    String customerID;
    String EPKey;
    String cSecret;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_count_up_time);

        floor = findViewById(R.id.floor);
        parking_slot = findViewById(R.id.parking_slot);
        license_plate = findViewById(R.id.license_plate);
        price_view = findViewById(R.id.price);
        time = findViewById(R.id.countup);
        progress_timer = findViewById(R.id.progress_timer);
        payment = findViewById(R.id.payment);

        dbStore = FirebaseFirestore.getInstance();
        database = FirebaseDatabase.getInstance();


        floor.setText(getIntent().getStringExtra("floor"));
        parking_slot.setText(getIntent().getStringExtra("parking_slot"));
        license_plate.setText(getIntent().getStringExtra("license_plate"));

        PaymentConfiguration.init(this, getString(R.string.PUBLISH_KEY));

        paymentSheet = new PaymentSheet(this, paymentSheetResult -> {
            onPaymentResult(paymentSheetResult);
        });

        payment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PaymentFlow();
            }
        });

        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://api.stripe.com/v1/customers", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject object = new JSONObject(response);
                    customerID = object.getString("id");

                    getEPKey(customerID);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> header = new HashMap<>();
                header.put("Authorization", "Bearer " + getString(R.string.SECRET_KEY));
                return header;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(CountUp_time.this);
        requestQueue.add(stringRequest);



        timer = new Timer();

    }

    private void onPaymentResult(PaymentSheetResult paymentSheetResult) {
        if(paymentSheetResult instanceof PaymentSheetResult.Completed){
            DocumentReference hisRef = dbStore.collection("users")
                    .document("hc13ZJ1SpDT18s1etewlBx44hIg1");


            AggregateQuery count = hisRef.collection("History").count();

            Map<String, Object> HistoryPrice = new HashMap<>();
            HistoryPrice.put("price", price);

            count.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
               if(task.isSuccessful()){
                   AggregateQuerySnapshot snapshot = task.getResult();
                   hisRef.collection("History")
                           .document(String.valueOf(snapshot.getCount()-1))
                           .set(HistoryPrice, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
                               @Override
                               public void onSuccess(Void unused) {
                                   Log.d(TAG, "price successfully written");

                                   SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                                   SharedPreferences.Editor editor = prefs.edit();
                                   editor.putBoolean("hasReserve", false);
                                   editor.apply();
                               }
                           }).addOnFailureListener(new OnFailureListener() {
                               @Override
                               public void onFailure(@NonNull Exception e) {
                                   Log.w(TAG, "failure written");
                               }
                           });
               }
            });
            DatabaseReference slotRef = database.getReference(String.format("Floor/%s/%s",floor.getText().toString(),parking_slot.getText().toString()));
            slotRef.setValue(null);

            Toast.makeText(this, "payment success", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CountUp_time.this, MainActivity.class));

        }
    }

    private void getEPKey(String customerID) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://api.stripe.com/v1/customers", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject object = new JSONObject(response);
                    EPKey = object.getString("id");
                    
                    getcSecret(customerID, EPKey);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> header = new HashMap<>();
                header.put("Authorization", "Bearer " + getString(R.string.SECRET_KEY));
                header.put("Stripe-Version", "2022-08-01");

                return header;
            }

            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("customer", customerID);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(CountUp_time.this);
        requestQueue.add(stringRequest);
    }

    private void getcSecret(String customerID, String epKey) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://api.stripe.com/v1/payment_intents", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject object = new JSONObject(response);
                    cSecret = object.getString("client_secret");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> header = new HashMap<>();
                header.put("Authorization", "Bearer " + getString(R.string.SECRET_KEY));
                return header;
            }

            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("customer", customerID);
                params.put("amount", String.valueOf(price) + "00");
                params.put("currency", "thb");
                params.put("automatic_payment_methods[enabled]", "true");

                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(CountUp_time.this);
        requestQueue.add(stringRequest);
    }

    private void PaymentFlow() {

        paymentSheet.presentWithPaymentIntent(
                cSecret, new PaymentSheet.Configuration("JustPark"
                , new PaymentSheet.CustomerConfiguration(
                        customerID,
                        EPKey
                ))
        );

    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("countTime", countTime);
        editor.putLong("endTime", endTime);
        editor.putBoolean("timeRunning", timeRunning);
        editor.putInt("counter", counter);

        editor.apply();

        if(timerTask != null){
            timerTask.cancel();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        endTime = prefs.getLong("endTime", 0);
        countTime = prefs.getLong("timeLeft", 0);
        timeRunning = prefs.getBoolean("timeRunning", false);
        counter = prefs.getInt("counter", 0);

        UpdateCountText();

        if(timeRunning) {
            countTime = System.currentTimeMillis() - endTime;
        }

        startTimer();
    }

    private void startTimer(){
        endTime = System.currentTimeMillis() - countTime;

        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void run() {
                        countTime+=1000;
                        counter++;
                        progress_timer.setProgressWithAnimation(counter);

                        if(counter == 3600) {
                            counter = 0;
                            String p = price_view.getText().toString();
                            int sum = Integer.parseInt(p.substring(8, p.length())) + 15;
                            price_view.setText(String.format("Price: ฿%d",sum));
                        }


                        UpdateCountText();
                    }
                });
            }
        };
        timeRunning = true;
        timer.scheduleAtFixedRate(timerTask,0,1000);
    }

    private void UpdateCountText(){
        int hours = (int) (countTime / (1000*60*60)) % 24;
        int minutes = (int) (countTime / (1000 * 60)) % 60;
        int seconds = (int) (countTime / 1000) % 60;
        if(minutes > 1){
            price = (hours + 1) * 15;
        }else {
            price = hours * 15;
        }

        price_view.setText(String.format(Locale.getDefault(),"Price: ฿%d",price));
        time.setText(String.format(Locale.getDefault(),"%02d : %02d : %02d",hours,minutes,seconds));
    }

}