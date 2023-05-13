package jp.ac.ecc.ie3b03.my_drone_java;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

public class DialogFragment_A extends DialogFragment {

    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
        public void onDialogNeutralClick(DialogFragment dialog);
    }

    NoticeDialogListener listener;

    //フラグメントができたときにlistenerをインスタンス化
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (NoticeDialogListener) context;

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //ダイアログを作るためのビルダーを作成
        //ビルダーでタイトルやメッセージを設定できる
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //ダイアログのタイトル文を設定
        builder.setTitle("エラー");

        //ダイアログのメッセージ文を設定
        builder.setMessage("接続に失敗しました。\n再接続しますか？");

        DialogClickListener listener = new DialogClickListener();

        //Positive ボタンに表示内容と、リスナを設定
        builder.setPositiveButton("再接続",listener);

        //Negative ボタンに表示内容と、リスナを設定
        builder.setNegativeButton("NO",listener);

        //Neutral ボタンに表示内容と、リスナを設定
        builder.setNeutralButton("ダイアログを閉じる(仮)" ,listener);

        //Neutral ボタンに表示内容と、リスナを設定
        //builder.setNeutralButton(R.string.dialog_button_neutral,listener);

        //設定したダイアログを作成
        AlertDialog dialog = builder.create();
        setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        //ダイアログをリターン
        return dialog;
    }

    private class DialogClickListener implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialog,int buttonId){

            //switchにてタップされたボタンでの条件分岐を行う
            switch(buttonId){
                //OKボタンがタップされたとき
                case DialogInterface.BUTTON_POSITIVE:
                    //OKボタンが押されたときのメソッドを呼び出し
                    //処理は継承先のMainActivityで実行
                    listener.onDialogPositiveClick(DialogFragment_A.this);
                    break;
                //Cancelボタンがタップされたとき
                case DialogInterface.BUTTON_NEGATIVE:
                    //Cancelボタンが押されたときのメソッドを呼び出し
                    //処理は継承先のMainActivityで実行
                    listener.onDialogNegativeClick(DialogFragment_A.this);
                    break;
                //Neutralボタンがタップされたとき
                case DialogInterface.BUTTON_NEUTRAL:
                    //Neutralボタンが押されたときのメソッドを呼び出し
                    //処理は継承先のMainActivityで実行
                    listener.onDialogNeutralClick(DialogFragment_A.this);
                    break;
            }
        }
    }
}
