package it.unipi.rcl.project.client;

import javax.swing.*;

public class ProfileForm extends WinsomeForm{
	private JButton profileButton;
	private JButton blogButton;
	private JButton discoverButton;
	private JButton feedButton;
	private JButton balanceButton;
	private JPanel panel;
	private JButton logoutButton;
	private JLabel usernameLabel;
	private JLabel followedLabel;
	private JLabel followersLabel;
	private JScrollPane followersPane;
	private JScrollPane followedPane;

	public ProfileForm(AppEventDelegate aed) {
		super(aed);
		usernameLabel.setText(ServerProxy.instance.user);
		ServerProxy.instance.getFollowerCount(count -> followersLabel.setText(count + " " + resourceBundle.getString("followers")), errorMessage -> {});
		ServerProxy.instance.getFollowedCount(count -> followedLabel.setText(count + " " + resourceBundle.getString("followed")), errorMessage -> {});
		logoutButton.addActionListener(actionEvent -> ServerProxy.instance.logout(aed::onLogout, errorMessage -> {}));
		profileButton.setEnabled(false);

		ServerProxy.instance.listFollowers(followers -> {
			if(followers == null || followers.size() < 1){
				followersPane.setViewportView(new JLabel(resourceBundle.getString("no.followers")));
			}else{
				JPanel contents = new JPanel();
				contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
				for(int userId: followers){
					JLabel userLabel = new JLabel(Integer.toString(userId));
					ServerProxy.instance.getUsernameFromId(userId, userLabel::setText, errorMessage -> {});
					contents.add(userLabel);
				}
				followersPane.setViewportView(contents);
			}
		}, errorMessage -> {});

		ServerProxy.instance.listFollowing(followed -> {
			if(followed == null || followed.size() < 1){
				followedPane.setViewportView(new JLabel(resourceBundle.getString("no.followed")));
			}else{
				JPanel contents = new JPanel();
				contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
				for(int userId: followed){
					JLabel userLabel = new JLabel(Integer.toString(userId));
					ServerProxy.instance.getUsernameFromId(userId, userLabel::setText, errorMessage -> {});
					contents.add(userLabel);
				}
				followedPane.setViewportView(contents);
			}
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
}
