package jp.gr.java_conf.hatalab.mnv;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;





import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
//import android.net.ContentURI;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;




public class MainActivity extends ListActivity {

	private List<String> directoryEntries = new ArrayList<String>();
	private File currentDirectory = new File("/sdcard");
	private String mInitDirName = "/sdcard";
	private boolean showBottomBarFlag = false;
	private boolean listFoldersFirstFlag = false;
	// ------------------------------
	// 定数定義
	// ------------------------------
	private static final int MENUID_NEW = Menu.FIRST;			// メニューID：新規作成
	private static final int MENUID_NEW_FOLDER = Menu.FIRST + 1;// メニューID：新規フォルダ
	private static final int MENUID_CLOSE = Menu.FIRST + 2;		// メニューID：終了
	private static final int MENUID_SETTINGS = Menu.FIRST + 3;		// メニューID：読み込み
//	private static final int MENUID_SAVE = Menu.FIRST + 2;		// メニューID：保存
//	private static final int MENUID_SAVE_AS = Menu.FIRST + 3;	// メニューID：名前を付けて保存
	private static final int MENUID_FIND = Menu.FIRST + 4;		// メニューID：検索

	
	private static final int SHOW_TEXT_EDIT      = 0;
	private static final int SHOW_SETTINGS       = 1;
	private static final int SHOW_FILELIST_COPY  = 2;
	private static final int SHOW_FILELIST_MOVE  = 3;

	private View mBottombar = null;
	private ViewGroup mMainlayout = null;
	
	private int mCurrentPosition = -1;

	private int mCurrentOrder = 1;
	private ImageView mOrderIcon;

	

	
//	private MainActivity mActivity;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	
		// setContentView() gets called within the next line,
		// so we do not need it here.
		setContentView(R.layout.main);
		
//		mActivity = this;
		
		getListView().setOnItemLongClickListener(new OnItemLongClickListener()
		{
			//@Override
//			protected void onItemLongClick(ListView l, View v, int position, long id) {
			public boolean onItemLongClick(AdapterView<?> l, View v, int position, long id) {
				myLongClick(position);
//				int selectionRowID = position;
//				String selectedFileString = MainActivity.this.directoryEntries.get(selectionRowID);
//				Toast.makeText(MainActivity.this, "LongClick:" + selectedFileString, Toast.LENGTH_LONG).show();
				return true;
			}                    
		
		});
		
		
		
		
		updateConfig();
				
		
		File initdir = new File(mInitDirName);
		if(initdir.exists()){
			browseTo(initdir);
		}else{
//	        Toast.makeText(this, initdir.getAbsoluteFile()+" is not exist.", Toast.LENGTH_LONG).show();
	        Toast.makeText(this, initdir.getAbsoluteFile()+ " " + getString(R.string.alert_initdir_is_not_exist), Toast.LENGTH_LONG).show();
			browseToRoot();
			
		}

		mOrderIcon = (ImageView)findViewById(R.id.orderIcon);
		if(mCurrentOrder > 0)mOrderIcon.setImageResource(android.R.drawable.arrow_up_float);
		else				 mOrderIcon.setImageResource(android.R.drawable.arrow_down_float);			
		
