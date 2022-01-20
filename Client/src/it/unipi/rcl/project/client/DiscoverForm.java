package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Pair;
import it.unipi.rcl.project.server.User;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class DiscoverForm extends Form{
	private JPanel panel;
	private JButton profileButton;
	private JButton balanceButton;
	private JButton feedButton;
	private JButton blogButton;
	private JButton discoverButton;
	private JScrollPane scrollPane;

	DiscoverForm(AppEventDelegate aed) {
		super(aed);

		profileButton.setText(ServerProxy.instance.user);
		ServerProxy.instance.getBalance(balance -> balanceButton.setText(balance + " WIN"), errorMessage -> {});
		feedButton.addActionListener(actionEvent -> appEventDelegate.onFeedTransition());
		balanceButton.addActionListener(actionEvent -> appEventDelegate.onBalanceTransition());

		ServerProxy.instance.listUsers(users -> {
			System.out.println(Arrays.toString(users.stream().map(Pair::toString).toArray()));
			JPanel contents = new JPanel();
			contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
			for (Pair<String, String[]> u: users) {
				if(u.first.equals(ServerProxy.instance.user)){
					continue;
				}
				contents.add(new UserPanel(u));
				contents.add(new UserPanel(u));
				contents.add(new UserPanel(u));
				contents.add(new UserPanel(u));
			}
			scrollPane.getViewport().add(contents);
		}, errorMessage -> {});

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

	private class UserPanel extends JPanel{
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

			JButton followButton = new JButton(resourceBundle.getString("follow"));
			followButton.addActionListener(actionEvent -> {
				followButton.setEnabled(false);
				ServerProxy.instance.follow(user.first, () -> {
					
				}, errorMessage -> {

				});
			});
			add(followButton);
		}
	}
}
