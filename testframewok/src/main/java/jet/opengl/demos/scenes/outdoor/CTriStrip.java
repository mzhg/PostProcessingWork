package jet.opengl.demos.scenes.outdoor;

import jet.opengl.postprocessing.util.StackInt;

final class CTriStrip {

	static final int 
	QUAD_TRIANG_TYPE_UNDEFINED = 0,

    // 01      11
    //  *------*
    //  |   .' |
    //  | .'   |
    //  * -----*
    // 00      10
    QUAD_TRIANG_TYPE_00_TO_11 = 1,

    // 01      11
    //  *------*
    //  | '.   |
    //  |   '. |
    //  * -----*
    // 00      10
    QUAD_TRIANG_TYPE_01_TO_10 = 2;
	
	private StackInt m_Indices;
	private int m_QuadTriangType = QUAD_TRIANG_TYPE_UNDEFINED;
	private CIndexGenerator m_IndexGenerator;
	
	public CTriStrip(StackInt indices, CIndexGenerator indexGenerator) {
		this.m_Indices = indices;
		m_IndexGenerator = indexGenerator;
	}
	
	void addStrip(int iBaseIndex,
            int iStartCol,
            int iStartRow,
            int iNumCols,
            int iNumRows,
            int QuadTriangType)
{
  assert( QuadTriangType == QUAD_TRIANG_TYPE_00_TO_11 || QuadTriangType == QUAD_TRIANG_TYPE_01_TO_10 );
  int iFirstVertex = iBaseIndex + m_IndexGenerator.indexOf(iStartCol, iStartRow + (QuadTriangType==QUAD_TRIANG_TYPE_00_TO_11 ? 1:0));
  if(m_QuadTriangType != QUAD_TRIANG_TYPE_UNDEFINED)
  {
      // To move from one strip to another, we have to generate two degenerate triangles
      // by duplicating the last vertex in previous strip and the first vertex in new strip
      m_Indices.push( m_Indices.peer() );
      m_Indices.push( iFirstVertex );
  }

  if(m_QuadTriangType != QUAD_TRIANG_TYPE_UNDEFINED && m_QuadTriangType != QuadTriangType || 
     m_QuadTriangType == QUAD_TRIANG_TYPE_UNDEFINED && QuadTriangType == QUAD_TRIANG_TYPE_01_TO_10)
  {
      // If triangulation orientation changes, or if start strip orientation is 01 to 10, 
      // we also have to add one additional vertex to preserve winding order
      m_Indices.push( iFirstVertex );
  }
  m_QuadTriangType = QuadTriangType;

  for(int iRow = 0; iRow < iNumRows-1; ++iRow)
  {
      for(int iCol = 0; iCol < iNumCols; ++iCol)
      {
          int iV00 = iBaseIndex + m_IndexGenerator.indexOf(iStartCol+iCol, iStartRow+iRow);
          int iV01 = iBaseIndex + m_IndexGenerator.indexOf(iStartCol+iCol, iStartRow+iRow+1);
          if( m_QuadTriangType == QUAD_TRIANG_TYPE_01_TO_10 )
          {
              if( iCol == 0 && iRow == 0)
                  assert(iFirstVertex == iV00);
              // 01      11
              //  *------*
              //  | '.   |
              //  |   '. |
              //  * -----*
              // 00      10
              m_Indices.push(iV00);
              m_Indices.push(iV01);
          }
          else if( m_QuadTriangType == QUAD_TRIANG_TYPE_00_TO_11 )
          {
              if( iCol == 0 && iRow == 0)
                  assert(iFirstVertex == iV01);
              // 01      11
              //  *------*
              //  |   .' |
              //  | .'   |
              //  * -----*
              // 00      10
              m_Indices.push(iV01);
              m_Indices.push(iV00);
          }
          else
          {
              assert(false);
          }
      }
  
      if(iRow < iNumRows-2)
      {
          m_Indices.push( m_Indices.peer() );
          m_Indices.push( iBaseIndex + m_IndexGenerator.indexOf(iStartCol, iStartRow+iRow+1 + (QuadTriangType==QUAD_TRIANG_TYPE_00_TO_11 ? 1:0)) );
      }
  }
}
}
