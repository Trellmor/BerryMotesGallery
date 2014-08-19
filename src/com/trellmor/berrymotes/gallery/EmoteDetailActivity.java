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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.trellmor.berrymotes.EmoteUtils;
import com.trellmor.berrymotes.provider.FileContract;
import com.trellmor.widget.LoadingDialog;
import com.trellmor.widget.ShareActionProvider;

/**
 * An activity representing a single Emote detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link EmoteGridActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link EmoteDetailFragment}.
 */
public class EmoteDetailActivity extends ActionBarActivity implements
		EmoteDetailFragment.Callbacks,
		ShareActionProvider.OnShareTargetSelectedListener {

	private SupportMenuItem mMenuShare;
	private ShareActionProvider mShareActionProvider;
	private Intent mShareIntent;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_emote_detail);

		// Show the Up button in the action bar.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putLong(EmoteDetailFragment.ARG_EMOTE_ID, getIntent()
					.getLongExtra(EmoteDetailFragment.ARG_EMOTE_ID, -1));
			EmoteDetailFragment fragment = new EmoteDetailFragment();
			fragment.setArguments(arguments);
			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
			transaction.add(R.id.emote_detail_container, fragment);
			transaction.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.detail, menu);

		// Locate MenuItem with ShareActionProvider
		mMenuShare = (SupportMenuItem) menu.findItem(R.id.action_share);

		mShareActionProvider = new ShareActionProvider(this);
		mShareActionProvider.setOnShareTargetSelectedListener(this);
		mMenuShare.setSupportActionProvider(mShareActionProvider);
		configureShare();
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			EmoteUtils.launchBerryMotesSettings(this);
			return true;
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpTo(this, new Intent(this,
					EmoteGridActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onEmoteLoaded(String name, boolean apng) {
		mShareIntent = new Intent(Intent.ACTION_SEND);
		mShareIntent.setType("image/*");
		mShareIntent.putExtra(Intent.EXTRA_STREAM,
				FileContract.getUriForEmote(name, apng));
		configureShare();
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
		PreloadImageTask task = new PreloadImageTask(this, new PreloadImageTask.Callback() {
			
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

	private void configureShare() {
		if (mMenuShare != null && mShareIntent != null) {
			mShareActionProvider.setShareIntent(mShareIntent);
			mMenuShare.setVisible(true);
		} else if (mMenuShare != null) {
			mMenuShare.setVisible(false);
		}
	}
}
