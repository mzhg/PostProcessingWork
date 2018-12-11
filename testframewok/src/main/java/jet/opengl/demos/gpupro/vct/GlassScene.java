package jet.opengl.demos.gpupro.vct;

import com.nvidia.developer.opengl.app.NvInputTransformer;

import org.lwjgl.util.vector.Quaternion;

final class GlassScene extends Scene{

    private float m_elpsedTime;
    private int m_lightIndex;
    private int m_buddhaIndex;

    GlassScene(NvInputTransformer transformer) {
        super(transformer);

        Mesh[] corners = buildConrer();
        Mesh lightCube = createSphere();
        Mesh buddha = loadObj("gpupro\\VoxelConeTracing\\Models\\teapot.obj");
        Mesh backWall = loadObj("gpupro\\VoxelConeTracing\\Models\\quadn.obj");

        MeshRenderer cornerBox0 = new MeshRenderer(corners[0], MaterialSetting.Green());
        MeshRenderer cornerBox1 = new MeshRenderer(corners[1], MaterialSetting.White());
        MeshRenderer cornerBox2 = new MeshRenderer(corners[2], MaterialSetting.Red());
        MeshRenderer cornerBox3 = new MeshRenderer(corners[3], MaterialSetting.Cyan());
        MeshRenderer cornerBox4 = new MeshRenderer(corners[4], MaterialSetting.White());
        MeshRenderer cornerBox5 = new MeshRenderer(corners[5], MaterialSetting.White());

        MeshRenderer lightMesh = new MeshRenderer(lightCube,MaterialSetting.Emissive());

        MeshRenderer buddhaRenderer = new MeshRenderer(buddha, MaterialSetting.White());
        buddhaRenderer.transform.setScale(1.8f, 1.8f, 1.8f);
        Quaternion quat = new Quaternion();
        quat.setFromAxisAngle(0,1, 0, 2.4f);
        buddhaRenderer.transform.setRotation(quat.x, quat.y, quat.z, quat.w);
        buddhaRenderer.transform.setPosition(0, -0.13f, 0.05f);// glm::vec3(0, 0.0, 0);
//        buddhaRenderer.transform.updateTransformMatrix();
        buddhaRenderer.name = "Buddha";
        MaterialSetting buddhaMaterialSetting = buddhaRenderer.materialSetting;
        buddhaMaterialSetting.specularColor.set(0.99f, 0.61f, 0.43f);
        buddhaMaterialSetting.diffuseColor.set(buddhaMaterialSetting.specularColor);
        buddhaMaterialSetting.emissivity = 0.00f;
        buddhaMaterialSetting.transparency = 1.00f;
        buddhaMaterialSetting.refractiveIndex = 1.21f;
        buddhaMaterialSetting.specularReflectivity = 1.00f;
        buddhaMaterialSetting.diffuseReflectivity = 0.0f;
        buddhaMaterialSetting.specularDiffusion = 1.9f;

        // Light cube.
        lightMesh.materialSetting.diffuseColor.x = 1.0f;
        lightMesh.materialSetting.diffuseColor.y = 1.0f;
        lightMesh.materialSetting.diffuseColor.z = 1.0f;
        lightMesh.materialSetting.emissivity = 8.0f;
        lightMesh.materialSetting.specularReflectivity = 0.0f;
        lightMesh.materialSetting.diffuseReflectivity = 0.0f;

        MeshRenderer bwr = new MeshRenderer(backWall, MaterialSetting.White());
        bwr.transform.setScale(2, 2, 2);
        bwr.transform.setPosition(0, 0, 0.99f);
        quat.setFromAxisAngle(1,0,0, -1.57079632679f);
        bwr.transform.setRotation(quat.x, quat.y, quat.z, quat.w);

        renderers.add(cornerBox0);
        renderers.add(cornerBox1);
        renderers.add(cornerBox2);
        renderers.add(cornerBox3);
        renderers.add(cornerBox4);
        renderers.add(cornerBox5);
        renderers.add(lightMesh);       m_lightIndex = renderers.size() - 1;
        renderers.add(buddhaRenderer);  m_buddhaIndex = renderers.size() - 1;
        renderers.add(bwr);

        // Lighting.
        PointLight p = new PointLight();
        p.color.set(0.63f, 0.47f, 0.51f);
        p.position.set(0, 0, 0.925f);
        pointLights.add(p);
    }

    @Override
    void onActive() {

    }

    @Override
    void update(float dt) {
//        glm::vec3 r = glm::vec3(sinf(float(Time::time * 0.67)), sinf(float(Time::time * 0.78)), cosf(float(Time::time * 0.67)));
        float x = (float)Math.sin(m_elpsedTime * 0.67);
        float y = (float)Math.sin(m_elpsedTime * 0.78);
        float z = (float)Math.cos(m_elpsedTime * 0.67);

        MeshRenderer lightMesh =renderers.get(m_lightIndex);
//        lightMesh.transform.position = 0.45f * r + 0.20f * r * glm::vec3(1, 0, 1);
        lightMesh.transform.setPosition(0.45f * x + 0.20f * x, 0.45f*y, 0.45f*z+0.20f*z);
        lightMesh.transform.setScale(0.049f, 0.049f, 0.049f);// = glm::vec3(0.049f);
//        renderers[lightCubeIndex]->transform.updateTransformMatrix();

        pointLights.get(0).position.set(0.45f * x + 0.20f * x, 0.45f*y, 0.45f*z+0.20f*z);
        lightMesh.materialSetting.diffuseColor.set(pointLights.get(0).color);

        Quaternion quat = new Quaternion();
        quat.setFromAxisAngle(0,1, 0, m_elpsedTime);
        renderers.get(m_buddhaIndex).transform.setRotation(quat.x, quat.y, quat.z, quat.w);

        transformer.getModelViewMat(view);

        m_elpsedTime += dt;
    }
}
