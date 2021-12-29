package it.unipi.rcl.project.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ResourceBundle;

public class SignUpInForm extends JFrame{
	private JButton registerButton;
	private JButton loginButton;
	private JPanel panel;
	private JTextField usernameTextField;
	private JPasswordField passwordTextField;
	private JButton passHideButton;

	private AppEventDelegate aed;

	public SignUpInForm(AppEventDelegate aed){
		ResourceBundle rb = ResourceBundle.getBundle("it/unipi/rcl/project/client/WinsomeStrings");

		this.aed = aed;

		loginButton.addActionListener(actionEvent -> {
			if(ServerProxy.instance.login(usernameTextField.getText(), passwordTextField.getText())){
				aed.onLoginComplete();
			}
		});

		registerButton.addActionListener(actionEvent -> {
			ServerProxy.instance.register(usernameTextField.getText(), passwordTextField.getText(), new String[]{""});
		});

		String passwordHint = "Password ";
		setHint(usernameTextField, "Username ");
		setHint(passwordTextField, passwordHint);

		passHideButton.addActionListener(actionEvent -> {
			if(passwordTextField.getText().equals(passwordHint)){
				return;
			}
			if(passwordTextField.getEchoChar() == (char) 0){
				passwordTextField.setEchoChar('*');
				passHideButton.setText(rb.getString("pass.show"));
			}else{
				passwordTextField.setEchoChar((char) 0);
				passHideButton.setText(rb.getString("pass.hide"));
			}
		});
	}

	public JPanel getPanel(){
		return panel;
	}

	private static void setHint(JTextField textField, String hint){
		textField.setText(hint);
		if(textField instanceof JPasswordField){
			((JPasswordField) textField).setEchoChar((char) 0);
		}
		textField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if(textField.getText().equals(hint)){
					textField.setText("");
					if(textField instanceof JPasswordField){
						((JPasswordField) textField).setEchoChar('*');
					}
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if(textField.getText().equals("")){
					textField.setText(hint);
					if(textField instanceof JPasswordField){
						((JPasswordField) textField).setEchoChar((char) 0);
					}
				}
			}
		});
	}
}
