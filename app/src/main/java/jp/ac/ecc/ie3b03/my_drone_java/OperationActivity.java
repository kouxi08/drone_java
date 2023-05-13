package jp.ac.ecc.ie3b03.my_drone_java;

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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import androidx.appcompat.app.ActionBar;

public class OperationActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener, DialogFragment_A.NoticeDialogListener {
    /* tag */
    private static final String TAG = "BluetoothSample";

    /* Bluetooth Adapter */
    private BluetoothAdapter mAdapter;

    /* Bluetoothデバイス */
    private BluetoothDevice mDevice;

    /* Bluetooth UUID(固定) */
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /* ESP32のデバイス名 環境に合わせて変更*/
    private static final String DEVICE_NAME = "ESP32_drone"; //DroneEsp32

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
     * Bluetoothから受信した値.
     */
    private TextView mInputTextView;

    /**
     * Action(ステータス表示).
     */
    private static final int VIEW_STATUS = 0;

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

    //接続中に表示されるダイアログの宣言
    ProgressDialog progressDialog;

    //Handlerの宣言
    final Handler handler = new Handler();

    //回転数を制御するThrottle_Leverの宣言
    private CustomImageView Throttle_Lever;
    //
    private int preDx, preDy;
    private TextView Kaitensu_textView;
    private int Kaitensu_count;

    private static String message = "";

    ThreadSleep1Second th = new ThreadSleep1Second();
    final Handler mainThreadHandler = new Handler();

    private int errorCount;//接続できなかった回数をカウント

    private String hello_check = "Ho?";
    private String connect_check = "Ct";
    private String standby_check = "Sy";

    private int result_hello;
    private int result_connect;
    private int result_standby;

    /**
     * Joy Stick関連の宣言
     */
    RelativeLayout layout_joystick;
    ImageView image_joystick, image_border;
    TextView TextView_X, TextView_Y, TextView_Angle, TextView_Distance, TextView_Direction;

