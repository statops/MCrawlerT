package fr.openium.sga.displayer.view;



import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import fr.openium.sga.displayer.controller.StoryBoardController;


public class StoryBoardPanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4666044575004047142L;
	
	
	

	
	public StoryBoardPanel(StoryBoardController c)
	{
		setLayout(new BorderLayout());
		
		
		setAlignmentY(LEFT_ALIGNMENT);
		setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(20, 20, 20, 10), new EtchedBorder()));
	}

	
	
	public void setRunButtonEnabled(boolean enabled)
	{
		
	}

}
