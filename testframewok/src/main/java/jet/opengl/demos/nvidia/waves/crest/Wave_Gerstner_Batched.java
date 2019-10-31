package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.demos.nvidia.waves.crest.collision.AvailabilityResult;
import jet.opengl.demos.nvidia.waves.crest.collision.SamplingData;
import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Rectf;

final class Wave_Gerstner_Batched implements Wave_Const{
    Wave_Spectrum _spectrum;

    private Wave_Simulation_Params m_Params;
    private Wave_CDClipmap m_Clipmap;

    private float[] _wavelengths;
    private float[] _amplitudes;
    private float[] _angleDegs;
    private float[] _phases;

    private GerstnerBatch[] _batches = null;
    private Technique mGerstnerShader;
    private boolean mDirectTowardsPoint = false;

    private float m_TotalTime;

    private Wave_Mesh m_QuadMesh;

    // IMPORTANT - this mirrors the constant with the same name in ShapeGerstnerBatch.shader, both must be updated together!
    static final int BATCH_SIZE = 32;

    enum CmdBufStatus
    {
        NoStatus,
        NotAttached,
        Attached
    }

    // scratch data used by batching code
    private final static class UpdateBatchScratchData {
        public static Vector4f[] _twoPiOverWavelengthsBatch = CommonUtil.initArray(new Vector4f[BATCH_SIZE / 4]);
        public static Vector4f[] _ampsBatch = CommonUtil.initArray(new Vector4f[BATCH_SIZE / 4]);
        public static Vector4f[] _waveDirXBatch = CommonUtil.initArray(new Vector4f[BATCH_SIZE / 4]);
        public static Vector4f[] _waveDirZBatch = CommonUtil.initArray(new Vector4f[BATCH_SIZE / 4]);
        public static Vector4f[] _phasesBatch = CommonUtil.initArray(new Vector4f[BATCH_SIZE / 4]);
        public static Vector4f[] _chopAmpsBatch = CommonUtil.initArray(new Vector4f[BATCH_SIZE / 4]);
    }

    void init(Wave_Simulation_Params params, Wave_CDClipmap clipmap)
    {
        m_Clipmap = clipmap;
        m_Params = params;
        m_TotalTime = 0;
        if (_spectrum == null)
        {
            _spectrum = new Wave_Spectrum();
        }

        InitBatches();
    }

    private void InitBatches()
    {
        if (mGerstnerShader == null || mDirectTowardsPoint != m_Params.direct_towards_Point)
        {
//            _waveShader = Shader.Find("Crest/Inputs/Animated Waves/Gerstner Batch");
//            Debug.Assert(_waveShader, "Could not load Gerstner wave shader, make sure it is packaged in the build.");
            mGerstnerShader = ShaderManager.getInstance().getProgram("Crest/Inputs/Animated Waves/Gerstner Batch " + (m_Params.direct_towards_Point ? 0 : 1));
        }

        _batches = new GerstnerBatch[MAX_LOD_COUNT];
        for (int i = 0; i < _batches.length; i++)
        {
            _batches[i] = new GerstnerBatch();
        }
    }

    void update(float deltaTime)
    {
        m_TotalTime += deltaTime * m_Params.time_scale;

        if (m_Params.evaluate_spectrum_runtime)
        {
            UpdateWaveData();
        }

        ReportMaxDisplacement();
    }

    private void UpdateWaveData() {
        // Set random seed to get repeatable results
        Numeric.setRandomSeed(m_Params.random_seed);

        _spectrum.GenerateWaveData(/*_componentsPerOctave*/m_Params.components_per_octave, _wavelengths,  _angleDegs);

        UpdateAmplitudes();

        // Won't run every time so put last in the random sequence
        if (_phases == null || _phases.length != _wavelengths.length) {
            InitPhases();
        }
    }

