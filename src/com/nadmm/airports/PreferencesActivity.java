/*
 * Airports for Android
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity 
            implements OnSharedPreferenceChangeListener {

    public static final String KEY_STARTUP_CHECK_EXPIRED_DATA = "startup_check_expired_data";
    public static final String KEY_STARTUP_SHOW_ACTIVITY = "startup_show_activity";
    public static final String KEY_SEARCH_LIMITED_RESULT = "search_limited_result";
    public static final String KEY_SEARCH_RESULT_LIMIT = "search_result_limit";
    public static final String KEY_SEARCH_AIRPORT_TYPES = "search_airport_types";
    public static final String KEY_LOCATION_USE_GPS = "location_use_gps";
    public static final String KEY_LOCATION_NEARBY_RADIUS = "location_nearby_radius";

    private SharedPreferences mSharedPrefs;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource( R.xml.preferences );
        mSharedPrefs = getPreferenceScreen().getSharedPreferences();
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Initialize the preference screen
        onSharedPreferenceChanged( mSharedPrefs, KEY_STARTUP_SHOW_ACTIVITY );
        onSharedPreferenceChanged( mSharedPrefs, KEY_SEARCH_AIRPORT_TYPES );
        onSharedPreferenceChanged( mSharedPrefs, KEY_SEARCH_RESULT_LIMIT );
        onSharedPreferenceChanged( mSharedPrefs, KEY_LOCATION_NEARBY_RADIUS );

        // Set up a listener whenever a key changes
        mSharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key ) {
        Preference pref = findPreference( key );
        if ( key.equals( KEY_STARTUP_SHOW_ACTIVITY ) ) {
            String value = mSharedPrefs.getString( KEY_STARTUP_SHOW_ACTIVITY, "browse" );
            if ( value.equals( "browse" ) ) {
                pref.setSummary( "Browse airports screen" );
            } else if ( value.equals( "favorite" ) ) {
                pref.setSummary( "Favorite airports screen" );
            } else if ( value.equals( "nearby" ) ) {
                pref.setSummary( "Nearby airports screen" );
            }
        } else if ( key.equals( KEY_LOCATION_NEARBY_RADIUS ) ) {
            String radius = mSharedPrefs.getString( key, "20" );
            pref.setSummary( "Show within a radius of "+radius+ " miles" );
        } else if ( key.equals( KEY_SEARCH_AIRPORT_TYPES ) ) {
            String type = mSharedPrefs.getString( key, "PUBLIC" );
            if ( type.equals( "ALL" ) ) {
                pref.setSummary( "Search within all airports" );
            } else {
                String typeDesc = type.equals( "PU" )? "public" : "private";
                pref.setSummary( "Search within "+typeDesc+" use airports only" );
            }
        } else if ( key.equals( KEY_SEARCH_RESULT_LIMIT ) ) {
            String value = mSharedPrefs.getString( key, "20" );
            int limit;
            try {
                // Try to parse the user input as a number
                limit = Integer.valueOf( value );
            } catch ( NumberFormatException e ) {
                // User entered an invalid number, reset to the default value
                limit = 20;
                EditTextPreference textPref = (EditTextPreference)pref;
                textPref.setText( String.valueOf( limit ) );
            }
            pref.setSummary( "Limit results to "+limit+" entries" );
        }
    }

}