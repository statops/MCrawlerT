package jp.gr.java_conf.hatalab.mnv;



import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.text.SimpleDateFormat;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;


public class TextEdit extends Activity {
	//ファイルに関する情報
	private String filepath = "/sdcard";//filenameが更新されたらaFile,isEncrypt,titleも更新する。
	private File aFile; //filepathが更新されたらaFileも更新する
	private boolean isEncrypt = false; //filepathが更新されたらこれも更新する。

	//for title bar
	private String mAppName = "";///getString(R.string.app_name);
	private String mFileNameOnTitleBar = "";
	private int    mCharaCount = 0;
	
	//起動直後のカーソル位置
	private int selStart = 0;
	private int selStop = 0;
	

	private String strText = "";
	private SpannableString spanText;
	private byte[] binText = null;
	private EditText edit;
//	private ScrollWrappableEditText edit;
	
	private String ErrorMessage = "";
	private ProgressDialog	progressDlg;
	final Handler notifyHandler = new Handler();

	private final String NewFileName = "NewFile.txt"; 
	
	
    private boolean ignoreOnResume = false; //ファイル選択activityからの返り時にonResume内の処理を無視するためのフラグ。onResumeよりonActivityResultが先に呼ばれるため
    private int mResetTimer = 3;//パスワードタイマーまたは一定時間経過後のonResume後、暗号化ファイルをcloseするためのタイマー
    private long mOnPauseTime = 0;//タイマーに使う時刻格納変数
    
    
    // Preferences
    private String charsetName = "UTF-8";
    private int lineBreakSetting = LINEBREAK_AUTO;
    private Typeface mTypeface = Typeface.DEFAULT;

    private boolean syncTitleFlag = false;
    private boolean showBottomBarFlag = true;
    private float   fontSize = 18;
    private boolean viewerModeFlag = false;
    private boolean autoSaveFlag = false;
    private boolean onPauseBeforeSelectSaveFile = false;
    
    private boolean needToRenameForSyncTitle = false;
    private boolean closeAfterSaveFlag = false;
    
    private ArrayList<myTemplateText> mTemplateList = null;

    private boolean autoLinkFlag = false;
    private boolean autoLinkWeb  = false;
    private boolean autoLinkEmail= false;
    private boolean autoLinkTel  = false;

    private boolean noTitleBarFlag = false;

    
    //メッセージが修正されたかどうかを確かめるためMessageDigestを取得する
    private String messageDigest;

    //Scroll中かどうかを示すフラグ
    private boolean onScrollFlag = false;
    
    private boolean showSearchboxFlag = false;
    private boolean hideIMEFlag = false;
    
    
	// ------------------------------
	// 定数定義
	// ------------------------------
	//	private static final int MENUID_NEW = Menu.FIRST;			// メニューID：新規作成
	private static final int MENUID_CLOSE = Menu.FIRST + 1;		// メニューID：読み込み
	private static final int MENUID_SAVE = Menu.FIRST + 2;		// メニューID：保存
	private static final int MENUID_SAVE_AS = Menu.FIRST + 3;	// メニューID：名前を付けて保存
	private static final int MENUID_CONV_ENC = Menu.FIRST + 4;	// エンコード変換
	private static final int MENUID_SEARCH = Menu.FIRST + 5;	// エンコード変換
	private static final int MENUID_INSERT_TEXT = Menu.FIRST + 6;	// 挿入
	private static final int MENUID_CONTEXT_MENU = Menu.FIRST + 7;	// コンテキストメニュー
	

	private static final int MENUID_SHOW_IME = Menu.FIRST + 10;	// 

	private static final int SHOW_FILELIST_OPEN = 0;
	private static final int SHOW_FILELIST_SAVE = SHOW_FILELIST_OPEN + 1;
	
	//改行コード
	public static final int LINEBREAK_AUTO = 0;
	public static final int LINEBREAK_CRLF = 1;
	public static final int LINEBREAK_LF   = 2;
	public static final int LINEBREAK_CR   = 3;

	@Override public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mAppName = getString(R.string.app_name);

		initConfig();
		Intent intent = getIntent();

        if( Intent.ACTION_VIEW.equals(getIntent().getAction())||
        	Intent.ACTION_EDIT.equals(getIntent().getAction())||
        	Intent.ACTION_GET_CONTENT.equals(getIntent().getAction())
        	) {
        	Uri uri = intent.getData();
        	if (uri != null){
        		filepath=uri.getPath();
        	}
        }else{
        	// 普通にLauncherから起動された場合
    		filepath = intent.getStringExtra("FILEPATH");
    		selStart = intent.getIntExtra("SELSTART", 0);
    		selStop  = intent.getIntExtra("SELSTOP" , 0);
        }
		//		System.out.println("filepath="+filepath);
		aFile = new File(filepath);

//		setContentView(R.layout.editbox_scrl);
//		edit = (ScrollWrappableEditText)findViewById(R.id.editbox);
		setContentView(R.layout.editbox);
		edit = (EditText)findViewById(R.id.editbox);
		edit.setTextSize(TypedValue.COMPLEX_UNIT_SP,fontSize);
		
//		Typeface tf = Typeface.MONOSPACE;
//		Typeface tf = Typeface.DEFAULT;
		edit.setTypeface(mTypeface);

		registerForContextMenu(edit);//追加したviewerMode用コンテキストメニューを有効にするため
		if(viewerModeFlag)hideIME();
	
		
		if(aFile.isDirectory()){
			// it's maybe new file.
//			TextEdit.this.setTitle(getString(R.string.app_name) + " - (NewFile)");
			setTitleBarText("(NewFile)");
			//message digest 取得
	        messageDigest = getMessageDigest();

		}else{
			this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

//			TextEdit.this.setTitle(getString(R.string.app_name) + " - " + aFile.getName());
			setTitleBarText(aFile.getName());
			progressDlg = ProgressDialog.show(this, null, "Now Loading...", true, false);
			new FileReadThread().start();
		}

		
		if(showBottomBarFlag){
			addBottomBar();
		}
		
