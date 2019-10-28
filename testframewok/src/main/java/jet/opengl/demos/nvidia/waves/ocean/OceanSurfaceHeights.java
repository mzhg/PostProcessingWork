package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;

import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

final class OceanSurfaceHeights implements OceanConst{
    private final Vector2f m_UVToWorldScale = new Vector2f();
    private final Vector2f m_UVToWorldRotation = new Vector2f();
    private final Vector2f m_UVToWorldOffset = new Vector2f();

    private int m_num_samples_w;
    private int m_num_samples_l;
    private int m_num_gpu_samples_w;
    private int m_num_gpu_samples_l;
    private int m_max_num_intervals_stride;

    private Vector2f[] m_pTargetSamplePositions;
    private Vector2f[] m_pAdjustedSamplePositions;
    private Vector4f[] m_pSampleDisplacements;
    private Vector2f[] m_pSampleGradients;

    private Vector2f[] m_pScratchTargetSampleInputs;
    private Vector2f[] m_pScratchAdjustedSampleInputs;
    private Vector4f[] m_pScratchSampleDisplacements;

    // FX objects
//    ID3DX11Effect* m_pFX;
    private Technique m_pRenderSurfaceToReverseLookupTechnique;
    private Technique m_pRenderQuadToUITechnique;
    private Technique m_pRenderMarkerTechnique;

    /*ID3DX11EffectScalarVariable* m_pNumQuadsWVariable;
    ID3DX11EffectScalarVariable* m_pNumQuadsHVariable;
    ID3DX11EffectVectorVariable* m_pQuadUVDimsVariable;
    ID3DX11EffectVectorVariable* m_pSrcUVToWorldScaleVariable;
    ID3DX11EffectVectorVariable* m_pSrcUVToWorldRotationVariable;
    ID3DX11EffectVectorVariable* m_pSrcUVToWorldOffsetVariable;
    ID3DX11EffectVectorVariable* m_pWorldToClipScaleVariable;
    ID3DX11EffectVectorVariable* m_pClipToWorldRotationVariable;
    ID3DX11EffectVectorVariable* m_pClipToWorldOffsetVariable;

    // UI rendering
    ID3DX11EffectShaderResourceVariable* m_pTexDiffuseVariable;

    // Marker rendering
    ID3DX11EffectShaderResourceVariable* m_pTexLookupVariable;
    ID3DX11EffectMatrixVariable* m_pMatViewProjVariable;
    ID3DX11EffectMatrixVariable* m_pMatWorldVariable;
    ID3DX11EffectVectorVariable* m_pWorldToUVScaleVariable;
    ID3DX11EffectVectorVariable* m_pWorldToUVRotationVariable;
    ID3DX11EffectVectorVariable* m_pWorldToUVOffsetVariable;*/

    private int[] m_pWaterSimulationShaderInputMappings;

    // D3D objects
//    ID3D11Device* m_pd3dDevice;
    private Texture2D m_pLookupSRV;
    private Texture2D m_pLookupRTV;

    private GLFuncProvider gl;
    private RenderTargets mFbo;

    private final OceanSurfaceHeightParams m_TechParams = new OceanSurfaceHeightParams();

    private static Vector2f do_rot(Vector2f coord, Vector2f rot) {
        Vector2f result = new Vector2f();
        result.x =  coord.x * rot.x + coord.y * rot.y;
        result.y = -coord.x * rot.y + coord.y * rot.x;
        return result;
    }

    Vector2f do_scale_and_rot(Vector2f coord, Vector2f scale, Vector2f rot) {

        Vector2f scaled = new Vector2f();
        scaled.x = coord.x * scale.x;
        scaled.y = coord.y * scale.y;

        return do_rot(scaled,rot);
    }

    Vector2f do_inv_scale_and_rot(Vector2f coord, Vector2f scale, Vector2f rot) {

        Vector2f inv_rot = new Vector2f();
        inv_rot.x =  rot.x;
        inv_rot.y = -rot.y;

        Vector2f rotated = do_rot(coord,inv_rot);

        Vector2f result = new Vector2f();
        result.x = rotated.x * 1.f/scale.x;
        result.y = rotated.y * 1.f/scale.y;

        return result;
    }

	private static final int kGPUSampleDensityMultiplier = 4;

    private static final float kMaxError = 0.01f;

    private static final int kMaxConvergenceIterations = 20;				// Empirically determined
    private static final float kConvergenceMultiplier = 0.975f;			// Empirically determined
    private static final float kProgressiveConvergenceMultiplier = 0.99f;	// Empirically determined

