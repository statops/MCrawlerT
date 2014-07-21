package fr.openium.sga.bissimulation;

import grph.set.IntSet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;



/**
 * A Partition is a set of Blocks.
 * 
 * P={B1, B3, B4}
 * 
 * * @author STASSIA
 * 
 */
public class Partition {

	private static final int MIN_NAME_IDENTIFIER = 0;
	private static final int MAX_NAME_IDENTIFIER = 2;
	private SgaGraph mGraph;

	public Partition(SgaGraph grph) {
		mGraph = grph;
		Block bk = new Block(grph.getVertices().clone());
		mBlocks.add(bk);
	}

	private HashSet<Block> mBlocks = new HashSet<Block>();

	public void add(Block b) {
		if (b != null && !b.getListOfState().isEmpty())
			mBlocks.add(b);
	}

	public HashSet<Block> getBlocks() {
		return mBlocks;
	}

	public int size() {
		return mBlocks.size();
	}

	public void remove(Block x) {
		mBlocks.remove(x);

	}

	/**
	 * 
	 */
	public void splitBlock() {
		if (mBlocks.isEmpty()) {
			return;
		}
		// pour chaque block si le nom (substring 3 ne se ressemble pas,
		// splitter)
		Iterator<Block> currentBlock = mBlocks.iterator();
		HashSet<Block> finalBlock = new HashSet<Block>();
		do {
			Block current = currentBlock.next();
			HashSet<Block> blockTemp = new HashSet<Block>();
			IntSet states = current.getListOfState();
			for (int i : states.toIntArray()) {
				affect(i, blockTemp, current);
			}
			currentBlock.remove();
			finalBlock.addAll(blockTemp);
		} while (currentBlock.hasNext());
		mBlocks.addAll(finalBlock);

	}

	/**
	 * @param i
	 * @param blockTemp
	 * @param current2
	 */
	private void affect(int i, HashSet<Block> blockTemp, Block current2) {

		if (blockTemp.isEmpty()) {
			Block temp = new Block(current2.getListOfState());
			temp.reset();
			temp.addState(i);
			blockTemp.add(temp);
			return;
		}
		GraphState currentState = mGraph.getState(i);
		Iterator<Block> element = blockTemp.iterator();
		do {
			Block current = element.next();
			for (int j : current.getListOfState().toIntArray()) {
				int comparator = getIntComparator(j);
				String id="";
				try {
					id=currentState.getName().substring(MIN_NAME_IDENTIFIER, comparator);
					System.out.println("id "+id);
				} catch (StringIndexOutOfBoundsException st){
					id =currentState.getName();
					System.err.println("id  "+id);
					st.printStackTrace();
				}
				
				if (mGraph.getState(j).getName().substring(MIN_NAME_IDENTIFIER, comparator)
						.equalsIgnoreCase(id)) {
					current.addState(i);
					element.remove();
					blockTemp.add(current);
					return;
				}
			}
		} while (element.hasNext());
		Block temp = new Block(current2.getListOfState());
		temp.reset();
		temp.addState(i);
		blockTemp.add(temp);
	}

	/**
	 * @param j
	 * @return
	 */
	private int getIntComparator(int j) {
		String name=mGraph.getState(j).getName();
		/**
		 * use id Delimiters
		 */
		StringTokenizer del=new StringTokenizer(name,"_");
		if (!del.hasMoreElements()){
			return 0;
		}
		if (del.countTokens()>1){
			String comprator=del.nextToken();
			return comprator.length() - 1;
		}
		
		/*
		 * use max
		 */
		if (name.length() < MAX_NAME_IDENTIFIER) {
			return name.length() - 1;
		}
		return MAX_NAME_IDENTIFIER;
	}
}
