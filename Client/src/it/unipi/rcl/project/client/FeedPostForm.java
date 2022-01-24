package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.PostViewShort;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class FeedPostForm extends Form{
	private JLabel titleLabel;
	private JLabel usernameLabel;
	public JPanel panel;
	private JLabel rewinLabel;

	public FeedPostForm (AppEventDelegate aed, PostViewShort post, boolean isBlog){
		super(aed);
		titleLabel.setText(makeWrappedText(appEventDelegate.getFrameWidth() - 60, post.title));
		usernameLabel.setText(post.authorId + "");
		ServerProxy.instance.getUsernameFromId(post.authorId, username -> usernameLabel.setText(username), errorMessage -> {});

		panel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				appEventDelegate.onPostViewTransition(post, isBlog);
			}

			@Override
			public void mousePressed(MouseEvent mouseEvent) {

			}

			@Override
			public void mouseReleased(MouseEvent mouseEvent) {

			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {

			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {

			}
		});

		if(post.rewinnerId != -1){
			ServerProxy.instance.getUsernameFromId(post.rewinnerId, username -> {
				rewinLabel.setText("(" + resourceBundle.getString("rewinned.by") + " " + username + ")");
			}, errorMessage -> {
				rewinLabel.setText("(" + resourceBundle.getString("rewinned.by") + " " + post.rewinnerId + ")");
			});
		}
	}

	@Override
	public JPanel getPanel() {
		return panel;
	}
}
