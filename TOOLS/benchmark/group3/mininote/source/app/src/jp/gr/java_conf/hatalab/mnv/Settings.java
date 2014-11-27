package jp.gr.java_conf.hatalab.mnv;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends PreferenceActivity{
	
	
	
    private static final int SELECT_DIR_ACTIVITY = 1; 
    private static final int TEMPLATE_TEXT_ACTIVITY = 2;
    private static final int SETTING_AUTO_LINK_ACTIVITY = 3;
    
    private PreferenceScreen mInitDirScreen;
    private PreferenceScreen mTemplateTextScreen;
    private ListPreference mPasswordTimerListScreen;
    private ListPreference mCharsetListScreen;
    private ListPreference mLinebreakListScreen;
    private CheckBoxPreference syncTitleCheckBox;
    private CheckBoxPreference showButtonsCheckBox;
    private CheckBoxPreference viewerModeCheckBox;
    private PreferenceScreen mFontSizePref;
    private CheckBoxPreference listFoldersFirstCheckBox;
    private CheckBoxPreference mAutoSaveCheckBox;
    private ListPreference mTypefaceListScreen;
    private PreferenceScreen mAutoLinkScreen;
    private CheckBoxPreference mNoTitleBarCheckBox;
    private PreferenceScreen mFontSizeOnListPref;
    private PreferenceScreen mDefaultFolderNamePref;

    
    private String initDir= "/sdcard";
//    private String initDirPrefix= "folder:";

    private String PWTimer= "3";
    private String charsetName = "utf-8";
    private String lineBreak = "auto";

    private Boolean syncTitleFlag = false;
    private Boolean showButtonsFlag = true;
    private Boolean viewerModeFlag  = false;
//    private String fontSizeString = "14";//maybe default is 14 sp.
    private float fontSize = 18; //maybe default is 18 sp?
    private Boolean listFoldersFirstFlag = false;
    private Boolean autoSaveFlag = false;

    private String typeface = "default";

    private Boolean noTitleBarFlag = false;

    private float fontSizeOnList = 24; //maybe default is 18 sp?

    private myTemplateText defaultFolderName;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        
        Config.update(this);
        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        initDir = Config.getInitDirName();
        //initDir = sharedPreferences.getString(getText(R.string.prefInitDirKey).toString(), initDir);
        PWTimer = Config.getPWTimer();
        //PWTimer = sharedPreferences.getString(getText(R.string.prefPWResetTimerKey).toString(), "3");
        //character set
        charsetName = Config.getCharsetName();
        //charsetName = sharedPreferences.getString(getText(R.string.prefCharsetNameKey).toString(), "utf-8");

        // flag of sync title
        syncTitleFlag = Config.getSyncTitleFlag();
        //syncTitleFlag = sharedPreferences.getBoolean(getText(R.string.prefSyncTitleKey).toString(), false);
        // flag of showing buttons
        showButtonsFlag = Config.getShowButtonsFlag();
        //showButtonsFlag = sharedPreferences.getBoolean(getText(R.string.prefShowButtonsKey).toString(), true);
        // flag of viewerMode
        viewerModeFlag = Config.getViewerModeFlag();
        //viewerModeFlag = sharedPreferences.getBoolean(getText(R.string.prefViewerModeKey).toString(), false);
        // fontSize
        fontSize = Config.getFontSize();
        //fontSize = sharedPreferences.getFloat(getText(R.string.prefFontSizeKey).toString(), fontSize);

        //line break
        lineBreak = Config.getLineBreak();
        //lineBreak = sharedPreferences.getString(getText(R.string.prefLineBreakCodeKey).toString(), "auto");

        // flag of list Folder first
        listFoldersFirstFlag = Config.getListFoldersFirstFlag();
        //listFoldersFirstFlag = sharedPreferences.getBoolean(getText(R.string.prefListFoldersFirstKey).toString(), false);
        
        // flag of auto save mode
        autoSaveFlag = Config.getAutoSaveFlag();
        //autoSaveFlag = sharedPreferences.getBoolean(getText(R.string.prefAutoSaveKey).toString(), false);

        //type face
        typeface = Config.getTypeface();
        //typeface = sharedPreferences.getString(getText(R.string.prefTypefaceKey).toString(), "DEFAULT");

        // flag of auto save mode
        noTitleBarFlag = Config.getNoTitleBarFlag();
        //noTitleBarFlag = sharedPreferences.getBoolean(getText(R.string.prefNoTitleBarKey).toString(), false);

        //Font size on List View
        fontSizeOnList = Config.getFontSizeOnList();

        //Font size on List View
        defaultFolderName = Config.getDefaultFolderName();

        //===========================
        //initDirScreen
        //===========================
        CharSequence csScreenPref3 = getText(R.string.prefInitDirKey);
        mInitDirScreen = (PreferenceScreen)findPreference(csScreenPref3);
        mInitDirScreen.setOnPreferenceClickListener(new OnPreferenceClickListener(){
//            @Override
            public boolean onPreferenceClick(Preference pref) {
                return onPreferenceClick_setInitDir(pref);
            }});
        mInitDirScreen.setSummary(getText(R.string.prefInitDirSummary) + ": " +initDir);
        
        //===========================
        //Password Timer
        //===========================
        CharSequence listScreen = getText(R.string.prefPWResetTimerKey);
        mPasswordTimerListScreen = (ListPreference)findPreference(listScreen);
//        mPasswordTimerListScreen.setSummary(PWTimer + " minites");
      mPasswordTimerListScreen.setSummary(PWTimer + " " +getText(R.string.prefPWResetTimerSummary));
        mPasswordTimerListScreen.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
        	//@Override
        	public boolean onPreferenceChange(Preference pref, Object val) {
        		String newval = val.toString();
        		mPasswordTimerListScreen.setSummary(newval + " " +getText(R.string.prefPWResetTimerSummary));
        		
        		return true;
        	}
        });

        //===========================
        //PreferenceList(charset)
        //===========================
        CharSequence charsetListScreen = getText(R.string.prefCharsetNameKey);
        mCharsetListScreen = (ListPreference)findPreference(charsetListScreen);
        mCharsetListScreen.setSummary(charsetName);
        mCharsetListScreen.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
        	//@Override
        	public boolean onPreferenceChange(Preference pref, Object val) {
        		String newval = val.toString();
        		mCharsetListScreen.setSummary(newval);
        		
        		return true;
        	}
        });

        //===========================
        //PreferenceList(linebreak)
        //===========================
        CharSequence linebreakListScreen = getText(R.string.prefLineBreakCodeKey);
        mLinebreakListScreen = (ListPreference)findPreference(linebreakListScreen);
        final CharSequence[] entries = mLinebreakListScreen.getEntries();
        final CharSequence[] entryValues = mLinebreakListScreen.getEntryValues();


		String currentEntry = lineBreak;
		for(int i= 0;i<entryValues.length;i++){
			if(entryValues[i].toString().equals(lineBreak)){
				currentEntry = entries[i].toString();
				break;
			}
		}
        mLinebreakListScreen.setSummary(currentEntry);
        
        mLinebreakListScreen.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
        	//@Override
        	public boolean onPreferenceChange(Preference pref, Object val) {
//        		String newval = val.toString();
//        		mLinebreakListScreen.setSummary(newval);
        		
        		String newval = val.toString();
        		String newEntry = newval;
        		for(int i= 0;i<entryValues.length;i++){
        			if(entryValues[i].toString().equals(newval)){
        				newEntry = entries[i].toString();
        				break;
        			}
        		}
        		mLinebreakListScreen.setSummary(newEntry);
        		
        		return true;
        	}
        });

        
        
        //===========================
        // SyncTitle
        //===========================
        CharSequence syncTitleKey = getText(R.string.prefSyncTitleKey);
        syncTitleCheckBox = (CheckBoxPreference)findPreference(syncTitleKey);
