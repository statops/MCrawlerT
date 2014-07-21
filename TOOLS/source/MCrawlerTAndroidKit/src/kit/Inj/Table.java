/**
 * 
 */
package kit.Inj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import kit.Utils.SgUtils;
import android.provider.BaseColumns;

/**
 * @author Stassia
 * 
 */
public class Table {
	
	private Hashtable<String, Column> Columns = new Hashtable<String, Column>();
	private ArrayList<Integer> row = new ArrayList<Integer>();
	public final String mName;

	/**
	 * 
	 */
	public Table(String name) {
		mName = name;

	}

	public void addValue(String columnName, String value, String line) {
		Column col = geColumns().get(columnName);
		if (col == null) {
			throw new NullPointerException("column name does not exist");
		}
		col.addValues(value, line);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mName == null) ? 0 : mName.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Table other = (Table) obj;
		if (mName == null) {
			if (other.mName != null)
				return false;
		} else if (!mName.equals(other.mName))
			return false;
		if (Columns == null) {
			if (other.Columns != null)
				return false;
		} else {
			if (Columns.size() != other.Columns.size())
				return false;
		}
		ArrayList<Object> content1 = getContent();
		ArrayList<Object> content2 = other.getContent();
		if (!SgUtils.equal(content1, content2))
			return false;

		/**
		 * check subset
		 */

		return true;
	}

	public void createColumn(String columnName, String type) {
		Column col = new Column(columnName, type);
		geColumns().put(columnName, col);
	}

	public void delete(String columnName, String value) {
		Column col = geColumns().get(columnName);
		if (col == null) {
			throw new NullPointerException("column name does not exist");
		}
		col.remove(value);
	}

	public class Column {
		private final String mName;
		private final String mType;
		private HashMap<String, String> mLine_Value = new HashMap<String, String>();

		public Column(String name, String type) {
			mName = name;
			mType = type;

		}

		/**
		 * @param value
		 */
		public void remove(String value) {
			for (String i : mLine_Value.keySet()) {
				if (mLine_Value.get(i).equalsIgnoreCase(value)) {
					mLine_Value.put(i, null);
				}
			}

		}

		public int addValues(String values, String line) {
			mLine_Value.put(line, values);
			return Integer.parseInt(line);
		}

		public String getName() {
			return mName;
		}

		public String getType() {
			return mType;
		}

		/**
		 * @param string
		 * @return
		 */
		public String getValue(String string) {
			for (String name : mLine_Value.keySet()) {
				return (mLine_Value.get(name));
			}
			return null;
		}

		/**
		 * @return
		 */
		public ArrayList<String> getContent() {
			ArrayList<String> content = new ArrayList<String>();
			for (String name : mLine_Value.keySet()) {
				String val = mLine_Value.get(name);
				content.add(val);
			}
			return content;
		}
	}

	/**
	 * @return
	 */
	public String toString() {
		return mName;

	};

	public ArrayList<Object> getContent() {
		ArrayList<Object> valuesArrayList = new ArrayList<Object>();
		for (String name : geColumns().keySet()) {
			Column col = geColumns().get(name);
			valuesArrayList.addAll(col.getContent());
		}
		return valuesArrayList;
	}

	public Hashtable<String, Column> geColumns() {
		return Columns;
	}

	public void seColumns(Hashtable<String, Column> columns) {
		Columns = columns;
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

	/**
	 * @param number
	 * @return
	 */
	public String getColumns(int number) {
		int i = 1;
		String col_name = "";
		for (String name : Columns.keySet()) {
			if (i > number) {
				break;
			}
			col_name = col_name + "," + Columns.get(name).getName();
			i++;
		}
		return col_name.substring(1);
	}

	/**
	 * @param number
	 * @return
	 */
	public String getValues(int number) {
		int i = 1;
		String value = "";
		for (String name : Columns.keySet()) {
			if (i > number) {
				break;
			}
			value = value + "," + "'" + Columns.get(name).getValue("1") + "'";
			i++;
		}
		return value.substring(1);
	}

	public ArrayList<Integer> getRow() {
		return row;
	}

	public void setRow(Integer a_row) {
		this.row.add(a_row);
	}

	/**
	 * @return
	 */
	public Hashtable<String, Column> getColumns() {

		return Columns;
	}
}
