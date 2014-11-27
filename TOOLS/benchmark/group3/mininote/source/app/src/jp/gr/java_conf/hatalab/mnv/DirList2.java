package jp.gr.java_conf.hatalab.mnv;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.util.Log;

public class DirList2 {
	
	private String mBaseDir = "/";
	private String mInitReturn = "";
	private String mPreviousReturn = "";
	private boolean mInitSearch = true;
	private String mFilter  = "(.*\\.txt|.*\\.chi)";
//	private String mFilter  = "(.*\\.txt)";
//	private String mFilter  = ".*";
	
	private boolean mlistFoldersFirstFlag = false;
	private boolean mBeginOfList = false;
	private boolean mEndOfList   = false;
	private boolean mCancelled   = false;
	
	private int mSortDirection = 1; // 1:ascend(昇順), -1:descend(降順)
	
	public DirList2(String initFilePath){
		File f = new File(initFilePath);
		if(!f.exists()){
			f = new File("/sdcard");
			if(!f.exists()){
				f = new File("/");
			}			
		}
		
		if(f.isDirectory()){
			mBaseDir = f.getAbsolutePath();//このファイル名が含まれる間は検索を続ける。
			mPreviousReturn = mBaseDir;
		}else{//ファイルの場合はそのファイルを含むディレクトリをbaseDirとする。
			mBaseDir = f.getParent();
			if(mBaseDir == null)mBaseDir = "/";
			mInitReturn = f.getAbsolutePath();//最初に返却するファイルパスはこれ
			mPreviousReturn = mInitReturn;
		}
		

//    	Log.d("DirList", "mInitDir = " + mBaseDir);

	}
//	private static String initDir = "/sdcard";

	public String nextFile(){
		setSortAscend();
		String resultFile;
		mCancelled = false;

//    	Log.d("DirList.nextFile()", "mInitDir = " + mBaseDir);

    	if(mInitSearch){
    		mInitSearch = false;
    		
    		if(!mInitReturn.equals("")){//初回
    			mPreviousReturn = mInitReturn;
    			mInitReturn = "";
    			resultFile = mPreviousReturn;

    		}else{
    			String nextFile = mBaseDir;
    			//ディレクトリも含め次のファイルが見つかったらresultが帰ってくる。
    			//従って、ファイルがディレクトリだったらもう一度検索する。
    			while(new File(nextFile).isDirectory()){
    				if(mCancelled)return "";
    				nextFile = searchNextFile(nextFile, "");
    			}
    				
       			if(!nextFile.equals(""))mPreviousReturn = nextFile;
    			resultFile = nextFile;
    			
    		}
    	}else{
    		
    		if( mEndOfList ){//既に末尾までいってしまった場合
    			resultFile = "";//前回見つからなかった場合はずっと空を返す。
    		}else{
    			
    			String nextFile;
    			if(mBeginOfList){//先頭の場合はmBaseDirをセットして末尾からすべて検索（昇順検索）
    				nextFile = mBaseDir;
//    	   			File f = new File(mPreviousReturn);
//        			nextFile = searchNextFile(f.getParent(),f.getName());
    			}else{
    	   			File f = new File(mPreviousReturn);
        			nextFile = searchNextFile(f.getParent(),f.getName());
    			}
 
    			while(new File(nextFile).isDirectory()){
    				if(mCancelled)return "";
    				nextFile = searchNextFile(nextFile, "");
    			}

    			
    			if(!nextFile.equals(""))mPreviousReturn = nextFile;
    			resultFile = nextFile;
    		}
    	}
    	
    	if(resultFile.equals("")){
    		mEndOfList = true;
    	}else{
    		mBeginOfList = false;
    		mEndOfList = false;    		
    	}
    	return resultFile;
	}

	public String previousFile(){
		setSortDescend();
		String resultFile;
		mCancelled = false;
//    	Log.d("DirList.nextFile()", "mInitDir = " + mBaseDir);

    	if(mInitSearch){
    		mInitSearch = false;
    		
    		if(!mInitReturn.equals("")){//初回
    			mPreviousReturn = mInitReturn;
    			mInitReturn = "";
    			resultFile = mPreviousReturn;

    		}else{
  //  			String nextFile = searchNextFile(mBaseDir, "");
    			String nextFile = mBaseDir;
    			//ディレクトリも含め次のファイルが見つかったらresultが帰ってくる。
    			//従って、ファイルがディレクトリだったらもう一度検索する。
    			while(new File(nextFile).isDirectory()){
    				if(mCancelled)return "";
    				nextFile = searchNextFile(nextFile, "");
    			}
    				
       			if(!nextFile.equals(""))mPreviousReturn = nextFile;
    			resultFile = nextFile;
    			
    		}
    	}else{
//    		if(mPreviousReturn.equals("") ){
    		if( mBeginOfList ){//既に先頭までいってしまった場合
    			resultFile = "";//前回見つからなかった場合はずっと空を返す。
    		}else{
    			
    			String nextFile;
    			if(mEndOfList){//末尾の場合はmBaseDirをセットして末尾からすべて検索（降順検索）
    				nextFile = mBaseDir;
//    	   			File f = new File(mPreviousReturn);
//        			nextFile = searchNextFile(f.getParent(),f.getName());
    			}else{
    	   			File f = new File(mPreviousReturn);
        			nextFile = searchNextFile(f.getParent(),f.getName());
    			}
 
    			while(new File(nextFile).isDirectory()){
    				if(mCancelled)return "";
    				nextFile = searchNextFile(nextFile, "");
    			}

       			if(!nextFile.equals(""))mPreviousReturn = nextFile;//先頭、末端のファイル名はmPreviousReturnに保持しておく

    			resultFile = nextFile;
    		}
    	}
    	
    	if(resultFile.equals("")){
    		mBeginOfList = true;
    	}else{
    		//ファイルが見つかればそれは先頭でも末尾でもない。
    		mBeginOfList = false;
    		mEndOfList   = false;
    	}
    	return resultFile;
	}

