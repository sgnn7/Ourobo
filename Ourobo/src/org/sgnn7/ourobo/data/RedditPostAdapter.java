package org.sgnn7.ourobo.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.sgnn7.ourobo.BrowserActivity;
import org.sgnn7.ourobo.R;
import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.eventing.IChangeEventListener;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.ImageCacheManager;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.content.Intent;
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

	private static final int DEFAULT_POST_COUNT = 15;
	private static final String DEFAULT_SORTING_TYPE = "new";

	private final List<RedditPost> redditPosts = new ArrayList<RedditPost>();

	private final SessionManager sessionManager;
	private final DownloadTaskFactory downloadTaskFactory;
	private final Activity activity;

	private final String baseUrl;
	private final String dataLocationUrl;
	private final String mobileBaseUrl;

	private final Drawable upvotedImage;
	private final Drawable downvotedImage;

	private DownloadTask currentDownloadTask = null;

	public RedditPostAdapter(Activity activity, SessionManager sessionManager, String baseUrl, String dataLocationUri,
			String mobileBaseUrl, IChangeEventListener finishedDownloadingListener) {
		this.activity = activity;
		this.downloadTaskFactory = createDownloadTaskFactory(finishedDownloadingListener);
		this.baseUrl = baseUrl;
		this.dataLocationUrl = dataLocationUri;
		this.mobileBaseUrl = mobileBaseUrl;
		this.sessionManager = sessionManager;

		this.upvotedImage = activity.getResources().getDrawable(R.drawable.upvoted);
		this.downvotedImage = activity.getResources().getDrawable(R.drawable.downvoted);
	}

	private DownloadTaskFactory createDownloadTaskFactory(final IChangeEventListener finishedDownloadingListener) {
		DownloadTaskFactory downloadTaskFactory = new DownloadTaskFactory() {
			@Override
			protected void onPostExecuteDownloadTask(List<RedditPost> results) {
				if (!results.isEmpty()) {
					addPosts(results);
				} else {
					Toast.makeText(activity, "Could not retrieve json data", Toast.LENGTH_LONG).show();
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
		int numberOfPosts;
		numberOfPosts = redditPosts.size();
		return numberOfPosts;
	}

	public Object getItem(int position) {
		return redditPosts.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		RedditPost redditPost = redditPosts.get(position);

		View redditPostHolder = null;
		// Does not work for some reason :(
		// if (convertView == null) {
		LogMe.i("Creating view: " + redditPost.getTitle());
		redditPostHolder = activity.getLayoutInflater().inflate(R.layout.post_layout, parent, false);
		// } else {
		// redditPostHolder = convertView;
		// }

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
		}
	}

	private void setPostHolderValues(int index, final RedditPost redditPost, View postHolder) {
		TextView titleView = (TextView) postHolder.findViewById(R.id.post_title);
		String title = sanitizeString(redditPost.getTitle().trim());
		titleView.setText(title);

		configureVotingButtons(postHolder, redditPost);

		final UrlFileType fileType = HttpUtils.getFileType(redditPost.getUrl());

		postHolder.setBackgroundDrawable(activity.getResources().getDrawable(
				getBackgroundIdBasedOnTypeAndIndex(fileType, index)));
		postHolder.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				stopAllDownloads();
				activity.startActivity(getBrowserViewIntent(redditPost, fileType));
			}
		});

		boolean isImageUrl = fileType.equals(UrlFileType.IMAGE);

		ViewSwitcher thumbnailHolder = (ViewSwitcher) postHolder.findViewById(R.id.post_thumbnail_holder);
		final ImageView thumbnail = (ImageView) thumbnailHolder.findViewById(R.id.post_thumbnail);

		AsyncThumbnailLoader thumbnailLazyLoader = new AsyncThumbnailLoader(postHolder, thumbnailHolder, thumbnail,
				baseUrl);
		if (isImageUrl) {
			thumbnailLazyLoader.loadImage(activity, redditPost.getUrl());
		} else {
			thumbnailLazyLoader.loadImage(activity, redditPost.getThumbnail());
		}

		RelativeLayout scoreHolder = (RelativeLayout) postHolder.findViewById(R.id.post_score_holder);
		scoreHolder.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				stopAllDownloads();

				String commentsUrl = mobileBaseUrl + redditPost.getPermalink();
				LogMe.e("Opening comments at: " + commentsUrl);

				Intent targetIntent = new Intent(activity, BrowserActivity.class);
				targetIntent.putExtra(BrowserActivity.URL_PARAMETER_KEY, commentsUrl);
				activity.startActivity(targetIntent);
			}
		});

		TextView scoreView = (TextView) scoreHolder.findViewById(R.id.post_score);
		scoreView.setText("" + redditPost.getScore());
	}

	private void configureVotingButtons(View postHolder, final RedditPost redditPost) {
		// disables default handler
		postHolder.findViewById(R.id.voting_buttons).setOnClickListener(null);

		View upvoteButton = postHolder.findViewById(R.id.upvote);
		View downvoteButton = postHolder.findViewById(R.id.downvote);

		upvoteButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				boolean isAuthenticated = sessionManager.authenticateUser();
				if (isAuthenticated) {
					voteOnStory(redditPost, true);
				}
			}
		});

		downvoteButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				boolean isAuthenticated = sessionManager.authenticateUser();
				if (isAuthenticated) {
					voteOnStory(redditPost, false);
				}
			}
		});

		if (redditPost.getLikes() != null) {
			ImageView upvote_icon = (ImageView) postHolder.findViewById(R.id.upvote_icon);
			ImageView downvote_icon = (ImageView) postHolder.findViewById(R.id.downvote_icon);

			if (redditPost.getLikes()) {
				upvote_icon.setImageDrawable(upvotedImage);
			} else {
				downvote_icon.setImageDrawable(downvotedImage);
			}
		}
	}

	private void voteOnStory(RedditPost redditPost, boolean isUpvote) {
		LogMe.e("Voting (" + isUpvote + ")...");
		new VotingTask(activity, sessionManager, baseUrl, redditPost, isUpvote).execute("someURL");
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

	private int getBackgroundIdBasedOnTypeAndIndex(UrlFileType fileType, int index) {
		boolean isOddPostNumber = index % 2 == 0;
		return isOddPostNumber ? R.drawable.gray_post_style : R.drawable.blue_post_style;
	}
}
