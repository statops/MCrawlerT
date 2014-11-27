package jp.gr.java_conf.hatalab.mnv;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Config {

	//common
    private static Boolean showButtonsFlag = true;
    private static String PWTimer= "3";
	
	//File Browser View
	private static String initDirName   = "/sdcard";
	private static Boolean listFoldersFirstFlag = false;
	private static float fontSizeOnList = 28;
	private static int fileListOrder = 1;
	//private static String defaultFolderName   = myTemplateText.PREFIX_NORMAL + "NewFolder";
    private static myTemplateText defaultFolderName = new myTemplateText("NewFolder");



	//Note View
    private static String charsetName = "utf-8";
    private static String lineBreak = "auto";
    private static Boolean syncTitleFlag = false;
    private static Boolean autoSaveFlag = false;
    private static Boolean viewerModeFlag  = false;
    private static Boolean noTitleBarFlag = false;
    private static float fontSize = 18; //maybe default is 18 sp?
    private static String typeface = "DEFAULT";




    
    
    
	public static String getPWTimer() {
		return PWTimer;
	}

	public static void setPWTimer(String pWTimer) {
		PWTimer = pWTimer;
	}

	public static String getCharsetName() {
		return charsetName;
	}

	public static void setCharsetName(String charsetName) {
		Config.charsetName = charsetName;
	}

	public static String getLineBreak() {
		return lineBreak;
	}

	public static void setLineBreak(String lineBreak) {
		Config.lineBreak = lineBreak;
	}

	public static Boolean getSyncTitleFlag() {
		return syncTitleFlag;
	}

	public static void setSyncTitleFlag(Boolean syncTitleFlag) {
		Config.syncTitleFlag = syncTitleFlag;
	}

	public static Boolean getShowButtonsFlag() {
		return showButtonsFlag;
	}

	public static void setShowButtonsFlag(Boolean showButtonsFlag) {
		Config.showButtonsFlag = showButtonsFlag;
	}

	public static Boolean getViewerModeFlag() {
		return viewerModeFlag;
	}

	public static void setViewerModeFlag(Boolean viewerModeFlag) {
		Config.viewerModeFlag = viewerModeFlag;
	}

	public static float getFontSize() {
		return fontSize;
	}

	public static void setFontSize(float fontSize) {
		Config.fontSize = fontSize;
	}

	public static Boolean getAutoSaveFlag() {
		return autoSaveFlag;
	}

	public static void setAutoSaveFlag(Boolean autoSaveFlag) {
		Config.autoSaveFlag = autoSaveFlag;
	}

	public static String getTypeface() {
		return typeface;
	}

	public static void setTypeface(String typeface) {
		Config.typeface = typeface;
	}

	public static Boolean getNoTitleBarFlag() {
		return noTitleBarFlag;
	}

	public static void setNoTitleBarFlag(Boolean noTitleBarFlag) {
		Config.noTitleBarFlag = noTitleBarFlag;
	}

	public static int getFileListOrder() {
		return fileListOrder;
	}

	public static void setFileListOrder(int fileListOrder) {
		Config.fileListOrder = fileListOrder;
	}

	public static String getInitDirName() {
		return initDirName;
	}

	public static void setInitDirName(String initDirName) {
		Config.initDirName = initDirName;
	}

	public static Boolean getListFoldersFirstFlag() {
		return listFoldersFirstFlag;
	}

	public static void setListFoldersFirstFlag(Boolean listFoldersFirstFlag) {
		Config.listFoldersFirstFlag = listFoldersFirstFlag;
	}



	public static float getFontSizeOnList() {
		return fontSizeOnList;
	}

	public static void setFontSizeOnList(float size) {
		Config.fontSizeOnList = size;
	}
	
    public static myTemplateText getDefaultFolderName() {
		return defaultFolderName;
	}

	public static void setDefaultFolderName(myTemplateText defaultFolderName) {
		Config.defaultFolderName = defaultFolderName;
	}

	
	public static void update(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
 
        //fontsizeセット
        fontSizeOnList = sharedPreferences.getFloat(context.getText(R.string.prefFontSizeOnListKey).toString(), fontSizeOnList);

        //初期フォルダを取得        
        initDirName = sharedPreferences.getString(context.getText(R.string.prefInitDirKey).toString(), initDirName);

        //デフォルオフォルダ名を取得 
         String str = sharedPreferences.getString(context.getText(R.string.prefDefaultFolderNameKey).toString(), defaultFolderName.getPrefString());
         defaultFolderName.setPrefString(str);
        
        //ListFoldersFirst
        listFoldersFirstFlag = sharedPreferences.getBoolean(context.getText(R.string.prefListFoldersFirstKey).toString(), listFoldersFirstFlag);
        //リスト順序　昇順か、降順
        fileListOrder = sharedPreferences.getInt(context.getText(R.string.prefFileListOrderKey).toString(), fileListOrder);
 
        // flag of auto save mode
        autoSaveFlag = sharedPreferences.getBoolean(context.getText(R.string.prefAutoSaveKey).toString(), autoSaveFlag);

        //type face
        typeface = sharedPreferences.getString(context.getText(R.string.prefTypefaceKey).toString(), typeface);

        // flag of auto save mode
        noTitleBarFlag = sharedPreferences.getBoolean(context.getText(R.string.prefNoTitleBarKey).toString(), noTitleBarFlag);

        
        //パスワードタイマー
        PWTimer = sharedPreferences.getString(context.getText(R.string.prefPWResetTimerKey).toString(), "3");
        //character set
        charsetName = sharedPreferences.getString(context.getText(R.string.prefCharsetNameKey).toString(), "utf-8");
        // flag of sync title
        syncTitleFlag = sharedPreferences.getBoolean(context.getText(R.string.prefSyncTitleKey).toString(), false);
        // flag of showing buttons
        showButtonsFlag = sharedPreferences.getBoolean(context.getText(R.string.prefShowButtonsKey).toString(), true);
        // flag of viewerMode
        viewerModeFlag = sharedPreferences.getBoolean(context.getText(R.string.prefViewerModeKey).toString(), false);
        // fontSize
//        fontSizeString = sharedPreferences.getString(getText(R.string.prefFontSizeKey).toString(), fontSizeString);
        fontSize = sharedPreferences.getFloat(context.getText(R.string.prefFontSizeKey).toString(), fontSize);
        //line break
        lineBreak = sharedPreferences.getString(context.getText(R.string.prefLineBreakCodeKey).toString(), "auto");

        
	}
}
