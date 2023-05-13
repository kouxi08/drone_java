package jp.ac.ecc.ie3b03.my_drone_java;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

//public class MainActivity extends ActionBarActivity implements Runnable, View.OnClickListener {
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    /**
     * 接続ボタン.
     */
    private Button connectButton;

    private Button TestConnectButton;

    private Button OptionButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Layoutにて設定したビューを表示
        setContentView(R.layout.activity_main);

        //アクションバー(タイトルバー)非表示
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        // Buttonの設定(Layoutにて設定したものを関連付け)
        connectButton = (Button)findViewById(R.id.connectButton);
        TestConnectButton = (Button)findViewById(R.id.Test_Connect_button);
        OptionButton = (Button)findViewById(R.id.Option_button);

        // ボタンのイベント設定
        connectButton.setOnClickListener(this);
        TestConnectButton.setOnClickListener(this);
        OptionButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(connectButton)) {
            Intent intent = new Intent(this, OperationActivity.class);
            intent.putExtra("Transition_Flag",true);
            startActivity(intent);
        } else if(v.equals(TestConnectButton)) {
            Intent intent = new Intent(this, SubActivity.class);
            intent.putExtra("Transition_Flag",true);
            startActivity(intent);
        }
        else if(v.equals(OptionButton)) {
            Intent intent = new Intent(this, OptionActivity.class);
            startActivity(intent);
        }
    }
}
