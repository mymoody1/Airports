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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Attendance;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;

public class AttendanceDetailsActivity extends Activity {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        AirportAttendanceTask task = new AirportAttendanceTask();
        task.execute( siteNumber );
    }

    private final class AirportAttendanceTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 3 ];
            
            DatabaseManager dbManager = DatabaseManager.instance( getApplicationContext() );
            cursors[ 0 ] = dbManager.getAirportDetails( siteNumber );

            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Attendance.TABLE_NAME );
            cursors[ 1 ] = builder.query( db,
                    new String[] { Attendance.ATTENDANCE_SCHEDULE },
                    Runways.SITE_NUMBER+"=? ", new String[] { siteNumber }, 
                    null, null, Attendance.SEQUENCE_NUMBER, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            cursors[ 2 ] = builder.query( db,
                    new String[] { Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=? "
                    +"AND substr("+Remarks.REMARK_NAME+", 1, 3)='A17'",
                    new String[] { siteNumber }, null, null, null, null );

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            if ( result == null ) {
                // TODO: Show an error here
                return;
            }

            View view = mInflater.inflate( R.layout.attendance_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.attendance_top_layout );

            // Title
            Cursor apt = result[ 0 ];
            GuiUtils.showAirportTitle( mMainLayout, apt );

            showAttendanceDetails( result );
        }

    }

    protected void showAttendanceDetails( Cursor[] result ) {
        Cursor att = result[ 1 ];
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById(
                R.id.detail_attendance_layout );
        if ( att.moveToFirst() ) {
            do {
                String schedule = att.getString(
                        att.getColumnIndex( Attendance.ATTENDANCE_SCHEDULE ) );
                addRow( layout, schedule );
            } while ( att.moveToNext() );
        }

        Cursor rmk = result[ 2 ];
        if ( rmk.moveToFirst() ) {
            do {
                String remark = rmk.getString(
                        rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                addRemarkRow( layout, remark );
            } while ( att.moveToNext() );            
        }
    }

    protected void addRow( LinearLayout layout, String remark ) {
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 10, 4, 2, 4 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 4, 4, 12, 4 );
        tv.setText( remark );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addRemarkRow( LinearLayout layout, String remark ) {
        int index = remark.indexOf( ' ' );
        if ( index != -1 ) {
            while ( remark.charAt( index ) == ' ' ) {
                ++index;
            }
            remark = remark.substring( index );
        }
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 10, 2, 2, 2 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 2, 12, 2 );
        tv.setText( remark );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

}