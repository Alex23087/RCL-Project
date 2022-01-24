package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Comment;
import it.unipi.rcl.project.common.PostView;
import it.unipi.rcl.project.common.PostViewShort;

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
	private JTextField commentTextField;
	private JButton commentButton;
	private JButton deletePostButton;
	private JButton rewinButton;
	private JPanel postCommentPanel;
	private PostView postView;

	public PostViewForm(AppEventDelegate aed, PostViewShort postViewShort, boolean comingFromBlog) {
		super(aed);
		String commentHint = resourceBundle.getString("comment.text") + " ";

		titleLabel.setText(makeWrappedText(appEventDelegate.getFrameWidth() - 40, postViewShort.title));
		upvoteButton.setEnabled(false);
		downvoteButton.setEnabled(false);
		commentButton.setEnabled(false);
		setHint(commentTextField, commentHint);

		ServerProxy.instance.getUsernameFromId(postViewShort.authorId, username -> authorLabel.setText(username), errorMessage -> {});
		ServerProxy.instance.showPost(postViewShort.id, postView -> {
			this.postView = postView;
			upvoteLabel.setText(Integer.toString(postView.upvotes));
			downvoteLabel.setText(Integer.toString(postView.downvotes));
			textLabel.setText(makeWrappedText(appEventDelegate.getFrameWidth() - 40, postView.text));
			commentButton.setEnabled(true);

			if(postView.upvoted){
				upvoteButton.setText(resourceBundle.getString("post.upvoted"));
				upvoteButton.setEnabled(false);
				downvoteButton.setEnabled(false);
			} else if (postView.downvoted){
				downvoteButton.setText(resourceBundle.getString("post.downvoted"));
				upvoteButton.setEnabled(false);
				downvoteButton.setEnabled(false);
			} else {
				upvoteButton.setEnabled(true);
				downvoteButton.setEnabled(true);

				upvoteButton.addActionListener(actionEvent -> {
					upvoteButton.setEnabled(false);
					downvoteButton.setEnabled(false);
					ServerProxy.instance.ratePost(postView.id, true, () -> {
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
					ServerProxy.instance.ratePost(postView.id, false, () -> {
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

			commentScrollPane.setViewportView(makePanelWithComments(postView.comments));
		}, errorMessage -> {
			new AlertForm("error", "error.unknown", "ok");
			if(comingFromBlog){
				appEventDelegate.onBlogTransition();
			}else{
				appEventDelegate.onFeedTransition();
			}
		});

		commentButton.addActionListener(actionEvent -> {
			if(commentTextField.getText().equals(commentHint)){
				new AlertForm("error", "comment.error.text", "ok");
				return;
			}

			commentButton.setEnabled(false);
			String commentText = commentTextField.getText().strip();
			ServerProxy.instance.addComment(postView.id, commentText, () -> {
				postView.comments.add(0, new Comment(ServerProxy.instance.userId, commentText));
				commentScrollPane.setViewportView(makePanelWithComments(postView.comments));
				commentTextField.setText(commentHint);
				commentButton.setEnabled(true);
			}, errorMessage -> {
				new AlertForm("error", "error.unknown", "ok");
				commentButton.setEnabled(true);
			});
		});

		if(postViewShort.authorId == ServerProxy.instance.userId){
			postCommentPanel.setVisible(false);
		}

		if(postViewShort.authorId != ServerProxy.instance.userId && postViewShort.rewinnerId != ServerProxy.instance.userId) {
			deletePostButton.setVisible(false);
			rewinButton.setVisible(true);
			rewinButton.addActionListener(actionEvent -> {
				rewinButton.setEnabled(false);
				ServerProxy.instance.rewinPost(postViewShort.id, () -> {
					AlertForm.successAlert("post.rewinned");
				}, errorMessage -> {
					rewinButton.setEnabled(true);
					AlertForm.errorAlert();
				});
			});
		}else{
			rewinButton.setVisible(false);
			deletePostButton.addActionListener(actionEvent -> {
				ServerProxy.instance.deletePost(postViewShort.id, () -> {
					AlertForm.successAlert("post.deleted");
					if(comingFromBlog){
						appEventDelegate.onBlogTransition();
					}else{
						appEventDelegate.onFeedTransition();
					}
				}, errorMessage -> {
					AlertForm.errorAlert("error.unknown");
				});
			});
		}

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
