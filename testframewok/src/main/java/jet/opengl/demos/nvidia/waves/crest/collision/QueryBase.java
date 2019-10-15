package jet.opengl.demos.nvidia.waves.crest.collision;

import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jet.opengl.demos.nvidia.waves.crest.MonoBehaviour;
import jet.opengl.demos.nvidia.waves.crest.helpers.AsyncGPUReadbackRequest;
import jet.opengl.demos.nvidia.waves.crest.helpers.PropertyWrapperComputeStandalone;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Provides heights and other physical data about the water surface. Works by uploading query positions to GPU and computing
 * the data and then transferring back the results asynchronously. An exception to this is water surface velocities - these can
 * not be computed on the GPU and are instead computed on the CPU by retaining last frames' query results and computing finite diffs.
 */
public abstract class QueryBase extends MonoBehaviour {
    protected int _kernelHandle;

    protected abstract String QueryShaderName();
//    protected abstract String QueryKernelName();

    static final int s_maxRequests = 4;
    static final int s_maxGuids = 64;

//    protected virtual ComputeShader ShaderProcessQueries => _shaderProcessQueries;
    GLSLProgram _shaderProcessQueries;
    PropertyWrapperComputeStandalone _wrapper;

    System.Action<AsyncGPUReadbackRequest> _dataArrivedAction;

        final int s_maxQueryCount = 4096;
    // Must match value in compute shader
        final int s_computeGroupSize = 64;
    public static boolean s_useComputeCollQueries = true;

    final static int sp_queryPositions_minGridSizes = Shader.PropertyToID("_QueryPositions_MinGridSizes");
    final static int sp_MeshScaleLerp = Shader.PropertyToID("_MeshScaleLerp");
    final static int sp_SliceCount = Shader.PropertyToID("_SliceCount");

    final float s_finiteDiffDx = 0.1f;

    BufferGL _computeBufQueries;
    BufferGL _computeBufResults;

    Vector3f[] _queryPosXZ_minGridSize = new Vector3f[s_maxQueryCount];

    /// <summary>
    /// Holds information about all query points. Maps from unique hash code to position in point array.
    /// </summary>
    class SegmentRegistrar
    {
//        public Dictionary<int, Vector2Int> _segments = new Dictionary<int, Vector2Int>();
        public  final HashMap<Integer, Vector2i> _segments = new HashMap<>();
        public int _numQueries = 0;
    }

    /// <summary>
    /// Since query results return asynchronously and may not return at all (in theory), we keep a ringbuffer
    /// of the registrars of the last frames so that when data does come back it can be interpreted correctly.
    /// </summary>
    class SegmentRegistrarRingBuffer
    {
        // Requests in flight plus 2 held values, plus one current
        final static int s_poolSize = s_maxRequests + 2 + 1;

        SegmentRegistrar[] _segments = new SegmentRegistrar[s_poolSize];

        public int _segmentRelease = 0;
        public int _segmentAcquire = 0;

        public SegmentRegistrar Current() // => _segments[_segmentAcquire];
        {
            return _segments[_segmentAcquire];
        }

        public SegmentRegistrarRingBuffer()
        {
            for (int i = 0; i < _segments.length; i++)
            {
                _segments[i] = new SegmentRegistrar();
            }
        }

        public void AcquireNew()
        {
            int lastIndex = _segmentAcquire;

            _segmentAcquire = (_segmentAcquire + 1) % _segments.length;

            // The last index should never increment and land on the first index - it should only happen the other way around.
            assert (_segmentAcquire != _segmentRelease) : "Segment registrar scratch exhausted.";

            _segments[_segmentAcquire]._numQueries = _segments[lastIndex]._numQueries;

            _segments[_segmentAcquire]._segments.clear();
            for (Map.Entry<Integer, Vector2i> segment : _segments[lastIndex]._segments.entrySet())
            {
                _segments[_segmentAcquire]._segments.put(segment.getKey(), segment.getValue());
            }
        }

        public void ReleaseLast()
        {
            _segmentRelease = (_segmentRelease + 1) % _segments.length;
        }

        public void RemoveRegistrations(int key)
        {
            // Remove the guid for all of the next spare segment registrars. However, don't touch the ones that are being
            // used for active requests.
            int i = _segmentAcquire;
            while (true)
            {
//                if (_segments[i]._segments.containsKey(key))
                {
                    _segments[i]._segments.remove(key);
                }

                i = (i + 1) % _segments.length;

                if (i == _segmentRelease)
                {
                    break;
                }
            }
        }

