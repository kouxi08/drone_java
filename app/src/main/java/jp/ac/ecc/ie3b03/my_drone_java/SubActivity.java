/**
 * 回転数などのデバッグ処理
 */

package jp.ac.ecc.ie3b03.my_drone_java;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class SubActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener, DialogFragment_A.NoticeDialogListener {
    /* tag */
    private static final String TAG = "BluetoothSample";

    /* Bluetooth Adapter */
    private BluetoothAdapter mAdapter;

    /* Bluetoothデバイス */
    private BluetoothDevice mDevice;

    /* Bluetooth UUID(固定) */
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /* デバイス名 環境に合わせて変更*/
    private static final String DEVICE_NAME = "ESP32_drone"; //DroneEsp32
    //private static final String DEVICE_NAME = "ESP32_Aka_test"; //DroneEsp32

    /* Soket */
    private BluetoothSocket mSocket;

    /* Thread */
    private static Thread mThread;

    /* Threadの状態を表す */
    private static boolean isRunning;

    /**
     * 戻るボタン.
     */
    private Button backButton;

    /**
     * kp,ki,kd値の変更ボタン.
     */
    private Button changeButton;

    /**
     * Bluetoothから受信した値.
     */
    private TextView mInputTextView;

    /**
     * Action(取得文字列).
     */
    private static final int VIEW_INPUT = 1;

    /**
     * Connect確認用フラグ
     */
    private static boolean connectFlg = false;

    /**
     * Threadフラグ
     */
    private static boolean Transition_Flag = false;

    /**
     * BluetoothのOutputStream.
     */
    OutputStream mmOutputStream = null;

    /**
     * モーターの受信データ.
     */
    private static TextView DroneMoterTextviw;
    private static TextView MadWickTextviw;

    ProgressDialog progressDialog;

    final Handler handler = new Handler();

    private EditText Kp_EditText;
    private EditText Ki_EditText;
    private EditText Kd_EditText;

    private CustomImageView Throttle_Lever_Sub;
    private int preDx, preDy;
    private TextView textView;
    private int kaitensu_count;

    private static String message = "";

    SubActivity.ThreadSleep1Second th = new SubActivity.ThreadSleep1Second();
    final Handler mainThreadHandler = new Handler();

    private int errorCount;//接続できなかった回数をカウント

    String hello_check = "Ho?";
    String connect_check = "Ct";
    String standby_check = "Sy";
    String motor1_check1 = "M4";
    String MadWick_check = "Yaw";
    String KP_check = "Kp";
    String Ki_check = "Ki";
    String Kd_check = "Kd";

    private int result_hello;
    private int result_connect;
    private int result_standby;
    private int result_motor1;
    private int result_madwick;
    private int result_Kp;
    private int result_Ki;
    private int result_Kd;

    String reception_KP = "0";
    String reception_KI = "0";
    String reception_KD = "0";

    int connect_count = 1;//「ho?」が送られてきた回数をカウント

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        //アクションバー(タイトルバー)非表示
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mThread = new Thread(th);

        textView = findViewById(R.id.text_view_sub);
        mInputTextView = (TextView) findViewById(R.id.inputValueTextView_sub);

        Kp_EditText = (EditText) findViewById(R.id.Kp_editText);
        Ki_EditText = (EditText) findViewById(R.id.Ki_editText);
        Kd_EditText = (EditText) findViewById(R.id.Kd_editText);

        DroneMoterTextviw = (TextView) findViewById(R.id.drone_moter_textView);
        MadWickTextviw = (TextView) findViewById(R.id.Madgwick_textView);
        ;

        Throttle_Lever_Sub = this.findViewById(R.id.Throttle_Lever_image_view_sub);
        Throttle_Lever_Sub.setOnTouchListener(this);

        // Buttonの設定(Layoutにて設定したものを関連付け)
        backButton = (Button) findViewById(R.id.back_button_sub);
        changeButton = (Button) findViewById(R.id.Change_button);
        // ボタンのイベント設定
        backButton.setOnClickListener(this);
        changeButton.setOnClickListener(this);

        // Bluetoothのデバイス名を取得
        // デバイス名は、RNBT-XXXXになるため、
        // DVICE_NAMEでデバイス名を定義
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
        for ( BluetoothDevice device : devices){
            if(device.getName().equals(DEVICE_NAME)){
                mDevice = device;
            }
        }
        Transition_Flag = getIntent().getBooleanExtra("Transition_Flag",false);
        //Bluetooth接続
        bluetooth_connect();
    }

    //ダイアログでボタンが押されたときに動作するコールバック関数を定義
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        //Throttle_Lever_Subの位置を指定
        Throttle_Lever_Sub.layout(1500, 800, 1770, 1009);
        //モーターの回転数をリセット
        kaitensu_count = 0;
        //Bluetooth再接続
        bluetooth_connect();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        //Bluetooth再接続せずMainActivityに戻る
        //Throttle_Lever_Subの位置を指定
        Throttle_Lever_Sub.layout(1500, 800, 1770, 1009);
        //モーターの回転数をリセット
        kaitensu_count = 0;
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDialogNeutralClick(DialogFragment dialog) {
        //仮置き
    }

    //Bluetooth接続ダイアログ
    public void bluetooth_connect(){


        //setTitle("ドローンと接続");
        message = "接続先: " + DEVICE_NAME + "\n接続中．．．";
        if (!connectFlg && Transition_Flag) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("ドローンと接続");
            progressDialog.setMessage(message);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            mThread = new Thread(th);
            // Threadを起動し、Bluetooth接続
            isRunning = true;
            mThread.start();
        }
    }
    // 別のアクティビティが起動した場合の処理
    @Override
    protected void onPause(){
        super.onPause();
        //Throttle_Lever_Subの位置を指定
        Throttle_Lever_Sub.layout(1500, 800, 1770, 1009);
        kaitensu_count = 0;
        Transition_Flag = false;
        if (connectFlg) {
            try {
                // 切断ボタン押下時、'切断:5'を送信
                mmOutputStream.write("切断:5".getBytes());
            } catch (IOException e) {}
        }
        isRunning = false;
        connectFlg = false;

        try{
            mInputTextView.setText("");
            mSocket.close();
        }catch(Exception e){

        }
    }

    //ESP32と接続し、データの送受信を処理するためのスレッド処理



    class ThreadSleep1Second extends Thread implements Runnable{
        @Override
        public void run(){
            InputStream mmInStream = null;
            try {

                /*追記*/
                if (ContextCompat.checkSelfPermission(SubActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    {
                        ActivityCompat.requestPermissions(SubActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                        return;
                    }
                }

                // 取得したデバイス名を使ってBluetoothでSocket接続
                mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mSocket.connect();
                mmInStream = mSocket.getInputStream();
                mmOutputStream = mSocket.getOutputStream();

                // InputStreamのバッファを格納
                byte[] buffer = new byte[1024];

                // 取得したバッファのサイズを格納
                int bytes;
                progressDialog.dismiss();

                connectFlg = true;
                if (connectFlg) {
                    try {
                        // ESP32に'controller connect!'を送信
                        mmOutputStream.write("controller connect!:0".getBytes());
                        errorCount = 0;//接続エラー回数をリセット
                        connect_count = 1;//カウンタをリセット
                    } catch (IOException e) {

                    }
                }
                while(isRunning){//受信データの処理
                    // InputStreamの読み込み
                    bytes = mmInStream.read(buffer);
                    Log.i(TAG,"bytes="+bytes);
                    // String型に変換
                    String readMsg = new String(buffer, 0, bytes);

                    // null以外なら表示
                    if(readMsg.trim() != null && !readMsg.trim().equals("")){
                        Log.i(TAG,"value="+readMsg.trim());

                        Message valueMsg = new Message();
                        valueMsg.what = VIEW_INPUT;
                        valueMsg.obj = readMsg;
                        mHandler.sendMessage(valueMsg);
                    }
                }
            }catch (Exception e){//接続できなかったり、接続が切れた時の処理
                /*
                try{
                    Thread.sleep(5000);
                }catch (InterruptedException eee){
                    eee.printStackTrace();
                }
                */
                //接続中に表示されるダイアログを閉じる
                progressDialog.dismiss();

                try{
                    mSocket.close();
                }catch(Exception ee){

                }
                isRunning = false;
                connectFlg = false;

                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(errorCount > 4){//progressDialogを5回表示しても接続できなかった場合の処理
                            //ダイアログフラグメントのオブジェクトを生成
                            DialogFragment_A dialogFragment = new DialogFragment_A();
                            //ダイアログの表示
                            dialogFragment.show(getSupportFragmentManager(),"DialogFragment_A");
                            errorCount = 0;
                        }else{
                            errorCount++;
                            bluetooth_connect();
                        }
                    }
                });
            }
        }
    }

    //回転数(Throttle_Lever_Sub)操作
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // x,y 位置取得
        int newDx = (int)event.getRawX();
        int newDy = (int)event.getRawY();

        switch (event.getAction()) {
            // タッチダウンでdragされた
            case MotionEvent.ACTION_MOVE:
                // ACTION_MOVEでの位置
                // performCheckを入れろと警告が出るので
                v.performClick();
                int dx = 1500 ; //Throttle_Lever_Sub.getLeft() + (newDx - preDx)  700;　dx(横軸)を固定にして、縦方向にしか動かせないようにする。
                int dy = Throttle_Lever_Sub.getTop() + (newDy - preDy);//縦軸
                int imgW = dx + Throttle_Lever_Sub.getWidth();//画像サイズの横軸
                int imgH = dy + Throttle_Lever_Sub.getHeight();//画像サイスの縦軸

                // Throttle_Leverの座標の範囲を設定し、画面外にいかないようにする
                if(dy < 100){//y座標が100を下回ると位置を固定しそれ以上いかないようにする
                    Throttle_Lever_Sub.layout(dx, 100, 1770, 309);
                    break;
                }else if(dy > 800){//y座標が800を超えると位置を固定しそれ以上いかないようにする
                    Throttle_Lever_Sub.layout(dx, 800, 1770, 1009);
                    break;
                }else{//y座標が100～800の間は自由に動かせる
                    Throttle_Lever_Sub.layout(dx, dy, imgW, imgH);
                }
                //Throttle_Lever_Sub.layout(dx, dy, imgW, imgH); 1.42857143
                //モーターの回転数はThrottle_Leverのy座標の位置を使う
                //回転数は1000~2000の値を渡すので
                kaitensu_count = (int) ((800 - dy)*1.42857143 + 1000);

                String str = "回転数\ndx="+dx+"\ndy="+dy+"\nimgW="+imgW+"\nimgH="+imgH+"\ncount="+kaitensu_count;
                textView.setText(str);

                // 接続中のみ書込みを行う
                if (connectFlg) {
                    try {
                        //回転数を送信
                        String kaitensu_count_string = String.valueOf(kaitensu_count) +":1";
                        mmOutputStream.write(kaitensu_count_string.getBytes());
                    } catch (IOException e) {

                    }
                }  else {
                    //mStatusTextView.setText("接続ボタンを押してください");
                }

                Log.d("onTouch","ACTION_MOVE: dx="+dx+", dy="+dy);
                break;

            case MotionEvent.ACTION_DOWN:
                // nothing to do
                break;

            case MotionEvent.ACTION_UP:
                // nothing to do
                break;

            default:
                break;
        }

        // タッチした位置を古い位置とする
        preDx = newDx;
        preDy = newDy;

        return true;
    }

    // ボタン押下時の処理
    @Override
    public void onClick(View v) {
        //切断ボタンを押したときの処理
        if(v.equals(backButton)) {
            Transition_Flag = false;
            if (connectFlg) {
                try {
                    // backボタン押下時、'切断:5'を送信
                    mmOutputStream.write("切断:5".getBytes());
                } catch (IOException e) {

                }
            }
            isRunning = false;
            connectFlg = false;
            try{
                mInputTextView.setText("");
                mSocket.close();
            } catch(Exception e){

            }
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

        } else if(v.equals(changeButton)) { //kp,ki,kdの値を変更するボタンを押したときの処理
        // 接続中のみ書込みを行う
        if (connectFlg) {
            try {
                //kpの値が変更された時、変更した値をESP32に送信する。
                if(!reception_KP.equals(Kp_EditText.getText().toString())){
                    reception_KP = Kp_EditText.getText().toString();

                    String send_kp = Kp_EditText.getText().toString() + ":p";
                    mmOutputStream.write(send_kp.getBytes());

                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, "KP値が変更されました。", Toast.LENGTH_LONG);
                    toast.show();
                }
                //kiの値が変更された時、変更した値をESP32に送信する。
                if(!reception_KI.equals(Ki_EditText.getText().toString())){
                    reception_KI= Ki_EditText.getText().toString();

                    String send_ki = Ki_EditText.getText().toString() + ":i";
                    mmOutputStream.write(send_ki.getBytes());

                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, "KI値が変更されました。", Toast.LENGTH_LONG);
                    toast.show();
                }
                //kdの値が変更された時、変更した値をESP32に送信する。
                if(!reception_KD.equals(Kd_EditText.getText().toString())){
                    reception_KD= Kd_EditText.getText().toString();

                    String send_kd = Kd_EditText.getText().toString() + ":d";
                    mmOutputStream.write(send_kd.getBytes());

                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, "KD値が変更されました。", Toast.LENGTH_LONG);
                    toast.show();
                }
            } catch (IOException e) {

            }
        }
    }

    }
    @Override
    public void onBackPressed(){
        //  androidのバックボタンを押したときの処理(何も書いていないので、バックボタンは機能しない)
    }

    //ESP32からデータを受信したときの処理
    public void zyusin(String msgStr){

        result_hello = msgStr.indexOf(hello_check);

        result_connect = msgStr.indexOf(connect_check);
        result_standby = msgStr.indexOf(standby_check);
        result_motor1 = msgStr.indexOf(motor1_check1);
        result_madwick = msgStr.indexOf(MadWick_check);

        result_Kp = msgStr.indexOf(KP_check);
        result_Ki = msgStr.indexOf(Ki_check);
        result_Kd = msgStr.indexOf(Kd_check);
        int beginIndex1 = msgStr.indexOf(":");

        if(result_hello != -1){//受信データに「Ho?(ESP32との接続確認のために送られてくる文字列)」が含まれているかのチェック
            mInputTextView.setText(msgStr + connect_count);
            connect_count++;
            if (connectFlg) {//接続されていたらESP32に「Hello!:2」を返す
                try {
                    mmOutputStream.write("Hello!:2".getBytes());
                } catch (IOException e) {

                }
            }
        }else if(result_motor1 != -1) {//受信データに「M1:1000…(各モーターの回転数データ)」が含まれているかのチェック
            DroneMoterTextviw.setText(msgStr);

        }else if(result_madwick != -1){//受信データに「Pitch:…(MadWickフィルタで計算したPitch,roll,yawの値)」が含まれているかのチェック
            MadWickTextviw.setText(msgStr);

        }else if(result_connect != -1){//受信データに「Co(ESP32と接続した際に送られてくる文字列)」が含まれているかのチェック
            mInputTextView.setText(msgStr);

        }else if(result_standby != -1){//受信データに「St(ESP32の初回セットアップが完了した際に送られてくる文字列)」が含まれているかのチェック
            mInputTextView.setText(msgStr);
            Throttle_Lever_Sub.layout(1500, 800, 1770, 1009);

        }else if(result_Kp != -1){//受信データに「Kp:(ESP32に設定されているKP値)」が含まれているかのチェック
            Kp_EditText.setText(msgStr.substring(beginIndex1 + 1));
            reception_KP = Kp_EditText.getText().toString();

        }else if(result_Ki != -1){//受信データに「Ki:(ESP32に設定されているKI値)」が含まれているかのチェック
            Ki_EditText.setText(msgStr.substring(beginIndex1 + 1));
            reception_KI = Ki_EditText.getText().toString();

        }else if(result_Kd != -1){//受信データに「Kd:(ESP32に設定されているKD値)」が含まれているかのチェック
            Kd_EditText.setText(msgStr.substring(beginIndex1 + 1));
            reception_KD = Kd_EditText.getText().toString();
        }
    }

    /**
     * 描画処理はHandlerでおこなう
     */
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int action = msg.what;
            String msgStr = (String)msg.obj;
            result_hello = msgStr.indexOf(hello_check);
            if(action == VIEW_INPUT){
                //mInputTextView.setText(msgStr);
                //ESP32からデータを受信した時、zyusin(496行目)に遷移する
                zyusin(msgStr);
            }
        }
    };
}