    JoyStickClass js;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operation);

        //アクションバー(タイトルバー)非表示
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        layout_joystick = (RelativeLayout) findViewById(R.id.layout_joystick);

        //JoyStickパラメータ値表示用textViewとの関連付け
        TextView_X = (TextView) findViewById(R.id.textView_X);
        TextView_Y = (TextView) findViewById(R.id.textView_Y);
        TextView_Angle = (TextView) findViewById(R.id.textView_Angle);
        TextView_Distance = (TextView) findViewById(R.id.textView_Distance);
        TextView_Direction = (TextView) findViewById(R.id.textView_Direction);

        // Buttonの設定(Layoutにて設定したものを関連付け)
        backButton = (Button) findViewById(R.id.back_button);
        // ボタンのイベント設定
        backButton.setOnClickListener(this);

        mInputTextView = (TextView) findViewById(R.id.inputValueTextView);

        //JoyStick設定
        js = new JoyStickClass(getApplicationContext(), layout_joystick, R.drawable.joystick_black);
        js.setStickSize(150, 150);
        js.setLayoutSize(500, 500);
        js.setLayoutAlpha(150);
        js.setStickAlpha(300);
        js.setOffset(90);
        js.setMinimumDistance(50);

        mThread = new Thread(th);

        //モーターの回転数やThrottle_Lever_image_viewの座標位置を表示するtextViewと関連付け
        Kaitensu_textView = findViewById(R.id.Kaitensu_TextView);

        Throttle_Lever = this.findViewById(R.id.Throttle_Lever_image_view);
        Throttle_Lever.setOnTouchListener(this);

        // Bluetoothのデバイス名を取得
        // デバイス名は、Drone_ESP32になるため、
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

        /**
         * Joy Stick関連の処理
         */
        layout_joystick.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("SetTextI18n")
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js.drawStick(arg1);
                if(arg1.getAction() == MotionEvent.ACTION_DOWN || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    TextView_X.setText("X : " + String.valueOf(js.getX()));
                    TextView_Y.setText("Y : " + String.valueOf(js.getY()));
                    TextView_Angle.setText("Angle : " + String.valueOf(js.getAngle()));
                    TextView_Distance.setText("Distance : " + String.valueOf(js.getDistance()));

                    int direction = js.get8Direction();
                    if(direction == JoyStickClass.STICK_UP) {
                        TextView_Direction.setText("Direction : Up");
                    } else if(direction == JoyStickClass.STICK_UPRIGHT) {
                        TextView_Direction.setText("Direction : Up Right");
                    } else if(direction == JoyStickClass.STICK_RIGHT) {
                        TextView_Direction.setText("Direction : Right");
                    } else if(direction == JoyStickClass.STICK_DOWNRIGHT) {
                        TextView_Direction.setText("Direction : Down Right");
                    } else if(direction == JoyStickClass.STICK_DOWN) {
                        TextView_Direction.setText("Direction : Down");
                    } else if(direction == JoyStickClass.STICK_DOWNLEFT) {
                        TextView_Direction.setText("Direction : Down Left");
                    } else if(direction == JoyStickClass.STICK_LEFT) {
                        TextView_Direction.setText("Direction : Left");
                    } else if(direction == JoyStickClass.STICK_UPLEFT) {
                        TextView_Direction.setText("Direction : Up Left");
                    } else if(direction == JoyStickClass.STICK_NONE) {
                        TextView_Direction.setText("Direction : Center");
                    }
                } else if(arg1.getAction() == MotionEvent.ACTION_UP) {
                    TextView_X.setText("X :");
                    TextView_Y.setText("Y :");
                    TextView_Angle.setText("Angle :");
                    TextView_Distance.setText("Distance :");
                    TextView_Direction.setText("Direction :");
                }
                return true;
            }
        });
        //Bluetooth接続
        bluetooth_connect();
    }

    //ダイアログでボタンが押されたときに動作するコールバック関数を定義
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        //Throttle_Lever_image_viewの位置を指定
        Throttle_Lever.layout(1500, 800, 1770, 1009);
        //モーターの回転数をリセット
        Kaitensu_count = 0;
        //Bluetooth再接続
        bluetooth_connect();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        //Bluetooth再接続せずMainActivityに戻る
        //Throttle_Lever_image_viewの位置を指定
        Throttle_Lever.layout(1500, 800, 1770, 1009);
        //モーターの回転数をリセット
        Kaitensu_count = 0;
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDialogNeutralClick(DialogFragment dialog) {
        //ダイアログを閉じるための仮置き
    }

    //Bluetooth接続ダイアログ
    public void bluetooth_connect(){
        message = "接続先: " + DEVICE_NAME + "\n接続中．．．";
        if (!connectFlg && Transition_Flag) {
            //ESP32と接続中ダイアログを表示
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
        //Throttle_Lever_image_viewの位置を指定
        Throttle_Lever.layout(1500, 800, 1770, 1009);
        Kaitensu_count = 0;
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
        }
        catch(Exception e){

        }
    }

    //ESP32と接続し、データの送受信を処理するためのスレッド処理
    class ThreadSleep1Second extends Thread implements Runnable{
        @Override
        public void run(){
            InputStream mmInStream = null;
            //Handler mainThreadHandler = new Handler(Looper.getMainLooper());
            try {

                /*追記*/
                if (ContextCompat.checkSelfPermission(OperationActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    {
                        ActivityCompat.requestPermissions(OperationActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
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
                if (connectFlg) {//接続できたらESP32に'connect!'を送信
                    try {
                        mmOutputStream.write("controller connect!:0".getBytes());
                        errorCount = 0;
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
                        if(errorCount >= 4){
                            //ダイアログが合計6回表示されても接続できなかった場合、エラーダイアログを表示する
                            //ダイアログフラグメントのオブジェクトを生成
                            DialogFragment_A dialogFragment = new DialogFragment_A();
                            //エラーダイアログの表示
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

    //回転数(Throttle_Lever)操作
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
                int dx = 1500 ; //dx(横軸)を固定し、縦方向にしか動かないようにする。
                int dy = Throttle_Lever.getTop() + (newDy - preDy);//横軸
                int imgW = dx + Throttle_Lever.getWidth();//画像サイズの横軸
                int imgH = dy + Throttle_Lever.getHeight();//画像サイスの縦軸

                //Throttle_Lever_image_viewの位置を設定する
                //Throttle_Leverの座標の範囲を設定し、画面外にいかないようにする
                if(dy < 100){//y座標が100を下回ると位置を固定しそれ以上いかないようにする
                    Throttle_Lever.layout(dx, 100, 1770, 309);
                    break;
                }else if(dy > 800){//y座標が800を超えると位置を固定しそれ以上いかないようにする
                    Throttle_Lever.layout(dx, 800, 1770, 1009);
                    break;
                }else{//y座標が100～800の間は自由に動かせる
                    Throttle_Lever.layout(dx, dy, imgW, imgH);
                }
                //Throttle_Lever.layout(dx, dy, imgW, imgH); 1.42857143
                Kaitensu_count = (int) ((800 - dy)*1.42857143 + 1000);

                //dxやdy、回転数等をTextviewに表示する
                String str = "回転数\ndx="+dx+"\ndy="+dy+"\nimgW="+imgW+"\nimgH="+imgH+"\ncount="+Kaitensu_count;
                Kaitensu_textView.setText(str);

                // 接続中のみ書込みを行う
                if (connectFlg) {
                    try {
                        //回転数を送信
                        String count_string = String.valueOf(Kaitensu_count) +":1";
                        mmOutputStream.write(count_string.getBytes());
                    } catch (IOException e) {

                    }
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
        //「切断」ボタンを押したときの処理
        if(v.equals(backButton)) {
            Transition_Flag = false;
            if (connectFlg) {
                try {
                    // 切断ボタン押下時、'切断:5'を送信
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
            //MainActivityに移動する
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }
    @Override
    public void onBackPressed(){
        // androidのバックボタンを押したときの処理(何も書いていないので、バックボタンは機能しない)
    }

    //ESP32からデータを受信したときの処理
    public void zyusin(String msgStr){

        result_hello = msgStr.indexOf(hello_check);
        result_connect = msgStr.indexOf(connect_check);
        result_standby = msgStr.indexOf(standby_check);

        if(result_hello != -1){//受信データに「Ho?(ESP32との接続確認のために送られてくる文字列)」が含まれている時の処理
            mInputTextView.setText(msgStr);
            if (connectFlg) {//接続されていたらESP32に「Hello!:2」を返す
                try {
                    mmOutputStream.write("Hello!:2".getBytes());
                } catch (IOException e) {

                }
            }
        }else if(result_connect != -1 || result_standby != -1){//受信データに「Co(ESP32と接続した際に送られてくる文字列)」か「St(ESP32の初回セットアップが完了した際に送られてくる文字列)」が含まれている時の処理
            mInputTextView.setText(msgStr);
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
            if(action == VIEW_INPUT){
                //ESP32からデータを受信した時、zyusin(483行目)に遷移する
                zyusin(msgStr);
            }
        }
    };
}