        public void ClearAvailable()
        {
            // Extreme approach - flush all segments for next spare registrars (but don't touch ones being used for active requests)
            int i = _segmentAcquire;
            while (true)
            {
                _segments[i]._segments.clear();
                _segments[i]._numQueries = 0;

                i = (i + 1) % _segments.length;

                if (i == _segmentRelease)
                {
                    break;
                }
            }
        }

        public void ClearAll()
        {
            for (int i = 0; i < _segments.length; i++)
            {
                _segments[i]._numQueries = 0;
                _segments[i]._segments.clear();
            }
        }
    }

    SegmentRegistrarRingBuffer _segmentRegistrarRingBuffer = new SegmentRegistrarRingBuffer();

    ByteBuffer _queryResults;
    float _queryResultsTime = -1f;
    HashMap<Integer, Vector2i> _resultSegments;

    ByteBuffer _queryResultsLast;
    float _queryResultsTimeLast = -1f;
    HashMap<Integer, Vector2i> _resultSegmentsLast;

    class ReadbackRequest
    {
        public AsyncGPUReadbackRequest _request;
        public float _dataTimestamp;
        public HashMap<Integer, Vector2i> _segments;
    }

    ArrayList<ReadbackRequest> _requests = new ArrayList<>();

    public interface QueryStatus
    {
        int
        OK = 0,
        RetrieveFailed = 1,
        PostFailed = 2,
        NotEnoughDataForVels = 4,
        VelocityDataInvalidated = 8,
        InvalidDtForVelocity = 16;
    }

    protected abstract void BindInputsAndOutputs(PropertyWrapperComputeStandalone wrapper, BufferGL resultsBuffer);

    /// <summary>
    /// Takes a unique request ID and some world space XZ positions, and computes the displacement vector that lands at this position,
    /// to a good approximation. The world space height of the water at that position is then SeaLevel + displacement.y.
    /// </summary>
    protected boolean UpdateQueryPoints(int i_ownerHash, SamplingData i_samplingData, Vector3f[] queryPoints, Vector3f[] queryNormals)
    {
        boolean segmentRetrieved = false;
        Vector2i segment;

        // We'll send in 3 points to get normals
        int countPts = (queryPoints != null ? queryPoints.length : 0);
        int countNorms = (queryNormals != null ? queryNormals.length : 0);
        int countTotal = countPts + countNorms * 3;

        segment = _segmentRegistrarRingBuffer.Current()._segments.get(i_ownerHash);

        if (/*_segmentRegistrarRingBuffer.Current()._segments.TryGetValue(i_ownerHash, out segment)*/ segment != null)
        {
            int segmentSize = segment.y - segment.x + 1;
            if (segmentSize == countTotal)
            {
                segmentRetrieved = true;
            }
            else
            {
                _segmentRegistrarRingBuffer.Current()._segments.remove(i_ownerHash);
            }
        }

        if (countTotal == 0)
        {
            // No query data
            return false;
        }

        if (!segmentRetrieved)
        {
            if (_segmentRegistrarRingBuffer.Current()._segments.size() >= s_maxGuids)
            {
                LogUtil.e(LogUtil.LogType.DEFAULT, "Too many guids registered with CollProviderCompute. Increase s_maxGuids.");
                return false;
            }

            segment = new Vector2i();
            segment.x = _segmentRegistrarRingBuffer.Current()._numQueries;
            segment.y = segment.x + countTotal - 1;
            _segmentRegistrarRingBuffer.Current()._segments.put(i_ownerHash, segment);

            _segmentRegistrarRingBuffer.Current()._numQueries += countTotal;

            //Debug.Log("Added points for " + guid);
        }

        // The smallest wavelengths should repeat no more than twice across the smaller spatial length. Unless we're
        // in the last LOD - then this is the best we can do.
        // i_samplingData._minSpatialLength
        float minWavelength = i_samplingData._minSpatialLength / 2f;
        float minGridSize = minWavelength / OceanRenderer.Instance.MinTexelsPerWave;

        for (int pointi = 0; pointi < countPts; pointi++)
        {
            _queryPosXZ_minGridSize[pointi + segment.x].x = queryPoints[pointi].x;
            _queryPosXZ_minGridSize[pointi + segment.x].y = queryPoints[pointi].z;
            _queryPosXZ_minGridSize[pointi + segment.x].z = minGridSize;
        }

        // To compute each normal, post 3 query points
        for (int normi = 0; normi < countNorms; normi++)
        {
            int arrIdx = segment.x + countPts + 3 * normi;

            _queryPosXZ_minGridSize[arrIdx + 0].x = queryNormals[normi].x;
            _queryPosXZ_minGridSize[arrIdx + 0].y = queryNormals[normi].z;
            _queryPosXZ_minGridSize[arrIdx + 0].z = minGridSize;

            _queryPosXZ_minGridSize[arrIdx + 1].x = queryNormals[normi].x + s_finiteDiffDx;
            _queryPosXZ_minGridSize[arrIdx + 1].y = queryNormals[normi].z;
            _queryPosXZ_minGridSize[arrIdx + 1].z = minGridSize;

            _queryPosXZ_minGridSize[arrIdx + 2].x = queryNormals[normi].x;
            _queryPosXZ_minGridSize[arrIdx + 2].y = queryNormals[normi].z + s_finiteDiffDx;
            _queryPosXZ_minGridSize[arrIdx + 2].z = minGridSize;
        }

        return true;
    }

