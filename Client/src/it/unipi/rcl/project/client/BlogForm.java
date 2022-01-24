package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.PostViewShort;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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

		ServerProxy.instance.viewBlog(posts -> {
			this.posts = posts;
			if(posts.size() > 0) {
				scrollPane.setViewportView(makePanelWithPostViews(posts, true));
			}else{
				scrollPane.setViewportView(new JLabel(makeWrappedText(appEventDelegate.getFrameWidth() - 50, resourceBundle.getString("blog.no.posts"))));
			}
		}, errorMessage -> {});

		postButton.addActionListener(actionEvent -> {
			String postText = textField.getText();
			String postTitle = titleField.getText();

			if(postTitle.equals(titleHint)){
				new AlertForm("error", "post.error.title", "ok");
				return;
			}
			if(postText.equals(textHint)){
				new AlertForm("error", "post.error.text", "ok");
				return;
			}
			postTitle = postTitle.strip();
			if(postTitle.length() > 20){
				AlertForm.errorAlert("post.title.too.long");
				return;
			}
			postText = postText.strip();
			if(postText.length() > 500){
				AlertForm.errorAlert("post.text.too.long");
				return;
			}

			postButton.setEnabled(false);
			ServerProxy.instance.createPost(titleField.getText().strip(), textField.getText().strip(), id -> {
				new AlertForm("success", "post.successful", "ok");
				titleField.setText(titleHint);
				textField.setText(textHint);
				postButton.setEnabled(true);
				ServerProxy.instance.showPost(id, postView -> {
					posts.add(0, postView);
					scrollPane.setViewportView(makePanelWithPostViews(posts, true));
				}, errorMessage -> {});
			}, errorMessage -> {
				postButton.setEnabled(true);
			});
		});

		setHint(textField, textHint);
		setHint(titleField, titleHint);

		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if(textField.getText().length() >= 500){
					e.consume();
				}
			}
		});

		titleField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if(titleField.getText().length() >= 20){
					e.consume();
				}
			}
		});

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
