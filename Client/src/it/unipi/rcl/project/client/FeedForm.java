package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Post;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

public class FeedForm extends Form{
	private JPanel panel;
	private JLabel usernameLabel;
	private JButton profileButton;
	private JButton balanceButton;
	private JButton feedButton;
	private JButton blogButton;
	private JButton discoverButton;
	private JScrollPane feedPane;
	private List<Post> posts;

	public FeedForm(AppEventDelegate appEventDelegate){
		super(appEventDelegate);

		ServerProxy.instance.getPosts(posts1 -> {
			this.posts = posts1;
			JPanel contents = new JPanel();
			contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
			for (Post p: posts) {
				contents.add(new FeedPostForm(p).panel);
			}
			feedPane.setViewportView(contents);
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

	private static class PostPanel extends JPanel{
		public PostPanel(Post post){
			super();
			setLayout(new FlowLayout());
			JLabel postTitleLabel = new JLabel(post.title);
			JLabel postTextLabel = new JLabel(post.text);
			add(postTitleLabel);
			add(postTextLabel);
		}
	}
}
