package it.unipi.rcl.project.client;

public interface AppEventDelegate {
	void onLoginComplete();
	void onBlogTransition();
	void onFeedTransition();
	void onBalanceTransition();
	void onDiscoverTransition();
}
