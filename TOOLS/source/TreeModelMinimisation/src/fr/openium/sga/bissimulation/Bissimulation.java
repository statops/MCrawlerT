package fr.openium.sga.bissimulation;

import grph.algo.degree.AbstractDegreeAlgorithm.DIRECTION;
import grph.set.IntSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * 
 * @author STASSIA
 * 
 */
public class Bissimulation {

	private Partition mP;
	private Splitters mW;
	private SgaGraph mGraph;
	private HashSet<InfoMap> mInfoMaps = new HashSet<InfoMap>();
	private HashSet<String> mAction;

	/**
	 * 
	 * @param lts
	 */
	public Bissimulation(SgaGraph grph) {
		mGraph = grph;

	}

	/**
	 * InfoMap B (a, p) =| Ta [ p] ∩ B |
	 * 
	 * @param B
	 *            : teh block
	 * @param a
	 *            : the action
	 * @param p
	 *            : the action
	 * @return
	 */
	public int info(Block B, String a, int source) {
		return (transitionGraph(B, a, source)).size();
	}

	public SgaGraph compute(SgaGraph grph) {
		init();
		do {
			HashSet<Block> block_set_to_split = (mW.get_a_Set());
			if (block_set_to_split == null) {
				continue;
			}
			if (block_set_to_split.size() == 1) {
				computeSimple(block_set_to_split);
			} else {
				computeComposed(block_set_to_split);
			}

		} while (!mW.isEmpty());
		/**
		 * create a graph from the final partition
		 */

		HashSet<GraphTranstion> trs = new HashSet<GraphTranstion>();
		mP.splitBlock();
		for (Block B : mP.getBlocks()) {
			trs.addAll(getTransition(B));
		}
		SgaGraph gr = new SgaGraph();
		gr.createGraph(trs);
		System.out.println(gr.toDot());
		return gr;
	}

	private HashSet<GraphTranstion> getTransition(Block b) {
		HashSet<GraphTranstion> trs = new HashSet<GraphTranstion>();
		for (Block other : mP.getBlocks()) {
			/*
			 * if (equalBlockSet(other, b)) { continue; }
			 */
			boolean done = false;
			for (int state : b.getListOfState().toIntArray()) {
				for (String action : mAction) {

					if (info(other, action, state) != 0) {
						ArrayList<GraphTranstion> tr = getTransition(b, other, action, state);
						trs.addAll(tr);
						done = true;
						/*
						 * on ne peut pas avoir deux actions vers une meme dest
						 */
						// break;
					}
				}
				if (done) {
					/**
					 * meme action de sortie pour tous les blocks
					 */
					// break;
				}
			}

		}

		return trs;
	}

	private ArrayList<GraphTranstion> getTransition(Block sourceBlock, Block destBlock, String action,
			int state) {
		// int i = info(destBlock, action, state);

		/**
		 * get the actionMap from state to destBlock
		 */
		HashSet<GraphAction> act = new HashSet<GraphAction>();
		for (int edges : getEdgeOutSetFrom(destBlock, state).toIntArray()) {
			GraphTranstion gt = mGraph.getEdgeMap().get(edges);
			if (gt.getAction().getValue().equalsIgnoreCase(action)) {
				act.add(gt.getAction());
			}
		}
		/**
		 * ====================================================================
		 * 
		 */
		String blockBStateName = getStatetNameFromBlock(sourceBlock);
		String blockOtherStateName = getStatetNameFromBlock(destBlock);
		ArrayList<GraphTranstion> listTr = new ArrayList<GraphTranstion>();
		for (GraphAction actionToInsert : act) {
			listTr.add(new GraphTranstion(new GraphState(blockBStateName),
					new GraphState(blockOtherStateName), actionToInsert));
		}
		return listTr;
	}

