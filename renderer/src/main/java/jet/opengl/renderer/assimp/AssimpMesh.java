package jet.opengl.renderer.assimp;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIBone;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.AIVertexWeight;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Pair;
import jet.opengl.postprocessing.util.StackInt;

public class AssimpMesh {
    static final int ATT_POSITION = 0;
    static final int ATT_NORMAL = 1;
    static final int ATT_TEXCOORD = 2;
    static final int ATT_COLOR = 3;
    static final int ATT_TANGENT = 4;
    static final int ATT_BONT_WEIGHT0 = 5;
    static final int ATT_BONT_WEIGHT1 = 6;
    static final int ATT_BLEND_SHAPE = 7;
    static final int ATT_COUNT = 8;

    final StackInt mBones = new StackInt();
    int mMaterilIndex;
    private final VertexAttrib[] m_VertexAttribs = new VertexAttrib[AssimpMesh.ATT_COUNT];
    private BufferGL mIndexBuffer;
    private int m_VAO;

    private BufferGL mBlendShapeBuffer;
    private int mPrimitive;
    private int mIndiceFormat;
    private int mIndiceCount;
    private int mVertexCount;
    private int mWeightCount;

    private float mUVScale;  // for the skin rendering.
    final Matrix4f mNodeTransform = new Matrix4f();
    private AssimpAsset mAsset;
    private List<BlendShapeDeformer> mBlendShape;

    private int mNumIndicesConvertOriginal;  //
    private int[] mIndicesConvertOriginal;
    private Vector3f[] mVerticesPosData;  // The transformed vertex informations, only support the fbx files.
    private final Vector3f mMeshCenter = new Vector3f();  // for the BlendShape, only support the fbx files.

    private AssimpMesh mMeshParent;
    private List<AssimpMesh> mMeshChildren;


    private final BoundingBox mBoundingBox = new BoundingBox();

    private boolean mEnabled = true;
    String mName;

    public AssimpMesh(AssimpAsset asset){
        mAsset = asset;
    }

    public void createMesh(AIMesh mesh, Map<String, Pair<AIBone, Integer>> paresedBones, BoundingBox boundingBox, boolean generateCurvature){
        createMesh(Collections.singletonList(mesh), paresedBones, boundingBox, generateCurvature);
    }

