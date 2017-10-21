package jet.opengl.demos.nvidia.sparkles;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ProgramLinkTask;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/10/20.
 */

public class SimpleLightProgram extends GLSLProgram {
    public static final String POSITION_ATTRIB_NAME = "In_Position";
    public static final String TEXTURE_ATTRIB_NAME = "In_Texcoord";
    public static final String NORMAL_ATTRIB_NAME = "In_Normal";
    public static final String COLOR_ATTRIB_NAME = "In_Color";

    private int m_g_ModelViewProjLoc = -1;
    private int m_g_LightSpecularLoc = -1;
    private int m_g_EyePosLoc = -1;
    private int m_g_LightAmbientLoc = -1;
    private int m_g_ColorLoc = -1;
    private int m_g_MaterialSpecularLoc = -1;
    private int m_g_LightPosLoc = -1;
    private int m_g_LightDiffuseLoc = -1;
    private int m_g_ModelLoc = -1;
    private int m_g_MaterialDiffuseLoc = -1;
    private int m_g_MaterialAmbientLoc = -1;

    private int normalAttribLoc;
    private int colorAttribLoc;
    private int posLoc;
    private int texLoc;

    public SimpleLightProgram(boolean uniform, ProgramLinkTask task) throws IOException {
        addLinkTask(task);
        final String path = "nvidia/sparkles/";
        if(uniform){
            setSourceFromFiles(path+"SimpleLightUniformColorVS.vert", path+"SimpleLightUniformColorPS.frag");
        }else{
            setSourceFromFiles(path+"SimpleLightAttribColorVS.vert", path+"SimpleLightAttribColorPS.frag");
        }

        m_g_ModelViewProjLoc = gl.glGetUniformLocation(m_program, "g_ModelViewProj");
        m_g_LightSpecularLoc = gl.glGetUniformLocation(m_program, "g_LightSpecular");
        m_g_EyePosLoc = gl.glGetUniformLocation(m_program, "g_EyePos");
        m_g_LightAmbientLoc = gl.glGetUniformLocation(m_program, "g_LightAmbient");
        m_g_ColorLoc = gl.glGetUniformLocation(m_program, "g_Color");
        m_g_MaterialSpecularLoc = gl.glGetUniformLocation(m_program, "g_MaterialSpecular");
        m_g_LightPosLoc = gl.glGetUniformLocation(m_program, "g_LightPos");
        m_g_LightDiffuseLoc = gl.glGetUniformLocation(m_program, "g_LightDiffuse");
        m_g_ModelLoc = gl.glGetUniformLocation(m_program, "g_Model");
        m_g_MaterialDiffuseLoc = gl.glGetUniformLocation(m_program, "g_MaterialDiffuse");
        m_g_MaterialAmbientLoc = gl.glGetUniformLocation(m_program, "g_MaterialAmbient");

        findAttrib();

        enable();
        setTextureUniform("g_InputTex", 0);
        disable();
    }

    protected void findAttrib(){
        if(m_program == 0)
            throw new IllegalArgumentException("programID is 0.");
        posLoc = gl.glGetAttribLocation(m_program, POSITION_ATTRIB_NAME);
        texLoc = gl.glGetAttribLocation(m_program, TEXTURE_ATTRIB_NAME);
        normalAttribLoc = gl.glGetAttribLocation(m_program, NORMAL_ATTRIB_NAME);
        colorAttribLoc = gl.glGetAttribLocation(m_program, COLOR_ATTRIB_NAME);
    }

    public int getNormalAttribLoc() { return normalAttribLoc;}
    public int getColorAttribLoc() { return colorAttribLoc;}
    public int getPositionAttribLoc() { return posLoc;}
    public int getTexCoordAttribLoc() { return texLoc;}

    public void setModelViewProj(Matrix4f mat) { if(m_g_ModelViewProjLoc >=0)gl.glUniformMatrix4fv(m_g_ModelViewProjLoc,false, CacheBuffer.wrap(mat));}
    public void setLightSpecular(Vector3f v) { if(m_g_LightSpecularLoc >=0)gl.glUniform3f(m_g_LightSpecularLoc, v.x, v.y, v.z);}
    public void setEyePos(Vector3f v) { if(m_g_EyePosLoc >=0)gl.glUniform3f(m_g_EyePosLoc, v.x, v.y, v.z);}
    public void setLightAmbient(Vector3f v) { if(m_g_LightAmbientLoc >=0)gl.glUniform3f(m_g_LightAmbientLoc, v.x, v.y, v.z);}
    public void setColor(Vector4f v) { if(m_g_ColorLoc >=0)gl.glUniform4f(m_g_ColorLoc, v.x, v.y, v.z, v.w);}
    public void setMaterialSpecular(Vector4f v) { if(m_g_MaterialSpecularLoc >=0)gl.glUniform4f(m_g_MaterialSpecularLoc, v.x, v.y, v.z, v.w);}
    public void setLightPos(Vector4f v) { if(m_g_LightPosLoc >=0)gl.glUniform4f(m_g_LightPosLoc, v.x, v.y, v.z, v.w);}
    public void setLightDiffuse(Vector3f v) { if(m_g_LightDiffuseLoc >=0)gl.glUniform3f(m_g_LightDiffuseLoc, v.x, v.y, v.z);}
    public void setModel(Matrix4f mat) { if(m_g_ModelLoc >=0)gl.glUniformMatrix4fv(m_g_ModelLoc, false, CacheBuffer.wrap(mat));}
    public void setMaterialDiffuse(Vector3f v) { if(m_g_MaterialDiffuseLoc >=0)gl.glUniform3f(m_g_MaterialDiffuseLoc, v.x, v.y, v.z);}
    public void setMaterialAmbient(Vector3f v) { if(m_g_MaterialAmbientLoc >=0)gl.glUniform3f(m_g_MaterialAmbientLoc, v.x, v.y, v.z);}

    public void setLightParams(LightParams params){
        setModel(params.model);
        setModelViewProj(params.modelViewProj);

        setLightPos(params.lightPos);
        setLightAmbient(params.lightAmbient);
        setLightDiffuse(params.lightDiffuse);
        setLightSpecular(params.lightSpecular);

        setMaterialAmbient(params.materialAmbient);
        setMaterialDiffuse(params.materialDiffuse);
        setMaterialSpecular(params.materialSpecular);
        setEyePos(params.eyePos);
        setColor(params.color);
    }

    public static class LightParams{
        public final Matrix4f model = new Matrix4f();
        public final Matrix4f modelViewProj = new Matrix4f();

        public final Vector4f lightPos = new Vector4f();   // w==0, means light direction, must be normalized
        public final Vector3f lightAmbient = new Vector3f();   // Ia
        public final Vector3f lightDiffuse = new Vector3f();   // Il
        public final Vector3f lightSpecular = new Vector3f();  // rgb = Cs * Il, w for power

        public final Vector3f materialAmbient = new Vector3f();   // ka
        public final Vector3f materialDiffuse = new Vector3f();   // kb
        public final Vector4f materialSpecular = new Vector4f();   // ks
        public final Vector3f eyePos = new Vector3f();
        public final Vector4f color = new Vector4f();
    }
}