    OceanSurfaceHeights(int max_num_samples, ReadableVector2f UVToWorldScale){
        m_UVToWorldScale.set(UVToWorldScale);

        // Figure out how to allocate samples to area
        int num_intervals_l = 1;
        int num_intervals_w = 1;
        int num_intervals_stride = 1;
        if(m_UVToWorldScale.x > m_UVToWorldScale.y) {
            num_intervals_w = (int)Math.floor(0.5f + m_UVToWorldScale.x/m_UVToWorldScale.y);
        } else {
            num_intervals_l = (int)Math.floor(0.5f + m_UVToWorldScale.y/m_UVToWorldScale.x);
        }

        while(((2*num_intervals_l+1)*(2*num_intervals_w+1)) <= max_num_samples) {
            num_intervals_l *= 2;
            num_intervals_w *= 2;
            num_intervals_stride *= 2;
        }

        m_num_samples_l = num_intervals_l + 1;
        m_num_samples_w = num_intervals_w + 1;
        m_max_num_intervals_stride = num_intervals_stride;

        m_num_gpu_samples_l = m_num_samples_l*kGPUSampleDensityMultiplier;
        m_num_gpu_samples_w = m_num_samples_w*kGPUSampleDensityMultiplier;

        int num_samples = m_num_samples_l * m_num_samples_w;

        m_pTargetSamplePositions = new Vector2f[num_samples];
        m_pAdjustedSamplePositions = new Vector2f[num_samples];
        m_pSampleDisplacements = new Vector4f[num_samples];
        m_pSampleGradients = new Vector2f[num_samples];

        m_pScratchSampleDisplacements = new Vector4f[num_samples];
        m_pScratchTargetSampleInputs = new Vector2f[num_samples];
        m_pScratchAdjustedSampleInputs = new Vector2f[num_samples];

        for(int i = 0; i < num_samples; i++){
            m_pTargetSamplePositions[i] = new Vector2f();
            m_pAdjustedSamplePositions[i] = new Vector2f();
            m_pSampleGradients[i] = new Vector2f();
            m_pScratchTargetSampleInputs[i] = new Vector2f();
            m_pScratchAdjustedSampleInputs[i] = new Vector2f();

            m_pSampleDisplacements[i] = new Vector4f();
            m_pScratchSampleDisplacements[i] = new Vector4f();
        }
    }