	private IntSet getEdgeOutSetFrom(Block destBlock, int source) {
		IntSet edges = mGraph.getOutEdges(source).clone();
		IntSet dest = mGraph.getNeighbours(source, DIRECTION.out).clone();
		
		/**
		 * si set n'appartient pas à block, à supprimer
		 */
		for (int ii : dest.toIntArray()) {
			if (!destBlock.contains(ii)) {
				dest.remove(ii);
			}
		}
		if (dest.isEmpty()){
			return dest;
			
		}
		IntSet value = null;
		for (int desti : dest.toIntArray()) {
			if (value == null)
				value = mGraph.getEdgesConnecting(source, desti);
			else
				value.addAll(mGraph.getEdgesConnecting(source, desti));

		}
		if (value==null){
			System.out.println("debug");
		}
		for (int jj : value.toIntArray()) {
			if (!edges.contains(jj)) {
				value.remove(jj);
			}
		}
		return value;
	}

	private String getStatetNameFromBlock(Block b) {
		String blockBStateName = "";
		for (int in_b : b.getListOfState().toIntArray()) {
			blockBStateName = blockBStateName + "," + mGraph.getVertexLabelProperty().getValue(in_b);
		}
		blockBStateName = blockBStateName.substring(1);
		return blockBStateName;
	}

	public SgaGraph compute() {
		return compute(mGraph);
	}

	private void computeComposed(HashSet<Block> block_set_to_split) {

		// B =Bi U Bii
		// Bi < Bii

		Block[] blockArray = getSetBlockPartition(block_set_to_split);
		if (blockArray == null) {
			System.out.println("block to remove : "+block_set_to_split);
			mW.removeBlockSet(block_set_to_split);
			return;
		}
		Block B = blockArray[0];
		Block Bi = blockArray[1];
		Block Bii = blockArray[2];
		mW.removeBlockSet(block_set_to_split);
		if (mAction.isEmpty()) {
			throw new NullPointerException("action set is empty");
		}
		Iterator<String> action = mAction.iterator();
		do {
			String alph = action.next();
			/**
			 * Construct I set of blocks to be splitted
			 */
			Block invTransition = transitionInvers(B, alph);
			System.out.println("Tinverse[" + B.toString() + "] :" + alph + " :"
					+ invTransition.getListOfState().toString());
			HashSet<Block> blockTobeSplitted = constructI(invTransition, alph);
			Iterator<Block> I = blockTobeSplitted.iterator();
			if (!I.hasNext()) {
				return;
			}
			/**
			 * compute x1, x2 x3
			 */
			do {
				Block X = new Block(I.next().getListOfState().clone());
				System.out.println("I :" + X.getListOfState().toString());
				Block X1 = new Block(X.getListOfState().clone());
				X1.reset();
				Block X2 = new Block(X.getListOfState().clone());
				X2.reset();
				Block X3 = new Block(X.getListOfState().clone());
				X3.reset();
				for (int state : X.getListOfState().toIntArray()) {
					int infBi = info(Bi, alph, state);
					int infB = info(B, alph, state);
					int infBii = 0;
					if (infBi == infB) {
						X1.addState(state);
						infBii = 0;
					}
					if (infBi == 0) {
						X2.addState(state);
						infBii = 0;
						infBi = info(Bi, alph, state);
					}
					if (infBi > 0 && infBi < infB) {
						X3.addState(state);
						infBii = infB - infBi;
					}
					System.out.println("X1 :" + X1.getListOfState().toString());
					System.out.println("X2 :" + X2.getListOfState().toString());
					System.out.println("X3 :" + X3.getListOfState().toString());
					mInfoMaps.add(new InfoMap(B, alph, state, infB));
					mInfoMaps.add(new InfoMap(Bi, alph, state, infBi));
					mInfoMaps.add(new InfoMap(Bii, alph, state, infBii));
				}

				/**
				 * check and update
				 */
				check_and_update(X, X1, X2, X3);
			} while (I.hasNext());

		} while (action.hasNext());

	}