    private void InitPhases()
    {
        final int _componentsPerOctave = m_Params.components_per_octave;
        // Set random seed to get repeatable results
        Numeric.setRandomSeed(m_Params.random_seed);
        int totalComps = _componentsPerOctave * Wave_Spectrum.NUM_OCTAVES;
        _phases = new float[totalComps];
        for (int octave = 0; octave < Wave_Spectrum.NUM_OCTAVES; octave++)
        {
            for (int i = 0; i < _componentsPerOctave; i++)
            {
                int index = octave * _componentsPerOctave + i;
                float rnd = (i + Numeric.random()) / _componentsPerOctave;
                _phases[index] = 2f * Numeric.PI * rnd;
            }
        }
    }

    void UpdateAmplitudes()
    {
        final int _componentsPerOctave = m_Params.components_per_octave;
        final float _weight = m_Params.gertner_weight;

        if (_amplitudes == null || _amplitudes.length != _wavelengths.length)
        {
            _amplitudes = new float[_wavelengths.length];
        }

        for (int i = 0; i < _wavelengths.length; i++)
        {
            _amplitudes[i] = _weight * _spectrum.GetAmplitude(_wavelengths[i], _componentsPerOctave);
        }
    }

    private void ReportMaxDisplacement()
    {
        assert (_spectrum._chopScales.length == Wave_Spectrum.NUM_OCTAVES) : "OceanWaveSpectrum {_spectrum.name} is out of date, please open this asset and resave in editor.";
        final int _componentsPerOctave = m_Params.components_per_octave;

        float ampSum = 0f;
        for (int i = 0; i < _wavelengths.length; i++)
        {
            ampSum += _amplitudes[i] * _spectrum._chopScales[i / _componentsPerOctave];
        }
        OceanRenderer.Instance.ReportMaxDisplacementFromShape(ampSum * _spectrum._chop, ampSum, ampSum);
    }
    /// <summary>
    /// Computes Gerstner params for a set of waves, for the given lod idx. Writes shader data to the given property.
    /// Returns number of wave components rendered in this batch.
    /// </summary>
    void UpdateBatch(int lodIdx, int firstComponent, int lastComponentNonInc, GerstnerBatch batch)
    {
        batch.Enabled = false;

        int numComponents = lastComponentNonInc - firstComponent;
        int numInBatch = 0;
        int dropped = 0;
        final int _componentsPerOctave = m_Params.components_per_octave;

        float twopi = 2f * Numeric.PI;
        float one_over_2pi = 1f / twopi;
        float minWavelengthThisBatch = m_Clipmap.m_LodTransform.MaxWavelength(lodIdx) / 2f;
        float maxWavelengthCurrentlyRendering = m_Clipmap.m_LodTransform.MaxWavelength(m_Clipmap.m_LodTransform.LodCount() - 1);
        float viewerAltitudeLevelAlpha = m_Clipmap.getViewerAltitudeLevelAlpha();

        // register any nonzero components
        for (int i = 0; i < numComponents; i++)
        {
            float wl = _wavelengths[firstComponent + i];

            // compute amp - contains logic for shifting wave components between last two LODs...
            float amp = _amplitudes[firstComponent + i];

            if (amp >= 0.001f)
            {
                if (numInBatch < BATCH_SIZE)
                {
                    int vi = numInBatch / 4;
                    int ei = numInBatch - vi * 4;

                    UpdateBatchScratchData._twoPiOverWavelengthsBatch[vi].set(ei, 2f * Numeric.PI / wl);
                    UpdateBatchScratchData._ampsBatch[vi].set(ei, amp);

                    float chopScale = _spectrum._chopScales[(firstComponent + i) / _componentsPerOctave];
                    UpdateBatchScratchData._chopAmpsBatch[vi].set(ei, -chopScale * _spectrum._chop * amp);

                    double angle = Math.toRadians (windDirectionAngle() + _angleDegs[firstComponent + i]);
                    UpdateBatchScratchData._waveDirXBatch[vi].set(ei, (float)Math.cos(angle));
                    UpdateBatchScratchData._waveDirZBatch[vi].set(ei, (float)Math.sin(angle));

                    // It used to be this, but I'm pushing all the stuff that doesn't depend on position into the phase.
                    //half4 angle = k * (C * _CrestTime + x) + _Phases[vi];
                    float gravityScale = _spectrum._gravityScales[(firstComponent + i) / _componentsPerOctave];
                    float gravity = OceanRenderer.Instance.Gravity() * /*_spectrum._gravityScale*/m_Params.gravityMultiplier;
                    float C = (float) Math.sqrt(wl * gravity * gravityScale * one_over_2pi);
                    float k = twopi / wl;
                    // Repeat every 2pi to keep angle bounded - helps precision on 16bit platforms
                    UpdateBatchScratchData._phasesBatch[vi].set(ei, (_phases[firstComponent + i] + k * C * /*OceanRenderer.Instance.CurrentTime() todo*/ 1) % (Numeric.PI * 2f));

                    numInBatch++;
                }
                else
                {
                    dropped++;
                }
            }
        }

        if (dropped > 0)
        {
            LogUtil.w(LogUtil.LogType.DEFAULT, String.format("Gerstner LOD{%d}: Batch limit reached, dropped {%d} wavelengths. To support bigger batch sizes, see the comment around the BATCH_SIZE declaration.", lodIdx, dropped));
            numComponents = BATCH_SIZE;
        }

        if (numInBatch == 0)
        {
            // no waves to draw - abort
            return;
        }

        // if we did not fill the batch, put a terminator signal after the last position
        if (numInBatch < BATCH_SIZE)
        {
            int vi_last = numInBatch / 4;
            int ei_last = numInBatch - vi_last * 4;

            for (int vi = vi_last; vi < BATCH_SIZE / 4; vi++)
            {
                for (int ei = ei_last; ei < 4; ei++)
                {
                    UpdateBatchScratchData._twoPiOverWavelengthsBatch[vi].set(ei, 1f); // wary of NaNs
                    UpdateBatchScratchData._ampsBatch[vi].set(ei, 0);
                    UpdateBatchScratchData._waveDirXBatch[vi].set(ei, 0);
                    UpdateBatchScratchData._waveDirZBatch[vi].set(ei, 0);
                    UpdateBatchScratchData._phasesBatch[vi].set(ei, 0);
                    UpdateBatchScratchData._chopAmpsBatch[vi].set(ei, 0);
                }

                ei_last = 0;
            }
        }

        // apply the data to the shape property
        for (int i = 0; i < 2; i++)
        {
            /*var mat = batch.GetMaterial(i);  todo
            mat.SetVectorArray(sp_TwoPiOverWavelengths, UpdateBatchScratchData._twoPiOverWavelengthsBatch);
            mat.SetVectorArray(sp_Amplitudes, UpdateBatchScratchData._ampsBatch);
            mat.SetVectorArray(sp_WaveDirX, UpdateBatchScratchData._waveDirXBatch);
            mat.SetVectorArray(sp_WaveDirZ, UpdateBatchScratchData._waveDirZBatch);
            mat.SetVectorArray(sp_Phases, UpdateBatchScratchData._phasesBatch);
            mat.SetVectorArray(sp_ChopAmps, UpdateBatchScratchData._chopAmpsBatch);
            mat.SetFloat(sp_NumInBatch, numInBatch);
            mat.SetFloat(sp_AttenuationInShallows, OceanRenderer.Instance._simSettingsAnimatedWaves.AttenuationInShallows);

            int numVecs = (numInBatch + 3) / 4;
            mat.SetInt(sp_NumWaveVecs, numVecs);
            mat.SetFloat(LodDataMgr.sp_LD_SliceIndex, lodIdx - i);
            OceanRenderer.Instance._lodDataAnimWaves.BindResultData(mat);*/

            if (OceanRenderer.Instance._lodDataSeaDepths != null)
            {
//                OceanRenderer.Instance._lodDataSeaDepths.BindResultData(mat, false);  todo
            }

            /*if (_directTowardsPoint)
            {
//                mat.SetVector(sp_TargetPointData, new Vector4f(_pointPositionXZ.x, _pointPositionXZ.y, _pointRadii.x, _pointRadii.y));  todo
            }*/
        }

        batch.Enabled = true;
    }