    public void createMesh(List<AIMesh> meshes, Map<String, Pair<AIBone, Integer>> paresedBones, BoundingBox boundingBox, boolean generateCurvature){
        int positionFormat = GLenum.GL_RGB32F;
        int normalFormat = 0;
        int texcoordFormat = 0;
        int colorFormat = 0;
        int tangentFormat = 0;
        int primitiveType = 0;
        int vertexCount = 0;
        int indiceCount = 0;

        List<AIVector3D.Buffer> positionBuffers = new ArrayList<>();
        List<AIVector3D.Buffer> normalBuffers = new ArrayList<>();
        List<AIVector3D.Buffer> texcoordBuffers = new ArrayList<>();
        List<AIColor4D.Buffer> colorBuffers = new ArrayList<>();
        List<AIVector3D.Buffer> tangentBuffers = new ArrayList<>();
        List<int[]>  indiceBuffers = new ArrayList<>();
        int[] vertexWeights = null;
        int maxWeight = 0;

        for(int i = 0; i < meshes.size(); i++){
            AIMesh mesh = meshes.get(i);

            {  // Primitive type
                int _primitveType = mesh.mPrimitiveTypes();
                check(i>0, primitiveType, _primitveType);
                primitiveType = _primitveType;
            }

            {// positions.
                int _vertexCount = mesh.mNumVertices();
                checkNot(true, _vertexCount, 0);
                vertexCount += _vertexCount;
                positionBuffers.add(mesh.mVertices());
            }

            {// normals.
                AIVector3D.Buffer normals = mesh.mNormals();
                if(normals != null){
                    normalBuffers.add(normals);
                    check(i>0, normalFormat, GLenum.GL_RGB32F);
                    normalFormat = GLenum.GL_RGB32F;
                }
            }

            { // tangent
                AIVector3D.Buffer tangents = mesh.mTangents();
                if(tangents != null){
                    tangentBuffers.add(tangents);
                    check(i>0, tangentFormat, GLenum.GL_RGB32F);
                    tangentFormat = GLenum.GL_RGB32F;
                }
            }

            { // Color
                 AIColor4D.Buffer colors = mesh.mColors(0);
                 if(colors != null){
                     check(i>0, colorFormat, GLenum.GL_RGBA32F);
                     colorFormat = GLenum.GL_RGBA32F;
                     colorBuffers.add(colors);
                 }
            }

            { // Texcoord
                AIVector3D.Buffer texcoords = mesh.mTextureCoords(0);
                if(texcoords != null){
                    if(mesh.mNumUVComponents(0) == 2){
                        check(i>0, texcoordFormat, GLenum.GL_RG32F);
                        texcoordFormat = GLenum.GL_RG32F;
                    }else{  // 3
                        check(i>0, texcoordFormat, GLenum.GL_RGB32F);
                        texcoordFormat = GLenum.GL_RGB32F;
                    }

                    texcoordBuffers.add(texcoords);
                }
            }

            { // Indices.
                int faceCount = mesh.mNumFaces();
//                checkNot(i>0 && faceCount != 0, indiceCount, 0);

                AIFace.Buffer facesBuffer = mesh.mFaces();
                int vertexPerFace = facesBuffer.get(0).mNumIndices();

                int elementCount = faceCount * vertexPerFace;
                int[] elementArrayBufferData = new int[elementCount];
                int baseVertex = vertexCount - mesh.mNumVertices();

                for (int faceIndex = 0; faceIndex < faceCount; ++faceIndex) {
                    AIFace face = facesBuffer.get(faceIndex);
                    if (face.mNumIndices() != vertexPerFace) {
                        throw new IllegalStateException("AIFace.mNumIndices() = " + face.mNumIndices());
                    }
                    IntBuffer indices = face.mIndices();
                    for(int j = 0; j < vertexPerFace; j++){
                        elementArrayBufferData[faceIndex * vertexPerFace + j] = indices.get() + baseVertex;
                    }
                }

                indiceBuffers.add(elementArrayBufferData);
                indiceCount += elementCount;
            }

            // bones. It is must be only one mesh if it contains bone animations.
            if(i == 0 && mesh.mNumBones() > 0) {
                int numVertices = mesh.mNumVertices();
                vertexWeights = new int[numVertices];

                // measure the vertex weight count.
                final int numBones = mesh.mNumBones();
                PointerBuffer boneBuffers = mesh.mBones();
                for(int boneIndex = 0; boneIndex < numBones; boneIndex++){
                    AIBone bone = AIBone.create(boneBuffers.get(boneIndex));

                    final int numWeights = bone.mNumWeights();
                    AIVertexWeight.Buffer weight = bone.mWeights();
                    for(int weightIndex = 0; weightIndex < numWeights; weightIndex++){
                        int vertexID = weight.get(weightIndex).mVertexId();
                        vertexWeights[vertexID]++;
                        maxWeight = Math.max(vertexWeights[vertexID], maxWeight);
                    }
                }
            }
        }

        switch (primitiveType){
            case Assimp.aiPrimitiveType_POINT:
                mPrimitive = GLenum.GL_POINTS;
                break;
            case Assimp.aiPrimitiveType_LINE:
                mPrimitive = GLenum.GL_LINES;
                break;
            case Assimp.aiPrimitiveType_TRIANGLE:
                mPrimitive = GLenum.GL_TRIANGLES;
                break;
            default:
                throw new IllegalArgumentException("Unkown primitive type:" + primitiveType);
        }

        // Create buffers
        if(vertexCount == 0)
            return;

        // Create Position buffer.
        m_VertexAttribs[ATT_POSITION] = new VertexAttrib();
        m_VertexAttribs[ATT_POSITION].internalFormat = positionFormat;
        m_VertexAttribs[ATT_POSITION].mVertexData = createBuffer(GLenum.GL_ARRAY_BUFFER, positionBuffers, vertexCount * Vector3f.SIZE);

        // Create Normal buffer.
        if(normalFormat != 0){
            m_VertexAttribs[ATT_NORMAL] = new VertexAttrib();
            m_VertexAttribs[ATT_NORMAL].internalFormat = normalFormat;
            m_VertexAttribs[ATT_NORMAL].mVertexData = createBuffer(GLenum.GL_ARRAY_BUFFER, normalBuffers, vertexCount * Vector3f.SIZE);
        }

        // Create tangent buffer.
        if(tangentFormat != 0){
            m_VertexAttribs[ATT_TANGENT] = new VertexAttrib();
            m_VertexAttribs[ATT_TANGENT].internalFormat = tangentFormat;
            m_VertexAttribs[ATT_TANGENT].mVertexData = createBuffer(GLenum.GL_ARRAY_BUFFER, tangentBuffers, vertexCount * Vector3f.SIZE);
        }

        // Create tangent buffer.
        if(colorFormat != 0){
            m_VertexAttribs[ATT_COLOR] = new VertexAttrib();
            m_VertexAttribs[ATT_COLOR].internalFormat = tangentFormat;
            m_VertexAttribs[ATT_COLOR].mVertexData = createBufferC(GLenum.GL_ARRAY_BUFFER, colorBuffers, vertexCount * Vector4f.SIZE);
        }

        // Create tangent buffer.
        if(texcoordFormat != 0){
            m_VertexAttribs[ATT_TEXCOORD] = new VertexAttrib();
            m_VertexAttribs[ATT_TEXCOORD].internalFormat = texcoordFormat;

            if(texcoordFormat == GLenum.GL_RGB32F){
                m_VertexAttribs[ATT_TEXCOORD].mVertexData = createBuffer(GLenum.GL_ARRAY_BUFFER, tangentBuffers, vertexCount * Vector3f.SIZE);
            }else{
                BufferGL result = new BufferGL();
                final int size = vertexCount * Vector2f.SIZE;
                ByteBuffer mem = CacheBuffer.getCachedByteBuffer(size);

                for(AIVector3D.Buffer buffer : texcoordBuffers){
                    final int texcoordCount = buffer.remaining();
                    ByteBuffer texcoordsBuffer = MemoryUtil.memByteBuffer(buffer.address(), AIVector3D.SIZEOF * texcoordCount);

                    for(int i = 0; i < texcoordCount; i++){
                        float u = texcoordsBuffer.getFloat(i * AIVector3D.SIZEOF);
                        float v = texcoordsBuffer.getFloat(i * AIVector3D.SIZEOF + 4);

                        mem.putFloat(u).putFloat(v);
                    }
                }

                mem.flip();
                result.initlize(GLenum.GL_ARRAY_BUFFER, size, mem, GLenum.GL_STATIC_DRAW);
                m_VertexAttribs[ATT_TEXCOORD].mVertexData = result;
            }
        }

        // fill the wieght buffers.
        if(maxWeight > 0){
            BufferGL[] results = createBuffer(meshes.get(0), paresedBones, maxWeight, vertexWeights);

            m_VertexAttribs[ATT_BONT_WEIGHT0].mVertexData = results[0];
            m_VertexAttribs[ATT_BONT_WEIGHT0].internalFormat = (maxWeight == 1) ? GLenum.GL_RG32F : GLenum.GL_RGBA32F;

            if(maxWeight > 2){
                m_VertexAttribs[ATT_BONT_WEIGHT1].mVertexData = results[1];
                m_VertexAttribs[ATT_BONT_WEIGHT1].internalFormat = (maxWeight == 3) ? GLenum.GL_RG32F : GLenum.GL_RGBA32F;
            }
        }

        // fill the indices.
        if(indiceCount > 0){
            IntBuffer indices = CacheBuffer.getCachedIntBuffer(indiceCount);
            for(int[] _indices : indiceBuffers){
                indices.put(_indices);
            }

            indices.flip();
            BufferGL result = new BufferGL();
            result.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, indiceCount * 4, indices, GLenum.GL_STATIC_DRAW);
            mIndexBuffer = result;

            mIndiceFormat = GLenum.GL_UNSIGNED_INT;
        }

