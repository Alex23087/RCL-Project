package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Pair;

import javax.swing.*;
import java.util.Arrays;

public class DiscoverForm extends Form{
	private JPanel panel;
	private JButton profileButton;
	private JButton balanceButton;
	private JButton feedButton;
	private JButton blogButton;
	private JButton discoverButton;

	DiscoverForm(AppEventDelegate aed) {
		super(aed);

		profileButton.setText(ServerProxy.instance.user);
		ServerProxy.instance.getBalance(balance -> balanceButton.setText(balance + " WIN"), errorMessage -> {});
		feedButton.addActionListener(actionEvent -> appEventDelegate.onFeedTransition());
		balanceButton.addActionListener(actionEvent -> appEventDelegate.onBalanceTransition());

		ServerProxy.instance.listUsers(users -> {
			System.out.println(Arrays.toString(users.stream().map(Pair::toString).toArray()));
		}, errorMessage -> {});
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}
}