		//initListener();
		initListener1();
	}
	
	private void initConfig(){
		//PasswordBoxのタイマーセット
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String timerString = sharedPreferences.getString(getText(R.string.prefPWResetTimerKey).toString(), "3");
        mResetTimer = Integer.parseInt(timerString);
        PasswordBox.setTimerVal(mResetTimer);
        
		//charset name セット
        charsetName = sharedPreferences.getString(getText(R.string.prefCharsetNameKey).toString(), "utf-8");
        
        //syncTitleFlagセット
        syncTitleFlag = sharedPreferences.getBoolean(getText(R.string.prefSyncTitleKey).toString(), false);

        //showBottomBarFlagセット
        showBottomBarFlag = sharedPreferences.getBoolean(getText(R.string.prefShowButtonsKey).toString(), true);

        //viewerModeFlagセット
        viewerModeFlag = sharedPreferences.getBoolean(getText(R.string.prefViewerModeKey).toString(), false);

        //fontsizeセット
        fontSize = sharedPreferences.getFloat(getText(R.string.prefFontSizeKey).toString(), fontSize);
        
        //linebreakセット
        String linebreakString = sharedPreferences.getString(getText(R.string.prefLineBreakCodeKey).toString(), "auto");
        if(linebreakString.equalsIgnoreCase("auto")){
        	lineBreakSetting = LINEBREAK_AUTO;
        }else if(linebreakString.equalsIgnoreCase("crlf")){
        	lineBreakSetting = LINEBREAK_CRLF;
        }else if(linebreakString.equalsIgnoreCase("lf")){
        	lineBreakSetting = LINEBREAK_LF;
        }else if(linebreakString.equalsIgnoreCase("cr")){
        	lineBreakSetting = LINEBREAK_CR;
        }
        
        //intentでファイルパスを渡されなかったときの為にinitDirをfilepathにセット
        filepath =  sharedPreferences.getString(getText(R.string.prefInitDirKey).toString(), "/sdcard");

        //autoSaveFlagセット
        autoSaveFlag = sharedPreferences.getBoolean(getText(R.string.prefAutoSaveKey).toString(), false);

        //typefaceセット
        String typefaceString = sharedPreferences.getString(getText(R.string.prefTypefaceKey).toString(), "DEFAULT");
        if(typefaceString.equalsIgnoreCase("DEFAULT")){
        	mTypeface = Typeface.DEFAULT;
        }else if(typefaceString.equalsIgnoreCase("MONOSPACE")){
        	mTypeface = Typeface.MONOSPACE;
        }else if(typefaceString.equalsIgnoreCase("SANS_SERIF")){
        	mTypeface = Typeface.SANS_SERIF;
        }else if(typefaceString.equalsIgnoreCase("SERIF")){
        	mTypeface = Typeface.SERIF;
        }

        //autoLink setting
        autoLinkWeb  = sharedPreferences.getBoolean(getText(R.string.prefAutoLinkWebKey).toString(),   false);
        autoLinkEmail= sharedPreferences.getBoolean(getText(R.string.prefAutoLinkEmailKey).toString(), false);
        autoLinkTel  = sharedPreferences.getBoolean(getText(R.string.prefAutoLinkTelKey).toString(),   false);
        if(autoLinkWeb || autoLinkEmail || autoLinkTel){
        	autoLinkFlag = true;
        }
        
        
        //noTitleBarFlagセット
        noTitleBarFlag = sharedPreferences.getBoolean(getText(R.string.prefNoTitleBarKey).toString(), false);
        //呼び出す箇所は、Activity.setContentView()よりも前である必要があります。
        if(noTitleBarFlag)requestWindowFeature(Window.FEATURE_NO_TITLE);

        
	}

	private void addBottomBar(){
    	View bottombar = getLayoutInflater().inflate(R.layout.bottom_bar, null);
		ViewGroup editboxlayout = (ViewGroup)findViewById(R.id.editboxlayout);

		// ボタンのクリックリスナー
		Button btnUpDir = (Button)bottombar.findViewById(R.id.LeftButton);
		btnUpDir.setText(R.string.BottomMenu_close);
		btnUpDir.setOnClickListener(new View.OnClickListener() {

			//		@Override
			public void onClick(View v) {
				//ボタンを押したときの動作
				closeFile();
			}

		});
		// ボタンのクリックリスナー
		Button btnMenu = (Button)bottombar.findViewById(R.id.RightButton);
		btnMenu.setText(R.string.BottomMenu_menu);
		btnMenu.setOnClickListener(new View.OnClickListener() {

			//		@Override
			public void onClick(View v) {
				//ボタンを押したときの動作
				openOptionsMenu();
			}

		});
		editboxlayout.addView(bottombar);

	}

	private void moveCursor(int x, int y){
		//x-= mPaddingLeft;
		//y-= mPaddingTop;
		//Layout l = getLayout();
		x-=edit.getPaddingLeft();
		y-=edit.getPaddingTop();
		Layout l = edit.getLayout();
		int offset = 0;
		int line = l.getLineForVertical(y);
		if( line == 0 && y < l.getLineTop(line) ){
			offset = 0;
		}else if( line >= l.getLineCount()-1 && y >= l.getLineTop(line+1) ){
			offset = l.getText().length();
		}else{
			offset = l.getOffsetForHorizontal(line,x);
		}
//    	Log.d("textEdit", "offset:" + offset);
		
		edit.setSelection(offset);

		/**
		//現在のselectionStartとselectionEndの値が一致していなかったらselectionEndだけ移動
		int start = edit.getSelectionStart();
		int end   = edit.getSelectionEnd();
		if(start == end){
			edit.setSelection(offset);
		}else{
			edit.setSelection(start,offset);
		}
		 **/
		
	}
	
	private void initListener1(){

		//scrollviewの処理
		final ScrollView scrollview = (ScrollView)findViewById(R.id.ScrollView01);
//    	scrollview.requestDisallowInterceptTouchEvent(true);
		//		scrollview.setOnTouchListener(new OnTouchListener() {
		
		edit.setOnTouchListener(new OnTouchListener(){
		    //@Override
		    public boolean onTouch(View v, MotionEvent event) {

		    	if(autoLinkFlag){
		    		int action = event.getAction();
			    	if (action == MotionEvent.ACTION_UP ||
			    			action == MotionEvent.ACTION_DOWN) {
			    		int x = (int) event.getX();
			    		int y = (int) event.getY();
			    		x -= ((EditText)v).getTotalPaddingLeft();
			    		y -= ((EditText)v).getTotalPaddingTop();
			    		x += v.getScrollX();
			    		y += v.getScrollY();
			    		
			    		Layout layout = ((EditText)v).getLayout();
			    		int line = layout.getLineForVertical(y);
			    		int off = layout.getOffsetForHorizontal(line, x);
			    		Spannable buffer = ((EditText)v).getText();
			    		
			    		ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

			    		if (link.length != 0) {
			    			if (action == MotionEvent.ACTION_UP) {
			    				link[0].onClick(v);
			    			} else if (action == MotionEvent.ACTION_DOWN) {
			    				Selection.setSelection(buffer,
			    						buffer.getSpanStart(link[0]),
			    						buffer.getSpanEnd(link[0]));
			    			}
			    			return true;
			    		}
			    	}

		    	}
		    	
		    	
		    	
		    	switch (event.getAction()) { 
		    	case MotionEvent.ACTION_DOWN: 
//			    	Log.d("textEdit", "onTouchEvent ACTION_DOWN");
		    		break; 
		    	case MotionEvent.ACTION_UP: 
//			    	Log.d("textEdit", "onTouchEvent ACTION_UP");
			    	if(onScrollFlag){
				        int ex = (int)(0.5+event.getX());
				        int ey = (int)(0.5+event.getY());			    		
			    		moveCursor(ex,ey);
			    		setOnScrollFlag(false);
			    	}
		    		break; 
		    	case MotionEvent.ACTION_MOVE: 
//			    	Log.d("textEdit", "onTouchEvent ACTION_MOVE");
		    		break; 
		    	case MotionEvent.ACTION_CANCEL: 
//			    	Log.d("textEdit", "onTouchEvent ACTION_CANCEL ");
		    		break; 
		    	} 
		    	
//		    	Log.d("textEdit", "onTouchEvent ex:" + ex +",ey:" + ey);

		        return false;
//		        return true;
		    
		    }
		});
		
		if(autoLinkFlag)edit.addTextChangedListener(new myTextWatcher());
//		edit.addTextChangedListener(new myTextWatcher());
		
		scrollview.setOnTouchListener(new OnTouchListener(){
		    //@Override
		    public boolean onTouch(View v, MotionEvent event) {
		    	
		    	setOnScrollFlag(true);
		    	
/**		    	
//		    	Log.d("scrollview", "onTouchEvent");
		        int ex = (int)(0.5+event.getX());
		        int ey = (int)(0.5+event.getY());

		        int x = scrollview.getScrollX();
	    		int y = scrollview.getScrollY();

		    	switch (event.getAction()) { 
		    	case MotionEvent.ACTION_DOWN: 
			    	Log.d("scrollview", "onTouchEvent ACTION_DOWN");
		    		break; 
		    	case MotionEvent.ACTION_UP: 
			    	Log.d("scrollview", "onTouchEvent ACTION_UP");

		    		break; 
		    	case MotionEvent.ACTION_MOVE: 
			    	Log.d("scrollview", "onTouchEvent ACTION_MOVE");
		    		break; 
		    	case MotionEvent.ACTION_CANCEL: 
			    	Log.d("scrollview", "onTouchEvent ACTION_CANCEL ");
		    		break; 
		    	} 
		    	

		    	Log.d("scrollview", "onTouchEvent ex:" + ex +",ey:" + ey + "e:" + x +",y:" + y );
//		    	Log.d("scrollview", "onTouchEvent x:" + x +",y:" + y +",ex:"+ ex +",ey:" + ey);
//	    		edit.scrollTo(0,scrollview.getScrollY());
 	**/
		    	
		    	
		        return false;
//		        return true;
		    
		    }
		});
		

	}
	private void setOnScrollFlag(boolean flag){
		onScrollFlag = flag;
	}
	
	
	
	
	
	// create Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		//        menu.add(0, MENUID_NEW, 0, R.string.menu_new)
		//			.setShortcut('0', 'n')
		//			.setIcon(android.R.drawable.ic_menu_edit);

		menu.add(0, MENUID_CLOSE, 0, R.string.menu_close)
		.setShortcut('0', 'c')
		.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		menu.add(0, MENUID_SAVE, 0, R.string.menu_save)
		.setShortcut('1', 's')
		.setIcon(android.R.drawable.ic_menu_save);

		menu.add(0, MENUID_SAVE_AS, 0, R.string.menu_save_as)
		.setShortcut('2', 'a')
		.setIcon(android.R.drawable.ic_menu_save);
