package fr.sga.sts;

import fr.sga.sts.displayer.ExtrapolatedTree;
import fr.sga.sts.displayer.MinimisedSTS;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			help();
			return;
		}
		if (args[0].equals("display")) {
			/*
			 * sts tree minimised
			 */
			if (args[1].equalsIgnoreCase("STS")) {
				MinimisedSTS stsTree = new MinimisedSTS(args[2]);
				stsTree.displayTree();
				return;
			}

			if (args[1].equalsIgnoreCase("MinSTS")) {
				MinimisedSTS stsTree = new MinimisedSTS(args[2]);
				stsTree.displayMinTree();
				return;
			}

			if (args[1].equalsIgnoreCase("ExSTS")) {
				ExtrapolatedTree stsTree = new ExtrapolatedTree(args[2]);
				stsTree.displayTree();
				return;
			}
			if (args[1].equalsIgnoreCase("MinExSTS")) {
				ExtrapolatedTree stsTree = new ExtrapolatedTree(args[2]);
				stsTree.displayMinTree();
				return;
			}

			MinimisedSTS stsTree = new MinimisedSTS(args[1]);
			stsTree.displayAll();
			ExtrapolatedTree estsTree = new ExtrapolatedTree(args[1]);
			estsTree.displayAll();
			return;
		}
	}

	private static void help() {
		System.err.println("No arguments");
		
	}

}
