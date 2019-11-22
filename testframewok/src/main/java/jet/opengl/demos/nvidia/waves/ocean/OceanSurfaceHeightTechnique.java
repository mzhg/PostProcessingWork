package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.CacheBuffer;

final class OceanSurfaceHeightTechnique extends Technique{

    private int m_g_worldToUVScaleLoc = -1;
    private int m_g_clipToWorldOffsetLoc = -1;
    private int m_g_worldToUVOffsetLoc = -1;
    private int m_g_worldToUVRotLoc = -1;
    private int m_g_numQuadsWLoc = -1;
    private int m_g_quadScaleLoc = -1;
    private int m_g_matViewProjLoc = -1;
    private int m_g_clipToWorldRotLoc = -1;
    private int m_g_matWorldLoc = -1;
    private int m_g_worldToClipScaleLoc = -1;
    private int m_g_srcUVToWorldScaleLoc = -1;
    private int m_g_quadUVDimsLoc = -1;
    private int m_g_srcUVToWorldRotLoc = -1;
    private int m_g_texDiffuseLoc = -1;
    private int m_g_numQuadsHLoc = -1;
    private int m_g_srcUVToWorldOffsetLoc = -1;

    private boolean m_inited;

    private void init(){
        // TODO Don't forget initlize the program here.
        m_g_worldToUVScaleLoc = gl.glGetUniformLocation(m_program, "g_worldToUVScale");
        m_g_clipToWorldOffsetLoc = gl.glGetUniformLocation(m_program, "g_clipToWorldOffset");
        m_g_worldToUVOffsetLoc = gl.glGetUniformLocation(m_program, "g_worldToUVOffset");
        m_g_worldToUVRotLoc = gl.glGetUniformLocation(m_program, "g_worldToUVRot");
        m_g_numQuadsWLoc = gl.glGetUniformLocation(m_program, "g_numQuadsW");
        m_g_quadScaleLoc = gl.glGetUniformLocation(m_program, "g_quadScale");
        m_g_matViewProjLoc = gl.glGetUniformLocation(m_program, "g_matViewProj");
        m_g_clipToWorldRotLoc = gl.glGetUniformLocation(m_program, "g_clipToWorldRot");
        m_g_matWorldLoc = gl.glGetUniformLocation(m_program, "g_matWorld");
        m_g_worldToClipScaleLoc = gl.glGetUniformLocation(m_program, "g_worldToClipScale");
        m_g_srcUVToWorldScaleLoc = gl.glGetUniformLocation(m_program, "g_srcUVToWorldScale");
        m_g_quadUVDimsLoc = gl.glGetUniformLocation(m_program, "g_quadUVDims");
        m_g_srcUVToWorldRotLoc = gl.glGetUniformLocation(m_program, "g_srcUVToWorldRot");
        m_g_texDiffuseLoc = gl.glGetUniformLocation(m_program, "g_texDiffuse");
        m_g_numQuadsHLoc = gl.glGetUniformLocation(m_program, "g_numQuadsH");
        m_g_srcUVToWorldOffsetLoc = gl.glGetUniformLocation(m_program, "g_srcUVToWorldOffset");
    }
    
    
    private void setWorldToUVScale(Vector2f v) { if(m_g_worldToUVScaleLoc >=0)gl.glUniform2f(m_g_worldToUVScaleLoc, v.x, v.y);}
    private void setClipToWorldOffset(Vector2f v) { if(m_g_clipToWorldOffsetLoc >=0)gl.glUniform2f(m_g_clipToWorldOffsetLoc, v.x, v.y);}
    private void setWorldToUVOffset(Vector2f v) { if(m_g_worldToUVOffsetLoc >=0)gl.glUniform2f(m_g_worldToUVOffsetLoc, v.x, v.y);}
    private void setWorldToUVRot(Vector2f v) { if(m_g_worldToUVRotLoc >=0)gl.glUniform2f(m_g_worldToUVRotLoc, v.x, v.y);}
    private void setNumQuadsW(float f) { if(m_g_numQuadsWLoc >=0)gl.glUniform1f(m_g_numQuadsWLoc, f);}
    private void setQuadScale(Vector4f v) { if(m_g_quadScaleLoc >=0)gl.glUniform4f(m_g_quadScaleLoc, v.x, v.y, v.z, v.w);}
    private void setMatViewProj(Matrix4f mat) { if(m_g_matViewProjLoc >=0)gl.glUniformMatrix4fv(m_g_matViewProjLoc, false, CacheBuffer.wrap(mat));}
    private void setClipToWorldRot(Vector2f v) { if(m_g_clipToWorldRotLoc >=0)gl.glUniform2f(m_g_clipToWorldRotLoc, v.x, v.y);}
    private void setMatWorld(Matrix4f mat) { if(m_g_matWorldLoc >=0)gl.glUniformMatrix4fv(m_g_matWorldLoc, false, CacheBuffer.wrap(mat));}
    private void setWorldToClipScale(Vector2f v) { if(m_g_worldToClipScaleLoc >=0)gl.glUniform2f(m_g_worldToClipScaleLoc, v.x, v.y);}
    private void setSrcUVToWorldScale(Vector4f v) { if(m_g_srcUVToWorldScaleLoc >=0)gl.glUniform4f(m_g_srcUVToWorldScaleLoc, v.x, v.y, v.z, v.w);}
    private void setQuadUVDims(Vector2f v) { if(m_g_quadUVDimsLoc >=0)gl.glUniform2f(m_g_quadUVDimsLoc, v.x, v.y);}
    private void setSrcUVToWorldRot(Vector4f v) { if(m_g_srcUVToWorldRotLoc >=0)gl.glUniform4f(m_g_srcUVToWorldRotLoc, v.x, v.y, v.z, v.w);}
    private void setNumQuadsH(float f) { if(m_g_numQuadsHLoc >=0)gl.glUniform1f(m_g_numQuadsHLoc, f);}
    private void setSrcUVToWorldOffset(Vector4f v) { if(m_g_srcUVToWorldOffsetLoc >=0)gl.glUniform4f(m_g_srcUVToWorldOffsetLoc, v.x, v.y, v.z, v.w);}