//        syncTitleCheckBox = new CheckBoxPreference(this);  

//        syncTitleCheckBox.setKey(getString(R.string.prefSyncTitleKey));  
        syncTitleCheckBox.setTitle(R.string.prefSyncTitle);  

        syncTitleCheckBox.setSummaryOn(R.string.prefSyncTitleSummaryOn);  
        syncTitleCheckBox.setSummaryOff(R.string.prefSyncTitleSummaryOff);  

        syncTitleCheckBox.setChecked(syncTitleFlag);  
        

        //===========================
        //show bottom bar(close/menu button)
        //===========================
        CharSequence showButtonsKey = getText(R.string.prefShowButtonsKey);
        showButtonsCheckBox = (CheckBoxPreference)findPreference(showButtonsKey);

//        ShowBarOnEditBoxCheckBox.setKey(getString(R.string.prefShowBarOnEditBoxKey));  
        showButtonsCheckBox.setTitle(R.string.prefShowButtons);  

        showButtonsCheckBox.setSummaryOn(R.string.prefShowButtonsSummaryOn);  
        showButtonsCheckBox.setSummaryOff(R.string.prefShowButtonsSummaryOff);  

        showButtonsCheckBox.setChecked(showButtonsFlag);  

        //===========================
        //viewer mode
        //===========================
        CharSequence viewerModeKey = getText(R.string.prefViewerModeKey);
        viewerModeCheckBox = (CheckBoxPreference)findPreference(viewerModeKey);

