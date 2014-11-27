package jp.gr.java_conf.hatalab.mnv;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import android.widget.Toast;

public class GrepActivity extends Activity {
	
	
	private EditText mEdit;
//	private TextView mEdit;
//	private Button mFindBtnForward;
//	private Button mFindBtnBackward;
	private ImageView mFindBtnForward;
	private ImageView mFindBtnBackward;
//	private EditText mEditSearchWord;
	private ImageView mFindButton;

	private float mFontSize = 18;
    private String mCharsetName = "UTF-8";
    private String mSearchBaseDir = "/sdcard";
    private Boolean mListFoldersFirstFlag = false;
    
    private boolean mAutoLinkFlag = false;
    private boolean mAutoLinkWeb  = false;
    private boolean mAutoLinkEmail= false;
    private boolean mAutoLinkTel  = false;

    
//    private int    mCurrentPosition = 0;
//    private boolean  mCurrentFileIsOpened = false;
//    private String mStringData;
    private DirList2 dirList;
    
    
    
  
    //検索条件
    private String  mSearchWord = "";
    private boolean mIncludeEncryptedFile = false;
    private int     mSearchDirection = FORWARD;
    private String  mCurrentFilePath = "";

	final static public int FORWARD = 0;
	final static public int BACKWARD = 1;
    
	final static String FILENAME_TEXT = "(.*\\.txt)";
	final static String FILENAME_TEXT_CHI = "(.*\\.txt|.*\\.chi)";
	
	
	private static final int MENUID_CLOSE = Menu.FIRST + 1;		// メニューID：読み込み
	private static final int MENUID_EDIT  = Menu.FIRST + 2;		// メニューID：EDIT

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		initViews();
		initConfig();
		initParams();	
   		initListener();
   		
