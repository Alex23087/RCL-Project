package it.unipi.rcl.project.client;

import javax.swing.*;

public class FeedForm {
	private JPanel panel;
	private JLabel usernameLabel;
	private JButton button1;
	private JButton button2;
	private JButton button3;
	private JButton button4;
	private JButton button5;
	private JButton button6;

	public FeedForm(){
		usernameLabel.setText(ServerProxy.instance.user);
	}

	public JPanel getPanel(){
		return panel;
	}
}
