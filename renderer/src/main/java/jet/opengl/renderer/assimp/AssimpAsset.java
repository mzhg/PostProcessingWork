package jet.opengl.renderer.assimp;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AIBone;
import org.lwjgl.assimp.AIColor3D;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AILight;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AINodeAnim;
import org.lwjgl.assimp.AIQuatKey;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.AIVectorKey;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Transform;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Pair;

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

    private final Map<String, AINode> mSceneNodes = new HashMap<>();

    private float mTimeline;
    private boolean mGenerateCurvature;
    private boolean mCombineMesh;
    private Matrix4f[] mKeyframes;
    private boolean [] mHandled;
    private final Transform mTempTransform = new Transform();

    private String mModelFilename;
    private GLFuncProvider gl;

    final VertexLocation mVertexLocation = new VertexLocation();

    // When true, the vertex data will be combined in one VAO that can't changed in runtime.
    private final boolean mStaticMesh;

    public AssimpAsset(){
        this(true);
    }

    public AssimpAsset(boolean staticMesh){
        mStaticMesh = staticMesh;
    }

    public void setVertexLocation(VertexLocation location){
        mVertexLocation.set(location);
    }

    public boolean isStaticMesh() { return mStaticMesh;}


    void load(String filename, BoundingBox boundingBox) throws IOException{
        load(filename, boundingBox, false);
    }

    void load(String filename, BoundingBox boundingBox, boolean generateCurvature) throws IOException{
        if(GLFuncProviderFactory.isInitlized())
            gl = GLFuncProviderFactory.getGLFuncProvider();

        mGenerateCurvature = generateCurvature;
        String resolvedPath = FileUtils.g_IntenalFileLoader.resolvePath(filename);

        AIScene scene = Assimp.aiImportFile(resolvedPath, Assimp.aiProcessPreset_TargetRealtime_Fast | Assimp.aiProcess_FlipUVs);
        if (scene == null) {
            throw new IllegalStateException(Assimp.aiGetErrorString());
        }

        mModelFilename = filename;

        flatNodes(scene);
        parseLights(scene);
        parseMaterials(scene);
        parseMesh(scene, boundingBox);
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

    static void assign(Matrix4f out, AIMatrix4x4 in){
        out.m00 = in.a1();
        out.m10 = in.a2();
        out.m20 = in.a3();
        out.m30 = in.a4();

        out.m01 = in.b1();
        out.m11 = in.b2();
        out.m21 = in.b3();
        out.m31 = in.b4();

        out.m02 = in.c1();
        out.m12 = in.c2();
        out.m22 = in.c3();
        out.m32 = in.c4();

        out.m03 = in.d1();
        out.m13 = in.d2();
        out.m23 = in.d3();
        out.m33 = in.d4();
    }

    private void flatNodes(AINode root){
        final String name = root.mName().dataString();
        if(mSceneNodes.containsKey(name)){
            throw new RuntimeException("Dumplicate node in a same scene, the name is " + name);
        }
        mSceneNodes.put(name, root);

        final int numChildren = root.mNumChildren();
        PointerBuffer childrenBuffer = root.mChildren();

        for(int childIndex = 0; childIndex < numChildren; childIndex++){
            AINode child = AINode.create(childrenBuffer.get(childIndex));

            flatNodes(child);
        }
    }

    void flatNodes(AIScene scene){
        mSceneNodes.clear();
        AINode root = scene.mRootNode();
        flatNodes(root);
    }

    AINode findNode(String name){ return mSceneNodes.get(name);}

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
            AIColor4D outColor = AIColor4D.calloc();
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

                        if(gl != null){
                            gl.glBindTexture(destMaterial.mTextures[textureID].getTarget(), destMaterial.mTextures[textureID].getTexture());
                            SamplerUtils.applyTexture2DSampler(destMaterial.mTextures[textureID].getTarget(), GLenum.GL_LINEAR_MIPMAP_LINEAR, GLenum.GL_LINEAR, GLenum.GL_REPEAT, GLenum.GL_REPEAT);
                        }
                    }
                }
            }

            texturePath.free();
            outColor.free();
        }
    }

    void parseMesh(AIScene scene, BoundingBox boundingBox){
        Map<String, Pair<AIBone, Integer>> parsedBones = new HashMap<>();
        List<AssimpBone> bones = new ArrayList<>();

        final int numMesh = scene.mNumMeshes();
        final PointerBuffer meshBuffer = scene.mMeshes();
        for(int meshIndex = 0; meshIndex < numMesh;meshIndex++){
            AIMesh mesh = AIMesh.create(meshBuffer.get(meshIndex));

            final int numBones = mesh.mNumBones();
            final PointerBuffer boneBuffer = mesh.mBones();
            for(int boneIndex = 0; boneIndex < numBones; boneIndex++){
                AIBone bone = AIBone.create(boneBuffer.get(boneIndex));

                final String boneName = bone.mName().dataString();
                Pair<AIBone, Integer> it = parsedBones.get(boneName);
                if(it == null){
                    final int name = parsedBones.size();
                    AssimpBone rf_bone = new AssimpBone(name);
                    bones.add(rf_bone);

                    assign(rf_bone.mOffset, bone.mOffsetMatrix());

                    parsedBones.put(boneName, new Pair<>(bone, name));
                    mBoneMaps.put(boneName, rf_bone);

                    AINode nodeTmp = findNode(boneName);
                    while (nodeTmp != null){
                        final String nodeName = nodeTmp.mName().dataString();
                        if(nodeName.equals(boneName)){
                            break;
                        }

                        Pair<AIBone, Integer> parentIt = parsedBones.get(nodeName);
                        if(parentIt == null){
                            int name2 = parsedBones.size();

                            AssimpBone rf_bone2 = new AssimpBone(name2);
                            bones.add(rf_bone2);

                            parsedBones.put(nodeName, new Pair<>(bone, name2));
                            mBoneMaps.put(nodeName, rf_bone2);
                            assign(rf_bone2.mOffset, bone.mOffsetMatrix());
                        }

                        nodeTmp = nodeTmp.mParent();
                    }
                }else{
                    // nothing need to do here.
                }
            }
        }

        for(Map.Entry<String, Pair<AIBone, Integer>> it : parsedBones.entrySet()){
            // find the correspond aiNode
            AINode node = findNode(it.getKey());

            AssimpBone bone = bones.get(it.getValue().second);
            assign(bone.mTransform, node.mTransformation());

            // find it's parent
            AINode parent = node.mParent();
            if(parent != null){
                Pair<AIBone, Integer> parentIt = parsedBones.get(parent.mName().dataString());
                if(parent == null){
                    bone.mParent = -1;
                }else{
                    bone.mParent = parentIt.second;
                }
            }

            // find it's children
            final int numChildren = node.mNumChildren();
            final PointerBuffer childrenBuffer = node.mChildren();
            for(int childIndex = 0; childIndex < numChildren; childIndex++){
                AINode child = AINode.create(childrenBuffer.get(childIndex));

                Pair<AIBone, Integer> childIt = parsedBones.get(child.mName().dataString());
                if(child == null)
                    continue;

                bone.mChildren.push(childIt.second);
            }
        }

        if(!bones.isEmpty()){
            mBones = new AssimpBone[bones.size()];
            bones.toArray(mBones);
        }

        // Parses the animations.
        final int numAnimations = scene.mNumAnimations();
        final PointerBuffer animationBuffers = scene.mAnimations();
        if(numAnimations > 0) {
            mAnimations = new AssimpAnimation[numAnimations];
            mKeyframes = new Matrix4f[numAnimations];
            mHandled = new boolean[numAnimations];

            for(int i = 0; i < numAnimations; i++)
                mKeyframes[i] = new Matrix4f();
        }

        for(int animationIndex =0; animationIndex < numAnimations; animationIndex++){
            AIAnimation anim = AIAnimation.create(animationBuffers.get(animationIndex));

            final int numChannels = anim.mNumChannels();
            final PointerBuffer channalBuffers = anim.mChannels();
            for(int channelIndex = 0; channelIndex < numChannels; channelIndex ++){
                AINodeAnim nodeAnim = AINodeAnim.create(channalBuffers.get(channelIndex));

                final String nodeAnimName = nodeAnim.mNodeName().dataString();
                Pair<AIBone, Integer> node_it = parsedBones.get(nodeAnimName);
                if(node_it == null)
                    continue;

                AssimpAnimation rf_anim = new AssimpAnimation();
                rf_anim.mBoneName = node_it.second;
                rf_anim.mDuration = (float) anim.mDuration();
                rf_anim.mTicksPerSecond = (float)anim.mTicksPerSecond();

                // static checking
                if(AIVectorKey.SIZEOF != 8 + Vector3f.SIZE){
                    throw new UnsupportedOperationException();
                }

                { // Parsing the position keys.
                    final int numPositionKeys = nodeAnim.mNumPositionKeys();
                    final AIVectorKey.Buffer positionBuffers = nodeAnim.mPositionKeys();
                    final ByteBuffer positionMem = MemoryUtil.memByteBuffer(positionBuffers.address(), AIVectorKey.SIZEOF * positionBuffers.remaining());

                    if(numPositionKeys > 0)
                        rf_anim.mPositionKeys = new VectorKey[numPositionKeys];
                    for(int positionIndex = 0; positionIndex < numPositionKeys; positionIndex ++){
                        VectorKey posKey = new VectorKey();
                        posKey.mTime = (float) positionMem.getDouble();
                        posKey.mValue.x = positionMem.getFloat();
                        posKey.mValue.y = positionMem.getFloat();
                        posKey.mValue.z = positionMem.getFloat();

                        rf_anim.mPositionKeys[positionIndex] = posKey;
                    }
                }

                { // Parsing the scalling keys.
                    final int numScalingKeys = nodeAnim.mNumScalingKeys();
                    final AIVectorKey.Buffer scalingBuffer = nodeAnim.mScalingKeys();
                    final ByteBuffer positionMem = MemoryUtil.memByteBuffer(scalingBuffer.address(), AIVectorKey.SIZEOF * scalingBuffer.remaining());

                    if(numScalingKeys > 0)
                        rf_anim.mScalingKeys = new VectorKey[numScalingKeys];
                    for(int positionIndex = 0; positionIndex < numScalingKeys; positionIndex ++){
                        VectorKey posKey = new VectorKey();
                        posKey.mTime = (float) positionMem.getDouble();
                        posKey.mValue.x = positionMem.getFloat();
                        posKey.mValue.y = positionMem.getFloat();
                        posKey.mValue.z = positionMem.getFloat();

                        rf_anim.mScalingKeys[positionIndex] = posKey;
                    }
                }

                { // Parsing the rotation keys.
                    final int numRotationKeys = nodeAnim.mNumRotationKeys();
                    final AIQuatKey.Buffer scalingBuffer = nodeAnim.mRotationKeys();
                    final ByteBuffer positionMem = MemoryUtil.memByteBuffer(scalingBuffer.address(), AIQuatKey.SIZEOF * scalingBuffer.remaining());

                    if(numRotationKeys > 0)
                        rf_anim.mRotationKeys = new QuatKey[numRotationKeys];
                    for(int positionIndex = 0; positionIndex < numRotationKeys; positionIndex ++){
                        QuatKey posKey = new QuatKey();
                        posKey.mTime = (float) positionMem.getDouble();
                        posKey.mValue.w = positionMem.getFloat();
                        posKey.mValue.x = positionMem.getFloat();
                        posKey.mValue.y = positionMem.getFloat();
                        posKey.mValue.z = positionMem.getFloat();

                        rf_anim.mRotationKeys[positionIndex] = posKey;
                    }
                }

                mAnimations[animationIndex]= rf_anim;
            }
        }

        // extract all of the meshes from scenes.
        final int numMeshes = scene.mNumMeshes();
        AIMesh[] meshes = null;
        boolean haveBlendShape = false;
        if(numMeshes > 0 ){
            meshes = new AIMesh[numMesh];
            PointerBuffer meshBuffers = scene.mMeshes();
            for(int meshIndex = 0; meshIndex < numMesh; meshIndex++){
                meshes[meshIndex] = AIMesh.create(meshBuffers.get(meshIndex));
//                haveBlendShape |= (meshes[meshIndex].m); todo this version doesn't support the blendshape.
            }
        }else{ // No mesh in the scene
            return;
        }

        // Merge the meshes which have the same materials.
        if(mCombineMesh && bones.isEmpty() && !haveBlendShape){
            Map<Integer, List<AIMesh>> muti_meshes = new HashMap<>();
            for(int meshIndex = 0; meshIndex < numMesh; meshIndex++){
                AIMesh mesh = meshes[meshIndex];
                final int materialIndex = mesh.mMaterialIndex();
                List<AIMesh> subMeshes = muti_meshes.get(materialIndex);
                if(subMeshes == null){
                    subMeshes = new ArrayList<>();

                    muti_meshes.put(materialIndex, subMeshes);
                }

                subMeshes.add(mesh);
            }

            int meshIndex = 0;
            mMeshes = new AssimpMesh[muti_meshes.size()];

            for(Integer materialIndex : muti_meshes.keySet()){
                List<AIMesh> subMeshes = muti_meshes.get(materialIndex);
                mMeshes[meshIndex] = new AssimpMesh(this);
                mMeshes[meshIndex].mMaterilIndex = materialIndex;
                mMeshes[meshIndex].createMesh(subMeshes, parsedBones, boundingBox, mGenerateCurvature);
            }
        }else{
            mMeshes = new AssimpMesh[numMeshes];

            // Compute the combined transform of the meshes.
            parseMeshTransform(scene.mRootNode());

            for(int meshIndex = 0; meshIndex < numMesh; meshIndex++){
                AIMesh mesh = meshes[meshIndex];
                mMeshes[meshIndex].mMaterilIndex = mesh.mMaterialIndex();

                // find the bones
                final int numBones = mesh.mNumBones();
                final PointerBuffer boneBuffers = mesh.mBones();
                for(int boneIndex = 0; boneIndex < numBones; boneIndex++){
                    AIBone bone = AIBone.create(boneBuffers.get(boneIndex));
                    Pair<AIBone, Integer> it = parsedBones.get(bone.mName().dataString());
                    mMeshes[meshIndex].mBones.push(it.second);
                }

                mMeshes[meshIndex].createMesh(mesh, parsedBones, boundingBox, mGenerateCurvature);
            }
        }

        if(!bones.isEmpty()){
            mBoneTrasnforms = new Matrix4f[bones.size()];
            mBoneOffsets = new Matrix4f[bones.size()];
            for (int i = 0; i < mBoneTrasnforms.length; i++){
                mBoneTrasnforms[i] = new Matrix4f();
                mBoneOffsets[i] = new Matrix4f();
            }
        }
    }

    private void parseMeshTransform(AINode node){
        if(node == null) return;

        final int numMeshes = node.mNumMeshes();
        final IntBuffer meshBuffers = node.mMeshes();

        final Matrix4f tmp = new Matrix4f();
        for(int meshIndex = 0; meshIndex < numMeshes; meshIndex++){
            int meshIdx = meshBuffers.get(meshIndex);
            if(mMeshes[meshIdx] == null){
                mMeshes[meshIdx] = new AssimpMesh(this);
            }

            AssimpMesh mesh = mMeshes[meshIdx];
            mesh.mName = node.mName().dataString();

            Matrix4f finalMat = mesh.mNodeTransform;
            assign(finalMat, node.mTransformation());

            AINode parent = node.mParent();
            while (parent != null){
                assign(tmp, parent.mTransformation());

                Matrix4f.mul(tmp, finalMat, finalMat);
                parent = parent.mParent();
            }
        }

        // Handle the children.
        final int numChildren = node.mNumChildren();
        PointerBuffer childrenBuffer = node.mChildren();

        for(int childIndex = 0; childIndex < numChildren; childIndex++){
            AINode child = AINode.create(childrenBuffer.get(childIndex));

            parseMeshTransform(child);
        }
    }

    private void recurseUpdateBone(int name, Matrix4f[] keyframes, boolean[] handled){
        if(handled[name] == false){
            AssimpBone bone = mBones[name];
            mBoneTrasnforms[name].load(keyframes[name]);
            if(bone.mParent >=0){
                recurseUpdateBone(bone.mParent, keyframes, handled);
                Matrix4f.mul(mBoneTrasnforms[bone.mParent], mBoneTrasnforms[name], mBoneTrasnforms[name]);
            }

            Matrix4f.mul(mBoneTrasnforms[name], bone.mOffset, mBoneOffsets[name]);
            handled[name] = true;
        }
    }

