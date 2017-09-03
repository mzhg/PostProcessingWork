package assimp.importer.blender;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import assimp.common.AssUtil;
import assimp.common.DefaultLogger;

/** Represents the full data structure information for a single BLEND file.
 *  This data is extracted from the DNA1 chunk in the file.<p>
 *  #DNAParser does the reading and represents currently the only place where 
 *  DNA is altered.*/
final class DNA {
	
	final Map<String, FactoryPair> converters = new HashMap<>();
	final ArrayList<Structure> structures = new ArrayList<>();
	final Object2IntMap<String> indices = new Object2IntOpenHashMap<>(7);
	
	public DNA() {
		indices.defaultReturnValue(-1);
	}
	
	/** Access a structure by its canonical name, the pointer version returns NULL on failure 
	  * while the reference version raises an error. */
	Structure get(String ss){
//		std::map<std::string, size_t>::const_iterator it = indices.find(ss);
//		if (it == indices.end()) {
//			throw Error((Formatter::format(),
//				"BlendDNA: Did not find a structure named `",ss,"`"
//				));
//		}
//
//		return structures[(*it).second];
		
		int index = indices.getInt(ss);
		if(index == -1){
//			throw new Error("BlendDNA: Did not find a structure named `" + ss + "'");
			return null;
		}
		
		return structures.get(index);
	}
	
	/** Access a structure by its canonical name, it would throw a Error if couldn't find the specified Structure. */
	Structure find(String ss){
//		std::map<std::string, size_t>::const_iterator it = indices.find(ss);
//		if (it == indices.end()) {
//			throw Error((Formatter::format(),
//				"BlendDNA: Did not find a structure named `",ss,"`"
//				));
//		}
//
//		return structures[(*it).second];
		
		int index = indices.getInt(ss);
		if(index == -1){
			throw new Error("BlendDNA: Did not find a structure named `" + ss + "'");
		}
		
		return structures.get(index);
	}
	
	Structure get(int i){
		try {
			return structures.get(i);
		} catch (Exception e) {
			throw new Error("BlendDNA: There is no structure with index `" + i + "'");
		}
	}
	
	// --------------------------------------------------------
	// basing on http://www.blender.org/development/architecture/notes-on-sdna/
	/** Add structure definitions for all the primitive types,
	 *  i.e. integer, short, char, float */
	void addPrimitiveStructures(){
		// NOTE: these are just dummies. Their presence enforces
		// Structure::Convert<target_type> to be called on these
		// empty structures. These converters are special 
		// overloads which scan the name of the structure and
		// perform the required data type conversion if one
		// of these special names is found in the structure
		// in question.
		Structure back;
		
		indices.put("int", structures.size());
		structures.add(back = new Structure() );
		back.name = "int";
		back.size = 4;

		indices.put("short", structures.size());
		structures.add(back = new Structure() );
		back.name = "short";
		back.size = 2;


		indices.put("char", structures.size());
		structures.add(back = new Structure() );
		back.name = "char";
		back.size = 1;
		
		indices.put("float", structures.size());
		structures.add(back = new Structure() );
		back.name = "float";
		back.size = 4;
		
		indices.put("double", structures.size());
		structures.add(back = new Structure() );
		back.name = "double";
		back.size = 8;

		// no long, seemingly.
	}