//        ShowBarOnEditBoxCheckBox.setKey(getString(R.string.prefShowBarOnEditBoxKey));  
        // Title
        viewerModeCheckBox.setTitle(R.string.prefViewerMode);  
        viewerModeCheckBox.setSummary(R.string.prefViewerModeSummary);  
        viewerModeCheckBox.setChecked(viewerModeFlag);  

        
        //===========================
        //Font Size
        //===========================
        CharSequence fontSizeKey = getText(R.string.prefFontSizeKey);
        mFontSizePref = (PreferenceScreen)findPreference(fontSizeKey);
        mFontSizePref.setSummary(getText(R.string.prefFontSizeSummary)+ ": " + Float.toString(fontSize) + " sp");

        //Log.d("FontSize ",getText(R.string.prefFontSizeSummary)+ Float.toString(fontSize) + " sp");

        mFontSizePref.setOnPreferenceClickListener(new OnPreferenceClickListener(){
//            @Override
            public boolean onPreferenceClick(Preference pref) {
                return onPreferenceClick_setFontSize(pref);
            }});

        //===========================
        //list folders first
        //===========================
        CharSequence listFoldersFirstKey = getText(R.string.prefListFoldersFirstKey);
        listFoldersFirstCheckBox = (CheckBoxPreference)findPreference(listFoldersFirstKey);

//        ShowBarOnEditBoxCheckBox.setKey(getString(R.string.prefShowBarOnEditBoxKey));  
        listFoldersFirstCheckBox.setTitle(R.string.prefListFoldersFirst); 

        listFoldersFirstCheckBox.setChecked(listFoldersFirstFlag);  

        
        //===========================
        //Auto save mode
        //===========================
        CharSequence autoSaveKey = getText(R.string.prefAutoSaveKey);
        mAutoSaveCheckBox = (CheckBoxPreference)findPreference(autoSaveKey);

