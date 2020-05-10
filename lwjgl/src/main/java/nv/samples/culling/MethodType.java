package nv.samples.culling;

enum MethodType {
    METHOD_FRUSTUM, // test boxes against frustum only
    METHOD_HIZ,     // test boxes against hiz texture
    METHOD_RASTER,  // test boxes against current dept-buffer of current fbo
    NUM_METHODS,
}
