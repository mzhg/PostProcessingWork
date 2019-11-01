package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Transform;

import jet.opengl.demos.nvidia.waves.ocean.Technique;

class Wave_Simulation_Common_Input implements Wave_LodData_Input{

    private Technique _material;
    private Wave_Mesh  _mesh;
    private final Transform transform = new Transform();

    Wave_Simulation_Common_Input(Technique material, Wave_Mesh mesh){
         _material = material;
        _mesh = mesh;
     }

     protected void update(){}

     @Override
     public void draw(float weight, boolean isTransition, Wave_Simulation_ShaderData shaderData) {
         update();

         if (weight > 0f)
         {
//             _materials[isTransition].SetFloat(sp_Weight, weight);
             shaderData._Weight = weight;
             // TODO don't forget the transform
             _material.enable(shaderData);
//             buf.DrawRenderer(_renderer, _materials[isTransition]);
            _mesh.Draw();
         }
     }

    @Override
    public float wavelength() {
        return 0;
    }

    @Override
    public boolean enabled() {
        return true;
    }
}
