package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Provides heights and other physical data about the water surface. Works by uploading query positions to GPU and computing
 * the data and then transferring back the results asynchronously. An exception to this is water surface velocities - these can
 * not be computed on the GPU and are instead computed on the CPU by retaining last frames' query results and computing finite diffs.
 */
abstract class Wave_Query_Base {
    protected Technique _kernelHandle;

    protected abstract String QueryShaderName();

    static final int s_maxRequests = 4;
    static final int s_maxGuids = 64;

//    protected virtual ComputeShader ShaderProcessQueries => _shaderProcessQueries;
//    Technique _shaderProcessQueries;

//    System.Action<AsyncGPUReadbackRequest> _dataArrivedAction;

    private static     final int s_maxQueryCount = 4096;
    // Must match value in compute shader
    private static    final int s_computeGroupSize = 64;
    public static boolean s_useComputeCollQueries = true;

    private final static int sp_queryPositions_minGridSizes =0; // Shader.PropertyToID("_QueryPositions_MinGridSizes");
    private final static int sp_MeshScaleLerp = 1; //Shader.PropertyToID("_MeshScaleLerp");
    private final static int sp_SliceCount = 2; // Shader.PropertyToID("_SliceCount");

    private static final float s_finiteDiffDx = 0.1f;

    private BufferGL _computeBufQueries;
    private BufferGL _computeBufResults;

    private Vector3f[] _queryPosXZ_minGridSize = CommonUtil.initArray(new Vector3f[s_maxQueryCount]);

    protected Wave_CDClipmap m_Clipmap;
    protected Wave_Simulation m_Simulation;

    private final Wave_Simulation_ShaderData m_ShaderData = new Wave_Simulation_ShaderData();

    public void init(Wave_CDClipmap clipmap, Wave_Simulation simulation){
        m_Clipmap = clipmap;
        m_Simulation = simulation;

        OnEnable();
    }

    /** Holds information about all query points. Maps from unique hash code to position in point array.*/
    private final class SegmentRegistrar
    {
//        public Dictionary<int, Vector2Int> _segments = new Dictionary<int, Vector2Int>();
        public  final HashMap<Integer, Vector2i> _segments = new HashMap<>();
        public int _numQueries = 0;
    }

    /// <summary>
    /// Since query results return asynchronously and may not return at all (in theory), we keep a ringbuffer
    /// of the registrars of the last frames so that when data does come back it can be interpreted correctly.
    /// </summary>
    private final class SegmentRegistrarRingBuffer
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

    private SegmentRegistrarRingBuffer _segmentRegistrarRingBuffer = new SegmentRegistrarRingBuffer();

    private Vector3f[] _queryResults;
    private float _queryResultsTime = -1f;
    private HashMap<Integer, Vector2i> _resultSegments;

    private Vector3f[] _queryResultsLast;
    private float _queryResultsTimeLast = -1f;
    private HashMap<Integer, Vector2i> _resultSegmentsLast;

    private final class ReadbackRequest
    {
        public AsyncGPUReadbackRequest _request;
        public float _dataTimestamp;
        public HashMap<Integer, Vector2i> _segments;
    }

    private ArrayList<ReadbackRequest> _requests = new ArrayList<>();

    protected interface QueryStatus
    {
        int
        OK = 0,
        RetrieveFailed = 1,
        PostFailed = 2,
        NotEnoughDataForVels = 4,
        VelocityDataInvalidated = 8,
        InvalidDtForVelocity = 16;
    }

    protected abstract void bindInputsAndOutputs(Wave_Simulation_ShaderData wrapper, BufferGL resultsBuffer);

    /**
     * Takes a unique request ID and some world space XZ positions, and computes the displacement vector that lands at this position,
     * to a good approximation. The world space height of the water at that position is then SeaLevel + displacement.y.
     * @param i_ownerHash
     * @param i_samplingData
     * @param queryPoints
     * @param queryNormals
     * @return
     */
    protected boolean updateQueryPoints(int i_ownerHash, SamplingData i_samplingData, Vector3f[] queryPoints, Vector3f[] queryNormals)
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
        float minGridSize = minWavelength / m_Clipmap.getMinTexelsPerWave();

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

