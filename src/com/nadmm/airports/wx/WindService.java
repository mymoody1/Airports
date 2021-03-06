/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.wx;

import java.io.File;

import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;

import android.content.Intent;
import android.text.format.DateUtils;

public class WindService extends NoaaService {

    private final String WIND_IMAGE_NAME = "ruc00hr_%s_wind.gif";
    private final String WIND_IMAGE_ZOOM_NAME = "ruc00hr_%s_wind_zoom.gif";
    private final String WIND_IMAGE_PATH = "/data/winds/";
    private final String WIND_IMAGE_ZOOM_PATH = "/data/winds/zoom/";

    private static final long WIND_CACHE_MAX_AGE = 60*DateUtils.MINUTE_IN_MILLIS;

    public WindService() {
        super( "progchart", WIND_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_WIND ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals( TYPE_IMAGE ) ) {
                String code = intent.getStringExtra( IMAGE_CODE );
                boolean hiRes = getResources().getBoolean( R.bool.WxHiResImages );
                String imageName = String.format(
                        hiRes? WIND_IMAGE_ZOOM_NAME : WIND_IMAGE_NAME,
                        code );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    try {
                        String path = hiRes? WIND_IMAGE_ZOOM_PATH : WIND_IMAGE_PATH;
                        path += imageName;
                        fetchFromNoaa( path, null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch Wind image: "
                                +e.getMessage() );
                    }
                }

                // Broadcast the result
                sendResultIntent( action, code, imageFile );
            }
        }
    }

}
