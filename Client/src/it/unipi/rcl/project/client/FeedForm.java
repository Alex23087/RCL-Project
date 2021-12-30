package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Post;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.util.Arrays;
import java.util.List;

public class FeedForm extends Form{
	private JPanel panel;
	private JLabel usernameLabel;
	private JButton button1;
	private JButton button2;
	private JButton button3;
	private JButton button4;
	private JButton button5;
	private JButton button6;
	private JButton usernameButton;
	private JButton balanceButton;
	private JScrollPane feedPanel;
	private List<Post> posts;

	public FeedForm(AppEventDelegate appEventDelegate){
		super(appEventDelegate);

		usernameButton.setText(ServerProxy.instance.user);
		posts = ServerProxy.instance.getPosts();

		System.out.println(Arrays.toString(posts.stream().map(Post::toString).toArray()));
		ServerProxy.instance.createPost("test", "text");
		ServerProxy.instance.createPost("test", "text");
		ServerProxy.instance.createPost("test", "text");
		ServerProxy.instance.createPost("test", "text");
		posts = ServerProxy.instance.getPosts();
		updateBalanceLabel();


		usernameButton.addActionListener(actionEvent -> {

		});
	}

	@Override
	public JPanel getPanel(){
		return panel;
	}

	public void updateBalanceLabel(){
		long balance = ServerProxy.instance.getBalance();
		balanceButton.setText(balance + " WIN");
	}
}