    void updateHeights(/*ID3D11DeviceContext* pDC,*/ GFSDK_WaveWorks_Simulation hSim, ReadableVector2f UVToWorldRotation, ReadableVector2f worldCentroid)
    {
        m_UVToWorldRotation.set(UVToWorldRotation);

        // To get the offset, add rotated and scaled (-0.5,-0.5) to worldCentroid
        Vector2f minus_half = new Vector2f(-0.5f,-0.5f);
        minus_half = do_scale_and_rot(minus_half,m_UVToWorldScale,m_UVToWorldRotation);
        m_UVToWorldOffset.set(worldCentroid);
        m_UVToWorldOffset.x += minus_half.x;
        m_UVToWorldOffset.y += minus_half.y;

	    final int num_intervals_w = m_num_samples_w-1;
        final int num_intervals_l = m_num_samples_l-1;

        Vector2f coord = new Vector2f();
        // Initialise sample inputs - note that we slop one sample over in each direction in order
        // to calculate gradients
        for(int vi = 0; vi != m_num_samples_l; ++vi) {
            float v = (float)(vi-1)/(float)(num_intervals_l-2);
            for(int ui = 0; ui != m_num_samples_w; ++ui) {
                float u = (float)(ui-1)/(float)(num_intervals_w-2);
                coord.x = u;
                coord.y = v;
                coord = do_scale_and_rot(coord,m_UVToWorldScale,m_UVToWorldRotation);
                coord.x += m_UVToWorldOffset.x;
                coord.y += m_UVToWorldOffset.y;
                m_pTargetSamplePositions[ui + vi * m_num_samples_w].set(coord);
            }
        }

        // Initialise coarse samples
        int num_samples = 0;
        int num_intervals_stride = m_max_num_intervals_stride;
        for(int vi = 0; vi < m_num_samples_l; vi += num_intervals_stride) {
            for(int ui = 0; ui < m_num_samples_w; ui += num_intervals_stride) {
                coord = m_pTargetSamplePositions[ui + vi * m_num_samples_w];
                m_pScratchTargetSampleInputs[num_samples].set(coord);
                m_pScratchAdjustedSampleInputs[num_samples].set(coord);
                ++num_samples;
            }
        }

        // Do the coarse converge
        float initial_conv_amt = 1.f;
        int num_sample_queries = converge_scratch(hSim,num_samples,initial_conv_amt);

        // Copy out into the results area
        num_samples = 0;
        for(int vi = 0; vi < m_num_samples_l; vi += num_intervals_stride) {
            for(int ui = 0; ui < m_num_samples_w; ui += num_intervals_stride) {
			    final int results_ix = ui + vi * m_num_samples_w;
                m_pAdjustedSamplePositions[results_ix] = m_pScratchAdjustedSampleInputs[num_samples];
                m_pSampleDisplacements[results_ix] = m_pScratchSampleDisplacements[num_samples];
                ++num_samples;
            }
        }

        Vector2f e01 = new Vector2f(), e23 = new Vector2f(), e02 = new Vector2f(), e13 = new Vector2f(), c = new Vector2f();

        // Progressive refinement
        while(num_intervals_stride > 1) {

		    final int half_stride = num_intervals_stride >> 1;

            // Initialise samples
            num_samples = 0;
            for(int vi = 0; vi != num_intervals_l; vi += num_intervals_stride) {
                for(int ui = 0; ui != num_intervals_w; ui += num_intervals_stride) {

                    Vector2f adj_corner_0 = m_pAdjustedSamplePositions[(ui+0)                    + (vi+0)                    * m_num_samples_w];
                    Vector2f adj_corner_1 = m_pAdjustedSamplePositions[(ui+num_intervals_stride) + (vi+0)                    * m_num_samples_w];
                    Vector2f adj_corner_2 = m_pAdjustedSamplePositions[(ui+0)                    + (vi+num_intervals_stride) * m_num_samples_w];
                    Vector2f adj_corner_3 = m_pAdjustedSamplePositions[(ui+num_intervals_stride) + (vi+num_intervals_stride) * m_num_samples_w];

                    e01.x = 0.5f * adj_corner_0.x + 0.5f * adj_corner_1.x;
                    e01.y = 0.5f * adj_corner_0.y + 0.5f * adj_corner_1.y;
                    e23.x = 0.5f * adj_corner_2.x + 0.5f * adj_corner_3.x;
                    e23.y = 0.5f * adj_corner_2.y + 0.5f * adj_corner_3.y;
                    e02.x = 0.5f * adj_corner_0.x + 0.5f * adj_corner_2.x;
                    e02.y = 0.5f * adj_corner_0.y + 0.5f * adj_corner_2.y;
                    e13.x = 0.5f * adj_corner_1.x + 0.5f * adj_corner_3.x;
                    e13.y = 0.5f * adj_corner_1.y + 0.5f * adj_corner_3.y;
                    c.x = 0.5f * e01.x + 0.5f * e23.x;
                    c.y = 0.5f * e01.y + 0.5f * e23.y;

                    if(0 == vi) {
                        m_pScratchTargetSampleInputs[num_samples].set(m_pTargetSamplePositions[(ui+half_stride) + (vi+0) * m_num_samples_w]);
                        m_pScratchAdjustedSampleInputs[num_samples].set(e01);
                        ++num_samples;
                    }

                    if(0 == ui) {
                        m_pScratchTargetSampleInputs[num_samples].set(m_pTargetSamplePositions[(ui+0) + (vi+half_stride) * m_num_samples_w]);
                        m_pScratchAdjustedSampleInputs[num_samples].set(e02);
                        ++num_samples;
                    }

                    m_pScratchTargetSampleInputs[num_samples].set(m_pTargetSamplePositions[(ui+num_intervals_stride) + (vi+half_stride) * m_num_samples_w]);
                    m_pScratchAdjustedSampleInputs[num_samples].set(e13);
                    ++num_samples;

                    m_pScratchTargetSampleInputs[num_samples].set(m_pTargetSamplePositions[(ui+half_stride) + (vi+num_intervals_stride) * m_num_samples_w]);
                    m_pScratchAdjustedSampleInputs[num_samples].set(e23);
                    ++num_samples;

                    m_pScratchTargetSampleInputs[num_samples].set(m_pTargetSamplePositions[(ui+half_stride) + (vi+half_stride) * m_num_samples_w]);
                    m_pScratchAdjustedSampleInputs[num_samples].set(c);
                    ++num_samples;
                }
            }

            // Converge
            num_sample_queries += converge_scratch(hSim,num_samples,initial_conv_amt);

            // Copy out results
            num_samples = 0;
            for(int vi = 0; vi != num_intervals_l; vi += num_intervals_stride) {
                for(int ui = 0; ui != num_intervals_w; ui += num_intervals_stride) {

                    int results_ix;

                    if(0 == vi) {
                        results_ix = (ui+half_stride) + (vi+0) * m_num_samples_w;
                        m_pAdjustedSamplePositions[results_ix].set(m_pScratchAdjustedSampleInputs[num_samples]);
                        m_pSampleDisplacements[results_ix].set(m_pScratchSampleDisplacements[num_samples]);
                        ++num_samples;
                    }

                    if(0 == ui) {
                        results_ix = (ui+0) + (vi+half_stride) * m_num_samples_w;
                        m_pAdjustedSamplePositions[results_ix].set(m_pScratchAdjustedSampleInputs[num_samples]);
                        m_pSampleDisplacements[results_ix].set(m_pScratchSampleDisplacements[num_samples]);
                        ++num_samples;
                    }

                    results_ix = (ui+num_intervals_stride) + (vi+half_stride) * m_num_samples_w;
                    m_pAdjustedSamplePositions[results_ix].set(m_pScratchAdjustedSampleInputs[num_samples]);
                    m_pSampleDisplacements[results_ix].set(m_pScratchSampleDisplacements[num_samples]);
                    ++num_samples;

                    results_ix = (ui+half_stride) + (vi+num_intervals_stride) * m_num_samples_w;
                    m_pAdjustedSamplePositions[results_ix].set(m_pScratchAdjustedSampleInputs[num_samples]);
                    m_pSampleDisplacements[results_ix].set(m_pScratchSampleDisplacements[num_samples]);
                    ++num_samples;

                    results_ix = (ui+half_stride) + (vi+half_stride) * m_num_samples_w;
                    m_pAdjustedSamplePositions[results_ix].set(m_pScratchAdjustedSampleInputs[num_samples]);
                    m_pSampleDisplacements[results_ix].set(m_pScratchSampleDisplacements[num_samples]);
                    ++num_samples;
                }
            }

            num_intervals_stride = half_stride;
            initial_conv_amt *= kProgressiveConvergenceMultiplier;
        }

	/*
	static int max_num_sample_queries = 0;
	if(num_sample_queries > max_num_sample_queries) {
		max_num_sample_queries = num_sample_queries;
		char buff[256];
		sprintf(buff, "Max queries: %d\n", max_num_sample_queries);
		OutputDebugStringA(buff);
	}
	*/

        updateGradients();
    }