/*
		menu.add(0, MENUID_CONV_ENC, 0, R.string.menu_convert_encoding)
		.setShortcut('3', 'e')
		.setIcon(android.R.drawable.ic_menu_view);
*/
		menu.add(0, MENUID_SEARCH, 0, R.string.menu_search)
		.setShortcut('4', 'f')
		.setIcon(android.R.drawable.ic_menu_search);
		
		menu.add(0, MENUID_INSERT_TEXT, 0, R.string.menu_insert_text)
		.setShortcut('5', 'v')
		.setIcon(android.R.drawable.ic_menu_add);
		
		menu.add(0, MENUID_CONTEXT_MENU, 0, R.string.menu_context_menu)
		.setShortcut('6', 'v')
		.setIcon(android.R.drawable.ic_menu_agenda);
		
		return true;
	}



	// メニュー押下時のイベント処理
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		super.onMenuItemSelected(featureId, item);
		
		switch(item.getItemId()) {
		
		case MENUID_CLOSE:		// 終了
			closeFile();
			break;


		case MENUID_SAVE:		// ファイル保存
			saveFile();
			break;
			
		case MENUID_SAVE_AS:	//名前を付けてファイル保存
			saveFileAsNewFile();
			break;
			
		case MENUID_CONV_ENC:	//Encoding変換
			new AlertDialog.Builder(TextEdit.this)
			.setTitle(R.string.convert_encoding_buttons_title)
			.setCancelable(true)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//エンコーディング変換
					try{
						strText = new String(binText,"Shift-JIS");
						strText = strText.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
						setStringToEditText();//edit.setText(strText);
						binText = strText.getBytes();
						//encoding 考慮が必要
					}catch(Exception e){
						e.printStackTrace();
						showMessage(e.toString());
					}

				
				}
			})
			.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//Do Nothing.
				}
			})
			.show(); //ダイアログ表示;

			break;

		case MENUID_SEARCH:	//本文を検索
			search();
	        
			break;

		case MENUID_INSERT_TEXT://文字列挿入
			insertText();
	        
			break;

		case MENUID_CONTEXT_MENU://menu
			openContextMenu(edit);
	        
			break;
		default:
			break;
		}
		return true;
	}

	//search menu
	private void search(){
		if(showSearchboxFlag == false){
			final View searchboxView = getLayoutInflater().inflate(R.layout.searchbox, null);
			final ViewGroup textEditlayout = (ViewGroup)findViewById(R.id.editboxlayout);
			final EditText findTxtEdit = (EditText)searchboxView.findViewById(R.id.editSearchWord);
			//		final Button findBtn = (Button) searchboxView.findViewById(R.id.btnFind);
			//		final Button findBtnBackward = (Button) searchboxView.findViewById(R.id.btnFindBackward);
			final ImageView findBtn = (ImageView) searchboxView.findViewById(R.id.btnFind);
			final ImageView findBtnBackward = (ImageView) searchboxView.findViewById(R.id.btnFindBackward);

			/*
			findTxtEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
		        public void onFocusChange(View v, boolean hasFocus) {
		            if (hasFocus) {
		            	//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						//InputMethodManager inputMethodManager =   
						//	(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
						//inputMethodManager.showSoftInputFromInputMethod(v.getWindowToken(), 0);
						
						InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
						inputMethodManager.showSoftInput(v, 0); 
		            }
		        }
		    });
			*/
			findTxtEdit.setOnKeyListener(new View.OnKeyListener(){
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					/**
		        	   // ここではEditTextに改行が入らないようにしている。
		               if (event.getAction() == KeyEvent.ACTION_DOWN) {
		                   return true;
		               }
					 **/
					// Enterを離したときに検索処理を実行
					if (event.getAction() == KeyEvent.ACTION_UP
							&& keyCode == KeyEvent.KEYCODE_ENTER) {
						// 本文を検索する。
						String findStr = findTxtEdit.getText().toString();
						if(findStr.length() > 0 && regexCompileOK(findStr)){

							InputMethodManager inputMethodManager =   
								(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
							inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);  

							searchWord(findStr);
						}

						return true;
					}
					return false;
				}
			});
			
			
			ImageView findCancelButton = (ImageView) searchboxView.findViewById(R.id.FindCancelBtn);
			findCancelButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// 検索ボックスを削除する
					textEditlayout.removeView(searchboxView);
					showSearchboxFlag = false;
				}
			});

			findBtn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// 本文を検索する。
					String findStr = findTxtEdit.getText().toString();
					if(findStr.length() > 0 && regexCompileOK(findStr)){

						InputMethodManager inputMethodManager =   
							(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
						inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);  

						searchWord(findStr);
					}
				}

			});

			findBtnBackward.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// 本文を検索する。
					String findStr = findTxtEdit.getText().toString();
					if(findStr.length() > 0 && regexCompileOK(findStr)){
						
						InputMethodManager inputMethodManager =   
							(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
						inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);  
						searchWordBackward(findStr);
					}		
				}

			});

			textEditlayout.addView(searchboxView);
			showSearchboxFlag = true;
			
			
			

			

			//findTxtEdit.clearFocus();
			//InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
			//inputMethodManager.showSoftInput(findTxtEdit, 0); 
			//InputMethodManager inputMethodManager =   
			//	(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
			//inputMethodManager.showSoftInputFromInputMethod(findTxtEdit.getWindowToken(), 0);
			findTxtEdit.requestFocus();
		}
	}
	
	boolean regexCompileOK(String keyword){
		//try regex compile
		try{
			Pattern.compile(keyword);
			return true;
		}catch(PatternSyntaxException e){
//			showMessage(getString(R.string.alert_regex_syntax_error) + "\n" + e.getMessage());
			showMessage(getString(R.string.alert_regex_syntax_error) + "\n" + e.getDescription() + "  near index "+ e.getIndex()+":\n" + e.getPattern());
			return false;
		}catch(Exception e){
			showMessage(e.toString());
			return false;
		}

	}
	
	//insert text menu
	private void insertText(){
		if(mTemplateList == null){
			// load list
			mTemplateList = TemplateTextList.loadPreferences(this);
		}
		
		//check clipbord
		ClipboardManager clip = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
		String clipText = clip.getText().toString();
		
				
		final ArrayList<String> list = new ArrayList<String>();

		if(clipText != null && clipText.length()>0)list.add(clipText);
		
		for(int i=0; i < mTemplateList.size(); i++){
			myTemplateText item = mTemplateList.get(i);
			list.add(item.toString());
		}
		
		if(list.size() == 0){
			Toast.makeText(this, getText(R.string.alert_no_template_text), Toast.LENGTH_LONG).show();
			return ;
		}
		ListView lv = new ListView(this);
		
		

        final AlertDialog alertDialog  = new AlertDialog.Builder(this)
        .setTitle(R.string.menu_insert_text)
        .setPositiveButton(R.string.alert_dialog_cancel, null)
        .setView(lv)
        .create();

		lv.setAdapter(new ArrayAdapter<String>(this, R.layout.insert_text_row, list));
//		lv.setPadding(10, 3, 10, 3);
		lv.setOnItemClickListener(new OnItemClickListener(){
            public void onItemClick(AdapterView<?> items, View view, int position, long id) {
                alertDialog.dismiss();
				int st = edit.getSelectionStart();
				int en = edit.getSelectionEnd();
				if(st > en){int tmp = st;st = en;en = tmp;}				
				edit.getText().replace(st, en, list.get(position).toString());

//                Toast.makeText(TextEdit.this, list.get(position).toString(), Toast.LENGTH_LONG).show();
            }
        });
        alertDialog.show();
        
		/**
		int offset = 0;
		if(clipText != null && clipText.length()>0)offset = 1;
		int max = mTemplateList.size();
		final CharSequence[] menu = new CharSequence[max + offset];
		
		if(offset > 0)menu[0] = clipText;
		
		Date inDate = new Date();		
		for(int i=0; i < max; i++){
			myTemplateText item = mTemplateList.get(i);
			if(item.isTimeFormat()){
				menu[i+offset] = DateFormat.format(item.getText(), inDate);
			}else{
				menu[i+offset] = item.getText();
			}
		}
		
		
		
		AlertDialog alertDialog = new AlertDialog.Builder(this)
		.setTitle(R.string.menu_insert_text)
		.setItems(menu, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int item)
			{
				int st = edit.getSelectionStart();
				int en = edit.getSelectionEnd();
				if(st > en){int tmp = st;st = en;en = tmp;}
				
				
				edit.getText().replace(st, en, menu[item]);
//				Toast.makeText(getApplicationContext(), "clicked:" + menu[item].toString(), Toast.LENGTH_SHORT).show();
				
			}
		})
		.create();
		alertDialog.show();
		**/
		
	}	
	
	
	// FileListアクティビティからの戻り
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		ignoreOnResume = true;//onResumeでの処理を無視するためのフラグセット
		switch( requestCode ) {
		case SHOW_FILELIST_OPEN:	// ファイルオープン
			if (resultCode == RESULT_OK) {
				filepath = data.getStringExtra(SelectFileName.INTENT_FILEPATH);
				aFile = new File(filepath);
//				TextEdit.this.setTitle(getString(R.string.app_name) + " - " + aFile.getName());
				setTitleBarText(aFile.getName());
				isEncrypt = data.getBooleanExtra(SelectFileName.INTENT_ENCRYPT, isEncrypt);

				progressDlg = ProgressDialog.show(
						TextEdit.this, null, getString(R.string.proc_open), true, false);
				new FileReadThread().start();
			}
			break;
		case SHOW_FILELIST_SAVE:	// ファイル保存
			if (resultCode == RESULT_OK) {
				// ファイル名取得
				final String destfilepath = data.getStringExtra(SelectFileName.INTENT_FILEPATH);
				final boolean destIsEncrypt = data.getBooleanExtra(SelectFileName.INTENT_ENCRYPT, isEncrypt);
				File destFile = new File(destfilepath);
				if(destFile.exists()){

					new AlertDialog.Builder(TextEdit.this)
					.setTitle(R.string.alert_overwrite)
					.setCancelable(true)
					.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							filepath = destfilepath;
							aFile = new File(filepath);
							//TextEdit.this.setTitle(getString(R.string.app_name) + " - " + aFile.getName());
							setTitleBarText(aFile.getName());
							isEncrypt = destIsEncrypt;
							strText = edit.getText().toString();
							doFileWrite();


						}
					})
					.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							//Do Nothing.
						}
					})
					.show(); //ダイアログ表示;

				}else{
					//既にファイルが存在しなければ、上書き保存の確認をせずに保存
					filepath = data.getStringExtra(SelectFileName.INTENT_FILEPATH);
					aFile = new File(filepath);
//					TextEdit.this.setTitle(getString(R.string.app_name) + " - " + aFile.getName());
					setTitleBarText(aFile.getName());
					strText = edit.getText().toString();
					isEncrypt = data.getBooleanExtra(SelectFileName.INTENT_ENCRYPT, isEncrypt);

//					System.out.println("onActivityResult:" + isEncrypt);

					doFileWrite();
				}
				
				
				
				
				
			}
			break;
		default:
			break;
		}
	}

	
	//バックグラウンドになる前にタイマーをリセットしておく
    @Override
    protected void onPause() {
        super.onPause();
//        Toast.makeText(this, "onPause()", Toast.LENGTH_SHORT).show();
        
        mOnPauseTime = new Date().getTime();
    	/**
        if(isEncrypt){
    		if(PasswordBox.getPassDigest() == null){//この処理でタイマーはリセットされてしまう
    			//タイマーが満了していたら
    		}else{
    			//満了していなかったら
    		}
        }
   		**/
        
        //AutoSave process
        if(autoSaveFlag && !onPauseBeforeSelectSaveFile){
            //message digest 修正されているかどうか確かめるためにdigestを取得して比較する。
            String tmpDigest = getMessageDigest();        
        	if(tmpDigest != null && tmpDigest.equals(messageDigest)){
        		//変更がなければ保存の必要なし
        	}else{
        		autoSaveFile();
        	}

        }
        onPauseBeforeSelectSaveFile = false;
    }

    //復帰したときにタイマーが満了していたらcloseする(暗号化ファイル限定)
    @Override
    protected void onResume() {
        super.onResume();
//		System.out.println("onResume:" + isEncrypt);

//        Toast.makeText(this, "onResume()", Toast.LENGTH_SHORT).show();
        if(isEncrypt && !ignoreOnResume){
    		long now = new Date().getTime();
    		if(now - mOnPauseTime > mResetTimer*60*1000){
    			//タイマーが満了していたら
    			finish();    			
    		}
        	/**
        	if(PasswordBox.getPassDigest() == null){
    			//タイマーが満了していたら
    			finish();
    		}else{
    			//満了していなかったら
    			//do nothing
    		}
        	**/
        }
        
        ignoreOnResume = false ;//初期化
    }

