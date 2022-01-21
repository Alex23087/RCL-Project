package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Pair;

import javax.swing.*;
import java.awt.*;
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
			System.out.println(Arrays.toString(users.stream().map(Pair::toString).toArray()));
			JPanel contents = new JPanel();
			contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
			for (Pair<String, String[]> u: users) {
				if(u.first.equals(ServerProxy.instance.user)){
					continue;
				}
				contents.add(new UserPanel(u));
			}
			scrollPane.getViewport().add(contents);
		}, errorMessage -> {});

		init();
	}

	private void updatePane(){
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(this::updatePane);
			return;
		}
		scrollPane.setViewportView(scrollPane.getViewport());
		scrollPane.revalidate();
		scrollPane.repaint();
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
		public UserPanel(Pair<String, String[]> user){
			super();
			setLayout(new FlowLayout());
			JLabel usernameLabel = new JLabel(user.first);
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
			if(ServerProxy.instance.followed.contains(user.first)){
				followButton.setText(resourceBundle.getString("following"));
				followButton.setEnabled(false);
				//TODO: maybe add unfollow button
			}else {
				followButton.setText(resourceBundle.getString("follow"));
				followButton.addActionListener(actionEvent -> {
					followButton.setEnabled(false);
					ServerProxy.instance.follow(user.first, () -> {
						followButton.setText(resourceBundle.getString("following"));
					}, errorMessage -> {
						switch (errorMessage) {
							case AlreadyFollowed:
								new AlertForm("error", "error.already.followed", "ok");
								break;
							case InvalidUsername:
								new AlertForm("error", "user.invalid", "ok");
								break;
						}
						followButton.setEnabled(true);
					});
				});
			}
			add(followButton);
		}
	}
}