    /// <summary>
    /// Signal that we're no longer servicing queries. Note this leaves an air bubble in the query buffer.
    /// </summary>
    public void RemoveQueryPoints(int guid)
    {
        _segmentRegistrarRingBuffer.RemoveRegistrations(guid);
    }

    /// <summary>
    /// Remove air bubbles from the query array. Currently this lazily just nukes all the registered
    /// query IDs so they'll be recreated next time (generating garbage). TODO..
    /// </summary>
    public void CompactQueryStorage()
    {
        _segmentRegistrarRingBuffer.ClearAvailable();
    }

    /// <summary>
    /// Copy out displacements, heights, normals. Pass null if info is not required.
    /// </summary>
    protected boolean RetrieveResults(int guid, Vector3f[] displacements, float[] heights, Vector3f[] normals)
    {
        if (_resultSegments == null)
        {
            return false;
        }

        // Check if there are results that came back for this guid
        Vector2i segment = _resultSegments.get(guid);
        if (/*!_resultSegments.TryGetValue(guid, out segment)*/segment == null)
        {
            // Guid not found - no result
            return false;
        }

        int countPoints = 0;
        if (displacements != null) countPoints = displacements.length;
        if (heights != null) countPoints = heights.length;
        if (displacements != null && heights != null) assert(displacements.length == heights.length);
        int countNorms = (normals != null ? normals.length : 0);
        int countTotal = countPoints + countNorms * 3;

        if (countPoints > 0)
        {
            // Retrieve Results
            if (displacements != null)
            {
//                _queryResults.Slice(segment.x, countPoints).CopyTo(displacements);
                _queryResults.position(segment.x * Vector3f.SIZE);
                for(int i = 0; i < countPoints; i++)
                {
                    displacements[i].load(_queryResults);
                }
            }

            // Retrieve Result heights
            if (heights != null)
            {
                int seaLevel = OceanRenderer.Instance.SeaLevel;
                for (int i = 0; i < countPoints; i++)
                {
                    float y = _queryResults.getFloat((i + segment.x) * Vector3f.SIZE + 4);
                    heights[i] = seaLevel + y /*_queryResults[i + segment.x].y*/;
                }
            }
        }

        if (countNorms > 0)
        {
            int firstNorm = segment.x + countPoints;

//            int dx = -Vector3.right * s_finiteDiffDx;
//            int dz = -Vector3.forward * s_finiteDiffDx;

            Vector3f p = new Vector3f();
            Vector3f px = new Vector3f();
            Vector3f pz = new Vector3f();
            for (int i = 0; i < countNorms; i++)
            {
//                var p = _queryResults[firstNorm + 3 * i + 0];
//                var px = dx + _queryResults[firstNorm + 3 * i + 1];
//                var pz = dz + _queryResults[firstNorm + 3 * i + 2];
                load3(p, _queryResults, firstNorm + 3 * i + 0);
                load3(px, _queryResults, firstNorm + 3 * i + 1);
                load3(pz, _queryResults, firstNorm + 3 * i + 2);

                px.x -= s_finiteDiffDx;
                pz.z -= s_finiteDiffDx;

//                normals[i] = Vector3.Cross(p - px, p - pz).normalized;
//                normals[i].y *= -1f;
                normals[i] = Vector3f.computeNormal(p, px, pz, normals[i]);
                normals[i].normalise();
            }
        }

        return true;
    }

