package assimp.importer.blender;

import assimp.common.AssimpConfig;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.StreamReader;

/** Factory to extract a #DNA from the DNA1 file block in a BLEND file. */
final class DNAParser implements FieldFlags{
	
	final FileDatabase db;
	
	public DNAParser(FileDatabase db) {
		this.db = db;
	}
	
	/** Obtain a reference to the extracted DNA information */
	DNA getDNA() { return db.dna;}
	
	static boolean match4(StreamReader stream, String string){
		byte b1 = stream.getI1();
		byte b2 = stream.getI1();
		byte b3 = stream.getI1();
		byte b4 = stream.getI1();
		
		return (b1==string.charAt(0) && b2==string.charAt(1) && b3 ==string.charAt(2) && b4==string.charAt(3));
	}
	
	/** Locate the DNA in the file and parse it. The input
	 *  stream is expected to point to the beginning of the DN1
	 *  chunk at the time this method is called and is
	 *  undefined afterwards.
	 *  @throw DeadlyImportError if the DNA cannot be read.
	 *  @note The position of the stream pointer is undefined
	 *    afterwards.*/
	void parse(){
		StreamReader stream = db.reader;
		DNA dna = db.dna;

		if(!match4(stream,"SDNA")) {
			throw new DeadlyImportError("BlenderDNA: Expected SDNA chunk");
		}

		// name dictionary
		if(!match4(stream,"NAME")) {
			throw new DeadlyImportError("BlenderDNA: Expected NAME field");
		}
		
//		std::vector<std::string> names (stream.GetI4());
//		for_each(std::string& s, names) {
//			while (char c = stream.GetI1()) {
//				s += c;
//			}
//		}
		StringBuilder sb = new StringBuilder();
		String[] names = new String[stream.getI4()];
		for(int i = 0; i < names.length; i++){
			if(sb.length() > 0) sb.delete(0, sb.length());
			byte c;
			while((c = stream.getI1()) != 0){
				sb.append((char)c);
			}
			
			names[i] = sb.toString();
		}

		// type dictionary
		for (;(stream.getCurrentPos() & 0x3) != 0; stream.getI1());
		if(!match4(stream,"TYPE")) {
			throw new DeadlyImportError("BlenderDNA: Expected TYPE field");
		}
		
//		std::vector<Type> types (stream.GetI4());
//		for_each(Type& s, types) {
//			while (char c = stream.GetI1()) {
//				s.name += c;
//			}
//		}
		
		Type[] types = new Type[stream.getI4()];
		for(int i = 0; i < types.length; i++){
			if(sb.length() > 0) sb.delete(0, sb.length());
			byte c;
			while((c = stream.getI1()) != 0)
				sb.append((char)c);
			
			types[i] = new Type(sb.toString());
		}
		sb = null;
		
		// type length dictionary
		for (;(stream.getCurrentPos() & 0x3) != 0; stream.getI1());
		if(!match4(stream,"TLEN")) {
			throw new DeadlyImportError("BlenderDNA: Expected TLEN field");
		}
		
//		for_each(Type& s, types) {
//			s.size = stream.GetI2();
//		}
		for(Type s : types)
			s.size = stream.getI2();

		// structures dictionary
		for (;(stream.getCurrentPos() & 0x3) != 0; stream.getI1());
		if(!match4(stream,"STRC")) {
			throw new DeadlyImportError("BlenderDNA: Expected STRC field");
		}

		int end = stream.getI4(), fields = 0;

//		dna.structures.reserve(end);
		dna.structures.ensureCapacity(end);
		for(int i = 0; i != end; ++i) {
			
			short n = stream.getI2();
			if (n >= types.length) {
				throw new DeadlyImportError(
					"BlenderDNA: Invalid type index in structure name"  + n + 
					" (there are only "+ types.length+ " entries)"
				);
			}

			// maintain separate indexes
//			dna.indices[types[n].name] = dna.structures.size();
			dna.indices.put(types[n].name, dna.structures.size());

			Structure s;
			dna.structures.add(s = new Structure());
//			Structure& s = dna.structures.back();
			s.name  = types[n].name;
			//s.index = dna.structures.size()-1;

			n = stream.getI2();
//			s.fields.reserve(n);
			s.fields.ensureCapacity(n);

			int offset = 0;
			for (int m = 0; m < n; ++m, ++fields) {

				short j = stream.getI2();
				if (j >= types.length) {
					throw new DeadlyImportError(
						"BlenderDNA: Invalid type index in structure field " + j +  
						" (there are only " + types.length + " entries)"
					);
				}
				Field f;
				s.fields.add(f = new Field());
//				Field& f = s.fields.back();
				f.offset = offset;

				f.type = types[j].name;
				f.size = types[j].size;

				j = stream.getI2();
				if (j >= names.length) {
					throw new DeadlyImportError( 
						"BlenderDNA: Invalid name index in structure field " + j+ 
						" (there are only "+ names.length+" entries)"
					);
				}

				f.name = names[j];
				f.flags = 0;
				
				// pointers always specify the size of the pointee instead of their own.
				// The pointer asterisk remains a property of the lookup name.
				if (f.name.charAt(0) == '*') {
					f.size = db.i64bit ? 8 : 4;
					f.flags |= FieldFlag_Pointer;
				}

				// arrays, however, specify the size of a single element so we
				// need to parse the (possibly multi-dimensional) array declaration
				// in order to obtain the actual size of the array in the file.
				// Also we need to alter the lookup name to include no array
				// brackets anymore or size fixup won't work (if our size does 
				// not match the size read from the DNA).
				int rbegin = f.name.length() - 1;
				if (/**f.name.rbegin()*/ f.name.charAt(rbegin) == ']') {
					final int rb = f.name.indexOf('[');
					if (rb == -1) {
						throw new DeadlyImportError(
							"BlenderDNA: Encountered invalid array declaration " +
							f.name);
					}

					f.flags |= FieldFlag_Array; 
					DNA.extractArraySize(f.name,f.array_sizes);
					f.name = f.name.substring(0,rb);

					f.size *= f.array_sizes[0] * f.array_sizes[1];
				}

				// maintain separate indexes
//				s.indices[f.name] = s.fields.size()-1;
				s.indices.put(f.name, s.fields.size()-1);
				offset += f.size;
			}
			s.size = offset;
		}

		if(DefaultLogger.LOG_OUT)
			DefaultLogger.debug("BlenderDNA: Got " + dna.structures.size() + 
			" structures with totally " + fields +" fields");

		if(AssimpConfig.ASSIMP_BUILD_BLENDER_DEBUG)
			dna.dumpToFile();

		dna.addPrimitiveStructures();
		dna.registerConverters();
	}
	
	private static final class Type{
		int size;
		String name;
		
		public Type(String name) {
			this.name = name;
		}
	}
}
