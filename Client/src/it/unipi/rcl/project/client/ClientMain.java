package it.unipi.rcl.project.client;

import javax.swing.*;
import java.awt.*;

public class ClientMain {

    public static void main(String[] args) {
	    JFrame appFrame = new JFrame();
	    appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    appFrame.setVisible(true);
	    appFrame.setSize(500, 500);
	    appFrame.setTitle("Winsome");

		AppEventDelegate aed = new AppEventDelegate() {
			@Override
			public void onLoginComplete() {
				ServerProxy.instance.getFollowed(f -> {}, em -> {});
				onFeedTransition();
			}

			@Override
			public void onBlogTransition() {
				transitionToForm(new BlogForm(this));
			}

			@Override
			public void onFeedTransition() {
				transitionToForm(new FeedForm(this));
			}

			@Override
			public void onBalanceTransition() {
				transitionToForm(new BalanceForm(this));
			}

			@Override
			public void onDiscoverTransition() {
				transitionToForm(new DiscoverForm(this));
			}

			private void transitionToForm(Form form) {
				if (!SwingUtilities.isEventDispatchThread()) {
					SwingUtilities.invokeLater(() -> transitionToForm(form));
					return;
				}
				appFrame.getContentPane().removeAll();
				appFrame.getContentPane().add(form.getPanel());
				appFrame.revalidate();
				appFrame.repaint();
			}
		};

		SignUpInForm suif = new SignUpInForm(aed);
	    appFrame.setLayout(new GridLayout());
	    appFrame.add(suif.getPanel());
	    appFrame.validate();
    }
}