    int converge_scratch(GFSDK_WaveWorks_Simulation hSim, int num_samples, float initial_conv_amt)
    {
        int num_sample_queries = 0;

        // Converge the scratch samples
        float rms_error;
        float convergence_amount = initial_conv_amt;
        int num_iterations = 0;
        Vector2f coord = new Vector2f();
        Vector2f error = new Vector2f();
        do {

            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetDisplacements(hSim, m_pScratchAdjustedSampleInputs, m_pScratchSampleDisplacements, num_samples);
            num_sample_queries += num_samples;

            rms_error = 0.f;
            for(int sample = 0; sample != num_samples; ++sample) {
                coord.x = m_pScratchAdjustedSampleInputs[sample].x + m_pScratchSampleDisplacements[sample].x;
                coord.y = m_pScratchAdjustedSampleInputs[sample].y + m_pScratchSampleDisplacements[sample].y;

                error.x = coord.x - m_pScratchTargetSampleInputs[sample].x;
                error.y = coord.y - m_pScratchTargetSampleInputs[sample].y;
                float sqr_error = error.x*error.x + error.y*error.y;
                rms_error += sqr_error;
            }

            rms_error = (float) Math.sqrt(rms_error/num_samples);

            if(rms_error > kMaxError) {
                for(int sample = 0; sample != num_samples; ++sample) {
//                    gfsdk_float2 coord;
                    coord.x = m_pScratchAdjustedSampleInputs[sample].x + m_pScratchSampleDisplacements[sample].x;
                    coord.y = m_pScratchAdjustedSampleInputs[sample].y + m_pScratchSampleDisplacements[sample].y;
//                    gfsdk_float2 error;
                    error.x = coord.x - m_pScratchTargetSampleInputs[sample].x;
                    error.y = coord.y - m_pScratchTargetSampleInputs[sample].y;
                    m_pScratchAdjustedSampleInputs[sample].x -= convergence_amount * error.x;
                    m_pScratchAdjustedSampleInputs[sample].y -= convergence_amount * error.y;
                }
            }

            convergence_amount *= kConvergenceMultiplier;	// Converge a bit less each time round
            ++num_iterations;

        } while (rms_error > kMaxError && num_iterations != kMaxConvergenceIterations);

        // assert(num_iterations != kMaxConvergenceIterations);	// Useful to know if we ran out of road!

	/*
	static int max_num_iterations = 0;
	if(num_iterations > max_num_iterations) {
		max_num_iterations = num_iterations;
		char buff[256];
		sprintf(buff, "Max iterations: %d\n", max_num_iterations);
		OutputDebugStringA(buff);
	}
	*/

        return num_sample_queries;
    }

    void getDisplacements(Vector2f[] inSamplePoints, Vector4f[] outDisplacements, int numSamples)
    {
        final float num_intervals_w = (m_num_samples_w-3);	// Not including the 'slop' samples
        final float num_intervals_l = (m_num_samples_l-3); // Not including the 'slop' samples

        Vector2f coord = new Vector2f();
        for(int sample = 0; sample != numSamples; ++sample) {

            // Transform the sample point to UV
            coord.x = inSamplePoints[sample].x - m_UVToWorldOffset.x;
            coord.y = inSamplePoints[sample].y - m_UVToWorldOffset.y;
            coord = do_inv_scale_and_rot(coord,m_UVToWorldScale,m_UVToWorldRotation);

            // Transform UV to non-slop samples
            coord.x *= num_intervals_w;
            coord.y *= num_intervals_l;
            assert(coord.x >= 0.f);
            assert(coord.x <= num_intervals_w);
            assert(coord.y >= 0.f);
            assert(coord.y <= num_intervals_l);

            // Then allow for the slop
            coord.x += 1.f;
            coord.y += 1.f;

            float flower_x = (float)Math.floor(coord.x);
            float flower_y = (float)Math.floor(coord.y);
            int lower_x = (int)(flower_x);
            int lower_y = (int)(flower_y);

            Vector4f p00 = m_pSampleDisplacements[(lower_x+0) + (lower_y+0)*m_num_samples_w];
            Vector4f p01 = m_pSampleDisplacements[(lower_x+1) + (lower_y+0)*m_num_samples_w];
            Vector4f p10 = m_pSampleDisplacements[(lower_x+0) + (lower_y+1)*m_num_samples_w];
            Vector4f p11 = m_pSampleDisplacements[(lower_x+1) + (lower_y+1)*m_num_samples_w];

            float frac_x = coord.x - lower_x;
            float frac_y = coord.y - lower_y;

            outDisplacements[sample].x = (1.f-frac_x)*(1.f-frac_y)*p00.x + frac_x*(1.f-frac_y)*p01.x + (1.f-frac_x)*frac_y*p10.x + frac_x*frac_y*p11.x;
            outDisplacements[sample].y = (1.f-frac_x)*(1.f-frac_y)*p00.y + frac_x*(1.f-frac_y)*p01.y + (1.f-frac_x)*frac_y*p10.y + frac_x*frac_y*p11.y;
            outDisplacements[sample].z = (1.f-frac_x)*(1.f-frac_y)*p00.z + frac_x*(1.f-frac_y)*p01.z + (1.f-frac_x)*frac_y*p10.z + frac_x*frac_y*p11.z;
            outDisplacements[sample].w = (1.f-frac_x)*(1.f-frac_y)*p00.w + frac_x*(1.f-frac_y)*p01.w + (1.f-frac_x)*frac_y*p10.w + frac_x*frac_y*p11.w;
        }
    }

