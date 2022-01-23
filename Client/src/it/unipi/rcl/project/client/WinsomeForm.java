package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Comment;
import it.unipi.rcl.project.common.PostViewShort;

import javax.swing.*;
import java.util.List;

public abstract class WinsomeForm extends Form{

	public WinsomeForm(AppEventDelegate aed){
		super(aed);
	}

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

	protected final JPanel makePanelWithPostViews(List<PostViewShort> posts, boolean isBlog){
		JPanel contents = new JPanel();
		contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
		for (PostViewShort p: posts) {
			contents.add(new FeedPostForm(appEventDelegate, p, isBlog).panel);
		}
		return contents;
	}

	protected final JPanel makePanelWithComments(List<Comment> comments){
		JPanel contents = new JPanel();
		contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
		for (Comment c: comments) {
			contents.add(new CommentForm(appEventDelegate, c).getPanel());
		}
		return contents;
	}
}