package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Post;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class FeedForm {
	private JPanel panel;
	private JLabel usernameLabel;
	private JButton button1;
	private JButton button2;
	private JButton button3;
	private JButton button4;
	private JButton button5;
	private JButton button6;
	private JLabel balanceLabel;
	private List<Post> posts;

	public FeedForm(){
		usernameLabel.setText(ServerProxy.instance.user);
		posts = ServerProxy.instance.getPosts();

		System.out.println(Arrays.toString(posts.stream().map(Post::toString).toArray()));
		ServerProxy.instance.createPost("test", "text");
		ServerProxy.instance.createPost("test", "text");
		ServerProxy.instance.createPost("test", "text");
		ServerProxy.instance.createPost("test", "text");
		posts = ServerProxy.instance.getPosts();
		updateBalanceLabel();
	}

	public JPanel getPanel(){
		return panel;
	}

	public void updateBalanceLabel(){
		long balance = ServerProxy.instance.getBalance();
		balanceLabel.setText(balance + " WIN");
	}
}
