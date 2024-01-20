package com.example.real_heartrate;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private static final int REQUEST_BODY_SENSORS = 2;
    private static final int REQUEST_BODY_SENSORS_BACKGROUND = 3;
    Button btn_hrTest_start,btn_hrTest_stop;
    TextView tv_hrTest;
    private boolean permissionGranted;

    private DatabaseReference mDatabase;

    //初期振り分けされているか
    private boolean IsPortioned = false;

    //水の量(mL単位)
    private int water_amount = 500;

    CountDownTimer　countDownTimer;








    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // Firebase Realtime Databaseへの参照を取得

        mDatabase = FirebaseDatabase.getInstance().getReference("test_user1");
        Log.d("database",mDatabase.toString());

        // "test_user1" ノード内の "hum" と "tmp" への参照を作成
//        DatabaseReference humRef = myRef.child("hum");
//        DatabaseReference tmpRef = myRef.child("tmp");

        // TextViewの参照を取得
//        TextView textViewHumidity = findViewById(R.id.textViewHumidity);
//        TextView textViewTemperature = findViewById(R.id.textViewTemperature);

        //mDatabase.child("test").setValue("test");


        mDatabase.addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Log.d("snap",dataSnapshot.toString());
           Post post = dataSnapshot.getValue(Post.class);
                Log.d("post",post.toString());
            Log.d("post","hum"+ post.hum);
            Log.d("post","tmp"+ post.tmp);
            Log.d("post","tof"+ post.tof);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());

            }
        });













        //);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);    //脈拍数
        Log.d("TAG", "sensor create ended.");

        btn_hrTest_start = findViewById(R.id.btn_hr_test_start);
        btn_hrTest_stop = findViewById(R.id.btn_hr_test_stop);
        tv_hrTest = findViewById(R.id.tv_hr_test);


        // BODY_SENSORSパーミッションの確認
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            // BODY_SENSORSパーミッションをリクエスト
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BODY_SENSORS},
                    REQUEST_BODY_SENSORS);
        } else {
            // BODY_SENSORSパーミッションが付与されたので、BODY_SENSORS_BACKGROUNDパーミッションのチェックに進む。
            checkBodySensorsBackgroundPermission();
        }

        //  セットアップbutton
        btn_hrTest_start.setOnClickListener(view -> permissionGranted = true);


        btn_hrTest_stop.setOnClickListener(view -> permissionGranted = false);

        

    }


    @Override
    protected void onResume() {
        super.onResume();


        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.w("hrActivity", "No heart rate sensor found");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);

    }



    private void checkBodySensorsBackgroundPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS_BACKGROUND)
                != PackageManager.PERMISSION_GRANTED) {
            // BODY_SENSORS_BACKGROUNDパーミッションのリクエスト
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BODY_SENSORS_BACKGROUND},
                    REQUEST_BODY_SENSORS_BACKGROUND);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public void drink(){

    }




    @Override
    public void onSensorChanged(SensorEvent event) {


        if (permissionGranted) {
            if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                // 心拍センサーイベントの処理
                float heartRate = event.values[0];
                Log.d("heart rate tag", String.format("Heart Rate: %.2f", heartRate));
                // 心拍数を表示するTextViewを設定
                tv_hrTest.setText(String.format("HR: %.2f", heartRate));
            }
        }


    }






}