    @Override
    public void enable(TechniqueParams params) {
        super.enable(params);

        OceanSurfaceHeightParams surfaceHeightParams = (OceanSurfaceHeightParams)params;

        if(!m_inited){
            init();
            m_inited = true;
        }

        setWorldToUVScale(surfaceHeightParams.g_worldToUVScale);
        setClipToWorldOffset(surfaceHeightParams.g_clipToWorldOffset);
        setWorldToUVOffset(surfaceHeightParams.g_worldToUVOffset);
        setWorldToUVRot(surfaceHeightParams.g_worldToUVRot);
        setNumQuadsW(surfaceHeightParams.g_numQuadsW);
        setQuadScale(surfaceHeightParams.g_quadScale);
        setMatViewProj(surfaceHeightParams.g_matViewProj);
        setClipToWorldRot(surfaceHeightParams.g_clipToWorldRot);
        setMatWorld(surfaceHeightParams.g_matWorld);
        setWorldToClipScale(surfaceHeightParams.g_worldToClipScale);
        setSrcUVToWorldScale(surfaceHeightParams.g_srcUVToWorldScale);
        setQuadUVDims(surfaceHeightParams.g_quadUVDims);
        setSrcUVToWorldRot(surfaceHeightParams.g_srcUVToWorldRot);
        setNumQuadsH(surfaceHeightParams.g_numQuadsH);
        setSrcUVToWorldOffset(surfaceHeightParams.g_srcUVToWorldOffset);

        bindTexture(0, surfaceHeightParams.g_texLookup, 0);
    }
}
