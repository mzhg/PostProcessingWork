package jet.opengl.demos.nvidia.waves.crest.gpureadback;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import jet.opengl.demos.nvidia.waves.crest.MonoBehaviour;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.collision.AvailabilityResult;
import jet.opengl.demos.nvidia.waves.crest.collision.SamplingData;
import jet.opengl.demos.nvidia.waves.crest.helpers.AsyncGPUReadbackRequest;
import jet.opengl.demos.nvidia.waves.crest.helpers.IFloatingOrigin;
import jet.opengl.demos.nvidia.waves.crest.helpers.Time;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgr;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodTransform;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Rectf;

public class GPUReadbackBase<LodDataType extends LodDataMgr> extends MonoBehaviour implements IFloatingOrigin {
    public boolean _doReadback = true;

    protected LodDataType _lodComponent;

    /// <summary>
    /// Minimum floating object width. The larger the objects that will float, the lower the resolution of the read data.
    /// If an object is small, the highest resolution LODs will be sample for physics. This is an optimisation. Set to 0
    /// to disable this optimisation and always copy high res data.
    /// </summary>
    protected float _minGridSize = 0f;
    /// <summary>
    /// Similar to the minimum width, but this setting will exclude the larger LODs from being copied. Set to 0 to disable
    /// this optimisation and always copy low res data.
    /// </summary>
    protected float _maxGridSize = 0f;

    protected IReadbackSettingsProvider _settingsProvider;

    protected boolean CanUseLastTwoLODs (){ return true; }

    protected static class PerLodData
    {
        public ReadbackData _resultData;
        public ReadbackData _resultDataPrevFrame;
        public Queue<ReadbackRequest> _requests = new ArrayDeque<>();

        public int _lastUpdateFrame = -1;
        public boolean _activelyBeingRendered = true;
    }

    SortedListCachedArrays/*<float, PerLodData>*/ _perLodData = new SortedListCachedArrays/*<float, PerLodData>*/();

    protected static class ReadbackRequest
    {
        public AsyncGPUReadbackRequest _request;
        public LodTransform.RenderData _renderData;
        public float _time;
    }

    final static int MAX_REQUESTS = 4;

    TexFormat _textureFormat = TexFormat.NotSet;

    /// <summary>
    /// When we do a readback we will get the rendered state for the previous time. This is associated with Time.time
    /// at the previous frame. This member stores this value.
    /// </summary>
    float _prevFrameTime = 0f;

