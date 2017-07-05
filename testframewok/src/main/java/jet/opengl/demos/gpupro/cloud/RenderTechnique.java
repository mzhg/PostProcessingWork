package jet.opengl.demos.gpupro.cloud;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/7/5.
 */

final class RenderTechnique extends GLSLProgram{
    private int mHeightLoc = -1;
    private int m_invMaxLoc = -1;
    private int mGround3Loc = -1;
    private int mGround2Loc = -1;
    private int mShadowLoc = -1;
    private int mParamLoc = -1;
    private int mGround4Loc = -1;
    private int mGround1Loc = -1;
    private int mGround0Loc = -1;
    private int mSpcLoc = -1;
    private int mL2SLoc = -1;
    private int mCloudCoverLoc = -1;
    private int mL2WLoc = -1;
    private int m_litAmbLoc = -1;
    private int mDensityLoc = -1;
    private int mLitLoc = -1;
    private int mDistanceLoc = -1;
    private int m_litColLoc = -1;
    private int mCloudLoc = -1;
    private int mL2CLoc = -1;
    private int mUVParamLoc = -1;
    private int mDifLoc = -1;
    private int mC2WLoc = -1;
    private int mW2CLoc = -1;
    private int m_scatLoc = -1;
    private int mEyeLoc = -1;
    private int mXZParamLoc = -1;
    private int mOffLoc = -1;
    private int mFallOffLoc = -1;
    private int mGroundBlendLoc = -1;
    private int mAmbLoc = -1;
    private int m_litDirLoc = -1;
    private int mPixLoc = -1;

    RenderTechnique(String vertfile, String fragfile){
        this(vertfile, fragfile, null);
    }

    RenderTechnique(String vertfile, String fragfile, String macro){
        final String path = "gpupro/Cloud/shaders/";

        setSourceFromStrings(path + vertfile, path + fragfile, macro != null ? new Macro(macro, 1) : null);
        initUniforms();
    }

    private void initUniforms(){
        mHeightLoc = gl.glGetUniformLocation(m_program, "vHeight");
        m_invMaxLoc = gl.glGetUniformLocation(m_program, "invMax");
        mGround3Loc = gl.glGetUniformLocation(m_program, "sGround3");
        mGround2Loc = gl.glGetUniformLocation(m_program, "sGround2");
        mShadowLoc = gl.glGetUniformLocation(m_program, "sShadow");
        mParamLoc = gl.glGetUniformLocation(m_program, "vParam");
        mGround4Loc = gl.glGetUniformLocation(m_program, "sGround4");
        mGround1Loc = gl.glGetUniformLocation(m_program, "sGround1");
        mGround0Loc = gl.glGetUniformLocation(m_program, "sGround0");
        mSpcLoc = gl.glGetUniformLocation(m_program, "mSpc");
        mLitLoc = gl.glGetUniformLocation(m_program, "sLit");
        mL2SLoc = gl.glGetUniformLocation(m_program, "mL2S");
        mCloudCoverLoc = gl.glGetUniformLocation(m_program, "fCloudCover");
        mL2WLoc = gl.glGetUniformLocation(m_program, "mL2W");
        m_litAmbLoc = gl.glGetUniformLocation(m_program, "litAmb");
        mDensityLoc = gl.glGetUniformLocation(m_program, "sDensity");
        mLitLoc = gl.glGetUniformLocation(m_program, "cLit");
        mDistanceLoc = gl.glGetUniformLocation(m_program, "vDistance");
        m_litColLoc = gl.glGetUniformLocation(m_program, "litCol");
        mCloudLoc = gl.glGetUniformLocation(m_program, "sCloud");
        mL2CLoc = gl.glGetUniformLocation(m_program, "mL2C");
        mUVParamLoc = gl.glGetUniformLocation(m_program, "vUVParam");
        mDifLoc = gl.glGetUniformLocation(m_program, "mDif");
        mC2WLoc = gl.glGetUniformLocation(m_program, "mC2W");
        mW2CLoc = gl.glGetUniformLocation(m_program, "mW2C");
        m_scatLoc = gl.glGetUniformLocation(m_program, "scat");
        mEyeLoc = gl.glGetUniformLocation(m_program, "vEye");
        mXZParamLoc = gl.glGetUniformLocation(m_program, "vXZParam");
        mOffLoc = gl.glGetUniformLocation(m_program, "vOff");
        mFallOffLoc = gl.glGetUniformLocation(m_program, "vFallOff");
        mGroundBlendLoc = gl.glGetUniformLocation(m_program, "sGroundBlend");
        mAmbLoc = gl.glGetUniformLocation(m_program, "cAmb");
        m_litDirLoc = gl.glGetUniformLocation(m_program, "litDir");
        mPixLoc = gl.glGetUniformLocation(m_program, "vPix");
    }