    /** Signal that we're no longer servicing queries. Note this leaves an air bubble in the query buffer. */
    public void RemoveQueryPoints(int guid)
    {
        _segmentRegistrarRingBuffer.RemoveRegistrations(guid);
    }

    /**
     * Remove air bubbles from the query array. Currently this lazily just nukes all the registered
     * query IDs so they'll be recreated next time (generating garbage). TODO..
     */
    public void CompactQueryStorage()
    {
        _segmentRegistrarRingBuffer.ClearAvailable();
    }

    /** Copy out displacements, heights, normals. Pass null if info is not required.*/
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
                for(int i = 0; i < countPoints; i++)
                {
                    if(displacements[i] == null)
                        displacements[i] = new Vector3f(_queryResults[i+segment.x]);
                    else
                        displacements[i].set(_queryResults[i+segment.x]);
                }
            }

            // Retrieve Result heights
            if (heights != null)
            {
                float seaLevel = m_Clipmap.getSeaLevel();
                for (int i = 0; i < countPoints; i++)
                {
                    heights[i] = seaLevel + _queryResults[i + segment.x].y;
                }
            }
        }

        if (countNorms > 0)
        {
            int firstNorm = segment.x + countPoints;

//            var dx = -Vector3.right * s_finiteDiffDx;
//            var dz = -Vector3.forward * s_finiteDiffDx;
            Vector3f dx = CacheBuffer.getCachedVec3();
            Vector3f dz = CacheBuffer.getCachedVec3();
            for (int i = 0; i < countNorms; i++)
            {
                dx.set(-s_finiteDiffDx, 0,0);
                dz.set(0, 0,-s_finiteDiffDx);

                ReadableVector3f p = _queryResults[firstNorm + 3 * i + 0];
//                var px = dx + _queryResults[firstNorm + 3 * i + 1];
//                var pz = dz + _queryResults[firstNorm + 3 * i + 2];
                Vector3f.add(dx,_queryResults[firstNorm + 3 * i + 1], dx);
                Vector3f.add(dz,_queryResults[firstNorm + 3 * i + 2], dz);

//                normals[i] = Vector3.Cross(p - px, p - pz).normalized;
                Vector3f.computeNormal(p, dx, dz, normals[i]);
                normals[i].normalise();
//                normals[i].y *= -1f;  todo
            }

            CacheBuffer.free(dx);
            CacheBuffer.free(dz);
        }

        return true;
    }

    /**
     * Compute time derivative of the displacements by calculating difference from last query. More complicated than it would seem - results
     * may not be available in one or both of the results, or the query locations in the array may change.
     * @param i_ownerHash
     * @param i_samplingData
     * @param i_queryPositions
     * @param results
     * @return
     */
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
            return QueryStatus.InvalidDtForVelocity;
        }

        int count = results.length;
        for (int i = 0; i < count; i++)
        {
//            results[i] = (_queryResults[i + segment.x] - _queryResultsLast[i + segmentLast.x]) / dt;
            Vector3f.sub(_queryResults[i + segment.x], _queryResultsLast[i + segmentLast.x], results[i]);
            results[i].scale(1.0f/dt);
        }

        return 0;
    }

    // This needs to run in Update()
    // - It needs to run before OceanRenderer.LateUpdate, because the latter will change the LOD positions/scales, while we will read
    // the last frames displacements.
    // - It should run after FixedUpdate, as physics objects will update query points there. Also it computes the displacement timestamps
    // using Time.time and Time.deltaTime, which would be incorrect if it were in FixedUpdate.
    void update(float time, float deltaTime)
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
            request._request = AsyncGPUReadbackRequest.create(m_Simulation, _computeBufResults);
            request._segments = _segmentRegistrarRingBuffer.Current()._segments;
            _requests.add(request);

            _segmentRegistrarRingBuffer.AcquireNew();
        }
    }

    void ExecuteQueries()
    {
//        _computeBufQueries.SetData(_queryPosXZ_minGridSize, 0, 0, _segmentRegistrarRingBuffer.Current()._numQueries);
        ByteBuffer bytes = CacheBuffer.wrap(Vector3f.SIZE, _queryPosXZ_minGridSize, 0, _segmentRegistrarRingBuffer.Current()._numQueries);
        _computeBufQueries.update(0, bytes);

//        _shaderProcessQueries.SetBuffer(_kernelHandle, sp_queryPositions_minGridSizes, _computeBufQueries);
        m_ShaderData._QueryPositions_MinGridSizes = _computeBufQueries;
        bindInputsAndOutputs(m_ShaderData, _computeBufResults);

        // LOD 0 is blended in/out when scale changes, to eliminate pops
        boolean needToBlendOutShape = m_Clipmap.scaleCouldIncrease();
        float meshScaleLerp = needToBlendOutShape ? m_Clipmap.getViewerAltitudeLevelAlpha() : 0f;
//        _shaderProcessQueries.SetFloat(sp_MeshScaleLerp, meshScaleLerp);
        m_ShaderData._MeshScaleLerp = meshScaleLerp;

//        _shaderProcessQueries.SetFloat(sp_SliceCount, OceanRenderer.Instance.CurrentLodCount);
        m_ShaderData._SliceCount = m_Clipmap.m_LodTransform.LodCount();

        int numGroups = (int)Math.ceil((float)_segmentRegistrarRingBuffer.Current()._numQueries / (float)s_computeGroupSize) * s_computeGroupSize;
//        _shaderProcessQueries.Dispatch(_kernelHandle, numGroups, 1, 1);
        _kernelHandle.enable(m_ShaderData);
        GLFuncProviderFactory.getGLFuncProvider().glDispatchCompute(numGroups, 1, 1);
    }

    /** Called when a compute buffer has been read back from the GPU to the CPU. */
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
                Vector3f[] tmp = _queryResults;
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

    private void OnEnable()
    {
//        _dataArrivedAction = new System.Action<AsyncGPUReadbackRequest>(DataArrived);
        m_Simulation.setAsyncGPUReadbackFinish(this::DataArrived);
//        _shaderProcessQueries = //Resources.Load<ComputeShader>(QueryShaderName());
//                GLSLProgram.createProgram(QueryShaderName(), null);
//        _kernelHandle = _shaderProcessQueries./*FindKernel(QueryKernelName())*/getProgram();

        _kernelHandle = ShaderManager.getInstance().getProgram(QueryShaderName());
//        _wrapper = new PropertyWrapperComputeStandalone(_shaderProcessQueries, _kernelHandle);

        _computeBufQueries = new BufferGL(/*s_maxQueryCount, 12, ComputeBufferType.Default*/);
        _computeBufResults = new BufferGL(/*s_maxQueryCount, 12, ComputeBufferType.Default*/);

        _computeBufQueries.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, s_maxQueryCount * 12 , null, GLenum.GL_DYNAMIC_COPY);
        _computeBufResults.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, s_maxQueryCount * 12 , null, GLenum.GL_DYNAMIC_COPY);

        _queryResults = CommonUtil.initArray(new Vector3f[s_maxQueryCount]);
        _queryResultsLast = CommonUtil.initArray(new Vector3f[s_maxQueryCount]);
    }

    protected void OnDisable()
    {
        _computeBufQueries.dispose();
        _computeBufResults.dispose();

//        _queryResults.Dispose();
//        _queryResultsLast.Dispose();

        _segmentRegistrarRingBuffer.ClearAll();
    }

    public int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
        int result = QueryStatus.OK;

        if (!updateQueryPoints(i_ownerHash, i_samplingData, i_queryPoints, o_resultNorms != null ? i_queryPoints : null))
        {
            result |= QueryStatus.PostFailed;
        }

        if (!RetrieveResults(i_ownerHash, o_resultDisps, null, o_resultNorms))
        {
            result |= QueryStatus.RetrieveFailed;
        }

        if (o_resultVels != null)
        {
            result |= CalculateVelocities(i_ownerHash, i_samplingData, i_queryPoints, o_resultVels);
        }

        return result;
    }

    public boolean retrieveSucceeded(int queryStatus)
    {
        return (queryStatus & QueryStatus.RetrieveFailed) == 0;
    }
}