    private static void load3(Vector3f out, ByteBuffer buf, int v3Pos)
    {
        out.x = buf.getFloat(v3Pos * Vector3f.SIZE);
        out.y = buf.getFloat(v3Pos * Vector3f.SIZE + 4);
        out.z = buf.getFloat(v3Pos * Vector3f.SIZE + 8);
    }

    /// <summary>
    /// Compute time derivative of the displacements by calculating difference from last query. More complicated than it would seem - results
    /// may not be available in one or both of the results, or the query locations in the array may change.
    /// </summary>
    protected int CalculateVelocities(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPositions, Vector3f[] results)
    {
        // Need at least 2 returned results to do finite difference
        if (_queryResultsTime < 0f || _queryResultsTimeLast < 0f)
        {
            return 1;
        }

        Vector2i segment = _resultSegments.get(i_ownerHash);
        if (/*!_resultSegments.TryGetValue(i_ownerHash, out segment)*/segment == null)
        {
            return QueryStatus.RetrieveFailed;
        }

        Vector2i segmentLast = _resultSegmentsLast.get(i_ownerHash);
        if (/*!_resultSegmentsLast.TryGetValue(i_ownerHash, out segmentLast)*/segmentLast == null)
        {
            return QueryStatus.NotEnoughDataForVels;
        }

        if ((segment.y - segment.x) != (segmentLast.y - segmentLast.x))
        {
            // Number of queries changed - can't handle that
            return QueryStatus.VelocityDataInvalidated;
        }

        float dt = _queryResultsTime - _queryResultsTimeLast;
        if (dt < 0.0001f)
        {
            return (int)QueryStatus.InvalidDtForVelocity;
        }

        int count = results.length;
        for (int i = 0; i < count; i++)
        {
//            results[i] = (_queryResults[i + segment.x] - _queryResultsLast[i + segmentLast.x]) / dt;
            int offset = (i + segment.x) * Vector3f.SIZE;
            results[i].x = (_queryResults.getFloat(offset) - _queryResultsLast.getFloat(offset))/dt;  offset += 4;
            results[i].y = (_queryResults.getFloat(offset) - _queryResultsLast.getFloat(offset))/dt;  offset += 4;
            results[i].z = (_queryResults.getFloat(offset) - _queryResultsLast.getFloat(offset))/dt;
        }

        return 0;
    }

    // This needs to run in Update()
    // - It needs to run before OceanRenderer.LateUpdate, because the latter will change the LOD positions/scales, while we will read
    // the last frames displacements.
    // - It should run after FixedUpdate, as physics objects will update query points there. Also it computes the displacement timestamps
    // using Time.time and Time.deltaTime, which would be incorrect if it were in FixedUpdate.
    void Update(float time, float deltaTime)
    {
        if (_segmentRegistrarRingBuffer.Current()._numQueries > 0)
        {
            ExecuteQueries();

            // Remove oldest requests if we have hit the limit
            while (_requests.size() >= s_maxQueryCount)
            {
                _requests.remove(0);
            }

            ReadbackRequest request = new ReadbackRequest();
            request._dataTimestamp = time - deltaTime;
            request._request = AsyncGPUReadback.Request(_computeBufResults, _dataArrivedAction);
            request._segments = _segmentRegistrarRingBuffer.Current()._segments;
            _requests.add(request);

            _segmentRegistrarRingBuffer.AcquireNew();
        }
    }

    void ExecuteQueries()
    {
        _computeBufQueries.SetData(_queryPosXZ_minGridSize, 0, 0, _segmentRegistrarRingBuffer.Current()._numQueries);
        _shaderProcessQueries.SetBuffer(_kernelHandle, sp_queryPositions_minGridSizes, _computeBufQueries);
        BindInputsAndOutputs(_wrapper, _computeBufResults);

        // LOD 0 is blended in/out when scale changes, to eliminate pops
        float needToBlendOutShape = OceanRenderer.Instance.ScaleCouldIncrease;
        float meshScaleLerp = needToBlendOutShape>0 ? OceanRenderer.Instance.ViewerAltitudeLevelAlpha : 0f;
        _shaderProcessQueries.SetFloat(sp_MeshScaleLerp, meshScaleLerp);

        _shaderProcessQueries.SetFloat(sp_SliceCount, OceanRenderer.Instance.CurrentLodCount);

        int numGroups = (int)Math.ceil((float)_segmentRegistrarRingBuffer.Current()._numQueries / (float)s_computeGroupSize) * s_computeGroupSize;
        _shaderProcessQueries.Dispatch(_kernelHandle, numGroups, 1, 1);
    }

