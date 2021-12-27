package it.unipi.rcl.project.client;

import javax.swing.*;
import java.awt.*;

public class SignUpInForm extends JFrame{
	private JButton registerButton;
	private JButton loginButton;
	private JPanel panel;
	private JTextField usernameTextField;
	private JPasswordField passwordTextField;

	public SignUpInForm(){
		setLayout(new GridLayout());
		add(panel);
		validate();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		setSize(500, 500);
		setTitle("Winsome");

		loginButton.addActionListener(actionEvent -> {
			System.out.println(ServerProxy.instance.login(usernameTextField.getText(), passwordTextField.getText()));
		});

		registerButton.addActionListener(actionEvent -> {
			ServerProxy.instance.register(usernameTextField.getText(), passwordTextField.getText(), new String[]{""});
		});
	}
}
