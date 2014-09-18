/**
 * 
 */
package kit.Inj;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import kit.Config.Config;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Tree;
import kit.Stress.Event;
import kit.TestRunner.AbstractCrawler;
import kit.Utils.SgUtils;
import kit.Utils.WriterEvent;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

/**
 * @author Stassia
 * 
 */
public class AbstractInjectionRunner extends AbstractCrawler {

	protected Table mTable;
	protected static State mState_To_Inj;

	/**
	 * @param pkg
	 * @param activityClass
	 */
	public AbstractInjectionRunner(String pkg,
			@SuppressWarnings("rawtypes") Class activityClass) {
		super(pkg, activityClass);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mOpenHelper = new DatabaseHelper(mContext);
	}

	/**
	 * Create table object from dataBase
	 * 
	 * @param table
	 */
	protected Table _createTableObject() {
		Table table = new Table(mTABLE_NAME);

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		/**
		 * completer l'elemnt de la table
		 */
		Cursor c = null;
		try {
			c = db.query(mTABLE_NAME, null, null, null, null, null, null);
		} catch (SQLiteException tableError) {
			return null;
		}

		assertNotNull(c);
		/**
		 * recuperation des valeurs
		 */
		c.moveToFirst();
		/**
		 * Noms des colonnes
		 */
		for (String name : c.getColumnNames()) {
			/**
			 * �a depend des API
			 */
			// table.createColumn(name, "" + c.getType(c.getColumnIndex(name)));
			table.createColumn(name, "text");
		}

		do {
			String _id = c.getString(c.getColumnIndex(Table.TestColumns._ID));
			table.getRow().add(Integer.parseInt(_id));
			for (int i = 0; i < c.getColumnCount(); i++) {
				String col = c.getColumnName(i);
				if (Config.DEBUG) {
					Log.d(TAG, "col " + col);
				}

				// int type = c.getType(i);
				if (Config.DEBUG) {
					// Log.d(TAG, "type " + type);
				}

				String value = c.getString(i);
				if (Config.DEBUG) {
					Log.d(TAG, "value " + value);
				}
				table.addValue(col, value, _id);
			}
			c.moveToNext();
		} while (!c.isAfterLast());
		db.close();
		return table;
	}

	/**
	 * @return
	 * @throws IOException
	 * 
	 */
	private String readDatabaseName() throws IOException {
		ArrayList<String> dbvalue = (ArrayList<String>) FileUtils.readLines(
				getDataBaseFile(), Config.UTF8);
		if (dbvalue == null || dbvalue.isEmpty()) {
			return null;
		} else {
			return dbvalue.get(0);
		}
	}

	/**
	 * @throws IOException
	 * 
	 */
	protected void initTestTable() throws IOException {
		if (Config.DEBUG) {
			Log.d(TAG, "initTestTable ");
		}
		// read database name
		mDATABASE_NAME = readDatabaseName();
		createTableInScratch();
		// Launch test table creation
		mTable = _createTableObject();
		// crate an initial value
		do_testTable();

	}

	protected static String mDATABASE_NAME;
	protected static int mDATABASE_VERSION = 100;
	protected static String mTABLE_NAME;

	/**
	 * 
	 */
	protected void do_testTable() {

		assertEquals("VULNERABLE : column size", 3, mTable.getColumns().size());
		assertEquals("VULNERABLE : row size", 1, mTable.getRow().size());
		assertEquals("VULNERABLE : row id_", "" + 1, ""
				+ mTable.getRow().get(0));
		for (String name : mTable.getColumns().keySet()) {
			if (name.equalsIgnoreCase(TestColumns._ID)) {
				assertEquals("VULNERABLE : " + TestColumns._ID + " changed",
						"1", mTable.getColumns().get(name).getValue("1"));
			}
			if (name.equalsIgnoreCase(TestColumns.TITLE)) {
				assertEquals("VULNERABLE : " + TestColumns.TITLE + " changed",
						"title", mTable.getColumns().get(name).getValue("1"));
			}
			if (name.equalsIgnoreCase(TestColumns.COLOUMN_1)) {
				assertEquals("VULNERABLE : " + TestColumns.COLOUMN_1
						+ " changed", "detail", mTable.getColumns().get(name)
						.getValue("1"));
			}

		}

	}

