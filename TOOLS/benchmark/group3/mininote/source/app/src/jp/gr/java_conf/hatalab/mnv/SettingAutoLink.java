package jp.gr.java_conf.hatalab.mnv;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class SettingAutoLink extends PreferenceActivity{

	private boolean autoLinkWeb = false;
	private boolean autoLinkEmail = false;
	private boolean autoLinkTel = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting_autolink);

        //現在の設定を読み込む
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        autoLinkWeb  = sharedPreferences.getBoolean(getText(R.string.prefAutoLinkWebKey).toString(),   false);
        autoLinkEmail= sharedPreferences.getBoolean(getText(R.string.prefAutoLinkEmailKey).toString(), false);
        autoLinkTel  = sharedPreferences.getBoolean(getText(R.string.prefAutoLinkTelKey).toString(),   false);

        //===========================
        //Auto Link Web URL
        //===========================
        CharSequence autoLinkWebKey = getText(R.string.prefAutoLinkWebKey);
        CheckBoxPreference autoLinkWebCheckBox = (CheckBoxPreference)findPreference(autoLinkWebKey);

        autoLinkWebCheckBox.setTitle(R.string.prefAutoLinkWeb);//Title
        autoLinkWebCheckBox.setSummary(R.string.prefAutoLinkWebSummary); //Summary
        autoLinkWebCheckBox.setChecked(autoLinkWeb);   //update check status

        //===========================
        //Auto Link Email
        //===========================
        CharSequence autoLinkEmailKey = getText(R.string.prefAutoLinkEmailKey);
        CheckBoxPreference autoLinkEmailCheckBox = (CheckBoxPreference)findPreference(autoLinkEmailKey);

        autoLinkEmailCheckBox.setTitle(R.string.prefAutoLinkEmail);//Title
        autoLinkEmailCheckBox.setSummary(R.string.prefAutoLinkEmailSummary); //Summary
        autoLinkEmailCheckBox.setChecked(autoLinkEmail);   //update check status

        //===========================
        //Auto Link Tel
        //===========================
        CharSequence autoLinkTelKey = getText(R.string.prefAutoLinkTelKey);
        CheckBoxPreference autoLinkTelCheckBox = (CheckBoxPreference)findPreference(autoLinkTelKey);

        autoLinkTelCheckBox.setTitle(R.string.prefAutoLinkTel);//Title
        autoLinkTelCheckBox.setSummary(R.string.prefAutoLinkTelSummary); //Summary
        autoLinkTelCheckBox.setChecked(autoLinkTel);   //update check status

    }
    
    // キーイベント発生時、呼び出されます
    //ここでKEYCODE_BACKを補足しておかないと、Settingsが終わった後にMainActivityがKEYCODE_BACKをキャッチしてしまい、
    //
    private boolean mBackKeyDown = false;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(event.getKeyCode()){
    		case KeyEvent.KEYCODE_BACK:
    			//back keyは無視するACTION_UPの時に処理をする。
    				mBackKeyDown = true;
    				return true;
    			//break;
    		default :
    			mBackKeyDown = false;
    			break;
    		}
    	}

        if (event.getAction() == KeyEvent.ACTION_UP) { // キーが離された時
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK: // BACK KEY
            	if(mBackKeyDown){
            		mBackKeyDown = false;//戻しておく
            		finish();
            	}
                return true;
                
            default:
        		mBackKeyDown = false;
        		break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

}
