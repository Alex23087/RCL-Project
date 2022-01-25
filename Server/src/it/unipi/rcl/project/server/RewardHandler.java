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

public class RewardHandler implements Runnable{
	private final int rewardInterval;
	private final ScheduledExecutorService executorService;
	private long calculationStartTimestamp = 0;
	private static final double e = Math.exp(1);
	private static final double authorPercentage = (double) ServerData.conf.get(ConfigurationParameter.AUTHOR_REWARD);
	private DatagramSocket socket;
	private DatagramPacket updateNotificationDatagram;

	public RewardHandler(int rewardInterval) throws UnknownHostException, SocketException {
		this.rewardInterval = rewardInterval;
		executorService = Executors.newSingleThreadScheduledExecutor();

		InetAddress address = InetAddress.getByName((String) ServerData.conf.get(ConfigurationParameter.MULTICAST));
		socket = new DatagramSocket();
		byte[] data = new byte[64];
		updateNotificationDatagram = new DatagramPacket(data, data.length, address, (int) ServerData.conf.get(ConfigurationParameter.MCASTPORT));
	}

	@Override
	public void run() {
		System.out.println("Rewards calculation started");
		calculationStartTimestamp = System.currentTimeMillis();
		Map<Integer, Double> rewardsPerUser = new HashMap<>();
		ServerData.posts.forEach(post -> {
			if((post.comments == null || post.comments.size() < 1) && (post.votes == null || post.votes.size() < 1)){
				return;
			}
			Pair<Double, Set<Integer>> calculationResult = calculateRewardForPost(post);
			double authorQuota = calculationResult.first / 100d * authorPercentage;
			double curatorQuota = (calculationResult.first - authorQuota) / calculationResult.second.size();

			double authorAmount = rewardsPerUser.getOrDefault(post.authorId, 0d);
			rewardsPerUser.put(post.authorId, authorAmount + authorQuota);

			calculationResult.second.forEach(userId -> {
				double curatorAmount = rewardsPerUser.getOrDefault(userId, 0d);
				rewardsPerUser.put(userId, curatorAmount + curatorQuota);
			});
		});

		rewardsPerUser.forEach((userId, amount) -> {
			User u = ServerData.getUser(userId);
			u.transactions.add(new Transaction(amount));
			u.balance += amount;
		});

		try {
			socket.send(updateNotificationDatagram);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		System.out.println("Rewards calculated");
	}

	public void start() {
		executorService.scheduleAtFixedRate(this, rewardInterval, rewardInterval, TimeUnit.SECONDS);
	}

	public void stop() {
		executorService.shutdown();
		try {
			executorService.awaitTermination(20, TimeUnit.SECONDS);
		} catch (InterruptedException e) {}
	}

	private Pair<Double, Set<Integer>> calculateRewardForPost(Post p){
		p.rewardIterations++;
		List<Vote> newPeopleLikes = p.votes.stream().filter(vote -> vote.timestamp >= p.lastRewardCalculation && vote.timestamp < calculationStartTimestamp).collect(Collectors.toList());
		List<Vote> oldPeopleLikes = p.votes.stream().filter(vote -> vote.timestamp < p.lastRewardCalculation).collect(Collectors.toList());
		List<Vote> newPeopleLikesList = newPeopleLikes.stream().filter(vote1 -> oldPeopleLikes.stream().noneMatch(vote2 -> vote1.voterId == vote2.voterId)).collect(Collectors.toList());
		int newPeopleLikesCount = newPeopleLikesList.stream().reduce(0, (counter, vote) -> counter + (vote.upvote ? 1 : -1), Integer::sum);

		List<Comment> newPeopleComments = p.comments.stream().filter(comment -> comment.timestamp >= p.lastRewardCalculation && comment.timestamp < calculationStartTimestamp).collect(Collectors.toList());
		List<Comment> oldPeopleComments = p.comments.stream().filter(comment -> comment.timestamp < p.lastRewardCalculation).collect(Collectors.toList());
		newPeopleComments = newPeopleComments.stream().filter(comment1 -> oldPeopleComments.stream().noneMatch(comment2 -> comment1.commenterId == comment2.commenterId)).collect(Collectors.toList());

		Map<Integer, Integer> numberOfCommentsPerUser = new HashMap<>();
		Iterator<Comment> commentIterator = newPeopleComments.iterator();
		while(commentIterator.hasNext()){
			Comment c = commentIterator.next();
			int oldCount = numberOfCommentsPerUser.getOrDefault(c.commenterId, 0);
			numberOfCommentsPerUser.put(c.commenterId, oldCount + 1);
		}
		double commentPointsTotal = numberOfCommentsPerUser.values().stream().reduce(0d, (counter, commentCount) -> counter + (2d / (1 + Math.pow(e, -(commentCount - 1)))), Double::sum);

		double score = Math.log(Math.max(newPeopleLikesCount, 0) + 1) + Math.log(commentPointsTotal + 1) / p.rewardIterations;
		p.lastRewardCalculation = calculationStartTimestamp;

		Set<Integer> curators = new HashSet<>(numberOfCommentsPerUser.keySet());
		for(Vote v: newPeopleLikesList){
			curators.add(v.voterId);
		}
		return new Pair<>(score, curators);
	}
}
