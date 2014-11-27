package jp.gr.java_conf.hatalab.mnv;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectFileName extends ListActivity{

	// インテント定数定義
	public static final String INTENT_DIRPATH = "DIRPATH";		// ディレクトリパス名
	public static final String INTENT_FILENAME = "FILENAME";	// ファイル名
	public static final String INTENT_ORG_FILENAME = "ORG_FILENAME";	// ファイル名
	public static final String INTENT_FILEPATH = "FILEPATH";	// フルパスファイル名
	public static final String INTENT_ENCRYPT = "ENCRYPT";	// フルパスファイル名
	public static final String INTENT_MODE = "MODE";			// モード
//	public static final String MODE_OPEN = "OPEN";				// モード：ファイルを開く
	public static final String MODE_SAVE = "SAVE";				// モード：ファイルを保存
	public static final String MODE_COPY = "COPY";				// モード：ファイルを開く
	public static final String MODE_MOVE = "MOVE";				// モード：ファイルを開く

	private static final int MODEID_NONE = 0;		// MODE ID：
	private static final int MODEID_SAVE = 1;		// MODE ID：
	private static final int MODEID_COPY = 2;		// MODE ID：
	private static final int MODEID_MOVE = 3;		// MODE ID：
	private int modeID = MODEID_NONE;
	
	// その他定数定義
//	private static final String TITLE_OPEN = "Open";
//	private static final String TITLE_SAVE = "Save as..";
//	private static final String TITLE_COPY = "copy to..";
//	private static final String TITLE_MOVE = "move to..";

//	private final String TITLE_SAVE = getText(R.string.menu_save_as).toString();
//	private final String TITLE_COPY = getText(R.string.longclick_menu_copy).toString();
//	private final String TITLE_MOVE = getText(R.string.longclick_menu_move).toString();

	private String DirPath;
	private String filename;
	private boolean encryptFlag = false;
	private List<String> items = null;
	
	private boolean mExistEncryptCheckBox = true;

	private EditText mEdtFileName;
	
	@Override public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.select_file);
		
		Button btnOK = (Button)findViewById(R.id.btnOK);
		btnOK.setText(R.string.alert_dialog_ok);
		
		Button btnCancel = (Button)findViewById(R.id.btnCancel);
		btnCancel.setText(R.string.alert_dialog_cancel);

		TextView txtTitle = (TextView)findViewById(R.id.txtTitle);
		CheckBox encryptCheck = (CheckBox)findViewById(R.id.encryptCheckBox);

		
		
		mEdtFileName = (EditText)findViewById(R.id.edtFileName);


		Bundle extras = getIntent().getExtras();
		// タイトルの設定
		if (extras != null) {
			filename = extras.getString(INTENT_FILEPATH);
			File aFile = new File(filename);
			DirPath = aFile.getParent();
			if(DirPath == null || DirPath == ""){
				DirPath="/";
			}

			mEdtFileName.setText(aFile.getName());

			
			
			if( extras.getString(INTENT_MODE).equals(MODE_SAVE) ) {
				modeID = MODEID_SAVE;
				txtTitle.setText(R.string.menu_save_as);
				encryptFlag = extras.getBoolean(INTENT_ENCRYPT);
				encryptCheck.setChecked(encryptFlag);
				
			}else if( extras.getString(INTENT_MODE).equals(MODE_COPY) ) {
				modeID = MODEID_COPY;
				txtTitle.setText(R.string.longclick_menu_copy);
				ViewGroup selectFileLayout = (ViewGroup)findViewById(R.id.selectFile);
				selectFileLayout.removeView(encryptCheck);
				mExistEncryptCheckBox = false;
			}else if( extras.getString(INTENT_MODE).equals(MODE_MOVE) ) {
				modeID = MODEID_MOVE;
				txtTitle.setText(R.string.longclick_menu_move);
				ViewGroup selectFileLayout = (ViewGroup)findViewById(R.id.selectFile);
				selectFileLayout.removeView(encryptCheck);
				mExistEncryptCheckBox = false;
			} else {

			}


			
//			System.out.println("(onCreate)DirPath=" + DirPath);
//			System.out.println("FilePath=" + filename);
		}
		//リスト構築
		fillList();

		// CheckBoxのクリックリスナー

		encryptCheck.setOnClickListener(new View.OnClickListener(){

//			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				EditText edtFileName = (EditText)findViewById(R.id.edtFileName);
				String strFileName = edtFileName.getText().toString();

				if(mExistEncryptCheckBox){
					CheckBox encryptCheck = (CheckBox)findViewById(R.id.encryptCheckBox);

					int sel_s = edtFileName.getSelectionStart();
					int sel_e = edtFileName.getSelectionEnd();

					if(encryptCheck.isChecked() == true) {
						// チェックされた状態の時の処理を記述
						int dot = strFileName.lastIndexOf('.');
						if(dot>=0){
							strFileName = strFileName.substring(0, dot).concat(".chi");
						}else{
							strFileName = strFileName.concat(".chi");
						}
						edtFileName.setText(strFileName);

					}
					else {
						// チェックされていない状態の時の処理を記述
						int dot = strFileName.lastIndexOf('.');
						if(dot>=0){
							strFileName = strFileName.substring(0, dot).concat(".txt");
						}else{
							strFileName = strFileName.concat(".txt");
						}
						edtFileName.setText(strFileName);
					}
					//カーソルを元の位置にセット
					int l = strFileName.length();
					if(sel_s > l)sel_s = l;
					if(sel_e > l)sel_e = l;				
					edtFileName.setSelection(sel_s,sel_e);

				}
			}
		});

		// OKボタンのクリックリスナー
		btnOK.setOnClickListener(new View.OnClickListener() {

			//		@Override
			public void onClick(View v) {
				Intent intent = new Intent();

				// OKボタン押下の場合は、以下のインテントを設定
				//  ファイル名/ディレクトリパス名/ファイルパス名
				EditText edtFileName = (EditText)findViewById(R.id.edtFileName);
				String strFileName = edtFileName.getText().toString().replaceAll("[\\s]*$", "");//末尾の空白を削除
				
				
				if(mExistEncryptCheckBox){
					CheckBox encryptCheck = (CheckBox)findViewById(R.id.encryptCheckBox);
					encryptFlag = encryptCheck.isChecked();
					
				}

				intent.putExtra(INTENT_FILENAME, strFileName );
				intent.putExtra(INTENT_DIRPATH, DirPath );
				intent.putExtra(INTENT_ENCRYPT, encryptFlag );
				intent.putExtra(INTENT_ORG_FILENAME, filename );
				

				String strFilePath;
				if( DirPath.equals("/")) {
					strFilePath = "/" + strFileName;
				} else {
					strFilePath = DirPath + "/" + strFileName;
				}
				intent.putExtra(INTENT_FILEPATH, strFilePath );
				// 処理結果を設定
				setResult(RESULT_OK, intent);
				finish();
			}

		});

		// Cancelボタンのクリックリスナー
		btnCancel.setOnClickListener(new View.OnClickListener() {
			//		@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				// 処理結果を設定
				setResult(RESULT_CANCELED, intent);
				finish();
			}
		});

		if(modeID == MODEID_SAVE){
			mEdtFileName.requestFocus();//ファイル名にフォーカス
		}
		
		int i = mEdtFileName.getText().toString().lastIndexOf('.');
		if(i>-1)mEdtFileName.setSelection(i);//拡張子の前にカーソルをおく

		/*doesn't work. maybe listview is needed focusable.
		//if Focus Change
		mEdtFileName.setOnFocusChangeListener(new View.OnFocusChangeListener(){
    		
    		//@Override
    		public void onFocusChange(View v, boolean flag){
    			if(flag == false){
    				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    				imm.hideSoftInputFromWindow(v.getWindowToken(),0);
    			}
    			Toast.makeText(getApplication(), "Focus changed.", Toast.LENGTH_SHORT).show();
    		}
    	});
		*/
	}


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		// リストアダプタから現在選択中のファイル名／ディレクトリ名を取得
		String strItem = (String)getListAdapter().getItem(position);

		if( strItem.equals("..") ) {
			// ディレクトリを1階層上がる場合
			upOneLevel();
			/*
			if( DirPath.lastIndexOf("/") <= 0 ) {
				// ルートから1階層目の場合
				DirPath = DirPath.substring(0, DirPath.lastIndexOf("/") + 1 );
			} else {
				// ルートから2階層目以上の場合
				DirPath = DirPath.substring(0, DirPath.lastIndexOf("/"));
			}
			fillList();
			*/
		} else if( strItem.substring(strItem.length() - 1 ).equals("/") ) {
			// ディレクトリに入る場合
			if( DirPath.equals("/") ) {
				//ルートの場合
				DirPath += strItem;
			} else {
				//ルートから1階層目以上の場合
				DirPath = DirPath + "/" + strItem;
			}
			DirPath = DirPath.substring(0, DirPath.length() - 1 );
			fillList();
		} else {
			// ファイルの場合はエディットテキストに設定
			EditText edtFileName = (EditText)findViewById(R.id.edtFileName);
			edtFileName.setText(strItem);
		}

	}

	private void upOneLevel(){
		String previousFileName = new File(DirPath).getName() + "/";

		
		// ディレクトリを1階層上がる場合
		if( DirPath.lastIndexOf("/") <= 0 ) {
			// ルートから1階層目の場合
			DirPath = DirPath.substring(0, DirPath.lastIndexOf("/") + 1 );
		} else {
			// ルートから2階層目以上の場合
			DirPath = DirPath.substring(0, DirPath.lastIndexOf("/"));
		}

		
		fillList();
		
		
		//カーソル参照していたフォルダの場所に合わせる。
		int posision = 0;
		for (String s : this.items) {
			//System.out.println(s);
			if(previousFileName.equals(s)){
				this.setSelection(posision);
				break;
			}
			posision++;
		}
	}




	// ファイルリスト構築
	private void fillList()
	{
		File[] files = new File(DirPath).listFiles();
		if( files == null ) {
			Toast.makeText(this, "Unable Access...",
					Toast.LENGTH_SHORT).show();
//			System.out.println("DirPath=" + DirPath);
			
			return;
		}

		// カレントディレクトリ名をTextViewに設定
		TextView txtDirName = (TextView)findViewById(R.id.txtDirName);
		txtDirName.setText(DirPath);

//		System.out.println("DirPath=" + DirPath);
//		System.out.println("FilePath=" + filename);

		if( items != null ) {
			items.clear();
		}
		items = new ArrayList<String>();

		// ルートじゃない場合は、階層を上がれるように".."をArrayListの先頭に設定
		if( !DirPath.equals("/") ) {
			items.add("..");
		}

		// ファイル・ディレクトリがあるだけひたすらArrayListに追加する
		for( File file : files ) {
			if( file.isDirectory() ) {
				items.add(file.getName() + "/" );
			} else {
				items.add(file.getName());
			}
//			System.out.println("file=" + file.getName());
		}

		/**
		// ArrayListをListActivityに設定する
		ArrayAdapter<String> fileList
		= new ArrayAdapter<String>(this, R.layout.file_row, items);
		**/

		FileListAdapter fileList = new FileListAdapter(this,this.items);
		
		
		//昇順で並べるようにする。
		fileList.sort(new Comparator<String>() {
			public int compare(String object1, String object2) {
				return object1.compareToIgnoreCase(object2);
//				return object1.compareTo(object2);
			};
		});
		//
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean listFoldersFirstFlag = sharedPreferences.getBoolean(getText(R.string.prefListFoldersFirstKey).toString(), false);
		if(listFoldersFirstFlag){
			fileList.sort(new Comparator<String>() {
				public int compare(String object1, String object2) {
					boolean obj1isDir = object1.matches("(.*/$|\\.\\.)");
					boolean obj2isDir = object2.matches("(.*/$|\\.\\.)");
					if(obj1isDir == obj2isDir)return 0;//両方dirか両方file
					else if(obj1isDir)return -1; //obj2がdir	//最初の引数が小さいときは負
					else return  1;//obj2がdir //最初の引数が大きいときは正
				};
			});
		}
		
		
		setListAdapter(fileList);
	}

    // キーイベント発生時、呼び出されます
    private boolean mBackKeyDown = false;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	
    	//if(mEdtFileName.hasFocus()){
    	//	return super.dispatchKeyEvent(event);
    	//}
    	//リスト以外のところにフォーカスがあればそのままreturn
    	if(!getListView().hasFocus()){
    		return super.dispatchKeyEvent(event);
    	}
    	
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(event.getKeyCode()){
            case KeyEvent.KEYCODE_DEL: // DELキー
//            	upOneLevel();//up dir
                return true;

            case KeyEvent.KEYCODE_BACK:
//    			Log.v("KeyEvent","KEYCODE_BACK,DOWN");

            	/*
    			if(!showBottomBarFlag){//back keyは無視するACTION_UPの時に処理をする。
    				mBackKeyDown = true;
    				return true;
    			}
    			*/
    			
    			break;
    			
    			
            case KeyEvent.KEYCODE_DPAD_LEFT: // 左キー
            	upOneLevel();//up dir
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: // 左キー
            	//upOneLevel();//up dir
            	//ToDo
            	KeyEvent e = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
                return super.dispatchKeyEvent(e);

    		default :
    			mBackKeyDown = false;
    			break;
    		}
    	}

        if (event.getAction() == KeyEvent.ACTION_UP) { // キーが離された時
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DEL: // DELキー
            	upOneLevel();//up dir
                return true;
            case KeyEvent.KEYCODE_BACK: // BACK KEY
            	
//    			Log.v("KeyEvent","KEYCODE_BACK,UP");
            	if(mBackKeyDown){
            		mBackKeyDown = false;//戻しておく
            		/*
            		if(!showBottomBarFlag){// 下部ボタンがないときは戻るボタンがup dirの機能となる
            			if(this.currentDirectory.getParent() == null
            					|| this.currentDirectory.toString().equals(mInitDirName) ){
            				PasswordBox.resetPassword();
            				finish();
            			}else{
            				upOneLevel();//up dir
            			}
            			return true;
            		}
            		*/
            	}else{
            		mBackKeyDown = false;
            	}
            	break;

            case KeyEvent.KEYCODE_DPAD_RIGHT: // 左キー
            	//upOneLevel();//up dir
            	//ToDo
            	KeyEvent e = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER);
                return super.dispatchKeyEvent(e);

            default:
        		mBackKeyDown = false;
        		break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

	
	
	
}
