package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.models.obj.NvGLModel;
import com.nvidia.developer.opengl.models.obj.NvModel;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks;
import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks_ErrorBlob;
import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks_Result;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class CMesh implements Disposeable{

    int	m_primtopo;
    final Vector3f m_posMin = new Vector3f(), m_posMax = new Vector3f();		// Bounding box in local space
    final Vector3f			m_posCenter = new Vector3f();			// Center of bounding box
    float						m_diameter;				// Diameter of bounding box
    float						m_uvScale = 1.f;				// Average world-space size of 1 UV unit
    private GLFuncProvider gl;
    private NvGLModel m_model;
    private int m_curvatureVB;

    CMesh(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void loadModel(String filename){
        NvGLModel model = new NvGLModel();
        model.loadModelFromFile(filename);
        model.initBuffers(true);

        float[] curvatures = CalculateCurvature(model.getModel());
        m_uvScale = CalculateUVScale(model.getModel());

        m_curvatureVB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_curvatureVB);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(curvatures), GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        m_model = model;
    }
    Vector3f getPosMin(){ return m_model.getMinExt();}
    Vector3f getPosMax(){ return m_model.getMaxExt();}

    float[] CalculateCurvature(NvModel pMesh)
    {
        // Calculate mesh curvature - also demonstrate using a custom allocator
        final float[] curvatures = new float[pMesh.getCompiledVertexCount()];
        GFSDK_FaceWorks_ErrorBlob errorBlob = new GFSDK_FaceWorks_ErrorBlob();
        GFSDK_FaceWorks_Result result = GFSDK_FaceWorks.GFSDK_FaceWorks_CalculateMeshCurvature(
        /*int(pMesh->m_verts.size())*/ pMesh.getCompiledVertexCount(),
        /*&pMesh->m_verts[0].m_pos*/pMesh.getCompiledVertices(),
            /*sizeof(Vertex)*/pMesh.getCompiledVertexSize()*4,
                pMesh.getCompiledPositionOffset()*4,
        /*&pMesh->m_verts[0].m_normal*/ pMesh.getCompiledVertices(),
            /*sizeof(Vertex)*/pMesh.getCompiledVertexSize()*4,
                pMesh.getCompiledNormalOffset()*4,
        /*int(pMesh->m_indices.size())*/pMesh.getCompiledIndexCount(NvModel.TRIANGLES),
        /*&pMesh->m_indices[0]*/pMesh.getCompiledIndices(NvModel.TRIANGLES),
            2, // smoothing passes
        /*&pMesh->m_verts[0].m_curvature*/curvatures,
            /*sizeof(Vertex)*/4,
        errorBlob/*,
        &allocator*/);

        if (result != GFSDK_FaceWorks_Result.OK)
        {
//            #if defined(_DEBUG)
//            wchar_t msg[512];
//            _snwprintf_s(msg, dim(msg), _TRUNCATE,
//                    L"GFSDK_FaceWorks_CalculateMeshCurvature() failed:\n%hs", errorBlob.m_msg);
//            DXUTTrace(__FILE__, __LINE__, E_FAIL, msg, true);
//            #endif
//            GFSDK_FaceWorks_FreeErrorBlob(&errorBlob);
            throw new IllegalArgumentException(errorBlob.m_msg);
        }

//        #if defined(_DEBUG) && 0
        // Report min, max, mean curvature over mesh
        float minCurvature = Float.MAX_VALUE;
        float maxCurvature = -Float.MAX_VALUE;
        float curvatureSum = 0.0f;
        for (int i = 0, cVert = pMesh.getCompiledVertexCount(); i < cVert; ++i)
        {
            minCurvature = Math.min(minCurvature, curvatures[i]);
            maxCurvature = Math.max(maxCurvature, curvatures[i]);
            curvatureSum += curvatures[i];
        }
        float meanCurvature = curvatureSum / /*float(pMesh->m_verts.size())*/pMesh.getCompiledVertexCount();
        LogUtil.i(LogUtil.LogType.DEFAULT, String.format("\tCurvature min = %0.2f cm^-1, max = %0.2f cm^-1, mean = %0.2f cm^-1",
                minCurvature, maxCurvature, meanCurvature));
//        #endif // defined(_DEBUG)\
        return curvatures;
    }

    float CalculateUVScale(NvModel pMesh)
    {
        final float[] uvScales = new float[1];
        GFSDK_FaceWorks_ErrorBlob errorBlob = new GFSDK_FaceWorks_ErrorBlob();
        GFSDK_FaceWorks_Result result = GFSDK_FaceWorks.GFSDK_FaceWorks_CalculateMeshUVScale(
        /*int(pMesh->m_verts.size())*/ pMesh.getCompiledVertexCount(),
        /*&pMesh->m_verts[0].m_pos*/pMesh.getCompiledVertices(),
            /*sizeof(Vertex)*/pMesh.getCompiledVertexSize()*4,
                pMesh.getCompiledPositionOffset() * 4,
        /*&pMesh->m_verts[0].m_uv*/pMesh.getCompiledVertices(),
            /*sizeof(Vertex)*/pMesh.getCompiledVertexSize()*4,
                pMesh.getCompiledTexCoordOffset()*4,
        /*int(pMesh->m_indices.size())*/pMesh.getCompiledIndexCount(),
        /*&pMesh->m_indices[0]*/pMesh.getCompiledIndices(),
        /*&pMesh->m_uvScale*/uvScales,
        errorBlob);
        if (result != GFSDK_FaceWorks_Result.OK)
        {
            /*#if defined(_DEBUG)
            wchar_t msg[512];
            _snwprintf_s(msg, dim(msg), _TRUNCATE,
                    L"GFSDK_FaceWorks_CalculateMeshUVScale() failed:\n%hs", errorBlob.m_msg);
            DXUTTrace(__FILE__, __LINE__, E_FAIL, msg, true);
            #endif
            GFSDK_FaceWorks_FreeErrorBlob(&errorBlob);
            return;*/
            throw new IllegalArgumentException(errorBlob.m_msg);
        }

        LogUtil.i(LogUtil.LogType.DEFAULT, String.format("\tUV scale %0.2f cm\n",uvScales[0]));
        return uvScales[0];
    }

    void Draw(){
        m_model.drawElements(0, 1, 2, 3);
    }

    @Override
    public void dispose() {
        m_model.dispose();
    }
}
