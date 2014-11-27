package com.j256.ormlite.dao;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;

import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.mapped.MappedPreparedStmt;

/**
 * Base collection that is set on a field that as been marked with the {@link ForeignCollectionField} annotation when an
 * object is refreshed or queried (i.e. not created).
 * 
 * <p>
 * <b>WARNING:</b> Most likely for(;;) loops should not be used here since we need to be careful about closing the
 * iterator.
 * </p>
 * 
 * @author graywatson
 */
public abstract class BaseForeignCollection<T, ID> implements ForeignCollection<T>, Serializable {

	private static final long serialVersionUID = -5158840898186237589L;

	protected transient final Dao<T, ID> dao;
	private transient final String columnName;
	private transient final Object parentId;
	private transient PreparedQuery<T> preparedQuery;
	private transient final String orderColumn;
	private transient final Object parent;

	protected BaseForeignCollection(Dao<T, ID> dao, Object parent, Object parentId, String columnName,
			String orderColumn) {
		this.dao = dao;
		this.columnName = columnName;
		this.parentId = parentId;
		this.orderColumn = orderColumn;
		this.parent = parent;
	}

	/**
	 * Add an element to the collection. This will also add the item to the associated database table.
	 * 
	 * @return Returns true if the item did not already exist in the collection otherwise false.
	 */
	public boolean add(T data) {
		if (dao == null) {
			return false;
		}
		try {
			dao.create(data);
			return true;
		} catch (SQLException e) {
			throw new IllegalStateException("Could not create data element in dao", e);
		}
	}

	/**
	 * Add the collection of elements to this collection. This will also them to the associated database table.
	 * 
	 * @return Returns true if the item did not already exist in the collection otherwise false.
	 */
	public boolean addAll(Collection<? extends T> collection) {
		if (dao == null) {
			return false;
		}
		boolean changed = false;
		for (T data : collection) {
			try {
				dao.create(data);
				changed = true;
			} catch (SQLException e) {
				throw new IllegalStateException("Could not create data elements in dao", e);
			}
		}
		return changed;
	}

	/**
	 * Remove the item from the collection and the associated database table.
	 * 
	 * NOTE: we can't just do a dao.delete(data) because it has to be in the collection.
	 * 
	 * @return True if the item was found in the collection otherwise false.
	 */
	public abstract boolean remove(Object data);

	/**
	 * Remove the items in the collection argument from the foreign collection and the associated database table.
	 * 
	 * NOTE: we can't just do a for (...) dao.delete(item) because the items have to be in the collection.
	 * 
	 * @return True if the item was found in the collection otherwise false.
	 */
	public abstract boolean removeAll(Collection<?> collection);

	/**
	 * Uses the iterator to run through the dao and retain only the items that are in the passed in collection. This
	 * will remove the items from the associated database table as well.
	 * 
	 * @return Returns true of the collection was changed at all otherwise false.
	 */
	public boolean retainAll(Collection<?> collection) {
		if (dao == null) {
			return false;
		}
		boolean changed = false;
		CloseableIterator<T> iterator = closeableIterator();
		try {
			while (iterator.hasNext()) {
				T data = iterator.next();
				if (!collection.contains(data)) {
					iterator.remove();
					changed = true;
				}
			}
			return changed;
		} finally {
			try {
				iterator.close();
			} catch (SQLException e) {
				// ignored
			}
		}
	}

	/**
	 * Clears the collection and uses the iterator to run through the dao and delete all of the items in the collection
	 * from the associated database table. This is different from removing all of the elements in the table since this
	 * iterator is across just one item's foreign objects.
	 */
	public void clear() {
		if (dao == null) {
			return;
		}
		CloseableIterator<T> iterator = closeableIterator();
		try {
			while (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
			}
		} finally {
			try {
				iterator.close();
			} catch (SQLException e) {
				// ignored
			}
		}
	}

	protected PreparedQuery<T> getPreparedQuery() throws SQLException {
		if (dao == null) {
			return null;
		}
		if (preparedQuery == null) {
			SelectArg fieldArg = new SelectArg();
			fieldArg.setValue(parentId);
			QueryBuilder<T, ID> qb = dao.queryBuilder();
			if (orderColumn != null) {
				qb.orderBy(orderColumn, true);
			}
			preparedQuery = qb.where().eq(columnName, fieldArg).prepare();
			if (preparedQuery instanceof MappedPreparedStmt) {
				@SuppressWarnings("unchecked")
				MappedPreparedStmt<T, Object> mappedStmt = ((MappedPreparedStmt<T, Object>) preparedQuery);
				mappedStmt.setParentInformation(parent, parentId);
			}
		}
		return preparedQuery;
	}
}