	// --------------------------------------------------------
	/** Fill the @c converters member with converters for all 
	 *  known data types. The implementation of this method is
	 *  in BlenderScene.cpp and is machine-generated.
	 *  Converters are used to quickly handle objects whose
	 *  exact data type is a runtime-property and not yet 
	 *  known at compile time (consier Object::data).*/
	void registerConverters(){
		converters.put("Object", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new BLEObject();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((BLEObject)in, db);
			}
		}
		));

		converters.put("Group", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new Group();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((Group)in, db);
			}
		}
		));

		converters.put("MTex", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MTex();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MTex)in, db);
			}
		}
		));

		converters.put("TFace", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new TFace();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((TFace)in, db);
			}
		}
		));

		converters.put("SubsurfModifierData", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new SubsurfModifierData();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((SubsurfModifierData)in, db);
			}
		}
		));

		converters.put("MFace", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MFace();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MFace)in, db);
			}
		}
		));

		converters.put("Lamp", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new Lamp();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((Lamp)in, db);
			}
		}
		));

		converters.put("MDeformWeight", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MDeformWeight();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MDeformWeight)in, db);
			}
		}
		));

		converters.put("PackedFile", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new PackedFile();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((PackedFile)in, db);
			}
		}
		));

		converters.put("Base", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new Base();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((Base)in, db);
			}
		}
		));

		converters.put("MTFace", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MTFace();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MTFace)in, db);
			}
		}
		));

		converters.put("Material", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new BLEMaterial();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((BLEMaterial)in, db);
			}
		}
		));

		converters.put("MTexPoly", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MTexPoly();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MTexPoly)in, db);
			}
		}
		));

		converters.put("Mesh", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new BLEMesh();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((BLEMesh)in, db);
			}
		}
		));

		converters.put("MDeformVert", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MDeformVert();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MDeformVert)in, db);
			}
		}
		));

		converters.put("World", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new World();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((World)in, db);
			}
		}
		));

		converters.put("MLoopCol", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MLoopCol();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MLoopCol)in, db);
			}
		}
		));

		converters.put("MVert", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MVert();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MVert)in, db);
			}
		}
		));

		converters.put("MEdge", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MEdge();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MEdge)in, db);
			}
		}
		));

		converters.put("MLoopUV", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MLoopUV();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MLoopUV)in, db);
			}
		}
		));

		converters.put("GroupObject", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new GroupObject();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((GroupObject)in, db);
			}
		}
		));

		converters.put("ListBase", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new ListBase();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((ListBase)in, db);
			}
		}
		));

		converters.put("MLoop", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MLoop();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MLoop)in, db);
			}
		}
		));

		converters.put("ModifierData", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new ModifierData();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((ModifierData)in, db);
			}
		}
		));

		converters.put("ID", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new ID();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((ID)in, db);
			}
		}
		));

		converters.put("MCol", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MCol();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MCol)in, db);
			}
		}
		));

		converters.put("MPoly", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MPoly();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MPoly)in, db);
			}
		}
		));

		converters.put("Scene", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new BLEScene();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((BLEScene)in, db);
			}
		}
		));

		converters.put("Library", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new Library();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((Library)in, db);
			}
		}
		));

		converters.put("Tex", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new Tex();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((Tex)in, db);
			}
		}
		));

		converters.put("Camera", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new BLECamera();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((BLECamera)in, db);
			}
		}
		));

		converters.put("MirrorModifierData", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new MirrorModifierData();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((MirrorModifierData)in, db);
			}
		}
		));

		converters.put("Image", new FactoryPair(new FactoryPair.AllocProcPtr() {
			public ElemBase call() {
				return new BLEImage();
			}
		},
		new FactoryPair.ConvertProcPtr() {
			public void call(Structure s, ElemBase in, FileDatabase db) {
				s.convert((BLEImage)in, db);
			}
		}
		));
	}


	// --------------------------------------------------------
	/** Take an input blob from the stream, interpret it according to 
	 *  a its structure name and convert it to the intermediate
	 *  representation. 
	 *  @param structure Destination structure definition
	 *  @param db File database.
	 *  @return A null pointer if no appropriate converter is available.*/
	ElemBase convertBlobToStructure(Structure structure, FileDatabase db){
//		std::map<std::string, FactoryPair >::const_iterator it = converters.find(structure.name);
//		if (it == converters.end()) {
//			return boost::shared_ptr< ElemBase >();
//		}
//
//		boost::shared_ptr< ElemBase > ret = (structure.*((*it).second.first))();
//		(structure.*((*it).second.second))(ret,db);
		FactoryPair it = converters.get(structure.name);
		if(it == null)
			return null;
		
		ElemBase ret = it.first.call();
		it.second.call(structure, ret, db);
		return ret;
	}

	// --------------------------------------------------------
	/** Find a suitable conversion function for a given Structure.
	 *  Such a converter function takes a blob from the input 
	 *  stream, reads as much as it needs, and builds up a
	 *  complete object in intermediate representation.
	 *  @param structure Destination structure definition
	 *  @param db File database.
	 *  @return A null pointer in .first if no appropriate converter is available.*/
	FactoryPair getBlobToStructureConverter( Structure structure, FileDatabase db){
		return converters.get(structure.name);
	}
	
	/** Dump the DNA to a text file. This is for debugging purposes. 
	 *  The output file is `dna.txt` in the current working folder*/
	void dumpToFile(){
		// we dont't bother using the VFS here for this is only for debugging.
		// (and all your bases are belong to us).

//		std::ofstream f("dna.txt");
//		if (f.fail()) {
//			DefaultLogger::get()->error("Could not dump dna to dna.txt");
//			return;
//		}
//		f << "Field format: type name offset size" << "\n";
//		f << "Structure format: name size" << "\n";
//
//		for_each(const Structure& s, structures) {
//			f << s.name << " " << s.size << "\n\n";
//			for_each(const Field& ff, s.fields) {
//				f << "\t" << ff.type << " " << ff.name << " " << ff.offset << " " << ff.size << std::endl;
//			}
//			f << std::endl;
//		}
//		DefaultLogger::get()->info("BlenderDNA: Dumped dna to dna.txt");
		
		try(FileWriter fileout = new FileWriter("dna.txt"); 
				BufferedWriter f = new BufferedWriter(fileout)){
			f.append("Field format: type name offset size\n");
			f.append("Structure format: name size\n");
			
			for(Structure s : structures){
				f.append(s.name).append(" ").append(Integer.toString(s.size)).append("\n\n");
				for(Field ff : s.fields){
					f.append("\t").append(ff.type).append(" ").append(ff.name).append(" ");
					f.append(Integer.toString(ff.offset)).append(" ").append(Integer.toString(ff.size)).append("\n");
				}
				f.append('\n');
			}
			
			DefaultLogger.info("BlenderDNA: Dumped dna to dna.txt");
		}catch (Exception e) {
			DefaultLogger.error("Could not dump dna to dna.txt");
			return;
		}
	}
	
	// --------------------------------------------------------
	/** Extract array dimensions from a C array declaration, such
	 *  as `...[4][6]`. Returned string would be `...[][]`.
	 *  @param out
	 *  @param array_sizes Receive maximally two array dimensions,
	 *    the second element is set to 1 if the array is flat.
	 *    Both are set to 1 if the input is not an array.
	 *  @throw DeadlyImportError if more than 2 dimensions are
	 *    encountered. */
	static void extractArraySize(String out, int[] array_sizes){
		array_sizes[0] = array_sizes[1] = 1;
		int pos = out.indexOf('[');
		if (pos++ == -1) {
			return;
		}
		array_sizes[0] = AssUtil.strtoul10(out, pos);

		pos = out.indexOf('[',pos);
		if (pos++ == -1) {
			return;
		}
		array_sizes[1] = AssUtil.strtoul10(out, pos);
	}
}
