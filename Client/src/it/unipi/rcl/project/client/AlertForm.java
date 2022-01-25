package it.unipi.rcl.project.client;

import javax.swing.*;

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
		Form.centerFrame(frame);
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}

	public static void errorAlert(String message){
		new AlertForm("error", message, "ok");
	}

	public static void errorAlert(){
		errorAlert("error.unknown");
	}

	public static void successAlert(String message){
		new AlertForm("success", message, "ok");
	}

	public static void followAlert(String username) {
		AlertForm a = new AlertForm("notification", "followed.you", "ok");
		a.textLabel.setText(username + " " + resourceBundle.getString("followed.you"));
	}

	public static void unfollowAlert(String username) {
		AlertForm a = new AlertForm("notification", "unfollowed.you", "ok");
		a.textLabel.setText(username + " " + resourceBundle.getString("unfollowed.you"));
	}
}