    /**
     * More complicated than one would hope - loops over each component and assigns to a Gerstner batch which will render to a LOD.
     * the camera WL range does not always match the octave WL range (because the vertices per wave is not constrained to powers of
     * 2, unfortunately), so i cant easily just loop over octaves. also any WLs that either go to the last WDC, or don't fit in the last
     *  WDC, are rendered into both the last and second-to-last WDCs, in order to transition them smoothly without pops in all scenarios.
     */
    void LateUpdate()
    {
        int componentIdx = 0;

        // seek forward to first wavelength that is big enough to render into current LODs
        float minWl = m_Clipmap.m_LodTransform.MaxWavelength(0) / 2f;
        while (_wavelengths[componentIdx] < minWl && componentIdx < _wavelengths.length)
        {
            componentIdx++;
        }

        for (int i = 0; i < _batches.length; i++)
        {
            // Default to disabling all batches
            _batches[i].Enabled = false;
        }

        int batch = 0;
        int lodIdx = 0;
        while (componentIdx < _wavelengths.length)
        {
            if (batch >= _batches.length)
            {
//                Debug.LogWarning("Out of Gerstner batches.", this);
                break;
            }

            // Assemble wavelengths into current batch
            int startCompIdx = componentIdx;
            while (componentIdx < _wavelengths.length && _wavelengths[componentIdx] < 2f * minWl)
            {
                componentIdx++;
            }

            // One or more wavelengths - update the batch
            if (componentIdx > startCompIdx)
            {
                UpdateBatch(lodIdx, startCompIdx, componentIdx, _batches[batch]);

                _batches[batch].Wavelength = minWl;
            }

            batch++;
            lodIdx = Math.min(lodIdx + 1, m_Clipmap.m_LodTransform.LodCount() - 1);
            minWl *= 2f;
        }
    }

