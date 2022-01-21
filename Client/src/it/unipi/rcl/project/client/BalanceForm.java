package it.unipi.rcl.project.client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BalanceForm extends WinsomeForm{
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

		init();
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

	@Override
	public JPanel getPanel() {
		return panel;
	}
}
