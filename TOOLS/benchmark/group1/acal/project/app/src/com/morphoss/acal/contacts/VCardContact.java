package com.morphoss.acal.contacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContacts.Data;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.AcalProperty;
import com.morphoss.acal.davacal.PropertyName;
import com.morphoss.acal.davacal.VCard;
import com.morphoss.acal.davacal.VComponent;
import com.morphoss.acal.davacal.VComponentCreationException;
import com.morphoss.acal.service.connector.Base64Coder;

public class VCardContact {
	
	public final static String TAG = "aCal VCardContact";

	private static final Pattern structuredAddressMatcher = Pattern.compile("^(.*);(.*);(.*);(.*);(.*);(.*);(.*)$");
	private static final Pattern structuredNameMatcher = Pattern.compile("^(.*);(.*);(.*);(.*);(.*)$");
	private static final Pattern simpleSplit = Pattern.compile("[.]");
	
	private final Resource vCardRow;
	private final VCard sourceCard;
	private Map<String,Set<AcalProperty>> typeMap = null;
	private Map<String,Set<AcalProperty>> groupMap = null;
	private AcalProperty uid = null;
	private int sequence = 0;
	
	private ArrayList<ContentProviderOperation> ops = null;
	private ContentResolver	cr;

	public VCardContact( Resource resourceRow ) throws VComponentCreationException {
		vCardRow = resourceRow;
		try {
			sourceCard = (VCard) VComponent.createComponentFromResource(resourceRow);
			sourceCard.setEditable();
		}
		catch ( Exception e ) {
			Log.w(TAG,"Could not build VCard from resource", e);
			throw new VComponentCreationException("Could not build VCard from resource."); 
		}
 
		AcalDateTime revisionTime = AcalDateTime.fromAcalProperty(sourceCard.getProperty(PropertyName.REV));
		if ( revisionTime == null ) {
			revisionTime = vCardRow.getLastModified();
		}
		sequence = (int) ((revisionTime.getEpoch() - 1000000000L) % 2000000000L);
		
		uid = sourceCard.getProperty(PropertyName.UID);
		if ( uid == null ) {
			uid = new AcalProperty("UID", Long.toString(vCardRow.getResourceId()));
		}
		buildTypeMap();
		
	}

	
	/**
	 * Traverses the properties, building an index by type and another by association.
	 * 
	 * VCARD properties may be either like "PROPERTY:VALUE" or possibly as "aname.property:VALUE" (case is irrelevant) and
	 * this is building an index so we can get all "PROPERTY" properties from typeMap and all "aname" properties from groupMap
	 * 
	 */
	private void buildTypeMap() {
		typeMap = new HashMap<String,Set<AcalProperty>>();
		groupMap = new HashMap<String,Set<AcalProperty>>();

		AcalProperty[] vCardProperties = sourceCard.getAllProperties();
		String[] nameSplit;
		Set<AcalProperty> s;
		for( AcalProperty prop : vCardProperties ) {
			nameSplit = simpleSplit.split(prop.getName().toUpperCase(Locale.US),2);
			if ( nameSplit.length == 1 ) {
				s = typeMap.get(nameSplit[0]);
				if ( s == null ) {
					s = new HashSet<AcalProperty>();
					typeMap.put(nameSplit[0], s);
				}
				s.add(prop);
			}
			else {
				s = typeMap.get(nameSplit[1]);
				if ( s == null ) {
					s = new HashSet<AcalProperty>();
					typeMap.put(nameSplit[1], s);
				}
				s.add(prop);

				s = groupMap.get(nameSplit[0]);
				if ( s == null ) {
					s = new HashSet<AcalProperty>();
					groupMap.put(nameSplit[0], s);
				}
				s.add(prop);
			}
		}
	}

	public String getUid() {
		return uid.getValue();
	}

	public String getFullName() {
		if ( sourceCard == null ) return null;
		AcalProperty fnProp = sourceCard.getProperty(PropertyName.FN);
		if ( fnProp == null ) return null;
		return fnProp.getValue();
	}

	public int getSequence() {
		return sequence;
	}

	
	public void writeToContact(Context context, Account account, Integer androidContactId) {
		this.cr = context.getContentResolver();
		this.ops = new ArrayList<ContentProviderOperation>();
		if ( androidContactId < 0 ) {
			Log.println(Constants.LOGD,TAG,"Inserting data for '"+sourceCard.getProperty(PropertyName.FN).getValue()+"'");
			ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
						.withValue(RawContacts.ACCOUNT_TYPE, account.type)
						.withValue(RawContacts.ACCOUNT_NAME, account.name)
						.withValue(RawContacts.SYNC1, this.getUid())
						.withValue(RawContacts.VERSION, this.getSequence())
						.build());

			this.writeContactDetails(true, 0);
		}
		else {
			Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, androidContactId);
			Log.println(Constants.LOGD,TAG,"Updating data for '"+sourceCard.getProperty(PropertyName.FN).getValue()+"'");