    void getGradients(Vector2f[] inSamplePoints, Vector2f[] outGradients, int numSamples)
    {
        Vector2f coord = new Vector2f();
	    final float num_intervals_w = (m_num_samples_w-3);	// Not including the 'slop' samples
        final float num_intervals_l = (m_num_samples_l-3); // Not including the 'slop' samples
        for(int sample = 0; sample != numSamples; ++sample) {

            // Transform the sample point to UV
            coord.x = inSamplePoints[sample].x - m_UVToWorldOffset.x;
            coord.y = inSamplePoints[sample].y - m_UVToWorldOffset.y;
            coord = do_inv_scale_and_rot(coord,m_UVToWorldScale,m_UVToWorldRotation);

            // Transform UV to non-slop samples
            coord.x *= num_intervals_w;
            coord.y *= num_intervals_l;
            assert(coord.x >= 0.f);
            assert(coord.x <= num_intervals_w);
            assert(coord.y >= 0.f);
            assert(coord.y <= num_intervals_l);

            // Then allow for the slop
            coord.x += 1.f;
            coord.y += 1.f;

            float flower_x = (float)Math.floor(coord.x);
            float flower_y = (float)Math.floor(coord.y);
            int lower_x = (int)(flower_x);
            int lower_y = (int)(flower_y);

            Vector2f p00 = m_pSampleGradients[(lower_x+0) + (lower_y+0)*m_num_samples_w];
            Vector2f p01 = m_pSampleGradients[(lower_x+1) + (lower_y+0)*m_num_samples_w];
            Vector2f p10 = m_pSampleGradients[(lower_x+0) + (lower_y+1)*m_num_samples_w];
            Vector2f p11 = m_pSampleGradients[(lower_x+1) + (lower_y+1)*m_num_samples_w];

            float frac_x = coord.x - (lower_x);
            float frac_y = coord.y - (lower_y);

            outGradients[sample].x = (1.f-frac_x)*(1.f-frac_y)*p00.x + frac_x*(1.f-frac_y)*p01.x + (1.f-frac_x)*frac_y*p10.x + frac_x*frac_y*p11.x;
            outGradients[sample].y = (1.f-frac_x)*(1.f-frac_y)*p00.y + frac_x*(1.f-frac_y)*p01.y + (1.f-frac_x)*frac_y*p10.y + frac_x*frac_y*p11.y;
        }
    }

    void getGradient(Vector2f inSamplePoint, Vector2f outGradient)
    {
        Vector2f coord = new Vector2f();
        final float num_intervals_w = (m_num_samples_w-3);	// Not including the 'slop' samples
        final float num_intervals_l = (m_num_samples_l-3); // Not including the 'slop' samples
        // Transform the sample point to UV
        coord.x = inSamplePoint.x - m_UVToWorldOffset.x;
        coord.y = inSamplePoint.y - m_UVToWorldOffset.y;
        coord = do_inv_scale_and_rot(coord,m_UVToWorldScale,m_UVToWorldRotation);

        // Transform UV to non-slop samples
        coord.x *= num_intervals_w;
        coord.y *= num_intervals_l;
        assert(coord.x >= 0.f);
        assert(coord.x <= num_intervals_w);
        assert(coord.y >= 0.f);
        assert(coord.y <= num_intervals_l);

        // Then allow for the slop
        coord.x += 1.f;
        coord.y += 1.f;

        float flower_x = (float)Math.floor(coord.x);
        float flower_y = (float)Math.floor(coord.y);
        int lower_x = (int)(flower_x);
        int lower_y = (int)(flower_y);

        Vector2f p00 = m_pSampleGradients[(lower_x+0) + (lower_y+0)*m_num_samples_w];
        Vector2f p01 = m_pSampleGradients[(lower_x+1) + (lower_y+0)*m_num_samples_w];
        Vector2f p10 = m_pSampleGradients[(lower_x+0) + (lower_y+1)*m_num_samples_w];
        Vector2f p11 = m_pSampleGradients[(lower_x+1) + (lower_y+1)*m_num_samples_w];

        float frac_x = coord.x - (lower_x);
        float frac_y = coord.y - (lower_y);

        outGradient.x = (1.f-frac_x)*(1.f-frac_y)*p00.x + frac_x*(1.f-frac_y)*p01.x + (1.f-frac_x)*frac_y*p10.x + frac_x*frac_y*p11.x;
        outGradient.y = (1.f-frac_x)*(1.f-frac_y)*p00.y + frac_x*(1.f-frac_y)*p01.y + (1.f-frac_x)*frac_y*p10.y + frac_x*frac_y*p11.y;
    }

    void updateGradients()
    {
        Vector2f grad = new Vector2f();
	    final float u_scale = 0.5f*(m_num_samples_w-3)/m_UVToWorldScale.x;
        final float v_scale = 0.5f*(m_num_samples_l-3)/m_UVToWorldScale.y;
        for(int vi = 1; vi != m_num_samples_l-1; ++vi) {
            for(int ui = 1; ui != m_num_samples_w-1; ++ui) {
                float u_neg = m_pSampleDisplacements[(ui-1) + (vi+0)*m_num_samples_w].z;
                float u_pos = m_pSampleDisplacements[(ui+1) + (vi+0)*m_num_samples_w].z;
                float v_neg = m_pSampleDisplacements[(ui+0) + (vi-1)*m_num_samples_w].z;
                float v_pos = m_pSampleDisplacements[(ui+0) + (vi+1)*m_num_samples_w].z;

                grad.x = u_scale*(u_pos-u_neg);
                grad.y = v_scale*(v_pos-v_neg);

                m_pSampleGradients[ui + vi*m_num_samples_w].set(do_rot(grad,m_UVToWorldRotation));
            }
        }
    }

