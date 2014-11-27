package jp.gr.java_conf.hatalab.mnv;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class ToastMaster {
	private static Toast sToast = null;
//	private static int   sResId = 0;
	private static Context sContext;
	
	
    public ToastMaster() {
 //       Log.d("ToastMaster","ToastMaster()");
	}

    public static void makeTextAndShow(Context context, int resId, int duration){
//        Log.d("ToastMaster","makeText()");
        //if(resId == sResId){
        if(sToast != null && sContext == context){
        	//cancel
//            Log.d("ToastMaster","makeText().cancel");
            sToast.cancel();
            sToast.setDuration(duration);
            sToast.setText(resId);
            sToast.show();
        }else{
        	//sResId = resId;
        	sContext = context;
        	sToast = Toast.makeText(context, resId, duration);
        	sToast.show();
        }
    }


}