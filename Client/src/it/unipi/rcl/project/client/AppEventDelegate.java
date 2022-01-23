package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.PostViewShort;

public interface AppEventDelegate {
	int getFrameWidth();
	void onLoginComplete();
	void onLogout();
	void onBlogTransition();
	void onFeedTransition();
	void onBalanceTransition();
	void onDiscoverTransition();
	void onProfileTransition();
	void onPostViewTransition(PostViewShort postViewShort, boolean comingFromBlog);
}
