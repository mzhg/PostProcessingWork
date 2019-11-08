package jet.opengl.demos.nvidia.waves.crest.collision;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.nvidia.waves.crest.AvailabilityResult;
import jet.opengl.demos.nvidia.waves.crest.SamplingData;
import jet.opengl.postprocessing.util.Rectf;

/** Interface for an object that returns ocean surface displacement and height.*/
public interface ICollProvider {
    /// <summary>
    /// Computes sampling state.
    /// </summary>
    /// <param name="i_displacedSamplingArea">The XZ rect in world space that bounds any collision queries.</param>
    /// <param name="i_minSpatialLength">Minimum width or length that we care about. Used to filter out high frequency waves as an optimisation.</param>
    /// <param name="o_samplingData">Result. Needs to be new'd in advance - passing a null pointer is not valid.</param>
    boolean GetSamplingData(Rectf i_displacedSamplingArea, float i_minSpatialLength, SamplingData o_samplingData);

    /** Clear sampling data state, call this when done with a state. */
    void ReturnSamplingData(SamplingData i_data);

    /// <summary>
    /// Samples displacement of ocean surface from the given world position.
    /// </summary>
//    [Obsolete("This API is deprecated. Use the 'Query' APIs instead.")]
    boolean SampleDisplacement(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement);

    /// <summary>
    /// Query water physical data at a set of points. Pass in null to any out parameters that are not required.
    /// </summary>
    /// <param name="i_ownerHash">Unique ID for calling code. Typically acquired by calling GetHashCode().</param>
    /// <param name="i_samplingData">Sampling data to inform sampling, obtained by calling GetSamplingData().</param>
    /// <param name="i_queryPoints">The world space points that will be queried.</param>
    /// <param name="o_resultHeights">Float array of water heights at the query positions. Pass null if this information is not required.</param>
    /// <param name="o_resultNorms">Water normals at the query positions. Pass null if this information is not required.</param>
    /// <param name="o_resultVels">Water surface velocities at the query positions. Pass null if this information is not required.</param>
    int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, float[] o_resultHeights, Vector3f[] o_resultNorms, Vector3f[] o_resultVels);

    /// <summary>
    /// Query water physical data at a set of points. Pass in null to any out parameters that are not required.
    /// </summary>
    /// <param name="i_ownerHash">Unique ID for calling code. Typically acquired by calling GetHashCode().</param>
    /// <param name="i_samplingData">Sampling data to inform sampling, obtained by calling GetSamplingData().</param>
    /// <param name="i_queryPoints">The world space points that will be queried.</param>
    /// <param name="o_resultDisps">Displacement vectors for water surface points that will displace to the XZ coordinates of the query points. Water heights are given by sea level plus the y component of the displacement.</param>
    /// <param name="o_resultNorms">Water normals at the query positions. Pass null if this information is not required.</param>
    /// <param name="o_resultVels">Water surface velocities at the query positions. Pass null if this information is not required.</param>
    int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms, Vector3f[] o_resultVels);

    /// <summary>
    /// Check if query results could be retrieved successfully using return code from Query() function
    /// </summary>
    boolean RetrieveSucceeded(int queryStatus);

    /**
     * bool in the first. float in the second.
     * @param i_worldPos
     * @param i_samplingData
     * @return
     */
    long SampleHeight(ReadableVector3f i_worldPos, SamplingData i_samplingData);

    /// <summary>
    /// Run diagnostics at a position.
    /// </summary>
    AvailabilityResult CheckAvailability(ReadableVector3f i_worldPos, SamplingData i_samplingData);
}
