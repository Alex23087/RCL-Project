package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.Transaction;

import javax.swing.*;
import java.util.Date;

public class BalanceForm extends WinsomeForm{
	private JPanel panel;
	private JButton profileButton;
	private JButton balanceButton;
	private JLabel balanceLabel;
	private JLabel btcLabel;
	private JButton blogButton;
	private JButton feedButton;
	private JButton discoverButton;
	private JScrollPane transactionPane;

	BalanceForm(AppEventDelegate aed) {
		super(aed);

		ServerProxy.instance.getBalance(balance -> balanceLabel.setText(balance + " WIN"), errorMessage -> {});
		ServerProxy.instance.getWalletInBitcoin(btcBalance -> btcLabel.setText(btcBalance + " BTC"), errorMessage -> {});
		ServerProxy.instance.getTransactions(transactions -> {
			if(transactions != null && transactions.size() > 0) {
				JPanel contents = new JPanel();
				contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
				for (Transaction t : transactions) {
					JPanel tPanel = new JPanel();
					tPanel.add(new JLabel(Double.toString(t.amount)));
					tPanel.add(new JLabel(dateFormat.format(new Date(t.timestamp))));
					contents.add(tPanel);
				}
				transactionPane.setViewportView(contents);
			}else{
				transactionPane.setViewportView(new JLabel(resourceBundle.getString("no.transactions")));
			}
		}, errorMessage -> {
			transactionPane.setViewportView(new JLabel(resourceBundle.getString("transaction.list.error")));
		});

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
