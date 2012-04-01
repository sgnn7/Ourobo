package org.sgnn7.ourobo.data;

import java.util.List;

import org.sgnn7.ourobo.authentication.SessionManager;

public abstract class DownloadTaskFactory {
	public DownloadTask newDownloadTask() {
		return new DownloadTask(getSessionManager()) {
			@Override
			protected void onDownloadComplete(List<RedditPost> results) {
				onPostExecuteDownloadTask(results);
			}
		};
	}

	protected abstract void onPostExecuteDownloadTask(List<RedditPost> results);

	protected abstract SessionManager getSessionManager();
}
