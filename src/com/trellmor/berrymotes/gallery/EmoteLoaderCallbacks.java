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

import com.trellmor.berrymotes.provider.EmotesContract;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;

public class EmoteLoaderCallbacks implements LoaderCallbacks<Cursor> {
	private Context mContext;
	private CursorAdapter mAdapter;
	
	private static final String[] PROJECTION = new String[] {
			EmotesContract.Emote._ID,
			EmotesContract.Emote.COLUMN_NAME,
			EmotesContract.Emote.COLUMN_IMAGE,
			EmotesContract.Emote.COLUMN_APNG };

	public static final String ARG_QUERY = "query";
	
	public EmoteLoaderCallbacks(Context context, CursorAdapter adapter) {
		mContext = context;
		mAdapter = adapter;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String selection = EmotesContract.Emote.COLUMN_INDEX + "=0";
		String[] selectionArgs = null;
		if (args != null && args.containsKey(ARG_QUERY)) {
			selection += " AND " + EmotesContract.Emote.COLUMN_NAME + " LIKE ?";
			selectionArgs = new String[] { "%" + args.getString(ARG_QUERY) + "%" };
		}
		
		return new CursorLoader(mContext, EmotesContract.Emote.CONTENT_URI,
				PROJECTION, selection, selectionArgs,
				EmotesContract.Emote.COLUMN_NAME + " ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

}