		mOrderIcon.setClickable(true);
		mOrderIcon.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				changeListOreder();
			}
		});
		
		
		//選択箇所が変わるたびにCurrent positionを保存する。
		getListView().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				setCurrentPosition(position);
				//ListView listView = (ListView) parent;
				//	    	                   String item = (String) listView.getSelectedItem();
				//	    	                   Log.v("Main", String.format("onItemSelected: %s", item));
			}

			public void onNothingSelected(AdapterView<?> parent) {
				//	    	                   Log.v("Main", "onNothingSelected");
			}
		});
	}

	
	// create Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);


		menu.add(0, MENUID_NEW, 0, R.string.menu_new)
		.setShortcut('0', 'n')
		.setIcon(android.R.drawable.ic_menu_edit);

		menu.add(0, MENUID_NEW_FOLDER, 0, R.string.menu_new_folder)
		.setShortcut('1', 'f')
		.setIcon(android.R.drawable.ic_menu_add);

		menu.add(0, MENUID_CLOSE, 0, R.string.menu_close)
		.setShortcut('2', 'c')
		.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		
		menu.add(0, MENUID_SETTINGS, 0, R.string.menu_preferences)
		.setShortcut('3', 'n')
		.setIcon(android.R.drawable.ic_menu_preferences);

		menu.add(0, MENUID_FIND, 0, R.string.menu_search)
		.setShortcut('3', 'n')
		.setIcon(android.R.drawable.ic_menu_search);
		
		return true;
	}



	// メニュー押下時のイベント処理
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		super.onMenuItemSelected(featureId, item);

		switch(item.getItemId()) {

		case MENUID_NEW:		// 新規作成
			Intent intent = new Intent(this, TextEdit.class);
			//新規作成の場合はディレクトリのパスをTextEditに渡して、TextEdit側で新しいファイルとしてハンドリングしてもらう。FILEPATH
			intent.putExtra("FILEPATH", this.currentDirectory.getAbsolutePath() );
			//			intent.putExtra("DESCRIPTION", item.getDescription());
			startActivity(intent);
			
			break;
			
		case MENUID_NEW_FOLDER:		// 新規作成
			createDir();
			break;
		case MENUID_CLOSE:		// 終了
			PasswordBox.resetPassword();
			finish();
			break;

		case MENUID_SETTINGS:		// 設定
			Intent intent1 = new Intent(this, Settings.class);
			startActivityForResult(intent1, SHOW_SETTINGS);
//			startActivity(intent1);
			
			break;

		case MENUID_FIND:		// 検索
			Intent intent2 = new Intent(this, GrepActivity.class);
			intent2.putExtra("FILEPATH", this.currentDirectory.getAbsolutePath() );
			//			intent.putExtra("DESCRIPTION", item.getDescription());
			startActivity(intent2);
			
			break;

			
		default:
			break;
		}
		return true;
	}

	/* 
	 * 別Activityから戻ってきたときの挙動 
	 */  
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {  
		super.onActivityResult(requestCode, resultCode, intent);  

		switch( requestCode ) {
		case SHOW_TEXT_EDIT:	// ファイルオープン
			refreshDir();//Refresh File List 
			break;
		case SHOW_SETTINGS:	// 設定
			updateConfig();
			break;
		case SHOW_FILELIST_COPY:	//
			if (resultCode == RESULT_OK) {
				// ファイル名取得
				final String dstfilepath = intent.getStringExtra(SelectFileName.INTENT_FILEPATH);
				final String srcfilepath = intent.getStringExtra(SelectFileName.INTENT_ORG_FILENAME);
				File dstFile = new File(dstfilepath);
				File srcFile = new File(srcfilepath);
				execCopyFile(srcFile,dstFile);
			}
			break;
		case SHOW_FILELIST_MOVE:	//
			if (resultCode == RESULT_OK) {
				// ファイル名取得
				final String dstfilepath = intent.getStringExtra(SelectFileName.INTENT_FILEPATH);
				final String srcfilepath = intent.getStringExtra(SelectFileName.INTENT_ORG_FILENAME);
				File dstFile = new File(dstfilepath);
				File srcFile = new File(srcfilepath);
				execMoveFile(srcFile,dstFile);
			}
			break;
		default:
			break;
		}
	} 

	/* 
	 * 戻るボタンを押したとき 
	 */  

	@Override
	public void onDestroy(){
		saveConfig();		
		PasswordBox.resetPassword();
		super.onDestroy(); //android.app.SuperNotCalledExceptionが発生しないように・・・。

	}

    @Override
    protected void onResume() {
        super.onResume();
        refreshDir();
    }
	
    // キーイベント発生時、呼び出されます
    private boolean mBackKeyDown = false;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(event.getKeyCode()){
            case KeyEvent.KEYCODE_DEL: // DELキー
//            	upOneLevel();//up dir
                return true;

            case KeyEvent.KEYCODE_BACK:
//    			Log.v("KeyEvent","KEYCODE_BACK,DOWN");
    			
    			if(!showBottomBarFlag){//back keyは無視するACTION_UPの時に処理をする。
    				mBackKeyDown = true;
    				return true;
    			}
    			
            	/**
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
    
    
	/**
	 * This function browses to the
	 * root-directory of the file-system.
	 */
	private void browseToRoot() {
		browseTo(new File("/"));
	}

	/**
	 * This function browses up one level
	 * according to the field: currentDirectory
	 */
	private void upOneLevel(){
		if(this.currentDirectory.getParent() != null){
			String previousFileName = this.currentDirectory.getName() + "/";
			this.browseTo(this.currentDirectory.getParentFile());
			
			//カーソル参照していたフォルダの場所に合わせる。
			int posision = 0;
			for (String s : this.directoryEntries) {
				//System.out.println(s);
				if(previousFileName.equals(s)){
					this.setSelection(posision);
					break;
				}
				posision++;
			}
		}
	}

	private void browseTo(final File aDirectory){
		if (aDirectory.isDirectory()){
			this.currentDirectory = aDirectory;
			fill(aDirectory.listFiles());
		}else{
			try {
				openFile(aDirectory);
			} catch (Exception e) {
				e.printStackTrace();
//				showDialog(e.toString());
				Toast.makeText(this, getString(R.string.alert_cannot_openfile)+ ": " + aDirectory, Toast.LENGTH_LONG).show();
			}
		}			
	}




	private void fill(File[] files) {
		this.directoryEntries.clear();

		// Add the "." and the ".." == 'Up one level'
//		this.directoryEntries.add(".");
		if(this.currentDirectory.getParent() != null)
			this.directoryEntries.add("..");




		int currentPathStringLenght = (this.currentDirectory.getAbsolutePath()).length();
		if(this.currentDirectory.getParent() != null)currentPathStringLenght++;

		if(files != null) for (File file : files){
			if (file.isDirectory()){
				this.directoryEntries.add(file.getAbsolutePath().substring(currentPathStringLenght) + "/");
			}else{
				this.directoryEntries.add(file.getAbsolutePath().substring(currentPathStringLenght));	
			}
		}

		// カレントディレクトリ名をTextViewに設定
		TextView txtDirName = (TextView)findViewById(R.id.txtDirName);
		txtDirName.setText(this.currentDirectory.getAbsolutePath());

		/**
		ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this,
				R.layout.file_row, this.directoryEntries);
	
		//昇順で並べるようにする。
		directoryList.sort(new Comparator<String>() {
			public int compare(String object1, String object2) {
				return object1.compareTo(object2);
			};
		});
		
		this.setListAdapter(directoryList);
		 **/
		
        // アダプタ生成
		FileListAdapter adapter = new FileListAdapter(this, this.directoryEntries );
		final int m_sortOrder = mCurrentOrder;
		//昇順で並べるようにする。
		adapter.sort(new Comparator<String>() {
			public int compare(String object1, String object2) {
				return object1.compareToIgnoreCase(object2)*m_sortOrder;
//				return object1.compareTo(object2);
			};
		});

		//昇順で並べるようにする。//ディレクトリは上
		if(listFoldersFirstFlag){
			adapter.sort(new Comparator<String>() {
				public int compare(String object1, String object2) {
					boolean obj1isDir = object1.matches("(.*/$|\\.\\.)");
					boolean obj2isDir = object2.matches("(.*/$|\\.\\.)");
					if(obj1isDir == obj2isDir){
						return 0;//両方dirか両方file
					}else if(obj1isDir){ //obj2がdir
						return -1*m_sortOrder; //最初の引数が小さいときは負
					}else{ //obj2がdir
						return  1*m_sortOrder; //最初の引数が大きいときは正

					}
				};
			});
		}
		
		// アダプタ設定
		setListAdapter(adapter);


	
	
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		mCurrentPosition = position;
		//		int selectionRowID = (int) this.getSelectionRowID();
		int selectionRowID = position;
		String selectedFileString = this.directoryEntries.get(selectionRowID);
		if (selectedFileString.equals(".")) {
			// Refresh
			this.browseTo(this.currentDirectory);
		} else if(selectedFileString.equals("..")){
			this.upOneLevel();
		} else {
			File clickedFile = null;
			if(this.currentDirectory.getParent() != null){
				clickedFile = new File(this.currentDirectory.getAbsolutePath() 
						+ "/" + this.directoryEntries.get(selectionRowID));
			}else{
				clickedFile = new File(this.currentDirectory.getAbsolutePath() 
						+ this.directoryEntries.get(selectionRowID));						
			}
			if(clickedFile != null)
				this.browseTo(clickedFile);
		}
	}

//	@Override
	protected void myLongClick(int position) {
		mCurrentPosition = position;

		//		int selectionRowID = (int) this.getSelectionRowID();
		int selectionRowID = position;
		String selectedFileString = this.directoryEntries.get(selectionRowID);
				
		if (selectedFileString.equals(".")) {
			// Do nothing
		} else if(selectedFileString.equals("..")){
			// Do nothing
		} else {
			File clickedFile = null;
			if(this.currentDirectory.getParent() == null){
				clickedFile = new File(this.currentDirectory.getAbsolutePath() 
						+ this.directoryEntries.get(selectionRowID));						
			}else{
				clickedFile = new File(this.currentDirectory.getAbsolutePath() 
						+ "/" + this.directoryEntries.get(selectionRowID));
			}
			if(clickedFile != null)
			{
				final File file=clickedFile;
//				this.browseTo(clickedFile);
//				CharSequence[] items = {"Open", "Delete", "Cancel"};
				CharSequence[] items = {
						getText(R.string.longclick_menu_open).toString(),  // item no 0
						getText(R.string.longclick_menu_delete).toString(),// item no 1
						getText(R.string.longclick_menu_rename).toString(),// item no 2
						getText(R.string.longclick_menu_copy).toString(),  // item no 3
						getText(R.string.longclick_menu_move).toString(),  // item no 4
						getText(R.string.longclick_menu_cancel).toString(),// item no 5
				};
				new AlertDialog.Builder(this)
				//.setTitle("Action")
				.setTitle(R.string.longclick_menu_title)
				.setItems(items, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int item)
					{
						String str="";
						switch(item) {

						case 0:		// Open
							browseTo(file);
							break;

						case 1:		// delete
							deleteFile(file);
							break;

						case 2:		// delete
							renameFile(file);
							break;

						case 3:		// copy
							copyFile(file);
							break;

						case 4:		// move
							moveFile(file);
							break;

						case 5:		// Cancel
							// Do nothing
							str = "Cancel";
							break;

						default:
							break;
						}

//						Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
						
					}
				})
				.show();

			}
		}
		
	}

	
	
	private void openFile(File aFile){

		String end = aFile.getName().substring(aFile.getName().lastIndexOf(".")+1, aFile.getName().length()).toLowerCase();
		if(end.equals("txt") || end.equals("chi") ){
			Intent intent = new Intent(this, TextEdit.class);
			intent.putExtra("FILEPATH", aFile.getAbsolutePath());
			
			startActivityForResult(intent, SHOW_TEXT_EDIT);

		}else{



			// other files...
			// Create an Intent
			Intent intent = new Intent();

			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setAction(android.content.Intent.ACTION_VIEW);
			String type = getMIMEType(aFile);
			// Setting up the data and the type for the intent
			intent.setDataAndType(Uri.fromFile(aFile),type);

			// will start the activtiy found by android or show a dialog to select one
			startActivity(intent); 

/**			            Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW,
			                            Uri.parse("file://" + aFile.getAbsolutePath()));
		            startActivity(myIntent);
	*/
		}
	}

	private void deleteFile(File aFile){
		final File file = aFile;
		new AlertDialog.Builder(MainActivity.this)
		.setTitle(getText(R.string.longclick_menu_delete))
		.setMessage(getText(R.string.longclick_menu_delete_confirm) + ": " + file.getName())
		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//ファイル削除
				/**
				try{
					MyUtil.deleteFile(file);
					refreshDir();//Refresh File List 

				}catch(Exception e){
					e.printStackTrace();
					showDialog(e.toString());
				}
				**/
				execDeleteFile(file);
			}
		})
		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//Do Nothing.
			}
		})
		.show(); //ダイアログ表示;

	}
	private void execDeleteFile(File file){
		FileOperatorTask task = new FileOperatorTask(this);
		task.execute(FileOperatorTask.DELETE_FILE,file.getAbsolutePath());

	}

	private void renameFile(File aFile){
		final File file = aFile;

		//コンテキストからインフレータを取得
		LayoutInflater inflater = LayoutInflater.from(this);
		//レイアウトXMLからビュー(レイアウト)をインフレート
		final View inputView = inflater.inflate(R.layout.input_name, null);

		final EditText nameEditText = (EditText)inputView.findViewById(R.id.dialog_edittext);
		nameEditText.setText(file.getName());
		
		int i = file.getName().lastIndexOf('.');
		if(i>-1)nameEditText.setSelection(i);//拡張子の前にカーソルをおく

		
		String title;
		if(file.isDirectory()){
			title = getString(R.string.folder_name_input);
		}else{
			title = getString(R.string.file_name_input);			
		}
		
		//ダイアログを構成
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title)
//		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			//		    @Override
			public void onClick(DialogInterface dialog, int which) {
				//OKボタンが押下された時に入力された文字を設定する
				EditText nameEditText = (EditText)inputView.findViewById(R.id.dialog_edittext);
				String name = nameEditText.getText().toString();

				String curDir = MainActivity.this.currentDirectory.getAbsolutePath();
				
				if(MainActivity.this.currentDirectory.getParent() == null){
					// nothing
				}else{
					curDir = curDir	+ "/";
				}

				
				if(name.length()>0){
					try {
						if(MyUtil.renameFile(file, new File(curDir + name))){
							// 成功したらToastを出す
							Toast.makeText(MainActivity.this, R.string.done_rename_file, Toast.LENGTH_SHORT).show();
							refreshDir();
						}else{
							Toast.makeText(MainActivity.this, R.string.error_operation_failed, Toast.LENGTH_SHORT).show();							
						}
					} catch (MyUtilException e) {
						e.printStackTrace();
						showDialog(getString(((MyUtilException) e).getCode()));						
					} catch (Exception e) {
						e.printStackTrace();
						showDialog(getString(R.string.alert_general_error) + "\n" + e.toString());
					}
					
				}else{
					//nameが空だったら終了する
					//	finish();
					Toast.makeText(MainActivity.this, R.string.input_name_empty, Toast.LENGTH_SHORT).show();
					renameFile(file);//もう一度やりなおし
				}

			}
		}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//do nothing
			}
		}).setOnCancelListener(new DialogInterface.OnCancelListener() {
//			  @Override
			public void onCancel(DialogInterface dialog) {
//				Log.v("TextEdit","onCancel");
				// キャンセルボタンと戻るボタンが押された時の処理をここに記述する。
				//do nothing
			}
		})
		.setView(inputView)