	//次にもう一度同じファイルを取得したい場合は以下を呼ぶ
	public void revert(){
		//初回時の処理が実行されるようにする。
		mInitSearch = true;
		mInitReturn = mPreviousReturn; 
	}

	//現在の検索ファイルを再設定
	//検索ワードが見つからなかったときに、見つかったところまで戻るために使う
	public void setCurrentFile(String filename){
		//現在表示中のファイルの次のファイルが検索されるようにする。
		mPreviousReturn = filename;
   		mBeginOfList = false;
		mEndOfList = false;    		

	}

	
	private String searchNextFile(String currentDir, String fileName){
//		System.out.println("searchNextFile(" + currentDir + " ," + fileName + ")");
		String nextFile = "";

		
		if(!currentDir.startsWith(mBaseDir) ){
//			System.out.println("currentDir:" + currentDir);
//			System.out.println("fileName:" + fileName);
//			System.out.println("mBaseDir:" + mBaseDir );
			return nextFile;// finish!
		}

//		System.out.println("currentDir:" + currentDir);

		ArrayList<File> list = new ArrayList<File>();
		File baseFile = new File(currentDir);
		File[] files = baseFile.listFiles();

		// System.out.println("==== add list ====");
		if(
				!currentDir.equals("/dev") &&
				!currentDir.equals("/proc") && //procファイルシステム配下はskip
				!currentDir.equals("/sys") &&
				files !=  null // return null if basefile is not a directory
				){ 
			for (File f : files) {
				if(f.getName().matches(mFilter) || f.isDirectory()){
					//if endwith(".txt") or endwith(".chi") or isDirectory
					list.add(f);
					// System.out.println("add list:" + f.getAbsolutePath());
				}
			}
		}
		// System.out.println("==================");


		Collections.sort(list, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				File f1 = (File)o1;
				File f2 = (File)o2;
				String data1 = f1.getName();
				String data2 = f2.getName();
				return data1.compareToIgnoreCase(data2)*mSortDirection;
//				return data1.compareTo(data2);
			}
		});
		
		
		//昇順で並べるようにする。//ディレクトリは上
		if(mlistFoldersFirstFlag){
			Collections.sort(list, new Comparator<Object>() {
				public int compare(Object o1, Object o2) {
					File f1 = (File)o1;
					File f2 = (File)o2;
					boolean obj1isDir = f1.isDirectory();
					boolean obj2isDir = f2.isDirectory();
					int ret;
					if(obj1isDir == obj2isDir){
						ret = 0;//両方dirか両方file
					}else if(obj1isDir){ //obj2がdir
						ret = -1; //最初の引数が小さいときは負
					}else{ //obj2がdir
						ret =  1; //最初の引数が大きいときは正
					}
					return ret*mSortDirection;
				}
			});

		}

		/**
		System.out.println("==== list ====");
		for (int i = 0; i < list.size(); i++) {
			File f = list.get(i);
			if( f.isDirectory()){
				System.out.println(f.getAbsolutePath() + "/");
			}else{
				System.out.println(f.getAbsolutePath());
			}
		}
		**/
//		System.out.println("==================");
		//fileNameがある時は、探して次のファイル名を検索
		int index = list.indexOf(new File(currentDir + "/" + fileName));
		//見つからなければ先頭、見つかれば次のファイルのindex
		index++; // if i = -1 ,i -> 0, not if i = -1 ; i + 1;(next index) 

		if(index >= list.size() ){
			//It was last index. and search up stair.
			if(currentDir.equals("/")){
				return ""; //　/をすべて調べ終わったら終わり。
			}else{
				File upDir = new File( currentDir );
				return searchNextFile( upDir.getParent() , upDir.getName());
			}
		}
//		System.out.println("+++++++++++++++++++++++++");


		for (int k = index; k < list.size(); k++) {
			File f = list.get(k);
//			System.out.println("Check file:" + f.getAbsolutePath());
			if(f.isDirectory()){
				/**
				String result = searchNextFile( f.getAbsolutePath(), "");
				if(result.equals("")){
					//skip
				}else{
					return result;
				}
				**/
				//ディレクトリでもいったん返す。Stack Overflow対策
				return f.getAbsolutePath();

			}else{
				return f.getAbsolutePath();
			}
		}



		return nextFile;
	}


	private void setSortAscend(){
		mSortDirection =  1;
	}	
	private void setSortDescend(){
		mSortDirection = -1;
	}

	public void setFoldersFirst(){
		mlistFoldersFirstFlag = true;
	}
	
	public void setCancel(){
		System.out.println("setCancel");

		mCancelled = true;
	}
	public void setFilenameFilter(String filter){
		mFilter = filter;
	}
}
