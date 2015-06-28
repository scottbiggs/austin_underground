package com.sleepfuriously.biggstourofaustin;

import android.support.v7.app.ActionBarActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The main Activity for the Underground Austin program.
 *<p>
 * Some definitions to clarify between similar meanings:
 *<p>
 * 		Coordinate (coords)	Longitude & latitude description of a
 * 					very specific place.  May apply to
 * 					many things (both a Location AND this
 * 					device (a Position) may/will have coordinates.
 *<p>
 * 		Location		A Java class that stores information about
 * 					a specific place on this planet.  It uses
 * 					Coordinates, but does much more.
 *<p>
 * 		DestinationData		A class that describes a place the
 * 					app will help you to find.  All sorts of
 * 					data is attributed here.  The class
 * 					DestinationData encapsulates this notion.
 * 					Also abbreviated as simply "destdata".
 *<p>
 *		LocationManager		A class object that allows access
 *					to the device location hardware.
 *<p>
 *		Target		The Coordinates that the user is trying to get to.
 *<p>
 * 		Position		Where this mobile device is in the world (currently -
 * 					in Coordinates, of course).
 *<p>
 *		Discovered	A Location that the user has successfully found.
 *<p>
 *		Hidden		Any Location that the user has not Discovered yet.
 *<p>
 * 			--------------
 *<p>
 * 		Direction	The direction is simply a measure of angle,
 * 					in degrees.  In other words, a Direction is
 * 					a general term for...a direction.
 *<p>
 * 		Bearing		This is the Direction that the user SHOULD
 * 					go to get to their destination.
 *<p>
 * 		Heading		This is the Direction that the phone is
 * 					pointed.
 *
 * The general strategy of this is similar to the main loop of
 * a game.  Except instead of a loop, I'm iterating whenever
 * a location or sensor event is called.  So here's the flowchart:
 *
 * This Activity displays the contents of a LocationData.  The
 * various LocationData are loaded as needed, but each are
 * numbered, starting at 0 (of course, we're programmers, right?).
 * Preferences denote which have been found and which haven't,
 * as well as what the first one should be.
 *
 */
public class MainActivity extends ActionBarActivity
						implements
							SensorEventListener,
							OnClickListener,
							LocationListener {

	//-------------------------
	//	Public Data
	//-------------------------

	//-------------------------
	//	Private UI Data
	//-------------------------

	/** Correspond to TextViews in our main layout */
	private TextView m_title_tv,
			m_main_msg_title_tv, m_main_msg_subtitle_tv /*,
			m_scroll_tv */;

	/** The various images for the compass and arrow display */
	private ImageView
		m_compass_ring_iv,
		m_direction_arrow_iv,
		m_on_target_iv;

	/**
	 * This is the LinearLayout inside our ScrollView.
	 * It'll hold our LocationData View.
	 */
//	private LinearLayout m_scrolling_ll;

	/** The buttons for this Activity. */
	private Button m_prev_butt, m_next_butt,
		m_test_butt;


	//-------------------------
	//	Private Constants
	//-------------------------

	private final static String TAG = "MainActivity";

	/** Test coordinates - my mailbox */
//	private final static double
//		MY_LAT = 30.310878,
//		MY_LONG = -97.676342;



	/**
	 * The various display states
	 * <p>
	 * STATE_WAITING_FOR_INPUT - Sensors haven't responded yet.
	 * <p>
	 * STATE_SEARCHING - Searching for the target.
	 * <p>
	 * STATE_FOUND_OFF_TARGET - Looking at a found Location,
	 * 		but Position != Target.
	 * <p>
	 * STATE_FOUND_ON_TARGET - Looking at a found Location,
	 * 		AND Position == Target.
	 */
	private final static int
		STATE_WAITING_FOR_INPUT = 0,
		STATE_SEARCHING = 1,
		STATE_FOUND_OFF_TARGET = 2,
		STATE_FOUND_ON_TARGET = 3;


	/**
	 * This needs to be appended with the number of the pref in question.
	 *
	 * The value associated with this key tells whether or not that
	 * particular location has been discovered yet.
	 */
	private final static String PREFS_DISCOVERED_PREFIX = "prefs_discovered_";


	//-------------------------
	//	Private Logical Data
	//-------------------------

	/**
	 * This Activity can display several different states.  This
	 * tells us which is currently being displayed.
	 */
	private int m_display_state = STATE_WAITING_FOR_INPUT;

	/**
	 * The angle that the compass is turned.  Note that this is NOT
	 * the same thing as the direction that the device is Facing
	 * (in fact, it should be the opposite!).
	 */
	private float m_compass_angle = 0f;

	/** The direction that we need to head to reach the target */
	private float m_bearing = 0f;

	/** Is FALSE iff this device cannot detect magnetic fields. */
	private boolean m_can_detect_magnetic = false;

	/** FALSE iff this device has no way to figure out its position in the world. */
	private boolean m_can_detect_position = false;

	/** Manages GPS and other location/positioning */
	private LocationManager m_location_mgr = null;

	/** A flag that's True once the Location system starts working */
	private boolean m_location_working = false;

	/** Used for the compass */
	private SensorManager m_sensor_mgr;

	/** Our current position, according to the sensors. */
	private Location m_position;

	/** The total number of DestinationData items this program knows. */
	private int m_num_destdata;

	/** Th LocationData that is currently displayed. */
	private int m_currently_displayed_destdata;

	/** Holds the LocationData that is currently displayed. */
	private DestinationData m_target_destdata;

	/** Tells if the current Location is discovered (true) or hidden (false). */
	private boolean m_current_discovered = false;

	/**
	 * Used for debugging. Causes the onLocationChanged to always return
	 * the current target when TRUE.
	 */
	private boolean m_debug_on_target = false;


	//-------------------------
	//	Methods
	//-------------------------

	//-------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Initialize the LocationData system and load up the
		// data for the first location.  Test to make sure
		// we have enough locations!
		set_first_destdata();

		// Initialize and display the correct UI
		if (is_location_discovered(m_currently_displayed_destdata)) {
			Log.d(TAG, "onCreate():  location " + m_currently_displayed_destdata + " Discovered!");
			m_current_discovered = true;
			display_discovered_layout();
		}
		else {
			Log.d(TAG, "onCreate():  location " + m_currently_displayed_destdata + " hidden");
			m_current_discovered = false;
			display_hidden_layout();
		}

		// Instantiate.  The provider isn't important.
		m_position = new Location("current location");

		// Set up the sensors and their related mgrs and callbacks.
		// Todo:
		//	This may need to be moved to onResume() so that people may
		//	change their sensors while this program is running (I know
		//	I do all the time!).
		test_sensors();

	} // onCreate(.)


	//-------------------------
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");

		// Setup the location manager
		m_location_mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				1000,	// 1 sec MINIMUM time interval
				1,		// min distance, 1 meters
				this);
		Log.i(TAG, "   - location updates starting.");
		m_location_working = false;		// Reset our flag.

		if (m_can_detect_magnetic == false) {
			Toast.makeText(this, "This device does not have a magnetic sensor. Sorry.",
					Toast.LENGTH_LONG)
				.show();
			return;		// Do nothing.
		}
		else {
			// This example uses the deprecated TYPE_ORIENTATION instead of
			// the better ROTATION.
			// Hmmm, after some research, I'm not sure that ROTATION is any
			// better.  This works, so I'm sticking with it.
			m_sensor_mgr.registerListener(this,
					m_sensor_mgr.getDefaultSensor(Sensor.TYPE_ORIENTATION),
//					SensorManager.SENSOR_DELAY_GAME);
					SensorManager.SENSOR_DELAY_UI);		// Seems slightly faster
		}

	} // onResume()


	//-------------------------
	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause()");

		// Turn off the location updates for battery's sake
		if (m_can_detect_position) {
			m_location_mgr.removeUpdates(this);
			Log.i(TAG, "   - location updates removed.");
		}
		else {
			Log.i(TAG, "   - device doesn't do locations, so no updates removed.");
		}

		// Turn off the compass stuff to save the battery
		if (m_can_detect_magnetic) {
			m_sensor_mgr.unregisterListener(this);
			Log.i(TAG, "   - magnetic sensor unregistered.");
		}
		else {
			Log.i(TAG, "   - no magnetic sensor, so nothing has been unregistered.");
		}

	} // onPause()


	// todo
	//	Don't we need an onDestroy() method to get rid of our managers when
	//	the program is completely over?


	/************************
	 * This is needed to keep this app from changing its
	 * orientation (along with a few lines in the Manifest).
	 *
	 * @param newConfig		Not used.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}


	//-------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	//-------------------------
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	//-------------------------
	@Override
	public void onClick(View v) {
		Log.d(TAG, "click!");

		switch (v.getId()) {
			case R.id.test_butt:
				Log.d(TAG, "onClick(): test button");
				// todo: just a debug statement
				Log.d(TAG, " - setting debug to true.");
				m_debug_on_target = true;
				update_display_state();

//				// todo
//				//	This is just for debugging
//				//
//				// Now to force the issue!
//				onLocationChanged(m_target_destdata.m_loc);
				break;

			case R.id.next_butt:
				Log.d(TAG, "onClick(): next button");
				break;

			case R.id.prev_butt:
				Log.d(TAG, "onClick(): prev button");
				break;

			default:
				Log.e(TAG, "onClick(): unknown View!");
				break;
		}
	}


	//-------------------------
	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "onProviderEnabled()");
	}


	//-------------------------
	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "onProviderDisabled()");
	}



	/******************************
	 * Not used
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d(TAG, "onAccuracyChanged() - (not used)");
	}


	/******************************
	 * Looks like I'm not registering this status change.  That's
	 * why I'm not getting any hits?
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
//		String str = "Provider = " + provider;
//
//		switch (status) {
//			case LocationProvider.OUT_OF_SERVICE:
//				str = str + getString(R.string.out_of_service);
//				break;
//
//			case LocationProvider.TEMPORARILY_UNAVAILABLE:
//				str = str + getString(R.string.temp_unavailable);
//				break;
//
//			case LocationProvider.AVAILABLE:
//				str = str + getString(R.string.available);
//				break;
//		}
//		if (m_current_tv == null) {
//			Log.e(TAG, "m_current_tv is null!!!  Aborting!");
//			return;
//		}
//		m_current_tv.setText(str);
//		Log.d(TAG, "onStatusChanged(): " + str);
	}


	/******************************
	 * Happens whenever the magnetic sensor changes (supposedly).
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		// Get the angle around the z-axis and display it.
		float angle = (Math.round(event.values[0]));
//		Log.d(TAG, "onSensorChanged(): angle = " + angle);

		update_ui(angle,
				m_position,
				m_target_destdata.m_loc);
	} // onSensroChanged(event)


		/***************************
		 * Called whenever the location manager responds to
		 * a change.  Kind of unpredictable as far as I see.
		 * <p>
		 * Using my terminology, this is actually called when our
		 * <i><b>Position</b></i> has changed.
		 * <p>
		 * Also, this is where the display state is called and
		 * updated (makes sense).
		 */
	private static int s_location_changed = 1;
	@Override
	public void onLocationChanged(Location pos) {
		Log.d(TAG, "onLocationChanged()");
		TextView debug_tv = (TextView) findViewById(R.id.debug_tv);
		debug_tv.setText("onLocationChanged() call #" + s_location_changed++);

		m_location_working = true;

		// Are we testing???
		if (m_debug_on_target) {
			Log.d(TAG, "  - setting current to target.");
			m_position.set(m_target_destdata.m_loc);
			update_ui(m_compass_angle, m_position, m_target_destdata.m_loc);
			return;
		}

		Log.d(TAG, "  - Normal functioning.");

		// Now we have a new position.  Change it and update our state.
		m_position = pos;
		update_display_state();

		// The complement of the compass_angle is the heading of the device.
		update_ui(-m_compass_angle,
				m_position,
				m_target_destdata.m_loc);
	} // onLocationChanged (loc)


	/***************************
	 * Use this to see if the user has discovered a location or
	 * not.
	 *
	 * @param loc	The Location number, a reference to the
	 * 				m_currently_displayed_destdata int.
	 *
	 * @return
	 */
	private boolean is_location_discovered (int loc) {
		// This works by accessing the preferences, which stores
		// the information about what's discovered and what's
		// hidden.
		SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(this);

		// Construct the key
		String key = PREFS_DISCOVERED_PREFIX + loc;

		return prefs.getBoolean(key, false);
	} // is_location_discovered (loc)


	/***************************
	 * Working exclusively by side effect, this method figures out
	 * what the first LocationData to display is.
	 *<p>
	 * Also does some initializations for the LocationDatas.
	 *<p>
	 * <b><u>side effects</u></b>:</br>
	 * 	<b>m_num_destdata</b>		Will be set to hold how many LocationDatas
	 * 							are available for this program.
	 *<p>
	 *	<b>m_currently_displayed_loc</b>	This is set here!  Right
	 *									now I'm just using 0.
	 *<p>
	 *	<b>m_target_destdata</b>	This is loaded up with the LocationData
	 *							as indicted by m_currently_displayed_destdata.
	 *
	 */
	private void set_first_destdata() {
		// How many LocationDatas does this program have now?  Grab one
		// of our arrays (they all should be the same length) and
		// use its length.
		CharSequence[] titles = getResources().getTextArray(R.array.titles);
		if (titles == null) {
			Log.e(TAG, "Unable to get resource array in set_first_destdata()! Aborting!");
			return;
		}
		m_num_destdata = titles.length;

		if (m_num_destdata == 0) {
			Log.e(TAG, "No LocationDatas. Aborting!!!");
			Toast.makeText(this, "Problem reading location data. Please re-install (sorry for the inconvience).",
					Toast.LENGTH_LONG).show();
			return;
		}

		// todo
		//	Do something more elegant than starting with the first one.
		//	How about starting with the first one that hasn't been found?
		m_currently_displayed_destdata = 0;
		m_target_destdata = new DestinationData();
		m_target_destdata.load(this, m_currently_displayed_destdata);

	} // set_first_destdata()


	/*******************
	 * Checks the conditions to see if the state has changed.
	 * If the state DOES change, all the side effects are done
	 * HERE.
	 *
	 * preconditions:
	 * 	m_position		Should have the device's correct
	 * 					coordinates (our position).
	 *
	 * 	m_target_destdata	Properly loaded with our target.
	 *
	 * side effects:
	 * 	m_display_state		Changed to reflect the correct state.
	 *
	 *	Some UI stuff may be changed to prepare for appropriate
	 *		use later.
	 *
	 * @return		TRUE iff this method a state change (and did
	 * 				the appropriate actions).  FALSE if no change.
	 */
	private boolean update_display_state() {
		boolean return_val = false;

		if (m_debug_on_target) {
			// This supercedes all state changes.
			if (m_display_state == STATE_SEARCHING) {
				setup_location_discovered();
				m_current_discovered = true;
				m_display_state = STATE_FOUND_ON_TARGET;
				return true;
			}
			return false;
		}

		switch (m_display_state) {
			case STATE_WAITING_FOR_INPUT:
				if (m_location_working) {
					m_display_state = STATE_SEARCHING;
					Log.d(TAG, "Moved from STATE_WAITING_FOR_LOCATION to STATE_MOVING_TO_TARGET.");
					return_val = true;
				}
				break;

			case STATE_SEARCHING:
				if (is_on_target(m_position, m_target_destdata.m_loc,
								m_target_destdata.get_slop()) == true) {
					Log.d(TAG, "Changing from moving_to_target to found_on_target");
					setup_location_discovered();
					m_display_state = STATE_FOUND_ON_TARGET;
					return_val = true;
				}
				break;

			case STATE_FOUND_ON_TARGET:
				// Did the user move away from their target?
				if (is_on_target(m_position, m_target_destdata.m_loc,
								m_target_destdata.get_slop()) == false) {
					Log.d(TAG, "Changing from found_on_target to found_off_target");
					setup_moving_to_target();
					m_display_state = STATE_FOUND_OFF_TARGET;
					return_val = true;
				}
				break;

			case STATE_FOUND_OFF_TARGET:
				// User may have gotten to the target again.
				if (is_on_target(m_position, m_target_destdata.m_loc,
								m_target_destdata.get_slop()) == true) {
					Log.d(TAG, "Changing from found_off_target to found_on_target");
					setup_location_discovered();
					m_display_state = STATE_FOUND_ON_TARGET;
				}
				break;

			default:
				Log.e(TAG, "Illegal state in update_ui()!");
				break;

		} // switch (m_display_state)

		return return_val;
	} // check_state()


	/*******************
	 * Are these two coords close enough to be considered
	 * the same place.
	 */
	private boolean is_on_target (Location a, Location b, float slop) {
		if (a.distanceTo(b) <= slop) {
			return true;
		}
		return false;
	}


	/*******************
	 * Tests the hardware to make sure that this device will
	 * do what we want it to do.
	 *
	 * NOTE: Testing the sensors via the SensorManager causes
	 * hangs on api 8.  So I'm commenting it out.
	 *
	 * Side Effects:
	 * 	m_location_mgr				Started
	 * 	m_can_detect_position		Set
	 *
	 *  m_sensor_mgr					Started
	 *  m_can_detect_magnetic		Set
	 */
	private void test_sensors() {

		//
		// This first section causes all sorts of problems
		// in API 8.  So it's commented out.
		//
		//	Perhaps this only applies to the emulator.  It seems
		//	to work fine on my phone.
		//

		// Test to see if this device detects magnetic fields.
		Log.d(TAG, "About to get a sensor manager...");
		m_sensor_mgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		Log.d(TAG, "Got it!");

		if (m_sensor_mgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
			// No magnetic detection possible on this device
//			m_heading_tv.setText(R.string.no_mag_sensor_msg);
			m_can_detect_magnetic = false;
//			m_main_msg_title_tv.setText(R.string.no_mag_sensor_msg);		// Should cause a crash
			Toast.makeText(this, "This device does not have a magnetic sensor. Sorry.", Toast.LENGTH_LONG).show();
			return;
		}
		else {
			m_can_detect_magnetic = true;
		}

		// todo
		//	Test on devices that don't have position detectors
		//	Test on devices with their GPS turned off
		//	Test on devices with all networking turned off
		//	Test on devices with no phone service
		//	Test all combinations of these!

		// Test to see if this device is set can find coordinates.
		m_location_mgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (m_location_mgr == null) {
			m_can_detect_position = false;
			Log.e(TAG, "Problem getting location manager. Aborting!");
//			m_title_tv.setText(R.string.no_location_detector_msg);		This should cause a crash
			Toast.makeText(this, "This device does not support location detecting. Sorry.", Toast.LENGTH_LONG).show();
			return;
		}

		try {
			m_can_detect_position = m_location_mgr.
					isProviderEnabled(LocationManager.GPS_PROVIDER);
		}
		catch (IllegalArgumentException e) {
			Log.e(TAG, "Big problem in isProviderEnabled()! Aborting!");
			m_can_detect_position = false;
		}
		if (m_can_detect_position == false) {
			try {
				m_can_detect_position = m_location_mgr.
						isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Even bigger problem in isProviderEnabled()! Aborting!");
				m_can_detect_position = false;
				return;
			}
		}

		return;
	} // test_sensors()


	//---------------------------------
	//	UI Setters
	//
	//		All these methods modify the UI.
	//---------------------------------

	/***************************
	 * The general form for updating the UI.  Simply a stub before
	 * determining if we're showing the hidden or discovered ui.
	 *
	 * @param heading	The direction the phone is actually pointing
	 * 					to (in degrees, of course).  So if the top of
	 * 					the phone is pointing East, this number is 90.
	 *
	 * @param current_loc	The current coords of this device.
	 *
	 * @param target_loc		The coords of our target.
	 */
	private void update_ui (float heading,
							Location current_loc,
							Location target_loc) {
		if (m_current_discovered) {
			update_discovered_ui (heading, current_loc, target_loc);
		}
		else {
			update_hidden_ui(heading, current_loc, target_loc);
		}
	} // update_ui (heading, current_loc, target_loc)


	/***************************
	 * This is the main thing that updates all the UI stuff for
	 * the hidden location layout (the location hasn't been
	 * discovered yet).
	 *
	 * Here are the items to be updated:
	 * 		Compass Ring
	 * 		Arrow to target
	 * 		Distance to target
	 * 		Warmer/Colder message
	 * 		Any state change (are we on target, etc.)
	 *
	 * @param heading	The direction the phone is actually pointing
	 * 					to (in degrees, of course).  So if the top of
	 * 					the phone is pointing East, this number is 90.
	 *
	 * @param current_loc	The current coords of this device.
	 *
	 * @param target_loc		The coords of our target.
	 */
	private void update_hidden_ui (float heading,
								Location current_loc,
								Location target_loc) {
		// Strategy:
		//	- calculate all our variables
		//	- update the compass
		//	- depending on the state, update the arrow


		// Calculate the distance we are to the target and whether
		// we are "on target".
		float dist = current_loc.distanceTo(target_loc);

		// Calculate our bearing.  After finding the absolute
		// direction, subtract the compass_direction to get
		// the direction the user should be facing relative to
		// the device.
		//	Note that when we're on target, this won't be used
		//	(it could fluctuate wildly).
		float bearing = current_loc.bearingTo(target_loc);
		bearing -= heading;

		// Always update our compass.
		animate_compass (heading);

		switch (m_display_state) {
			case STATE_WAITING_FOR_INPUT:
				// todo
				//	Might give an update on the status?
				break;

			case STATE_SEARCHING:
			case STATE_FOUND_OFF_TARGET:
				display_distance (dist);
				animate_direction(bearing);
				break;

			case STATE_FOUND_ON_TARGET:
				Log.e(TAG, "Encountered the STATE_FOUND_ON_TARGET in update_hidden_ui()!");
				break;

			default:
				Log.e(TAG, "Illegal state in update_ui()!");
				break;
		}

	} // update_hidden_ui (compass_dir, current_loc, target_loc)


	/***************************
	 * This is the main thing that updates all the UI stuff for
	 * locations that have already been found by the user.
	 *
	 * Here are the items to be updated:
	 * 		Compass Ring
	 * 		Arrow to target
	 * 		Distance to target
	 * 		Warmer/Colder message
	 * 		Any state change (are we on target, etc.)
	 *
	 * @param heading	The direction the phone is actually pointing
	 * 					to (in degrees, of course).  So if the top of
	 * 					the phone is pointing East, this number is 90.
	 *
	 * @param current_loc	The current coords of this device.
	 *
	 * @param target_loc		The coords of our target.
	 */
	private void update_discovered_ui (float heading,
									Location current_loc,
									Location target_loc) {
		// Strategy:
		//	- calculate all our variables
		//	- update the compass
		//	- depending on the state, update the arrow

		// Distance form Position to Target.
		float dist = current_loc.distanceTo(target_loc);

		// Calculate our bearing.  Not used when "on target".
		float bearing = current_loc.bearingTo(target_loc);
		bearing -= heading;

		// Always update our compass.
		animate_compass (heading);

		// todo



	} // update_discovered_ui (heading, current_loc, target_loc)


	/***************************
	 * Sets the current layout to the DISCOVERED layout.
	 * Then all the appropriate widgets are initialized.
	 *
	 * preconditions:
	 * 	m_currently_displayed_destdata	Appropriately set.
	 *
	 */
	private void display_discovered_layout() {
		setContentView(R.layout.main_discovered);
		deactivate_all_widgets();

		// todo
		//	Load up the widgets for this display

	} // display_discovered_layout()


	/***************************
	 * Sets the current layout to the HIDDEN layout.
	 * Then all the appropriate widgets are initialized.
	 *
	 * preconditions:
	 * 	m_currently_displayed_destdata	Appropriately set.
	 *
	 */
	private void display_hidden_layout() {
		setContentView(R.layout.main_hidden);
		deactivate_all_widgets();

		// Load up our widgets
		m_title_tv = (TextView) findViewById(R.id.top_tv);
		m_main_msg_title_tv = (TextView) findViewById(R.id.main_msg_title_tv);
		m_main_msg_subtitle_tv = (TextView) findViewById(R.id.main_msg_details_tv);
//		m_scroll_tv = (TextView) findViewById(R.id.scroll_tv);

//		m_scrolling_ll = (LinearLayout) findViewById(R.id.scroller_ll);

		m_compass_ring_iv = (ImageView) findViewById(R.id.compass_iv);
		m_direction_arrow_iv = (ImageView) findViewById(R.id.direction_iv);
		m_on_target_iv = (ImageView) findViewById(R.id.on_target_iv);

		m_prev_butt = (Button) findViewById(R.id.prev_butt);
		m_next_butt = (Button) findViewById(R.id.next_butt);
		m_test_butt = (Button) findViewById(R.id.test_butt);

		// Set the button listeners
		m_prev_butt.setOnClickListener(this);
		m_next_butt.setOnClickListener(this);
		m_test_butt.setOnClickListener(this);

		// Fill in the title for this Activity (the Hint for now).
		m_title_tv.setText(m_target_destdata.m_hint);
	} // display_discovered_layout()


	/***************************
	 * Turns off all the widgets and sets them to null.  This is
	 * to prevent accidental usage of a widget that is not active
	 * in the current layout (because this Activity uses two different
	 * layouts).
	 */
	private void deactivate_all_widgets() {
		m_title_tv = null;
		m_main_msg_title_tv = null;
//		m_scroll_tv = null;
//		m_scrolling_ll = null;
		m_compass_ring_iv = null;
		m_direction_arrow_iv = null;
		m_on_target_iv = null;
		m_prev_butt = null;
		m_next_butt = null;
		m_test_butt = null;
	} // deactivate_all_widgets()


	/*******************
	 * Sets up the UI for when we enter this state. Does no
	 * state changing (the caller's responsibility).
	 *
	 * - Turn off the directional arrow View.
	 * - Turn on the On-Target View.
	 * - Display the on-target message.
	 * - Display the story and any relevant images.
	 *
	 * Side Effects:
	 * 	various widgets changed
	 */
	private void setup_location_discovered() {
		m_direction_arrow_iv.setVisibility(View.INVISIBLE);
		m_on_target_iv.setVisibility(View.VISIBLE);
		m_main_msg_title_tv.setText(R.string.on_target);
	} // setup_on_target()


	/*******************
	 * This sets up the UI for when the user is moving around
	 * (hopefully towards the target, but this will often happen
	 * when they finished enjoying a LocationData and move away).
	 *
	 * No state changing is done (caller's responsibility.
	 *
	 * - Turn off on-target View.
	 * - Turn on direction arrow View.
	 * - Display off-target message
	 * - Display the story and stuff IF they have found the
	 * 	target.
	 */
	private void setup_moving_to_target() {
		m_direction_arrow_iv.setVisibility(View.VISIBLE);
		m_on_target_iv.setVisibility(View.INVISIBLE);
	} // setup_moving_to_target()


	/***************************
	 * Simply makes the appropriate distance messages.  Does no
	 * state checking or modifications.
	 *
	 * This should be called ONLY if we're NOT on-target as it
	 * changes stuff in the ScrollView.
	 *
	 * @param dist	The current distance we are from our target.
	 */
	private void display_distance (float dist) {
		String str = String.format("%.1f meters", dist);
		m_main_msg_subtitle_tv.setText(str);
//		m_scroll_tv.setText(str);

		String hot_cold_str = m_target_destdata.m_hotcold.get_msg(this, m_position);
		m_main_msg_title_tv.setText(hot_cold_str);
	} // display_distance (dist)


	/***************************
	 * Displays all the information we have about a LocationData.
	 * Generally called once the user gets on-target, this inflates
	 * a complicated view related to this LocationData and spits into
	 * a ScrollView at the bottom of the screen.
	 *
	 * @param loc	The class that holds the all the data we want
	 * 				display.
	 */
	private void display_destdata_info (DestinationData loc) {
//		LayoutInflater inflator = getLayoutInflater();
//		View destdata_view = inflator.inflate(R.layout.location_story, m_scrolling_ll, false);
//
//		m_scroll_tv.setVisibility(View.GONE);
//
//		TextView title = (TextView) destdata_view.findViewById(R.id.location_title_tv);
//		title.setText(loc.m_title);
//
//		TextView lat_long = (TextView) destdata_view.findViewById(R.id.location_lat_long_tv);
//		lat_long.setText(loc.get_latitude_str() + ", " + loc.get_longitude_str());
//
//		TextView loc_addr = (TextView) destdata_view.findViewById(R.id.location_address_tv);
//		loc_addr.setText(loc.m_address);
//
//		TextView subtitle = (TextView) destdata_view.findViewById(R.id.location_sub_title_tv);
//		subtitle.setText(loc.m_subtitle);
//
//		TextView story = (TextView) destdata_view.findViewById(R.id.location_story_tv);
//		story.setText(loc.m_story);
//
//		//		todo: images
//
//		m_scrolling_ll.addView(destdata_view);
	} // display_location_info (loc)


	/***************************
	 * Causes the compass ring to rotate with an animation
	 * based on the given angle.  The old angle is remembered
	 * and used as the base of the animation.
	 *
	 * @param heading	The angle of direction that the
	 *                  phone is turned towards.
	 *<p>
	 * side effects:
	 *	m_current_degree		Modified to remember the angle
	 *                  		for next time.
	 */
	private void animate_compass (float heading) {
		// create a rotation animation (reverse turn degree degrees)
		RotateAnimation ra = new RotateAnimation(
				m_compass_angle,
				-heading,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF,
				0.5f);

		// how long the animation will take place
//		ra.setDuration(210);
		ra.setDuration(0);		// Eliminates the weirdness when going past 0

		// set the animation after the end of the reservation status
		ra.setFillAfter(true);

		// Start the animation
		m_compass_ring_iv.startAnimation(ra);
		m_compass_angle = -heading;
	} // animate_compass (angle)


	/***************************
	 * Causes the direction arrow to rotate (with animation)
	 * based on the given angle.  The old angle is remembered
	 * and used as the base for the animation.
	 * @param bearing
	 */
	private void animate_direction (float bearing) {
//		Log.d(TAG, "animate_direction ( " + bearing + " )");

		// create a rotation animation (reverse turn degree degrees)
		RotateAnimation ra = new RotateAnimation(
				m_bearing,
				bearing,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF,
				0.5f);

		// how long the animation will take place
		ra.setDuration(0);		// Eliminates the weirdness when going past 0

		// set the animation after the end of the reservation status
		ra.setFillAfter(true);

		// Start the animation
		m_direction_arrow_iv.startAnimation(ra);
		m_bearing = bearing;
	} // animate_direction (bearing)

}