    public void setHeight(Vector2f v) { if(mHeightLoc >=0)gl.glUniform2f(mHeightLoc, v.x, v.y);}
    public void setInvMax(Vector2f v) { if(m_invMaxLoc >=0)gl.glUniform2f(m_invMaxLoc, v.x, v.y);}
    
    public void setParam(Vector3f v) { if(mParamLoc >=0)gl.glUniform3f(mParamLoc, v.x, v.y, v.z);}
    public void setSpc(Vector4f v) { if(mSpcLoc >=0)gl.glUniform4f(mSpcLoc, v.x, v.y, v.z, v.w);}
    public void setL2S(Matrix4f mat) { if(mL2SLoc >=0)gl.glUniformMatrix4fv(mL2SLoc, false, CacheBuffer.wrap(mat));}
    public void setCloudCover(float f) { if(mCloudCoverLoc >=0)gl.glUniform1f(mCloudCoverLoc, f);}
    public void setL2W(Matrix4f mat) { if(mL2WLoc >=0)gl.glUniformMatrix4fv(mL2WLoc, false, CacheBuffer.wrap(mat));}
    public void setLitAmb(Vector3f v) { if(m_litAmbLoc >=0)gl.glUniform3f(m_litAmbLoc, v.x, v.y, v.z);}
    
    public void setLit(Vector3f v) { if(mLitLoc >=0)gl.glUniform3f(mLitLoc, v.x, v.y, v.z);}
    public void setDistance(Vector2f v) { if(mDistanceLoc >=0)gl.glUniform2f(mDistanceLoc, v.x, v.y);}
    public void setLitCol(Vector3f v) { if(m_litColLoc >=0)gl.glUniform3f(m_litColLoc, v.x, v.y, v.z);}
    
    public void setL2C(Matrix4f mat) { if(mL2CLoc >=0)gl.glUniformMatrix4fv(mL2CLoc, false, CacheBuffer.wrap(mat));}
    public void setUVParam(Vector4f v) { if(mUVParamLoc >=0)gl.glUniform4f(mUVParamLoc, v.x, v.y, v.z, v.w);}
    public void setDif(Vector4f v) { if(mDifLoc >=0)gl.glUniform4f(mDifLoc, v.x, v.y, v.z, v.w);}
    public void setC2W(Matrix4f mat) { if(mC2WLoc >=0)gl.glUniformMatrix4fv(mC2WLoc, false, CacheBuffer.wrap(mat));}
    public void setW2C(Matrix4f mat) { if(mW2CLoc >=0)gl.glUniformMatrix4fv(mW2CLoc, false, CacheBuffer.wrap(mat));}
    public void setScat(SScatteringShaderParameters v) {
        if(m_scatLoc >=0){
            gl.glUniform4fv(m_scatLoc, v.toFloats());
        }
    }
    public void setEye(Vector3f v) { if(mEyeLoc >=0)gl.glUniform3f(mEyeLoc, v.x, v.y, v.z);}
    public void setXZParam(Vector4f v) { if(mXZParamLoc >=0)gl.glUniform4f(mXZParamLoc, v.x, v.y, v.z, v.w);}
    public void setOff(Vector4f v) { if(mOffLoc >=0)gl.glUniform4f(mOffLoc, v.x, v.y, v.z, v.w);}
    public void setFallOff(Vector4f v) { if(mFallOffLoc >=0)gl.glUniform4f(mFallOffLoc, v.x, v.y, v.z, v.w);}
    
    public void setAmb(Vector3f v) { if(mAmbLoc >=0)gl.glUniform3f(mAmbLoc, v.x, v.y, v.z);}
    public void setLitDir(Vector3f v) { if(m_litDirLoc >=0)gl.glUniform3f(m_litDirLoc, v.x, v.y, v.z);}
    public void setPix(Vector2f v) { if(mPixLoc >=0)gl.glUniform2f(mPixLoc, v.x, v.y);}
}