	private void check_and_update(Block x, Block x1, Block x2, Block x3) {

		if (equalBlockSet(x, x1) || equalBlockSet(x, x2) || equalBlockSet(x, x3)) {
			// nothing

		} else {
			mP.remove(x);
			mP.add(x1);
			mP.add(x2);
			mP.add(x3);
			HashSet<Block> new_splitter = new HashSet<Block>();
			new_splitter.add(x);
			new_splitter.add(x1);
			IntSet x2x3Set = x2.getListOfState().clone();
			x2x3Set.addAll(x3.getListOfState().clone());
			Block x2x3 = new Block((x2x3Set));
			new_splitter.add(x2x3);
			mW.add(new_splitter);
			new_splitter.clear();
			new_splitter.add(x2x3);
			new_splitter.add(x2);
			new_splitter.add(x3);
			mW.add(new_splitter);
		}

		System.out.println("Partion: ");
		for (Block btoPrint : mP.getBlocks()) {
			System.out.println(btoPrint.toString());
		}
		System.out.print("Splitters: ");
		System.out.println(mW.toString());

	}

	private boolean equalBlockSet(Block x, Block x3) {
		HashSet<Integer> x1 = new HashSet<Integer>(x.getListOfState().toIntegerArrayList());
		HashSet<Integer> x2 = new HashSet<Integer>(x3.getListOfState().toIntegerArrayList());
		x1.removeAll(x2);
		return (x1.size() > 0) ? false : true;
	}

	private Block[] getSetBlockPartition(HashSet<Block> block_set_to_split) {
		HashSet<Block> block_set_to_splittemp = extracted_block(block_set_to_split);
		Block[] result = new Block[3];
		if (block_set_to_splittemp.size() != 3) {
			return null;
		}
		Block temp = block_set_to_splittemp.iterator().next();
		IntSet resultarray1 = temp.getListOfState().clone();
		block_set_to_splittemp.remove(temp);
		temp = block_set_to_splittemp.iterator().next();
		IntSet resultarray2 = temp.getListOfState().clone();
		block_set_to_splittemp.remove(temp);
		temp = block_set_to_splittemp.iterator().next();
		IntSet resultarray3 = temp.getListOfState().clone();
		HashSet<IntSet> set = new HashSet<IntSet>();
		set.add(resultarray1);
		set.add(resultarray2);
		set.add(resultarray3);
		/**
		 * get the principal Block
		 */
		result[0] = (resultarray1.size() > resultarray2.size() && resultarray1.size() > resultarray3.size()) ? new Block(
				resultarray1) : new Block((resultarray2.size() > resultarray3.size()) ? resultarray2
				: resultarray3);
		set.remove(result[0].getListOfState());
		/**
		 * get the partitions 2
		 */
		result[1] = (resultarray1.size() < resultarray2.size() && resultarray1.size() < resultarray3.size()) ? new Block(
				resultarray1) : new Block((resultarray2.size() < resultarray3.size()) ? resultarray2
				: resultarray3);
				
		set.remove(result[1].getListOfState());

		result[2] = new Block(set.iterator().next());
		return result;
	}

	@SuppressWarnings("unchecked")
	private HashSet<Block> extracted_block(HashSet<Block> block_set_to_split) {
		return (HashSet<Block>) block_set_to_split.clone();
	}
	/**
	 * Simple Splitter composed of B
	 * @param B=block_set_to_split
	 */