//        ShowBarOnEditBoxCheckBox.setKey(getString(R.string.prefShowBarOnEditBoxKey));  
        mAutoSaveCheckBox.setTitle(R.string.prefAutoSave); 

        mAutoSaveCheckBox.setChecked(autoSaveFlag);  
        
        //===========================
        //PreferenceList(Typeface)
        //===========================
        CharSequence typefaceListScreen = getText(R.string.prefTypefaceKey);
        mTypefaceListScreen = (ListPreference)findPreference(typefaceListScreen);
        final CharSequence[] tf_entries = mTypefaceListScreen.getEntries();
        final CharSequence[] tf_entryValues = mTypefaceListScreen.getEntryValues();


		String tf_currentEntry = typeface;
		for(int i= 0;i<tf_entryValues.length;i++){
			if(tf_entryValues[i].toString().equals(typeface)){
				tf_currentEntry = tf_entries[i].toString();
				break;
			}
		}
        mTypefaceListScreen.setSummary(tf_currentEntry);
        
        mTypefaceListScreen.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
        	//@Override
        	public boolean onPreferenceChange(Preference pref, Object val) {
//        		String newval = val.toString();
//        		mLinebreakListScreen.setSummary(newval);
        		
        		String newval = val.toString();
        		String newEntry = newval;
        		for(int i= 0;i<tf_entryValues.length;i++){
        			if(tf_entryValues[i].toString().equals(newval)){
        				newEntry = tf_entries[i].toString();
        				break;
        			}
        		}
        		mTypefaceListScreen.setSummary(newEntry);
        		
        		return true;
        	}
        });
        
        //===========================
        //TemplateTextScreen
        //===========================
        CharSequence key = getText(R.string.prefTemplateTextKeyPrefix);
        mTemplateTextScreen = (PreferenceScreen)findPreference(key);
        mTemplateTextScreen.setOnPreferenceClickListener(new OnPreferenceClickListener(){
        	//            @Override
        	public boolean onPreferenceClick(Preference pref) {
        		return onPreferenceClick_setTemplateText(pref);
        	}
        });

   
        //===========================
        //autoLinkScreen
        //===========================
        CharSequence csAutoLinkPref = getText(R.string.prefAutoLinkKey);
        
        mAutoLinkScreen = (PreferenceScreen)findPreference(csAutoLinkPref);
        mAutoLinkScreen.setOnPreferenceClickListener(new OnPreferenceClickListener(){
//            @Override
        	public boolean onPreferenceClick(Preference pref) {
        		return onPreferenceClick_setAutoLink(pref);
        	}}
        );
//        mInitDirScreen.setSummary(initDirPrefix+initDir);

        
        //===========================
        //No Title Bar mode
        //===========================
        CharSequence noTitleBarKey = getText(R.string.prefNoTitleBarKey);
        mNoTitleBarCheckBox = (CheckBoxPreference)findPreference(noTitleBarKey);

        mNoTitleBarCheckBox.setTitle(R.string.prefNoTitleBar); 

        mNoTitleBarCheckBox.setChecked(noTitleBarFlag); 

        
        //===========================
        //Font Size On List View
        //===========================
        CharSequence fontSizeOnListKey = getText(R.string.prefFontSizeOnListKey);
        mFontSizeOnListPref = (PreferenceScreen)findPreference(fontSizeOnListKey);
        mFontSizeOnListPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){
//            @Override
            public boolean onPreferenceClick(Preference pref) {
                return onPreferenceClick_setFontSizeOnList(pref);
            }});
        mFontSizeOnListPref.setSummary(getText(R.string.prefFontSizeOnListSummary)+ ": " + Float.toString(fontSizeOnList) + " sp");
        
        
        
        //===========================
        //default folder name
        //===========================
        CharSequence defaultFolderNameKey = getText(R.string.prefDefaultFolderNameKey);
        mDefaultFolderNamePref = (PreferenceScreen)findPreference(defaultFolderNameKey);
        mDefaultFolderNamePref.setSummary(getText(R.string.prefDefaultFolderNameSummary) + " " + defaultFolderName.getText());

        mDefaultFolderNamePref.setOnPreferenceClickListener(new OnPreferenceClickListener(){
//            @Override
            public boolean onPreferenceClick(Preference pref) {
                return onPreferenceClick_setDefaultFolderName(pref);
            }});

    }

    private boolean onPreferenceClick_setDefaultFolderName(final Preference pref){    	
    	boolean isTimeFormat = defaultFolderName.isTimeFormat();
    	String text = defaultFolderName.getText();
    	
    	
		//コンテキストからインフレータを取得
		LayoutInflater inflater = LayoutInflater.from(this);
		//レイアウトXMLからビュー(レイアウト)をインフレート
		final View inputView = inflater.inflate(R.layout.edit_template_text, null);

		final EditText nameEditText = (EditText)inputView.findViewById(R.id.dialog_edittext);
		nameEditText.setText(text);
		String defaultFolerNameHint = "Folder-%03d";
		nameEditText.setHint(defaultFolerNameHint);
		

		final CheckBox timeformatCheck = (CheckBox)inputView.findViewById(R.id.timeformat_checkbox);
		timeformatCheck.setChecked(isTimeFormat);

		final Button sampleButton = (Button)inputView.findViewById(R.id.sampleButton);
		if(!isTimeFormat && sampleButton.getVisibility() == View.VISIBLE)sampleButton.setVisibility(View.INVISIBLE);
		
		//checkボックスのクリックリスナー
		timeformatCheck.setOnClickListener(new View.OnClickListener(){
//			@Override
			public void onClick(View v) {
				if(timeformatCheck.isChecked() == true) {
					// チェックされた状態の時の処理を記述
					if(sampleButton.getVisibility() == View.INVISIBLE)
						sampleButton.setVisibility(View.VISIBLE);
				}
				else {
					// チェックされていない状態の時の処理を記述
					if(sampleButton.getVisibility() == View.VISIBLE)
						sampleButton.setVisibility(View.INVISIBLE);
				}
			}
		});

		//ボタンのクリックリスナー
		sampleButton.setOnClickListener(new View.OnClickListener() {
			//		@Override
			public void onClick(View v) {
				//ボタンを押したときの動作
				getTimeFormatSample(nameEditText);
			}

		});
		
		nameEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
//	        @Override
	        public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
	            if (hasFocus) {
	                inputMethodManager.showSoftInput(v, 0);//.showSoftInput(v.getWindowToken(),0);
//	                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	            
	            }else{
	                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(),0);
	            }
	        }
	    });

		
		
		final AlertDialog alertDialog  = new AlertDialog.Builder(this)
		.setTitle(R.string.edit_template_title)
