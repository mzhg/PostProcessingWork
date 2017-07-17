package jet.opengl.demos.demos.scenes.outdoor;

class HierarchyArrayInt {

	private int[][] m_data;
	
	public int get(SQuadTreeNodeLocation at){
		
		return m_data[at.level][at.horzOrder + (at.vertOrder << at.level)];
	}
	
	public void set(SQuadTreeNodeLocation at, int value){
		m_data[at.level][at.horzOrder + (at.vertOrder << at.level)] = value;
	}
	
	public void resize(int numLevelsInHierarchy){
		m_data = new int[numLevelsInHierarchy][];
		if( numLevelsInHierarchy > 0 )
		{
			for(int level = numLevelsInHierarchy - 1; level >= 0;level-- )
			{
				int numElementsInLevel = 1 << level;
				m_data[level] = new int[numElementsInLevel*numElementsInLevel];
			}
		}
	}
}
