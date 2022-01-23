package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.PostViewShort;

import javax.swing.*;

public interface AppEventDelegate {
	int getFrameWidth();
	void onLoginComplete();
	void onBlogTransition();
	void onFeedTransition();
	void onBalanceTransition();
	void onDiscoverTransition();
	void onPostViewTransition(PostViewShort postViewShort, boolean comingFromBlog);
}