//		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			//		    @Override
			public void onClick(DialogInterface dialog, int which) {
				//OKボタンが押下された時に入力された文字を設定する
				String text = nameEditText.getText().toString();
				boolean check = timeformatCheck.isChecked();
//				name = name.replaceAll("^[\\s　]*$", "");//空白だけ
				
				if(text.length()>0){
					//check Time format
					if(check){
						try{
							//Date inDate = new Date();
							//SimpleDateFormat sdf = new SimpleDateFormat(text);
							//String formated text = sdf.format(inDate);

							
							myTemplateText testText = new myTemplateText(text,check);
							testText.setWithNumber(true);
							testText.toString(0);
							
							//OK if no exception 
							setDefaultFolderName(text, check);
						}catch(Exception e){
							//Toast.makeText(TemplateTextList.this, e.toString(), Toast.LENGTH_LONG).show();
							onPreferenceClick_setDefaultFolderName(pref);
							e.printStackTrace();
							MyUtil.showMessage(e.toString(), Settings.this);
						}
					}else{
						setDefaultFolderName(text, check);						
					}
					
					
				}else{
					//nameが空だったら終了する
					//	finish();
					//Toast.makeText(Settings.this, "Input text is empty.", Toast.LENGTH_SHORT).show();
					Toast.makeText(Settings.this, R.string.settings_input_text_empty, Toast.LENGTH_SHORT).show();
					onPreferenceClick_setDefaultFolderName(pref);
				}

			}
		})
		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//do nothing
			}
		})
		.setOnCancelListener(new DialogInterface.OnCancelListener() {
//			  @Override
			public void onCancel(DialogInterface dialog) {
//				Log.v("TextEdit","onCancel");
				// キャンセルボタンと戻るボタンが押された時の処理をここに記述する。
				//do nothing
			}
		})
		.setView(inputView)
		.create();
		
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);


		alertDialog.show(); //ダイアログ表示

        
        return true;
    }
    
	final private String[] format_sample = {
			"yy-MM-dd",
			"'Folder'yyyyMMdd",
			"'Folder-'yyyy-MM-dd'.%02d'",
			"'Folder-'yyyy-MM-dd'.%05d'"
	};
	
	private void getTimeFormatSample(final EditText editView){

		CharSequence[] menu = new CharSequence[format_sample.length];
//		Date inDate = new Date();		
		for(int i=0; i < menu.length; i++){
//			SimpleDateFormat sdf = new SimpleDateFormat(format_sample[i]);
			myTemplateText temp = new myTemplateText(format_sample[i],true);
			temp.setWithNumber(true);
			menu[i] = temp.toString(0);
		}
		
		
		
		new AlertDialog.Builder(this)
		.setTitle(R.string.text_timeformat_example)
		.setItems(menu, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int item)
			{
				////insert sample format
				//int st = editView.getSelectionStart();
				//int en = editView.getSelectionEnd();
				//if(st > en){int tmp = st;st = en;en = tmp;}
				//editView.getText().replace(st, en, format_sample[item]);
				
				//replace to sample format
				editView.setText(format_sample[item]);
//				Toast.makeText(getApplicationContext(), "clicked:" + menu[item].toString(), Toast.LENGTH_SHORT).show();
				
			}
		})
		.show();

		
	}
	
    private void setDefaultFolderName(String name, boolean isTimeFormat){
    	defaultFolderName.setText(name);
    	defaultFolderName.setTimeFormat(isTimeFormat);
    	defaultFolderName.setWithNumber(true); //always true;
 
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Editor ed = sp.edit();
        ed.putString(getText(R.string.prefDefaultFolderNameKey).toString(), defaultFolderName.getPrefString());
        ed.commit();
        mDefaultFolderNamePref.setSummary(getText(R.string.prefDefaultFolderNameSummary) + " " + defaultFolderName.getText());
    }
    
    private boolean onPreferenceClick_setFontSize(Preference pref){
		LayoutInflater inflater = LayoutInflater.from(this);
		final View setFontSizeView = inflater.inflate(R.layout.set_fontsize, null);
		final TextView textview = (TextView)setFontSizeView.findViewById(R.id.dialog_textview);

        float current_size = fontSize;
        final float offset = 5;
		final SeekBar  seekBar = (SeekBar)setFontSizeView.findViewById(R.id.seekbar);
        seekBar.setMax(43);//max is 43 + 5. "5" is offset
        seekBar.setProgress((int) (current_size - offset) );
		
        final String sampletext = getText(R.string.prefFontSizeSampleText).toString();
        textview.setText(sampletext + ": " + Float.toString (current_size));
        textview.setTextSize(TypedValue.COMPLEX_UNIT_SP,current_size);
        
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        	float size;
            //@Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Log.v("onStartTrackingTouch()",String.valueOf(seekBar.getProgress()));
            }
            //@Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
