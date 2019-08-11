package jet.opengl.renderer.assimp;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor3D;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AILight;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.util.Map;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

public class AssimpAsset implements Disposeable {

    private Map<String, AssimpBone> mBoneMaps;
    private AssimpMaterial[] mMaterials;
    private Texture2D[] mEmbeddedTextures;
    private AssimpMesh[] mMeshes;
    private boolean mHasBlendShape;

    private AssimpBone[] mBones;
    private Matrix4f[] mBoneTrasnforms;
    private Matrix4f[] mBoneOffsets;

    private AssimpAnimation[] mAnimations;

    private SceneLight[] mSceneLights;

    private float mTimeline;
    private boolean mGenerateCurvature;

    private String mModelFilename;

    void load(String filename, BoundingBox boundingBox) throws IOException{
        load(filename, boundingBox, false);
    }

    void load(String filename, BoundingBox boundingBox, boolean generateCurvature) throws IOException{
        mGenerateCurvature = generateCurvature;
        String resolvedPath = FileUtils.g_IntenalFileLoader.resolvePath(filename);

        AIScene scene = Assimp.aiImportFile(resolvedPath, Assimp.aiProcessPreset_TargetRealtime_Fast | Assimp.aiProcess_FlipUVs);
        if (scene == null) {
            throw new IllegalStateException(Assimp.aiGetErrorString());
        }

        mModelFilename = filename;

        parseLights(scene);
        parseMaterials(scene);
        parseMesh(scene);
    }

    static void assign(Vector3f out, AIVector3D in){
        out.x = in.x();
        out.y = in.y();
        out.z = in.z();
    }

    static void assign(Vector3f out, AIColor3D in){
        out.x = in.r();
        out.y = in.g();
        out.z = in.b();
    }

    static void assign(Vector4f out, AIColor4D in){
        out.x = in.r();
        out.y = in.g();
        out.z = in.b();
        out.w = in.a();
    }

    void parseLights(AIScene scene){
        if(scene.mNumLights() > 0){
            mSceneLights = new SceneLight[scene.mNumLights()];
        }else{
            return;
        }

        PointerBuffer lighgts = scene.mLights();
        for(int lightIndex = 0; lightIndex < mSceneLights.length; lightIndex++){
            SceneLight sceneLight = mSceneLights[lightIndex] = new SceneLight();

            AILight light = AILight.create(lighgts.get(lightIndex));

            assign(sceneLight.mDirection, light.mDirection());
            assign(sceneLight.mPosition, light.mPosition());
            assign(sceneLight.mUp, light.mUp());
            assign(sceneLight.mDiffuseColor, light.mColorDiffuse());
            assign(sceneLight.mSpecularColor, light.mColorSpecular());
            assign(sceneLight.mAmbientColor, light.mColorAmbient());

            switch (light.mType()){
                case Assimp.aiLightSource_DIRECTIONAL:
                    sceneLight.mType = LightType.DIRECTIONAL;
                    break;
                case Assimp.aiLightSource_POINT:
                    sceneLight.mType = LightType.POINT;
                    sceneLight.mAttenuationQuadratic = light.mAttenuationQuadratic();
                    sceneLight.mAttenuationLinear = light.mAttenuationLinear();
                    sceneLight.mAttenuationConstant = light.mAttenuationConstant();
                    break;
                case Assimp.aiLightSource_SPOT:
                    sceneLight.mType = LightType.SPOT;
                    sceneLight.mAttenuationQuadratic = light.mAttenuationQuadratic();
                    sceneLight.mAttenuationLinear = light.mAttenuationLinear();
                    sceneLight.mAttenuationConstant = light.mAttenuationConstant();
                    sceneLight.mSpotAngle = light.mAngleOuterCone();
                    sceneLight.mSpotPenumbraAngle = light.mAngleInnerCone();
                    break;
                default:
                    throw new UnsupportedOperationException("Invald Light Type");
            }
        }
    }