    protected void Start()
    {
        _lodComponent = OceanRenderer.Instance.GetComponent<LodDataType>();
        if (OceanRenderer.Instance.CurrentLodCount() <= (CanUseLastTwoLODs() ? 0 : 1))
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "No data components of type " + typeof(LodDataType).Name + " found in the scene. Disabling GPU readback.");
            enabled = false;
            return;
        }

        /*if (!SystemInfo.supportsAsyncGPUReadback)
        {
            Debug.LogError("This device does not support GPU readback. " + this.GetType().Name + " will be disabled.", this);
            enabled = false;
            return;
        }*/

        SetTextureFormat(_lodComponent.TextureFormat());
    }

    protected void Update()
    {
        UpdateGridSizes();

        ProcessRequestsInternal(true);

        _prevFrameTime = Time.time;
    }

    void UpdateGridSizes()
    {
        if (_settingsProvider != null)
        {
            Vector2f result = new Vector2f();
            _settingsProvider.GetMinMaxGridSizes(/*out _minGridSize, out _maxGridSize*/result);
            _minGridSize = result.x;
            _maxGridSize = result.y;
        }

        // Grid sizes are on powers of two - make sure at least one grid is always included
        _maxGridSize = Math.max(_maxGridSize, 1.999f * _minGridSize);
        assert(_maxGridSize > 0f);
    }

    public void ProcessRequests()
    {
        // can process any newly arrived requests but don't queue up new ones
        ProcessRequestsInternal(false);
    }

    void ProcessRequestsInternal(boolean runningFromUpdate)
    {
        OceanRenderer ocean = OceanRenderer.Instance;
        int lodCount = ocean.CurrentLodCount();

        // When viewer changes altitude, lods will start/stop updating. Mark ones that are/arent being rendered!
        for (int i = 0; i < _perLodData.KeyArray.length; i++)
        {
            int lastUsableIndex = CanUseLastTwoLODs() ? (lodCount - 1) : (lodCount - 3);

            _perLodData.ValueArray[i]._activelyBeingRendered =
                    _perLodData.KeyArray[i] >= ocean._lodTransform._renderData[0]._texelWidth &&
                            _perLodData.KeyArray[i] <= ocean._lodTransform._renderData[lastUsableIndex]._texelWidth;

            if (!_perLodData.ValueArray[i]._activelyBeingRendered)
            {
                // It would be cleaner to destroy these. However they contain a NativeArray with a non-negligible amount of data
                // which we don't want to alloc and dealloc willy nilly, so just mark as invalid by setting time to -1.
                _perLodData.ValueArray[i]._resultData._time = -1f;
                _perLodData.ValueArray[i]._resultDataPrevFrame._time = -1f;
            }
        }

        LodTransform lt = ocean._lodTransform;

        for (int lodIndex = 0; lodIndex < ocean.CurrentLodCount(); lodIndex++)
        {
            Float lodTexelWidth = lt._renderData[lodIndex]._texelWidth;

            // Don't add uninitialised data
            if (lodTexelWidth == 0f) continue;

            if (lodTexelWidth >= _minGridSize && (lodTexelWidth <= _maxGridSize || _maxGridSize == 0f))
            {
                TextureGL tex = _lodComponent.DataTexture();
                if (tex == null) continue;

                if (!_perLodData.containsKey(lodTexelWidth))
                {
                    PerLodData resultData = new PerLodData();
                    resultData._resultData = new ReadbackData();
                    resultData._resultDataPrevFrame = new ReadbackData();

                    // create native arrays
                    assert(_textureFormat != TexFormat.NotSet) :  "ReadbackLodData: Texture format must be set.";
                    int num = _textureFormat.ordinal() * tex.getWidth() * tex.getHeight();
                    if (resultData._resultData._data == null || resultData._resultData._data.capacity() != num)
                    {
                        resultData._resultData._data = BufferUtils.createShortBuffer(num);  //new NativeArray<ushort>(num, Allocator.Persistent);
                        resultData._resultDataPrevFrame._data = BufferUtils.createShortBuffer(num);  //new NativeArray<ushort>(num, Allocator.Persistent);
                    }

                    _perLodData.Add(lodTexelWidth, resultData);
                }

                PerLodData lodData = _perLodData.get(lodTexelWidth);

                if (lodData._activelyBeingRendered)
                {
                    // Only enqueue new requests at beginning of update turns out to be a good time to sample the textures to
                    // ensure everything in the frame is done.
                    if (runningFromUpdate)
                    {
                        EnqueueReadbackRequest(tex, lodIndex, lt._renderData[lodIndex], _prevFrameTime);
                    }

                    ProcessArrivedRequests(lodData);
                }
            }
        }
    }

    /// <summary>
    /// Request current contents of cameras shape texture. queue pattern inspired by: https://github.com/keijiro/AsyncCaptureTest
    /// </summary>
    void EnqueueReadbackRequest(TextureGL target, int lodIndex, LodTransform.RenderData renderData, float previousFrameTime)
    {
        if (!_doReadback)
        {
            return;
        }

        PerLodData lodData = _perLodData.get(renderData._texelWidth);

        // only queue up requests while time is advancing
        if (previousFrameTime <= lodData._resultData._time)
        {
            return;
        }

        if (lodData._requests.size() < MAX_REQUESTS)
        {
            lodData._requests.add(
                new ReadbackRequest
                (
                    _request = AsyncGPUReadback.Request(target, 0, 0, target.width, 0, target.height, lodIndex, 1),
                            _renderData = renderData,
                            _time = previousFrameTime,
                )
            );
        }
    }

    void ProcessArrivedRequests(PerLodData lodData) {
        Queue<ReadbackRequest> requests = lodData._requests;

        if (!lodData._activelyBeingRendered) {
            // Dump all requests :/. No point processing these, and we have marked the data as being invalid and don't
            // want any new data coming in and stomping the valid flag.
            requests.clear();
            return;
        }

        // Physics stuff may call update from FixedUpdate() - therefore check if this component was already
        // updated this frame.
        if (lodData._lastUpdateFrame == Time.frameCount) {
            return;
        }
        lodData._lastUpdateFrame = Time.frameCount;

        // remove any failed readback requests
        for (int i = 0; i < MAX_REQUESTS && requests.size() > 0; i++) {
            ReadbackRequest request = requests.peek();
            if (request._request.hasError()) {
                requests.remove();
            } else {
                break;
            }
        }

        // process current request queue
        if (requests.size() > 0) {
            ReadbackRequest request = requests.peek();
            if (request._request.done()) {
                requests.remove();

                // Eat up any more completed requests to squeeze out latency wherever possible
                ReadbackRequest nextRequest;
                while (requests.size() > 0 && (nextRequest = requests.peek())._request.done()) {
                    // Has error will be true if data already destroyed and is therefore unusable
                    if (!nextRequest._request.hasError()) {
                        request = nextRequest;
                    }
                    requests.remove();
                }

//                UnityEngine.Profiling.Profiler.BeginSample("Copy out readback data");

                ReadbackData result = lodData._resultData;
                ReadbackData resultLast = lodData._resultDataPrevFrame;

                // copy result into resultLast
                resultLast._renderData = result._renderData;
                resultLast._time = result._time;
//                Swap(ref result._data, ref resultLast._data);
                {
                    ShortBuffer tmp = result._data;
                    result._data = resultLast._data;
                    resultLast._data = tmp;
                }

                // copy new data into result
//                var data = request._request.GetData < ushort > ();
//                data.CopyTo(result._data);
                request._request.GetData(result._data);
                result._renderData = request._renderData;
                result._time = request._time;

//                UnityEngine.Profiling.Profiler.EndSample();
            }
        }
    }

    public enum TexFormat
    {
        NotSet,
        RHalf,
        RGHalf ,
        RGBAHalf,
    }

    public void SetTextureFormat(int fmt)
    {
        switch (fmt)
        {
            case GLenum.GL_R16F:
                _textureFormat = TexFormat.RHalf;
                break;
            case GLenum.GL_RG16F:
                _textureFormat = TexFormat.RGHalf;
                break;
            case GLenum.GL_RGBA16F:
                _textureFormat = TexFormat.RGBAHalf;
                break;
            default:
//                Debug.LogError("Unsupported texture format for readback: " + fmt.ToString(), this);
                throw new IllegalStateException("Unsupported texture format for readback: " + fmt);
        }
    }

    protected void OnDisable()
    {
        // free native array when component removed or destroyed
        for (PerLodData lodData : _perLodData.ValueArray)
        {
            if (lodData == null || lodData._resultData == null) continue;
            if (lodData._resultData._data != null) lodData._resultData._data = null;
            if (lodData._resultDataPrevFrame._data != null) lodData._resultDataPrevFrame._data = null;
        }

        _perLodData.clear();
    }

    public static class ReadbackData
    {
        public ShortBuffer _data;
        public LodTransform.RenderData _renderData;
        public float _time;

        public boolean Valid () { return _time >= 0f; }

        public boolean SampleARGB16(ReadableVector3f i_worldPos, Vector3f o_displacement)
        {
            if (!Valid()) { o_displacement.set(0,0,0); return false; }

            float xOffset = i_worldPos.getX() - _renderData._posSnapped.x;
            float zOffset = i_worldPos.getZ() - _renderData._posSnapped.z;
            float r = _renderData._texelWidth * _renderData._textureRes / 2f;
            if (Math.abs(xOffset) >= r || Math.abs(zOffset) >= r)
            {
                // outside of this collision data
                o_displacement.set(0,0,0);
                return false;
            }

            float u = 0.5f + 0.5f * xOffset / r;
            float v = 0.5f + 0.5f * zOffset / r;
            int x = (int)Math.floor(u * _renderData._textureRes);
            int y = (int)Math.floor(v * _renderData._textureRes);
            int idx = 4 * (y * (int)_renderData._textureRes + x);

            o_displacement.x = Numeric.convertHFloatToFloat(_data.get(idx + 0));
            o_displacement.y = Numeric.convertHFloatToFloat(_data.get(idx + 1));
            o_displacement.z = Numeric.convertHFloatToFloat(_data.get(idx + 2));

            return true;
        }

        public boolean InterpolateARGB16(ReadableVector3f i_worldPos, Vector3f o_displacement)
        {
            if (!Valid()) { o_displacement.set(0,0,0); return false; }

            float xOffset = i_worldPos.getX() - _renderData._posSnapped.x;
            float zOffset = i_worldPos.getZ() - _renderData._posSnapped.z;
            float r = _renderData._texelWidth * _renderData._textureRes / 2f;
            if (Math.abs(xOffset) >= r || Math.abs(zOffset) >= r)
            {
                // outside of this collision data
                o_displacement.set(0,0,0);
                return false;
            }

            float u = 0.5f + 0.5f * xOffset / r;
            float v = 0.5f + 0.5f * zOffset / r;
            float u_texels = Math.max(u * _renderData._textureRes - 0.5f, 0f);
            float v_texels = Math.max(v * _renderData._textureRes - 0.5f, 0f);

            int width = (int)_renderData._textureRes;

            int x0 = (int)Math.floor(u_texels);
            int x1 = Math.min(x0 + 1, width - 1);
            int z0 = (int)Math.floor(v_texels);
            int z1 = Math.min(z0 + 1, width - 1);

            int idx00 = 4 * (z0 * width + x0);
            int idx01 = 4 * (z0 * width + x1);
            int idx10 = 4 * (z1 * width + x0);
            int idx11 = 4 * (z1 * width + x1);

            float x00 = Numeric.convertHFloatToFloat(_data.get(idx00 + 0));
            float y00 = Numeric.convertHFloatToFloat(_data.get(idx00 + 1));
            float z00 = Numeric.convertHFloatToFloat(_data.get(idx00 + 2));
            float x01 = Numeric.convertHFloatToFloat(_data.get(idx01 + 0));
            float y01 = Numeric.convertHFloatToFloat(_data.get(idx01 + 1));
            float z01 = Numeric.convertHFloatToFloat(_data.get(idx01 + 2));
            float x10 = Numeric.convertHFloatToFloat(_data.get(idx10 + 0));
            float y10 = Numeric.convertHFloatToFloat(_data.get(idx10 + 1));
            float z10 = Numeric.convertHFloatToFloat(_data.get(idx10 + 2));
            float x11 = Numeric.convertHFloatToFloat(_data.get(idx11 + 0));
            float y11 = Numeric.convertHFloatToFloat(_data.get(idx11 + 1));
            float z11 = Numeric.convertHFloatToFloat(_data.get(idx11 + 2));

            float xf = u_texels % 1f;
            float zf = v_texels % 1f;
            o_displacement.x = Numeric.mix(Numeric.mix(x00, x01, xf), Numeric.mix(x10, x11, xf), zf);
            o_displacement.y = Numeric.mix(Numeric.mix(y00, y01, xf), Numeric.mix(y10, y11, xf), zf);
            o_displacement.z = Numeric.mix(Numeric.mix(z00, z01, xf), Numeric.mix(z10, z11, xf), zf);

            return true;
        }

        public boolean SampleRG16(ReadableVector3f i_worldPos, Vector2f flow)
        {
            if (!Valid()) { flow.set(0,0); return false; }

            float xOffset = i_worldPos.getX() - _renderData._posSnapped.x;
            float zOffset = i_worldPos.getZ() - _renderData._posSnapped.z;
            float r = _renderData._texelWidth * _renderData._textureRes / 2f;
            if (Math.abs(xOffset) >= r || Math.abs(zOffset) >= r)
            {
                // outside of this collision data
                flow.set(0,0);
                return false;
            }

            float u = 0.5f + 0.5f * xOffset / r;
            float v = 0.5f + 0.5f * zOffset / r;
            int x = (int)Math.floor(u * _renderData._textureRes);
            int y = (int)Math.floor(v * _renderData._textureRes);
            int idx = 2 * (y * (int)_renderData._textureRes + x);
            flow.x = Numeric.convertHFloatToFloat(_data.get(idx + 0));
            flow.y = Numeric.convertHFloatToFloat(_data.get(idx + 1));

            return true;
        }
    }

    /// <summary>
    /// Returns result of GPU readback of a LOD data. Do not hold onto the returned reference across frames.
    /// </summary>
    protected PerLodData GetData(float gridSize)
    {
        return _perLodData.get(gridSize);
    }

    /// <summary>
    /// Returns result of GPU readback of a LOD data. Do not hold onto the returned reference across frames.
    /// </summary>
    protected PerLodData GetData(Rectf sampleAreaXZ, float minSpatialLength)
    {
        PerLodData lastCandidate = null;

        for (int i = 0; i < _perLodData.KeyArray.length; i++)
        {
            PerLodData lodData = _perLodData.ValueArray[i];
            if (!lodData._activelyBeingRendered || lodData._resultData._time == -1f)
            {
                continue;
            }

            // Check that the region of interest is covered by this data
            Rectf wdcRect = lodData._resultData._renderData.RectXZ();
            // Shrink rect by 1 texel border - this is to make finite differences fit as well
            float texelWidth = _perLodData.KeyArray[i];
            wdcRect.x += texelWidth; wdcRect.y += texelWidth;
            wdcRect.width -= 2f * texelWidth; wdcRect.height -= 2f * texelWidth;
            if (!wdcRect.contains(sampleAreaXZ.x, sampleAreaXZ.y) || !wdcRect.contains(sampleAreaXZ.x+sampleAreaXZ.width, sampleAreaXZ.y+sampleAreaXZ.height))
            {
                continue;
            }

            // This data covers our required area, so store it as a potential candidate
            lastCandidate = lodData;

            // The smallest wavelengths should repeat no more than twice across the smaller spatial length. Unless we're
            // in the last LOD - then this is the best we can do.
            float minWavelength = texelWidth * OceanRenderer.Instance.MinTexelsPerWave;
            if (minSpatialLength / minWavelength > 2f)
            {
                continue;
            }

            // A good match - return immediately
            return lodData;
        }

        // We didnt get a perfect match, but pick the next best candidate
        return lastCandidate;
    }

    public AvailabilityResult CheckAvailability(ReadableVector3f i_worldPos, SamplingData i_samplingData)
    {
        assert(i_samplingData._minSpatialLength >= 0f && i_samplingData._tag != null);

        Rectf sampleAreaXZ = new Rectf(i_worldPos.getX(), i_worldPos.getZ(), 0f, 0f);

        boolean oneWasInRect = false;
        boolean wavelengthsLargeEnough = false;

        for (Map.Entry<Float, PerLodData> gridSize_lodData : _perLodData.entrySet())
        {
            if (!gridSize_lodData.getValue()._activelyBeingRendered || gridSize_lodData.getValue()._resultData._time == -1f)
            {
                continue;
            }

            // Check that the region of interest is covered by this data
            Rectf wdcRect = gridSize_lodData.getValue()._resultData._renderData.RectXZ();
            // Shrink rect by 1 texel border - this is to make finite differences fit as well
            float texelWidth = gridSize_lodData.getKey();
            wdcRect.x += texelWidth; wdcRect.y += texelWidth;
            wdcRect.width -= 2f * texelWidth; wdcRect.height -= 2f * texelWidth;
//            if (!wdcRect.contains(sampleAreaXZ.min) || !wdcRect.Contains(sampleAreaXZ.max))
            if(!wdcRect.contains(sampleAreaXZ))
            {
                continue;
            }
            oneWasInRect = true;

            // The smallest wavelengths should repeat no more than twice across the smaller spatial length. Unless we're
            // in the last LOD - then this is the best we can do.
            float minWavelength = texelWidth * OceanRenderer.Instance.MinTexelsPerWave;
            if (i_samplingData._minSpatialLength / minWavelength > 2f)
            {
                continue;
            }
            wavelengthsLargeEnough = true;

            return AvailabilityResult.DataAvailable;
        }

        if (!oneWasInRect)
        {
            return AvailabilityResult.NoDataAtThisPosition;
        }
        if (!wavelengthsLargeEnough)
        {
            return AvailabilityResult.NoLODsBigEnoughToFilterOutWavelengths;
        }
        // Should not get here.
        return AvailabilityResult.ValidationFailed;
    }


    public void GetStats(/*out int count, out int minQueueLength, out int maxQueueLength*/int[] results)
    {
        int minQueueLength = MAX_REQUESTS;
        int maxQueueLength = 0;
        int count = 0;

        for (PerLodData gridSize_lodData : _perLodData.values())
        {
            if (!gridSize_lodData._activelyBeingRendered)
            {
                continue;
            }

            count++;

            int queueLength = gridSize_lodData._requests.size();
            minQueueLength = Math.min(queueLength, minQueueLength);
            maxQueueLength = Math.max(queueLength, maxQueueLength);
        }

        if (minQueueLength == MAX_REQUESTS) minQueueLength = -1;
        if (maxQueueLength == 0) maxQueueLength = -1;

        results[0] = count;
        results[1] = minQueueLength;
        results[2] = maxQueueLength;
    }

    public boolean GetSamplingData(Rectf i_displacedSamplingArea, float i_minSpatialLength, SamplingData o_samplingData)
    {
        o_samplingData._minSpatialLength = i_minSpatialLength;

        Rectf undisplacedRect = new Rectf(
                i_displacedSamplingArea.x - OceanRenderer.Instance.MaxHorizDisplacement,
                i_displacedSamplingArea.y - OceanRenderer.Instance.MaxHorizDisplacement,
                i_displacedSamplingArea.width + 2f * OceanRenderer.Instance.MaxHorizDisplacement,
                i_displacedSamplingArea.height + 2f * OceanRenderer.Instance.MaxHorizDisplacement
        );
        o_samplingData._tag = GetData(undisplacedRect, i_minSpatialLength);

        return o_samplingData._tag != null;
    }

    public void ReturnSamplingData(SamplingData i_data)
    {
        i_data._tag = null;
    }

    public void SetOrigin(ReadableVector3f newOrigin)
    {
        for (PerLodData pld : _perLodData.values())
        {
//            pld._resultData._renderData._posSnapped -= newOrigin;
//            pld._resultDataPrevFrame._renderData._posSnapped -= newOrigin;

            Vector3f.sub(pld._resultData._renderData._posSnapped, newOrigin, pld._resultData._renderData._posSnapped);
            Vector3f.sub(pld._resultDataPrevFrame._renderData._posSnapped, newOrigin, pld._resultDataPrevFrame._renderData._posSnapped);

            // manually update each request
            for(ReadbackRequest req : pld._requests){
                Vector3f.sub(req._renderData._posSnapped, newOrigin, req._renderData._posSnapped);
            }
        }
    }

}
