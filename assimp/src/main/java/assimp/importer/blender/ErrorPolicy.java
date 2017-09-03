package assimp.importer.blender;

/** Range of possible behaviours for fields absend in the input file. Some are
 *  mission critical so we need them, while others can silently be default
 *  initialized and no animations are harmed. */
interface ErrorPolicy {

	/** Substitute default value and ignore */
	static final int ErrorPolicy_Igno = 0;
	/** Substitute default value and write to log */
	static final int ErrorPolicy_Warn = 1;
	/** Substitute a massive error message and crash the whole matrix. Its time for another zion */
	static final int ErrorPolicy_Fail = 2;
}
