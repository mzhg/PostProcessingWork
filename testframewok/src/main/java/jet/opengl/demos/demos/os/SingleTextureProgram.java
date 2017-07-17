package jet.opengl.demos.demos.os;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.AttribBinder;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/6/5.
 */
class SingleTextureProgram extends GLSLProgram{
    private int uniformMVP = -1;
    private int uniformColor = -1;

//    uniform bool g_RenderCircle;
//    uniform float g_CircleTime;  // ranged [0..1]
//    uniform vec3  g_CircleColor;
//    uniform float g_CirlceBorder;   //  default 0.2
//    uniform float g_CircleRadius;   // ranged (0, 1] defualt is 0.8
    private int renderCircle = -1;
    private int circleColor = -1;
    private int circleBorder = -1;
    private int circleRadius = -1;
    private int circleTime = -1;

//    uniform bool g_Hovered;
//    uniform vec4 g_HorverdColor;
    private int hovered;
    private int hoveredColor;

    private int renderFile = -1;

    SingleTextureProgram(){
        setAttribBinding(new AttribBinder("aPosition", 0), new AttribBinder("aTexCoord", 1));
        try {
            setSourceFromFiles("OS/shaders/simple_v_t2.glvs", "OS/shaders/simple_v_t2.glfs");
        } catch (IOException e) {
            e.printStackTrace();
        }

        uniformMVP = getUniformLocation("uMvp");
        uniformColor = getUniformLocation("g_MaskColor");

        renderCircle = getUniformLocation("g_RenderCircle");
        circleTime = getUniformLocation("g_CircleTime");
        circleColor = getUniformLocation("g_CircleColor");
        circleBorder = getUniformLocation("g_CirlceBorder");
        circleRadius = getUniformLocation("g_CircleRadius");

        hovered = getUniformLocation("g_Hovered");
        hoveredColor = getUniformLocation("g_HorverdColor");

        renderFile = getUniformLocation("g_RenderFilm");

        enable();
        setTextureUniform("uTexSampler", 0);
        setMaskColor(1,1,1,1);

        setRenderCircle(false);
        setCircleColor(1.0f,1.0f,1f);
        setCircleBorder(0.06f);
        setCircleRadius(0.9f);
        setCircleTime(0);

        setHovered(false);
        setHoveredColor(1,1,1, 0.5f);
        setRenderFilm(false);
    }

    void setMVP(Matrix4f mat){ gl.glUniformMatrix4fv(uniformMVP, false, CacheBuffer.wrap(mat));}
    void setMaskColor(float r, float g, float b, float a) { gl.glUniform4f(uniformColor, r, g, b,a);}
    void setRenderCircle(boolean flag)  { gl.glUniform1i(renderCircle, flag ?1:0);}
    void setRenderFilm(boolean flag)  { gl.glUniform1i(renderFile, flag ?1:0);}
    void setCircleColor(float red, float green, float blue) { gl.glUniform3f(circleColor, red, green, blue);}
    void setCircleBorder(float border) { gl.glUniform1f(circleBorder, border);}
    void setCircleRadius(float radius) { gl.glUniform1f(circleRadius, radius);}
    void setCircleTime(float time)     { gl.glUniform1f(circleTime, time);}
    void setHovered(boolean flag)      { gl.glUniform1i(hovered, flag?1:0);}
    void setHoveredColor(float r, float g, float b, float a) { gl.glUniform4f(hoveredColor, r, g, b,a);}
}