    void init()
    {
        /*HRESULT hr;

        SAFE_RELEASE(m_pFX);
        ID3DXBuffer* pEffectBuffer = NULL;
        V_RETURN(LoadFile(TEXT(".\\Media\\ocean_surface_heights_d3d11.fxo"), &pEffectBuffer));
        V_RETURN(D3DX11CreateEffectFromMemory(pEffectBuffer->GetBufferPointer(), pEffectBuffer->GetBufferSize(), 0, m_pd3dDevice, &m_pFX));
        pEffectBuffer->Release();*/

        gl = GLFuncProviderFactory.getGLFuncProvider();
        mFbo = new RenderTargets();
        m_pRenderSurfaceToReverseLookupTechnique = ShaderManager.getInstance().getProgram("RenderSurfaceToReverseLookupTech");
        m_pRenderQuadToUITechnique = ShaderManager.getInstance().getProgram("RenderQuadToUITech");
        m_pRenderMarkerTechnique = ShaderManager.getInstance().getProgram("RenderMarkerTech");

        /*m_pNumQuadsWVariable = m_pFX->GetVariableByName("g_numQuadsW")->AsScalar();
        m_pNumQuadsHVariable = m_pFX->GetVariableByName("g_numQuadsH")->AsScalar();
        m_pQuadUVDimsVariable = m_pFX->GetVariableByName("g_quadUVDims")->AsVector();
        m_pSrcUVToWorldScaleVariable = m_pFX->GetVariableByName("g_srcUVToWorldScale")->AsVector();
        m_pSrcUVToWorldRotationVariable = m_pFX->GetVariableByName("g_srcUVToWorldRot")->AsVector();
        m_pSrcUVToWorldOffsetVariable = m_pFX->GetVariableByName("g_srcUVToWorldOffset")->AsVector();
        m_pWorldToClipScaleVariable = m_pFX->GetVariableByName("g_worldToClipScale")->AsVector();
        m_pClipToWorldRotationVariable = m_pFX->GetVariableByName("g_clipToWorldRot")->AsVector();
        m_pClipToWorldOffsetVariable = m_pFX->GetVariableByName("g_clipToWorldOffset")->AsVector();
        m_pTexDiffuseVariable = m_pFX->GetVariableByName("g_texDiffuse")->AsShaderResource();
        m_pTexLookupVariable = m_pFX->GetVariableByName("g_texLookup")->AsShaderResource();

        m_pMatViewProjVariable = m_pFX->GetVariableByName("g_matViewProj")->AsMatrix();
        m_pMatWorldVariable = m_pFX->GetVariableByName("g_matWorld")->AsMatrix();
        m_pWorldToUVScaleVariable =m_pFX->GetVariableByName("g_worldToUVScale")->AsVector();
        m_pWorldToUVRotationVariable =m_pFX->GetVariableByName("g_worldToUVRot")->AsVector();
        m_pWorldToUVOffsetVariable =m_pFX->GetVariableByName("g_worldToUVOffset")->AsVector();

        UINT NumSimulationShaderInputs = GFSDK_WaveWorks_Simulation_GetShaderInputCountD3D11();
        m_pWaterSimulationShaderInputMappings = new UINT[NumSimulationShaderInputs];

        D3DX11_PASS_SHADER_DESC passShaderDesc;
        ID3DX11EffectPass* pRenderSurfaceToReverseLookupPass = m_pRenderSurfaceToReverseLookupTechnique->GetPassByIndex(0);

        V_RETURN(pRenderSurfaceToReverseLookupPass->GetVertexShaderDesc(&passShaderDesc));
        ID3D11ShaderReflection* pShadedReflectionVS = GetReflection(passShaderDesc);

        V_RETURN(pRenderSurfaceToReverseLookupPass->GetHullShaderDesc(&passShaderDesc));
        ID3D11ShaderReflection* pShadedReflectionHS = GetReflection(passShaderDesc);

        V_RETURN(pRenderSurfaceToReverseLookupPass->GetDomainShaderDesc(&passShaderDesc));
        ID3D11ShaderReflection* pShadedReflectionDS = GetReflection(passShaderDesc);

        V_RETURN(pRenderSurfaceToReverseLookupPass->GetPixelShaderDesc(&passShaderDesc));
        ID3D11ShaderReflection* pShadedReflectionPS = GetReflection(passShaderDesc);

        for(UINT i = 0; i != NumSimulationShaderInputs; ++i)
        {
            GFSDK_WaveWorks_ShaderInput_Desc inputDesc;
            GFSDK_WaveWorks_Simulation_GetShaderInputDescD3D11(i, &inputDesc);

            m_pWaterSimulationShaderInputMappings[i] = GetShaderInputRegisterMapping(pShadedReflectionVS, pShadedReflectionHS, pShadedReflectionDS, pShadedReflectionPS, inputDesc);
        }*/

        {
//            SAFE_RELEASE(m_pLookupSRV);
//            SAFE_RELEASE(m_pLookupRTV);
//            ID3D11Texture2D* pTexture = NULL;

            // Set up textures for rendering hull profile
            Texture2DDesc tex_desc = new Texture2DDesc();
            tex_desc.width = (m_num_gpu_samples_w-2);
            tex_desc.height = (m_num_gpu_samples_l-2);
            tex_desc.arraySize = 1;
//            tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;
//            tex_desc.CPUAccessFlags = 0;
            tex_desc.format = DXGI_FORMAT_R32G32B32A32_FLOAT;
            tex_desc.mipLevels = 1;
//            tex_desc.MiscFlags = 0;
//            tex_desc.SampleDesc.Count = 1;
//            tex_desc.SampleDesc.Quality = 0;
//            tex_desc.Usage = D3D11_USAGE_DEFAULT;

//            V_RETURN(m_pd3dDevice->CreateTexture2D(&tex_desc,NULL,&pTexture));
//            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pTexture,NULL,&m_pLookupSRV));
//            V_RETURN(m_pd3dDevice->CreateRenderTargetView(pTexture,NULL,&m_pLookupRTV));
//            SAFE_RELEASE(pTexture);

            m_pLookupSRV = m_pLookupRTV = TextureUtils.createTexture2D(tex_desc, null);
        }

//        return S_OK;
    }

