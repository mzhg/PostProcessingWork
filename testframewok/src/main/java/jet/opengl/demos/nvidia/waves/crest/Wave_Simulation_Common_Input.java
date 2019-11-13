package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Transform;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

class Wave_Simulation_Common_Input implements Wave_LodData_Input{

    private Technique _material;
    private Wave_Mesh  _mesh;
    private final Transform transform = new Transform();

    private float _waveLenght;
    private boolean _enabled = true;
    public void setWaveLegnth(float waveLenght){
        _waveLenght = waveLenght;
    }

    public void setEnabled(boolean enabled){
        _enabled = enabled;
    }

    Wave_Simulation_Common_Input(Technique material, Wave_Mesh mesh){
         _material = material;
        _mesh = mesh;
     }

     public Transform getTransform() { return transform;}

     protected void update(){}

     @Override
     public void draw(float weight, boolean isTransition, Wave_Simulation_ShaderData shaderData) {
         update();

         if (weight > 0f)
         {
             shaderData._Weight = weight;
             transform.getMatrix(shaderData.unity_ObjectToWorld);
             _material.enable(shaderData);

             GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
             gl.glEnable(GLenum.GL_BLEND);
             gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
             gl.glBlendEquation(GLenum.GL_FUNC_ADD);
             if(_mesh != null)
                 _mesh.Draw();
             else{
                 gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
             }

             _material.printOnce();
         }
     }

    @Override
    public float wavelength() {
        return _waveLenght;
    }

    @Override
    public boolean enabled() {
        return _enabled;
    }
}
