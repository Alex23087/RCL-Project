package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.*;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that handles the rewards calculations
 */
public class RewardHandler implements Runnable{
	private static final double e = Math.exp(1);
	private static final double authorPercentage = (double) ServerData.conf.get(ConfigurationParameter.AUTHOR_REWARD);

	private final ScheduledExecutorService executorService;
	private final int rewardInterval;
	private long calculationStartTimestamp = 0;

	/**
	 * UDP fields
	 */
	private final DatagramSocket socket;
	private final DatagramPacket updateNotificationDatagram;

	public RewardHandler(int rewardInterval) throws UnknownHostException, SocketException {
		this.rewardInterval = rewardInterval;

		executorService = Executors.newSingleThreadScheduledExecutor();

		/*
		 * Initialise the UDP variables
		 */
		InetAddress address = InetAddress.getByName((String) ServerData.conf.get(ConfigurationParameter.MULTICAST));
		socket = new DatagramSocket();
		byte[] data = new byte[64];
		updateNotificationDatagram = new DatagramPacket(data, data.length, address, (int) ServerData.conf.get(ConfigurationParameter.MCASTPORT));
	}

	@Override
	public void run() {
		System.out.println("Rewards calculation started");
		//Remember the time the current iteration has started
		calculationStartTimestamp = System.currentTimeMillis();
		Map<Integer, Double> rewardsPerUser = new HashMap<>();

		ServerData.posts.forEach(post -> {
			//If the current post has no comments and no votes, skip it
			if((post.comments == null || post.comments.size() < 1) && (post.votes == null || post.votes.size() < 1)){
				return;
			}

			//Calculate the full reward
			Pair<Double, Set<Integer>> calculationResult = calculateRewardForPost(post);
			if(calculationResult.first == 0d){
				return;
			}

			/*
			 * Split the reward between author and curators.
			 * N.b. curatorQuota is the quota per curator, not the cumulative amount.
			 */
			double authorQuota = calculationResult.first / 100d * authorPercentage;
			double curatorQuota = (calculationResult.first - authorQuota) / calculationResult.second.size();

			//Get the current author total reward and update it
			double authorTotalReward = rewardsPerUser.getOrDefault(post.authorId, 0d);
			rewardsPerUser.put(post.authorId, authorTotalReward + authorQuota);

			//For each curator, get the current total reward and update it
			calculationResult.second.forEach(userId -> {
				double curatorTotalReward = rewardsPerUser.getOrDefault(userId, 0d);
				rewardsPerUser.put(userId, curatorTotalReward + curatorQuota);
			});
		});

		//For each user that got a reward, create a transaction with the reward and update the total balance.
		rewardsPerUser.forEach((userId, amount) -> {
			User u = ServerData.getUser(userId);
			u.transactions.add(0, new Transaction(amount));
			u.balance += amount;
		});

		//Send the balance update notification
		try {
			socket.send(updateNotificationDatagram);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		System.out.println("Rewards calculated");
	}

	/**
	 * Starts the handler by scheduling it to execute at fixed intervals
	 */
	public void start() {
		executorService.scheduleAtFixedRate(this, rewardInterval, rewardInterval, TimeUnit.SECONDS);
	}

	/**
	 * Stops the handler executor service
	 */
	public void stop() {
		executorService.shutdown();
		try {
			executorService.awaitTermination(20, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {}
	}

	/**
	 * Calculates the reward for a single post
	 */
	private Pair<Double, Set<Integer>> calculateRewardForPost(Post post){
		//Update the number of iterations that have been executed for the post
		post.rewardIterations++;

		//Get a list of the new likes added since the last iteration
		List<Vote> newPeopleLikes = post.votes.stream()
				.filter(vote -> vote.timestamp >= post.lastRewardCalculation && vote.timestamp < calculationStartTimestamp)
				.collect(Collectors.toList());

		//Sum all the votes, increasing the count if the vote is an upvote, decreasing it if it's a downvote
		int newPeopleLikesCount = newPeopleLikes.stream().reduce(0, (counter, vote) -> counter + (vote.upvote ? 1 : -1), Integer::sum);

		//Get a list of the new comments added since the last iteration
		List<Comment> newPeopleComments = post.comments.stream()
				.filter(comment -> comment.timestamp >= post.lastRewardCalculation && comment.timestamp < calculationStartTimestamp)
				.collect(Collectors.toList());

		/*
		 * Create a map containing the number of new comments users have added
		 */
		Map<Integer, Integer> numberOfCommentsPerUser = new HashMap<>();
		for (Comment comment : newPeopleComments) {
			int oldCount = numberOfCommentsPerUser.getOrDefault(comment.commenterId, 0);
			numberOfCommentsPerUser.put(comment.commenterId, oldCount + 1);
		}

		//Calculate the sum of comment points according to the formula specified
		double commentPointsTotal = numberOfCommentsPerUser.values().stream()
				.reduce(0d, (counter, commentCount) -> counter + (2d / (1 + Math.pow(e, -(commentCount - 1)))), Double::sum);

		//Calculate the total reward for the post, according to the formula
		double rewardAmount = Math.log(Math.max(newPeopleLikesCount, 0) + 1) + Math.log(commentPointsTotal + 1) / post.rewardIterations;

		//Update the last reward calculation timestamp for the post, so the next iteration can start from there
		post.lastRewardCalculation = calculationStartTimestamp;

		//Create the set of curators to reward
		Set<Integer> curators = new HashSet<>(numberOfCommentsPerUser.keySet());
		for(Vote v: newPeopleLikes){
			if(v.upvote) { //Only add people who upvoted
				curators.add(v.voterId);
			}
		}
		return new Pair<>(rewardAmount, curators);
	}
}
