package nv.samples.culling;

import jet.opengl.postprocessing.texture.Texture2D;

abstract class Job {
    int     m_numObjects;
    // world-space matrices {mat4 world, mat4 worldInverseTranspose}
    BufferValue m_bufferMatrices;
    BufferValue  m_bufferBboxes; // only used in dualindex mode (2 x vec4)
    // 1 32-bit integer per object (index)
    BufferValue  m_bufferObjectMatrix;
    // object-space bounding box (2 x vec4)
    // or 1 32-bit integer per object (dualindex mode)
    BufferValue m_bufferObjectBbox;

    // 1 32-bit integer per object
    BufferValue  m_bufferVisOutput;

    // 1 32-bit integer per 32 objects (1 bit per object)
    BufferValue  m_bufferVisBitsCurrent;
    BufferValue  m_bufferVisBitsLast;

    // for HiZ
    Texture2D m_textureDepthWithMipmaps;

    // derive from this class and implement this function how you want to
    // deal with the results that are provided in the buffer
    abstract void resultFromBits( BufferValue bufferVisBitsCurrent );
    // for readback methods we need to wait for a result
    void resultClient() {};
}
