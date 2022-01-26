package it.unipi.rcl.project.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

/**
 * Class that holds various utility functions for the forms used in the app
 */
public abstract class Form {
	protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	protected static ResourceBundle resourceBundle = ResourceBundle.getBundle("it/unipi/rcl/project/client/WinsomeStrings");

	protected AppEventDelegate appEventDelegate;

	/**
	 * Returns the main panel that contains the contents of this form
	 */
	public abstract JPanel getPanel();

	protected Form(AppEventDelegate aed){
		this.appEventDelegate = aed;
	}

	/**
	 * Sets a hint to a JTextField (swing does not provide that feature natively).
	 * This is achieved by setting the text to the hint, then registering a focus listener
	 * that clears the text when the component is focused and the text is the hint,
	 * then if the component loses focus and it's empty, puts the hint text back in it.
	 */
	protected static void setHint(JTextField textField, String hint){
		textField.setText(hint);

		//If the textField is a JPasswordField, set it to show it contents as clear text
		if(textField instanceof JPasswordField){
			((JPasswordField) textField).setEchoChar((char) 0);
		}

		textField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if(textField.getText().equals(hint)){
					textField.setText("");
					//Restoring default behaviour of the JPasswordField
					if(textField instanceof JPasswordField){
						((JPasswordField) textField).setEchoChar('*');
					}
					textField.setForeground(Color.BLACK);
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if(textField.getText().equals("")){
					textField.setText(hint);
					//Do not hide JPasswordField's contents
					if(textField instanceof JPasswordField){
						((JPasswordField) textField).setEchoChar((char) 0);
					}
					textField.setForeground(Color.LIGHT_GRAY);
				}
			}
		});
	}

	/**
	 * Moves the frame to the center of the screen
	 */
	public static void centerFrame(JFrame frame){
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation((int) (dimension.getWidth() / 2 - frame.getSize().getWidth() / 2), (int) (dimension.getHeight() / 2 - frame.getSize().getHeight() / 2));
	}

	/**
	 * Hack to allow text inside JLabels to wrap
	 */
	protected static String makeWrappedText(int width, String str){
		return "<html><div width=" + width + ">" + str + "</div></html>";
	}
}