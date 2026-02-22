package org.sgnn7.ourobo.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.sgnn7.ourobo.BrowserActivity;
import org.sgnn7.ourobo.R;
import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.eventing.IChangeEventListener;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.ImageCacheManager;
import org.sgnn7.ourobo.util.LogMe;

import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class RedditPostAdapter extends BaseAdapter {
	private static final String PARAMETER_SEPARATOR = "&";
	private static final String DEFAULT_SORTING_TYPE = "new";

	private static final int DEFAULT_POST_COUNT = 15;

	private final List<RedditPost> redditPosts = new ArrayList<RedditPost>();

	private final DownloadTaskFactory downloadTaskFactory;
	private final SessionManager sessionManager;
	private final Resources resources;
	private final Activity activity;

	private final String baseUrl;
	private final String dataLocationUrl;
	private final String mobileBaseUrl;

	private final Drawable upvotedImage;
	private final Drawable downvotedImage;

	private final IChangeEventListener finishedDownloadingListener;
	private DownloadTask currentDownloadTask = null;

	public RedditPostAdapter(Activity activity, SessionManager sessionManager, String baseUrl, String dataLocationUri,
			String mobileBaseUrl, IChangeEventListener finishedDownloadingListener) {
		this.activity = activity;
		resources = activity.getResources();
		this.finishedDownloadingListener = finishedDownloadingListener;
		this.downloadTaskFactory = createDownloadTaskFactory(finishedDownloadingListener);
		this.baseUrl = baseUrl;
		this.dataLocationUrl = dataLocationUri;
		this.mobileBaseUrl = mobileBaseUrl;
		this.sessionManager = sessionManager;

		this.upvotedImage = ContextCompat.getDrawable(activity, R.drawable.upvoted);
		this.downvotedImage = ContextCompat.getDrawable(activity, R.drawable.downvoted);
	}

	private DownloadTaskFactory createDownloadTaskFactory(final IChangeEventListener finishedDownloadingListener) {
		DownloadTaskFactory downloadTaskFactory = new DownloadTaskFactory() {
			@Override
			protected void onPostExecuteDownloadTask(List<RedditPost> results) {
				if (!results.isEmpty()) {
					addPosts(results);
				} else {
					Toast.makeText(activity, "Could not retrieve data", Toast.LENGTH_LONG).show();
					// Could cause DDOS
					// this.newDownloadTask();
				}
				finishedDownloadingListener.handle();
			}

			@Override
			protected SessionManager getSessionManager() {
				return sessionManager;
			}
		};

		return downloadTaskFactory;
	}

	public int getCount() {
		return redditPosts.size();
	}

	public Object getItem(int position) {
		return redditPosts.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		RedditPost redditPost = redditPosts.get(position);

		View redditPostHolder = convertView;
		if (redditPostHolder == null) {
			LogMe.d("Creating view: " + redditPost.getTitle());
			redditPostHolder = activity.getLayoutInflater().inflate(R.layout.post_layout, parent, false);
		}

		setPostHolderValues(position, redditPost, redditPostHolder);
		redditPostHolder.invalidate();
		return redditPostHolder;
	}

	public void addPosts(final List<RedditPost> newRedditPosts) {
		Set<String> viewedLinks = new HashSet<String>();
		for (RedditPost oldRedditPost : redditPosts) {
			viewedLinks.add(oldRedditPost.getName());
		}

		for (RedditPost newRedditPost : newRedditPosts) {
			if (!viewedLinks.contains(newRedditPost.getName())) {
				redditPosts.add(newRedditPost);
			} else {
				LogMe.e("Removing duplicate post: " + newRedditPost.getName());
			}
		}
		viewedLinks = null; // GC optimization

		LogMe.e("Posts set. Add Size: " + newRedditPosts.size() + ". Total: " + redditPosts.size());
		notifyDataSetChanged();
	}

	public void refreshViews() {
		LogMe.e("Clearing view");
		redditPosts.clear();
		notifyDataSetChanged();

		downloadMoreContent();
	}

	public void downloadMoreContent() {
		LogMe.i("Loading more content...");

		if (currentDownloadTask != null) {
			stopDownloadTask();
		}

		currentDownloadTask = downloadTaskFactory.newDownloadTask();
		currentDownloadTask.addTaskDoneListener(new IChangeEventListener() {
			public void handle() {
				currentDownloadTask = null;
			}
		});
		currentDownloadTask.execute(dataLocationUrl, getParameterString());
	}

	public String getLastPostId() {
		return redditPosts.get(redditPosts.size() - 1).getName();
	}

	public void stopAllDownloads() {
		ImageCacheManager.stopDownloads();
		stopDownloadTask();
	}

	private void stopDownloadTask() {
		if (currentDownloadTask != null && !currentDownloadTask.isCancelled()) {
			currentDownloadTask.cancel(true);
			currentDownloadTask = null;
			finishedDownloadingListener.handle();
		}
	}

	private void setPostHolderValues(int index, final RedditPost redditPost, View postHolder) {

		TextView titleView = (TextView) postHolder.findViewById(R.id.post_title);
		String title = sanitizeString(redditPost.getTitle().trim());
		titleView.setText(title);

		configureVotingButtons(postHolder, redditPost);

		final UrlFileType fileType = HttpUtils.getFileType(redditPost.getUrl());

		postHolder.setBackground(ContextCompat.getDrawable(activity,
				index % 2 == 0 ? R.drawable.gray_post_style : R.drawable.blue_post_style));
		postHolder.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				stopAllDownloads();
				activity.startActivity(getBrowserViewIntent(redditPost, fileType));
			}
		});

		ViewSwitcher thumbnailHolder = (ViewSwitcher) postHolder.findViewById(R.id.post_thumbnail_holder);
		final ImageView thumbnail = (ImageView) thumbnailHolder.findViewById(R.id.post_thumbnail);

		thumbnail.setImageDrawable(null);
		thumbnail.setTag(redditPost.getName());
		thumbnailHolder.setDisplayedChild(0);
		RelativeLayout.LayoutParams thumbnailHolderLayoutParams = new RelativeLayout.LayoutParams(
				thumbnailHolder.getLayoutParams());
		thumbnailHolderLayoutParams.width = (int) resources.getDimension(R.dimen.thumbnail_holder_width);
		thumbnailHolderLayoutParams.height = (int) resources.getDimension(R.dimen.thumbnail_holder_height);
		thumbnailHolderLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		thumbnailHolder.setLayoutParams(thumbnailHolderLayoutParams);
		thumbnailHolder.forceLayout();
		thumbnailHolder.invalidate();

		AsyncThumbnailLoader thumbnailLazyLoader = new AsyncThumbnailLoader(postHolder, thumbnailHolder, thumbnail,
				baseUrl, redditPost.getName());

		// TODO - This is too slow
		// boolean isImageUrl = fileType.equals(UrlFileType.IMAGE);
		// if (isImageUrl) {
		// thumbnailLazyLoader.loadImage(activity, redditPost.getUrl());
		// } else {
		thumbnailLazyLoader.loadImage(activity, redditPost.getThumbnail());
		// }

		TextView scoreView = (TextView) postHolder.findViewById(R.id.post_score);
		scoreView.setText(formatScore(redditPost.getScore()));

		TextView commentCountView = (TextView) postHolder.findViewById(R.id.post_comment_count);
		commentCountView.setText(formatScore(redditPost.getNum_comments()));
	}

	private void configureVotingButtons(final View postHolder, final RedditPost redditPost) {
		// disables default handler
		postHolder.findViewById(R.id.voting_buttons).setOnClickListener(null);

		final ImageView upvoteIcon = (ImageView) postHolder.findViewById(R.id.upvote_icon);
		final ImageView downvoteIcon = (ImageView) postHolder.findViewById(R.id.downvote_icon);
		final TextView scoreView = (TextView) postHolder.findViewById(R.id.post_score);

		// Reset icons for recycled views
		upvoteIcon.setImageResource(R.drawable.upvote);
		downvoteIcon.setImageResource(R.drawable.downvote);

		if (redditPost.getLikes() != null) {
			if (redditPost.getLikes()) {
				upvoteIcon.setImageDrawable(upvotedImage);
			} else {
				downvoteIcon.setImageDrawable(downvotedImage);
			}
		}

		VotingTask.VoteResultListener voteListener = new VotingTask.VoteResultListener() {
			public void onVoteSuccess(boolean isUpvote) {
				if (isUpvote) {
					redditPost.setLikes(true);
					redditPost.setScore(redditPost.getScore() + 1);
					upvoteIcon.setImageDrawable(upvotedImage);
					downvoteIcon.setImageResource(R.drawable.downvote);
				} else {
					redditPost.setLikes(false);
					redditPost.setScore(redditPost.getScore() - 1);
					downvoteIcon.setImageDrawable(downvotedImage);
					upvoteIcon.setImageResource(R.drawable.upvote);
				}
				scoreView.setText(formatScore(redditPost.getScore()));
			}
		};

		final VotingTask.VoteResultListener listener = voteListener;

		postHolder.findViewById(R.id.upvote).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				voteOnStory(redditPost, true, listener);
			}
		});

		postHolder.findViewById(R.id.downvote).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				voteOnStory(redditPost, false, listener);
			}
		});
	}

	private void voteOnStory(RedditPost redditPost, boolean isUpvote, VotingTask.VoteResultListener listener) {
		LogMe.e("Voting (" + isUpvote + ")...");
		new VotingTask(activity, sessionManager, baseUrl, redditPost, isUpvote, listener).execute();
	}

	private static String formatScore(int score) {
		int abs = Math.abs(score);
		String sign = score < 0 ? "-" : "";
		if (abs >= 1_000_000) {
			return sign + (abs / 1_000_000) + "m";
		} else if (abs >= 1_000) {
			return sign + (abs / 1_000) + "k";
		}
		return String.valueOf(score);
	}

	private String sanitizeString(String text) {
		String sanitizedText = "NULL";
		if (text != null && text.length() != 0) {
			sanitizedText = StringEscapeUtils.unescapeHtml4(text);
		}
		return sanitizedText;
	}

	private String getParameterString() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("limit", "" + DEFAULT_POST_COUNT);
		parameters.put("sort", DEFAULT_SORTING_TYPE);
		parameters.put("raw_json", "1");

		if (getCount() != 0) {
			parameters.put("after", getLastPostId());
		}

		String parameterString = "";
		for (String parameterKey : parameters.keySet()) {
			parameterString += PARAMETER_SEPARATOR + parameterKey + "=" + parameters.get(parameterKey);
		}
		parameterString = "?" + parameterString.substring(1);

		LogMe.d("Parameters: " + parameterString);

		return parameterString;
	}

	private Intent getBrowserViewIntent(final RedditPost redditPost, final UrlFileType fileType) {
		Intent targetIntent = new Intent(activity, BrowserActivity.class);
		targetIntent.putExtra(BrowserActivity.URL_PARAMETER_KEY, injectRedditMobileUrls(redditPost.getUrl()));
		return targetIntent;
	}

	private String injectRedditMobileUrls(String url) {
		return url.replace(baseUrl, mobileBaseUrl);
	}
}