	/**
	 * 
	 */
	private void createTableInScratch() {
		mTABLE_NAME = Config.TEST_TABLE;
		mOpenHelper = new DatabaseHelper(mContext);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		if (Config.DEBUG) {
			Log.d(TAG, "DROP TABLE IF EXISTS");
		}
		db.execSQL("DROP TABLE IF EXISTS " + mTABLE_NAME);
		db.execSQL("CREATE TABLE " + mTABLE_NAME + " (" + TestColumns._ID
				+ " INTEGER PRIMARY KEY," + TestColumns.TITLE + " TEXT,"
				+ TestColumns.COLOUMN_1 + " TEXT );");
		ContentValues values = new ContentValues();
		values.put(TestColumns.TITLE, "title");
		values.put(TestColumns.COLOUMN_1, "detail");
		long id = db.insert(mTABLE_NAME, TestColumns.TITLE, values);
		if (Config.DEBUG) {
			Log.d(TAG, "createTable in Database ");
			Log.d(TAG, "id " + id);
		}
		db.close();
	}

	private static final String TAG = AbstractInjectionRunner.class.getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.example.android.notepad.test.AbstractCrawler#getEditTextSet(java.
	 * util.ArrayList, boolean)
	 */
	@Override
	protected ArrayList<String> getEditTextSet(
			ArrayList<EditText> currentViews, boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.example.android.notepad.test.AbstractCrawler#performActionOnWidgets
	 * (android.app.Activity, java.util.ArrayList, java.lang.String,
	 * kit.Scenario.Tree, kit.Scenario.State)
	 */
	@Override
	protected Tree performActionOnWidgets(Activity mainActivity,
			ArrayList<View> viewToHandle, String pairWiseSequence,
			Tree current_Tree, State current_state) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.example.android.notepad.test.AbstractCrawler#handleInErrorViewCase
	 * (boolean, android.app.Activity, kit.Scenario.ScenarioData,
	 * kit.Scenario.Tree)
	 */
	@Override
	protected void handleInErrorViewCase(boolean viewError,
			Activity currentActivity, ScenarioData mScenarioData2,
			Tree current_Tree) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.example.android.notepad.test.AbstractCrawler#handleOtherCase(android
	 * .app.Activity, android.app.Activity, java.util.ArrayList,
	 * kit.Scenario.Transition, kit.Scenario.Tree)
	 */
	@Override
	protected void handleOtherCase(Activity currentActivity,
			Activity mainActivity, ArrayList<View> currentViews, Transition tr,
			Tree current_Tree) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.example.android.notepad.test.AbstractCrawler#handleNewActivity(android
	 * .app.Activity, android.app.Activity, kit.Scenario.Transition,
	 * kit.Scenario.Tree)
	 */
	@Override
	protected void handleNewActivity(Activity currentActivity,
			Activity mainActivity, Transition tr, Tree current_Tree)
			throws InterruptedException {
		// TODO Auto-generated method stub

	}

	private DatabaseHelper mOpenHelper;

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, mDATABASE_NAME, null, mDATABASE_VERSION);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database
		 * .sqlite.SQLiteDatabase)
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {

		}

	}

	/**
	 * apart our table name
	 */
	protected void resetAppDataBase() {
		/**
		 * get the list of table
		 */
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		ArrayList<String> tables = listTables(db);
		for (String table : tables) {
			if (Config.DEBUG) {
				Log.e(TAG, "resetDataBase ");
				Log.d(TAG, "table " + table);
			}
			if (table.equalsIgnoreCase(mTABLE_NAME)
					|| table.equalsIgnoreCase("android_metadata")) {
				continue;
			}
			db.delete(table, null, null);
			if (Config.DEBUG) {
				Log.e(TAG, "deleted  ");
			}
		}
		db.close();
	}

	/**
	 * @throws CloneNotSupportedException
	 * 
	 */
	protected void complete_back_track() throws CloneNotSupportedException {
		try {
			restartActivity();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (mPath == null) {
			if (Config.DEBUG) {
				Log.d(TAG, "complete_back_track ");
				Log.d(TAG, "mPath is Null ");
			}
		}

		_blind_goToLastState(mState_To_Inj.clone());

	}

	/**
	 * @param back
	 *            to state
	 * @throws InterruptedException
	 * @throws CloneNotSupportedException
	 */
	protected void backTrack(State state) throws InterruptedException,
			CloneNotSupportedException {
		/**
		 * resetDatabase
		 */
		resetAppDataBase();
		if (!goBack(state.getShortName(), 0)) {
			complete_back_track();
		}
	}

	protected ArrayList<String> listTables(SQLiteDatabase db) {

		ArrayList<String> tableList = new ArrayList<String>();
		String SQL_GET_ALL_TABLES = "SELECT name FROM "
				+ "sqlite_master WHERE type='table' ORDER BY name";
		Cursor cursor = db.rawQuery(SQL_GET_ALL_TABLES, null);
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			do {
				tableList.add(cursor.getString(0));
			} while (cursor.moveToNext());
		}
		cursor.close();
		return tableList;
	}

	protected void remove_non_handable(ArrayList<View> allCurrentView) {
		Iterator<View> currentViewIterator = allCurrentView.iterator();
		while (currentViewIterator.hasNext()) {
			View view = currentViewIterator.next();
			if (!SgUtils.isHandableView(view)) {
				currentViewIterator.remove();
			}
		}

	}

	/**
	 * @return
	 */
	protected File getTestDataFile() {
		String rvPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ Config.TESTRESULTS
				+ File.separator
				+ Config.TESTDATA_XML;
		return new File(rvPath);
	}

	/**
	 * @param currentEvent
	 * @param seqEditText
	 * @param message
	 * @throws CloneNotSupportedException
	 */
	protected void writeFailReport(Event currentEvent,
			ArrayList<Event> seqEditText, String message)
			throws CloneNotSupportedException {
		String id = SgUtils.get_a_tree_Id(mPath, "");
		Tree finalTree = new Tree(id, id);
		State source = mState_To_Inj.clone();
		Transition tr = new Transition(
				SgUtils.get_a_transition_id(mScenarioData));
		source.setId(tr.getId());
		State dest = getDestState(tr, Scenario.END);
		mScenarioData.addStates(dest);
		tr.setSource(source);
		tr.setDest(dest);
		// ajout des editTexts
		for (Event edit : seqEditText) {
			SgUtils.addTransitionAction(edit, tr); // ******
		}
		// ajout de l'action
		SgUtils.addTransitionAction(currentEvent, tr);
		// ajout de l'erreur
		SgUtils.addError(tr, message);
		finalTree.setTransitions(tr);
		mScenarioData.setTransitions(tr);
		mScenarioData.setTrees(finalTree);
		mScenarioGenerator.generateXml(mScenarioData);
		// FileUtils.write(file, data)
	}

	/**
	 * @param currentEvent
	 * @param seqEditText
	 */
	private final String EXECUTED_EVENT_LIST_PATH = Environment
			.getExternalStorageDirectory()
			+ Config.TESTRESULTS
			+ File.separator + Config.EXECUTED_EVENTS;

	protected void Stimulate(String _id, Event currentEvent,
			ArrayList<Event> seqEditText) {
		for (Event editTex : seqEditText) {
			stimulate(_id, editTex);
		}
		stimulate(_id, currentEvent);
	}

	private void stimulate(String _id, Event event) {
		saveEvent(_id, event);
		try {
			SgUtils.trigger(mSolo, event, mContext);
		} catch (junit.framework.AssertionFailedError clickEventError) {
			return;
		}
	}

	private void saveEvent(String _id, Event event) {
		new WriterEvent(_id, event, new File(EXECUTED_EVENT_LIST_PATH)).start();
	}

	/**
	 * @return
	 */
	protected ArrayList<Object> getViewValues() {
		ArrayList<Object> values = new ArrayList<Object>();
		ArrayList<View> views = mSolo.getCurrentViews();
		remove_non_handable(views);
		for (View v : views) {
			String value = SgUtils.getValue(v, mContext);
			StringTokenizer token = new StringTokenizer(value);
			if (!token.hasMoreTokens())
				continue;
			do {
				values.add(token.nextToken());
			} while (token.hasMoreTokens());

		}
		return values;
	}

	protected void preambuleStep() {
		mScenarioData = mPath;
		mScenarioData.setTaskId(mState_To_Inj.getId());
		// go to target state
		_blind_goToLastState(mState_To_Inj);
		// goToLastState(mPath.getInitialState(), mState);

	}

	protected void readTargetStatePath() {
		mPath = readPath(getScenarioPathFile());

	}

	protected void readTargetState() {
		mState_To_Inj = SgUtils.get_last_state(mPath);

	}

	public static final class TestColumns implements BaseColumns {
		// This class cannot be instantiated
		private TestColumns() {
		}

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.note";

		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
		 * note.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.note";

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = "modified DESC";

		/**
		 * The title of the note
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String TITLE = "title";

		/**
		 * The note itself
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLOUMN_1 = "note";

		/**
		 * The timestamp for when the note was created
		 * <P>
		 * Type: INTEGER (long from System.curentTimeMillis())
		 * </P>
		 */
		public static final String CREATED_DATE = "created";

		/**
		 * The timestamp for when the note was last modified
		 * <P>
		 * Type: INTEGER (long from System.curentTimeMillis())
		 * </P>
		 */
		public static final String MODIFIED_DATE = "modified";
	}

}
