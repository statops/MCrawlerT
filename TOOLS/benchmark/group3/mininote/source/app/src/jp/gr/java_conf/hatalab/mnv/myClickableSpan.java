package jp.gr.java_conf.hatalab.mnv;

import java.util.regex.Matcher;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class myClickableSpan extends ClickableSpan{
	

	TextView tvPost;
	String sText;
	Activity mActivity;

	public myClickableSpan(String url, EditText tvPost,Activity activity) {
		super();
		this.sText = url;
		this.tvPost = tvPost;
		this.mActivity = activity;
	}

	@Override
	public void onClick(View v){
		int start = ((TextView)v).getSelectionStart();
		int end   = ((TextView)v).getSelectionEnd();
		String selectedText = ((TextView)v).getText().subSequence(start, end).toString();		
//		Log.v("ClickableSpan", "onClick occur : " + sText);
//		Log.v("ClickableSpan", "onClick occur : " + selectedText);
		
		Matcher matcherURL = MyUtil.WEB_URL_PATTERN.matcher(selectedText);
		if(matcherURL.find()){
			String s = matcherURL.group();
			Uri uri = Uri.parse(s);
			Intent i = new Intent(Intent.ACTION_VIEW,uri);
			mActivity.startActivity(i);
			return ;
		}

		Matcher matcherEmail = MyUtil.EMAIL_ADDRESS_PATTERN.matcher(selectedText);
		if(matcherEmail.find()){
			String s = matcherEmail.group();
			Uri uri = Uri.parse("mailto:" + s);
			Intent i = new Intent(Intent.ACTION_VIEW,uri);
			mActivity.startActivity(i);
			return ;
		}

		Matcher matcherTEL = MyUtil.PHONE_PATTERN.matcher(selectedText);
		if(matcherTEL.find()){
			String s = matcherTEL.group();
			Uri uri = Uri.parse("tel:" + s);
			Intent i = new Intent(Intent.ACTION_VIEW,uri);
			mActivity.startActivity(i);
			return ;
		}


		
	}


}
