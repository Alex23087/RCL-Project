package it.unipi.rcl.project.client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

public class RegisterTagsForm {
	private JTextField textField1;
	private JTextField textField2;
	private JTextField textField3;
	private JTextField textField4;
	private JTextField textField5;
	private JButton acceptButton;
	private JPanel panel;

	public RegisterTagsForm(ServerProxy.Callback<String[]> callback) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setContentPane(panel);
		frame.setVisible(true);
		frame.setSize(500, 500);
		frame.setTitle("");

		acceptButton.addActionListener(actionEvent -> {
			List<String> tags = new ArrayList<>(5);
			addConditionally(textField1, tags);
			addConditionally(textField2, tags);
			addConditionally(textField3, tags);
			addConditionally(textField4, tags);
			addConditionally(textField5, tags);
			frame.dispose();
			callback.run(tags.toArray(new String[0]));
		});

		textField1.addKeyListener(new TagListener(textField1));
		textField2.addKeyListener(new TagListener(textField2));
		textField3.addKeyListener(new TagListener(textField3));
		textField4.addKeyListener(new TagListener(textField4));
		textField5.addKeyListener(new TagListener(textField5));

		acceptButton.setEnabled(false);
	}

	private static String getCleanTag(String tag){
		return tag.strip();
	}

	private static boolean isValidTag(String tag){
		return tag.length() > 0;
	}

	private static void addConditionally(JTextField textField, List<String> list){
		String t = getCleanTag(textField.getText());
		if(isValidTag(t)){
			list.add(t);
		}
	}

	private class TagListener implements KeyListener {
		JTextField tagField;

		public TagListener(JTextField tf){
			tagField = tf;
		}

		@Override
		public void keyTyped(KeyEvent keyEvent) {
		}

		@Override
		public void keyPressed(KeyEvent keyEvent) {

		}

		@Override
		public void keyReleased(KeyEvent keyEvent) {
			if(!acceptButton.isEnabled() && isTagFieldValid()){
				acceptButton.setEnabled(true);
			}else if(acceptButton.isEnabled() && !isAnyTagFieldValid()){
				acceptButton.setEnabled(false);
			}
		}

		private boolean isTagFieldValid(JTextField tagField){
			return isValidTag(getCleanTag(tagField.getText()));
		}

		private boolean isTagFieldValid(){
			return isTagFieldValid(this.tagField);
		}

		private boolean isAnyTagFieldValid(){
			return
					isTagFieldValid(textField1) ||
					isTagFieldValid(textField2) ||
					isTagFieldValid(textField3) ||
					isTagFieldValid(textField4) ||
					isTagFieldValid(textField5);
		}
	}
}
