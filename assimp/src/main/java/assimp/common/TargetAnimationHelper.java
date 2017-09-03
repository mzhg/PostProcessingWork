package assimp.common;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

/** Helper class for the 3DS and ASE loaders to compute camera and spot light
 *  animations.<p>
 *
 * 3DS and ASE store the differently to Assimp - there is an animation
 * channel for the camera/spot light itself and a separate position
 * animation channels specifying the position of the camera/spot light
 * look-at target */
public class TargetAnimationHelper {

	private List<VectorKey> targetPositions, objectPositions;
	private final Vector3f fixedMain = new Vector3f();
	
	// ------------------------------------------------------------------
	/** Sets the target animation channel<p>
	 *
	 *  This channel specifies the position of the camera/spot light
	 *  target at a specific position.
	 *
	 *  @param _targetPositions Translation channel*/
	public void setTargetAnimationChannel (List<VectorKey> _targetPositions){
		targetPositions = _targetPositions;
	}


	// ------------------------------------------------------------------
	/** Sets the main animation channel
	 *
	 *  @param _objectPositions Translation channel */
	public void setMainAnimationChannel (List<VectorKey> _objectPositions){
		objectPositions = _objectPositions;
	}

	// ------------------------------------------------------------------
	/** Sets the main animation channel to a fixed value 
	 *
	 *  @param fixed Fixed value for the main animation channel*/
	public void setFixedMainAnimationChannel(Vector3f fixed){
		objectPositions = null; // just to avoid confusion
		fixedMain.set(fixed);
	}


	// ------------------------------------------------------------------
	/** Computes final animation channels
	 * @param distanceTrack Receive camera translation keys ... != null. */
	public void process( List<VectorKey> distanceTrack ){
		// TODO: in most cases we won't need the extra array
		List<VectorKey> real = null;
		List<VectorKey> fill = (distanceTrack == objectPositions ? (real = new ArrayList<VectorKey>()) : distanceTrack);
//		fill->reserve(std::max( objectPositions->size(), targetPositions->size() ));

		// Iterate through all object keys and interpolate their values if necessary.
		// Then get the corresponding target position, compute the difference
		// vector between object and target position. Then compute a rotation matrix
		// that rotates the base vector of the object coordinate system at that time
		// to match the diff vector. 

		Vector3f tmp = new Vector3f();
		KeyIterator iter = new KeyIterator(objectPositions,targetPositions,fixedMain, null);
		for (;!iter.finished();iter.next())
		{
			Vector3f  position  = iter.getCurPosition();
			Vector3f  tposition = iter.getCurTargetPosition();

			// diff vector
			Vector3f diff = Vector3f.sub(tposition, position, tmp);
			float f = diff.length();

			// output distance vector
			if (f > 0)
			{
				VectorKey v;
				fill.add(v = new VectorKey());
				v.mTime  = (float) iter.getCurTime();
				v.mValue .set(diff);

//				diff /= f;
				diff.scale(1.0f/f);
			}
			else
			{
				// FIXME: handle this
			}

			// diff is now the vector in which our camera is pointing
		}

		if (real != null) {
			distanceTrack.clear();
			distanceTrack.addAll(real);
		}
	}
	
	/** Helper class to iterate through all keys in an animation channel.<p>
	 *
	 *  Missing tracks are interpolated. This is a helper class for
	 *  TargetAnimationHelper, but it can be freely used for other purposes.
	*/
	private static final class KeyIterator{
		
		//! Did we reach the end?
		boolean reachedEnd;

		//! Represents the current position of the iterator
		Vector3f curPosition, curTargetPosition;
		
		Vector3f tmpPosition;
		Vector3f targetPosition;

		double curTime = -1.;

		//! Input tracks and the next key to process
		List<VectorKey> objPos,targetObjPos;

		int nextObjPos, nextTargetObjPos;
		List<VectorKey> defaultObjPos /*= new ArrayList<VectorKey>()*/;
		List<VectorKey> defaultTargetObjPos /*= new ArrayList<VectorKey>()*/;
		
