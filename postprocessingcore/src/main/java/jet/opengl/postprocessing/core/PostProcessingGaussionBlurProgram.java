package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/3/13.
 */

public class PostProcessingGaussionBlurProgram extends GLSLProgram {

    private int halfPixelSizeIndex;
    public PostProcessingGaussionBlurProgram(int kernel) throws IOException{
        if (kernel < 1)
            // kernel less 1 we could igore the gauss blur.
            throw new IllegalArgumentException("Invalid kernel.");

        kernel = Math.max(3, kernel);
        int n = (kernel + 1) / 4;
        if (n >= 1 && (kernel + 1) % 4 != 0)
        {
            // rounded up
            n++;
        }

        kernel = 4 * n - 1;

        WeightsAndOffsets gaussionData = GenerateGaussShaderKernelWeightsAndOffsets(kernel);

        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingGaussionBlurPS.frag",
                new Macro("SAMPLES", gaussionData.weights.length));
        enable();

        {  // Setup the init data.
            int iChannel0Loc = getUniformLocation("g_Texture");
            gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
            int weightsLoc = getUniformLocation("g_Weights");
            gl.glUniform1fv(weightsLoc, CacheBuffer.wrap(gaussionData.weights));
            weightsLoc = getUniformLocation("g_Offsets");
            gl.glUniform1fv(weightsLoc, CacheBuffer.wrap(gaussionData.offsets));
        }

        halfPixelSizeIndex = getUniformLocation("g_HalfPixelSize");
    }

    public void setHalfPixelSize(float x, float y){
        if(halfPixelSizeIndex >= 0){
            gl.glUniform2f(halfPixelSizeIndex, x,y);
        }
    }

    private static double[] GenerateSeparableGaussKernel( double sigma, int kernelSize )
    {
        /*
        if( (kernelSize % 2) != 1 )
        {
            assert( false ); // kernel size must be odd number
            return std::vector<double>();
        }*/

        int halfKernelSize = kernelSize/2;

//        std::vector<double> kernel;
//        kernel.resize( kernelSize );
        double[] kernel = new double[kernelSize];

        final double cPI= 3.14159265358979323846;
        double mean     = halfKernelSize;
        double sum      = 0.0;
        for (int x = 0; x < kernelSize; ++x)
        {
            kernel[x] = Math.sqrt( Math.exp( -0.5 * (Math.pow((x-mean)/sigma, 2.0) + Math.pow((mean)/sigma,2.0)) )
                    / (2 * cPI * sigma * sigma) );
            sum += kernel[x];
        }
        for (int x = 0; x < kernelSize; ++x)
            kernel[x] /= (float)sum;

        return kernel;
    }

    private static float[] GetAppropriateSeparableGauss(int kernelSize){
        // Search for sigma to cover the whole kernel size with sensible values (might not be ideal for all cases quality-wise but is good enough for performance testing)
        final double epsilon = 2e-2f / kernelSize;
        double searchStep = 1.0;
        double sigma = 1.0;
        while( true )
        {

            double[] kernelAttempt = GenerateSeparableGaussKernel( sigma, kernelSize );
            if( kernelAttempt[0] > epsilon )
            {
                if( searchStep > 0.02 )
                {
                    sigma -= searchStep;
                    searchStep *= 0.1;
                    sigma += searchStep;
                    continue;
                }

                float[] retVal = new float[kernelSize];
                for (int i = 0; i < kernelSize; i++)
                    retVal[i] = (float)kernelAttempt[i];
                return retVal;
            }

            sigma += searchStep;

            if( sigma > 1000.0 )
            {
                assert( false ); // not tested, preventing infinite loop
                break;
            }
        }

        return null;
    }

    private static WeightsAndOffsets GenerateGaussShaderKernelWeightsAndOffsets(int kernelSize){
        // Gauss filter kernel & offset creation
        float[] inputKernel = GetAppropriateSeparableGauss(kernelSize);

        float[] oneSideInputs = new float[kernelSize/2 + 1];
        for( int i = (kernelSize/2); i >= 0; i-- )
        {
            if( i == (kernelSize/2) )
                oneSideInputs[i] = ( (float)inputKernel[i] * 0.5f );
            else
                oneSideInputs[i] = ( (float)inputKernel[i] );
        }

        assert( (oneSideInputs.length % 2) == 0 );
        int numSamples = oneSideInputs.length/2;

        float[] weights = new float[numSamples];

        for( int i = 0; i < numSamples; i++ )
        {
            float sum = oneSideInputs[i*2+0] + oneSideInputs[i*2+1];
            weights[i] = sum;
        }

        float[] offsets = new float[numSamples];

        for( int i = 0; i < numSamples; i++ )
        {
            offsets[i] = ( i*2.0f + oneSideInputs[i*2+1] / weights[i] );
        }

        return new WeightsAndOffsets(weights, offsets);
    }

    private static final class WeightsAndOffsets{
        float[] weights;
        float[] offsets;

        WeightsAndOffsets(){}
        WeightsAndOffsets(float[] weights, float[] offsets){
            this.weights = weights;
            this.offsets = offsets;
        }
    }
}
