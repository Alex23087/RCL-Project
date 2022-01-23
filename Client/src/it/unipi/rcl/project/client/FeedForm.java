package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.PostViewShort;

import javax.swing.*;
import java.util.List;

public class FeedForm extends WinsomeForm{
	private JPanel panel;
	private JButton profileButton;
	private JButton balanceButton;
	private JButton feedButton;
	private JButton blogButton;
	private JButton discoverButton;
	private JScrollPane feedPane;
	private List<PostViewShort> posts;

	public FeedForm(AppEventDelegate appEventDelegate){
		super(appEventDelegate);

		ServerProxy.instance.getFeed(posts -> {
			this.posts = posts;
			feedPane.setViewportView(makePanelWithPostViews(posts, false));
		}, errorMessage -> {});

		init();
	}

	@Override
	public JPanel getPanel(){
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
