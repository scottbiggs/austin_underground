package com.sleepfuriously.biggstourofaustin;

import android.content.Context;
import android.location.Location;

/**
 * This class is used to control how far the device
 * is from a given Coordinates.  It does calculations
 * making the hot/cold distances relative to the accuracy
 * required for this the target.  And of course it
 * supplies messages when asked.
 *
 * This class doesn't really do that much, but it is
 * nice to encapsulate all this busy work.
 *
 */
public class HotColdDistance {

	//-----------------------------
	//	Constants
	//-----------------------------

	private float DEFAULT_SLOP = 3f;

	/**
	 * These distances tell the user how far they are from
	 * the Target.  But instead of simple distances, these are
	 * all relative to the slop value.  So this is simply a
	 * list of slop multipliers that you can use to calculate
	 * the described distance.
	 *
	 * 		slop * [this constant] = distance
	 */
	private float
		BURNING = 2f,
		VERY_HOT = 3.3f,
		HOT = 7f,
		WARMER = 30f,
		WARM = 130f,
		LUKE_WARM = 800f,
		COOL = 1600f,			// MILE (assuming default slop = 3
		COLD = 3200f,
		VERY_COLD = 16000f;



	//-----------------------------
	//	Class Data
	//-----------------------------

	private final static String TAG = "HotColdDistance";

	/**
	 * How many meters from the exact target Coordinates
	 * we can be and still considered, "there."  Used as
	 * the basis of many calculations.
	 */
	private float m_slop = DEFAULT_SLOP;

	/** Holds the target coordiantes */
	private Location m_target = null;


	//-----------------------------
	//	Methods
	//-----------------------------

	/******************************
	 * Constructor
	 *
	 * @param slop		How many meters off we can be
	 * 					and still be "on target."
	 */
	public HotColdDistance (float slop) {
		m_slop = slop;
	}

	/******************************
	 * Constructor
	 *
	 * @param slop		How many meters off we can be
	 * 					and still be "on target."
	 *
	 * @param target		The coordinates of the target.
	 */
	public HotColdDistance (float slop, Location target) {
		m_slop = slop;
		m_target = target;
	}


	/******************************/
	public void set_target (Location target) {
		m_target = target;
	}

	/******************************/
	public void set_slop (float slop) {
		m_slop = slop;
	}

	/******************************/
	public Location get_target() {
		return m_target;
	}

	/******************************/
	public float get_slop() {
		return m_slop;
	}


	/******************************
	 * Simply tells if the target is within our slop.
	 * Thus, it's "on target", right?  Duh.
	 *
	 * @param target		Location of the target.
	 *
	 * @return	TRUE iff distance from position to m_target
	 * 			is less than m_slop.
	 */
	public boolean within_slop (Location position) {
		return within_slop (position, m_target);
	}

	/******************************
	 * Easy way to see if two coordinates are within
	 * the slop distance.
	 *
	 * preconditions:
	 * 		m_slop needs to be set.
	 *
	 * @param position	One coordinate.
	 * @param target		The other coordinate.
	 *
	 * @return	TRUE iff the two coords are within slop
	 * 			distance from each other.
	 */
	public boolean within_slop (Location position,
								Location target) {
		float dist = position.distanceTo(target);
		if (dist <= m_slop)
			return true;

		return false;
	}


	/******************************
	 * This is the main thrust of this class.  Get a
	 * string appropriate for the distance between our
	 * Position and the Target.
	 *
	 * @param ctx	The context so this can access resources.
	 *
	 * @param position	The Position of the mobile device.
	 *
	 * @param target		The coordinates of where we're going.
	 *
	 * @return	A message for the user that'll help them know
	 * 			how close they are to their Target.
	 */
	public String get_msg (Context ctx,
						Location position, Location target) {
		float dist = position.distanceTo(target);

		if (dist <= m_slop)
			return ctx.getString(R.string.yay);
		if (dist <= m_slop * BURNING)
			return ctx.getString(R.string.burning);
		if (dist <= m_slop * VERY_HOT)
			return ctx.getString(R.string.very_hot);
		if (dist <= m_slop * HOT)
			return ctx.getString(R.string.hot);
		if (dist <= m_slop * WARMER)
			return ctx.getString(R.string.warmer);
		if (dist <= m_slop * WARM)
			return ctx.getString(R.string.warm);
		if (dist <= m_slop * LUKE_WARM)
			return ctx.getString(R.string.luke_warm);
		if (dist <= m_slop * COOL)
			return ctx.getString(R.string.cool);
		if (dist <= m_slop * COLD)
			return ctx.getString(R.string.cold);
		if (dist <= m_slop * VERY_COLD)
			return ctx.getString(R.string.very_cold);

		return ctx.getString(R.string.freezing);
	} // get_msg (ctx, position, target)


	/******************************
	 * This is the main thrust of this class.  Get a
	 * string appropriate for the distance between our
	 * Position and the target that this class has already
	 * stored.
	 *
	 * @param ctx	The context so this can access resources.
	 *
	 * @param position	The Position of the mobile device.
	 *
	 * @return	A message for the user that'll help them know
	 * 			how close they are to their Target.
	 */
	public String get_msg (Context ctx, Location position) {
		return get_msg (ctx, position, m_target);
	}
}
