package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Rectf;

/** Interface for an object that returns ocean surface displacement and height.*/
interface Wave_Collision_Provider {
    /**
     * Computes sampling state.
     * @param i_displacedSamplingArea  The XZ rect in world space that bounds any collision queries.
     * @param i_minSpatialLength  Minimum width or length that we care about. Used to filter out high frequency waves as an optimisation.
     * @param o_samplingData  Result. Needs to be new'd in advance - passing a null pointer is not valid.
     * @return
     */
    boolean getSamplingData(Rectf i_displacedSamplingArea, float i_minSpatialLength, SamplingData o_samplingData);

    /** Clear sampling data state, call this when done with a state. */
    void returnSamplingData(SamplingData i_data);

    /** Samples displacement of ocean surface from the given world position. */
    boolean sampleDisplacement(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement);

    /** displacementValid in first,  velValid in second */
    long sampleDisplacementVel(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement, /*out bool o_displacementValid, out*/ Vector3f o_displacementVel/*, out bool o_velValid*/);

    /** Samples ocean surface height at given world position. */
    void sampleHeight(ReadableVector3f i_worldPos, SamplingData i_samplingData, SamplingHeight o_height);

    /** Sample ocean normal at an undisplaced world position.*/
    boolean sampleNormal(ReadableVector3f i_undisplacedWorldPos, SamplingData i_samplingData, Vector3f o_normal);

    /** Computes the position which will be displaced to the given world position.*/
    boolean computeUndisplacedPosition(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f undisplacedWorldPos);

    /**
     * Query water physical data at a set of points. Pass in null to any out parameters that are not required.
     * @param i_ownerHash   Unique ID for calling code. Typically acquired by calling hashCode().
     * @param i_samplingData  Sampling data to inform sampling, obtained by calling getSamplingData().
     * @param i_queryPoints   The world space points that will be queried.
     * @param o_resultHeights  Float array of water heights at the query positions. Pass null if this information is not required.
     * @param o_resultNorms Water normals at the query positions. Pass null if this information is not required.
     * @param o_resultVels Water surface velocities at the query positions. Pass null if this information is not required.
     * @return
     */
    int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, float[] o_resultHeights, Vector3f[] o_resultNorms, Vector3f[] o_resultVels);

    /**
     *  Query water physical data at a set of points. Pass in null to any out parameters that are not required.
     * @param i_ownerHash   Unique ID for calling code. Typically acquired by calling hashCode().
     * @param i_samplingData  Sampling data to inform sampling, obtained by calling getSamplingData().
     * @param i_queryPoints  The world space points that will be queried.
     * @param o_resultDisps  Displacement vectors for water surface points that will displace to the XZ coordinates of the query points. Water heights are given by sea level plus the y component of the displacement.
     * @param o_resultNorms  Water normals at the query positions. Pass null if this information is not required.
     * @param o_resultVels  Water surface velocities at the query positions. Pass null if this information is not required.
     * @return
     */
    int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms, Vector3f[] o_resultVels);

    /// <summary>
    /// Check if query results could be retrieved successfully using return code from Query() function
    /// </summary>
    boolean retrieveSucceeded(int queryStatus);

    /// <summary>
    /// Run diagnostics at a position.
    /// </summary>
    AvailabilityResult checkAvailability(ReadableVector3f i_worldPos, SamplingData i_samplingData);
}
