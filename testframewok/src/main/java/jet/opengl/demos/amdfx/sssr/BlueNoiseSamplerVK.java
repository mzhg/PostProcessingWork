package jet.opengl.demos.amdfx.sssr;

import jet.opengl.postprocessing.buffer.BufferGL;

/**
 The BlueNoiseSamplerVK class represents a blue-noise sampler to be used for random number generation.

 \note Original implementation can be found here: https://eheitzresearch.wordpress.com/762-2/
 */
class BlueNoiseSamplerVK {
    // The Sobol sequence buffer.
    BufferGL sobol_buffer_;
    // The ranking tile buffer for sampling.
    BufferGL ranking_tile_buffer_;
    // The scrambling tile buffer for sampling.
    BufferGL scrambling_tile_buffer_;
}