//                Log.v("onProgressChanged()", String.valueOf(progress) + ", " + String.valueOf(fromTouch));
                size = progress + offset;
                textview.setText(sampletext + ": " + Float.toString (size));
                textview.setTextSize(TypedValue.COMPLEX_UNIT_SP,size);
            }
            //@Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Log.v("onStopTrackingTouch()",String.valueOf(seekBar.getProgress()));
            }
        });
		
		
		final AlertDialog alertDialog  = new AlertDialog.Builder(this)
		.setTitle(R.string.prefFontSize)
		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			//		    @Override
			public void onClick(DialogInterface dialog, int which) {
				
				float size = seekBar.getProgress() + offset;
            	setFontSize(size);

			}
		}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//Do Nothing
			}
		})
		.setView(setFontSizeView)
		.create();


		alertDialog.show();

        
        return true;
    }
    
    private void setFontSize(float size){
    	fontSize = size;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Editor ed = sp.edit();
        ed.putFloat(getText(R.string.prefFontSizeKey).toString(), fontSize);
        ed.commit();

        mFontSizePref.setSummary(getText(R.string.prefFontSizeSummary)+ ": "+Float.toString(fontSize) + " sp");

    }
    

    
    private boolean onPreferenceClick_setFontSizeOnList(Preference pref){
		LayoutInflater inflater = LayoutInflater.from(this);

		final View setFontSizeView = inflater.inflate(R.layout.set_fontsize, null);
		final TextView textview = (TextView)setFontSizeView.findViewById(R.id.dialog_textview);

        float current_size = fontSizeOnList;
        final float offset = 5;
		final SeekBar  seekBar = (SeekBar)setFontSizeView.findViewById(R.id.seekbar);
        seekBar.setMax(43);//max is 43 + 5. "5" is offset
        seekBar.setProgress((int) (current_size - offset) );
		
        final String sampletext = getText(R.string.prefFontSizeOnListSampleText).toString();
        textview.setText(sampletext + ": " + Float.toString (current_size));
        textview.setTextSize(TypedValue.COMPLEX_UNIT_SP,current_size);
        
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        	float size;
            //@Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Log.v("onStartTrackingTouch()",String.valueOf(seekBar.getProgress()));
            }
            //@Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