//    private void setBlendShape(int meshid, int bid, )
    /** Set the specular reflectance for the given material by the materialIndex, if materialIndex == -1, set the values to all materials. */
    public void setSpecular(ReadableVector3f f0, float roughness, int materialIndex){
        if(materialIndex>=0){
            mMaterials[materialIndex].mSpecularColor.set(f0, 1);
            mMaterials[materialIndex].mRoughness = roughness;
        }else{
            for(int i = 0; i < CommonUtil.length(mMaterials); i++){
                mMaterials[i].mSpecularColor.set(f0, 1);
                mMaterials[i].mRoughness = roughness;
            }
        }
    }

    public AssimpMaterial getMaterial(int index){
        return mMaterials[index];
    }

    /** Specify the materils. */
    public void setMaterials(AssimpMaterial[] materials, boolean releaseBefore){
        if(releaseBefore){
            for(int i = 0; i < CommonUtil.length(mMaterials); i++){
                if(mMaterials[i] != null){
                    mMaterials[i].dispose();
                }
            }
        }

        mMaterials = materials;
    }

    private void updateBlendShape(){}

    // Update the animations if contained.
    public void update(float deltaTime){
        if(mHasBlendShape)
            updateBlendShape();

        final int numAnimations = CommonUtil.length(mAnimations);
        if(numAnimations == 0)
            return;


        for(int i =0; i < numAnimations; i++){
            mKeyframes[i].setIdentity();
            mHandled[i] = false;
        }

        mTimeline += deltaTime;
        for(int i = 0; i < numAnimations; i++){
            mAnimations[i].interpolate(mTimeline, mTempTransform);
            mTempTransform.getMatrix(mKeyframes[mAnimations[i].mBoneName]);
        }

        //Resolve the bone matrix
        final int numBones = CommonUtil.length(mBones);
        for(int i =0; i < numBones; i++){
            recurseUpdateBone(i, mKeyframes, mHandled);
        }
    }

    public void apply(int meshIndex){
        mMeshes[meshIndex].apply();
    }

    public int getMaterialCount(){
        return CommonUtil.length(mMaterials);
    }

    public int getMeshCount(){
        return CommonUtil.length(mMeshes);
    }

    @Override
    public void dispose() {

    }
}
