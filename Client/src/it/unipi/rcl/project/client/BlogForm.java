package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.PostViewShort;

import javax.swing.*;
import java.util.List;

public class BlogForm extends WinsomeForm {
	private JButton feedButton;
	private JButton balanceButton;
	private JPanel panel;
	private JButton blogButton;
	private JButton profileButton;
	private JButton discoverButton;
	private JScrollPane scrollPane;
	private JTextField textField;
	private JButton postButton;
	private JTextField titleField;

	private List<PostViewShort> posts;

	BlogForm(AppEventDelegate aed) {
		super(aed);
		String textHint = resourceBundle.getString("post.text") + " ";
		String titleHint = resourceBundle.getString("post.title") + " ";

		ServerProxy.instance.getPosts(posts -> {
			this.posts = posts;
			scrollPane.setViewportView(makePanelWithPostViews(posts, true));
		}, errorMessage -> {});

		postButton.addActionListener(actionEvent -> {
			if(textField.getText().equals(textHint)){
				new AlertForm("error", "post.error.text", "ok");
			}else if(titleField.getText().equals(titleHint)){
				new AlertForm("error", "post.error.title", "ok");
			}else{
				postButton.setEnabled(false);
				ServerProxy.instance.createPost(titleField.getText().strip(), textField.getText().strip(), id -> {
					new AlertForm("success", "post.successful", "ok");
					titleField.setText(titleHint);
					textField.setText(textHint);
					postButton.setEnabled(true);
					ServerProxy.instance.getPostViewFromId(id, postView -> {
						posts.add(0, postView);
						scrollPane.setViewportView(makePanelWithPostViews(posts, true));
					}, errorMessage -> {});
				}, errorMessage -> {
					postButton.setEnabled(true);
				});
			}
		});

		setHint(textField, textHint);
		setHint(titleField, titleHint);

		blogButton.setEnabled(false);
		init();
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}

	@Override
	protected JButton getBalanceButton() {
		return balanceButton;
	}

	@Override
	protected JButton getBlogButton() {
		return blogButton;
	}

	@Override
	protected JButton getDiscoverButton() {
		return discoverButton;
	}

	@Override
	protected JButton getFeedButton() {
		return feedButton;
	}

	@Override
	protected JButton getProfileButton() {
		return profileButton;
	}
}
