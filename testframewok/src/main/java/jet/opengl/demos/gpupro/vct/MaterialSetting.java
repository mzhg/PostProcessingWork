package jet.opengl.demos.gpupro.vct;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.shader.GLSLProgram;

/** Represents a setting for a material that can be used along with voxel cone tracing GI. */
final class MaterialSetting {
    
    static final String diffuseColorName = "material.diffuseColor";
	static final String specularColorName = "material.specularColor";
	static final String emissivityName = "material.emissivity";
	static final String transparencyName = "material.transparency";
	static final String refractiveIndexName = "material.refractiveIndex";
	static final String specularReflectanceName = "material.specularReflectivity";
	static final String diffuseReflectanceName = "material.diffuseReflectivity";
	static final String specularDiffusionName = "material.specularDiffusion";
    
    final Vector3f diffuseColor = new Vector3f();
    final Vector3f specularColor = new Vector3f(1, 1, 1);
    float specularReflectivity, diffuseReflectivity, emissivity, specularDiffusion = 2.0f;
    float transparency = 0.0f, refractiveIndex = 1.4f;

    void Upload(GLSLProgram pro, boolean useProgram /*= true*/) {
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        int program = pro.getProgram();
        if (useProgram) gl.glUseProgram(program);

        // Vec3s.
        int index;
        index = gl.glGetUniformLocation(program, diffuseColorName);
        if(index >= 0) gl.glUniform3f(index, diffuseColor.x, diffuseColor.y, diffuseColor.z);

        index = gl.glGetUniformLocation(program, specularColorName);
        if(index >= 0) gl.glUniform3f(index, specularColor.x, specularColor.y, specularColor.z);

        // Floats.
        index = gl.glGetUniformLocation(program, emissivityName);
        if(index >= 0) gl.glUniform1f(index, emissivity);
        index = gl.glGetUniformLocation(program, specularReflectanceName);
        if(index >= 0) gl.glUniform1f(index, specularReflectivity);
        index = gl.glGetUniformLocation(program, diffuseReflectanceName);
        if(index >= 0) gl.glUniform1f(index, diffuseReflectivity);
        index = gl.glGetUniformLocation(program, specularDiffusionName);
        if(index >= 0) gl.glUniform1f(index, specularDiffusion);
        index = gl.glGetUniformLocation(program, transparencyName);
        if(index >= 0) gl.glUniform1f(index, transparency);
        index = gl.glGetUniformLocation(program, refractiveIndexName);
        if(index >= 0) gl.glUniform1f(index, refractiveIndex);
    }

    boolean IsEmissive() { return emissivity > 0.00001f; }

    // Basic constructor.
    MaterialSetting(Vector3f _diffuseColor){
        this(_diffuseColor, 0, 0, 1);
    }

    MaterialSetting(
            Vector3f _diffuseColor /*= glm::vec3(1)*/,
            float _emissivity /*= 0.0f*/,
            float _specularReflectivity /*= 0.0f*/,
            float _diffuseReflectivity /*= 1.0f*/
	) {
        diffuseColor.set(_diffuseColor);
        emissivity = _emissivity;
        specularReflectivity = _specularReflectivity;
        diffuseReflectivity = _diffuseReflectivity;
    }


    static MaterialSetting Default() {
        return new MaterialSetting(new Vector3f(1,1,1));
    }

    static MaterialSetting White() {
        return new MaterialSetting(
                new Vector3f(0.97f, 0.97f, 0.97f)
		);
    }

    static MaterialSetting Cyan() {
        return new MaterialSetting(
                new Vector3f(0.30f, 0.95f, 0.93f)
		);
    }

    static MaterialSetting Purple() {
        return new MaterialSetting(
                new Vector3f(0.97f, 0.05f, 0.93f)
		);
    }

    static MaterialSetting Red() {
        return new MaterialSetting(
                new Vector3f(1.0f, 0.26f, 0.27f)
		);
    }

    static MaterialSetting Green() {
        return new MaterialSetting(
                new Vector3f(0.27f, 1.0f, 0.26f)
		);
    }

    static MaterialSetting Blue() {
        return new MaterialSetting(
                new Vector3f(0.35f, 0.38f, 1.0f)
		);
    }

    static MaterialSetting Emissive() {
        return new MaterialSetting(
                new Vector3f(0.85f, 0.9f, 1.0f),
        1.0f, 0, 1
		);
    }
}