//                Log.v("onProgressChanged()", String.valueOf(progress) + ", " + String.valueOf(fromTouch));
                size = progress + offset;
                textview.setText(sampletext + ": " + Float.toString (size));
                textview.setTextSize(TypedValue.COMPLEX_UNIT_SP,size);
            }
            //@Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Log.v("onStopTrackingTouch()",String.valueOf(seekBar.getProgress()));
            }
        });
		
		
		final AlertDialog alertDialog  = new AlertDialog.Builder(this)
		.setTitle(R.string.prefFontSizeOnList)
		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			//		    @Override
			public void onClick(DialogInterface dialog, int which) {
				
				float size = seekBar.getProgress() + offset;
            	setFontSizeOnList(size);

			}
		}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//Do Nothing
			}
		})
		.setView(setFontSizeView)
		.create();


		alertDialog.show();

        
        return true;
    }
    
    private void setFontSizeOnList(float size){
    	fontSizeOnList = size;

    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Editor ed = sp.edit();
        ed.putFloat(getText(R.string.prefFontSizeOnListKey).toString(), fontSizeOnList);
        ed.commit();

        mFontSizeOnListPref.setSummary(getText(R.string.prefFontSizeOnListSummary)+ ": " +Float.toString(fontSizeOnList) + " sp");

    }

    
    
    private boolean onPreferenceClick_setInitDir(Preference pref){
    	
    	
        Intent intent = new Intent(this,SelectDirName.class);   
		intent.putExtra(SelectDirName.INTENT_FILEPATH, initDir);
        
        startActivityForResult(intent, SELECT_DIR_ACTIVITY);   
        return true;
    }

    private boolean onPreferenceClick_setAutoLink(Preference pref){	
        Intent intent = new Intent(this,SettingAutoLink.class);
        startActivityForResult(intent, SETTING_AUTO_LINK_ACTIVITY);   
        return true;
    }

    
    private boolean onPreferenceClick_setTemplateText(Preference pref){
        Intent intent = new Intent(this,TemplateTextList.class);
//		intent.putExtra(SelectDirName.INTENT_FILEPATH, initDir);
        
        startActivityForResult(intent, TEMPLATE_TEXT_ACTIVITY);   
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
//        Toast.makeText(this, "onActivityResult", Toast.LENGTH_SHORT).show();
  
        if (requestCode == SELECT_DIR_ACTIVITY){  

            if (resultCode == RESULT_OK){   
                Bundle extras = intent.getExtras(); 
                if (extras != null){   
                    initDir = extras.getString(SelectDirName.INTENT_DIRPATH);

                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                    Editor ed = sp.edit();
                    ed.putString(getText(R.string.prefInitDirKey).toString(), initDir);
                    ed.commit();
                }   
            }
            mInitDirScreen.setSummary(getText(R.string.prefInitDirSummary) + ": " +initDir);
        }
        else if (requestCode == TEMPLATE_TEXT_ACTIVITY){ 
        	// need to nothing.
        }
    } 
    
    
    //
    private boolean mBackKeyDown = false;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(event.getKeyCode()){
    		case KeyEvent.KEYCODE_BACK:
    			//back key　ACTION_UP
    				mBackKeyDown = true;
    				return true;
    			//break;
    		default :
    			mBackKeyDown = false;
    			break;
    		}
    	}

        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK: // BACK KEY
            	if(mBackKeyDown){
            		mBackKeyDown = false;
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