	private void computeSimple(HashSet<Block> block_set_to_split) {
		HashSet<Block> B = block_set_to_split;
		Block simple_plitter = B.iterator().next();
		/**
		 * remove the splitter
		 */
		mW.removeBlockSet(B);
		Iterator<String> action = mAction.iterator();
		if (!action.hasNext()) {
			throw new NullPointerException("action set is empty");
		}
		do {
			String alph = action.next();
			System.out.println("current action alpha : "+alph);
			/**
			 * Construct set of blocks to be splitted
			 */
			Block invTransition = transitionInvers(simple_plitter, alph);
			System.out.println("Tinverse  [" + simple_plitter.toString() + "] :" + alph + " :"
					+ invTransition.getListOfState().toString());
			System.out.println("Build block to be splitted");
			HashSet<Block> blockTobeSplitted = constructI(invTransition, alph);
			Iterator<Block> I = blockTobeSplitted.iterator();
			if (!I.hasNext()) {
				System.out.println("There is no block to be splitted  ");
				return;
			}
			do {
				Block X = new Block(I.next().getListOfState().clone());
				System.out.println("I :" + X.getListOfState().toString());
				Block X1 = interSect(X, invTransition);
				System.out.println("X1 :" + X1.getListOfState().toString());
				Block X2 = not_interSect(X, X1);
				System.out.println("X2 :" + X2.getListOfState().toString());
				/**
				 * compute info map
				 */
				for (int st : simple_plitter.getListOfState().toIntArray()) {
					mInfoMaps.add(new InfoMap(simple_plitter, alph, st, info(simple_plitter, alph, st)));
				}
				/**
				 * check and update
				 */
				check_and_update(X, X1, X2);
			} while (I.hasNext());

		} while (action.hasNext());
		
	}

	private void check_and_update(Block x, Block x1, Block x2) {
		if (emptyExist(x1, x2)) {
			System.out.println(x1.toString() +"and "+"is Empty ");
		} else if (notEmpty(x1, x2)) {
			mP.remove(x);
			mP.add(x1);
			mP.add(x2);
			HashSet<Block> new_splitter = new HashSet<Block>();
			new_splitter.add(x);
			new_splitter.add(x1);
			new_splitter.add(x2);
			mW.add(new_splitter);
		}

		System.out.println("Partion: ");
		for (Block btoPrint : mP.getBlocks()) {
			System.out.println(btoPrint.toString());
		}
		System.out.print("Splitters: ");
		System.out.println(mW.toString());

	}
/**
 * check if x1>0 &&x2>0
 * @param x1
 * @param x2
 * @return
 */
	private boolean notEmpty(Block x1, Block x2) {
		if (x1.getListOfState().size() > 0 && x2.getListOfState().size() > 0) {
			return true;
		}
		return false;
	}
	/**
	 * Check if x1 or x2 is empty
	 * @param x1
	 * @param x2
	 * @return
	 */

	private boolean emptyExist(Block x1, Block x2) {
		if (x1.getListOfState().isEmpty() || x2.getListOfState().isEmpty()) {
			return true;
		}
		return false;
	}

	public Block not_interSect(Block x, Block x1) {
		IntSet x1x1 = x1.getListOfState().clone();
		IntSet xx = x.getListOfState().clone();
		for (int i : x1x1.toIntArray()) {
			if (xx.contains(i)) {
				xx.remove(i);
			}
		}
		return new Block(xx);
	}

	public Block interSect(Block x, Block y) {
		System.out.print(x.toString() + " INTERSECTION " + y.toString() + " =  ");
		IntSet xx = x.getListOfState().clone();
		int[] xxValueArray = x.getListOfState().toIntArray();
		int[] yyvalueArray = y.getListOfState().toIntArray();
		xx.remove(xx.toIntArray());
		xx.addAll(intersect(xxValueArray, yyvalueArray));
		System.out.println(xx.toString());
		return new Block(xx);
	}

	public int[] intersect(int[] xxValue, int[] yyvalue) {
		HashSet<Integer> result = new HashSet<Integer>();
		HashSet<Integer> yset = new HashSet<Integer>();
		for (int j : yyvalue) {
			yset.add(j);
		}
		for (int i : xxValue) {
			if (yset.contains(i)) {
				result.add(i);
			}

		}
		/**
		 * convert to Array
		 */
		int[] resultarray = new int[result.size()];
		int i = 0;
		for (Integer val : result)
			resultarray[i++] = val;
		return resultarray;
	}
	/**
	 * T-1[action]B=?
	 * @param B
	 * @param action
	 * @return
	 */

