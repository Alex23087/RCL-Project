package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.PostViewShort;

import javax.swing.*;

/**
 * Class that handles the app initialisation
 */
public class ClientMain {

    public static void main(String[] args) {
		//Create and set up the main frame
	    JFrame appFrame = new JFrame();
	    appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    appFrame.setVisible(true);
	    appFrame.setSize(800, 600);
	    appFrame.setTitle("Winsome");
		Form.centerFrame(appFrame);

		//Set the look and feel to the system ones
	    try {
		    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
		    e.printStackTrace();
	    }

		//Create the delegate object to handle app transitions
	    AppEventDelegate aed = new AppEventDelegate() {
		    /**
		     * Returns the width of the main app frame
		     */
		    @Override
		    public int getFrameWidth() {
			    return appFrame.getWidth();
		    }

		    @Override
			public void onLoginComplete() {
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

		    /**
		     * Utility method to transition to a new app page
		     */
		    private void transitionToForm(Form form) {
				//Make sure this method is being executed on the main UI thread
				if (!SwingUtilities.isEventDispatchThread()) {
					SwingUtilities.invokeLater(() -> transitionToForm(form));
					return;
				}
				//Swap the frame contents
				appFrame.getContentPane().removeAll();
				appFrame.getContentPane().add(form.getPanel());
				appFrame.revalidate();
				appFrame.repaint();
			}
		};

		//Registering handlers for the network component
		ServerProxy.instance.registerUnknownExceptionHandler(() -> {
			aed.onLogout();
			AlertForm.errorAlert("connection.lost");
		});

	    ServerProxy.instance.registerFollowedNotificationHandler(userId -> {
		    ServerProxy.instance.getUsernameFromId(userId, AlertForm::followAlert, errorMessage -> {});
	    });

	    ServerProxy.instance.registerUnfollowedNotificationHandler(userId -> {
		    ServerProxy.instance.getUsernameFromId(userId, AlertForm::unfollowAlert, errorMessage -> {});
	    });

		//Call to onLogout to show the login page
		aed.onLogout();
    }
}
