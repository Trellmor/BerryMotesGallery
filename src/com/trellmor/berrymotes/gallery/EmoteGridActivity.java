/*
 * BerryMotes Gallery 
 * Copyright (C) 2014 Daniel Triendl <trellmor@trellmor.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.trellmor.berrymotes.gallery;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.trellmor.berrymotes.EmoteUtils;
import com.trellmor.berrymotes.provider.EmotesContract;
import com.trellmor.berrymotes.provider.FileContract;
import com.trellmor.widget.LoadingDialog;
import com.trellmor.widget.ShareActionProvider;

/**
 * An activity representing a list of Emotes. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link EmoteDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link EmoteGridFragment} and the item details (if present) is a
 * {@link EmoteDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link EmoteGridFragment.Callbacks} interface to listen for item selections.
 */
public class EmoteGridActivity extends ActionBarActivity implements
		EmoteGridFragment.Callbacks, EmoteDetailFragment.Callbacks,
		ShareActionProvider.OnShareTargetSelectedListener {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;
	private SupportMenuItem mMenuShare;
	private ShareActionProvider mShareActionProvider;
	private EmoteGridFragment mGridFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_emote_grid);

		mGridFragment = (EmoteGridFragment) getSupportFragmentManager()
				.findFragmentById(R.id.emote_grid);

		View emoteDetailContainer = findViewById(R.id.emote_detail_container);
		if (emoteDetailContainer != null) {
			emoteDetailContainer.setVisibility((isPickIntent()) ? View.GONE
					: View.VISIBLE);

			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			mGridFragment.setActivateOnItemClick(true);
		}

		if (!EmoteUtils.isBerryMotesInstalled(this,
				EmoteUtils.BERRYMOTES_VERSION_1_3_0)) {
			EmoteUtils.showInstallDialog(this);
		}
	}

	/**
	 * Callback method from {@link EmoteGridFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(long id) {
		if (isPickIntent()) {
			pickEmote(id);
		} else {
			if (mTwoPane) {
				// In two-pane mode, show the detail view in this activity by
				// adding or replacing the detail fragment using a
				// fragment transaction.
				Bundle arguments = new Bundle();
				arguments.putLong(EmoteDetailFragment.ARG_EMOTE_ID, id);
				EmoteDetailFragment fragment = new EmoteDetailFragment();
				fragment.setArguments(arguments);
				FragmentTransaction transaction = getSupportFragmentManager()
						.beginTransaction();
				transaction.replace(R.id.emote_detail_container, fragment);
				transaction.addToBackStack(null);
				transaction.commit();
				mMenuShare.setVisible(false);
			} else {
				// In single-pane mode, simply start the detail activity
				// for the selected item ID.
				Intent detailIntent = new Intent(this,
						EmoteDetailActivity.class);
				detailIntent.putExtra(EmoteDetailFragment.ARG_EMOTE_ID, id);
				startActivity(detailIntent);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.grid, menu);

		if (mTwoPane) {
			// Locate MenuItem with ShareActionProvider
			mMenuShare = (SupportMenuItem) menu.findItem(R.id.action_share);

			mShareActionProvider = new ShareActionProvider(this);
			mShareActionProvider.setOnShareTargetSelectedListener(this);
			mMenuShare.setSupportActionProvider(mShareActionProvider);
		}

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		final SupportMenuItem menuSearch = (SupportMenuItem) menu
				.findItem(R.id.search);
		final SearchView fSearchView = (SearchView) menuSearch.getActionView();

		fSearchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));
		fSearchView.setIconifiedByDefault(false);

		fSearchView
				.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

					@Override
					public boolean onQueryTextSubmit(String query) {
						mGridFragment.searchEmotes(query);
						SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
								EmoteGridActivity.this,
								EmoteSearchSuggestionProvider.AUTHORITY,
								EmoteSearchSuggestionProvider.MODE);
						suggestions.saveRecentQuery(query, null);
						fSearchView.clearFocus();
						return true;
					}

					@Override
					public boolean onQueryTextChange(String newText) {
						mGridFragment.searchEmotes(newText);
						return true;
					}
				});

		fSearchView
				.setOnSuggestionListener(new SearchView.OnSuggestionListener() {

					@Override
					public boolean onSuggestionSelect(int position) {
						return false;
					}

					@Override
					public boolean onSuggestionClick(int position) {
						Cursor cursor = (Cursor) fSearchView
								.getSuggestionsAdapter().getItem(position);
						fSearchView.setQuery(
								cursor.getString(cursor
										.getColumnIndex(SearchManager.SUGGEST_COLUMN_QUERY)),
								false);
						return true;
					}
				});

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			EmoteUtils.launchBerryMotesSettings(this);
			return true;
		case R.id.action_export:
			new AlertDialog.Builder(this)
					.setTitle(R.string.title_export)
					.setMessage(R.string.text_export)
					.setPositiveButton(R.string.export,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									if (!EmoteExportService
											.isServiceRunning(EmoteGridActivity.this)) {
										startService(new Intent(
												EmoteGridActivity.this,
												EmoteExportService.class));
									}
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onEmoteLoaded(String name, boolean apng) {
		if (mMenuShare != null) {
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("image/*");
			shareIntent.putExtra(Intent.EXTRA_STREAM,
					FileContract.getUriForEmote(name, apng));

			mShareActionProvider.setShareIntent(shareIntent);
			mMenuShare.setVisible(true);
		}
	}

	@Override
	public boolean onShareTargetSelected(ShareActionProvider source,
			Intent intent) {
		Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

		final LoadingDialog dialog = new LoadingDialog(this);
		dialog.setCancelable(false);
		dialog.setText(R.string.loading_image);
		dialog.show();

		final Intent copyIntent = new Intent(intent);
		PreloadImageTask task = new PreloadImageTask(this,
				new PreloadImageTask.Callback() {

					@Override
					public void onLoaded(boolean result) {
						dialog.dismiss();

						if (result && copyIntent != null) {
							startActivity(copyIntent);
						}

					}
				});
		task.execute(uri);
		return true; // Handled
	}

	private boolean isPickIntent() {
		String action = getIntent().getAction();
		return (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT
				.equals(action));
	}

	private void pickEmote(long id) {
		Cursor c = getContentResolver().query(
				EmotesContract.Emote.CONTENT_URI,
				new String[] { EmotesContract.Emote.COLUMN_NAME,
						EmotesContract.Emote.COLUMN_APNG },
				EmotesContract.Emote._ID + "=?",
				new String[] { Long.toString(id) }, null);

		if (c.moveToFirst()) {
			String name = c.getString(c
					.getColumnIndex(EmotesContract.Emote.COLUMN_NAME));
			boolean apng = c.getInt(c
					.getColumnIndex(EmotesContract.Emote.COLUMN_APNG)) == 1;

			Uri uri = FileContract.getUriForEmote(name, apng);
			final Intent intent = new Intent();
			intent.putExtra(Intent.EXTRA_STREAM, uri);

			final LoadingDialog dialog = new LoadingDialog(this);
			dialog.setCancelable(false);
			dialog.setText(R.string.loading_image);
			dialog.show();

			PreloadImageTask task = new PreloadImageTask(this,
					new PreloadImageTask.Callback() {

						@Override
						public void onLoaded(boolean result) {
							dialog.dismiss();

							if (result) {
								setResult(RESULT_OK, intent);
								finish();
							}

						}
					});
			task.execute(uri);
		}
		c.close();
	}
}
