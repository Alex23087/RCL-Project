package it.unipi.rcl.project.client;

import javax.swing.*;

public class BlogForm extends Form {
	private JButton feedButton;
	private JButton balanceButton;
	private JPanel panel;

	BlogForm(AppEventDelegate aed) {
		super(aed);
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}
}
