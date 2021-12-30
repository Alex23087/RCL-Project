package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Post;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	private JButton profileButton;
	private JButton balanceButton;
	private JScrollPane feedPanel;
	private JButton feedButton;
	private JButton blogButton;
	private JButton discoverButton;
	private List<Post> posts;

	public FeedForm(AppEventDelegate appEventDelegate){
		super(appEventDelegate);

		ServerProxy.instance.getPosts(posts1 -> {
			this.posts = posts1;
			System.out.println(Arrays.toString(posts.stream().map(Post::toString).toArray()));
		}, errorMessage -> {});

		ServerProxy.instance.createPost("test", "text", id -> {}, errorMessage -> {});
		ServerProxy.instance.createPost("test", "text", id -> {}, errorMessage -> {});
		ServerProxy.instance.createPost("test", "text", id -> {}, errorMessage -> {});
		ServerProxy.instance.createPost("test", "text", id -> {}, errorMessage -> {});
		ServerProxy.instance.getPosts(posts1 -> {
			this.posts = posts1;
			System.out.println(Arrays.toString(posts.stream().map(Post::toString).toArray()));
		}, errorMessage -> {});



		profileButton.setText(ServerProxy.instance.user);
		updateBalanceLabel();


		blogButton.addActionListener(actionEvent -> appEventDelegate.onBlogTransition());

		balanceButton.addActionListener(actionEvent -> appEventDelegate.onBalanceTransition());
		discoverButton.addActionListener(actionEvent -> appEventDelegate.onDiscoverTransition());
	}

	@Override
	public JPanel getPanel(){
		return panel;
	}

	public void updateBalanceLabel(){
		ServerProxy.instance.getBalance(balance -> balanceButton.setText(balance + " WIN"), errorMessage -> {});
	}
}
