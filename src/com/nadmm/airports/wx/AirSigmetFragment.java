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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ImageViewActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.wx.AirSigmet.AirSigmetEntry;

public class AirSigmetFragment extends FragmentBase {

    private final int AIRSIGMET_RADIUS_NM = 50;
    private final int AIRSIGMET_HOURS_BEFORE = 3;

    protected Location mLocation;
    protected BroadcastReceiver mReceiver;
    protected String mStationId;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( true );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                String action = intent.getAction();
                if ( action.equals( NoaaService.ACTION_GET_AIRSIGMET_TEXT ) ) {
                    AirSigmet airSigmet = (AirSigmet) intent.getSerializableExtra( NoaaService.RESULT );
                    showAirSigmetText( airSigmet );
                } else if ( action.equals( NoaaService.ACTION_GET_AIRSIGMET_MAP ) ) {
                    String path = intent.getStringExtra( NoaaService.RESULT );
                    showAirSigmetMap( path );
                }
            }

        };
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( NoaaService.ACTION_GET_AIRSIGMET_TEXT );
        filter.addAction( NoaaService.ACTION_GET_AIRSIGMET_MAP );
        getActivity().registerReceiver( mReceiver, filter );

        Bundle args = getArguments();
        String stationId = args.getString( NoaaService.STATION_ID );
        setBackgroundTask( new AirSigmetTask() ).execute( stationId );
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver( mReceiver );
        super.onPause();
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.airsigmet_detail_view, container, false );
        Button btnMap = (Button) view.findViewById( R.id.btnShowOnMap );
        btnMap.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                requestAirSigmetMap( false );
            }
        } );

        return createContentView( view );
    }

    private final class AirSigmetTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String stationId = params[ 0 ];

            Cursor[] cursors = new Cursor[ 1 ];
            SQLiteDatabase db = getDbManager().getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Wxs.TABLE_NAME );
            String selection = Wxs.STATION_ID+"=?";
            Cursor c = builder.query( db, new String[] { "*" }, selection,
                    new String[] { stationId }, null, null, null, null );
            cursors[ 0 ] = c;

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            Cursor wxs = result[ 0 ];
            if ( wxs.moveToFirst() ) {
                mStationId = wxs.getString( wxs.getColumnIndex( Wxs.STATION_ID ) );
                mLocation = new Location( "" );
                float lat = wxs.getFloat( wxs.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) );
                float lon = wxs.getFloat( wxs.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) );
                mLocation.setLatitude( lat );
                mLocation.setLongitude( lon );    
                // Now request the airmet/sigmet
                requestAirSigmetText( false );
            }
            return true;
        }

    }

    protected void requestAirSigmetText( boolean refresh ) {
        double[] box = GeoUtils.getBoundingBoxDegrees( mLocation, AIRSIGMET_RADIUS_NM );
        Intent service = new Intent( getActivity(), AirSigmetService.class );
        service.setAction( NoaaService.ACTION_GET_AIRSIGMET_TEXT );
        service.putExtra( NoaaService.STATION_ID, mStationId );
        service.putExtra( NoaaService.COORDS_BOX, box );
        service.putExtra( NoaaService.HOURS_BEFORE, AIRSIGMET_HOURS_BEFORE );
        service.putExtra( NoaaService.FORCE_REFRESH, refresh );
        getActivity().startService( service );
    }

    protected void requestAirSigmetMap( boolean refresh ) {
        startRefreshAnimation();
        Intent service = new Intent( getActivity(), AirSigmetService.class );
        service.setAction( NoaaService.ACTION_GET_AIRSIGMET_MAP );
        service.putExtra( NoaaService.FORCE_REFRESH, refresh );
        getActivity().startService( service );
    }

    protected void showAirSigmetText( AirSigmet airSigmet ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.airsigmet_entries_layout );
        layout.removeAllViews();

        TextView tv = (TextView) findViewById( R.id.airsigmet_title_msg );
        if ( !airSigmet.entries.isEmpty() ) {
            tv.setText( String.format( "%d AIR/SIGMETs reported within %d NM of %s in"
                    +" last %d hours", airSigmet.entries.size(), AIRSIGMET_RADIUS_NM,
                    mStationId, AIRSIGMET_HOURS_BEFORE ) );
            for ( AirSigmetEntry entry : airSigmet.entries ) {
                showAirSigmetEntry( layout, entry );
            }
        } else {
            tv.setText( String.format( "No AIR/SIGMETs reported within %d NM of %s in"
                    +" last %d hours",
                    AIRSIGMET_RADIUS_NM, mStationId, AIRSIGMET_HOURS_BEFORE ) );
        }

        tv = (TextView) findViewById( R.id.wx_fetch_time );
        tv.setText( "Fetched on "+TimeUtils.formatDateTime( getActivity(), airSigmet.fetchTime )  );
        tv.setVisibility( View.VISIBLE );

        stopRefreshAnimation();
        setFragmentContentShown( true );
    }

    protected void showAirSigmetEntry( LinearLayout layout, AirSigmetEntry entry ) {
        RelativeLayout item = (RelativeLayout) inflate( R.layout.airsigmet_detail_item );
        TextView tv = (TextView) item.findViewById( R.id.airsigmet_type );
        tv.setText( entry.type );
        tv = (TextView) item.findViewById( R.id.wx_raw_airsigmet );
        tv.setText( entry.rawText );
        tv = (TextView) item.findViewById( R.id.airsigmet_age );
        tv.setText( TimeUtils.formatDateRange( getActivity(), entry.fromTime, entry.toTime ) );

        LinearLayout details = (LinearLayout) item.findViewById( R.id.airsigmet_details );

        if ( entry.hazardType != null && entry.hazardType.length() > 0 ) {
            addRow( details, "Type", entry.hazardType );
        }

        if ( entry.hazardSeverity != null && entry.hazardSeverity.length() > 0 ) {
            addRow( details, "Severity", entry.hazardSeverity );
        }

        if ( entry.minAltitudeFeet < Integer.MAX_VALUE
                || entry.maxAltitudeFeet < Integer.MAX_VALUE ) {
            addRow( details, "Altitude", FormatUtils.formatFeetRangeMsl(
                    entry.minAltitudeFeet, entry.maxAltitudeFeet ) );
        }

        if ( entry.movementDirDegrees < Integer.MAX_VALUE ) {
            StringBuilder sb = new StringBuilder();
            sb.append( String.format( "%d\u00B0 (%s)", entry.movementDirDegrees,
                    GeoUtils.getCardinalDirection( entry.movementDirDegrees ) ) );
            if ( entry.movementSpeedKnots < Integer.MAX_VALUE ) {
                sb.append( String.format( " at %d knots", entry.movementSpeedKnots ) );
            }
            addRow( details, "Movement", sb.toString() );
        }

        layout.addView( item, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );
    }

    protected void showAirSigmetMap( String path ) {
        stopRefreshAnimation();
        Intent view = new Intent( getActivity(), ImageViewActivity.class );
        view.putExtra( ImageViewActivity.IMAGE_PATH, path );
        view.putExtra( ImageViewActivity.IMAGE_TITLE, "AIRMET/SIGMET" );
        view.putExtra( ImageViewActivity.IMAGE_SUBTITLE, "Currently Active" );
        startActivity( view );
    }

    @Override
    public void onPrepareOptionsMenu( Menu menu ) {
        setRefreshItemVisible( true );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
        case R.id.menu_refresh:
            startRefreshAnimation();
            requestAirSigmetText( true );
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

}