			ops.add(ContentProviderOperation.newUpdate(rawContactUri)
					.withYieldAllowed(true)
					.withValue(RawContacts.ACCOUNT_TYPE, account.type)
					.withValue(RawContacts.ACCOUNT_NAME, account.name)
					.withValue(RawContacts.SYNC1, this.getUid())
					.withValue(RawContacts.VERSION, this.getSequence())
					.build());


			this.writeContactDetails(false, androidContactId);

		}
		
		try {
			Log.println(Constants.LOGD,TAG,"Applying update batch: "+ops.toString());
			context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
		}
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,Log.getStackTraceString(e));
		}
		catch (OperationApplicationException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,Log.getStackTraceString(e));
		}
	}
	
	private void writeContactDetails(boolean isInsert, int rawContactId) {
		String propertyName;
		AcalProperty[] vCardProperties = sourceCard.getAllProperties();
		Uri contactDataUri = ContactsContract.Data.CONTENT_URI;
		if ( !isInsert )
			contactDataUri.buildUpon().appendQueryParameter(ContactsContract.Data.RAW_CONTACT_ID, ""+rawContactId);

		for (AcalProperty prop : vCardProperties) {
			propertyName = prop.getName();
			String nameSplit[] = simpleSplit.split(prop.getName().toUpperCase(Locale.US),2);
			propertyName = (nameSplit.length == 2 ? nameSplit[1] : nameSplit[0]);

			try {
				if ( propertyName.equals("FN") ) 		doStructuredName(isInsert, rawContactId, prop, sourceCard.getProperty("N"));
				else if ( propertyName.equals("TEL") )	doPhone(isInsert, rawContactId, prop);
				else if ( propertyName.equals("ADR") )	doStructuredAddress(isInsert, rawContactId, prop);
				else if ( propertyName.equals("EMAIL")) doEmail(isInsert, rawContactId, prop);
				else if ( propertyName.equals("PHOTO")) doPhoto(isInsert, rawContactId, prop);
				else
					continue;
			}
			catch( Exception e ) {
				Log.e(TAG,"Error processing VCARD "+propertyName+" in \n"+sourceCard.getCurrentBlob()+"\n", e);
			}

		}
	}


	private void applyOp(String type, Builder op) {
		Log.println(Constants.LOGD,TAG,"Applying "+type+" change for:"+op.build().toString());
		ops.add(op.build());
	}


	private ContentProviderOperation.Builder beginOp(boolean isInsert, int rawContactId, String mimeType, String selection, String[] selectionArgs ) {
		ContentProviderOperation.Builder op;
		if ( isInsert ) {
			op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
			op.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
		}
		else {
			selection = Data.RAW_CONTACT_ID + "="+rawContactId+" AND "+Data.MIMETYPE + "='"+mimeType+"' "+(selection==null?"":" AND "+selection);
			Cursor cur = cr.query(ContactsContract.Data.CONTENT_URI, new String[] {Data._ID }, selection, selectionArgs, null);

			if ( cur.getCount() == 0 )
				op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
			else
				op = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
						.withSelection(selection, selectionArgs);
		}
		op.withValue(Data.MIMETYPE, mimeType);
		return op;
	}
	
	private void doStructuredName(boolean isInsert, int rawContactId, AcalProperty fnProp, AcalProperty nProp) {
		Log.v(TAG,"Processing field FN:"+fnProp.getValue());

		ContentProviderOperation.Builder op = beginOp( isInsert, rawContactId, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, null,null);

		if ( nProp != null ) {
			Matcher m = structuredNameMatcher.matcher(nProp.getValue());
			if ( m.matches() ) {
				/**
				 * The structured property value corresponds, in
				 * sequence, to the Surname (also known as family name), Given Names,
				 * Honorific Prefixes, and Honorific Suffixes.
				 */
				op.withValue(CommonDataKinds.StructuredName.FAMILY_NAME, m.group(1));
				op.withValue(CommonDataKinds.StructuredName.GIVEN_NAME, m.group(2));
				op.withValue(CommonDataKinds.StructuredName.PREFIX, m.group(3));
				op.withValue(CommonDataKinds.StructuredName.SUFFIX, m.group(4));
				Log.v(TAG,"Processing 'N' field: '"+nProp.getValue()+"' prefix>"
							+ m.group(3) + "< firstname> " + m.group(2) + "< lastname>" + m.group(1) + "< suffix>" + m.group(4));
			}
		}
	
		op.withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, fnProp.getValue());
		applyOp("FN",op);
	}


	private void doPhone(boolean isInsert, int rawContactId, AcalProperty telProp ) {
		String phoneTypeName = telProp.getParam("TYPE");
		int phoneType = CommonDataKinds.Phone.TYPE_OTHER;
		if ( phoneTypeName != null ) {
			phoneTypeName = phoneTypeName.toUpperCase(); 
			if ( phoneTypeName.contains("HOME") )		phoneType = CommonDataKinds.Phone.TYPE_HOME;
			else if ( phoneTypeName.contains("WORK") )	phoneType = CommonDataKinds.Phone.TYPE_WORK;
			else if ( phoneTypeName.contains("CELL") )	phoneType = CommonDataKinds.Phone.TYPE_MOBILE;
		}

		ContentProviderOperation.Builder op = beginOp( isInsert, rawContactId, CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
				CommonDataKinds.Phone.TYPE+"="+phoneType+"", null);

		op.withValue(CommonDataKinds.Phone.NUMBER,telProp.getValue());
		
		applyOp("TEL",op);
	}


	private void doStructuredAddress(boolean isInsert, int rawContactId, AcalProperty adrProp) {
		String addressTypeName = adrProp.getParam("TYPE").toUpperCase();
		if ( addressTypeName == null ) addressTypeName = "";
		else addressTypeName.toUpperCase();
		
		Log.v(TAG,"Processing field ADR:"+addressTypeName+":"+adrProp.getValue());
	
		Matcher m = structuredAddressMatcher.matcher(adrProp.getValue());
		if ( m.matches() ) {

			int opType = CommonDataKinds.StructuredPostal.TYPE_OTHER;
			if ( addressTypeName.contains("HOME") ) opType = CommonDataKinds.StructuredPostal.TYPE_HOME;
			else if ( addressTypeName.contains("WORK") ) opType = CommonDataKinds.StructuredPostal.TYPE_WORK;

			ContentProviderOperation.Builder op = beginOp( isInsert, rawContactId, CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
					CommonDataKinds.StructuredPostal.TYPE+"="+opType+"", null);

			op.withValue(CommonDataKinds.StructuredPostal.TYPE, opType);
			
			/**
			 * The structured type value corresponds, in sequence, to the post office box; the extended
			 * address (e.g. apartment or suite number); the street address; the locality (e.g., city); 
			 * the region (e.g., state or province); the postal code; the country name.
			 */
			op.withValue(CommonDataKinds.StructuredPostal.POBOX, m.group(1));
			if ( m.group(2) == null || m.group(2).equals("") )
				op.withValue(CommonDataKinds.StructuredPostal.STREET, m.group(3));
			else
				op.withValue(CommonDataKinds.StructuredPostal.STREET, m.group(2) + " / " + m.group(3));
				
			op.withValue(CommonDataKinds.StructuredPostal.CITY, m.group(4));
			op.withValue(CommonDataKinds.StructuredPostal.REGION, m.group(5));
			op.withValue(CommonDataKinds.StructuredPostal.POSTCODE, m.group(6));
			op.withValue(CommonDataKinds.StructuredPostal.COUNTRY, m.group(7));
			
			applyOp("ADR",op);
		}
		else {
			Log.i(TAG,"Ignoring badly structured ADR data in '"+adrProp.toRfcString()+"'");
		}
	}


	private void doEmail(boolean isInsert, int rawContactId, AcalProperty emailProp) {
		String emailTypeName = emailProp.getParam("TYPE");
		if ( emailTypeName == null ) emailTypeName = "";
		else emailTypeName.toUpperCase();
		int emailType = CommonDataKinds.Email.TYPE_OTHER;
		if ( emailTypeName.contains("HOME") )		emailType = CommonDataKinds.Email.TYPE_HOME;
		else if ( emailTypeName.contains("WORK") ) 	emailType = CommonDataKinds.Email.TYPE_WORK;
		Log.v(TAG,"Processing field EMAIL:"+emailTypeName+":"+emailProp.getValue());

		ContentProviderOperation.Builder op = beginOp( isInsert, rawContactId, CommonDataKinds.Email.CONTENT_ITEM_TYPE,
				CommonDataKinds.Email.TYPE+"="+emailType+"", null);
		op.withValue(CommonDataKinds.Email.DATA,emailProp.getValue());
		
		applyOp("EMAIL",op);
	}


	private void doPhoto(boolean isInsert, int rawContactId, AcalProperty prop) {
		byte[] decodedString = Base64Coder.decode(prop.getValue().replaceAll(" ",""));
		ContentProviderOperation.Builder op = beginOp( isInsert, rawContactId, CommonDataKinds.Photo.CONTENT_ITEM_TYPE,
				null, null);
		op.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO,decodedString);
		Log.v(TAG,"Processing field PHOTO:"+prop.getValue());
		applyOp("PHOTO",op);
	}


	public static ContentValues getAndroidContact(Context context, Integer rawContactId) {
		Uri contactDataUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
		Cursor cur = context.getContentResolver().query(contactDataUri, null, null, null, null);
		try {
			if ( cur.moveToFirst() ) {
				ContentValues result = new ContentValues();
				DatabaseUtils.cursorRowToContentValues(cur, result);
				cur.close();
				return result;
			}
		}
		catch( Exception e ) {
			Log.w(TAG,"Could not retrieve Android contact",e);
		}
		finally {
			if ( cur != null ) cur.close();
		}
		return null;
	}


	public void writeToVCard(Context context, ContentValues androidContact) {
		sourceCard.setEditable();
		
		Log.println( Constants.LOGD, TAG, "I should write this to a VCard!" );
		for( Map.Entry<String,Object> androidValue : androidContact.valueSet() ) {
			String key = androidValue.getKey();
			Object value = androidValue.getValue();
			Log.println( Constants.LOGD, TAG, key+"="+(value == null ? "null" : value.toString()) );
		}
	}
	
}