   		getSearchWordDialog();//検索ワード入力ダイアログ
	}
	
	private void initViews(){
		setContentView(R.layout.grepbox);
		mEdit = (EditText)findViewById(R.id.editbox);
//		mFindBtnForward = (Button)findViewById(R.id.btnFindForward);
//		mFindBtnBackward = (Button)findViewById(R.id.btnFindBackward);
		mFindBtnForward = (ImageView)findViewById(R.id.btnFindForward);
		mFindBtnBackward = (ImageView)findViewById(R.id.btnFindBackward);

//		mEditSearchWord = (EditText)findViewById(R.id.editSearchWord);

		mFindButton = (ImageView)findViewById(R.id.FindBtn);

		
		
//		mEdit.setText("enter keyword");
		hideIME();
//		showIME();

		
	}
	
	

	
	private void initConfig(){
		//PasswordBoxのタイマーセット
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String timerString = sharedPreferences.getString(getText(R.string.prefPWResetTimerKey).toString(), "3");
        PasswordBox.setTimerVal(Integer.parseInt(timerString));
        
		//charset name セット
        mCharsetName = sharedPreferences.getString(getText(R.string.prefCharsetNameKey).toString(), "utf-8");
                
        //fontsizeセット
        mFontSize = sharedPreferences.getFloat(getText(R.string.prefFontSizeKey).toString(), mFontSize);
		mEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP,mFontSize);
     
        //ListFoldersFirst
        mListFoldersFirstFlag = sharedPreferences.getBoolean(getText(R.string.prefListFoldersFirstKey).toString(), false);

        
        //autoLink setting
        mAutoLinkWeb  = sharedPreferences.getBoolean(getText(R.string.prefAutoLinkWebKey).toString(),   false);
        mAutoLinkEmail= sharedPreferences.getBoolean(getText(R.string.prefAutoLinkEmailKey).toString(), false);
        mAutoLinkTel  = sharedPreferences.getBoolean(getText(R.string.prefAutoLinkTelKey).toString(),   false);
        if(mAutoLinkWeb || mAutoLinkEmail || mAutoLinkTel){
        	mAutoLinkFlag = true;
        }
	}

	private void initParams(){
		//intentから 検索のベースディレクトリとファイルを取得する。
		Intent intent = getIntent();
   		String filepath = intent.getStringExtra("FILEPATH");

   		//ファイルパスが存在しなかったら終了
   		if(filepath == null || filepath.equals("")){
   			Toast.makeText(this, "file path is invalid.", Toast.LENGTH_SHORT).show();
   			finish();
   		}

   		
   		File file = new File(filepath);
   		//ファイル/パスが存在しなかったら終了
   		if(!file.exists()){
   			Toast.makeText(this, "file/direcotry is not exist.", Toast.LENGTH_SHORT).show();
   			finish();
   		}
   		
   		dirList = new DirList2(filepath);
   		if(mListFoldersFirstFlag)dirList.setFoldersFirst();
   		
   		if(file.isDirectory()){
   			mSearchBaseDir = file.getAbsolutePath();
   		}else{
   			String parent  = file.getParent();
   			if(parent != null && !parent.equals("")){
   	   			mSearchBaseDir = file.getParent();    				
   			}else{
   	   			mSearchBaseDir = "/";
   			}
   		}
//    	Log.d("GrepActivity", "initParams:filepath = " + filepath);

   		
	}
	

	
	private void initListener(){
    	
		mFindBtnForward.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(mSearchWord.length()>0){
					// 本文を検索する。
					mSearchDirection = FORWARD;
					doSearchWord(mSearchWord,mIncludeEncryptedFile);
				}else{
					getSearchWordDialog();//検索ワード入力ダイアログ
				}
			}

		});

		mFindBtnBackward.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(mSearchWord.length()>0){
					// 本文を検索する。
					mSearchDirection = BACKWARD;
					doSearchWord(mSearchWord,mIncludeEncryptedFile);
				}else{
					getSearchWordDialog();//検索ワード入力ダイアログ
				}

			}

		});
		
		mFindButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// 検索情報入力Dialogを表示する。
		   		getSearchWordDialog();//検索ワード入力ダイアログ

			}
		});
		
		
		mEdit.setOnTouchListener(new OnTouchListener(){
		    //@Override
		    public boolean onTouch(View v, MotionEvent event) {

		    	if(mAutoLinkFlag){
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
//		    	Log.d("textEdit", "onTouchEvent ex:" + ex +",ey:" + ey);

		        return false;
//		        return true;
		    
		    }
		});

	}

	
	
	private void search(String word, int direction){
		// direction: FORWARD/BACKFARD
		// if current file has not been read, read file
		if(mIncludeEncryptedFile){
			dirList.setFilenameFilter(FILENAME_TEXT_CHI);
		}else{
			dirList.setFilenameFilter(FILENAME_TEXT);
		}
		
		
		if(searchTextView(word,direction)){//現在のviewを探しあれば表示
			mEdit.requestFocus();//フォーカスを当てる
		}else{//現在のviewにkeywordがなければ次のファイルを探して、表示する。
			GrepTask task;
			task = new GrepTask(this);
			task.execute(word,String.valueOf(direction),mCurrentFilePath);
			
		}
		
	}
	
	// create Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENUID_CLOSE, 0, R.string.menu_close)
		.setShortcut('0', 'c')
		.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		menu.add(0, MENUID_EDIT, 0, R.string.menu_edit)
		.setShortcut('4', 'f')
		.setIcon(android.R.drawable.ic_menu_edit);
		
		
		
		return true;
	}

	// メニュー押下時のイベント処理
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		super.onMenuItemSelected(featureId, item);

		switch(item.getItemId()) {
		case MENUID_CLOSE:		// 終了
			finish();
			break;


		case MENUID_EDIT:		// 検索
			if(mCurrentFilePath.equals("")){
				
			}else{

				Intent intent = new Intent(this, TextEdit.class);
				intent.putExtra("FILEPATH", mCurrentFilePath );
				intent.putExtra("SELSTART", mEdit.getSelectionStart());
				intent.putExtra("SELSTOP" , mEdit.getSelectionEnd());
				//			intent.putExtra("DESCRIPTION", item.getDescription());
				startActivity(intent);
			}
			break;

			
		default:
			break;
		}
		return true;
	}

	
	/**
	 * input keyword
	 */
	private void getSearchWordDialog(){

		//コンテキストからインフレータを取得
		LayoutInflater inflater = LayoutInflater.from(this);

		//レイアウトXMLからビュー(レイアウト)をインフレート
		final View inputView = inflater.inflate(R.layout.grep_search_word, null);

		final CheckBox encryptCheckBox = (CheckBox)inputView.findViewById(R.id.checkbox_id);
		final EditText keywordEditText = (EditText)inputView.findViewById(R.id.dialog_keyword);
		final RadioGroup btnDirection   = (RadioGroup)inputView.findViewById(R.id.radiogroup_id);
		if( mSearchWord != null && !mSearchWord.equals("") ){
			keywordEditText.setText(mSearchWord);
		}
		
		if(PasswordBox.getPassDigest() == null){
			encryptCheckBox.setChecked(false);
//			dirList.setFilenameFilter(FILENAME_TEXT);
		}else{
			encryptCheckBox.setChecked(true);
//			dirList.setFilenameFilter(FILENAME_TEXT_CHI);
		}

		
		//ダイアログを構成
		final AlertDialog alertDialog  = new AlertDialog.Builder(this)
		.setTitle(R.string.input_searchword)
//		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			//		    @Override
			public void onClick(DialogInterface dialog, int which) {
				//OKボタンが押下された時に入力された文字を設定する
//				CheckBox encryptCheckBox = (CheckBox)inputView.findViewById(R.id.checkbox_id);
//				RadioGroup btnDirection   = (RadioGroup)inputView.findViewById(R.id.radiogroup_id);
//				EditText keywordEditText = (EditText)inputView.findViewById(R.id.dialog_keyword);
				
				boolean encrypt = encryptCheckBox.isChecked();
				String keyword = keywordEditText.getText().toString();
				int direction = btnDirection.getCheckedRadioButtonId();
				if(direction == R.id.radiobutton_forward){
					mSearchDirection = FORWARD;
				}else{
					mSearchDirection = BACKWARD;					
				}
				
				
				if(keyword.length()<1){
					//keywordが空だったらもう一度
					//Toast.makeText(GrepActivity.this, R.string.seachword_empty, Toast.LENGTH_LONG).show();
					ToastMaster.makeTextAndShow(GrepActivity.this, R.string.seachword_empty, Toast.LENGTH_SHORT);
					getSearchWordDialog();
				}else if(regexCompileOK(keyword)){
					
					mSearchWord = keyword;
					mIncludeEncryptedFile = encrypt;					
					
					doSearchWord(keyword,encrypt);
				}


			}
		}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//TextEditを閉じる.
				//finish();
			}
		}).setOnCancelListener(new DialogInterface.OnCancelListener() {
//			  @Override
			public void onCancel(DialogInterface dialog) {
//				Log.v("TextEdit","onCancel");
				// キャンセルボタンと戻るボタンが押された時の処理をここに記述する。
				//finish();
			}
		})
		.setView(inputView)
		.create();

		keywordEditText.setOnKeyListener(new View.OnKeyListener(){
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// Enterを離したときに検索処理を実行
				if (event.getAction() == KeyEvent.ACTION_UP
						&& keyCode == KeyEvent.KEYCODE_ENTER) {
					// 本文を検索する。
					boolean encrypt = encryptCheckBox.isChecked();
					String keyword = keywordEditText.getText().toString();
					int direction = btnDirection.getCheckedRadioButtonId();
					if(direction == R.id.radiobutton_forward){
						mSearchDirection = FORWARD;
					}else{
						mSearchDirection = BACKWARD;					
					}

					if(keyword.length() > 0 && regexCompileOK(keyword)){

						InputMethodManager inputMethodManager =   
							(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
						inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);  
						mSearchWord = keyword;
						mIncludeEncryptedFile = encrypt;					
						
						doSearchWord(keyword,encrypt);
						alertDialog.dismiss();//閉じる
					}

					return true;
				}
				return false;
			}
		});

		
		alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		alertDialog.show(); //ダイアログ表示

		keywordEditText.requestFocus();

	    
	}

	private void doSearchWord(String keyword,boolean encrypt){
		
		
		if(encrypt && PasswordBox.getPassDigest() == null ){
			//パスワードを入力してから開始
			inputPasswordAndSearchWord();
		}else{//検索開始
			search(mSearchWord,mSearchDirection);
		}
		return;
	}
	
	private void inputPasswordAndSearchWord(){
		//コンテキストからインフレータを取得
		LayoutInflater inflater = LayoutInflater.from(this);
		//レイアウトXMLからビュー(レイアウト)をインフレート
		final View inputView = inflater.inflate(R.layout.input_pass, null);
		
		//ダイアログを構成
		final AlertDialog alertDialog  = new AlertDialog.Builder(this)
		.setTitle(R.string.pass_input_text)
//		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//OKボタンが押下された時に入力された文字を設定する
				EditText passEditText = (EditText)inputView.findViewById(R.id.dialog_edittext);
				String pass = passEditText.getText().toString();
				if(pass.length()>0){
					PasswordBox.setPassword(pass);
					//検索開始
					search(mSearchWord,mSearchDirection);
					
				}else{
					//passが空だったら終了する
					//	finish();
					Toast.makeText(GrepActivity.this, R.string.password_empty, Toast.LENGTH_LONG).show();
					inputPasswordAndSearchWord();
				}

			}
		}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//
				//finish();
			}
		}).setOnCancelListener(new DialogInterface.OnCancelListener() {
//			  @Override
			public void onCancel(DialogInterface dialog) {
//				Log.v("TextEdit","onCancel");
				// キャンセルボタンと戻るボタンが押された時の処理をここに記述する。
				//finish();
			}
		})
		.setView(inputView)
		.create();

		alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		alertDialog.show(); //ダイアログ表示

	}
	
	
	
	//現在のviewの中を検索する。見つかればSelectionをsetして、trueをかえす。
	//見つからなければfalseを返す
	private boolean searchTextView(String word, int order){
		
		//forword
		if(order == FORWARD){
			int start = mEdit.getSelectionEnd();
			GrepMatchInfo sel = MyUtil.searchWord(word, mEdit.getText().toString(),start);
			if(sel != null){
				mEdit.setSelection(sel.start, sel.stop);
				return true;
			}			
		}else{//BACKWARD
			int start = mEdit.getSelectionStart();
			GrepMatchInfo sel = MyUtil.searchWordBackward(word, mEdit.getText().toString(),start);
			if(sel != null){
				mEdit.setSelection(sel.start, sel.stop);
				return true;
			}						
		}
		return false;
	}
	
		
	
	
	
	public String getNextFile(){
		if(dirList == null){
			return "";
		}else{
			return dirList.nextFile();
			
		}
	}
	public String getPrevFile(){
		if(dirList == null){
			return "";
		}else{
			return dirList.previousFile();
			
		}
	}
	public void dirListRevert(){
		if(dirList != null){
			dirList.revert();
		}
	}
	
	
	public void stopDirList(){
		dirList.setCancel();
	}

	
	public void setFileInfo(FileInfo fInfo){
		if(fInfo != null){
			String filename = fInfo.getFile().getName();
			if(filename == null || filename.equals("")){
				//			this.setTitle(getString(R.string.app_name));			
			}else{
				//String name = fInfo.getFile().getName();
				mCurrentFilePath = fInfo.getFile().getAbsolutePath();
				String name = mCurrentFilePath.replace(mSearchBaseDir+"/", "");//rootの場合は / がダブりreplaceされないがrootなので、replaceされなくてOK
				
				this.setTitle(name);			
				mEdit.setText(fInfo.getData());
				if(mAutoLinkFlag)setMySpanAll();
				mEdit.setSelection(fInfo.getSelStart(), fInfo.getSelEnd());
				mEdit.requestFocus();//フォーカスを当てる

			}
		}else{
			//見つからなければdirListをリセットするか、現在のファイルまで巻き戻す。
			//GrepTask内でのエラー発生時も現在のフィイルまで巻き戻す。
			if(mCurrentFilePath.equals("")){
				initParams();
			}else{
				dirList.setCurrentFile(mCurrentFilePath);
			}
	
		}
	}

	
	public String getCharsetName(){
		return mCharsetName;
	}

	public String getSearchWord(){
		return mSearchWord;
	}

	public boolean IncludeEncryptedFile(){
		return mIncludeEncryptedFile;
	}

	public int getSearchDirection(){
		return mSearchDirection;
	}

	public String getCurrentFilePath(){
		return mCurrentFilePath;
	}
	
	
	private void hideIME(){
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
		mEdit.setRawInputType(0);
		imm.hideSoftInputFromWindow(mEdit.getWindowToken(),0);  
	
	}
	private void showIME(){
		// 表示方法  
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
		int type = InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE;
		mEdit.setInputType(type);
		imm.showSoftInput(mEdit, 0);  


	}
	
    // キーイベント発生時、呼び出されます
    private boolean mBackKeyDown = false;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(event.getKeyCode()){
    		case KeyEvent.KEYCODE_BACK:
    			mBackKeyDown = true;
    			if(true){//back keyは無視するACTION_UPの時に処理をする。
    				return true;
    			}
    			break;
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

	
	
    private void setMySpanAll(){
    	Spannable buffer =     (Spannable) mEdit.getText();	

    	if(mAutoLinkWeb){
    		Matcher matcherURL = MyUtil.WEB_URL_PATTERN.matcher(buffer);
    		while(matcherURL.find()){
    			buffer.setSpan(new myClickableSpan(matcherURL.group(),mEdit,this),matcherURL.start(),matcherURL.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    		}
    	}
				
    	if(mAutoLinkTel){
    		Matcher matcherTEL = MyUtil.PHONE_PATTERN.matcher(buffer);
    		while(matcherTEL.find()){
    			buffer.setSpan(new myClickableSpan(matcherTEL.group(),mEdit,this),matcherTEL.start(),matcherTEL.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    		}
    	}

    	if(mAutoLinkEmail){
    		Matcher matcherMAIL = MyUtil.EMAIL_ADDRESS_PATTERN.matcher(buffer);
    		while(matcherMAIL.find()){
    			buffer.setSpan(new myClickableSpan(matcherMAIL.group(),mEdit,this),matcherMAIL.start(),matcherMAIL.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    		}
    	}
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
	
	
	

}
