package jet.opengl.demos.demos.scenes.outdoor;

/** Base class for iterators traversing the quad tree */
class HierarchyIteratorBase {

	final SQuadTreeNodeLocation m_current = new SQuadTreeNodeLocation();
	int m_currentLevelSize;
	
	SQuadTreeNodeLocation get() { return m_current; }
	int level() { return m_current.level; }
	int horz()  { return m_current.horzOrder; }
	int vert()  { return m_current.vertOrder; }
}