    void renderTextureToUI(/*ID3D11DeviceContext* pDC*/)
    {
        /*m_pTexDiffuseVariable->SetResource(getGPULookupSRV());
        m_pRenderQuadToUITechnique->GetPassByIndex(0)->Apply(0, pDC);

        pDC->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
        pDC->IASetInputLayout(NULL);
        pDC->Draw(4,0);

        m_pTexDiffuseVariable->SetResource(NULL);
        m_pRenderQuadToUITechnique->GetPassByIndex(0)->Apply(0, pDC);*/
    }

    void getGPUWorldToUVTransform(Vector2f offset, Vector2f rot, Vector2f scale)
    {
        // To match CPU sampling, a sample at the min corner of the footprint (worldToUVOffset) should map to (0,0) whereas a sample
        // at the max corner should map to (m_num_gpu_samples_w-3,m_num_gpu_samples_l-3), hence...
	    final float world_quad_size_w = m_UVToWorldScale.x/(m_num_gpu_samples_w-3);
        final float world_quad_size_h = m_UVToWorldScale.y/(m_num_gpu_samples_l-3);
        scale.x = 1.f/(m_UVToWorldScale.x+world_quad_size_w);
        scale.y = 1.f/(m_UVToWorldScale.y+world_quad_size_h);
        offset.x = -m_UVToWorldOffset.x;
        offset.y = -m_UVToWorldOffset.y;
        rot.x = m_UVToWorldRotation.x;
        rot.y = -m_UVToWorldRotation.y;
    }

    Texture2D getGPULookupSRV() { return m_pLookupSRV; }

