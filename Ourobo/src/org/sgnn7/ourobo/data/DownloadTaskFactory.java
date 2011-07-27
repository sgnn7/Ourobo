package org.sgnn7.ourobo.data;

import java.util.List;

public abstract class DownloadTaskFactory {
	public DownloadTask newDownloadTask() {
		return new DownloadTask() {
			@Override
			protected void onPostExecute(List<RedditPost> results) {
				onPostExecuteDownloadTask(results);
			}
		};
	}

	protected abstract void onPostExecuteDownloadTask(List<RedditPost> results);
}
