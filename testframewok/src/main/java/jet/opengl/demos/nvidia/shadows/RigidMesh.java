package jet.opengl.demos.nvidia.shadows;

import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

final class RigidMesh implements Disposeable {

	private static final int PositionOffset = 0;   // [0,  5]
    private static final int NormalOffset   = 12;   // [6, 11]

    private Vertex[]    m_vertices;
    private int     m_vertexCount;      // Number of vertices in mesh
    private int     m_indexCount;       // Number of indices in mesh
    private int     m_vertexBuffer;     // vertex buffer object for vertices
    private int     m_indexBuffer;      // vertex buffer object for indices
    private final Vector3f m_center   = new Vector3f();
    private final Vector3f  m_extents = new Vector3f();
    private GLFuncProvider gl;
    
    public RigidMesh(float[][] vertices, int vertexCount, short[] indices, int indexCount) {
    	m_vertices = new Vertex[vertexCount];
    	m_vertexCount = vertexCount;
    	m_indexCount = indexCount;
        gl = GLFuncProviderFactory.getGLFuncProvider();

    	if (m_vertices != null)
        {
    		float FLT_MAX = Float.MAX_VALUE;
            // Convert all the vertices and calculate the bounding box
            float mins[] = { FLT_MAX, FLT_MAX, FLT_MAX };
            float maxs[] = { -FLT_MAX, -FLT_MAX, -FLT_MAX };
            for (int i = 0; i < vertexCount; ++i)
            {
            	m_vertices[i] = new Vertex();
                m_vertices[i].set(vertices[i]);
                for (int j = 0; j < 3; ++j)
                {
                    mins[j] = Math.min(vertices[i][j], mins[j]);
                    maxs[j] = Math.max(vertices[i][j], maxs[j]);
                }
            }

            // Calculate the center and extents from the bounding box
            m_extents.set(maxs[0] - mins[0], maxs[1] - mins[1], maxs[2] - mins[2]);
            m_extents.scale(0.5f);
            m_center.set(mins[0], mins[1], mins[2]);
            Vector3f.add(m_center, m_extents, m_center);
            
            /*System.out.println("Center: " + m_center);
            System.out.println("Extents: " + m_extents);
            System.out.println("vertexCount = " + vertexCount);
            System.out.println("indexCount = " + indexCount);
            System.out.println("---------------------------------------------\n");*/

            // This functions copies the vertex and index buffers into their respective VBO's
            m_vertexBuffer = gl.glGenBuffer();
            m_indexBuffer = gl.glGenBuffer();

            // Stick the data for the vertices into its VBO
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_vertexBuffer);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, wrap(m_vertices), GLenum.GL_STATIC_DRAW);

            // Stick the data for the indices into its VBO
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_indexBuffer);
            gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, CacheBuffer.wrap(indices, 0, indexCount), GLenum.GL_STATIC_DRAW);

            // Clear the VBO state
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
	}
    
    @Override
    public void dispose() {
    	if (m_vertexBuffer != 0)
        {
            gl.glDeleteBuffer(m_vertexBuffer);
            m_vertexBuffer = 0;
        }

        if (m_indexBuffer != 0)
        {
            gl.glDeleteBuffer(m_indexBuffer);
        	m_indexBuffer = 0;
        }
    }
    
    void render(int positionLocation, int normalLocation)
    {
        // Bind the VBO for the vertex data
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_vertexBuffer);

        // Set up attribute for the position (3 floats)
        if (positionLocation >= 0)
        {
            gl.glVertexAttribPointer(positionLocation, 3, GLenum.GL_FLOAT, false, 24, PositionOffset);
            gl.glEnableVertexAttribArray(positionLocation);
        }

        // Set up attribute for the normal (3 floats)
        if (normalLocation >= 0)
        {
            gl.glVertexAttribPointer(normalLocation, 3, GLenum.GL_FLOAT, false, 24, NormalOffset);
            gl.glEnableVertexAttribArray(normalLocation);
        }

        // Set up the indices
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_indexBuffer);

        // Do the actual drawing
        gl.glDrawElements(GLenum.GL_TRIANGLES, m_indexCount, GLenum.GL_UNSIGNED_SHORT, 0);

        // Clear state
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        if (positionLocation >= 0)
        {
            gl.glDisableVertexAttribArray(positionLocation);
        }
        if (normalLocation >= 0)
        {
            gl.glDisableVertexAttribArray(normalLocation);
        }
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    private static FloatBuffer wrap(Vertex[] data){
		FloatBuffer buf = CacheBuffer.getCachedFloatBuffer(data.length * 6);
		for(int i = 0; i < data.length; i++){
			buf.put(data[i].m_position);
			buf.put(data[i].m_normal);
		}
		buf.flip();
		return buf;
	}
    
    int getVertexCount()  { return m_vertexCount; }
    int getIndexCount()  { return m_indexCount; }

    Vector3f getCenter()  { return m_center; }
    Vector3f getExtents()  { return m_extents; }
    
    boolean isValid() { return m_vertexBuffer != 0 && m_indexBuffer != 0; }

    private static final class Vertex{
		float[] m_position = new float[3];
		float[] m_normal = new float[3];
		
		Vertex set(float[] v){
			m_position[0] = v[0];	
			m_position[1] = v[1];	
			m_position[2] = v[2];
			
			m_normal[0] = v[3];	
			m_normal[1] = v[4];	
			m_normal[2] = v[5];
			
			return this;
		}
	}
}
