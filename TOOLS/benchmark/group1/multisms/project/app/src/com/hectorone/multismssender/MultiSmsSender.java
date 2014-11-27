package com.hectorone.multismssender;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class MultiSmsSender extends Activity {
	private Button mAddButton;
	private Button mAddGroupButton;
	private Button mSend;
	private TextView mContacts;
	private TextView mEditor;
	private CheckBox mDeliveryCheckBox;

	private ProgressDialog mSendingDialog;

	public static final int ACTIVITY_EDIT      = 0;
	public static final int ACTIVITY_ADD_GROUP = 1;
	public static final int ACTIVITY_DELIVERY  = 2;

	private static final int INSERT_ID         = Menu.FIRST;

	public static final int MANY_MESSAGE       = 50;

	private static final int DIALOG_PROGRESS        = 0;
	private static final int DIALOG_FINISHED        = 1;
	private static final int DIALOG_NONUMBER        = 2;
	private static final int DIALOG_MANYMESSAGE     = 3;
	private static final int DIALOG_STARTWAIT       = 4;
	private static final int DIALOG_PROGRESS_CANCEL = 5;
	private static final int SENDING_DIALOG_KEY     = 6;

	public static final String PARAM_NUMBERS_LIST = "param number list";
	public static final String PARAM_FLUSH = "param flush";
	public static final String PARAM_ENTRY_ID = "entry_id";

	public static final String DEBUG_TAG="MultiSmsSender";

	private boolean appli_running = true;

	MessageSenderThread mThreadSender;
	private boolean mManyMessageContinue;

	final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			int type = msg.getData().getInt("ORIGIN");

			switch (type) {
			case DIALOG_PROGRESS: {
				int total = msg.getData().getInt("total");
				mSendingDialog.setProgress(total);
			}
				break;

			case DIALOG_PROGRESS_CANCEL: {
				mSendingDialog.cancel();
			}
				break;

			case DIALOG_FINISHED: {
				int total = msg.getData().getInt("total");
				new AlertDialog.Builder(MultiSmsSender.this)
						.setPositiveButton(
								getResources().getString(R.string.ok),
								new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();

									}

								})
						.setMessage(
								total
										+ " "
										+ getResources().getString(
												R.string.message_sent)).show();
				break;
			}

			case DIALOG_NONUMBER: {
				new AlertDialog.Builder(MultiSmsSender.this)
						.setPositiveButton(
								getResources().getString(R.string.ok),
								new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();

									}
								})
						.setMessage(
								getResources().getString(R.string.enter_number))
						.show();
				break;
			}
			case DIALOG_MANYMESSAGE: {

				new AlertDialog.Builder(MultiSmsSender.this)
						.setMessage(
								getResources().getString(
										R.string.warning_many_message))
						.setCancelable(false)
						.setPositiveButton(
								getResources().getString(R.string.ok),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										synchronized (MultiSmsSender.this) {
											MultiSmsSender.this.notify();
											mManyMessageContinue = true;
										}
										dialog.dismiss();
									}
								})
						.setNegativeButton(
								getResources().getString(R.string.cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										synchronized (MultiSmsSender.this) {
											MultiSmsSender.this.notify();
											mManyMessageContinue = false;
										}
										dialog.dismiss();
									}
								}).show();
				break;

			}
			case DIALOG_STARTWAIT: {
				new AlertDialog.Builder(MultiSmsSender.this)
						.setPositiveButton(
								getResources().getString(R.string.ok),
								new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog,
											int which) {
										synchronized (MultiSmsSender.this) {
											MultiSmsSender.this.notify();
										}
										dialog.dismiss();

									}
								})
						.setMessage(
								getResources().getString(R.string.more_message))
						.show();
				break;

			}
			}

		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mAddButton         = (Button) findViewById(R.id.contacts);
		mAddGroupButton    = (Button) findViewById(R.id.groups);
		mSend              = (Button) findViewById(R.id.send);
		mContacts          = (TextView) findViewById(R.id.numbers);
		mEditor            = (TextView) findViewById(R.id.editor);
		mDeliveryCheckBox  = (CheckBox) findViewById(R.id.deliveryCheckBox);

		mContacts.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		// mEditor.setImeOptions(EditorInfo.IME_ACTION_DONE);

		mAddButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				selectNumbers();
			}
		});

		mAddGroupButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				display_group_list();
			}
		});

		mSend.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				showDialog(SENDING_DIALOG_KEY);
				mThreadSender = new MessageSenderThread(mHandler);
				mThreadSender.start();
			}
		});
	}

	public void selectNumbers() {
		// startActivityForResult(new Intent(Intent.ACTION_PICK,
		// People.CONTENT_URI), 0);
		Intent i = new Intent(this, PhoneNumberSelection.class);
		String rawNumbers = mContacts.getText().toString();
		String[] numbers = rawNumbers.split(",");
		i.putExtra(PARAM_NUMBERS_LIST, numbers);
		startActivityForResult(i, ACTIVITY_EDIT);
	}

	private class MessageSenderThread extends Thread {

		Handler mHandler;

		public MessageSenderThread(Handler h) {
			mHandler = h;
		}

		public synchronized void run() {
			super.run();
			sendMessage(mHandler);
		}

	}

	public void sendMessage(Handler handler) {

		SmsManager manager                  = SmsManager.getDefault();
		String message                      = mEditor.getText().toString();
		HashMap<String, Long> deliveryIdMap = new HashMap<String, Long>();
		mManyMessageContinue                = true;

		if("".equals(message)) {
			{
				displayDialog(handler, DIALOG_PROGRESS_CANCEL,null);
			}
			return;
		}

		String[] numbers                     = mContacts.getText().toString().split(",");
		ArrayList<String> phoneNumberConform = new ArrayList<String>();
		int size                             = numbers.length;
		boolean haveDeliveryReports          = mDeliveryCheckBox.isChecked();
		long messageId                       = -1;
		ArrayList<String> messages           = manager.divideMessage(message);
		int messageCount                     = messages.size();

		if (haveDeliveryReports) {
			ContentValues values = new ContentValues(2);

			values.put(DeliveryDbAdapter.KEY_MESSAGE_NAME,
					message.substring(0, Math.min(30, message.length()))
							.replace('\n', ' '));
			values.put(DeliveryDbAdapter.KEY_MESSAGE_DATE, DateFormat
					.getDateInstance().format(new Date()));

			messageId = Long.parseLong(getContentResolver()
					.insert(DeliveryDbAdapter.CONTENT_MESSAGE_URI, values)
					.getPathSegments().get(1));
		}

		// Check if numbers are correct and prepare deliveryId
		for (int i = 0; i < size; i++) {
			String newN = numbers[i].trim();
			newN = newN.replace(" ", "");
			if (!newN.equals("")
					&& PhoneNumberUtils.isWellFormedSmsAddress(newN)
					&& !phoneNumberConform.contains(newN)) {
				phoneNumberConform.add(newN);
				if (haveDeliveryReports) {
					ContentValues values = new ContentValues(3);
					values.put(DeliveryDbAdapter.KEY_DELIVERY_ENTRY_NAME,
							nameFromNumber(getContentResolver(), newN));
					values.put(DeliveryDbAdapter.KEY_DELIVERY_ENTRY_NUMBER,
							newN);
					values.put(DeliveryDbAdapter.KEY_DELIVERY_ENTRY_MESSAGE_ID,
							messageId);

					long entryId = Long.parseLong(getContentResolver()
							.insert(DeliveryDbAdapter.CONTENT_DELIVERY_URI,
									values).getPathSegments().get(1));
					deliveryIdMap.put(newN, entryId);

				}

			}
		}

		numbers = new String[size];
		numbers = phoneNumberConform.toArray(numbers);
		size    = phoneNumberConform.size();

		if (size != 0) {
			if (size > MANY_MESSAGE) {
				{
					displayDialog(handler, DIALOG_MANYMESSAGE, null);
				}
				synchronized (MultiSmsSender.this) {
					try {
						MultiSmsSender.this.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}

			if (mManyMessageContinue) {

				int message_sent = 0;
				int chunk_max    = Math.min(MANY_MESSAGE, size);
				do {
					if (message_sent > 0) {
						displayDialog(handler, DIALOG_STARTWAIT, null);
						synchronized (MultiSmsSender.this) {
							try {
								MultiSmsSender.this.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

					}
					// Log.d(DEBUG_TAG,
					// "size is"+size+" message_sent "+message_sent+" max"+chunk_max);
					for (int i = message_sent; i < chunk_max; i++) {
						message_sent++;
						String newN = numbers[i];
						Message msg = handler.obtainMessage();
						Bundle b    = new Bundle();
						b.putInt("ORIGIN", DIALOG_PROGRESS);
						b.putInt("total", (i * 100) / size);
						msg.setData(b);
						handler.sendMessage(msg);

						ArrayList<PendingIntent> deliveryIntents = null;
						ArrayList<PendingIntent> sentIntents = null;

						if (haveDeliveryReports) {
							deliveryIntents = new ArrayList<PendingIntent>(
									messageCount);
							sentIntents = new ArrayList<PendingIntent>(
									messageCount);
							// Add to the Google MMS app
							ContentValues values = new ContentValues();
							values.put("address", newN);
							values.put("body", message);

							getContentResolver().insert(
									Uri.parse("content://sms/sent"), values);

							long entryId = deliveryIdMap.get(newN);
							// Log.d(DEBUG_TAG,
							// "entry is "+entryId+" to number"+newN);
							for (int j = 0; j < messageCount; j++) {
								if (j == (messageCount - 1)) {
									Uri entryURI = Uri
											.withAppendedPath(
													DeliveryDbAdapter.CONTENT_DELIVERY_URI,
													"" + entryId);
									Intent intent = new Intent(
											MessageReceiver.MESSAGE_RECEIVED,
											entryURI, this,
											MessageReceiver.class);

									deliveryIntents.add(PendingIntent
											.getBroadcast(this, 0, intent, 0));
								} else {
									deliveryIntents.add(null);
								}
							}

						}
						manager.sendMultipartTextMessage(newN, null, messages,
								sentIntents, deliveryIntents);

					}
					chunk_max = Math.min(message_sent + MANY_MESSAGE, size);

				} while (((size - message_sent) > 0) && appli_running);

				Message msg = handler.obtainMessage();
				Bundle b    = new Bundle();
				b.putInt("ORIGIN", DIALOG_FINISHED);
				b.putInt("total", phoneNumberConform.size());
				msg.setData(b);
				handler.sendMessage(msg);
			}
			displayDialog(handler, DIALOG_PROGRESS_CANCEL, null);

		} else {
			displayDialog(handler, DIALOG_NONUMBER, null);
		}

	}

	private void displayDialog(Handler handler, int dialogId,
			HashMap<String, Integer> params) {
		Message msg = handler.obtainMessage();
		Bundle b = new Bundle();
		b.putInt("ORIGIN", dialogId);
		if (params != null) {
			for (String paramName : params.keySet()) {
				b.putInt(paramName, params.get(paramName));
			}
		}
		msg.setData(b);
		handler.sendMessage(msg);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case SENDING_DIALOG_KEY: {
			mSendingDialog = new ProgressDialog(this);
			mSendingDialog.setTitle(R.string.sending);
			mSendingDialog.setMessage(getResources().getString(R.string.wait));
			mSendingDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mSendingDialog.setCancelable(false);

			return mSendingDialog;
		}
		}
		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case ACTIVITY_ADD_GROUP:
		case ACTIVITY_EDIT:
			if (intent != null) {
				String[] numbers = intent.getExtras().getStringArray(
						PARAM_NUMBERS_LIST);
				boolean flush = intent.getExtras().getBoolean(PARAM_FLUSH);
				String string = "";
				HashSet<String> res = new HashSet<String>();
				for (int i = 0; i < numbers.length; i++) {
					String newN = numbers[i].trim();
					if (!newN.equals("")) {
						res.add(newN);
					}
				}
				if (!flush) {
					String oldContactsString = mContacts.getText().toString();
					String[] oldContacts = oldContactsString.split(",");
					for (int i = 0; i < oldContacts.length; i++) {
						String newN = oldContacts[i].trim();
						if (!newN.equals("")) {
							res.add(newN);
						}
					}
				}

				for (String number : res) {
					string += number + ", ";
				}

				mContacts.setText(string);
			}
			break;

		default:
			break;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, INSERT_ID, 0, R.string.add_group);
		menu.add(0, INSERT_ID + 1, 0, R.string.delivery);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case INSERT_ID:
			display_group_list();
			return true;
		case INSERT_ID + 1:
			display_delivery_list();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	protected void onDestroy() {
		appli_running = false;
		super.onDestroy();
	}

	public void display_group_list() {

		Intent i = new Intent(this, SelectGroupActivity.class);
		startActivityForResult(i, ACTIVITY_ADD_GROUP);
	}

	public void display_delivery_list() {

		Intent i = new Intent(this, SelectDeliveryActivity.class);
		startActivityForResult(i, ACTIVITY_DELIVERY);
	}

	// *********************** HELPER ****************************************

	public String nameFromNumber(ContentResolver resolver, String number) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));
		Cursor c = null;
		try {
			c = resolver.query(uri, new String[] { PhoneLookup.DISPLAY_NAME },
					null, null, null);
		} catch (Exception e) {
			return "";
		}

		if (c != null) {
			c.moveToFirst();
			if (c.isFirst()) {
				return c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
			} else {
				return "";
			}
		}
		return "";
	}

}
