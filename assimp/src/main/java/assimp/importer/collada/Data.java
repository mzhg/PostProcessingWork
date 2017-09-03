package assimp.importer.collada;

import java.util.ArrayList;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

/** Data source array: either floats or strings */
final class Data {
	boolean mIsStringArray;
	FloatArrayList mValues;
	ArrayList<String> mStrings;
}
