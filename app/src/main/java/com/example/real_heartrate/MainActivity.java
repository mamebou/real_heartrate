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
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    Button btn_hrTest_start,btn_hrTest_stop, btn_dring;
    TextView tv_hrTest;
    private boolean permissionGranted;

    private DatabaseReference mDatabase;

    //初期振り分けされているか
    private boolean IsPortioned = false;

    //水の量(mL単位)
    private int waterAmount = 500;

    //タイマー残り時間
    //2時間は72000000
    private long remainTime = 40000;

    CountDownTimer timer;

    private long[] notifyTIme = new long[5];

    //画像の切り替わり数(カウントダウン形式)
    private int notifyCount = 4;

    private long notifyTIming = 0;

    private ImageView stateImage;

    private VibrationEffect vib;

    //温度、本番環境ではセンサ値を利用
    private int temperture = 27;

    //環境情報は0, 1, 2で判別
    //0: 低温, 1:中温 2:高温
    private int envState = 0;

    int remainWarterAmount = 0;

    private int tof;

    private int defaultAmountPerMinit = 250 / 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stateImage = findViewById(R.id.state_image);
        btn_dring = findViewById(R.id.drink_button);


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
           String tofString = post.tof;
           float tofFloat = Float.valueOf(tofString);
           int tofInt = (int)tofFloat;
                Log.d("post", String.valueOf(tofInt));
            Log.d("post","hum"+ post.hum);
            Log.d("post","tmp"+ post.tmp);
            Log.d("post","tof"+ post.tof);
            //tof = Integer.parseInt(post.tof);
            remainWarterAmount = getRmainWaterAmount(tofInt);
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
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
        btn_hrTest_start.setOnClickListener(start_button_pushed);


        btn_hrTest_stop.setOnClickListener(stop_button_pushed);

        btn_dring.setOnClickListener(dring_button_pushed);

        if(temperture <= 25){
            envState = 0;
            //暫定
            waterAmount = 400;
        }
        else if(temperture <= 30){
            envState = 1;
            //暫定
            waterAmount = 500;
        }
        else{
            envState = 2;
            //暫定
            waterAmount = 600;
        }

    }

    private View.OnClickListener stop_button_pushed = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            permissionGranted = false;
            timer.cancel();
        }
    };

    private View.OnClickListener dring_button_pushed = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            timer.cancel();
            notifyCount = 4;
            stateImage.setImageResource(R.drawable.state6_6);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            long devidedTIme = remainTime / 5;
            long defaultNeedWaterAmount = devidedTIme * defaultAmountPerMinit;
            long needWaterAmountPerMin = (remainWarterAmount / remainTime);
            Log.d("inDrink WarterAmount", String.valueOf(remainWarterAmount));
            if(needWaterAmountPerMin == 0){
                needWaterAmountPerMin = 1;
            }
            long notifyTImePer = (defaultNeedWaterAmount / needWaterAmountPerMin);
            notifyTIming = notifyTImePer;
            for(int i = 0; i < 5; i++){
                notifyTIme[i] = notifyTIming;
                Log.d("each notify time", String.valueOf(notifyTIme[i]));
                notifyTIming += devidedTIme;
            }
            if(remainWarterAmount == 0){
                for(int i = 0; i < 5; i++){
                    notifyTIme[i] = 1;
                }
            }
            timer = new CountDownTimer(remainTime, 1000){
                @Override
                public void onFinish(){
                    // タイムアップ時の処理をここに記述
                    // この場合start()を実行してから３秒後に呼ばれる
                    timer.cancel();
                }
                @Override
                public void onTick(long millisUntilFinished){
                    // このメソッドの仮引数では残り時間が渡される
                    // また、コンストラクタの第二引数で指定した間隔で呼ばれる
                    // この場合１秒ごとに呼ばれる
                    remainTime = millisUntilFinished;
                    if(notifyTIme[notifyCount] > millisUntilFinished){
                        //もうちょいうまくやれるかもとりあえずごり押し
                        Log.d("message", "画像差し替え処理");
                        if(notifyCount == 4){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vib =  VibrationEffect.createWaveform(new long[]{0, 300}, VibrationEffect.DEFAULT_AMPLITUDE);
                                Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(vib);
                            }
                            stateImage.setImageResource(R.drawable.state5_6);
                        }
                        else if(notifyCount == 3){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vib =  VibrationEffect.createWaveform(new long[]{0, 300, 300, 300}, VibrationEffect.DEFAULT_AMPLITUDE);
                                Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(vib);
                            }
                            stateImage.setImageResource(R.drawable.state4_6);
                        }
                        else if(notifyCount == 2){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vib =  VibrationEffect.createWaveform(new long[]{0, 300, 300, 300, 300, 300}, VibrationEffect.DEFAULT_AMPLITUDE);
                                Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(vib);
                            }
                            stateImage.setImageResource(R.drawable.stete3_6);
                        }
                        else if(notifyCount == 1){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vib =  VibrationEffect.createWaveform(new long[]{0, 300, 300, 300, 300, 300, 300, 300}, VibrationEffect.DEFAULT_AMPLITUDE);
                                Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(vib);
                            }
                            stateImage.setImageResource(R.drawable.state2_6);
                        }
                        else if(notifyCount == 0){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vib =  VibrationEffect.createWaveform(new long[]{0, 300, 300, 300, 300, 300, 300, 300, 300, 300}, VibrationEffect.DEFAULT_AMPLITUDE);
                                Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(vib);
                            }
                            stateImage.setImageResource(R.drawable.state1_6);
                        }
                        notifyCount--;
                        if(notifyCount < 0){
                            notifyCount = 0;
                        }
                    }
                }
            };
            timer.start();

            Toast.makeText(MainActivity.this,"button clicked",Toast.LENGTH_SHORT).show();
        }
    };

    private int getRmainWaterAmount(int tofValue){
        remainWarterAmount = 0;
        Log.d("tofValue", String.valueOf(tofValue));
        if(tofValue <= 30){
            remainWarterAmount = 500;
        }
        else if(tofValue <= 76){
            remainWarterAmount = 400;
        }
        else if(tofValue <= 159){
            remainWarterAmount = 300;
        }
        else if(tofValue <= 193){
            remainWarterAmount = 200;
        }
        else if(tofValue <= 220){
            remainWarterAmount = 100;
        }
        return remainWarterAmount;
    }

    private View.OnClickListener start_button_pushed = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            permissionGranted = true;
            timer = new CountDownTimer(remainTime, 1000){
                @Override
                public void onFinish(){
                    // タイムアップ時の処理をここに記述
                    // この場合start()を実行してから３秒後に呼ばれる
                    timer.cancel();
                }
                @Override
                public void onTick(long millisUntilFinished){
                    // このメソッドの仮引数では残り時間が渡される
                    // また、コンストラクタの第二引数で指定した間隔で呼ばれる
                    // この場合１秒ごとに呼ばれる
                    remainTime = millisUntilFinished;
                    if(notifyTIme[notifyCount] > millisUntilFinished){
                        //もうちょいうまくやれるかもとりあえずごり押し
                        Log.d("message", "画像差し替え処理");
                        if(notifyCount == 4){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vib =  VibrationEffect.createWaveform(new long[]{0, 300}, VibrationEffect.DEFAULT_AMPLITUDE);
                                Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(vib);
                            }
                            stateImage.setImageResource(R.drawable.state5_6);
                        }
                        else if(notifyCount == 3){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vib =  VibrationEffect.createWaveform(new long[]{0, 300, 300, 300}, VibrationEffect.DEFAULT_AMPLITUDE);
                                Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(vib);
                            }
                            stateImage.setImageResource(R.drawable.state4_6);
                        }
                        else if(notifyCount == 2){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vib =  VibrationEffect.createWaveform(new long[]{0, 300, 300, 300, 300, 300}, VibrationEffect.DEFAULT_AMPLITUDE);
                                Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(vib);
                            }
                            stateImage.setImageResource(R.drawable.stete3_6);
                        }
                        else if(notifyCount == 1){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vib =  VibrationEffect.createWaveform(new long[]{0, 300, 300, 300, 300, 300, 300, 300}, VibrationEffect.DEFAULT_AMPLITUDE);
                                Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(vib);
                            }
                            stateImage.setImageResource(R.drawable.state2_6);
                        }
                        else if(notifyCount == 0){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vib =  VibrationEffect.createWaveform(new long[]{0, 300, 300, 300, 300, 300, 300, 300, 300, 300}, VibrationEffect.DEFAULT_AMPLITUDE);
                                Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(vib);
                            }
                            stateImage.setImageResource(R.drawable.state1_6);
                        }
                        notifyCount--;
                        if(notifyCount < 0){
                            notifyCount = 0;
                        }
                    }
                }
            };
            long devidedTIme = remainTime / 5;
            devidedTIme = (remainTime - devidedTIme) / 5;
            notifyTIming = devidedTIme;
            for(int i = 0; i < 5; i++){
                notifyTIme[i] = notifyTIming;
                Log.d("each notify time", String.valueOf(notifyTIme[i]));
                notifyTIming += devidedTIme;
            }
            timer.start();
        }
    };

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