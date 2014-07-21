package com.trellmor.berrymotes.gallery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.trellmor.berrymotes.provider.EmotesContract;

public class EmoteExportService extends Service {
	private static final String TAG = EmoteExportService.class.getName();

	private static int NOTIFICATION_ID = 1;

	public static boolean isServiceRunning(Context context) {
		ActivityManager manager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (EmoteExportService.class.getName()
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flag, int startId) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this);
		builder.setContentTitle(getText(R.string.title_export));
		builder.setContentText(getText(R.string.export_running));
		builder.setSmallIcon(R.drawable.ic_stat_export);

		startForeground(NOTIFICATION_ID, builder.build());

		EmotesExportTask export = new EmotesExportTask(this, builder);
		export.execute();
		return START_NOT_STICKY;
	}

	private class EmotesExportTask extends AsyncTask<Void, Integer, Boolean> {
		private NotificationCompat.Builder mBuilder;
		private NotificationManager mNotifyManager;
		private ContentResolver mResolver;
		private Context mContext;

		public EmotesExportTask(Context context,
				NotificationCompat.Builder notification) {
			mContext = context;
			mResolver = context.getContentResolver();
			mNotifyManager = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			mBuilder = notification;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (isStorageAvailable()) {
				File emoteDir = new File(
						Environment.getExternalStorageDirectory(),
						"RedditEmotes");

				try {
					if (!emoteDir.exists())
						emoteDir.mkdirs();
					File nomedia = new File(emoteDir, ".nomedia");
					if (!nomedia.exists())
						nomedia.createNewFile();

					Cursor cursor = mResolver.query(
							EmotesContract.Emote.CONTENT_URI, new String[] {
									EmotesContract.Emote.COLUMN_NAME,
									EmotesContract.Emote.COLUMN_IMAGE },
							EmotesContract.Emote.COLUMN_INDEX + "=?",
							new String[] { "0" }, null);

					if (cursor.moveToFirst()) {
						final int POS_NAME = cursor
								.getColumnIndex(EmotesContract.Emote.COLUMN_NAME);
						final int POS_IMAGE = cursor
								.getColumnIndex(EmotesContract.Emote.COLUMN_IMAGE);
						final int PROGRESS_MAX = cursor.getCount() / 100;
						mBuilder.setProgress(PROGRESS_MAX, 0, false);
						mNotifyManager
								.notify(NOTIFICATION_ID, mBuilder.build());

						do {
							File destFile = new File(emoteDir,
									cursor.getString(POS_NAME) + ".png");
							if (!destFile.exists()) {
								FileChannel source = null;
								FileChannel destination = null;
								try {
									source = new FileInputStream(
											cursor.getString(POS_IMAGE))
											.getChannel();
									destination = new FileOutputStream(destFile)
											.getChannel();

									destination.transferFrom(source, 0,
											source.size());
								} finally {
									if (destination != null)
										destination.close();
									if (source != null)
										source.close();
								}
							}

							if (cursor.getPosition() % 100 == 0) {
								mBuilder.setProgress(PROGRESS_MAX,
										(cursor.getPosition() / 100), false);
								mNotifyManager.notify(NOTIFICATION_ID,
										mBuilder.build());
							}
						} while (cursor.moveToNext());
					}
					cursor.close();
					return true;
				} catch (IOException e) {
					Log.e(TAG, "Export failed", e);
				}
			} else {
				mBuilder.setContentText(mContext
						.getText(R.string.export_failed_nomedia));
				mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
			}

			return false;
		}

		private boolean isStorageAvailable() {
			String state = Environment.getExternalStorageState();
			return Environment.MEDIA_MOUNTED.equals(state);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				mBuilder.setContentText(mContext
						.getText(R.string.export_finished));
			} else {
				mBuilder.setContentText(mContext
						.getText(R.string.export_failed));
			}
			mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
			stopForeground(false);
			stopSelf();
		}
	}
}
