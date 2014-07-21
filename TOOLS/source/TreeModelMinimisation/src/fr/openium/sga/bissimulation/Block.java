package fr.openium.sga.bissimulation;

import grph.set.IntSet;

/**
 * 
 * @author STASSIA
 * 
 *         A Block is a set of states. For example, B3={3,4}.
 * 
 */
public class Block {

	public Block() {

	}

	public Block(int i) {
		mListOfState.add(i);
	}

	public Block(IntSet vertices) {
		setListOfState(vertices.clone());
	}

	public IntSet getListOfState() {
		return mListOfState;
	}

	public void setListOfState(IntSet mListOfState) {
		this.mListOfState = mListOfState;
	}

	public void addState(IntSet state) {
		if (mListOfState == null) {
			setListOfState(state);
		} else
			mListOfState.addAll(state);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mListOfState == null) ? 0 : mListOfState.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Block other = (Block) obj;
		if (mListOfState == null) {
			if (other.mListOfState != null)
				return false;
		} else if (!mListOfState.equals(other.mListOfState))
			return false;
		return true;
	}

	/**
	 * States
	 */
	private IntSet mListOfState;

	public boolean contains(int i) {
		return mListOfState.contains(i);
	}

	@Override
	public String toString() {
		return mListOfState.toString();
	}

	public int size() {
		return mListOfState.size();
	}

	public void addState(int state) {
		mListOfState.add(state);

	}

	public void reset() {
		mListOfState.remove(mListOfState.toIntArray());
	}

}
