package jet.opengl.demos.demos.scenes.outdoor;

/** Iterator for recursively traversing the quad tree starting from the specified level up to the root */
final class HierarchyReverseIterator extends  HierarchyIteratorBase{
	public HierarchyReverseIterator(int nLevels) {
		m_current.level = nLevels - 1;
		m_currentLevelSize = 1 << m_current.level;
	}
	
	boolean isValid() { return m_current.level >= 0; }
	void next()
	{
		if( ++m_current.horzOrder == m_currentLevelSize )
		{
			m_current.horzOrder = 0;
			if( ++m_current.vertOrder == m_currentLevelSize )
			{
				m_current.vertOrder = 0;
				m_currentLevelSize = 1 << --m_current.level;
			}
		}
	}
}
