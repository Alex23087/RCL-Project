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

	public ProfileForm(AppEventDelegate aed) {
		super(aed);
		usernameLabel.setText(ServerProxy.instance.user);
		followedLabel.setText(ServerProxy.instance.followed.size() + " " + resourceBundle.getString("followed"));
		followersLabel.setText(ServerProxy.instance.followers.size() + " " + resourceBundle.getString("followers"));
		logoutButton.addActionListener(actionEvent -> ServerProxy.instance.logout(aed::onLogout, errorMessage -> {}));
		profileButton.setEnabled(false);
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
