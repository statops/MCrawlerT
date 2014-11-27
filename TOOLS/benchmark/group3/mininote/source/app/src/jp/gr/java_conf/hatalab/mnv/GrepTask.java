package jp.gr.java_conf.hatalab.mnv;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

// ファイル読み込みスレッド
class GrepTask extends AsyncTask<String, Integer, FileInfo>{

	private ProgressDialog mProgressDialog;
	private GrepActivity mActivity;
	private boolean errorOccured = false;
	private String  errorMessage = "";

	private Handler handler;//バックグランドとUIとでやり取りするため
	
	// コンストラクタ
	public GrepTask(GrepActivity activity) {
		mActivity = activity;
	}
	
	// タスクを実行した直後にコールされる
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// プログレスバーを表示する
		mProgressDialog = new ProgressDialog(mActivity);
		
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {  
	        public void onCancel(DialogInterface dialog) {  
	          // Task を停止  
	        	taskCancel();  
	        }  
	      }); 
		
		
		//mProgressDialog.setMessage("Now Searching...");
		mProgressDialog.setMessage(mActivity.getText(R.string.notify_now_searching));
		
		mProgressDialog.show();
		errorOccured = false;

		//ハンドラー設定
		handler = new Handler();  
	}
		
	@Override
	protected FileInfo doInBackground(String... params) {
		FileInfo fInfo = null;//= new FileInfo();
		String data;
		String keyword     = mActivity.getSearchWord();
		String currentfile = mActivity.getCurrentFilePath();
		int direction      = mActivity.getSearchDirection();
		boolean includeEncryptedFile = mActivity.IncludeEncryptedFile(); 
		
		boolean match = false;
		
		GrepMatchInfo matchInfo = null;
		
		String searchingMessage = mActivity.getText(R.string.notify_now_searching).toString();
		
		while(!match){
			if(isCancelled())break;//AsyncTaskがcancelledならbreak
			//continue 続く
			//break 終わり
			String filename = "";
			try {
				//Get file name...
				if(direction == GrepActivity.FORWARD){
					filename= mActivity.getNextFile();
					//今開いているファイルと同じ物が出てきたらもう一度
					if(!filename.equals("") && filename.equals(currentfile))filename= mActivity.getNextFile();
				}else{//Backward
					filename= mActivity.getPrevFile();
					if(!filename.equals("") && filename.equals(currentfile))filename= mActivity.getPrevFile();
				}
				
				
				//Log.d("GrepActivity", "FileReadThread:" + filename);
				if(filename.equals("")){
					//can not fine next file.
					break;
				}

				//file format check. double check with dirList
				if(includeEncryptedFile){
					if(!filename.matches(GrepActivity.FILENAME_TEXT_CHI))continue;
				}else{
					if(!filename.matches(GrepActivity.FILENAME_TEXT))continue;					
				}
				
				//進捗を表示するためにファイル名を取得
				String shortfilename = new File(filename).getName();
				postMessage(searchingMessage + " " + shortfilename);
				//postMessage("Searching in " + shortfilename);
				
				data = MyUtil.readFile(filename, mActivity.getCharsetName());
				
				if(direction == GrepActivity.FORWARD){
					matchInfo = MyUtil.searchWord(keyword, data, 0);
				}else{//Backward
					matchInfo = MyUtil.searchWordBackward(keyword, data, data.length());
				}
				
				if(matchInfo != null){
					fInfo = new FileInfo();
					fInfo.setFile(new File(filename));
					fInfo.setData(data);
					fInfo.setSelStart(matchInfo.start);
					fInfo.setSelEnd(matchInfo.stop);
					break;
				}



			}
			catch (MyUtilException e) {
//				e.printStackTrace();
				String s =  mActivity.getString(((MyUtilException) e).getCode());

				errorOccured = true;
//				errorMessage = e.toString();
				errorMessage += filename + ":\n"+ s ;
				
				//次回実行時に同じファイルが呼び出されるようにする。
				mActivity.dirListRevert();
				break;
			}
			catch (Exception e) {
//				e.printStackTrace();
				errorOccured = true;
//				errorMessage = e.toString();
				errorMessage += filename + ":\n"+ e.toString() + "\n";

				//次回実行時に同じファイルが呼び出されるようにする。
				mActivity.dirListRevert();
				break;
			}
			
			
		}
		
		return fInfo;
	}

	private void postMessage(final String msg){
		// Handlerに、UIスレッドへのmsgをPOSTする。  
		handler.post(new Runnable() {  
			public void run() {  
				mProgressDialog.setMessage(msg);

			}  
		});  
	}
	
	// メインスレッド上で実行される
	@Override
	protected void onPostExecute(FileInfo result) {
		super.onPostExecute(result);
    	if(mProgressDialog != null && mProgressDialog.isShowing())mProgressDialog.dismiss();
		
		if(errorOccured){
			//検索に成功してもしなくても、検索中にエラーが発生していたら表示する。
			MyUtil.showMessage(errorMessage ,mActivity);
			mActivity.setFileInfo(null);
		}else if(result == null){
	    	//Toast.makeText(mActivity, R.string.notFound, Toast.LENGTH_SHORT).show();	
	    	ToastMaster.makeTextAndShow(mActivity, R.string.notFound, Toast.LENGTH_SHORT);
			mActivity.setFileInfo(null);
		}else{
			mActivity.setFileInfo(result);
		}

	}

	
    /* cancel() がコールされると呼び出される。 */
    @Override
    protected void onCancelled() {
    	mActivity.stopDirList();
    	if(mProgressDialog != null && mProgressDialog.isShowing())mProgressDialog.dismiss();
    	Toast.makeText(mActivity, "cancel...", Toast.LENGTH_SHORT).show();
    	super.onCancelled();
    }
    private void taskCancel(){
    	cancel(true);
    }
}