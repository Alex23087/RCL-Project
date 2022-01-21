package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Post;

import javax.swing.*;
import java.awt.*;

public class FeedPostForm {
	private JLabel titleLabel;
	private JLabel textLabel;
	private JLabel usernameLabel;
	public JPanel panel;

	public FeedPostForm (Post post){
		super();
		titleLabel.setText(post.title);
		textLabel.setText(post.text);
		usernameLabel.setText(post.authorId + "");
	}
}
