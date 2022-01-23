package it.unipi.rcl.project.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ResourceBundle;

public abstract class Form {
	protected static ResourceBundle resourceBundle = ResourceBundle.getBundle("it/unipi/rcl/project/client/WinsomeStrings");
	protected AppEventDelegate appEventDelegate;

	Form(AppEventDelegate aed){
		this.appEventDelegate = aed;
	}

	protected static void setHint(JTextField textField, String hint){
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

	public abstract JPanel getPanel();

	public static void centerFrame(JFrame frame){
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation((int) (dimension.getWidth() / 2 - frame.getSize().getWidth() / 2), (int) (dimension.getHeight() / 2 - frame.getSize().getHeight() / 2));
	}

	protected final static String makeWrappedText(int width, String str){
		return "<html><div width=" + width + ">" + str + "</div></html>";
	}
}