package com.parking.justpark;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatSpinner;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChooseSpot extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    AppCompatSpinner dropdown_floor;
    TextView parking_slot,available_car;
    LinearLayout zone_left, zone_right;
    AppCompatImageButton back;
    AppCompatButton book_spot;
    private static String floor;
    private static int indexSlot;
    private static Map<String, Integer> slot_list;
    FirebaseDatabase database;
    private static boolean hasReserve;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_spot);

        dropdown_floor = findViewById(R.id.dropdown_floor);
        zone_left = findViewById(R.id.zone_left);
        zone_right = findViewById(R.id.zone_right);
        parking_slot = findViewById(R.id.parking_slot);
        available_car = findViewById(R.id.available);
        book_spot = findViewById(R.id.book_spot);
        back = findViewById(R.id.back);
        floor = "1";

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        hasReserve = prefs.getBoolean("hasReserve",false);

        if(hasReserve)
            book_spot.setText("Go to your reserve");
        else
            book_spot.setText("Book spot");


        zone_left.post(new Runnable() {
            @Override
            public void run() {
                float cut = zone_left.getHeight() * 90 / 100;
                int start = 20;
                int result = (int) (zone_left.getHeight() - cut);
//                result /= 2;

                if(zone_left.getHeight() >= 1935){
                    result += 100;
                    start = 50;
                }else
                    result += 60;

                zone_left.setPaddingRelative(start,result,5, result);
                zone_right.setPaddingRelative(5,result,start,result);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChooseSpot.this, MainActivity.class));
            }
        });



        database = FirebaseDatabase.getInstance();


        setSlot();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.floor_array, R.layout.custom_spinner_list_item);
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        dropdown_floor.setAdapter(adapter);

        dropdown_floor.setOnItemSelectedListener(this);
        book_spot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                String path = "Floor/"+floor+"/"+parking_slot.getText().toString();
                DatabaseReference slotRef = database.getReference(path);
                slotRef.setValue(slot_list.get(parking_slot.getText().toString()));*/
                if(hasReserve){
                    startActivity(new Intent(ChooseSpot.this, Countdown_time.class));
                }else if(parking_slot.getText().toString().length() > 2) {
                        Intent sendData = new Intent(ChooseSpot.this, CarInfo.class);
                        sendData.putExtra("floor", floor);
                        sendData.putExtra("parking_lot", parking_slot.getText().toString());
                        sendData.putExtra("IndexSlot", slot_list.get(parking_slot.getText().toString()));
                        startActivity(sendData);
                    }else{
                    //Show Dialog
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        DatabaseReference slot_floor = database.getReference("Floor/"+floor);
        slot_floor.addChildEventListener(new ChildEventListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                System.out.println("ADD: " + snapshot.getValue());
                addCar(snapshot.getKey(), snapshot.getValue().toString());
                int car = 0;
                if(available_car.getText().toString().length() > 4){
                    car = Integer.parseInt(available_car.getText().toString().substring(0,2));
                }else car = Character.getNumericValue(available_car.getText().toString().charAt(0));
                available_car.setText(String.format("%d/12",car+1));
            }

            @SuppressLint("DefaultLocale")
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //System.out.println("CHANGE: " + snapshot.getKey());
                //addCar(snapshot.getKey(), snapshot.getValue().toString());
            }

            @SuppressLint("DefaultLocale")
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                System.out.println(String.format("KEY: [%s] | VALUE: [%s]",snapshot.getKey(),snapshot.getValue()));
                AppCompatButton slot;
                if(Integer.parseInt(snapshot.getKey().substring(3,5)) < 7){
                    slot = (AppCompatButton) zone_left.getChildAt(((Long) snapshot.getValue()).intValue());
                    slot.setRotation(0);
                }else slot = (AppCompatButton) zone_right.getChildAt(((Long) snapshot.getValue()).intValue());

                slot.setText(snapshot.getKey());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    slot.setTextColor(getColor(R.color.white));
                }

                slot.setEnabled(!hasReserve);

                slot.setBackground(getDrawable(R.drawable.bg_unselect_spot));

                int car = 0;
                if(available_car.getText().toString().length() > 4){
                    car = Integer.parseInt(available_car.getText().toString().substring(0,2));
                }else car = Character.getNumericValue(available_car.getText().toString().charAt(0));
                available_car.setText(String.format("%d/12",car-1));
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void addCar(String key, String value){
        if(key != null) {
            if (floor.equals(key.charAt(1) + "")) {
                AppCompatButton slot;
                int index = Integer.parseInt(value);
                if (Integer.parseInt(key.substring(3, 5)) < 7) {
                    slot = (AppCompatButton) zone_left.getChildAt(index);
                    slot.setRotation(180);
                } else slot = (AppCompatButton) zone_right.getChildAt(index);

                if (!slot.getText().toString().isEmpty()) {
                    slot.setBackground(getDrawable(R.drawable.car));
                    slot.setText("");
                    slot.setEnabled(!hasReserve);
                    slot_view = null;
                }
            }
        }
    }

    private void setSlotName(){
        int slot_count = 0;
        slot_list = new HashMap<>();

        for(int i = 0 ; i < zone_left.getChildCount(); i++){
            if(zone_left.getChildAt(i) instanceof  AppCompatButton){
                slot_count++;
                String slot_id = String.format("F%s-%02d",floor,slot_count);
                AppCompatButton slot = (AppCompatButton) zone_left.getChildAt(i);
                slot.setText(slot_id);
                slot_list.put(slot_id, i);

                slot.setBackground(getDrawable(R.drawable.bg_unselect_spot));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    slot.setTextColor(getColor(R.color.white));
                }
                slot.setRotation(0);
                slot.setEnabled(!hasReserve);
            }
        }
        for(int i = 0 ; i < zone_right.getChildCount(); i++){
            if(zone_right.getChildAt(i) instanceof  AppCompatButton){
                slot_count++;
                AppCompatButton slot = (AppCompatButton) zone_right.getChildAt(i);
                String slot_id = String.format("F%s-%02d",floor,slot_count);
                slot.setText(slot_id);

                slot_list.put(slot_id, i);

                slot.setBackground(getDrawable(R.drawable.bg_unselect_spot));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    slot.setTextColor(getColor(R.color.white));
                }

                slot.setEnabled(!hasReserve);
            }
        }
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale", "UseCompatLoadingForDrawables"})
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        floor = parent.getItemAtPosition(position).toString();
        DatabaseReference slotRef = database.getReference("Floor/"+floor);
        parking_slot.setText("F"+floor);

        slotRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if(task.isSuccessful()){
                    HashMap<String,Integer> dataList = (HashMap<String, Integer>) task.getResult().getValue();
                    if(dataList != null) {
                        available_car.setText(dataList.size()+"/12");
                        for (Map.Entry<String, Integer> entry : dataList.entrySet()) {
                            addCar(entry.getKey(), String.valueOf(entry.getValue()));
                        }
                    }else available_car.setText("0/12");
                }else{
                    Log.e("firebase", "Error getting data", task.getException());
                }
            }
        });

        setSlotName();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    static String slot_state;
    static AppCompatButton slot_view = null;

    private void setSlot(){
        for(int i = 0 ; i < zone_left.getChildCount(); i++){
            if(zone_left.getChildAt(i) instanceof AppCompatButton ){
                AppCompatButton slot = (AppCompatButton) zone_left.getChildAt(i);
                slot.setOnClickListener(new View.OnClickListener() {
                    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables", "ResourceAsColor"})
                    @Override
                    public void onClick(View v) {
                        if(!slot.getText().equals("Book")) {
                            if(slot_view == null){
                                slotSelected(slot);
                            }else {
                                slotUnSelected(slot_view);
                                slotSelected(slot);
                            }
                        }else {
                            slotUnSelected(slot);
                        }

                    }
                });
            }
        }

        for(int i = 0 ; i < zone_right.getChildCount(); i++){
            if(zone_left.getChildAt(i) instanceof AppCompatButton ){
                AppCompatButton slot = (AppCompatButton) zone_right.getChildAt(i);
                slot.setOnClickListener(new View.OnClickListener() {
                    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables", "ResourceAsColor"})
                    @Override
                    public void onClick(View v) {
                        if(!slot.getText().equals("Book")) {
                            if(slot_view == null){
                                slotSelected(slot);
                            }else {
                                slotUnSelected(slot_view);
                                slotSelected(slot);
                            }

                        }else {
                            slotUnSelected(slot);
                        }

                    }
                });
            }
        }
    }



    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    private void slotSelected(AppCompatButton slot){
        slot_state = slot.getText().toString();
        slot_view = slot;
        slot.setBackground(getDrawable(R.drawable.bg_select_spot));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            slot.setTextColor(getColor(R.color.black));
        }
        parking_slot.setText(slot.getText().toString());
        slot.setText("Book");

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void slotUnSelected(AppCompatButton slot){
        slot_view = null;
        slot.setBackground(getDrawable(R.drawable.bg_unselect_spot));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            slot.setTextColor(getColor(R.color.white));
        }
        parking_slot.setText("F"+floor);
        slot.setText(slot_state);
    }


}