    float ComputeWaveSpeed(float wavelength/*, float depth*/)
    {
        // wave speed of deep sea ocean waves: https://en.wikipedia.org/wiki/Wind_wave
        // https://en.wikipedia.org/wiki/Dispersion_(water_waves)#Wave_propagation_and_dispersion
        float g = 9.81f;
        float k = 2f * Numeric.PI / wavelength;
        //float h = max(depth, 0.01);
        //float cp = sqrt(abs(tanh_clamped(h * k)) * g / k);
        float cp = (float)Math.sqrt(g / k);
        return cp;
    }

    public boolean GetSurfaceVelocity(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_surfaceVel)
    {
        o_surfaceVel.set(0,0,0);

        if (_amplitudes == null) return false;

        Vector2f pos = new Vector2f(i_worldPos.getX(), i_worldPos.getZ());
        float mytime = /*OceanRenderer.Instance.CurrentTime()  todo*/ 0;
        float windAngle = windDirectionAngle();
        float minWaveLength = i_samplingData._minSpatialLength / 2f;

        for (int j = 0; j < _amplitudes.length; j++)
        {
            if (_amplitudes[j] <= 0.001f) continue;
            if (_wavelengths[j] < minWaveLength) continue;

            float C = ComputeWaveSpeed(_wavelengths[j]);

            // direction
            Vector2f D = new Vector2f((float)Math.cos(Math.toRadians(windAngle + _angleDegs[j])), (float)Math.sin(Math.toRadians(windAngle + _angleDegs[j])));
            // wave number
            float k = 2f * Numeric.PI / _wavelengths[j];

            float x = Vector2f.dot(D, pos);
            float t = k * (x + C * mytime) + _phases[j];
            double disp = -_spectrum._chop * k * C * Math.cos(t);
            /*o_surfaceVel += _amplitudes[j] * new Vector3(
                    D.x * disp,
                    -k * C * Mathf.Sin(t),
                    D.y * disp
            );*/

            o_surfaceVel.set((float)(D.x * disp),
                    (float)(-k * C * Math.sin(t)),
                    (float)(D.y * disp));
            o_surfaceVel.scale(_amplitudes[j]);
        }

        return true;
    }

