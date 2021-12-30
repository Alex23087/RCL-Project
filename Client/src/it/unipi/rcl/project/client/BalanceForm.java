package it.unipi.rcl.project.client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BalanceForm extends Form{
	private JPanel panel;
	private JButton profileButton;
	private JButton balanceButton;
	private JLabel balanceLabel;
	private JLabel btcLabel;
	private JButton blogButton;
	private JButton feedButton;
	private JButton discoverButton;

	BalanceForm(AppEventDelegate aed) {
		super(aed);

		ServerProxy.instance.getBalance(balance -> balanceLabel.setText(balance + " WIN"), errorMessage -> {});
		ServerProxy.instance.getBTCBalance(btcBalance -> btcLabel.setText(btcBalance + " BTC"), errorMessage -> {});

		profileButton.setText(ServerProxy.instance.user);
		feedButton.addActionListener(actionEvent -> appEventDelegate.onFeedTransition());
		discoverButton.addActionListener(actionEvent -> appEventDelegate.onDiscoverTransition());
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}
}