/**
    @Override
    protected void onStop() {
//        super.onStop();
//        Toast.makeText(this, "onStop()", Toast.LENGTH_SHORT).show();
// ここでcloseFile処理をするとエラーになる。
        closeFile();
    }
**/

    // キーイベント発生時、呼び出されます
    private boolean mBackKeyDown = false;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(event.getKeyCode()){
    		case KeyEvent.KEYCODE_BACK:
    			mBackKeyDown = true;
    			//back keyのACTION_DOWNの時に処理をする。
 //   			closeFile();//
    			return true;

    		case KeyEvent.KEYCODE_DPAD_LEFT: // 左キー
    			//現在のカーソルの位置が0なら閉じる
    			if(edit.getSelectionStart() == 0 && edit.getSelectionEnd() == 0){
    				closeFile();
    				return true;
    			}
    			//through to default
    		default :
    			mBackKeyDown = false;
            	break;
    		}
    	}

        if (event.getAction() == KeyEvent.ACTION_UP) { // キーが離された時
            switch (event.getKeyCode()) {
			//back keyは無視するACTION_DOWNの時に処理をする。
            case KeyEvent.KEYCODE_BACK: // BACK KEY
            	 if(mBackKeyDown){
            		 mBackKeyDown = false;//戻しておく
            		 closeFile();//
                     return true;
            	 }else{
            		 mBackKeyDown = false;
            	 }
            default:
            	mBackKeyDown = false;
            	break;
            }
        }
        return super.dispatchKeyEvent(event);
    }


	// ファイル読み出し処理終了時の処理（ファイル処理スレッドから通知される）
	final Runnable run_readFinished = new Runnable()
	{
		public void run() {
			// ファイル処理終了後の処理を記述
			progressDlg.dismiss();

			//header check
			String BFHeader = "";
			if(binText.length >= 4 )BFHeader = new String(binText,0,4);
			if(BFHeader.equals("BF01") && !filepath.endsWith(".txt")){//This File is BF01
				if(PasswordBox.getPassDigest() == null){
					getPasswordAndDecryptData();
				}else{
					decryptData();
					setStringToEditText();//edit.setText(strText);
					setSelection();
				}

			}else{
				try{
					//strText = new String(binText);
					setBinTextToStrText();
//					strText = new String(binText,charsetName);
					//				strText = convertCharEncodeingJISAutoDetect(binText);
					// encodewoを指定して、strTextに格納する。
				}catch(Exception e){
					e.printStackTrace();
					showMessage(e.toString());
				}
				setStringToEditText();//edit.setText(strText);
				setSelection();
			}

	        //message digest 取得 ファイルを正常に読み込んだのでdigest更新
	        messageDigest = getMessageDigest();

			//			Toast.makeText(TextEdit.this, "complete reading file",
			//				       Toast.LENGTH_SHORT).show();
		}
	};

	// ファイル書き込み処理終了時の処理（ファイル処理スレッドから通知される）
	final Runnable run_writeFinished = new Runnable()
	{
		public void run() {
			String s = aFile.getName();
			// FileWriteThreadの中で更新できないので、ここで更新
//			TextEdit.this.setTitle(getString(R.string.app_name) + " - " + s);
			setTitleBarText(aFile.getName());
//			System.out.println("FileWriteThread:" + aFile.getName());

			
			// ファイル処理終了後の処理を記述
			//edit.setText(strText);
			progressDlg.dismiss();
//			Toast.makeText(TextEdit.this, "complete saving file",
//					Toast.LENGTH_LONG).show();
			int duration = Toast.LENGTH_LONG;
			if(autoSaveFlag)duration = Toast.LENGTH_SHORT;
			//Toast.makeText(TextEdit.this, "save: " + s,	duration).show();
			Toast.makeText(TextEdit.this, getString(R.string.notify_file_save) + ": " +  s,	duration).show();
			
			
	        //途中で保存しても最後に保存してもsetResultしておく。途中で保存したらcloseAfterSaveFlagがtrueにならないため。
			setResultForActionGetContent();

			if(closeAfterSaveFlag){
				finish();
			}

			//message digest 取得 ファイルを正常に保存できたのでdigest更新
	        messageDigest = getMessageDigest();
	        

		}
	};


	// ファイルアクセスエラー時のToast表示（ファイル処理スレッドから通知される）
	final Runnable run_file_acc_err = new Runnable()
	{
		public void run() {
			progressDlg.dismiss();
			showMessage(ErrorMessage);
			ErrorMessage="";
			//			Toast.makeText(TextEdit.this, R.string.file_access_error,
			//				       Toast.LENGTH_SHORT).show();
			closeAfterSaveFlag = false;//initialize
		}
	};

	// ----------------------------------------
	// 以下、ファイル処理スレッド
	// ----------------------------------------

	// ファイル読み込みスレッド
	private class FileReadThread extends Thread
	{
		public void run()
		{

			try {
				//				Thread.sleep(1000);

				binText = MyUtil.readTextFile(filepath);

				// ハンドラでActivity(アプリ)に処理終了を通知する
				notifyHandler.post(run_readFinished);
			}catch (MyUtilException e) {
				e.printStackTrace();
				strText="";
				ErrorMessage = getString(((MyUtilException) e).getCode());
				progressDlg.dismiss();
				// ハンドラでActivity(アプリ)にエラーを通知する
				notifyHandler.post(run_file_acc_err);
				
				
			}catch (Exception e) {
				e.printStackTrace();
				strText="";
				ErrorMessage = e.toString();
				progressDlg.dismiss();
				// ハンドラでActivity(アプリ)にエラーを通知する
				notifyHandler.post(run_file_acc_err);
			}

		}

	};

	// ファイル書き込みスレッド
	private class FileWriteThread extends Thread
	{
		public void run() {
			try {
				//				Thread.sleep(1000);

				MyUtil.writeTextFile(filepath, binText);					
				// ハンドラでActivity(アプリ)に処理終了を通知する
				
				if(syncTitleFlag && needToRenameForSyncTitle){ //タイトルとファイル名を同期させるためのrename処理
					//タイトルとファイル名を同期させるためのrename処理

					//現在のファイル名を拡張子抜きで取得
					File orgFile = new File(filepath);
					String dstFilepath = getFilenameFromHeadLineNonDuplicate();
					
					//比較して、一致していなければrename
					if(!filepath.equals(dstFilepath)){
						File dstFile = new File(dstFilepath);
						MyUtil.renameFile(orgFile, dstFile);
						// 各種変数の更新
						filepath = dstFilepath;
						aFile = new File(filepath);
						// FileWriteThreadの中で更新できないので、run_writeFinishedで更新する
						//TextEdit.this.setTitle(getString(R.string.app_name) + " - " + aFile.getName());

					}
						
					
				}
				needToRenameForSyncTitle = false;//もとに戻しておく
				
				notifyHandler.post(run_writeFinished);
			}catch (Exception e) {
				e.printStackTrace();
				ErrorMessage = getString(R.string.alert_general_error) + "\n" + e.toString();
				// ハンドラでActivity(アプリ)にエラーを通知する
				notifyHandler.post(run_file_acc_err);
			}
		}
	};


	private void doFileWrite(){
		if(isEncrypt){
			//encryptしてbinText
			getPasswordAndEncryptData();
		}else{
			progressDlg = ProgressDialog.show(
					TextEdit.this, null, getString(R.string.proc_save), true, false);

			try{
				setStrTextToBinText();
//				binText = strText.getBytes(charsetName);			
				// encodeを指定してbinに変換する。
				new FileWriteThread().start();			

			}catch(Exception e){
				e.printStackTrace();
				showMessage(getString(R.string.alert_general_error) + "\n" + e.toString());
			}
		}
	}

	/*
	 * データ保存時のパスワード取得とencrypt
	 */
	private void getPasswordAndEncryptData(){

		if(PasswordBox.getPassDigest() != null){
			encryptDataAndSave();
		}else{
			getPasswordForEncryptAndSave();
		}
	}

	private void getPasswordForEncryptAndSave(){
		//コンテキストからインフレータを取得
		LayoutInflater inflater = LayoutInflater.from(this);
		//レイアウトXMLからビュー(レイアウト)をインフレート
		final View inputView = inflater.inflate(R.layout.input_pass2, null);

		//ダイアログを構成
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final AlertDialog alertDialog  = new AlertDialog.Builder(this)
		.setTitle(R.string.pass_input_text)
		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			//		    @Override
			public void onClick(DialogInterface dialog, int which) {
				//OKボタンが押下された時に入力された文字を設定する
				EditText passEditText = (EditText)inputView.findViewById(R.id.dialog_edittext);
				EditText passEditTextConfirm = (EditText)inputView.findViewById(R.id.dialog_edittext_confirm);
				String pass = passEditText.getText().toString();
				if(pass.equals(passEditTextConfirm.getText().toString()) && pass.length() > 0){
					PasswordBox.setPassword(pass);
					encryptDataAndSave();
				}else{
					Toast.makeText(TextEdit.this, R.string.password_not_match, Toast.LENGTH_LONG).show();
					getPasswordForEncryptAndSave();
				}

			}
		}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//Do Nothing
			}
		})
		.setView(inputView)
		.create();

		alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		alertDialog.show(); //ダイアログ表示
		
	
	}
	
	private void encryptDataAndSave(){
		try{
// strTextをgetByteするところでencodeを指定する。
			setStrTextToBinText();
//			binText = strText.getBytes(charsetName);
			binText = MyUtil.encrypt(binText, PasswordBox.getPassDigest());
//			binText = MyUtil.encrypt(strText.getBytes(), PasswordBox.getPassDigest());

			/**
//			SecretKeySpec sksSpec = new SecretKeySpec(key.getBytes(), "Blowfish");
			SecretKeySpec sksSpec = new SecretKeySpec(PasswordBox.getPassDigest(), "Blowfish");
			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.DECRYPT_MODE, sksSpec);
			byte[] decrypted = cipher.doFinal(binText);
			strText = new String(decrypted);
			 */
			progressDlg = ProgressDialog.show(
					TextEdit.this, null, getString(R.string.proc_save), true, false);
			new FileWriteThread().start();	

		}catch(Exception e){
			e.printStackTrace();
			PasswordBox.resetPassword();
			showMessage(getString(R.string.alert_general_error) + "\n" + e.toString());
		}

	}


	/**
	 * input password 1
	 */
	private void __getPasswordAndDecryptData(){

		//コンテキストからインフレータを取得
		LayoutInflater inflater = LayoutInflater.from(this);

		//レイアウトXMLからビュー(レイアウト)をインフレート
		final View inputView = inflater.inflate(R.layout.input_pass, null);

		inputView.setOnFocusChangeListener(new OnFocusChangeListener() {
//	        @Override
	        public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
	            if (hasFocus) {
//	                inputMethodManager.showSoftInput(nameEditText,0);
	                inputMethodManager.showSoftInput(v, 0);//.showSoftInput(v.getWindowToken(),0);
//	                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	            
	            }else{
//	                inputMethodManager.hideSoftInputFromWindow(nameEditText,0);
	                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(),0);
	            }
	        }
	    });
		
		
		
		//ダイアログを構成
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.pass_input_text)
//		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			//		    @Override
			public void onClick(DialogInterface dialog, int which) {
				//OKボタンが押下された時に入力された文字を設定する
				EditText passEditText = (EditText)inputView.findViewById(R.id.dialog_edittext);
				String pass = passEditText.getText().toString();
				if(pass.length()>0){
					PasswordBox.setPassword(pass);
					decryptData();
					setStringToEditText();//edit.setText(strText);
					setSelection();
					//message digest 取得
					messageDigest = getMessageDigest();
				}else{
					//passが空だったら終了する
					//	finish();
					Toast.makeText(TextEdit.this, R.string.password_empty, Toast.LENGTH_LONG).show();
					getPasswordAndDecryptData();
				}

			}
		}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//TextEditを閉じる.
				finish();
			}
		}).setOnCancelListener(new DialogInterface.OnCancelListener() {
//			  @Override
			public void onCancel(DialogInterface dialog) {
//				Log.v("TextEdit","onCancel");
				// キャンセルボタンと戻るボタンが押された時の処理をここに記述する。
				finish();
			}
		})
		.setView(inputView)
		.show(); //ダイアログ表示


	    
	}

	/**
	 * input password 1
	 */
	private void getPasswordAndDecryptData(){

		//コンテキストからインフレータを取得
		LayoutInflater inflater = LayoutInflater.from(this);

		//レイアウトXMLからビュー(レイアウト)をインフレート
		final View inputView = inflater.inflate(R.layout.input_pass, null);

		
		//ダイアログを構成
		final AlertDialog alertDialog  = new AlertDialog.Builder(this)
		.setTitle(R.string.pass_input_text)
//		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			//		    @Override
			public void onClick(DialogInterface dialog, int which) {
				//OKボタンが押下された時に入力された文字を設定する
				EditText passEditText = (EditText)inputView.findViewById(R.id.dialog_edittext);
				String pass = passEditText.getText().toString();
				if(pass.length()>0){
					PasswordBox.setPassword(pass);
					decryptData();
					setStringToEditText();//edit.setText(strText);
					setSelection();
					//message digest 取得
					messageDigest = getMessageDigest();
				}else{
					//passが空だったら終了する
					//	finish();
					Toast.makeText(TextEdit.this, R.string.password_empty, Toast.LENGTH_LONG).show();
					getPasswordAndDecryptData();
				}

			}
		}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//TextEditを閉じる.
				finish();
			}
		}).setOnCancelListener(new DialogInterface.OnCancelListener() {
//			  @Override
			public void onCancel(DialogInterface dialog) {
//				Log.v("TextEdit","onCancel");
				// キャンセルボタンと戻るボタンが押された時の処理をここに記述する。
				finish();
			}
		})
		.setView(inputView)
		.create();

		alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		alertDialog.show(); //ダイアログ表示


	    
	}

	private void decryptData(){
		try{
			binText = MyUtil.decrypt(binText, PasswordBox.getPassDigest());
			// encodeしてstrに変換
			setBinTextToStrText();
//			strText = new String(binText,charsetName);
//			strText = new String(binText);
//			strText = convertCharEncodeingJISAutoDetect(binText);
			
			//		strText = MyUtil.decrypt(binText, PasswordBox.getPassDigest());
			///		binText = strText.getBytes();


			/**
//		SecretKeySpec sksSpec = new SecretKeySpec(key.getBytes(), "Blowfish");
		SecretKeySpec sksSpec = new SecretKeySpec(PasswordBox.getPassDigest(), "Blowfish");
		Cipher cipher = Cipher.getInstance("Blowfish");
		cipher.init(Cipher.DECRYPT_MODE, sksSpec);
		byte[] decrypted = cipher.doFinal(binText);
		strText = new String(decrypted);
			 */
			
		}catch(MyUtilException e){
			e.printStackTrace();
			PasswordBox.resetPassword();
			String errorMsg = getString(((MyUtilException) e).getCode());
			showMessageAndClose(errorMsg);
			
		}catch(Exception e){
			e.printStackTrace();
			PasswordBox.resetPassword();
			showMessageAndClose(getString(R.string.alert_general_error) + "\n" + e.toString());
		}

		isEncrypt = true;

	}

	
	private void searchWord(String word) {

		int initPosition = 0;
		int wordlength;
		// 現在のカーソル位置を取得
//		initPosition = edit.getSelectionStart();
		initPosition = edit.getSelectionEnd();
		wordlength = word.length();
		// 検索！
		// テキストボックスの内容をバッファに入れておく。
//		strText = edit.getText().toString();//ファイルの更新を検知できなくなるので、strTextには入れない
		
		//正規表現で検索する
		Pattern pattern = Pattern.compile(word,Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
//		Matcher matcher = pattern.matcher(strText);
		Matcher matcher = pattern.matcher(edit.getText().toString());
		int i= -1;
		if( matcher.find(initPosition)) { //もしinitPosition以降に見つかったら
			i = matcher.start();
			wordlength = matcher.group().length();
		}else if(matcher.find()){
			i = matcher.start();			
			wordlength = matcher.group().length();
		}
		//正規表現で検索する。ここまで

		
		// 見つからない場合はトースト表示
		if(i < 0){
			//Toast.makeText(TextEdit.this, R.string.notFound, Toast.LENGTH_LONG).show();
			ToastMaster.makeTextAndShow(TextEdit.this, R.string.notFound, Toast.LENGTH_LONG);
		}else{
			// 見つかった場合はカーソル位置を移動
			//edit.setSelection(i);
			edit.setSelection(i, i+wordlength);
		}
		// edit側にフォーカスを移動する。
		edit.requestFocus();

	}
	
	private void searchWordBackward(String word) {

		int wordlength = 0;
		// 現在のカーソル位置を取得
		int initPosition = edit.getSelectionStart();
//		int initPosition = edit.getSelectionEnd();
		int maxlength = edit.getText().length();

		// 検索！
		// テキストボックスの内容をバッファに入れておく。
//		strText = edit.getText().toString();//ファイルの更新を検知できなくなるので、strTextには入れない
		
		//正規表現で検索する
		Pattern pattern = Pattern.compile(word,Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
//		Matcher matcher = pattern.matcher(strText);
		Matcher matcher = pattern.matcher(edit.getText().toString());
		int i= -1;
		int l = 0;
		int start = -1;
		
		while(matcher.find()){//先頭から検索
			i = matcher.start();
			l = matcher.group().length();			

			if(i >= initPosition){//通りすぎて
				if(start > -1){//、かつ、見つかっていたらbreak 通り過ぎて見つかっていなかったら最後まで。
					break;
				}else{//initPositionを最後の位置に持っていく
					initPosition = maxlength;
				}
			}
			
			start= i;
			wordlength = l;
		}		
		//正規表現で検索する。ここまで

		
		// 見つからない場合はトースト表示
		if(start < 0){
			//Toast.makeText(TextEdit.this, R.string.notFound, Toast.LENGTH_LONG).show();
			ToastMaster.makeTextAndShow(TextEdit.this, R.string.notFound, Toast.LENGTH_LONG);
		}else{
			// 見つかった場合はカーソル位置を移動
			//edit.setSelection(i);
			edit.setSelection(start, start+wordlength);
		}
		// edit側にフォーカスを移動する。
		edit.requestFocus();

	}


	//文字コード変換(S-JIS,EUC,JIS -> UTF)
	public String convertCharEncodeingJISAutoDetect(byte[] b){
		String s = new String(b);
		try{
			s = new String(b,"JISAutoDetect");
//			s = new String(b,"Shift_JIS");
		}catch(Exception e){
			e.printStackTrace();
			showMessage(e.toString());
		}
		return s;

	}

	

    // close buttomを押したときの処理
    private void closeFile(){

    	
    	
        //message digest 修正されているかどうか確かめるためにdigestを取得して比較する。
        String tmpDigest = getMessageDigest();

//		System.out.println("digest:" + tmpDigest.toString() + ":" + messageDigest.toString());
        
    	if(tmpDigest != null && tmpDigest.equals(messageDigest)){
    		// not modified
    		strText="";
    		finish();
    	}else{
    		
    		if(autoSaveFlag){
    	    	//autoSaveFlagがtrueなら保存処理する。そうでなければダイアログを表示して決める。
        		closeAfterSaveFlag = true;//保存が正常に完了したらCloseする
            	autoSaveFile();
            	
    		}else{
    			new AlertDialog.Builder(TextEdit.this)
    			.setTitle(R.string.alert_close_modified_file)
    			.setCancelable(true)
    			.setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int whichButton) {
    					saveFile();
    					closeAfterSaveFlag = true;//保存が正常に完了したらCloseする
    					//		    		finish();//ここでfinishすると、保存が完了する前にActivityが終了してしまう。


    				}
    			})
    			.setNeutralButton(R.string.alert_dialog_no,
    					new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int whichButton) {
    					strText="";
    					finish();
    				}
    			})
    			.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int whichButton) {
    					//Do Nothing.
    				}
    			})
    			.show(); //ダイアログ表示;
    		}
    	}

    }
    
    private void saveFile(){
		needToRenameForSyncTitle = false;//初期化しておく

		
		//			if( !filepath.equals("") ) {
		if( !aFile.isDirectory() ) {

			//既存ファイルなら後でrename
			//FileWriteThreadでrenameするためにフラグをたてておく
			if(syncTitleFlag)needToRenameForSyncTitle = true; 
			
			
			strText = edit.getText().toString();
			doFileWrite();
//			break;
		}
		else if(aFile.isDirectory() && syncTitleFlag){
			//新規ファイルでsyncTitleだったらファイル名をTitleから作ってそのファイル名で保存する。

			strText = edit.getText().toString();

			filepath = getFilenameFromHeadLineNonDuplicate();
			aFile = new File(filepath);
//			TextEdit.this.setTitle(getString(R.string.app_name) + " - " + aFile.getName());
			setTitleBarText(aFile.getName());
//			System.out.println("(TextEdit)filepath=" + filepath);

			doFileWrite();
//			break;	
		}
		else{
		// PASS THROUGH
		saveFileAsNewFile();
		}
		
    }

    //自動保存用
    // onPause(),closeFile()の中で呼び出される
    private void autoSaveFile(){
    	
    	
    	needToRenameForSyncTitle = false;//初期化しておく

		//			if( !filepath.equals("") ) {
    	
		if(aFile.isDirectory()){
			//新規ファイルならTitleからファイル名を作って保存する。

			strText = edit.getText().toString();

			filepath = getFilenameFromHeadLineNonDuplicate();
			aFile = new File(filepath);
//			TextEdit.this.setTitle(getString(R.string.app_name) + " - " + aFile.getName());
			setTitleBarText(aFile.getName());
//			System.out.println("(TextEdit)filepath=" + filepath);

			doFileWrite();
//			break;	
		}else{//新規ファイルではない場合
			
			//ファイル名とタイトルを同期させる場合
			//FileWriteThreadでrenameするためにフラグをたてておく
			if(syncTitleFlag)needToRenameForSyncTitle = true; 
			
			
			strText = edit.getText().toString();
			doFileWrite();
//			break;
			
		}
    	
		
    }

    
    //別名でファイルを保存
    private void saveFileAsNewFile(){
    	
		Intent intent = new Intent(TextEdit.this, SelectFileName.class);

		needToRenameForSyncTitle = false;//初期化しておく
		String param_filepath;
		strText = edit.getText().toString();
		if( aFile.isDirectory() ){
			String s = getHeadLineForFilename();
			if(s.length() > 0){
				param_filepath = filepath + "/" + s + ".txt";					
			}else{
				param_filepath = filepath + "/" + NewFileName;					
			}
		}else{
			param_filepath = filepath;
		}
		//System.out.println("(TextEdit)filepath=" + param_filepath);

		onPauseBeforeSelectSaveFile = true; //intent for avoid aute save file
		intent.putExtra(SelectFileName.INTENT_MODE, SelectFileName.MODE_SAVE);
		intent.putExtra(SelectFileName.INTENT_FILEPATH, param_filepath);
		intent.putExtra(SelectFileName.INTENT_ENCRYPT, isEncrypt);

		startActivityForResult(intent, SHOW_FILELIST_SAVE);

    }
    
    
    private String getHeadLineForFilename(){
		String headLine = "";

		//
		if(strText.length() > 0){
			int i = strText.indexOf('\n');
			if(i >= 0 ){
//				if(i > 32)i=32;//32文字以下にする。
				headLine = strText.substring(0, i);
			}else{
				headLine = strText;
			}
			//文字列の前後からスペース(全角半角、改行、タブなど)を削除
			//s.trim();
			headLine = headLine.replaceAll("^[\\s　]*", "").replaceAll("[\\s　]*$", "");
			//文字列の前後から/を削除 // :\/:,;*?"<>|
			headLine = headLine.replaceAll("[/:,;*?\"<>|]", ".");
			headLine = headLine.replaceAll("\\\\", ".");
		}
		return headLine;
    }
    
    private String getFilenameFromHeadLineNonDuplicate(){
    	String currentDir = "";
    	String extention = ".txt";
    	
    	if(aFile.isDirectory()){
    		currentDir = aFile.getPath();
    	}else{
    		currentDir = aFile.getParent();
    	}
    	String s = getHeadLineForFilename();
    	if(s.length() == 0)s = "noTitle";
    	
    	if(isEncrypt)extention = ".chi";
  
    	String dstfilename = currentDir + "/" + s + extention;
    	
    	
    	File destFile = new File(dstfilename);
    	
    	//ファイル名を作成して、それが既に存在する間は(n)をつけて増やしていく
    	
//		System.out.println("getFilenameFromHeadLineNonDuplicate:" + filepath);
//		System.out.println("getFilenameFromHeadLineNonDuplicate:" + dstfilename);
		
		int i = 0;
		while(destFile.exists() && !filepath.equals(dstfilename)){ //存在してかつ自分自身ではない場合は(i)をつける。
			i++;
			dstfilename = currentDir + "/" + s +"(" + i +")" + extention;
			destFile = new File(dstfilename);
			
//			System.out.println("getFilenameFromHeadLineNonDuplicate:" + filepath);
//			System.out.println("getFilenameFromHeadLineNonDuplicate:" + dstfilename);
			
		}
		
       	return dstfilename;
    }
    
    
    private String getMessageDigest(){
    	
        //memo内容更新を確かめるためのMessageDigest
    	try{
    		MessageDigest md = MessageDigest.getInstance("MD5");
//    		md.update(strText.getBytes());
    		md.update(edit.getText().toString().getBytes());
//    		System.out.println("message digest:" + md.digest().toString());
    		byte[] b = md.digest();
    		
    		//Stringとして保持しておいたほうが処理しやすいので変換
    	    StringBuffer s = new StringBuffer();
    	    for (int i = 0; i < b.length; i++) {
    	        int d = b[i];
    	        d += d < 0 ? 256 : 0; // byte 128-255
    	        if (d < 16) { //0-15 16
    	            s.append("0");
    	        }
    	        s.append(Integer.toString(d, 16));
    	    }
    	    return s.toString();
    	    
    		
    		
    	}
		catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(TextEdit.this, "Fail to get message digest.", Toast.LENGTH_SHORT).show();
	
    	}
		return null;

    }
    
    // strText - binText間の変換
    // Charasetの変更と改行コードの変更を行う。
    private void setBinTextToStrText() throws Exception{
		strText = new String(binText,charsetName);//バイナリデータをstrTextにセット
		
		if(lineBreakSetting == LINEBREAK_AUTO){//AUTOなら設定を自動検知して設定する
			lineBreakSetting = checkLineBreak();
		}

		/*
		//LFでなければLFに置き換える。
		if(lineBreakSetting == LINEBREAK_CRLF){
			strText = strText.replaceAll("\r\n", "\n");
		}else if(lineBreakSetting == LINEBREAK_CR){
			strText = strText.replaceAll("\r", "\n");			
		}else if(lineBreakSetting == LINEBREAK_LF){
			// don't anything
		}
		*/
		//無条件にCRLF,CRはLFに変更
		strText = strText.replaceAll("\r\n", "\n").replaceAll("\r", "\n");	
	
    	
    }
    private void setStrTextToBinText() throws Exception{
		//LFでなければ指定の改行コードに置き換える。
		if(lineBreakSetting == LINEBREAK_CRLF){
			strText = strText.replaceAll("\n", "\r\n");
		}else if(lineBreakSetting == LINEBREAK_CR){
			strText = strText.replaceAll("\n", "\r");			
		}else if(lineBreakSetting == LINEBREAK_LF){
			// don't anything
		}		
		binText = strText.getBytes(charsetName);//strTextをバイナリデータに変換する。
    	
    	
    }
    //strTextを検索して、改行コードを判別する。
    
    private int checkLineBreak(){
    	if(strText.contains("\r\n")){ //CRLF
//			Toast.makeText(TextEdit.this, "Line Break: CRLF", Toast.LENGTH_SHORT).show();
        	return LINEBREAK_CRLF;
    	}else if(strText.contains("\r")){
//			Toast.makeText(TextEdit.this, "Line Break: CR", Toast.LENGTH_SHORT).show();
        	return LINEBREAK_CR;    			
    	}else{
//			Toast.makeText(TextEdit.this, "Line Break: LF", Toast.LENGTH_SHORT).show();
    		return LINEBREAK_LF;//判断つかなければlinuxのLF
    	}
    }
    
    //カーソル位置をセットする
    private void setSelection(){
    	int length = strText.length();
    	if(selStart > length)selStart = length;
    	if(selStop > length)selStop = length;
    	edit.setSelection(selStart, selStop);
    	
    }
    
    //editTextに文字列をセットする
    private void setStringToEditText(){

    	if(autoLinkFlag){
    		spanText = new SpannableString(strText);
    		edit.setText(spanText);
//    		setMySpanAll();//setTextしたときにonTextChangedが呼ばれるので不要
    	}else{
    		edit.setText(strText);    		
    	}
    	
    	//sizecheck(truncated check)
    	int size_str = strText.length();
    	int size_editbox = edit.length();
    	if(size_str != size_editbox){
    		showMessage(getString(R.string.alert_text_trancated) + "\n" + size_str + " -> " + size_editbox);
    	}

    }
    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
    		ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	if(hideIMEFlag)menu.add(0, MENUID_SHOW_IME, 0,  R.string.menu_show_ime);
    }

    //メニューのアイテムが選択された際に起動される
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	super.onContextItemSelected(item);
    	
    	switch (item.getItemId()) {
		case MENUID_SHOW_IME:
			showIME();
			break;
		default:
			break;
		}

    	return true;
    }
    
	private void hideIME(){
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
		edit.setRawInputType(0);
		imm.hideSoftInputFromWindow(edit.getWindowToken(),0);
		
		hideIMEFlag = true;
	
	}
	private void showIME(){
		// 表示方法  
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
		int type = InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE;
		edit.setInputType(type);
		imm.showSoftInput(edit, 0);  

		hideIMEFlag = false;

	}

	
	private void setResultForActionGetContent(){
	//for ACTION_GET_CONTENT
		Intent intent = getIntent();
		if ( intent != null ){
			if ( Intent.ACTION_GET_CONTENT.equals(intent.getAction())){
				if ( aFile != null && aFile.exists()){
					intent.setData(Uri.parse("file://" + aFile.getAbsolutePath()));
					setResult( RESULT_OK ,intent );
				}
			}
		}
	}
    
	
	public class myTextWatcher implements TextWatcher{
		
		public void afterTextChanged(Editable s) {
//	    	Log.d("UITextWatcher", "afterTextChanged :" + s.toString());
//			setTitleBarCount(s.length());
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {			
//	    	Log.d("UITextWatcher", "beforeTextChanged:start=" + start + ",count=" + count +",s=" + s );			
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

//			int length = s.length();
//			setTitleBarCount(length);
						
			if(autoLinkFlag){
				//	    	Log.d("UITextWatcher", "onTextChanged    :start=" + start + ",before=" + before + ",count=" + count +",s=" + s );
				//Log.d("UITextWatcher", "onTextChanged    :start=" + start + ",before=" + before + ",count=" + count );

				int length = s.length();

				//startのある行からstart+countのある行までを対象にする。
				int line_start=0;
				for(int i = start-1 ; i >= 0 ;i--){
					if(s.charAt(i) == '\n'){
						line_start =  i+1;//見つかったら改行の次が行の開始index
						break;
					}
				}
				int line_end = start+count;
				for(int i = start+count ; i<length ;i++){
					line_end =  i;
					if(s.charAt(i) == '\n'){
						break;
					}
				}

//				Log.d("UITextWatcher", "onTextChanged    :line_start,end=" + line_start + "," + line_end );
				resetMySpan(line_start,line_end);
			}
	    }
		
	}
	
    private void setMySpanAll(){
    	Spannable buffer =     (Spannable) edit.getText();	

    	if(autoLinkWeb){
    		Matcher matcherURL = MyUtil.WEB_URL_PATTERN.matcher(buffer);
    		while(matcherURL.find()){
    			buffer.setSpan(new myClickableSpan(matcherURL.group(),edit,this),matcherURL.start(),matcherURL.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    		}
    	}
				
    	if(autoLinkTel){
    		Matcher matcherTEL = MyUtil.PHONE_PATTERN.matcher(buffer);
    		while(matcherTEL.find()){
    			buffer.setSpan(new myClickableSpan(matcherTEL.group(),edit,this),matcherTEL.start(),matcherTEL.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    		}
    	}

    	if(autoLinkEmail){
    		Matcher matcherMAIL = MyUtil.EMAIL_ADDRESS_PATTERN.matcher(buffer);
    		while(matcherMAIL.find()){
    			buffer.setSpan(new myClickableSpan(matcherMAIL.group(),edit,this),matcherMAIL.start(),matcherMAIL.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    		}
    	}
    }
    
    private void resetMySpan(int start, int end){
    	
    	Spannable buffer =     (Spannable) edit.getText();	
		ClickableSpan[] link = buffer.getSpans(start, end, ClickableSpan.class);
		
		CharSequence subBuffer = buffer.subSequence(start, end);
		
		
		for(ClickableSpan s : link){
//			Log.d("UITextWatcher", "resetMySpan:removeSpan " + s.toString() );
			buffer.removeSpan(s);
		}

		//検索対象部分だけsubstringで取り出して、offset計算してやる。
		//文字数カウンターはonbeforeEdit
		if(autoLinkWeb){
			Matcher matcherURL = MyUtil.WEB_URL_PATTERN.matcher(subBuffer);
			while(matcherURL.find()){
				int find_start = matcherURL.start() + start;
				int find_end   = matcherURL.end()   + start;
//				Log.d("UITextWatcher", "resetMySpan:matcherURL.find()");
				buffer.setSpan(new myClickableSpan(matcherURL.group(),edit,this),find_start,find_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}				
		if(autoLinkTel){
			Matcher matcherTEL = MyUtil.PHONE_PATTERN.matcher(subBuffer);
			while(matcherTEL.find()){
				int find_start = matcherTEL.start() + start;
				int find_end   = matcherTEL.end()   + start;
//				Log.d("UITextWatcher", "resetMySpan:matcherTEL.find()");
				buffer.setSpan(new myClickableSpan(matcherTEL.group(),edit,this),find_start,find_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		
		if(autoLinkEmail){
			Matcher matcherMAIL = MyUtil.EMAIL_ADDRESS_PATTERN.matcher(subBuffer);
			while(matcherMAIL.find()){
				int find_start = matcherMAIL.start() + start;
				int find_end   = matcherMAIL.end()   + start;
//				Log.d("UITextWatcher", "resetMySpan:matcherEmail.find()" + find_start + "," + find_end + " start,end=" + start + "," + end);
				buffer.setSpan(new myClickableSpan(matcherMAIL.group(),edit,this),find_start,find_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}


    }

    private void setTitleBarText(String filename){
    	mFileNameOnTitleBar = filename;
    	TextEdit.this.setTitle(mAppName + " - " + mFileNameOnTitleBar);
//      	TextEdit.this.setTitle(mAppName + " - " + mFileNameOnTitleBar + "   (" + mCharaCount +")" );
    }
    private void setTitleBarCount(int count){
    	mCharaCount = count;
    	TextEdit.this.setTitle(mAppName + " - " + mFileNameOnTitleBar + "   (" + mCharaCount +")" );
    }
    
	/**
	 * Dialogを表示
	 *
	 * @param msg 表示するメッセージ
	 */
	private void showMessage(String msg){
		new AlertDialog.Builder(this)
		.setMessage(msg)
		.setNeutralButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			// この中に"OK"時の処理をいれる。
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		})
		.show();
	}

	private void showMessageAndClose(String msg){
		new AlertDialog.Builder(this)
		.setMessage(msg)
		.setNeutralButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			// この中に"OK"時の処理をいれる。
			public void onClick(DialogInterface dialog, int whichButton) {
				finish();
			}
		})
		.show();
	}
    
	
}
