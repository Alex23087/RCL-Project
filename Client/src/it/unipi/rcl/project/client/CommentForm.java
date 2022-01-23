package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Comment;

import javax.swing.*;
import java.util.Date;

public class CommentForm extends Form{
	private JLabel usernameLabel;
	private JLabel textLabel;
	private JLabel timestampLabel;
	private JPanel panel;

	public CommentForm(AppEventDelegate aed, Comment comment){
		super(aed);
		textLabel.setText(makeWrappedText(aed.getFrameWidth() - 40, comment.text));
		timestampLabel.setText(dateFormat.format(new Date(comment.timestamp)));
		ServerProxy.instance.getUsernameFromId(comment.commenterId, username -> usernameLabel.setText(username), errorMessage -> {});
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}
}