    void parseMaterials(AIScene scene) throws IOException {

        // load the embedded textures.
        final int numTextures = scene.mNumTextures();
        if(numTextures > 0){
            PointerBuffer sceneBuffers = scene.mTextures();
            for(int textureIndex = 0; textureIndex < numTextures; textureIndex ++){
                AITexture texture = AITexture.create(sceneBuffers.get(textureIndex));

                // This is a compressed file texture.
                if(texture.mHeight() == 0){
                    String hintFormat = texture.achFormatHintString();
                    LogUtil.i(LogUtil.LogType.DEFAULT, "Found a compressd texture, its format is " + hintFormat);

                    if(hintFormat.equalsIgnoreCase("png") || hintFormat.equalsIgnoreCase("jpg") || hintFormat.equalsIgnoreCase("bmp")){
                        // TODO
                    }else{
                        // TODO
                    }

                    throw new UnsupportedOperationException();
                }else{
                    // TODO It's too hard to handle this case.
                    throw new UnsupportedOperationException();
                }
            }
        }

        // Load materials.
        final int[] AI_TEXTURE_TYPES = {
            Assimp.aiTextureType_DIFFUSE,
            Assimp.aiTextureType_SPECULAR,
            Assimp.aiTextureType_AMBIENT,
            Assimp.aiTextureType_EMISSIVE,
            Assimp.aiTextureType_HEIGHT,
            Assimp.aiTextureType_NORMALS,
            Assimp.aiTextureType_SHININESS,
            Assimp.aiTextureType_OPACITY,
            Assimp.aiTextureType_DISPLACEMENT,
            Assimp.aiTextureType_LIGHTMAP,
            Assimp.aiTextureType_REFLECTION,
        };

        final int numMaterials = scene.mNumMaterials();
        if(numMaterials > 0){
            mMaterials = new AssimpMaterial[numMaterials];

            // temporal variables.
            AIColor4D outColor = AIColor4D.create();
            float[] outFloat = new float[1];
            int[] max = {1};

            AIString texturePath = AIString.calloc();
            int[] mapping = max;
            int[] uvindex = new int[1];
            float[] blend = outFloat;
            int[] op = new int[1];
            int[] mapmode = new int[1];
            int[] flags = new int[1];

            PointerBuffer materialBuffers = scene.mMaterials();
            for(int materialIndex = 0; materialIndex < numMaterials; materialIndex++){
                AIMaterial material = AIMaterial.create(materialBuffers.get(materialIndex));
                AssimpMaterial destMaterial = mMaterials[materialIndex] = new AssimpMaterial();

                if(Assimp.aiGetMaterialColor(material, Assimp.AI_MATKEY_COLOR_AMBIENT,0,0, outColor) == Assimp.aiReturn_SUCCESS)
                    assign(destMaterial.mAmbientColor, outColor);

                if(Assimp.aiGetMaterialColor(material, Assimp.AI_MATKEY_COLOR_DIFFUSE,0,0, outColor) == Assimp.aiReturn_SUCCESS)
                    assign(destMaterial.mDiffuseColor, outColor);

                if(Assimp.aiGetMaterialColor(material, Assimp.AI_MATKEY_COLOR_SPECULAR,0,0, outColor) == Assimp.aiReturn_SUCCESS)
                    assign(destMaterial.mSpecularColor, outColor);

                if(Assimp.aiGetMaterialFloatArray(material, Assimp.AI_MATKEY_SHININESS,0,0, outFloat, max) == Assimp.aiReturn_SUCCESS)
                    destMaterial.mRoughness = (float) (Numeric.log2(outFloat[0])/Numeric.log2(8192));

                for(int textureID = 0; textureID < AI_TEXTURE_TYPES.length; textureID++){
                    int result = Assimp.aiGetMaterialTexture(material, AI_TEXTURE_TYPES[textureID],0, texturePath, mapping, uvindex, blend,op, mapmode, flags);
                    if(result == Assimp.aiReturn_SUCCESS){
                        String textureFilepath = FileUtils.getParent(mModelFilename) + "/" + texturePath.dataString();
                        destMaterial.mTextures[textureID] = AssimpMaterial.loadTexture2D(textureFilepath, true, true);
                        // TODO Applying  Sampler.
                    }
                }
            }

            texturePath.free();
            outColor.free();
        }
    }

    void parseMesh(AIScene scene){

    }

    @Override
    public void dispose() {

    }
}
