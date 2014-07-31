package fr.openium.sga.displayer.view;

import java.awt.BorderLayout;
import java.util.Observable;
import java.util.Observer;


import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;


import fr.openium.sga.displayer.controller.StoryBoardController;

public class StoryBoardView extends JPanel implements Observer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6010178069429650820L;

	private JPanel storyboard;


	public StoryBoardView(StoryBoardController c) {
		initializeFrame(c);
	}

	/**
	 * Initialise la fen�tre.
	 */
	private void initializeFrame(StoryBoardController c) {
		setLayout(new BorderLayout());
		add(createStoryBoardPanel(c), BorderLayout.CENTER);
	}

	private JPanel createStoryBoardPanel(StoryBoardController c) {
		storyboard = new JPanel(new BorderLayout());
		storyboard.add(c.getStoryboard(), BorderLayout.CENTER);
		storyboard.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

		return storyboard;
	}

	@Override
	public void update(Observable source, Object info) {

	}

}