    void renderMarkerArray(Matrix4f matViewProj, Matrix4f matWorld)
    {
//        m_pTexLookupVariable->SetResource(getGPULookupSRV());
        m_TechParams.g_texLookup = getGPULookupSRV();
//        m_pMatViewProjVariable->SetMatrix((float*)&matViewProj);
        m_TechParams.g_matViewProj = matViewProj;
//        m_pMatWorldVariable->SetMatrix((float*)&matWorld);
        m_TechParams.g_matWorld = matWorld;

        Vector2f worldToUVScale = m_TechParams.g_worldToUVScale;
        Vector2f worldToUVOffset = m_TechParams.g_worldToUVOffset;
        Vector2f worldToUVRot = m_TechParams.g_worldToUVRot;
        getGPUWorldToUVTransform(worldToUVOffset, worldToUVRot, worldToUVScale);

//        m_pWorldToUVScaleVariable->SetFloatVector((float*)&worldToUVScale);
//        m_pWorldToUVRotationVariable->SetFloatVector((float*)&worldToUVRot);
//        m_pWorldToUVOffsetVariable->SetFloatVector((float*)&worldToUVOffset);

//        m_pRenderMarkerTechnique->GetPassByIndex(0)->Apply(0, pDC);
        m_pRenderMarkerTechnique.enable(m_TechParams);

        /*pDC->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        pDC->IASetInputLayout(NULL);
        pDC->Draw(300,0);*/

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 300);


//        m_pTexLookupVariable->SetResource(NULL);
//        m_pRenderMarkerTechnique->GetPassByIndex(0)->Apply(0, pDC);
    }

    void updateGPUHeights(/*ID3D11DeviceContext* pDC, */GFSDK_WaveWorks_Simulation hSim, Matrix4f matView)
    {
        // Not strictly necessary, but good for debugging and SLI
        FloatBuffer clearColor = CacheBuffer.wrap(1.f,0.f,1.f,1.f);
//        pDC->ClearRenderTargetView(m_pLookupRTV, clearColor);
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glClearTexImage(m_pLookupRTV.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_FLOAT, clearColor);

        // We inflate the rendered area by the conservative displacement amount to make sure we overlap the target
        // with enough slop to absorb displacements
        final float min_inflate_amount = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetConservativeMaxDisplacementEstimate(hSim);

        // Expand number of quads to match slop
        final float world_quad_size_w = m_UVToWorldScale.x/(m_num_gpu_samples_w-3);
        final float world_quad_size_h = m_UVToWorldScale.y/(m_num_gpu_samples_l-3);
        final int extra_quads_w = (int)Math.ceil(min_inflate_amount/world_quad_size_w);
        final int extra_quads_h = (int)Math.ceil(min_inflate_amount/world_quad_size_h);
        final float inflate_w = (extra_quads_w)*world_quad_size_w;
        final float inflate_h = (extra_quads_h)*world_quad_size_h;
        final int quads_w = (m_num_gpu_samples_w-2) + 2 * extra_quads_w;
        final int quads_h = (m_num_gpu_samples_l-2) + 2 * extra_quads_h;

        // Note that our CPU sampling scheme is somewhat different from conventional GPU sampling,
        // hence we have an extra quad's worth of world space in play to ensure coverage of all the
        // texels in the lookup
        Vector2f srcUVToWorld_additional_offset = new Vector2f(-inflate_w-0.5f*world_quad_size_w,-inflate_h-0.5f*world_quad_size_h);
        srcUVToWorld_additional_offset = do_rot(srcUVToWorld_additional_offset,m_UVToWorldRotation);

        Vector2f srcUVToWorldOffset = new Vector2f(m_UVToWorldOffset);
        srcUVToWorldOffset.x += srcUVToWorld_additional_offset.x;
        srcUVToWorldOffset.y += srcUVToWorld_additional_offset.y;

        Vector2f srcUVToWorldScale = m_UVToWorldScale.clone();
        srcUVToWorldScale.x += 2.f * inflate_w + world_quad_size_w;
        srcUVToWorldScale.y += 2.f * inflate_h + world_quad_size_h;

//        m_pSrcUVToWorldScaleVariable->SetFloatVector((FLOAT*)&srcUVToWorldScale);
        m_TechParams.g_srcUVToWorldScale.set(srcUVToWorldScale.x,srcUVToWorldScale.y);
//        m_pSrcUVToWorldRotationVariable->SetFloatVector((FLOAT*)&m_UVToWorldRotation);
        m_TechParams.g_srcUVToWorldRot.set(m_UVToWorldRotation.x, m_UVToWorldRotation.y);
//        m_pSrcUVToWorldOffsetVariable->SetFloatVector((FLOAT*)&srcUVToWorldOffset);
        m_TechParams.g_srcUVToWorldOffset.set(srcUVToWorldOffset.x, srcUVToWorldOffset.y);

        // World to clip are inverse of vanilla forwards, but with the additional half-quad offset to
        // match CPU sampling scheme
        Vector2f worldToClipScale = new Vector2f();
        worldToClipScale.x = 1.f/(m_UVToWorldScale.x+world_quad_size_w);
        worldToClipScale.y = 1.f/(m_UVToWorldScale.y+world_quad_size_h);

        Vector2f clipToWorld_additional_offset = new Vector2f(-0.5f*world_quad_size_w,-0.5f*world_quad_size_h);
        clipToWorld_additional_offset = do_rot(clipToWorld_additional_offset,m_UVToWorldRotation);

        Vector2f clipToWorldOffset = m_UVToWorldOffset.clone();
        clipToWorldOffset.x += clipToWorld_additional_offset.x;
        clipToWorldOffset.y += clipToWorld_additional_offset.y;

//        m_pWorldToClipScaleVariable->SetFloatVector((FLOAT*)&worldToClipScale);
        m_TechParams.g_worldToClipScale = worldToClipScale;
//        m_pClipToWorldRotationVariable->SetFloatVector((FLOAT*)&m_UVToWorldRotation);
        m_TechParams.g_clipToWorldRot = m_UVToWorldRotation;
//        m_pClipToWorldOffsetVariable->SetFloatVector((FLOAT*)&clipToWorldOffset);
        m_TechParams.g_clipToWorldOffset = clipToWorldOffset;

        // Quads setup
        float quadsWH[] = {quads_w,(quads_h)};
//        m_pNumQuadsWVariable->SetFloat(quadsWH[0]);
        m_TechParams.g_numQuadsW = quads_w;
//        m_pNumQuadsHVariable->SetFloat(quadsWH[1]);
        m_TechParams.g_numQuadsH = quads_h;
        float quadUVdims[] = {1.f/(quads_w),1.f/(quads_h)};
//        m_pQuadUVDimsVariable->SetFloatVector(quadUVdims);
        m_TechParams.g_quadUVDims.set(1.f/(quads_w),1.f/(quads_h));

        // Preserve original viewports
        /*D3D11_VIEWPORT original_viewports[D3D11_VIEWPORT_AND_SCISSORRECT_OBJECT_COUNT_PER_PIPELINE];
        UINT num_original_viewports = sizeof(original_viewports)/sizeof(original_viewports[0]);
        pDC->RSGetViewports( &num_original_viewports, original_viewports);*/

        // RTV setup
        /*pDC->OMSetRenderTargets(1,&m_pLookupRTV,NULL);
	    const D3D11_VIEWPORT viewport = {0.f, 0.f, FLOAT(m_num_gpu_samples_w-2), FLOAT(m_num_gpu_samples_l-2), 0.f, 1.f };
        pDC->RSSetViewports(1,&viewport);*/
        mFbo.bind();
        mFbo.setRenderTexture(m_pLookupRTV, null);
        gl.glViewport(0,0,m_num_gpu_samples_w-2, m_num_gpu_samples_l-2);

        // Render
//        m_pRenderSurfaceToReverseLookupTechnique->GetPassByIndex(0)->Apply(0, pDC);
        m_pRenderSurfaceToReverseLookupTechnique.enable(m_TechParams);
        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetRenderStateD3D11(hSim,matView, m_pWaterSimulationShaderInputMappings, null);
        /*pDC->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_4_CONTROL_POINT_PATCHLIST);
        pDC->IASetInputLayout(NULL);
        pDC->Draw(4*quads_w*quads_h,0);*/
        gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES, 4);
        gl.glDrawArrays(GLenum.GL_PATCHES, 0, 4*quads_w*quads_h);

        // Restore viewports
//        pDC->RSSetViewports(num_original_viewports,original_viewports);
    }
}
