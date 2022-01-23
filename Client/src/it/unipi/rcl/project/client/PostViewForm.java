package it.unipi.rcl.project.client;

import it.unipi.rcl.project.client.AppEventDelegate;
import it.unipi.rcl.project.client.WinsomeForm;
import it.unipi.rcl.project.common.PostView;
import it.unipi.rcl.project.common.PostViewShort;
import it.unipi.rcl.project.server.ServerData;

import javax.swing.*;

public class PostViewForm extends WinsomeForm {
	private JButton profileButton;
	private JButton blogButton;
	private JButton discoverButton;
	private JButton feedButton;
	private JButton balanceButton;
	private JPanel panel;
	private JButton upvoteButton;
	private JButton downvoteButton;
	private JLabel authorLabel;
	private JLabel titleLabel;
	private JLabel textLabel;
	private JLabel upvoteLabel;
	private JLabel downvoteLabel;
	private JScrollPane commentScrollPane;
	private JPanel postPanel;
	private PostView postView;

	public PostViewForm(AppEventDelegate aed, PostViewShort postViewShort, boolean comingFromBlog) {
		super(aed);
		titleLabel.setText(makeWrappedText(appEventDelegate.getFrameWidth() - 40, postViewShort.title));
		ServerProxy.instance.getUsernameFromId(postViewShort.authorId, username -> authorLabel.setText(username), errorMessage -> {});
		ServerProxy.instance.getPostViewFromId(postViewShort.id, postView -> {
			this.postView = postView;
			upvoteLabel.setText(Integer.toString(postView.upvotes));
			downvoteLabel.setText(Integer.toString(postView.downvotes));
			textLabel.setText(makeWrappedText(appEventDelegate.getFrameWidth() - 40, postView.text));

			if(postView.upvoted){
				upvoteButton.setText(resourceBundle.getString("post.upvoted"));
				upvoteButton.setEnabled(false);
				downvoteButton.setEnabled(false);
			}else if (postView.downvoted){
				downvoteButton.setText(resourceBundle.getString("post.downvoted"));
				upvoteButton.setEnabled(false);
				downvoteButton.setEnabled(false);
			}else {
				upvoteButton.addActionListener(actionEvent -> {
					upvoteButton.setEnabled(false);
					downvoteButton.setEnabled(false);
					ServerProxy.instance.vote(postView.id, true, () -> {
						postView.setUpvoted();
						postView.upvotes++;
						upvoteLabel.setText(Integer.toString(postView.upvotes));
						upvoteButton.setText(resourceBundle.getString("post.upvoted"));
					}, errorMessage -> {
						upvoteButton.setEnabled(true);
						downvoteButton.setEnabled(true);
						switch (errorMessage) {
							case VoterIsAuthor:
								new AlertForm("error", "error.voter.is.author", "ok");
						}
					});
				});

				downvoteButton.addActionListener(actionEvent -> {
					upvoteButton.setEnabled(false);
					downvoteButton.setEnabled(false);
					ServerProxy.instance.vote(postView.id, false, () -> {
						postView.setDownvoted();
						postView.downvotes++;
						downvoteLabel.setText(Integer.toString(postView.downvotes));
						downvoteButton.setText(resourceBundle.getString("post.downvoted"));
					}, errorMessage -> {
						upvoteButton.setEnabled(true);
						downvoteButton.setEnabled(true);
						switch (errorMessage) {
							case VoterIsAuthor:
								new AlertForm("error", "error.voter.is.author", "ok");
						}
					});
				});
			}
		}, errorMessage -> {
			new AlertForm("error", "error.unknown", "ok");
			if(comingFromBlog){
				appEventDelegate.onBlogTransition();
			}else{
				appEventDelegate.onFeedTransition();
			}
		});
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
