package it.unipi.rcl.project.client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AlertForm extends Form{
	private JPanel panel;
	private JLabel textLabel;
	private JButton okButton;

	public AlertForm(String title, String label, String button){
		super(null);
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setContentPane(panel);
		frame.setVisible(true);
		frame.setSize(400, 200);
		frame.setTitle(resourceBundle.getString(title));
		textLabel.setText(resourceBundle.getString(label));
		okButton.setText(resourceBundle.getString(button));

		okButton.addActionListener(actionEvent -> frame.dispose());
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}
}