    /// <summary>
    /// Called when a compute buffer has been read back from the GPU to the CPU.
    /// </summary>
    void DataArrived(AsyncGPUReadbackRequest req)
    {
        // Can get callbacks after disable, so detect this.
        if (_queryResults == null)
        {
            _requests.clear();
            return;
        }

        // Remove any error requests
        for (int i = _requests.size() - 1; i >= 0; --i)
        {
            if (_requests.get(i)._request.hasError())
            {
                _requests.remove(i);
                _segmentRegistrarRingBuffer.ReleaseLast();
            }
        }

        // Find the last request that was completed
        int lastDoneIndex = _requests.size() - 1;
        while (lastDoneIndex >= 0 && !_requests.get(lastDoneIndex)._request.done())
        {
            --lastDoneIndex;
        }

        // If there is a completed request, process it
        if (lastDoneIndex >= 0)
        {
            // Update "last" results
//            Swap(ref _queryResults, ref _queryResultsLast);
            {
                ByteBuffer tmp = _queryResults;
                _queryResults = _queryResultsLast;
                _queryResultsLast = tmp;
            }

            _queryResultsTimeLast = _queryResultsTime;
            _resultSegmentsLast = _resultSegments;

             _requests.get(lastDoneIndex)._request.GetData/*<Vector3>*/(_queryResults);
//            data.CopyTo(_queryResults);
            _queryResultsTime = _requests.get(lastDoneIndex)._dataTimestamp;
            _resultSegments = _requests.get(lastDoneIndex)._segments;
        }

        // Remove all the requests up to the last completed one
        for (int i = lastDoneIndex; i >= 0; --i)
        {
            _requests.remove(i);
            _segmentRegistrarRingBuffer.ReleaseLast();
        }
    }

    protected void OnEnable()
    {
        _dataArrivedAction = new System.Action<AsyncGPUReadbackRequest>(DataArrived);

        _shaderProcessQueries = //Resources.Load<ComputeShader>(QueryShaderName());
                GLSLProgram.createProgram(QueryShaderName(), null);
        _kernelHandle = _shaderProcessQueries./*FindKernel(QueryKernelName())*/getProgram();
        _wrapper = new PropertyWrapperComputeStandalone(_shaderProcessQueries, _kernelHandle);

        _computeBufQueries = new BufferGL(/*s_maxQueryCount, 12, ComputeBufferType.Default*/);
        _computeBufResults = new BufferGL(/*s_maxQueryCount, 12, ComputeBufferType.Default*/);

        _computeBufQueries.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, s_maxQueryCount * 12 , null, GLenum.GL_DYNAMIC_COPY);
        _computeBufResults.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, s_maxQueryCount * 12 , null, GLenum.GL_DYNAMIC_COPY);

        _queryResults = BufferUtils.createByteBuffer(s_maxQueryCount * Vector3f.SIZE); // new NativeArray<Vector3>(, Allocator.Persistent, NativeArrayOptions.ClearMemory);
        _queryResultsLast = BufferUtils.createByteBuffer(s_maxQueryCount * Vector3f.SIZE); //new NativeArray<Vector3>(s_maxQueryCount, Allocator.Persistent, NativeArrayOptions.ClearMemory);
    }

    protected void OnDisable()
    {
        _computeBufQueries.dispose();
        _computeBufResults.dispose();

//        _queryResults.Dispose();
//        _queryResultsLast.Dispose();

        _segmentRegistrarRingBuffer.ClearAll();
    }

    public int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
        int result = (int)QueryStatus.OK;

        if (!UpdateQueryPoints(i_ownerHash, i_samplingData, i_queryPoints, o_resultNorms != null ? i_queryPoints : null))
        {
            result |= (int)QueryStatus.PostFailed;
        }

        if (!RetrieveResults(i_ownerHash, o_resultDisps, null, o_resultNorms))
        {
            result |= (int)QueryStatus.RetrieveFailed;
        }

        if (o_resultVels != null)
        {
            result |= CalculateVelocities(i_ownerHash, i_samplingData, i_queryPoints, o_resultVels);
        }

        return result;
    }

    public boolean RetrieveSucceeded(int queryStatus)
    {
        return (queryStatus & (int)QueryStatus.RetrieveFailed) == 0;
    }
}
