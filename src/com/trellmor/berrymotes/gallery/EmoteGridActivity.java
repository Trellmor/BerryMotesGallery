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

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.trellmor.berrymotes.EmoteUtils;
import com.trellmor.berrymotes.provider.FileContract;
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
	private static final String TAG = EmoteGridActivity.class.getName();

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

		if (findViewById(R.id.emote_detail_container) != null) {
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
				EmoteUtils.BERRYMOTES_VERSION_1_2_0)) {
			EmoteUtils.showInstallDialog(this);
		}
	}

	/**
	 * Callback method from {@link EmoteGridFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(long id) {
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
			Intent detailIntent = new Intent(this, EmoteDetailActivity.class);
			detailIntent.putExtra(EmoteDetailFragment.ARG_EMOTE_ID, id);
			startActivity(detailIntent);
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

		final MenuItem fMenuSearch = menu.findItem(R.id.search);
		final SearchView fSearch = (SearchView) fMenuSearch.getActionView();

		fSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				mGridFragment.searchEmotes(query);
				fSearch.clearFocus();
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				mGridFragment.searchEmotes(newText);
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

		Dialog dialog = new Dialog(this, R.style.Theme_Dialog_NoActionBar);
		dialog.setContentView(R.layout.dialog_loading);
		dialog.setCancelable(false);
		TextView message = (TextView) dialog.findViewById(R.id.text_message);
		message.setText(R.string.loading_image);
		dialog.show();

		PreloadImageTask task = new PreloadImageTask(this, intent, dialog);
		task.execute(uri);
		return true; // Handled
	}
}
