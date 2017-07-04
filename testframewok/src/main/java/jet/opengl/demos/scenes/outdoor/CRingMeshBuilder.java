package jet.opengl.demos.scenes.outdoor;

import com.nvidia.developer.opengl.utils.BoundingBox;
import com.nvidia.developer.opengl.utils.StackFloat;
import com.nvidia.developer.opengl.utils.StackInt;

import java.nio.IntBuffer;
import java.util.List;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

final class CRingMeshBuilder {

	private List<SRingSectorMesh> m_RingMeshes;
	private StackFloat m_VB;
	private int m_iGridDimenion;
	
	public CRingMeshBuilder(StackFloat vb, int iGridDimenion,List<SRingSectorMesh> ringMeshes) {
		m_RingMeshes = ringMeshes;
		m_VB = vb;
		m_iGridDimenion = iGridDimenion;
	}
	
	void createMesh(int iBaseIndex, 
            int iStartCol, 
            int iStartRow, 
            int iNumCols, 
            int iNumRows, 
            int QuadTriangType){
		SRingSectorMesh CurrMesh;
		m_RingMeshes.add(CurrMesh = new SRingSectorMesh() );
//        auto& CurrMesh = m_RingMeshes.back();

//        std::vector<UINT> IB;
//        StdTriStrip32 TriStrip( IB, CStdIndexGenerator(m_iGridDimenion) );
		StackInt IB = new StackInt();
		CTriStrip TriStrip = new CTriStrip(IB, new CIndexGenerator(m_iGridDimenion));
        TriStrip.addStrip(iBaseIndex, iStartCol, iStartRow, iNumCols, iNumRows, QuadTriangType);

        CurrMesh.uiNumIndices = IB.size();
        
     // Create the buffer
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        int buffer = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, buffer);
        IntBuffer data = CacheBuffer.wrap(IB.getData(), 0, IB.size());
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, data, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        
        CurrMesh.pIndBuff = buffer;
        
     // Compute bounding box
        final float FLT_MAX = Float.MAX_VALUE;
        BoundingBox BB = CurrMesh.boundBox;
//        BB.fMaxX =BB.fMaxY = BB.fMaxZ = -FLT_MAX;
//        BB.fMinX =BB.fMinY = BB.fMinZ = +FLT_MAX;
        BB._max.set(-FLT_MAX, -FLT_MAX, -FLT_MAX);
        BB._min.set(FLT_MAX, FLT_MAX, FLT_MAX);
//        for(auto Ind = IB.begin(); Ind != IB.end(); ++Ind)
        int[] indices = IB.getData();
        float[] verts = m_VB.getData();
        
        for(int i = 0; i < IB.size(); i++)
        {
        	int Ind = indices[i];
//            const auto &CurrVert = m_VB[*Ind].f3WorldPos;
        	int index = 5 * Ind;
        	float CurrVertX = verts[index + 0];
        	float CurrVertY = verts[index + 1];
        	float CurrVertZ = verts[index + 2];
        	
            BB._min.x = Math.min(BB._min.x, CurrVertX);
            BB._min.y = Math.min(BB._min.y, CurrVertY);
            BB._min.z = Math.min(BB._min.z, CurrVertZ);

            BB._max.x = Math.max(BB._max.x, CurrVertX);
            BB._max.y = Math.max(BB._max.y, CurrVertY);
            BB._max.z = Math.max(BB._max.z, CurrVertZ);
        }
	}
}
