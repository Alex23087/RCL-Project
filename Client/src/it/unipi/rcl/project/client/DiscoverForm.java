package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class DiscoverForm extends WinsomeForm{
	private JPanel panel;
	private JButton profileButton;
	private JButton balanceButton;
	private JButton feedButton;
	private JButton blogButton;
	private JButton discoverButton;
	private JScrollPane scrollPane;

	DiscoverForm(AppEventDelegate aed) {
		super(aed);

		ServerProxy.instance.listUsers(users -> {
			JPanel contents = new JPanel();
			contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
			for (Pair<Integer, String[]> u: users) {
				if(u.first.equals(ServerProxy.instance.userId)){
					continue;
				}
				contents.add(new UserPanel(u));
			}
			scrollPane.getViewport().add(contents);
		}, errorMessage -> {});

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

	private static class UserPanel extends JPanel{
		private boolean isFollowed;

		public UserPanel(Pair<Integer, String[]> user){
			super();
			setLayout(new FlowLayout());
			JLabel usernameLabel = new JLabel(user.first.toString());
			ServerProxy.instance.getUsernameFromId(user.first, usernameLabel::setText, errorMessage -> {});
			usernameLabel.setHorizontalTextPosition(JLabel.TRAILING);
			add(usernameLabel);

			JPanel tags = new JPanel();
			tags.setLayout(new FlowLayout());
			for(String t: user.second){
				JLabel tagLabel = new JLabel(t);
				tagLabel.setForeground(Color.GRAY);
				tags.add(tagLabel);
			}
			add(tags);

			JButton followButton = new JButton();

			isFollowed = ServerProxy.instance.followed.contains(user.first);

			if(isFollowed){
				followButton.setText(resourceBundle.getString("unfollow"));
			}else {
				followButton.setText(resourceBundle.getString("follow"));
			}

			followButton.addActionListener(actionEvent -> {
				followButton.setEnabled(false);
				if(isFollowed){
					ServerProxy.instance.unfollow(user.first, () -> {
						followButton.setText(resourceBundle.getString("follow"));
						isFollowed = false;
						followButton.setEnabled(true);
					}, errorMessage -> {
						switch (errorMessage) {
							case NotFollowing:
								new AlertForm("error", "error.not.following", "ok");
								break;
							case InvalidUserId:
								new AlertForm("error", "user.invalid", "ok");
								break;
						}
						followButton.setEnabled(true);
					});
				}else{
					ServerProxy.instance.follow(user.first, () -> {
						followButton.setText(resourceBundle.getString("unfollow"));
						isFollowed = true;
						followButton.setEnabled(true);
					}, errorMessage -> {
						switch (errorMessage) {
							case AlreadyFollowed:
								new AlertForm("error", "error.already.followed", "ok");
								break;
							case InvalidUserId:
								new AlertForm("error", "user.invalid", "ok");
								break;
						}
						followButton.setEnabled(true);
					});
				}
			});
			add(followButton);
		}
	}
}
