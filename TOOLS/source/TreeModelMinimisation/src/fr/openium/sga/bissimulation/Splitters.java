package fr.openium.sga.bissimulation;

import java.util.HashSet;
import java.util.Iterator;

/**
 * A set of set W contains blocks that are going to be used to split the
 * Partition
 * 
 * @author STASSIA
 * 
 */
public class Splitters {

	private HashSet<HashSet<Block>> mSetBlocks = new HashSet<HashSet<Block>>();

	public HashSet<HashSet<Block>> getSetBlocks() {
		return mSetBlocks;
	}

	public void add(HashSet<Block> setBlocks) {
		/**
		 * prendre des clones
		 */
		HashSet<Block> toAdd = new HashSet<Block>();
		for (Block b : setBlocks) {
			toAdd.add(new Block(b.getListOfState().clone()));
		}
		mSetBlocks.add(toAdd);
	}

	public HashSet<Block> get_a_Set() {
		Iterator<HashSet<Block>> iter = mSetBlocks.iterator();
		if (!iter.hasNext())
			return null;
		return iter.next();
	}

	
	public boolean isSimple() {
		if (!mSetBlocks.isEmpty() && mSetBlocks.iterator().next().size() == 1) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		String toPrint = "";
		int i = 0;
		if (mSetBlocks == null || mSetBlocks.isEmpty()) {
			return "Empty or null";
		} else {
			Iterator<HashSet<Block>> iter = mSetBlocks.iterator();
			do {
				HashSet<Block> a_block = iter.next();
				Iterator<Block> toPrintBlock = a_block.iterator();
				if (!toPrintBlock.hasNext()) {
					continue;
				}
				toPrint = toPrint + "\n Splitters " + i++ + "\n";
				do {
					toPrint = toPrint + "\n" + toPrintBlock.next().toString();
				} while (toPrintBlock.hasNext());
			} while (iter.hasNext());

		}
		return toPrint;
	}

	public void removeBlockSet(HashSet<Block> simple_plitterSet) {
		mSetBlocks.remove(simple_plitterSet);
	}

	public boolean isEmpty() {
		return mSetBlocks.isEmpty();
	}
}
