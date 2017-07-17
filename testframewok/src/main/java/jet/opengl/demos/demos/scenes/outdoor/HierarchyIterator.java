package jet.opengl.demos.demos.scenes.outdoor;

/** Iterator for recursively traversing the quad tree starting from the root up to the specified level */
final class HierarchyIterator extends HierarchyIteratorBase{

	private int m_nLevels;
	
	public HierarchyIterator(int nLevels) {
		m_nLevels = nLevels;
		
		m_currentLevelSize = 1;
	}
	
	boolean isValid() { return m_current.level < m_nLevels; }
	void next()
	{
		if( ++m_current.horzOrder == m_currentLevelSize )
		{
			m_current.horzOrder = 0;
			if( ++m_current.vertOrder == m_currentLevelSize )
			{
				m_current.vertOrder = 0;
				m_currentLevelSize = 1 << ++m_current.level;
			}
		}
	}
}
