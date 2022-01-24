package it.unipi.rcl.project.client;

import javax.swing.*;

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

		ServerProxy.instance.getWallet(balance -> balanceLabel.setText(balance + " WIN"), errorMessage -> {});
		ServerProxy.instance.getWalletInBitcoin(btcBalance -> btcLabel.setText(btcBalance + " BTC"), errorMessage -> {});

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
