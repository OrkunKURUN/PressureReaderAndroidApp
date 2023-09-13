package com.example.pressurereader;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class QueryRecordActivity extends AppCompatActivity {
    private DatabaseReference rDatabase, rDatabase2;
    private ArrayList<String> plates = new ArrayList<>();
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_record);

        EditText plate = findViewById(R.id.searchPlate);
        Spinner select = findViewById(R.id.selectPlate);
        Button search = findViewById(R.id.searchButton);
        TextView result = findViewById(R.id.queryResult);

        rDatabase = FirebaseDatabase.getInstance().getReference().child("vehicles");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        ValueEventListener plateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot vSnapshot : snapshot.getChildren()){
                    Vehicle vehicle = vSnapshot.getValue(Vehicle.class);
                    assert vehicle != null;
                    adapter.add(vehicle.getPlate());
                }
                adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
                select.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("TAG", "loadPost:onCancelled", error.toException());
            }
        } ;
        rDatabase.addValueEventListener(plateListener);

        AdapterView.OnItemSelectedListener selectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                plate.setText((String)adapterView.getItemAtPosition(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };
        select.setOnItemSelectedListener(selectedListener);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rDatabase2 = rDatabase.child(plate.getText().toString());
                rDatabase2.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()) {
                            Log.e("firebase", "Error getting data", task.getException());
                        }
                        else {
                            Vehicle vehicle = task.getResult().getValue(Vehicle.class);
                            String text = "Vehicle type:\t";
                            assert vehicle != null;
                            if (vehicle.getVehicleType() == 0)
                                text = text + "Semi truck";
                            else
                                text = text + "Trailer";
                            text = text + "\nSystem leaking:\t";
                            if (vehicle.getLeaking())
                                text = text + "Yes";
                            else
                                text = text + "No";
                            result.setText(text);
                        }
                    }
                });
            }
        });
    }
}
