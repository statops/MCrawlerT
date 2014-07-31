package fr.openium.sga.displayer;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JFrame;

import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioParser;

import fr.openium.sga.displayer.controller.StoryBoardController;
import fr.openium.sga.displayer.view.StoryBoardView;

public class Displayer extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1053307690497791153L;

	/**
	 * Constructeur de la fen�tre.
	 * 
	 * @throws Exception
	 */
	public Displayer(ScenarioData tree, String appName,
			File robotiumScreenShot, int story_board_type, int pad, int x, int y)
			throws Exception {
		super("maquette crawler");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		/**
		 * ajouter Scenarion data
		 */
		Storyboard storyboard = new Storyboard(tree, appName,
				robotiumScreenShot, story_board_type, pad, x, y);

		Component toDisplay = storyboard.getStoryBoard();
		/*
		 * FileSinkImages pic = new FileSinkImages(OutputType.PNG,
		 * Resolutions.VGA);
		 * 
		 * pic.setLayoutPolicy(LayoutPolicy.COMPUTED_FULLY_AT_NEW_IMAGE);
		 * pic.writeAll(storyboard.getGraph(),
		 * robotiumScreenShot.getAbsolutePath
		 * ()+File.separator+"storyBoard.png");
		 */StoryBoardController c = new StoryBoardController(toDisplay);
		this.addWindowListener(c);
		setMinimumSize(new Dimension(1200, 800));
		setTitle("Story board of " + appName);

		// setJMenuBar(new Menu(c));
		getContentPane().add(new StoryBoardView(c));

		pack();

		setVisible(true);
	}

	/*----------------------------------------------------------*/

	public static void main(String[] args) throws Exception {
		System.setProperty("org.graphstream.ui.renderer",
				"org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		String tree_type = args[4];
		if (tree_type == null) {
			tree_type = "" + GraphGenerator.SIMPLE_TYPE;
		}
		int pad = 30;
		int x = 30;
		int y = 45;
		pad = (args.length>5 &&args[5] != null) ? Integer.parseInt(args[5]) : pad;
		x = (args.length>6 && args[6] != null) ? Integer.parseInt(args[6]) : x;
		y = (args.length>7 && args[7] != null) ? Integer.parseInt(args[7]) : y;

		// Displayer fen = new Displayer(ScenarioParser.parse(new
		// File(args[1])),
		// args[2], new File(args[3]), Integer.parseInt(tree_type), pad, x, y,
		// y);

		Displayer window = new Displayer(
				ScenarioParser.parse(new File(args[1])), args[2], new File(
						args[3]), Integer.parseInt(tree_type), pad, x, y);

		window.setVisible(true);
	}

	public static void displayStoryBoard(ScenarioData graph, File screenShot) {

		if (!screenShot.exists() && !screenShot.isDirectory()) {
			throw new NullPointerException(" This directory does not exist");

		}

		// Create Graph with sate id and file name in screenShot

	}

}
