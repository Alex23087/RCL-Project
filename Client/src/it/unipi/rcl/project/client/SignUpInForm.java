package it.unipi.rcl.project.client;

import javax.swing.*;

public class SignUpInForm extends Form{
	private JButton registerButton;
	private JButton loginButton;
	private JPanel panel;
	private JTextField usernameTextField;
	private JPasswordField passwordTextField;
	private JButton passHideButton;

	public SignUpInForm(AppEventDelegate aed){
		super(aed);

		loginButton.addActionListener(actionEvent -> {
			if(ServerProxy.instance.login(usernameTextField.getText(), passwordTextField.getText())){
				appEventDelegate.onLoginComplete();
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
				passHideButton.setText(resourceBundle.getString("pass.show"));
			}else{
				passwordTextField.setEchoChar((char) 0);
				passHideButton.setText(resourceBundle.getString("pass.hide"));
			}
		});
	}

	@Override
	public JPanel getPanel(){
		return panel;
	}
}
