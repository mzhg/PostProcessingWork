package assimp.importer.dxf;

import assimp.common.AssUtil;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.LineSplitter;
import assimp.common.StreamReader;

//read pairs of lines, parse group code and value and provide utilities
//to convert the data to the target data type.
final class LineReader {

	LineSplitter splitter;
	int groupcode;
	String value;
	int end;
	
	public LineReader(StreamReader reader) {
		splitter = new LineSplitter(reader, false, true);
	}
	
	// -----------------------------------------
	boolean is(int gc, String what) {
		return groupcode == gc && what.equals(value);
	}

	// -----------------------------------------
	boolean is(int gc) {
		return groupcode == gc;
	}

	// -----------------------------------------
	int groupCode() {
		return groupcode;
	}

	// -----------------------------------------
	String value() {
		return value;
	}

	// -----------------------------------------
	boolean end(){
		return !(end <= 1);
	}
	
	// -----------------------------------------
	int valueAsInt() {
		return AssUtil.parseInt(value)/*strtol10(value.c_str())*/;
	}

	// -----------------------------------------
	float valueAsFloat() {
//		return fast_atof(value.c_str());
		return AssUtil.parseFloat(value);
	}
	
	/** pseudo-iterator increment to advance to the next (groupcode/value) pair */
	LineReader next(){
		if (end != 0) {
			if (end == 1) {
				++end;
			}
			return this;
		}

		try {
			groupcode = AssUtil.parseInt(splitter.get().toString());  //strtol10(splitter->c_str());
//			splitter++;
			splitter.next();

			value = splitter.get().toString();
//			splitter++;
			splitter.next();

			// automatically skip over {} meta blocks (these are for application use
			// and currently not relevant for Assimp).
			if (value.length() > 0 && value.charAt(0) == '{') {

				int cnt = 0;
				for(;splitter.get().length() > 0 && splitter.get().charAt(0) != '}'; splitter.next(), cnt++);

//				splitter++;
				splitter.next();
				DefaultLogger.debug(AssUtil.makeString("DXF: skipped over control group (",cnt," lines)"));
			}
		} catch(Exception e) {
//			ai_assert(!splitter);
			throw new DeadlyImportError(e);
		}
		if (!splitter.hasMore()) {
			end = 1;
		}
		return this;
	}
}
