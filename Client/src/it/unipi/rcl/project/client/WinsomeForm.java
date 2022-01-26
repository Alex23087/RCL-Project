package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Comment;
import it.unipi.rcl.project.common.PostViewShort;

import javax.swing.*;
import java.util.List;

/**
 * Class that represents a single page of the application.
 * Holds utility functions and initialisation code for the navigation bar.
 */
public abstract class WinsomeForm extends Form{

	public WinsomeForm(AppEventDelegate aed){
		super(aed);
	}

	/**
	 * Method called to initialise the navigation bar. Needs to be called after the first stages of the constructor.
	 */
	protected final void init(){
		getFeedButton().setText(resourceBundle.getString("feed"));
		getDiscoverButton().setText(resourceBundle.getString("discover"));
		getBlogButton().setText(resourceBundle.getString("blog"));
		getBalanceButton().addActionListener(actionEvent -> appEventDelegate.onBalanceTransition());
		getBlogButton().addActionListener(actionEvent -> appEventDelegate.onBlogTransition());
		getDiscoverButton().addActionListener(actionEvent -> appEventDelegate.onDiscoverTransition());
		getFeedButton().addActionListener(actionEvent -> appEventDelegate.onFeedTransition());
		getProfileButton().addActionListener(actionEvent -> appEventDelegate.onProfileTransition());

		getProfileButton().setText(ServerProxy.instance.user);
		ServerProxy.instance.getBalance(balance -> getBalanceButton().setText(balance + " WIN"), errorMessage -> {});
	}

	protected abstract JButton getBalanceButton();
	protected abstract JButton getBlogButton();
	protected abstract JButton getDiscoverButton();
	protected abstract JButton getFeedButton();
	protected abstract JButton getProfileButton();

	/**
	 * Creates a panel that shows the posts in the list passed as parameter.
	 */
	protected final JPanel makePanelWithPostViews(List<PostViewShort> posts, boolean isBlog){
		JPanel contents = new JPanel();
		contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
		for (PostViewShort p: posts) {
			contents.add(new FeedPostForm(appEventDelegate, p, isBlog).panel);
		}
		return contents;
	}

	/**
	 * Creates a panel that shows the comments in the list passed as parameter.
	 */
	protected final JComponent makePanelWithComments(List<Comment> comments){
		if(comments != null && comments.size() > 0) {
			JPanel contents = new JPanel();
			contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
			for (Comment c : comments) {
				contents.add(new CommentForm(appEventDelegate, c).getPanel());
			}
			return contents;
		}else{
			return new JLabel(resourceBundle.getString("no.comments"));
		}
	}
}