        mVertexCount = vertexCount;
        mIndiceCount = indiceCount;

        mBoundingBox.init();
        computeBoundingBox(positionBuffers, mBoundingBox);
        boundingBox.expandBy(mBoundingBox);
    }

    void updateBlendShape(){}

    public void apply(){
        if(!mEnabled)
            return;

        if(!GLFuncProviderFactory.isInitlized())
            return;

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        if(mAsset.isStaticMesh()){
            if(m_VAO == 0)
                m_VAO = createVAO(m_VertexAttribs, mIndexBuffer, mAsset.mVertexLocation);

            gl.glBindVertexArray(m_VAO);
        }else{
            VertexLocation location = mAsset.mVertexLocation;
            if(location.position < 0)
                throw new IllegalStateException();

            bindAttribs(gl, m_VertexAttribs, mIndexBuffer, location);
        }

        if(mIndiceCount > 0){
            gl.glDrawElements(mPrimitive, mIndiceCount, mIndiceFormat, 0);
        }else{
            gl.glDrawArrays(mPrimitive, 0, mVertexCount);
        }

        if(mAsset.isStaticMesh()){
            gl.glBindVertexArray(0);
        }else{
            unbindAttribs(gl, m_VertexAttribs, mIndexBuffer, mAsset.mVertexLocation);
        }
    }

    private static int createVAO(VertexAttrib[] vertexData, BufferGL indiceBuffer, VertexLocation location){
        if(!GLFuncProviderFactory.isInitlized())
            return 0;

        if(location.position < 0)
            throw new IllegalStateException();

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        int VAO = gl.glGenVertexArray();
        gl.glBindVertexArray(VAO);
        bindAttribs(gl, vertexData, indiceBuffer, location);
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

        return VAO;
    }

    private static void bindAttribs(GLFuncProvider gl, VertexAttrib[] vertexData, BufferGL indiceBuffer, VertexLocation location){
        bindAttrib(gl, vertexData[ATT_POSITION].mVertexData, location.position, vertexData[ATT_POSITION].internalFormat, vertexData[ATT_POSITION].instanceRate);
        bindAttrib(gl, vertexData[ATT_NORMAL].mVertexData, location.normal, vertexData[ATT_NORMAL].internalFormat, vertexData[ATT_NORMAL].instanceRate);
        bindAttrib(gl, vertexData[ATT_TEXCOORD].mVertexData, location.texcoord, vertexData[ATT_TEXCOORD].internalFormat, vertexData[ATT_TEXCOORD].instanceRate);
        bindAttrib(gl, vertexData[ATT_TANGENT].mVertexData, location.tangent, vertexData[ATT_TANGENT].internalFormat, vertexData[ATT_TANGENT].instanceRate);
        bindAttrib(gl, vertexData[ATT_COLOR].mVertexData, location.color, vertexData[ATT_COLOR].internalFormat, vertexData[ATT_COLOR].instanceRate);
        bindAttrib(gl, vertexData[ATT_BONT_WEIGHT0].mVertexData, location.boneWeight0, vertexData[ATT_BONT_WEIGHT0].internalFormat, vertexData[ATT_BONT_WEIGHT0].instanceRate);
        bindAttrib(gl, vertexData[ATT_BONT_WEIGHT1].mVertexData, location.boneWeight1, vertexData[ATT_BONT_WEIGHT1].internalFormat, vertexData[ATT_BONT_WEIGHT1].instanceRate);

        if(indiceBuffer != null){
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, indiceBuffer.getBuffer());
        }
    }

    private static void bindAttrib(GLFuncProvider gl, BufferGL buffer, int location, int internalFormat, int instaceRate){
        if(location >=0 && buffer != null){
            int type =  TextureUtils.measureDataType(internalFormat);
            int size = TextureUtils.getFormatChannels(internalFormat);
            boolean normalized = TextureUtils.isNormalizedFormat(internalFormat);

            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, buffer.getBuffer());
            gl.glVertexAttribPointer(location, size, type, normalized, 0, 0);
            gl.glEnableVertexAttribArray(location);

            if(instaceRate > 0)
                gl.glVertexAttribDivisor(location, instaceRate);
        }
    }

    private static void unbindAttribs(GLFuncProvider gl, VertexAttrib[] vertexData, BufferGL indiceBuffer, VertexLocation location){
        unbindAttrib(gl, location.position,  vertexData[ATT_POSITION].instanceRate);
        unbindAttrib(gl, location.normal,  vertexData[ATT_NORMAL].instanceRate);
        unbindAttrib(gl, location.texcoord, vertexData[ATT_TEXCOORD].instanceRate);
        unbindAttrib(gl, location.tangent, vertexData[ATT_TANGENT].instanceRate);
        unbindAttrib(gl, location.color, vertexData[ATT_COLOR].instanceRate);
        unbindAttrib(gl, location.boneWeight0, vertexData[ATT_BONT_WEIGHT0].instanceRate);
        unbindAttrib(gl, location.boneWeight1, vertexData[ATT_BONT_WEIGHT1].instanceRate);

        if(indiceBuffer != null){
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    private static void unbindAttrib(GLFuncProvider gl, int location, int instaceRate){
        if(location >=0){
            gl.glDisableVertexAttribArray(location);

            if(instaceRate > 0)
                gl.glVertexAttribDivisor(location, 0);
        }
    }

    private static void computeBoundingBox(List<AIVector3D.Buffer> buffers, BoundingBox boundingBox){
        for(AIVector3D.Buffer buffer : buffers){
            final int vertexCount = buffer.remaining();
            ByteBuffer source = MemoryUtil.memByteBuffer(buffer.address(), AIVector3D.SIZEOF * vertexCount);

            for(int i = 0; i< vertexCount; i++){
                float x = source.getFloat();
                float y = source.getFloat();
                float z = source.getFloat();

                boundingBox.expandBy(x,y,z);
            }
        }
    }

    private static BufferGL createBuffer(int target, List<AIVector3D.Buffer> buffers, int size){
        BufferGL result = new BufferGL();

        ByteBuffer mem = CacheBuffer.getCachedByteBuffer(size);
        final long address = MemoryUtil.memAddress(mem);
        int offset = 0;
        for(AIVector3D.Buffer buffer : buffers){
            int length = buffer.remaining() * AIVector3D.SIZEOF;
            MemoryUtil.memCopy(buffer.address(), address + offset, length);
            offset += length;
        }

        result.initlize(target, size, mem, GLenum.GL_STATIC_DRAW);
        return result;
    }

    private static BufferGL createBufferC(int target, List<AIColor4D.Buffer> buffers, int size){
        BufferGL result = new BufferGL();

        ByteBuffer mem = CacheBuffer.getCachedByteBuffer(size);
        final long address = MemoryUtil.memAddress(mem);
        int offset = 0;
        for(AIColor4D.Buffer buffer : buffers){
            int length = buffer.remaining() * AIVector3D.SIZEOF;
            MemoryUtil.memCopy(buffer.address(), address + offset, length);
            offset += length;
        }

        result.initlize(target, size, mem, GLenum.GL_STATIC_DRAW);
        return result;
    }

    private static BufferGL[] createBuffer(AIMesh mesh, Map<String, Pair<AIBone, Integer>> paresedBones, int maxWeight, int[] vertexWeights){
        final int vertexCount = vertexWeights.length;

        ByteBuffer mem0 = null;
        ByteBuffer mem1 = null;

        int stride0 = 0;
        int stride1 = 0;

        if(maxWeight ==1){
            mem0 = CacheBuffer.getCachedByteBuffer(vertexCount * maxWeight * Vector2f.SIZE);
            stride0 = Vector2f.SIZE;
        }else if(maxWeight == 2){
            mem0 = CacheBuffer.getCachedByteBuffer(vertexCount * maxWeight * Vector4f.SIZE);
            stride0 = Vector4f.SIZE;
        }else if(maxWeight == 3){
            mem0 = CacheBuffer.getCachedByteBuffer(vertexCount * maxWeight * Vector4f.SIZE);
            mem1 = BufferUtils.createByteBuffer(vertexCount * maxWeight * Vector2f.SIZE);

            stride0 = Vector4f.SIZE;
            stride1 = Vector2f.SIZE;
        }else /*if(maxWeight == 4)*/{
            mem0 = CacheBuffer.getCachedByteBuffer(vertexCount * maxWeight * Vector4f.SIZE);
            mem1 = BufferUtils.createByteBuffer(vertexCount * maxWeight * Vector4f.SIZE);

            stride0 = Vector4f.SIZE;
            stride1 = Vector4f.SIZE;
        }

        Arrays.fill(vertexWeights, 0);
        final int numBones = mesh.mNumBones();
        PointerBuffer boneBuffers = mesh.mBones();
        for(int i = 0; i < numBones; i++){
            AIBone bone = AIBone.create(boneBuffers.get(i));
            final String boneName = bone.mName().dataString();
            int boneIndex = paresedBones.get(boneName).second;

            final int numWeights = bone.mNumWeights();
            AIVertexWeight.Buffer weightBuffers = bone.mWeights();
            ByteBuffer data = MemoryUtil.memByteBuffer(weightBuffers.address(), AIVertexWeight.SIZEOF * weightBuffers.remaining());
            for(int j = 0; j < numWeights; j++){
                int vertexID = data.getInt(j*AIVertexWeight.SIZEOF);
                float weight = data.getFloat(j*AIVertexWeight.SIZEOF+4);

                if(vertexWeights[vertexID] < 2){
                    // put the values into mem0
                    mem0.putFloat(vertexID * stride0 + vertexWeights[vertexID] * Vector2f.SIZE, weight);
                    mem0.putInt(vertexID * stride0 + vertexWeights[vertexID] * Vector2f.SIZE + 4, boneIndex);
                }else{
                    mem1.putFloat(vertexID * stride1 + (vertexWeights[vertexID]-2) * Vector2f.SIZE, weight);
                    mem1.putInt(vertexID * stride1+ (vertexWeights[vertexID]-2) * Vector2f.SIZE + 4, boneIndex);
                }

                vertexWeights[vertexID]++;
            }
        }

        BufferGL[] results = new BufferGL[2];
        results[0] = new BufferGL();
        results[0].initlize(GLenum.GL_ARRAY_BUFFER, mem0.remaining(), mem0, GLenum.GL_STATIC_DRAW);

        if(mem1 != null){
            results[1] = new BufferGL();
            results[1].initlize(GLenum.GL_ARRAY_BUFFER, mem1.remaining(), mem1, GLenum.GL_STATIC_DRAW);
        }

        return results;
    }

    private static void check(boolean condition, int a, int b){
        if(GLCheck.CHECK && condition){
            if(a != b)
                throw new IllegalStateException();
        }
    }

    private static void checkNot(boolean condition, int a, int b){
        if(GLCheck.CHECK && condition){
            if(a == b)
                throw new IllegalStateException();
        }
    }
}
