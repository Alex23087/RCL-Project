package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.PostViewShort;

import javax.swing.*;

public class ClientMain {

    public static void main(String[] args) {
	    JFrame appFrame = new JFrame();
	    appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    appFrame.setVisible(true);
	    appFrame.setSize(800, 600);
	    appFrame.setTitle("Winsome");
		Form.centerFrame(appFrame);

	    try {
		    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
		    e.printStackTrace();
	    }

	    AppEventDelegate aed = new AppEventDelegate() {
		    @Override
		    public int getFrameWidth() {
			    return appFrame.getWidth();
		    }

		    @Override
			public void onLoginComplete() {
				ServerProxy.instance.listFollowing(f -> {}, em -> {});
				onFeedTransition();
			}

		    @Override
		    public void onLogout() {
			    transitionToForm(new SignUpInForm(this));
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

		    @Override
		    public void onProfileTransition() {
			    transitionToForm(new ProfileForm(this));
		    }

		    @Override
		    public void onPostViewTransition(PostViewShort postViewShort, boolean comingFromBlog) {
			    transitionToForm(new PostViewForm(this, postViewShort, comingFromBlog));
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

		ServerProxy.instance.registerUnknownExceptionHandler(() -> {
			aed.onLogout();
			AlertForm.errorAlert("connection.lost");
		});

		aed.onLogout();
    }
}
