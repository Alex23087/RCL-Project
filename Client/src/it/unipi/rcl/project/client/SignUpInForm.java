package it.unipi.rcl.project.client;

import javax.swing.*;
import java.awt.event.ActionListener;

public class SignUpInForm extends Form{
	private JButton registerButton;
	private JButton loginButton;
	private JPanel panel;
	private JTextField usernameTextField;
	private JPasswordField passwordTextField;
	private JButton passHideButton;

	public SignUpInForm(AppEventDelegate aed){
		super(aed);

		ActionListener loginListener = actionEvent -> {
			//TODO: add client side checks
			ServerProxy.instance.login(usernameTextField.getText().strip(), passwordTextField.getText().strip(), () -> appEventDelegate.onLoginComplete(), errorMessage -> {
				switch(errorMessage){
					case UserAlreadyLoggedIn:
						new AlertForm("error", "user.already.logged","ok");
						break;
					case InvalidUsername:
						new AlertForm("error", "user.invalid","ok");
						break;
					case InvalidPassword:
						new AlertForm("error", "pass.invalid","ok");
						break;
				}
			});
		};

		loginButton.addActionListener(loginListener);

		registerButton.addActionListener(actionEvent -> {
			//TODO: add client side checks
			new RegisterTagsForm(tags-> ServerProxy.instance.register(usernameTextField.getText().strip(), passwordTextField.getText().strip(), tags, () -> loginListener.actionPerformed(actionEvent), errorMessage -> {
				switch(errorMessage){
					case UserAlreadyExists:
						new AlertForm("error", "user.exists", "ok");
						break;
					case InvalidTags:
						new AlertForm("error", "tags.invalid", "ok");
						break;
					case InvalidPassword:
						new AlertForm("error", "pass.invalid", "ok");
						break;
					case InvalidUsername:
						new AlertForm("error", "user.invalid", "ok");
				}
			}));
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