    public long SampleHeight(ReadableVector3f i_worldPos, SamplingData i_samplingData/*, out float o_height*/)
    {
        float o_height = 0f;

        Vector3f posFlatland = new Vector3f(i_worldPos);
        posFlatland.y = OceanRenderer.Instance.transform.getPositionY();

        Vector3f undisplacedPos = new Vector3f();
        if (!ComputeUndisplacedPosition(posFlatland, i_samplingData, undisplacedPos))
            return /*false*/ 0;

        Vector3f disp = new Vector3f();
        if (!SampleDisplacement(undisplacedPos, i_samplingData, disp))
            return /*false*/ 0;

        o_height = posFlatland.y + disp.y;

//        return true;
        return Numeric.encode(1, Float.floatToIntBits(o_height));
    }

    public boolean GetSamplingData(Rectf i_displacedSamplingArea, float i_minSpatialLength, SamplingData o_samplingData)
    {
        // We're not bothered with areas as the waves are infinite, so just store the min wavelength.
        o_samplingData._minSpatialLength = i_minSpatialLength;
        return true;
    }

    public void ReturnSamplingData(SamplingData i_data)
    {
        i_data._minSpatialLength = -1f;
    }

    public boolean ComputeUndisplacedPosition(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_undisplacedWorldPos)
    {
        // FPI - guess should converge to location that displaces to the target position
        Vector3f guess = new Vector3f(i_worldPos);
        // 2 iterations was enough to get very close when chop = 1, added 2 more which should be
        // sufficient for most applications. for high chop values or really stormy conditions there may
        // be some error here. one could also terminate iteration based on the size of the error, this is
        // worth trying but is left as future work for now.
        Vector3f disp = new Vector3f();
        for (int i = 0; i < 4 && SampleDisplacement(guess, i_samplingData, disp); i++)
        {
//            Vector3 error = guess + disp - i_worldPos;
            float errorx = guess.x + disp.x - i_worldPos.getX();
            float errorz = guess.z + disp.z - i_worldPos.getZ();
            guess.x -= errorx;
            guess.z -= errorz;
        }

        o_undisplacedWorldPos = guess;
        o_undisplacedWorldPos.y = OceanRenderer.Instance.SeaLevel();

        return true;
    }

    public AvailabilityResult CheckAvailability(ReadableVector3f i_worldPos, SamplingData i_samplingData)
    {
        return _amplitudes == null ? AvailabilityResult.NotInitialisedYet : AvailabilityResult.DataAvailable;
    }

