package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Comment;
import it.unipi.rcl.project.common.PostView;
import it.unipi.rcl.project.common.PostViewShort;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Form that shows the page of a post together with its comments
 */
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

			//If the user has already upvoted/downvoted the post, disable the buttons
			if (postView.upvoted) {
				upvoteButton.setText(resourceBundle.getString("post.upvoted"));
				upvoteButton.setEnabled(false);
				downvoteButton.setEnabled(false);
			} else if (postView.downvoted) {
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

			//Set the comments pane up
			commentScrollPane.setViewportView(makePanelWithComments(postView.comments));
		}, errorMessage -> {
			new AlertForm("error", "error.unknown", "ok");
			//Go back to the previous page
			if (comingFromBlog) {
				appEventDelegate.onBlogTransition();
			} else {
				appEventDelegate.onFeedTransition();
			}
		});

		commentButton.addActionListener(actionEvent -> {
			if (commentTextField.getText().equals(commentHint)) {
				new AlertForm("error", "comment.error.text", "ok");
				return;
			}

			commentButton.setEnabled(false);
			String commentText = commentTextField.getText().trim();
			ServerProxy.instance.addComment(postView.id, commentText, () -> {
				//Add the new comment to the list
				postView.comments.add(0, new Comment(ServerProxy.instance.userId, commentText));
				commentScrollPane.setViewportView(makePanelWithComments(postView.comments));
				//Reset the comment text field
				commentTextField.setText(commentHint);
				commentButton.setEnabled(true);
			}, errorMessage -> {
				new AlertForm("error", "error.unknown", "ok");
				commentButton.setEnabled(true);
			});
		});

		//If the user is the author, disable the comment feature
		if (postViewShort.authorId == ServerProxy.instance.userId) {
			postCommentPanel.setVisible(false);
		}

		//If the user is not the author nor the rewinner, disable the delete post feature, and show the rewin button
		if (postViewShort.authorId != ServerProxy.instance.userId && postViewShort.rewinnerId != ServerProxy.instance.userId) {
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
		} else { //If the user is the poster or the rewinner, show the delete post button and hide the rewin one
			rewinButton.setVisible(false);
			deletePostButton.addActionListener(actionEvent -> {
				ServerProxy.instance.deletePost(postViewShort.id, () -> {
					AlertForm.successAlert("post.deleted");
					if (comingFromBlog) {
						appEventDelegate.onBlogTransition();
					} else {
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



	{
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		panel = new JPanel();
		panel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
		panel.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		profileButton = new JButton();
		profileButton.setText("Button");
		panel1.add(profileButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		blogButton = new JButton();
		blogButton.setText("Button");
		panel1.add(blogButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		discoverButton = new JButton();
		discoverButton.setText("Button");
		panel1.add(discoverButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		feedButton = new JButton();
		feedButton.setText("Button");
		panel1.add(feedButton, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		balanceButton = new JButton();
		balanceButton.setText("Button");
		panel1.add(balanceButton, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		postPanel = new JPanel();
		postPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 2, new Insets(10, 10, 10, 10), -1, -1));
		panel.add(postPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		titleLabel = new JLabel();
		Font titleLabelFont = this.$$$getFont$$$(null, Font.BOLD, -1, titleLabel.getFont());
		if (titleLabelFont != null) titleLabel.setFont(titleLabelFont);
		titleLabel.setText("Label");
		postPanel.add(titleLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		textLabel = new JLabel();
		textLabel.setText("Label");
		postPanel.add(textLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
		postPanel.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		upvoteLabel = new JLabel();
		upvoteLabel.setText("Label");
		panel2.add(upvoteLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		upvoteButton = new JButton();
		this.$$$loadButtonText$$$(upvoteButton, this.$$$getMessageFromBundle$$$("it/unipi/rcl/project/client/WinsomeStrings", "upvote"));
		panel2.add(upvoteButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		downvoteLabel = new JLabel();
		downvoteLabel.setText("Label");
		panel2.add(downvoteLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		downvoteButton = new JButton();
		this.$$$loadButtonText$$$(downvoteButton, this.$$$getMessageFromBundle$$$("it/unipi/rcl/project/client/WinsomeStrings", "downvote"));
		panel2.add(downvoteButton, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		postPanel.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		deletePostButton = new JButton();
		this.$$$loadButtonText$$$(deletePostButton, this.$$$getMessageFromBundle$$$("it/unipi/rcl/project/client/WinsomeStrings", "post.delete"));
		panel3.add(deletePostButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		authorLabel = new JLabel();
		authorLabel.setForeground(new Color(-10516293));
		authorLabel.setText("Label");
		panel3.add(authorLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		rewinButton = new JButton();
		this.$$$loadButtonText$$$(rewinButton, this.$$$getMessageFromBundle$$$("it/unipi/rcl/project/client/WinsomeStrings", "rewin"));
		postPanel.add(rewinButton, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		commentScrollPane = new JScrollPane();
		panel.add(commentScrollPane, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		final JLabel label1 = new JLabel();
		this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("it/unipi/rcl/project/client/WinsomeStrings", "comments"));
		panel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		postCommentPanel = new JPanel();
		postCommentPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		panel.add(postCommentPanel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		commentTextField = new JTextField();
		postCommentPanel.add(commentTextField, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		commentButton = new JButton();
		this.$$$loadButtonText$$$(commentButton, this.$$$getMessageFromBundle$$$("it/unipi/rcl/project/client/WinsomeStrings", "post.comment"));
		postCommentPanel.add(commentButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
		if (currentFont == null) return null;
		String resultName;
		if (fontName == null) {
			resultName = currentFont.getName();
		} else {
			Font testFont = new Font(fontName, Font.PLAIN, 10);
			if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
				resultName = fontName;
			} else {
				resultName = currentFont.getName();
			}
		}
		Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
		boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
		Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
		return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
	}

	private static Method $$$cachedGetBundleMethod$$$ = null;

	private String $$$getMessageFromBundle$$$(String path, String key) {
		ResourceBundle bundle;
		try {
			Class<?> thisClass = this.getClass();
			if ($$$cachedGetBundleMethod$$$ == null) {
				Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
				$$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
			}
			bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
		} catch (Exception e) {
			bundle = ResourceBundle.getBundle(path);
		}
		return bundle.getString(key);
	}

	/**
	 * @noinspection ALL
	 */
	private void $$$loadLabelText$$$(JLabel component, String text) {
		StringBuffer result = new StringBuffer();
		boolean haveMnemonic = false;
		char mnemonic = '\0';
		int mnemonicIndex = -1;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '&') {
				i++;
				if (i == text.length()) break;
				if (!haveMnemonic && text.charAt(i) != '&') {
					haveMnemonic = true;
					mnemonic = text.charAt(i);
					mnemonicIndex = result.length();
				}
			}
			result.append(text.charAt(i));
		}
		component.setText(result.toString());
		if (haveMnemonic) {
			component.setDisplayedMnemonic(mnemonic);
			component.setDisplayedMnemonicIndex(mnemonicIndex);
		}
	}

	/**
	 * @noinspection ALL
	 */
	private void $$$loadButtonText$$$(AbstractButton component, String text) {
		StringBuffer result = new StringBuffer();
		boolean haveMnemonic = false;
		char mnemonic = '\0';
		int mnemonicIndex = -1;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '&') {
				i++;
				if (i == text.length()) break;
				if (!haveMnemonic && text.charAt(i) != '&') {
					haveMnemonic = true;
					mnemonic = text.charAt(i);
					mnemonicIndex = result.length();
				}
			}
			result.append(text.charAt(i));
		}
		component.setText(result.toString());
		if (haveMnemonic) {
			component.setMnemonic(mnemonic);
			component.setDisplayedMnemonicIndex(mnemonicIndex);
		}
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return panel;
	}
}
