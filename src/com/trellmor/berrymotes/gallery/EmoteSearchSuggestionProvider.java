package com.trellmor.berrymotes.gallery;

import android.content.SearchRecentSuggestionsProvider;

public class EmoteSearchSuggestionProvider extends
		SearchRecentSuggestionsProvider {
	public static final String AUTHORITY = "com.trellmor.berrymotes.gallery.EmoteSearchSuggestionProvider";
	public final static int MODE = DATABASE_MODE_QUERIES;
	
	public EmoteSearchSuggestionProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}
}