    // Compute normal to a surface with a parameterization - equation 14 here: http://mathworld.wolfram.com/NormalVector.html
    public boolean SampleNormal(ReadableVector3f i_undisplacedWorldPos, SamplingData i_samplingData, Vector3f o_normal)
    {
        o_normal.set(0,0,0);

        if (_amplitudes == null) return false;

        Vector2f pos = new Vector2f(i_undisplacedWorldPos.getX(), i_undisplacedWorldPos.getZ());
        float mytime =m_TotalTime;
        float windAngle = windDirectionAngle();
        float minWaveLength = i_samplingData._minSpatialLength / 2f;

        // base rate of change of our displacement function in x and z is unit
        Vector3f delfdelx = new Vector3f(1,0,0);
        Vector3f delfdelz = new Vector3f(0,0,1);

        for (int j = 0; j < _amplitudes.length; j++)
        {
            if (_amplitudes[j] <= 0.001f) continue;
            if (_wavelengths[j] < minWaveLength) continue;

            float C = ComputeWaveSpeed(_wavelengths[j]);

            // direction
//            var D = new Vector2(Mathf.Cos((windAngle + _angleDegs[j]) * Mathf.Deg2Rad), Mathf.Sin((windAngle + _angleDegs[j]) * Mathf.Deg2Rad));
            Vector2f D = new Vector2f((float)Math.cos(Math.toRadians(windAngle + _angleDegs[j])), (float)Math.sin(Math.toRadians(windAngle + _angleDegs[j])));
            // wave number
            float k = 2f * Numeric.PI / _wavelengths[j];

            float x = Vector2f.dot(D, pos);
            float t = k * (x + C * mytime) + _phases[j];
            double disp = k * -_spectrum._chop * Math.cos(t);
            double dispx = D.x * disp;
            double dispz = D.y * disp;
            double dispy = -k * Math.sin(t);

//            delfdelx += _amplitudes[j] * new Vector3(D.x * dispx, D.x * dispy, D.y * dispx);
//            delfdelz += _amplitudes[j] * new Vector3(D.x * dispz, D.y * dispy, D.y * dispz);
            delfdelx.x += _amplitudes[j] * D.x * dispx;
            delfdelx.y += _amplitudes[j] * D.x * dispy;
            delfdelx.z += _amplitudes[j] * D.y * dispx;

            delfdelz.x += _amplitudes[j] * D.x * dispz;
            delfdelz.y += _amplitudes[j] * D.y * dispy;
            delfdelz.z += _amplitudes[j] * D.y * dispz;
        }

        Vector3f.cross(delfdelz, delfdelx, o_normal);
        o_normal.normalise();

        return true;
    }

    public boolean SampleDisplacement(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement)
    {
        o_displacement.set(0,0,0);

        if (_amplitudes == null)
        {
            return false;
        }

        Vector2f pos = new Vector2f(i_worldPos.getX(), i_worldPos.getZ());
        float mytime = m_TotalTime;
        float windAngle = windDirectionAngle();
        float minWavelength = i_samplingData._minSpatialLength / 2f;

        for (int j = 0; j < _amplitudes.length; j++)
        {
            if (_amplitudes[j] <= 0.001f) continue;
            if (_wavelengths[j] < minWavelength) continue;

            float C = ComputeWaveSpeed(_wavelengths[j]);

            // direction
//            Vector2 D = new Vector2(Mathf.Cos((windAngle + _angleDegs[j]) * Mathf.Deg2Rad), Mathf.Sin((windAngle + _angleDegs[j]) * Mathf.Deg2Rad));
            Vector2f D = new Vector2f((float)Math.cos(Math.toRadians(windAngle + _angleDegs[j])), (float)Math.sin(Math.toRadians(windAngle + _angleDegs[j])));
            // wave number
            float k = 2f * Numeric.PI / _wavelengths[j];

            float x = Vector2f.dot(D, pos);
            float t = k * (x + C * mytime) + _phases[j];
            float disp = (float) (-_spectrum._chop * Math.sin(t));
            /*o_displacement += _amplitudes[j] * new Vector3(
                    D.x * disp,
                    Mathf.Cos(t),
                    D.y * disp
            );*/

            o_displacement.set(D.x * disp,
                    (float)Math.cos(t),
                    D.y * disp);
            o_displacement.scale(_amplitudes[j]);
        }

        return true;
    }

    /** displacementValid in first,  velValid in second */
    public long SampleDisplacementVel(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement, Vector3f o_displacementVel)
    {
        boolean o_displacementValid = SampleDisplacement(i_worldPos, i_samplingData, o_displacement);
        boolean o_velValid = GetSurfaceVelocity(i_worldPos, i_samplingData, o_displacementVel);

        return Numeric.encode(o_displacementValid ? 1 : 0, o_velValid ? 1:0);
    }

