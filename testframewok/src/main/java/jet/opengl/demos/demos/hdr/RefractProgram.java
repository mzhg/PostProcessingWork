package jet.opengl.demos.demos.hdr;

import java.io.IOException;

/**
 * Created by mazhen'gui on 2017/3/16.
 */

final class RefractProgram extends  BaseProgram{
    private int u_emission;
    private int u_color;

    private int attribPos;
    private int attribNormal;

    public RefractProgram() throws IOException {
//        CharSequence vert = .readText("hdr_shaders/matteObject.vert");
//        CharSequence frag = NvAssetLoader.readText("hdr_shaders/refractObject.frag");
//
//        NvGLSLProgram program = new NvGLSLProgram();
//        program.setSourceFromStrings(vert, frag, true);
//        programID = program.getProgram();

        setSourceFromFiles("HDR/shaders/matteObject.vert", "HDR/shaders/refractObject.frag");
        initVS();

        u_emission = getUniformLocation("emission");
        u_color    = getUniformLocation("color");

        int envMap      = getUniformLocation("envMap");

        attribPos = getAttribLocation("PosAttribute");
        attribNormal = getAttribLocation("myNormal");

        enable();
        gl.glUniform1i(envMap, 0);
    }

    public void applyEmission(float r, float g, float b){	gl.glUniform3f(u_emission, r, g, b);}
    public void applyColor(float r, float g, float b, float a){	gl.glUniform4f(u_color, r, g, b, a);}

    @Override
    public int getAttribPosition() { return attribPos; }
    @Override
    public int getAttribNormal() {return attribNormal;}
}
