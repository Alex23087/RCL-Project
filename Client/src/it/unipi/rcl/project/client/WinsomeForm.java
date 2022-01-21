package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Post;

import javax.swing.*;
import java.util.List;

public abstract class WinsomeForm extends Form{

	public WinsomeForm(AppEventDelegate aed){
		super(aed);
	}

	protected final void init(){
		getBalanceButton().addActionListener(actionEvent -> appEventDelegate.onBalanceTransition());
		getBlogButton().addActionListener(actionEvent -> appEventDelegate.onBlogTransition());
		getDiscoverButton().addActionListener(actionEvent -> appEventDelegate.onDiscoverTransition());
		getFeedButton().addActionListener(actionEvent -> appEventDelegate.onFeedTransition());
		//getProfileButton().addActionListener(actionEvent -> appEventDelegate.onProfileTransition());

		getProfileButton().setText(ServerProxy.instance.user);
		ServerProxy.instance.getBalance(balance -> getBalanceButton().setText(balance + " WIN"), errorMessage -> {});
	}

	protected abstract JButton getBalanceButton();
	protected abstract JButton getBlogButton();
	protected abstract JButton getDiscoverButton();
	protected abstract JButton getFeedButton();
	protected abstract JButton getProfileButton();

	protected final JPanel makePanelWithPosts(List<Post> posts){
		JPanel contents = new JPanel();
		contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
		for (Post p: posts) {
			contents.add(new FeedPostForm(p).panel);
		}
		return contents;
	}
}