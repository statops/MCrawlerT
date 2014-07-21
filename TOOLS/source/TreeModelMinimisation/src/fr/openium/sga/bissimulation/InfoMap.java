package fr.openium.sga.bissimulation;

/**
 * An Info map is defined for a Block B, state p and action 
 * 
 * 
 * InfoMap B (a, p) =|Ta [ p] ∩ B |
 * 
 * @author STASSIA
 * 
 */
public class InfoMap {
	private final Block block;
	private final String action;
	private final int state;
	private final int value;
	/**
	 * infoB[st][Act]=val
	 * 
	 * @param B
	 * @param Act
	 * @param st
	 * @param val
	 */

	public InfoMap(Block B, String Act, int st, int val) {
		block = B;
		action = Act;
		state = st;
		value = val;
	}

	public Block getBlock() {
		return block;
	}

	public String getAction() {
		return action;
	}

	public int getState() {
		return state;
	}

	public int getValue() {
		return value;
	}

}
