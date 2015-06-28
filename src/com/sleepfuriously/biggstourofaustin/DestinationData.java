package com.sleepfuriously.biggstourofaustin;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.webkit.WebViewFragment;
import android.widget.ImageView;


/**
 * This class holds all the data (and maybe a few methods)
 * for a location that the user is trying to find (a <i>destination</i>).
 */
public class DestinationData {

	//-------------------------
	//	Constants
	//-------------------------

	/**
	 * This is the prefix for all the SharedPreference keys that
	 * relate to the found/not-found data (boolean) for all the
	 * locations.
	 *
	 * To make a key for a specific location, concatenate this
	 * prefix with the array number of the location in question.
	 * For example, to find out if the location at array position
	 * 2 has been found or not use this code:
	 *
	 * String key = PREF_KEY_FOUND_PREFIX + 2;
	 * SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	 * boolean found = prefs.getBoolean(key);
	 */
	private final static String PREF_KEY_FOUND_PREFIX = "LocationData_fnd";

	/** The name of the assets file for our json data. */
	private final static String DESTINATION_DATA_FILENAME = "LocationData.json";


	//-------------------------
	//	Member Data
	//-------------------------

	/** The all-important location! */
	public Location m_loc;

	/** The Strings for the UI.  Null if not used. */
	public String
		m_title,
		m_address,
		m_subtitle,
		m_story,
		m_hint;		// Displayed at the top of the main screen.

	/** How cool this attraction is */
	public float m_rating = 0f;

	/**
	 * This is the distance you have to be from the actual location
	 * to be "on target."  Some items are very large, others are
	 * very specific, hence this variable.  Default is 3 meters.
	 */
//	public float m_on_target_distance = 3f;

	/**
	 * Holds the images.  Possible that there won't be any.
	 *
	 *  todo
	 *  Change this to URLs so I can download them as needed--don't
	 *  want to waste too much space with this app.
	 */
	public ArrayList<ImageView> m_images;

	/** Tells if the use has found this location or not */
	public boolean m_found = false;

	/** Used to gather our hot/cold messages */
	public HotColdDistance m_hotcold = null;

	/**
	 * The html version of this Location (to be shown when the
	 * location has been found).
	 */
	WebViewFragment m_webview = null;


	//-------------------------
	//	Methods
	//-------------------------

	/***************************
	 * Constructor
	 */
	public DestinationData() {
		m_loc = new Location("LocationData Class");
		m_images = new ArrayList<ImageView>();
	}


	/***************************
	 * Load up the data for the specified location into this class.
	 * Please use this routine instead of writing to the data
	 * directly.
	 *
	 * Currently all the data is stored in Resource Files.  This may
	 * change in the future, so it's best that these changes stay
	 * localized in this method.
	 *
	 * side effects:
	 * 		m_location_data		Filled with info about the location.
	 *
	 * @param ctx				The Context (use "this" from an Activity
	 * 							or Service).
	 *
	 * @param loc_num			The position in the location array
	 * 							to load up.
	 */
	public void load (Context ctx, int loc_num) {

		// the location (latitude & longitude)
		double latitude, longitude;
		String[] array = ctx.getResources().getStringArray(R.array.latitudes);
		latitude = Double.parseDouble(array[loc_num]);
		array = ctx.getResources().getStringArray(R.array.longitudes);
		longitude = Double.parseDouble(array[loc_num]);
		m_loc.setLatitude(latitude);
		m_loc.setLongitude(longitude);

		// hint
		String[] hints = ctx.getResources().getStringArray(R.array.hints);
		m_hint = hints[loc_num];

		// title
		String[] titles = ctx.getResources().getStringArray(R.array.titles);
		m_title = titles[loc_num];

		// address
		String[] addresses = ctx.getResources().getStringArray(R.array.addresses);
		m_address = addresses[loc_num];

		// subtitle
		String[] subtitles = ctx.getResources().getStringArray(R.array.subtitles);
		m_subtitle = subtitles[loc_num];

		// story
		String[] stories = ctx.getResources().getStringArray(R.array.stories);
		m_story = stories[loc_num];

		// rating
		String[] ratings = ctx.getResources().getStringArray(R.array.ratings);
		m_rating = Float.parseFloat(ratings[loc_num]);

		// on target distance
		String[] distances = ctx.getResources().getStringArray(R.array.distances);
		float slop = Float.parseFloat(distances[loc_num]);
		m_hotcold = new HotColdDistance(slop, m_loc);

		// todo
		// images

		// Get the data stored in the preferences (which is
		// primarily what locations have been found).
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		m_found = prefs.getBoolean(PREF_KEY_FOUND_PREFIX + loc_num, false);

	} // load_location_data (loc_num)



	/***************************
	 * Want a nice string for the latitude in degrees, minutes, and seconds
	 * that's suitable for human eyes?  This'll do ya!
	 */
	public String get_latitude_str() {
		return Location.convert(m_loc.getLatitude(), Location.FORMAT_SECONDS);
	}

	public String get_longitude_str() {
		return Location.convert(m_loc.getLongitude(), Location.FORMAT_SECONDS);
	}

	/***************************
	 * Returns how close we can be (in meters, of course) and still
	 * be "on target."
	 *
	 * preconditions:
	 * 		m_hotcold		Should be set (which is done when
	 * 						load_location_data() is called).
	 *
	 * @return	The distance allowable for "on target."
	 * 			0 if not data has been loaded.
	 */
	public float get_slop() {
		if (m_hotcold == null) {
			return 0f;
		}

		return m_hotcold.get_slop();
	} // get_slop()


}
