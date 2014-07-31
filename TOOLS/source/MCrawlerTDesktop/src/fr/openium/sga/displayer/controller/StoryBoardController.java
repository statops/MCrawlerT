package fr.openium.sga.displayer.controller;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;

import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class StoryBoardController extends MouseAdapter implements ActionListener, ListSelectionListener , MouseListener, WindowListener{

	private Component storyBoard;
	
	public StoryBoardController(Component board)
	
	{
		storyBoard=board;
		
	}
	
	
	
	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		
	}

	@Override
	public void actionPerformed(ActionEvent clic) {
		
	}

	
	
	
	
	@Override
	public void mouseClicked(java.awt.event.MouseEvent e) {}

	@Override
	public void mouseEntered(java.awt.event.MouseEvent e) {}

	@Override
	public void mouseExited(java.awt.event.MouseEvent e) {}

	@Override
	public void mousePressed(java.awt.event.MouseEvent e) {}

	@Override
	public void mouseReleased(java.awt.event.MouseEvent e) {
		
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
	
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}



	public Component getStoryboard() {
		
		return storyBoard;
	}
}