	public Block transitionInvers(Block B, String action) {
		Block outSet = new Block();
		for (int i : B.getListOfState().toIntArray()) {
			IntSet value = transitionInverse(new Block(mGraph.getVertices().clone()), action, i);
			System.out.println("T[" + action + "][" + mGraph.getVertexLabelProperty().getValue(i) + "]= "
					+ value.toString());
			outSet.addState(value);
		}
		System.out.println("T[" + action + "][" + B.toString() + "]= "
				+ outSet.toString());
		return outSet;
	}

	private HashSet<Block> constructI(Block invTransition, String alph) {
		HashSet<Block> I = new HashSet<Block>();

		for (Block b : mP.getBlocks()) {
			if (!interSect(b, invTransition).getListOfState().isEmpty()) {
				I.add(new Block(b.getListOfState().clone()));
			}
		}
		return I;
	}

	public void init() {
		// Partition(P)={B0}; Splitters(W)={B0};B0={vertices}:Actions=les labels
		// des edgs
		mP = new Partition(mGraph);
		mW = new Splitters();
		mW.add(mP.getBlocks());
		/**
		 * set Action
		 */
		mAction = mGraph.getActions();
	}

	public HashSet<Block> choose(Splitters W) {
		/**
		 * may be null in the case of W is empty
		 */
		return W.get_a_Set();
	}

	public Splitters remove(Splitters W, HashSet<Block> bToRemove) {
		W.getSetBlocks().remove(bToRemove);
		return W;
	}
/**
 * 
 
	public boolean check() {
		return false;
		// TODO
	}

	public void update() {
		// TODO
	}

	public void replace() {
		// TODO
	}
*/
	public IntSet transitionGraph(Block block, String action, int source) {
		/**
		 * ne prendre que les outputstates
		 */
		IntSet set = mGraph.getNeighbours(source, DIRECTION.out).clone();
		IntSet edges = mGraph.getOutEdges(source).clone();
		return transitionSet(set, block, source, action, edges);
	}

	public int[] getEdges(int source, int dest) {
		return mGraph.getEdgesConnecting(source, dest).toIntArray();

	}
/**
 * 
 * @param set
 * @param block
 * @param source
 * @param action
 * @param edges
 * @return
 */
	private IntSet transitionSet(IntSet set, Block block, int source, String action, IntSet edges) {
		/**
		 * si set n'appartient pas au block, à supprimer
		 */
		for (int ii : set.toIntArray()) {
			if (!block.contains(ii)) {
				set.remove(ii);
			}
		}
		IntSet blocksetIntSet=set.clone();
		blocksetIntSet.clear();
		/**
		 * supprimer si l'action diff
		 */

		for (int edge : edges.toIntArray()) {
			IntSet setOut = mGraph.getVerticesIncidentTo(edge).clone();
			for (int ver : setOut.toIntArray()) {
				if (ver != source) {
					if (!getAction(edge).equalsIgnoreCase(action)) {
						set.remove(ver);
					}else {
						blocksetIntSet.add(ver);
					}
				}

			}

		}

		return blocksetIntSet;
	}

	private String getAction(int edge) {
		String action=mGraph.getEdgeMap().get(edge).getAction().getValue();
		return action;

	}
/**
 * T-1[action]Bo=dest
 * Bo?
 * @param Bo
 * @param action
 * @param dest
 * @return
 */
	public IntSet transitionInverse(Block Bo, String action, int dest) {
		/**
		 * ne prendre que les input
		 */
		IntSet set = mGraph.getNeighbours(dest, DIRECTION.in);
		IntSet edges = mGraph.getInEdges(dest).clone();
		return transitionSet(set, Bo, dest, action, edges);
	}

	public Splitters getSplitters() {
		return mW;
	}

	public Partition getPartition() {
		return mP;
	}
}