//		.show(); //ダイアログ表示
		;//divide statement to focus control
		
		final AlertDialog dialog = builder.create();
		dialog.show();
		
		nameEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
	        public void onFocusChange(View v, boolean hasFocus) {
	            if (hasFocus) {
	            	dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	            }
	        }
	    });

		nameEditText.requestFocus();
	}

	
	private void createDir(){

		//コンテキストからインフレータを取得
		LayoutInflater inflater = LayoutInflater.from(this);
		//レイアウトXMLからビュー(レイアウト)をインフレート
		final View inputView = inflater.inflate(R.layout.input_name, null);

		EditText nameEditText = (EditText)inputView.findViewById(R.id.dialog_edittext);
		///nameEditText.setText(R.string.default_new_folder_name);
		
		myTemplateText defaultName = Config.getDefaultFolderName();
		String name = defaultName.toString();
		if(defaultName.isWithNumber()){
			int next_num = 0;
			String regex = defaultName.getNumberRegex();
			if(regex != null){
				Pattern p = Pattern.compile(regex);
				for(String s :directoryEntries){
					Matcher m = p.matcher(s.replaceAll("/$",""));
					if(m.find()){
						String str_num = m.group(1);
						int  num = Integer.parseInt(str_num);
						if(num >= next_num)next_num = num + 1;
					}
				}

				//name = defaultName.toString(next_num);
				try{
					name = defaultName.toString(next_num);
				}catch(Exception e){
					//Toast.makeText(TemplateTextList.this, e.toString(), Toast.LENGTH_LONG).show();
					MyUtil.showMessage(e.toString(), this);
				}
				
			}
		}
		name=name.replaceAll("\\\\%", "%");
		Log.d("Main",name);

		name=name.replaceAll("[/:,;*?\"<>|]", ".");//replace invalid character
		nameEditText.setText(name);
		

		//ダイアログを構成
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.folder_name_input)
//		.setCancelable(true)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			//		    @Override
			public void onClick(DialogInterface dialog, int which) {
				//OKボタンが押下された時に入力された文字を設定する
				EditText nameEditText = (EditText)inputView.findViewById(R.id.dialog_edittext);
				String name = nameEditText.getText().toString();

				String curDir = MainActivity.this.currentDirectory.getAbsolutePath();
				
				if(MainActivity.this.currentDirectory.getParent() == null){
					// nothing
				}else{
					curDir = curDir	+ "/";
				}

				
				if(name.length()>0){
					try {
						if(MyUtil.createDir(new File(curDir + name))){
							refreshDir();
						}else{
							Toast.makeText(MainActivity.this, R.string.error_operation_failed, Toast.LENGTH_SHORT).show();							
						}
					} catch (MyUtilException e) {
						e.printStackTrace();
						showDialog(getString(((MyUtilException) e).getCode()));
					} catch (Exception e) {
						e.printStackTrace();
						showDialog(e.toString());
					}
					
				}else{
					//nameが空だったら終了する
					//	finish();
					Toast.makeText(MainActivity.this, R.string.input_name_empty, Toast.LENGTH_LONG).show();
					createDir();//もう一度やりなおし
				}

			}
		}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//do nothing
			}
		}).setOnCancelListener(new DialogInterface.OnCancelListener() {
//			  @Override
			public void onCancel(DialogInterface dialog) {
//				Log.v("TextEdit","onCancel");
				// キャンセルボタンと戻るボタンが押された時の処理をここに記述する。
				//do nothing
			}
		})
		.setView(inputView)
		.show(); //ダイアログ表示

		
	}
	private void moveFile(File aFile){
		final File file = aFile;

		
		Intent intent = new Intent(this, SelectFileName.class);

		intent.putExtra(SelectFileName.INTENT_MODE, SelectFileName.MODE_MOVE);
		intent.putExtra(SelectFileName.INTENT_FILEPATH, file.getAbsolutePath());
		intent.putExtra(SelectFileName.INTENT_ENCRYPT, false);

		startActivityForResult(intent, SHOW_FILELIST_MOVE);


	}

	private void execMoveFile(File srcFile, File dstFile){
		FileOperatorTask task = new FileOperatorTask(this);
		task.execute(FileOperatorTask.MOVE_FILE,srcFile.getAbsolutePath(),dstFile.getAbsolutePath());

	}
	
	private void copyFile(File aFile){
		final File file = aFile;

		
		Intent intent = new Intent(this, SelectFileName.class);

		intent.putExtra(SelectFileName.INTENT_MODE, SelectFileName.MODE_COPY);
		intent.putExtra(SelectFileName.INTENT_FILEPATH, file.getAbsolutePath());
		intent.putExtra(SelectFileName.INTENT_ENCRYPT, false);

		startActivityForResult(intent, SHOW_FILELIST_COPY);


	}

	private void execCopyFile(File srcFile, File dstFile){
		FileOperatorTask task = new FileOperatorTask(this);
		task.execute(FileOperatorTask.COPY_FILE,srcFile.getAbsolutePath(),dstFile.getAbsolutePath());

		/**
		try {
			MyUtil.copyFile(srcFile, dstFile);
			refreshDir();
			Toast.makeText(this, "Copy "+ srcFile.getName() + "\n to " + dstFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

		} catch (Exception e) {
			e.printStackTrace();
			showDialog(e.toString());
		}
		**/
	}
	
	
	//設定値を読み込み、更新
	private void updateConfig(){
		Config.update(this);

        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        //mInitDirName = sharedPreferences.getString(getText(R.string.prefInitDirKey).toString(), "/sdcard");
        //listFoldersFirstFlag = sharedPreferences.getBoolean(getText(R.string.prefListFoldersFirstKey).toString(), false);
        //mCurrentOrder = sharedPreferences.getInt(getText(R.string.prefFileListOrderKey).toString(), 1);
     
		//初期フォルダを取得        
        mInitDirName = Config.getInitDirName();
        //ListFoldersFirst
        listFoldersFirstFlag = Config.getListFoldersFirstFlag();
        // Order, ascend or descent
        mCurrentOrder = Config.getFileListOrder();
        
		//ボタン表示
		//showBottomBarFlagセット
        boolean tempFlag = showBottomBarFlag;
        //showBottomBarFlag = sharedPreferences.getBoolean(getText(R.string.prefShowButtonsKey).toString(), true);
        showBottomBarFlag = Config.getShowButtonsFlag();
        
        if(showBottomBarFlag &&(mBottombar == null || mMainlayout == null)){//initialize
        	mBottombar = getLayoutInflater().inflate(R.layout.bottom_bar, null);
        	mMainlayout = (ViewGroup)findViewById(R.id.mainLayout);
        	// ボタンのクリックリスナー
        	Button btnUpDir = (Button)mBottombar.findViewById(R.id.LeftButton);
        	btnUpDir.setText(R.string.BottomMenu_updir);
        	//		Button btnUpDir = (Button)findViewById(R.id.ButtonUpDir);
        	btnUpDir.setOnClickListener(new View.OnClickListener() {
        		//		@Override
        		public void onClick(View v) {
        			//ボタンを押したときの動作
        			upOneLevel();
        		}

        	});
        	// ボタンのクリックリスナー
        	Button btnMenu = (Button)mBottombar.findViewById(R.id.RightButton);
        	btnMenu.setText(R.string.BottomMenu_menu);
        	//		Button btnMenu = (Button)findViewById(R.id.ButtonMenu);
        	btnMenu.setOnClickListener(new View.OnClickListener() {
        		//		@Override
        		public void onClick(View v) {
        			//ボタンを押したときの動作
        			openOptionsMenu();
        		}

        	});

        }
		

        //Log.v("configUpdate","showFlag:"+showBottomBarFlag+", mBottombar:" + mBottombar
		//		+ ", isEnable():" + mBottombar.isEnabled()
		//		+ ", isShown():" + mBottombar.isShown());

		if(tempFlag != showBottomBarFlag){
			
			if(showBottomBarFlag){
				mMainlayout.addView(mBottombar);
			}else{
				mMainlayout.removeView(mBottombar);
				
			}
			
		}

	}
	
	private void saveConfig(){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = pref.edit();
		//save list oreder
		editor.putInt(getText(R.string.prefFileListOrderKey).toString(), mCurrentOrder);
		
		editor.commit();
	}
	
	
	private void changeListOreder(){
		mCurrentOrder *= -1;//反転させる。
		if(mCurrentOrder > 0){
			mOrderIcon.setImageResource(android.R.drawable.arrow_up_float);
		}else{
			mOrderIcon.setImageResource(android.R.drawable.arrow_down_float);			
		}
		
		

//		mCurrentPosition = this.getSelectedItemPosition();

		if(mCurrentPosition > -1){
			int count = this.getListView().getAdapter().getCount();
			mCurrentPosition = count - mCurrentPosition -1;
		}
//		this.setSelection(mCurrentPosition);

		
		
		refreshDir();
	}
	
	/**
	 * Dialogを表示
	 *
	 * @param msg 表示するメッセージ
	 */
	private void showDialog(String msg){
		new AlertDialog.Builder(this)
		.setMessage(msg)
		.setNeutralButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			// この中に"YES"時の処理をいれる。
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		})
		.show();
	}




	/**
	 * Returns the MIME type for the given file.
	 *
	 * @param f the file for which you want to determine the MIME type
	 * @return the detected MIME type
	 */
	private String getMIMEType(File f)
	{
		String end = f.getName().substring(f.getName().lastIndexOf(".")+1, f.getName().length()).toLowerCase();

		String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(end);

	/**	
//		String type = "";
		if(end.equals("mp3") ||
				end.equals("aac") ||
				end.equals("aac") ||
				end.equals("amr") ||
				end.equals("mpeg") ||
				end.equals("mp4") ) type = "audio";
		else if(end.equals("jpg") ||
				end.equals("gif") ||
				end.equals("png") ||
				end.equals("jpeg")) type = "image";
		else if(end.equals("htm") ||
				end.equals("html") ||
				end.equals("php") ||
				end.equals("csv") ||
				end.equals("xml") ||
				end.equals("txt")) type = "text";
		else type = "*";

		type += "/*";

		*/
		return type;
	}
	
	void setCurrentPosition(int position){
		mCurrentPosition = position;
	}
	
	
	
//	private void refreshDir(){
	public void refreshDir(){
		if (this.currentDirectory.isDirectory()){
			//int position = this.getSelectedItemPosition();
			//if(position < 0)position = mCurrentPosition;

			int position = mCurrentPosition;
			//Log.v("TextEdit","refreshDir:postion=" + position);
			fill(this.currentDirectory.listFiles());
			
			int count = this.getListView().getAdapter().getCount();
			//Log.v("TextEdit","refreshDircount=" + count);
			if(position >= count)position = count-1;
			this.setSelection(position);
			
		}
	}
	
}