		// ------------------------------------------------------------------
		/** Constructs a new key iterator
		 *
		 *  @param _objPos Object position track. May be null.
		 *  @param _targetObjPos Target object position track. May be null.
		 *  @param defaultObjectPos Default object position to be used if
		 *	  no animated track is available. May be null.
		 *  @param defaultTargetPos Default target position to be used if
		 *	  no animated track is available. May be null.
		 */
		KeyIterator(List<VectorKey> _objPos, List<VectorKey> _targetObjPos,
			    Vector3f defaultObjectPos, Vector3f defaultTargetPos){
			objPos = _objPos;
			targetObjPos = _targetObjPos;
			nextObjPos = 0;
			nextTargetObjPos = 0;
			
			// Generate default transformation tracks if necessary
			if (objPos == null || objPos.isEmpty())
			{
				defaultObjPos = new ArrayList<VectorKey>(1);
				defaultObjPos.add(new VectorKey());
				defaultObjPos.get(0).mTime  = 10e10f;

				if (defaultObjectPos != null)
					defaultObjPos.get(0).mValue.set(defaultObjectPos);

				objPos = defaultObjPos;
			}
			if (targetObjPos == null || targetObjPos.isEmpty())
			{
				defaultTargetObjPos = new ArrayList<VectorKey>(1);
				defaultTargetObjPos.add(new VectorKey());
				defaultTargetObjPos.get(0).mTime  = 10e10f;

				if (defaultTargetPos != null)
					defaultTargetObjPos.get(0).mValue.set(defaultTargetPos);

				targetObjPos = defaultTargetObjPos;
			}
		}

		// ------------------------------------------------------------------
		/** Returns true if all keys have been processed
		 */
		boolean finished() {return reachedEnd;}

		// ------------------------------------------------------------------
		/** Increment the iterator
		 */
		void next(){
			// If we are already at the end of all keyframes, return
			if (reachedEnd) {
				return;
			}

			// Now search in all arrays for the time value closest
			// to our current position on the time line
			double d0,d1;
			
			d0 = objPos.get      ( Math.min( nextObjPos, objPos.size()-1)             ).mTime;
			d1 = targetObjPos.get( Math.min( nextTargetObjPos, targetObjPos.size()-1) ).mTime;	
			
			// Easiest case - all are identical. In this
			// case we don't need to interpolate so we can
			// return earlier
			if ( d0 == d1 )
			{
				curTime = d0;
				curPosition = objPos.get(nextObjPos).mValue;
				curTargetPosition = targetObjPos.get(nextTargetObjPos).mValue;

				// increment counters
				if (objPos.size() != nextObjPos-1)
					++nextObjPos;

				if (targetObjPos.size() != nextTargetObjPos-1)
					++nextTargetObjPos;
			}

			// An object position key is closest to us
			else if (d0 < d1)
			{
				curTime = d0;

				// interpolate the other
				if (1 == targetObjPos.size() || nextTargetObjPos == 0)	{
					curTargetPosition = targetObjPos.get(0).mValue;
				}
				else
				{
					VectorKey last  = targetObjPos.get(nextTargetObjPos);
					VectorKey first = targetObjPos.get(nextTargetObjPos-1);

					targetPosition = curTargetPosition = Vector3f.mix(first.mValue, last.mValue, (float) (
						(curTime-first.mTime) / (last.mTime-first.mTime) ), targetPosition);
				}

				if (objPos.size() != nextObjPos-1)
					++nextObjPos;
			}
			// A target position key is closest to us
			else
			{
				curTime = d1;

				// interpolate the other
				if (1 == objPos.size() || nextObjPos == 0)	{
					curPosition = objPos.get(0).mValue;
				}
				else
				{
					VectorKey last  = objPos.get(nextObjPos);
					VectorKey first = objPos.get(nextObjPos-1);

					tmpPosition = curPosition = Vector3f.mix(first.mValue, last.mValue, (float) (
						(curTime-first.mTime) / (last.mTime-first.mTime)), tmpPosition);
				}

				if (targetObjPos.size() != nextTargetObjPos-1)
					++nextTargetObjPos;
			}

			if (nextObjPos >= objPos.size()-1 && nextTargetObjPos >= targetObjPos.size()-1)
			{
				// We reached the very last keyframe
				reachedEnd = true;
			}
		}

		// ------------------------------------------------------------------
		/** Getters to retrieve the current state of the iterator
		 */
		Vector3f getCurPosition() {return curPosition;}

		Vector3f getCurTargetPosition() {return curTargetPosition;}

		double getCurTime() {return curTime;}
	}
}