    public int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
        if (o_resultDisps != null)
        {
            for (int i = 0; i < o_resultDisps.length; i++)
            {
                SampleDisplacement(i_queryPoints[i], i_samplingData, o_resultDisps[i]);
            }
        }

        if (o_resultNorms != null)
        {
            for (int i = 0; i < o_resultNorms.length; i++)
            {
                Vector3f undispPos = new Vector3f();
                if (ComputeUndisplacedPosition(i_queryPoints[i], i_samplingData, undispPos))
                {
                    SampleNormal(undispPos, i_samplingData, o_resultNorms[i]);
                }
                else
                {
                    o_resultNorms[i].set(0,1,0);
                }
            }
        }

        return 0;
    }

    public int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, float[] o_resultHeights, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
        if (o_resultHeights != null)
        {
            for (int i = 0; i < o_resultHeights.length; i++)
            {
                long result = SampleHeight(i_queryPoints[i], i_samplingData);
                o_resultHeights[i] = Float.intBitsToFloat(Numeric.decodeSecond(result));
            }
        }

        if (o_resultNorms != null)
        {
            for (int i = 0; i < o_resultNorms.length; i++)
            {
                Vector3f undispPos = new Vector3f();
                if (ComputeUndisplacedPosition(i_queryPoints[i], i_samplingData, undispPos))
                {
                    SampleNormal(undispPos, i_samplingData, o_resultNorms[i]);
                }
                else
                {
                    o_resultNorms[i].set(0,1,0);
                }
            }
        }

        if (o_resultVels != null)
        {
            for (int i = 0; i < o_resultVels.length; i++)
            {
                GetSurfaceVelocity(i_queryPoints[i], i_samplingData, o_resultVels[i]);
            }
        }

        return 0;
    }

    public boolean RetrieveSucceeded(int queryStatus)
    {
        return queryStatus == 0;
    }

    // for inner use.
    private ReadableVector2f WindDir(){
        return m_Params.wind_dir;
    }

    /** Get the anlge along the z-axis. */
    private float windDirectionAngle(){
        float angle =  (float) Math.acos(m_Params.wind_dir.y);  // assum the dir had been normalized.
        return m_Params.wind_dir.x >= 0 ? angle : -angle;
    }

    public void setOrigin(ReadableVector3f newOrigin)
    {
        if (_phases == null) return;

        float windAngle = windDirectionAngle();
        for (int i = 0; i < _phases.length; i++)
        {
//            var direction = new Vector3(Mathf.Cos((windAngle + _angleDegs[i]) * Mathf.Deg2Rad), 0f, Mathf.Sin((windAngle + _angleDegs[i]) * Mathf.Deg2Rad));
            float directionZ = (float) Math.cos(Math.toRadians(windAngle + _angleDegs[i]));
            float directionX = (float) Math.sin(Math.toRadians(windAngle + _angleDegs[i]));
            float phaseOffsetMeters = Vector3f.dot(newOrigin, directionX,0,directionZ);

            // wave number
            float k = 2f * Numeric.PI / _wavelengths[i];
            _phases[i] = (_phases[i] + phaseOffsetMeters * k) % (Numeric.PI * 2f);
        }
    }

    private final class GerstnerBatch {
        float Wavelength;
        boolean Enabled ;

        public void Draw(CommandBuffer buf, float weight, int isTransition)
        {
            if (Enabled && weight > 0f)
            {
                /*PropertyWrapperMaterial mat = GetMaterial(isTransition);
                mat.SetFloat(RegisterLodDataInputBase.sp_Weight, weight);*/
//                buf.DrawMesh(RasterMesh(), Matrix4x4.identity, mat.material);  todo
            }
        }
    }
}
