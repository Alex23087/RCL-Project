package it.unipi.rcl.project.client;

import javax.swing.*;

public class BalanceForm extends Form{
	private JPanel panel;
	private JButton button1;
	private JButton button2;
	private JLabel balanceLabel;
	private JLabel btcLabel;

	BalanceForm(AppEventDelegate aed) {
		super(aed);

		balanceLabel.setText(ServerProxy.instance.getBalance() + " WIN");
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}
}
