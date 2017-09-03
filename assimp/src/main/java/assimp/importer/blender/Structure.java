package assimp.importer.blender;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.ObjectHolder;

/** Represents a data structure in a BLEND file. A Structure defines n fields
 *  and their locatios and encodings the input stream. Usually, every
 *  Structure instance pertains to one equally-named data structure in the
 *  BlenderScene.h header. This class defines various utilities to map a
 *  binary `blob` read from the file to such a structure instance with
 *  meaningful contents. */
final class Structure implements ErrorPolicy, FieldFlags{

	String name;
	final ArrayList<Field> fields = new ArrayList<>();
	final Object2IntMap<String> indices = new Object2IntOpenHashMap<>();
	int size;
	
	int cache_idx = -1;
	
	public Structure() {
		indices.defaultReturnValue(-1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	/** Access a field of the structure by its canonical name. The pointer version
	 *  returns NULL on failure while the reference version raises an import error. */
	Field get(String ss){
		int i = indices.getInt(ss);
		return i >= 0 ? fields.get(i) : null;
	}
	
	/** Access a field of the structure by its index */
	Field get(int i){
		try {
			return fields.get(i);
		} catch (Exception e) {
			throw new DeadlyImportError("BlendDNA: There is no field with index `" + i + "` in structure `"+name+"`");
		}
	}
	
	byte convertByte(FileDatabase db){
		// automatic rescaling from char to float and vice versa (seems useful for RGB colors)
		if (name.equals("float")) {
//			dest = static_cast<char>(db.reader->GetF4() * 255.f);
//			return;
			return (byte) (db.reader.getF4() * 255.f);
		}
		else if (name.equals("double")) {
//			dest = static_cast<char>(db.reader->GetF8() * 255.f);
//			return;
			return (byte) (db.reader.getF8() * 255.0);
		}
		
		return (byte)convertDispatcher(this, db);
	}
	
	double readFieldDouble(String name, FileDatabase db, int error_flag){
		int old = db.reader.getCurrentPos();
		try {
			Field f = get(name);
			// find the structure definition pertaining to this field
			Structure s = db.dna.get(f.type);

			db.reader.incPtr(f.offset);
//			s.convert(out,db);
			return s.convertDouble(db);
		}
		catch (Exception e) {
			return _defaultInitializer(error_flag, e);
		}finally{
			// and recover the previous stream position
			db.reader.setCurrentPos(old);

			if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
				++db.stats().fields_read;
		}
	}
	
	float readFieldFloat(String name, FileDatabase db, int error_flag){
		int old = db.reader.getCurrentPos();
		try {
			Field f = get(name);
			// find the structure definition pertaining to this field
			Structure s = db.dna.get(f.type);

			db.reader.incPtr(f.offset);
//			s.convert(out,db);
			return s.convertFloat(db);
		}
		catch (Exception e) {
			return _defaultInitializer(error_flag, e);
		}finally{
			// and recover the previous stream position
			db.reader.setCurrentPos(old);

			if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
				++db.stats().fields_read;
		}
	}
	
	int readFieldInt(String name, FileDatabase db, int error_flag){
		int old = db.reader.getCurrentPos();
		try {
			Field f = get(name);
			// find the structure definition pertaining to this field
			Structure s = db.dna.get(f.type);

			db.reader.incPtr(f.offset);
//			s.convert(out,db);
			return s.convertInt(db);
		}
		catch (Exception e) {
			return _defaultInitializer(error_flag, e);
		}finally{
			// and recover the previous stream position
			db.reader.setCurrentPos(old);

			if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
				++db.stats().fields_read;
		}
	}
	
	short readFieldShort(String name, FileDatabase db, int error_flag){
		int old = db.reader.getCurrentPos();
		try {
			Field f = get(name);
			// find the structure definition pertaining to this field
			Structure s = db.dna.get(f.type);

			db.reader.incPtr(f.offset);
//			s.convert(out,db);
			return s.convertShort(db);
		}
		catch (Exception e) {
			return _defaultInitializer(error_flag, e);
		}finally{
			// and recover the previous stream position
			db.reader.setCurrentPos(old);

			if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
				++db.stats().fields_read;
		}
	}
	
	static byte _defaultInitializer(int error_flag, Exception e){
		switch (error_flag) {
		case ErrorPolicy.ErrorPolicy_Fail:
			throw new Error(e);
		case ErrorPolicy.ErrorPolicy_Igno:
			return (byte)0;
		case ErrorPolicy.ErrorPolicy_Warn:
			DefaultLogger.warn(e.toString());
			return (byte)0;
		default:
			return (byte)0;
		}
	}
	
	static void _defaultInitializer(ElemBase out, int error_flag, Exception e){
		switch (error_flag) {
		case ErrorPolicy.ErrorPolicy_Fail:
			throw new Error(e);
		case ErrorPolicy.ErrorPolicy_Igno:
			if(out != null)
				out.zero();
			break;
		case ErrorPolicy.ErrorPolicy_Warn:
			DefaultLogger.warn(e.toString());
			if(out != null)
				out.zero();
			break;
		default:
		}
	}
	
	byte readFieldByte(String name, FileDatabase db, int error_flag){
		int old = db.reader.getCurrentPos();
		try {
			Field f = get(name);
			// find the structure definition pertaining to this field
			Structure s = db.dna.get(f.type);

			db.reader.incPtr(f.offset);
//			s.convert(out,db);
			return s.convertByte(db);
		}
		catch (Exception e) {
			return _defaultInitializer(error_flag, e);
		}finally{
			// and recover the previous stream position
			db.reader.setCurrentPos(old);

			if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
				++db.stats().fields_read;
		}
	}
	
	void readField(Pointer out, String name, FileDatabase db, int error_flag){
		int old = db.reader.getCurrentPos();
		try {
			Field f = get(name);
			// find the structure definition pertaining to this field
			Structure s = db.dna.get(f.type);

			db.reader.incPtr(f.offset);
			s.convert(out, db);
		}
		catch (Exception e) {
			out.val = _defaultInitializer(error_flag, e);
		}finally{
			// and recover the previous stream position
			db.reader.setCurrentPos(old);

			if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
				++db.stats().fields_read;
		}
	}
	
	//--------------------------------------------------------------------------------
	boolean resolvePointerFileOffset(ObjectHolder<FileOffset> out, Pointer ptrval, FileDatabase db, Field f, boolean b){
		// Currently used exclusively by PackedFile::data to represent
		// a simple offset into the mapped BLEND file. 
		out.reset();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

//		out =  boost::shared_ptr< FileOffset > (new FileOffset());
		out.set(new FileOffset());
		out.get().val = block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) );
		return false;
	}
	
	//--------------------------------------------------------------------------------
//	template <> bool Structure :: ResolvePointer<boost::shared_ptr,ElemBase>(boost::shared_ptr<ElemBase>& out, 
//		const Pointer & ptrval, 
//		const FileDatabase& db, 
//		const Field&,
//		bool
//	) const 
	boolean resolvePointer(ObjectHolder<ElemBase> out, Pointer ptrval, FileDatabase db, Field f, boolean b)
	{
		// Special case when the data type needs to be determined at runtime.
		// Less secure than in the `strongly-typed` case.

//		out.reset();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set(db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int)(block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s,db);
		if (builders.first == null) {
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
//			out.reset();
			DefaultLogger.warn("Failed to find a converter for the `" + s.name + "` structure");
			return false;
		}

		// allocate the object hull
//		out = (s.*builders.first)();
		out.set(builders.first.call());
		
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
//		db.cache(out).set(s,out,ptrval);
		out.set(db.cache().get(s,ptrval));

		// and do the actual conversion
//		(s.*builders.second)(out,db);
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
		out.get().dna_type = s.name;


		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().pointers_resolved;
		return false;
	}
	
	//--------------------------------------------------------------------------------
	FileBlockHead locateFileBlockForAddress(Pointer ptrval, FileDatabase db) 
	{
		// the file blocks appear in list sorted by
		// with ascending base addresses so we can run a 
		// binary search to locate the pointee quickly.

		// NOTE: Blender seems to distinguish between side-by-side
		// data (stored in the same data block) and far pointers,
		// which are only used for structures starting with an ID.
		// We don't need to make this distinction, our algorithm
		// works regardless where the data is stored.
//		vector<FileBlockHead>::const_iterator it = std::lower_bound(db.entries.begin(),db.entries.end(),ptrval);
		FileBlockHead it = null;
		for(FileBlockHead fbh : db.entries){
			if(fbh.address.val >= ptrval.val){
				it = fbh;
				break;
			}
		}
		
		if (it == null/*db.entries.end()*/) {
			// this is crucial, pointers may not be invalid.
			// this is either a corrupted file or an attempted attack.
			throw new DeadlyImportError("Failure resolving pointer 0x" + Long.toHexString(ptrval.val) +
				/*std::hex,ptrval.val,*/", no file block falls into this address range"
				);
		}
		if (ptrval.val >= it.address.val + it.size) {
			throw new DeadlyImportError("Failure resolving pointer 0x" + Long.toHexString(ptrval.val) +
				/*std::hex,ptrval.val,*/", nearest file block starting at 0x" + 
				Long.toHexString(it.address.val) + " ends at 0x" + 
				Long.toHexString(it.address.val + it.size));
		}
		return it;
	}
	
	int convertInt(FileDatabase db){
		return (int) convertDispatcher(this, db);
	}
	
	// ------------------------------------------------------------------------------------------------
	void convert(Pointer dest, FileDatabase db)
	{
		if (db.i64bit) {
			dest.val = db.reader.getI8();
			//db.reader->IncPtr(-8);
			return;
		}
		dest.val = db.reader.getI4();
		//db.reader->IncPtr(-4);
	}
	
	short convertShort(FileDatabase db){
		// automatic rescaling from short to float and vice versa (seems to be used by normals)
		if (name == "float") {
//			dest = static_cast<short>(db.reader->GetF4() * 32767.f);
//			//db.reader->IncPtr(-4);
//			return;
			return (short) (db.reader.getF4() * 32767.f);
		}
		else if (name == "double") {
//			dest = static_cast<short>(db.reader->GetF8() * 32767.);
//			//db.reader->IncPtr(-8);
//			return;
			return (short) (db.reader.getF8() * 32767.0);
		}
		return (short)convertDispatcher(this,db);
	}
	
	// ------------------------------------------------------------------------------------------------
	float convertFloat  (FileDatabase db)
	{
		// automatic rescaling from char to float and vice versa (seems useful for RGB colors)
		if (name.equals("char")) {
			return db.reader.getI1() / 255.f;
		}
		// automatic rescaling from short to float and vice versa (used by normals)
		else if (name.equals("short")) {
			return db.reader.getI2() / 32767.f;
		}
		return (float)convertDispatcher(this,db);
	}

	// ------------------------------------------------------------------------------------------------
	double convertDouble(FileDatabase db)
	{
		if (name.equals("char")) {
			return db.reader.getI1() / 255.;
		}
		else if (name.equals("short")) {
			return db.reader.getI2() / 32767.;
		}
		return convertDispatcher(this,db);
	}
	
	static double convertDispatcher(Structure in, FileDatabase db){
		if (in.name.equals("int")) {
//			out = static_cast_silent<T>()(db.reader->GetU4());
			return db.reader.getI4();
		}
		else if (in.name.equals("short")) {
//			out = static_cast_silent<T>()(db.reader->GetU2());
			return db.reader.getI2();
		}
		else if (in.name.equals("char")) {
//			out = static_cast_silent<T>()(db.reader->GetU1());
			return db.reader.getI1();
		}
		else if (in.name.equals("float")) {
//			out = static_cast<T>(db.reader->GetF4());
			return db.reader.getF4();
		}
		else if (in.name.equals("double")) {
//			out = static_cast<T>(db.reader->GetF8());
			return db.reader.getF8();
		}
		else {
			throw new DeadlyImportError("Unknown source for conversion to primitive data type: "+in.name);
		}
	}
	
	void convert(Base dest, FileDatabase db){
		// note: as per https://github.com/assimp/assimp/issues/128,
		// reading the Object linked list recursively is prone to stack overflow.
		// This structure converter is therefore an hand-written exception that
		// does it iteratively.
		final int initial_pos = db.reader.getCurrentPos();

		ObjectHolder<BLEObject> holder = new ObjectHolder<BLEObject>();
		ObjectHolder<Base> holderBase = new ObjectHolder<Base>();
//		std::pair<Base*, int> todo = std::make_pair(&dest, initial_pos);
		Base first = dest;
		int second = initial_pos;
		for ( ;; ) {
		
			Base cur_dest = first;
			db.reader.setCurrentPos(second);

			// we know that this is a double-linked, circular list which we never
			// traverse backwards, so don't bother resolving the back links.
			cur_dest.prev = null;
			readFieldPtrBLEObject(holder,"*object",db,false, ErrorPolicy_Warn);
			cur_dest.object = holder.get();

			// the return value of ReadFieldPtr indicates whether the object 
			// was already cached. In this case, we don't need to resolve
			// it again.
			if(!readFieldPtrBase(holderBase,"*next",db, true, ErrorPolicy_Warn) && (cur_dest.next = holderBase.get()) != null) {
//				todo = std::make_pair(&*cur_dest.next, db.reader->GetCurrentPos());
				first = cur_dest.next;
				second = db.reader.getCurrentPos();
				continue;
			}
			break;
		}
		
		db.reader.setCurrentPos(initial_pos + size);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
		Structure other = (Structure) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	void convert(ElemBase dest, FileDatabase db){
		// TODO nothing need to do.
	}
	
	void readField(ID out, String name, FileDatabase db, int error_flag){
		int old = db.reader.getCurrentPos();
		try {
			Field f = get(name);
			// find the structure definition pertaining to this field
			Structure s = db.dna.get(f.type);

			db.reader.incPtr(f.offset);
			s.convert(out,db);
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer(out, error_flag, e);
		}

		// and recover the previous stream position
		db.reader.setCurrentPos(old);

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
	}

	void readFieldArray(float[][] out, String name, FileDatabase db, int error_policy){
		int old = db.reader.getCurrentPos();
		final int m = out.length;
		final int n = out.length;
		float default_value = 0;
		
		try{
			Field f = get(name);
			Structure s = db.dna.get(f.type);
			
			// is the input actually an array?
			if ((f.flags & FieldFlags.FieldFlag_Array) == 0) {
				throw new Error("Field `"+name+"` of structure `"+ name + "` ought to be an array of size " + m + "*" + n);
			}

			db.reader.incPtr(f.offset);

			// size conversions are always allowed, regardless of error_policy
			int i = 0;
			for(; i < Math.min(f.array_sizes[0],m); ++i) {
				int j = 0;
				for(; j < Math.min(f.array_sizes[1],n); ++j) {
					out[i][j] = s.convertFloat(db);
				}
				for(; j < n; ++j) {
//					_defaultInitializer<ErrorPolicy_Igno>()(out[i][j]);
					out[i][j] = default_value;
				}
			}
			for(; i < m; ++i) {
//				_defaultInitializer<ErrorPolicy_Igno>()(out[i]);
				Arrays.fill(out[i], default_value);
			}
		}catch(Exception e){
			_defaultInitializer(error_policy, e);
			for(int i = 0; i < m; i++)
				Arrays.fill(out[i], default_value);
		}
		
		// and recover the previous stream position
		db.reader.setCurrentPos(old);
		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
	}


void readFieldArray(byte[] out, String name, FileDatabase db, int error_policy){
		int old = db.reader.getCurrentPos();
		byte default_value = 0;
		
		try{
			final int length = out.length;
			Field f = get(name);
			Structure s = db.dna.get(f.type);
			
			// is the input actually an array?
			if ((f.flags & FieldFlags.FieldFlag_Array) == 0) {
				throw new Error("Field `"+name+"` of structure `"+ name + "` ought to be an array of size " + length);
			}

			db.reader.incPtr(f.offset);

			// size conversions are always allowed, regardless of error_policy
			int i = 0;
			for(; i < Math.min(f.array_sizes[0],length); ++i) {
				out[i] = s.convertByte(db);
			}
			
			for(; i < length; ++i) {
//				_defaultInitializer<ErrorPolicy_Igno>()(out[i]);
				out[i] = default_value;
			}
		}catch(Exception e){
			Arrays.fill(out, default_value);
		}
		
		// and recover the previous stream position
		db.reader.setCurrentPos(old);
		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
	}


	boolean readFieldPtrBLEObject(ObjectHolder<BLEObject> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerBLEObject(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerBLEObject(ObjectHolder<BLEObject> out, Pointer ptrval, FileDatabase db, Field f, boolean non_recursive)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		Structure s = db.dna.get(f.type);
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// also determine the target type from the block header
		// and check if it matches the type which we expect.
		Structure ss = db.dna.get(block.dna_index);
		if (ss != s) {
			throw new Error("Expected target to be of type `" + s.name +
				"` but seemingly it is a `"+ss.name+"` instead"
				);
		}

		// try to retrieve the object from the cache
		out.set((BLEObject) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
//		int num = block.size / ss.size; 
//		T* o = _allocate(out,num);
		out.reset(new BLEObject());
		BLEObject o = out.get();

		// cache the object before we convert it to avoid cyclic recursion.
		db.cache().set(s,o,ptrval); 

		// if the non_recursive flag is set, we don't do anything but leave
		// the cursor at the correct position to resolve the object.
		if (!non_recursive) {
//			for (int i = 0; i < num; ++i,++o) {
//				s.Convert(*o,db);
//			}
//
//			db.reader->SetCurrentPos(pold);
			
			s.convert(o, db);
			db.reader.setCurrentPos(pold);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			if(out.notNull()) {
				++db.stats().pointers_resolved;
			}
		}
		
		return false;
	}


	boolean readFieldPtrGroup(ObjectHolder<Group> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerGroup(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerGroup(ObjectHolder<Group> out, Pointer ptrval, FileDatabase db, Field f, boolean non_recursive)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		Structure s = db.dna.get(f.type);
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// also determine the target type from the block header
		// and check if it matches the type which we expect.
		Structure ss = db.dna.get(block.dna_index);
		if (ss != s) {
			throw new Error("Expected target to be of type `" + s.name +
				"` but seemingly it is a `"+ss.name+"` instead"
				);
		}

		// try to retrieve the object from the cache
		out.set((Group) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
//		int num = block.size / ss.size; 
//		T* o = _allocate(out,num);
		out.reset(new Group());
		Group o = out.get();

		// cache the object before we convert it to avoid cyclic recursion.
		db.cache().set(s,o,ptrval); 

		// if the non_recursive flag is set, we don't do anything but leave
		// the cursor at the correct position to resolve the object.
		if (!non_recursive) {
//			for (int i = 0; i < num; ++i,++o) {
//				s.Convert(*o,db);
//			}
//
//			db.reader->SetCurrentPos(pold);
			
			s.convert(o, db);
			db.reader.setCurrentPos(pold);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			if(out.notNull()) {
				++db.stats().pointers_resolved;
			}
		}
		
		return false;
	}


	boolean readFieldPtrElemBase(ObjectHolder<ElemBase> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerElemBase(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerElemBase(ObjectHolder<ElemBase> out, Pointer ptrval, FileDatabase db, Field f, boolean non_recursive)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		Structure s = db.dna.get(f.type);
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// also determine the target type from the block header
		// and check if it matches the type which we expect.
		Structure ss = db.dna.get(block.dna_index);
		if (ss != s) {
			throw new Error("Expected target to be of type `" + s.name +
				"` but seemingly it is a `"+ss.name+"` instead"
				);
		}

		// try to retrieve the object from the cache
		out.set(db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
//		int num = block.size / ss.size; 
//		T* o = _allocate(out,num);
		out.reset(new ElemBase());
		ElemBase o = out.get();

		// cache the object before we convert it to avoid cyclic recursion.
		db.cache().set(s,o,ptrval); 

		// if the non_recursive flag is set, we don't do anything but leave
		// the cursor at the correct position to resolve the object.
		if (!non_recursive) {
//			for (int i = 0; i < num; ++i,++o) {
//				s.Convert(*o,db);
//			}
//
//			db.reader->SetCurrentPos(pold);
			
			s.convert(o, db);
			db.reader.setCurrentPos(pold);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			if(out.notNull()) {
				++db.stats().pointers_resolved;
			}
		}
		
		return false;
	}


void readField(ListBase out, String name, FileDatabase db, int error_flag){
		int old = db.reader.getCurrentPos();
		try {
			Field f = get(name);
			// find the structure definition pertaining to this field
			Structure s = db.dna.get(f.type);

			db.reader.incPtr(f.offset);
			s.convert(out,db);
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer(out, error_flag, e);
		}

		// and recover the previous stream position
		db.reader.setCurrentPos(old);

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
	}


	void convert(BLEObject dest, FileDatabase db){
		ObjectHolder<BLEObject> holderBLEObject = new ObjectHolder<BLEObject>();
ObjectHolder<Group> holderGroup = new ObjectHolder<Group>();
ObjectHolder<ElemBase> holderElemBase = new ObjectHolder<ElemBase>();
		readField(dest.id,"id",db, ErrorPolicy_Fail);
		dest.type = readFieldInt("type",db,ErrorPolicy_Fail);
		readFieldArray(dest.obmat,"obmat",db, ErrorPolicy_Warn);
		readFieldArray(dest.parentinv,"parentinv",db, ErrorPolicy_Warn);
		readFieldArray(dest.parsubstr,"parsubstr",db, ErrorPolicy_Warn);
		readFieldPtrBLEObject(holderBLEObject,"*parent",db, false, ErrorPolicy_Warn);
		dest.parent = holderBLEObject.get();
		readFieldPtrBLEObject(holderBLEObject,"*track",db, false, ErrorPolicy_Warn);
		dest.track = holderBLEObject.get();
		readFieldPtrBLEObject(holderBLEObject,"*proxy",db, false, ErrorPolicy_Warn);
		dest.proxy = holderBLEObject.get();
		readFieldPtrBLEObject(holderBLEObject,"*proxy_from",db, false, ErrorPolicy_Warn);
		dest.proxy_from = holderBLEObject.get();
		readFieldPtrBLEObject(holderBLEObject,"*proxy_group",db, false, ErrorPolicy_Warn);
		dest.proxy_group = holderBLEObject.get();
		readFieldPtrGroup(holderGroup,"*dup_group",db, false, ErrorPolicy_Warn);
		dest.dup_group = holderGroup.get();
		readFieldPtrElemBase(holderElemBase,"*data",db, false, ErrorPolicy_Fail);
		dest.data = holderElemBase.get();
		readField(dest.modifiers,"modifiers",db, ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	boolean readFieldPtrGroupObject(ObjectHolder<GroupObject> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerGroupObject(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerGroupObject(ObjectHolder<GroupObject> out, Pointer ptrval, FileDatabase db, Field f, boolean non_recursive)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		Structure s = db.dna.get(f.type);
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// also determine the target type from the block header
		// and check if it matches the type which we expect.
		Structure ss = db.dna.get(block.dna_index);
		if (ss != s) {
			throw new Error("Expected target to be of type `" + s.name +
				"` but seemingly it is a `"+ss.name+"` instead"
				);
		}

		// try to retrieve the object from the cache
		out.set((GroupObject) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
//		int num = block.size / ss.size; 
//		T* o = _allocate(out,num);
		out.reset(new GroupObject());
		GroupObject o = out.get();

		// cache the object before we convert it to avoid cyclic recursion.
		db.cache().set(s,o,ptrval); 

		// if the non_recursive flag is set, we don't do anything but leave
		// the cursor at the correct position to resolve the object.
		if (!non_recursive) {
//			for (int i = 0; i < num; ++i,++o) {
//				s.Convert(*o,db);
//			}
//
//			db.reader->SetCurrentPos(pold);
			
			s.convert(o, db);
			db.reader.setCurrentPos(pold);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			if(out.notNull()) {
				++db.stats().pointers_resolved;
			}
		}
		
		return false;
	}


	void convert(Group dest, FileDatabase db){
		ObjectHolder<GroupObject> holderGroupObject = new ObjectHolder<GroupObject>();
		readField(dest.id,"id",db, ErrorPolicy_Fail);
		dest.layer = readFieldInt("layer",db,ErrorPolicy_Igno);
		readFieldPtrGroupObject(holderGroupObject,"*gobject",db, false, ErrorPolicy_Igno);
		dest.gobject = holderGroupObject.get();

		db.reader.incPtr(size);

	}
	boolean readFieldPtrTex(ObjectHolder<Tex> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerTex(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerTex(ObjectHolder<Tex> out, Pointer ptrval, FileDatabase db, Field f, boolean non_recursive)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		Structure s = db.dna.get(f.type);
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// also determine the target type from the block header
		// and check if it matches the type which we expect.
		Structure ss = db.dna.get(block.dna_index);
		if (ss != s) {
			throw new Error("Expected target to be of type `" + s.name +
				"` but seemingly it is a `"+ss.name+"` instead"
				);
		}

		// try to retrieve the object from the cache
		out.set((Tex) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
//		int num = block.size / ss.size; 
//		T* o = _allocate(out,num);
		out.reset(new Tex());
		Tex o = out.get();

		// cache the object before we convert it to avoid cyclic recursion.
		db.cache().set(s,o,ptrval); 

		// if the non_recursive flag is set, we don't do anything but leave
		// the cursor at the correct position to resolve the object.
		if (!non_recursive) {
//			for (int i = 0; i < num; ++i,++o) {
//				s.Convert(*o,db);
//			}
//
//			db.reader->SetCurrentPos(pold);
			
			s.convert(o, db);
			db.reader.setCurrentPos(pold);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			if(out.notNull()) {
				++db.stats().pointers_resolved;
			}
		}
		
		return false;
	}


void readFieldArray(float[] out, String name, FileDatabase db, int error_policy){
		int old = db.reader.getCurrentPos();
		float default_value = 0;
		
		try{
			final int length = out.length;
			Field f = get(name);
			Structure s = db.dna.get(f.type);
			
			// is the input actually an array?
			if ((f.flags & FieldFlags.FieldFlag_Array) == 0) {
				throw new Error("Field `"+name+"` of structure `"+ name + "` ought to be an array of size " + length);
			}

			db.reader.incPtr(f.offset);

			// size conversions are always allowed, regardless of error_policy
			int i = 0;
			for(; i < Math.min(f.array_sizes[0],length); ++i) {
				out[i] = s.convertFloat(db);
			}
			
			for(; i < length; ++i) {
//				_defaultInitializer<ErrorPolicy_Igno>()(out[i]);
				out[i] = default_value;
			}
		}catch(Exception e){
			Arrays.fill(out, default_value);
		}
		
		// and recover the previous stream position
		db.reader.setCurrentPos(old);
		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
	}


	void convert(MTex dest, FileDatabase db){
		ObjectHolder<BLEObject> holderBLEObject = new ObjectHolder<BLEObject>();
ObjectHolder<Tex> holderTex = new ObjectHolder<Tex>();
		dest.mapto = readFieldShort("mapto",db,ErrorPolicy_Igno);
		dest.blendtype = readFieldInt("blendtype",db,ErrorPolicy_Igno);
		readFieldPtrBLEObject(holderBLEObject,"*object",db, false, ErrorPolicy_Igno);
		dest.object = holderBLEObject.get();
		readFieldPtrTex(holderTex,"*tex",db, false, ErrorPolicy_Igno);
		dest.tex = holderTex.get();
		readFieldArray(dest.uvname,"uvname",db, ErrorPolicy_Igno);
		dest.projx = readFieldInt("projx",db,ErrorPolicy_Igno);
		dest.projy = readFieldInt("projy",db,ErrorPolicy_Igno);
		dest.projz = readFieldInt("projz",db,ErrorPolicy_Igno);
		dest.mapping = readFieldByte("mapping",db,ErrorPolicy_Igno);
		readFieldArray(dest.ofs,"ofs",db, ErrorPolicy_Igno);
		readFieldArray(dest.size,"size",db, ErrorPolicy_Igno);
		dest.rot = readFieldFloat("rot",db,ErrorPolicy_Igno);
		dest.texflag = readFieldInt("texflag",db,ErrorPolicy_Igno);
		dest.colormodel = readFieldShort("colormodel",db,ErrorPolicy_Igno);
		dest.pmapto = readFieldShort("pmapto",db,ErrorPolicy_Igno);
		dest.pmaptoneg = readFieldShort("pmaptoneg",db,ErrorPolicy_Igno);
		dest.r = readFieldFloat("r",db,ErrorPolicy_Warn);
		dest.g = readFieldFloat("g",db,ErrorPolicy_Warn);
		dest.b = readFieldFloat("b",db,ErrorPolicy_Warn);
		dest.k = readFieldFloat("k",db,ErrorPolicy_Warn);
		dest.colspecfac = readFieldFloat("colspecfac",db,ErrorPolicy_Igno);
		dest.mirrfac = readFieldFloat("mirrfac",db,ErrorPolicy_Igno);
		dest.alphafac = readFieldFloat("alphafac",db,ErrorPolicy_Igno);
		dest.difffac = readFieldFloat("difffac",db,ErrorPolicy_Igno);
		dest.specfac = readFieldFloat("specfac",db,ErrorPolicy_Igno);
		dest.emitfac = readFieldFloat("emitfac",db,ErrorPolicy_Igno);
		dest.hardfac = readFieldFloat("hardfac",db,ErrorPolicy_Igno);
		dest.norfac = readFieldFloat("norfac",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
void readFieldArray(int[] out, String name, FileDatabase db, int error_policy){
		int old = db.reader.getCurrentPos();
		int default_value = 0;
		
		try{
			final int length = out.length;
			Field f = get(name);
			Structure s = db.dna.get(f.type);
			
			// is the input actually an array?
			if ((f.flags & FieldFlags.FieldFlag_Array) == 0) {
				throw new Error("Field `"+name+"` of structure `"+ name + "` ought to be an array of size " + length);
			}

			db.reader.incPtr(f.offset);

			// size conversions are always allowed, regardless of error_policy
			int i = 0;
			for(; i < Math.min(f.array_sizes[0],length); ++i) {
				out[i] = s.convertInt(db);
			}
			
			for(; i < length; ++i) {
//				_defaultInitializer<ErrorPolicy_Igno>()(out[i]);
				out[i] = default_value;
			}
		}catch(Exception e){
			Arrays.fill(out, default_value);
		}
		
		// and recover the previous stream position
		db.reader.setCurrentPos(old);
		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
	}


	void convert(TFace dest, FileDatabase db){
				readFieldArray(dest.uv,"uv",db, ErrorPolicy_Fail);
		readFieldArray(dest.col,"col",db, ErrorPolicy_Fail);
		dest.flag = readFieldByte("flag",db,ErrorPolicy_Igno);
		dest.mode = readFieldShort("mode",db,ErrorPolicy_Igno);
		dest.tile = readFieldShort("tile",db,ErrorPolicy_Igno);
		dest.unwrap = readFieldShort("unwrap",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
void readField(ModifierData out, String name, FileDatabase db, int error_flag){
		int old = db.reader.getCurrentPos();
		try {
			Field f = get(name);
			// find the structure definition pertaining to this field
			Structure s = db.dna.get(f.type);

			db.reader.incPtr(f.offset);
			s.convert(out,db);
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer(out, error_flag, e);
		}

		// and recover the previous stream position
		db.reader.setCurrentPos(old);

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
	}


	void convert(SubsurfModifierData dest, FileDatabase db){
				readField(dest.modifier,"modifier",db, ErrorPolicy_Fail);
		dest.subdivType = readFieldShort("subdivType",db,ErrorPolicy_Warn);
		dest.levels = readFieldShort("levels",db,ErrorPolicy_Fail);
		dest.renderLevels = readFieldShort("renderLevels",db,ErrorPolicy_Igno);
		dest.flags = readFieldShort("flags",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(MFace dest, FileDatabase db){
				dest.v1 = readFieldInt("v1",db,ErrorPolicy_Fail);
		dest.v2 = readFieldInt("v2",db,ErrorPolicy_Fail);
		dest.v3 = readFieldInt("v3",db,ErrorPolicy_Fail);
		dest.v4 = readFieldInt("v4",db,ErrorPolicy_Fail);
		dest.mat_nr = readFieldInt("mat_nr",db,ErrorPolicy_Fail);
		dest.flag = readFieldByte("flag",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(Lamp dest, FileDatabase db){
				readField(dest.id,"id",db, ErrorPolicy_Fail);
		dest.type = readFieldInt("type",db,ErrorPolicy_Fail);
		dest.flags = readFieldShort("flags",db,ErrorPolicy_Igno);
		dest.colormodel = readFieldShort("colormodel",db,ErrorPolicy_Igno);
		dest.totex = readFieldShort("totex",db,ErrorPolicy_Igno);
		dest.r = readFieldFloat("r",db,ErrorPolicy_Warn);
		dest.g = readFieldFloat("g",db,ErrorPolicy_Warn);
		dest.b = readFieldFloat("b",db,ErrorPolicy_Warn);
		dest.k = readFieldFloat("k",db,ErrorPolicy_Warn);
		dest.energy = readFieldFloat("energy",db,ErrorPolicy_Igno);
		dest.dist = readFieldFloat("dist",db,ErrorPolicy_Igno);
		dest.spotsize = readFieldFloat("spotsize",db,ErrorPolicy_Igno);
		dest.spotblend = readFieldFloat("spotblend",db,ErrorPolicy_Igno);
		dest.att1 = readFieldFloat("att1",db,ErrorPolicy_Igno);
		dest.att2 = readFieldFloat("att2",db,ErrorPolicy_Igno);
		dest.falloff_type = readFieldInt("falloff_type",db,ErrorPolicy_Igno);
		dest.sun_brightness = readFieldFloat("sun_brightness",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(MDeformWeight dest, FileDatabase db){
				dest.def_nr = readFieldInt("def_nr",db,ErrorPolicy_Fail);
		dest.weight = readFieldFloat("weight",db,ErrorPolicy_Fail);

		db.reader.incPtr(size);

	}
	boolean readFieldPtrFileOffset(ObjectHolder<FileOffset> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerFileOffset(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	void convert(PackedFile dest, FileDatabase db){
		ObjectHolder<FileOffset> holderFileOffset = new ObjectHolder<FileOffset>();
		dest.size = readFieldInt("size",db,ErrorPolicy_Warn);
		dest.seek = readFieldInt("seek",db,ErrorPolicy_Warn);
		readFieldPtrFileOffset(holderFileOffset,"*data",db, false, ErrorPolicy_Warn);
		dest.data = holderFileOffset.get();

		db.reader.incPtr(size);

	}
	void convert(MTFace dest, FileDatabase db){
				readFieldArray(dest.uv,"uv",db, ErrorPolicy_Fail);
		dest.flag = readFieldByte("flag",db,ErrorPolicy_Igno);
		dest.mode = readFieldShort("mode",db,ErrorPolicy_Igno);
		dest.tile = readFieldShort("tile",db,ErrorPolicy_Igno);
		dest.unwrap = readFieldShort("unwrap",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	boolean readFieldPtrMTex(MTex[] out, String name, FileDatabase db,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer[] ptrval = new Pointer[out.length];
		AssUtil.initArray(ptrval);
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((FieldFlag_Pointer|FieldFlag_Pointer) != (f.flags & (FieldFlag_Pointer|FieldFlag_Pointer))) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer AND an array");
			}

			db.reader.incPtr(f.offset);
			int i = 0;
			for(; i < Math.min(f.array_sizes[0], ptrval.length); i++)
				convert(ptrval[i],db);
			
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			Arrays.fill(out, null);
			return false;
		}
		
		ObjectHolder<MTex> holder = new ObjectHolder<MTex>();
		boolean res = true;
		for(int i = 0; i < out.length; ++i) {
			// resolve the pointer and load the corresponding structure
			res = resolvePointerMTex(holder,ptrval[i],db) && res;
			out[i] = holder.get();
		}

		// and recover the previous stream position
		db.reader.setCurrentPos(old);

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMTex(ObjectHolder<MTex> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MTex) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MTex)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	void convert(BLEMaterial dest, FileDatabase db){
		ObjectHolder<Group> holderGroup = new ObjectHolder<Group>();
		readField(dest.id,"id",db, ErrorPolicy_Fail);
		dest.r = readFieldFloat("r",db,ErrorPolicy_Warn);
		dest.g = readFieldFloat("g",db,ErrorPolicy_Warn);
		dest.b = readFieldFloat("b",db,ErrorPolicy_Warn);
		dest.specr = readFieldFloat("specr",db,ErrorPolicy_Warn);
		dest.specg = readFieldFloat("specg",db,ErrorPolicy_Warn);
		dest.specb = readFieldFloat("specb",db,ErrorPolicy_Warn);
		dest.har = readFieldShort("har",db,ErrorPolicy_Igno);
		dest.ambr = readFieldFloat("ambr",db,ErrorPolicy_Warn);
		dest.ambg = readFieldFloat("ambg",db,ErrorPolicy_Warn);
		dest.ambb = readFieldFloat("ambb",db,ErrorPolicy_Warn);
		dest.mirr = readFieldFloat("mirr",db,ErrorPolicy_Igno);
		dest.mirg = readFieldFloat("mirg",db,ErrorPolicy_Igno);
		dest.mirb = readFieldFloat("mirb",db,ErrorPolicy_Igno);
		dest.emit = readFieldFloat("emit",db,ErrorPolicy_Warn);
		dest.alpha = readFieldFloat("alpha",db,ErrorPolicy_Warn);
		dest.ref = readFieldFloat("ref",db,ErrorPolicy_Igno);
		dest.translucency = readFieldFloat("translucency",db,ErrorPolicy_Igno);
		dest.roughness = readFieldFloat("roughness",db,ErrorPolicy_Igno);
		dest.darkness = readFieldFloat("darkness",db,ErrorPolicy_Igno);
		dest.refrac = readFieldFloat("refrac",db,ErrorPolicy_Igno);
		readFieldPtrGroup(holderGroup,"*group",db, false, ErrorPolicy_Igno);
		dest.group = holderGroup.get();
		dest.diff_shader = readFieldShort("diff_shader",db,ErrorPolicy_Warn);
		dest.spec_shader = readFieldShort("spec_shader",db,ErrorPolicy_Warn);
		readFieldPtrMTex(dest.mtex,"*mtex",db, ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	boolean readFieldPtrBLEImage(ObjectHolder<BLEImage> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerBLEImage(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerBLEImage(ObjectHolder<BLEImage> out, Pointer ptrval, FileDatabase db, Field f, boolean non_recursive)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		Structure s = db.dna.get(f.type);
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// also determine the target type from the block header
		// and check if it matches the type which we expect.
		Structure ss = db.dna.get(block.dna_index);
		if (ss != s) {
			throw new Error("Expected target to be of type `" + s.name +
				"` but seemingly it is a `"+ss.name+"` instead"
				);
		}

		// try to retrieve the object from the cache
		out.set((BLEImage) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
//		int num = block.size / ss.size; 
//		T* o = _allocate(out,num);
		out.reset(new BLEImage());
		BLEImage o = out.get();

		// cache the object before we convert it to avoid cyclic recursion.
		db.cache().set(s,o,ptrval); 

		// if the non_recursive flag is set, we don't do anything but leave
		// the cursor at the correct position to resolve the object.
		if (!non_recursive) {
//			for (int i = 0; i < num; ++i,++o) {
//				s.Convert(*o,db);
//			}
//
//			db.reader->SetCurrentPos(pold);
			
			s.convert(o, db);
			db.reader.setCurrentPos(pold);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			if(out.notNull()) {
				++db.stats().pointers_resolved;
			}
		}
		
		return false;
	}


	void convert(MTexPoly dest, FileDatabase db){
		ObjectHolder<BLEImage> holderBLEImage = new ObjectHolder<BLEImage>();
		readFieldPtrBLEImage(holderBLEImage,"*tpage",db, false, ErrorPolicy_Igno);
		dest.tpage = holderBLEImage.get();
		dest.flag = readFieldByte("flag",db,ErrorPolicy_Igno);
		dest.transp = readFieldByte("transp",db,ErrorPolicy_Igno);
		dest.mode = readFieldShort("mode",db,ErrorPolicy_Igno);
		dest.tile = readFieldShort("tile",db,ErrorPolicy_Igno);
		dest.pad = readFieldShort("pad",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	boolean readFieldPtrMFace(ArrayList<MFace> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMFace(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMFace(ArrayList<MFace> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MFace> holder = new ObjectHolder<MFace>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMFaceList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMFaceList(ObjectHolder<MFace> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MFace) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MFace)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrMTFace(ArrayList<MTFace> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMTFace(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMTFace(ArrayList<MTFace> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MTFace> holder = new ObjectHolder<MTFace>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMTFaceList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMTFaceList(ObjectHolder<MTFace> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MTFace) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MTFace)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrTFace(ArrayList<TFace> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerTFace(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerTFace(ArrayList<TFace> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<TFace> holder = new ObjectHolder<TFace>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerTFaceList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerTFaceList(ObjectHolder<TFace> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((TFace) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((TFace)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrMVert(ArrayList<MVert> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMVert(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMVert(ArrayList<MVert> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MVert> holder = new ObjectHolder<MVert>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMVertList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMVertList(ObjectHolder<MVert> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MVert) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MVert)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrMEdge(ArrayList<MEdge> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMEdge(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMEdge(ArrayList<MEdge> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MEdge> holder = new ObjectHolder<MEdge>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMEdgeList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMEdgeList(ObjectHolder<MEdge> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MEdge) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MEdge)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrMLoop(ArrayList<MLoop> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMLoop(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMLoop(ArrayList<MLoop> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MLoop> holder = new ObjectHolder<MLoop>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMLoopList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMLoopList(ObjectHolder<MLoop> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MLoop) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MLoop)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrMLoopUV(ArrayList<MLoopUV> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMLoopUV(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMLoopUV(ArrayList<MLoopUV> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MLoopUV> holder = new ObjectHolder<MLoopUV>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMLoopUVList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMLoopUVList(ObjectHolder<MLoopUV> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MLoopUV) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MLoopUV)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrMLoopCol(ArrayList<MLoopCol> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMLoopCol(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMLoopCol(ArrayList<MLoopCol> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MLoopCol> holder = new ObjectHolder<MLoopCol>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMLoopColList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMLoopColList(ObjectHolder<MLoopCol> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MLoopCol) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MLoopCol)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrMPoly(ArrayList<MPoly> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMPoly(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMPoly(ArrayList<MPoly> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MPoly> holder = new ObjectHolder<MPoly>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMPolyList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMPolyList(ObjectHolder<MPoly> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MPoly) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MPoly)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrMTexPoly(ArrayList<MTexPoly> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMTexPoly(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMTexPoly(ArrayList<MTexPoly> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MTexPoly> holder = new ObjectHolder<MTexPoly>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMTexPolyList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMTexPolyList(ObjectHolder<MTexPoly> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MTexPoly) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MTexPoly)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrMDeformVert(ArrayList<MDeformVert> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMDeformVert(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMDeformVert(ArrayList<MDeformVert> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MDeformVert> holder = new ObjectHolder<MDeformVert>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMDeformVertList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMDeformVertList(ObjectHolder<MDeformVert> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MDeformVert) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MDeformVert)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrMCol(ArrayList<MCol> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMCol(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMCol(ArrayList<MCol> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MCol> holder = new ObjectHolder<MCol>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMColList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMColList(ObjectHolder<MCol> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MCol) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MCol)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	boolean readFieldPtrBLEMaterial(ArrayList<BLEMaterial> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerBLEMaterial(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerBLEMaterial(ArrayList<BLEMaterial> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<BLEMaterial> holder = new ObjectHolder<BLEMaterial>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerBLEMaterialList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerBLEMaterialList(ObjectHolder<BLEMaterial> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((BLEMaterial) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((BLEMaterial)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	void convert(BLEMesh dest, FileDatabase db){
				readField(dest.id,"id",db, ErrorPolicy_Fail);
		dest.totface = readFieldInt("totface",db,ErrorPolicy_Fail);
		dest.totedge = readFieldInt("totedge",db,ErrorPolicy_Fail);
		dest.totvert = readFieldInt("totvert",db,ErrorPolicy_Fail);
		dest.totloop = readFieldInt("totloop",db,ErrorPolicy_Igno);
		dest.totpoly = readFieldInt("totpoly",db,ErrorPolicy_Igno);
		dest.subdiv = readFieldShort("subdiv",db,ErrorPolicy_Igno);
		dest.subdivr = readFieldShort("subdivr",db,ErrorPolicy_Igno);
		dest.subsurftype = readFieldShort("subsurftype",db,ErrorPolicy_Igno);
		dest.smoothresh = readFieldShort("smoothresh",db,ErrorPolicy_Igno);
		readFieldPtrMFace(dest.mface,"*mface",db, false, ErrorPolicy_Fail);
		readFieldPtrMTFace(dest.mtface,"*mtface",db, false, ErrorPolicy_Igno);
		readFieldPtrTFace(dest.tface,"*tface",db, false, ErrorPolicy_Igno);
		readFieldPtrMVert(dest.mvert,"*mvert",db, false, ErrorPolicy_Fail);
		readFieldPtrMEdge(dest.medge,"*medge",db, false, ErrorPolicy_Warn);
		readFieldPtrMLoop(dest.mloop,"*mloop",db, false, ErrorPolicy_Igno);
		readFieldPtrMLoopUV(dest.mloopuv,"*mloopuv",db, false, ErrorPolicy_Igno);
		readFieldPtrMLoopCol(dest.mloopcol,"*mloopcol",db, false, ErrorPolicy_Igno);
		readFieldPtrMPoly(dest.mpoly,"*mpoly",db, false, ErrorPolicy_Igno);
		readFieldPtrMTexPoly(dest.mtpoly,"*mtpoly",db, false, ErrorPolicy_Igno);
		readFieldPtrMDeformVert(dest.dvert,"*dvert",db, false, ErrorPolicy_Igno);
		readFieldPtrMCol(dest.mcol,"*mcol",db, false, ErrorPolicy_Igno);
		readFieldPtrBLEMaterial(dest.mat,"**mat",db, false, ErrorPolicy_Fail);

		db.reader.incPtr(size);

	}
	boolean readFieldPtrMDeformWeight(ArrayList<MDeformWeight> out, String name, FileDatabase db, boolean non_recursive, int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.clear();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerMDeformWeight(out,ptrval,db);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerMDeformWeight(ArrayList<MDeformWeight> out, Pointer ptrval, FileDatabase db){
		// This is a function overload, not a template specialization. According to
		// the partial ordering rules, it should be selected by the compiler
		// for array-of-pointer inputs, i.e. Object::mats.
		out.clear();
		if (ptrval.val == 0) { 
			return false;
		}

		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		int num = block.size / (db.i64bit?8:4); 

		// keep the old stream position
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ ptrval.val - block.address.val));

		boolean res = false;
		// allocate raw storage for the array
		out.ensureCapacity(num);
		Pointer val = new Pointer();
		ObjectHolder<MDeformWeight> holder = new ObjectHolder<MDeformWeight>();
		for (int i = 0; i< num; ++i) {
			convert(val,db);
			// and resolve the pointees
			res = resolvePointerMDeformWeightList(holder/*out[i]*/,val,db) && res;
			out.add(holder.get());
		}

		db.reader.setCurrentPos(pold);
		return res;
	}
	
	boolean resolvePointerMDeformWeightList(ObjectHolder<MDeformWeight> out, Pointer ptrval, FileDatabase db)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);
		
		// determine the target type from the block header
		Structure s = db.dna.get(block.dna_index);

		// try to retrieve the object from the cache
		out.set((MDeformWeight) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
		FactoryPair builders = db.dna.getBlobToStructureConverter(s, db);
		if(builders.first == null){
			// this might happen if DNA::RegisterConverters hasn't been called so far
			// or if the target type is not contained in `our` DNA.
			out.reset();
			DefaultLogger.warn(AssUtil.makeString(
				"Failed to find a converter for the `",s.name,"` structure"
				));
			return false;
		}
		
		// allocate the object hull
		out.set((MDeformWeight)builders.first.call());
		// cache the object immediately to prevent infinite recursion in a 
		// circular list with a single element (i.e. a self-referencing element).
		db.cache().set(s, out.get(), ptrval);
		
		// and do the actual conversion
		builders.second.call(s, out.get(), db);
		db.reader.setCurrentPos(pold);
		
		// store a pointer to the name string of the actual type
		// in the object itself. This allows the conversion code
		// to perform additional type checking.
//		out->dna_type = s.name.c_str();
		out.get().dna_type = s.name;

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			++db.stats().pointers_resolved;
		}
		
		return false;
	}


	void convert(MDeformVert dest, FileDatabase db){
				readFieldPtrMDeformWeight(dest.dw,"*dw",db, false, ErrorPolicy_Warn);
		dest.totweight = readFieldInt("totweight",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(World dest, FileDatabase db){
				readField(dest.id,"id",db, ErrorPolicy_Fail);

		db.reader.incPtr(size);

	}
	void convert(MLoopCol dest, FileDatabase db){
				dest.r = readFieldByte("r",db,ErrorPolicy_Igno);
		dest.g = readFieldByte("g",db,ErrorPolicy_Igno);
		dest.b = readFieldByte("b",db,ErrorPolicy_Igno);
		dest.a = readFieldByte("a",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(MVert dest, FileDatabase db){
				readFieldArray(dest.co,"co",db, ErrorPolicy_Fail);
		readFieldArray(dest.no,"no",db, ErrorPolicy_Fail);
		dest.flag = readFieldByte("flag",db,ErrorPolicy_Igno);
		dest.mat_nr = readFieldInt("mat_nr",db,ErrorPolicy_Warn);
		dest.bweight = readFieldInt("bweight",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(MEdge dest, FileDatabase db){
				dest.v1 = readFieldInt("v1",db,ErrorPolicy_Fail);
		dest.v2 = readFieldInt("v2",db,ErrorPolicy_Fail);
		dest.crease = readFieldByte("crease",db,ErrorPolicy_Igno);
		dest.bweight = readFieldByte("bweight",db,ErrorPolicy_Igno);
		dest.flag = readFieldShort("flag",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(MLoopUV dest, FileDatabase db){
				readFieldArray(dest.uv,"uv",db, ErrorPolicy_Igno);
		dest.flag = readFieldInt("flag",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(GroupObject dest, FileDatabase db){
		ObjectHolder<GroupObject> holderGroupObject = new ObjectHolder<GroupObject>();
ObjectHolder<BLEObject> holderBLEObject = new ObjectHolder<BLEObject>();
		readFieldPtrGroupObject(holderGroupObject,"*prev",db, false, ErrorPolicy_Fail);
		dest.prev = holderGroupObject.get();
		readFieldPtrGroupObject(holderGroupObject,"*next",db, false, ErrorPolicy_Fail);
		dest.next = holderGroupObject.get();
		readFieldPtrBLEObject(holderBLEObject,"*ob",db, false, ErrorPolicy_Igno);
		dest.ob = holderBLEObject.get();

		db.reader.incPtr(size);

	}
	void convert(ListBase dest, FileDatabase db){
		ObjectHolder<ElemBase> holderElemBase = new ObjectHolder<ElemBase>();
		readFieldPtrElemBase(holderElemBase,"*first",db, false, ErrorPolicy_Igno);
		dest.first = holderElemBase.get();
		readFieldPtrElemBase(holderElemBase,"*last",db, false, ErrorPolicy_Igno);
		dest.last = holderElemBase.get();

		db.reader.incPtr(size);

	}
	void convert(MLoop dest, FileDatabase db){
				dest.v = readFieldInt("v",db,ErrorPolicy_Igno);
		dest.e = readFieldInt("e",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(ModifierData dest, FileDatabase db){
		ObjectHolder<ElemBase> holderElemBase = new ObjectHolder<ElemBase>();
		readFieldPtrElemBase(holderElemBase,"*next",db, false, ErrorPolicy_Warn);
		dest.next = holderElemBase.get();
		readFieldPtrElemBase(holderElemBase,"*prev",db, false, ErrorPolicy_Warn);
		dest.prev = holderElemBase.get();
		dest.type = readFieldInt("type",db,ErrorPolicy_Igno);
		dest.mode = readFieldInt("mode",db,ErrorPolicy_Igno);
		readFieldArray(dest.name,"name",db, ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(ID dest, FileDatabase db){
				readFieldArray(dest.name,"name",db, ErrorPolicy_Warn);
		dest.flag = readFieldShort("flag",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	void convert(MCol dest, FileDatabase db){
				dest.r = readFieldByte("r",db,ErrorPolicy_Fail);
		dest.g = readFieldByte("g",db,ErrorPolicy_Fail);
		dest.b = readFieldByte("b",db,ErrorPolicy_Fail);
		dest.a = readFieldByte("a",db,ErrorPolicy_Fail);

		db.reader.incPtr(size);

	}
	void convert(MPoly dest, FileDatabase db){
				dest.loopstart = readFieldInt("loopstart",db,ErrorPolicy_Igno);
		dest.totloop = readFieldInt("totloop",db,ErrorPolicy_Igno);
		dest.mat_nr = readFieldShort("mat_nr",db,ErrorPolicy_Igno);
		dest.flag = readFieldByte("flag",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	boolean readFieldPtrWorld(ObjectHolder<World> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerWorld(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerWorld(ObjectHolder<World> out, Pointer ptrval, FileDatabase db, Field f, boolean non_recursive)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		Structure s = db.dna.get(f.type);
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// also determine the target type from the block header
		// and check if it matches the type which we expect.
		Structure ss = db.dna.get(block.dna_index);
		if (ss != s) {
			throw new Error("Expected target to be of type `" + s.name +
				"` but seemingly it is a `"+ss.name+"` instead"
				);
		}

		// try to retrieve the object from the cache
		out.set((World) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
//		int num = block.size / ss.size; 
//		T* o = _allocate(out,num);
		out.reset(new World());
		World o = out.get();

		// cache the object before we convert it to avoid cyclic recursion.
		db.cache().set(s,o,ptrval); 

		// if the non_recursive flag is set, we don't do anything but leave
		// the cursor at the correct position to resolve the object.
		if (!non_recursive) {
//			for (int i = 0; i < num; ++i,++o) {
//				s.Convert(*o,db);
//			}
//
//			db.reader->SetCurrentPos(pold);
			
			s.convert(o, db);
			db.reader.setCurrentPos(pold);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			if(out.notNull()) {
				++db.stats().pointers_resolved;
			}
		}
		
		return false;
	}


	boolean readFieldPtrBase(ObjectHolder<Base> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerBase(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerBase(ObjectHolder<Base> out, Pointer ptrval, FileDatabase db, Field f, boolean non_recursive)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		Structure s = db.dna.get(f.type);
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// also determine the target type from the block header
		// and check if it matches the type which we expect.
		Structure ss = db.dna.get(block.dna_index);
		if (ss != s) {
			throw new Error("Expected target to be of type `" + s.name +
				"` but seemingly it is a `"+ss.name+"` instead"
				);
		}

		// try to retrieve the object from the cache
		out.set((Base) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
//		int num = block.size / ss.size; 
//		T* o = _allocate(out,num);
		out.reset(new Base());
		Base o = out.get();

		// cache the object before we convert it to avoid cyclic recursion.
		db.cache().set(s,o,ptrval); 

		// if the non_recursive flag is set, we don't do anything but leave
		// the cursor at the correct position to resolve the object.
		if (!non_recursive) {
//			for (int i = 0; i < num; ++i,++o) {
//				s.Convert(*o,db);
//			}
//
//			db.reader->SetCurrentPos(pold);
			
			s.convert(o, db);
			db.reader.setCurrentPos(pold);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			if(out.notNull()) {
				++db.stats().pointers_resolved;
			}
		}
		
		return false;
	}


	void convert(BLEScene dest, FileDatabase db){
		ObjectHolder<BLEObject> holderBLEObject = new ObjectHolder<BLEObject>();
ObjectHolder<World> holderWorld = new ObjectHolder<World>();
ObjectHolder<Base> holderBase = new ObjectHolder<Base>();
		readField(dest.id,"id",db, ErrorPolicy_Fail);
		readFieldPtrBLEObject(holderBLEObject,"*camera",db, false, ErrorPolicy_Warn);
		dest.camera = holderBLEObject.get();
		readFieldPtrWorld(holderWorld,"*world",db, false, ErrorPolicy_Warn);
		dest.world = holderWorld.get();
		readFieldPtrBase(holderBase,"*basact",db, false, ErrorPolicy_Warn);
		dest.basact = holderBase.get();
		readField(dest.base,"base",db, ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}
	boolean readFieldPtrLibrary(ObjectHolder<Library> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerLibrary(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerLibrary(ObjectHolder<Library> out, Pointer ptrval, FileDatabase db, Field f, boolean non_recursive)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		Structure s = db.dna.get(f.type);
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// also determine the target type from the block header
		// and check if it matches the type which we expect.
		Structure ss = db.dna.get(block.dna_index);
		if (ss != s) {
			throw new Error("Expected target to be of type `" + s.name +
				"` but seemingly it is a `"+ss.name+"` instead"
				);
		}

		// try to retrieve the object from the cache
		out.set((Library) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
//		int num = block.size / ss.size; 
//		T* o = _allocate(out,num);
		out.reset(new Library());
		Library o = out.get();

		// cache the object before we convert it to avoid cyclic recursion.
		db.cache().set(s,o,ptrval); 

		// if the non_recursive flag is set, we don't do anything but leave
		// the cursor at the correct position to resolve the object.
		if (!non_recursive) {
//			for (int i = 0; i < num; ++i,++o) {
//				s.Convert(*o,db);
//			}
//
//			db.reader->SetCurrentPos(pold);
			
			s.convert(o, db);
			db.reader.setCurrentPos(pold);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			if(out.notNull()) {
				++db.stats().pointers_resolved;
			}
		}
		
		return false;
	}


	void convert(Library dest, FileDatabase db){
		ObjectHolder<Library> holderLibrary = new ObjectHolder<Library>();
		readField(dest.id,"id",db, ErrorPolicy_Fail);
		readFieldArray(dest.name,"name",db, ErrorPolicy_Warn);
		readFieldArray(dest.filename,"filename",db, ErrorPolicy_Fail);
		readFieldPtrLibrary(holderLibrary,"*parent",db, false, ErrorPolicy_Warn);
		dest.parent = holderLibrary.get();

		db.reader.incPtr(size);

	}
	void convert(Tex dest, FileDatabase db){
		ObjectHolder<BLEImage> holderBLEImage = new ObjectHolder<BLEImage>();
		dest.imaflag = readFieldShort("imaflag",db,ErrorPolicy_Igno);
		dest.type = readFieldInt("type",db,ErrorPolicy_Fail);
		readFieldPtrBLEImage(holderBLEImage,"*ima",db, false, ErrorPolicy_Warn);
		dest.ima = holderBLEImage.get();

		db.reader.incPtr(size);

	}
	void convert(BLECamera dest, FileDatabase db){
				readField(dest.id,"id",db, ErrorPolicy_Fail);
		dest.type = readFieldInt("type",db,ErrorPolicy_Warn);
		dest.flag = readFieldInt("flag",db,ErrorPolicy_Warn);
		dest.angle = readFieldFloat("angle",db,ErrorPolicy_Warn);

		db.reader.incPtr(size);

	}
	void convert(MirrorModifierData dest, FileDatabase db){
		ObjectHolder<BLEObject> holderBLEObject = new ObjectHolder<BLEObject>();
		readField(dest.modifier,"modifier",db, ErrorPolicy_Fail);
		dest.axis = readFieldShort("axis",db,ErrorPolicy_Igno);
		dest.flag = readFieldShort("flag",db,ErrorPolicy_Igno);
		dest.tolerance = readFieldFloat("tolerance",db,ErrorPolicy_Igno);
		readFieldPtrBLEObject(holderBLEObject,"*mirror_ob",db, false, ErrorPolicy_Igno);
		dest.mirror_ob = holderBLEObject.get();

		db.reader.incPtr(size);

	}
	boolean readFieldPtrPackedFile(ObjectHolder<PackedFile> out, String name, FileDatabase db, boolean non_recursive,int error_flag)
	{
		int old = db.reader.getCurrentPos();
		Pointer ptrval = new Pointer();
		Field f;
		try {
			f = get(name);

			// sanity check, should never happen if the genblenddna script is right
			if ((f.flags & FieldFlag_Pointer) == 0) {
				throw new Error("Field `" + name + "` of structure `"+
					name + "` ought to be a pointer");
			}

			db.reader.incPtr(f.offset);
			convert(ptrval,db);
			// actually it is meaningless on which Structure the Convert is called
			// because the `Pointer` argument triggers a special implementation.
		}
		catch (Exception e) {
//			_defaultInitializer<error_policy>()(out,e.what());
			_defaultInitializer((ElemBase)null, error_flag, e);
			out.reset();
			return false;
		}

		// resolve the pointer and load the corresponding structure
		final boolean res = resolvePointerPackedFile(out,ptrval,db,f, non_recursive);

		if(!non_recursive) {
			// and recover the previous stream position
			db.reader.setCurrentPos(old);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().fields_read;
		return res;
	}
	
	boolean resolvePointerPackedFile(ObjectHolder<PackedFile> out, Pointer ptrval, FileDatabase db, Field f, boolean non_recursive)
	{
		out.reset(); // ensure null pointers work
		if (ptrval.val == 0) { 
			return false;
		}
		Structure s = db.dna.get(f.type);
		// find the file block the pointer is pointing to
		FileBlockHead block = locateFileBlockForAddress(ptrval,db);

		// also determine the target type from the block header
		// and check if it matches the type which we expect.
		Structure ss = db.dna.get(block.dna_index);
		if (ss != s) {
			throw new Error("Expected target to be of type `" + s.name +
				"` but seemingly it is a `"+ss.name+"` instead"
				);
		}

		// try to retrieve the object from the cache
		out.set((PackedFile) db.cache().get(s,ptrval)); 
		if (out.notNull()) {
			return true;
		}

		// seek to this location, but save the previous stream pointer.
		int pold = db.reader.getCurrentPos();
		db.reader.setCurrentPos((int) (block.start+ /*static_cast<size_t>*/((ptrval.val - block.address.val) )));
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.

		// continue conversion after allocating the required storage
//		int num = block.size / ss.size; 
//		T* o = _allocate(out,num);
		out.reset(new PackedFile());
		PackedFile o = out.get();

		// cache the object before we convert it to avoid cyclic recursion.
		db.cache().set(s,o,ptrval); 

		// if the non_recursive flag is set, we don't do anything but leave
		// the cursor at the correct position to resolve the object.
		if (!non_recursive) {
//			for (int i = 0; i < num; ++i,++o) {
//				s.Convert(*o,db);
//			}
//
//			db.reader->SetCurrentPos(pold);
			
			s.convert(o, db);
			db.reader.setCurrentPos(pold);
		}

		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
			if(out.notNull()) {
				++db.stats().pointers_resolved;
			}
		}
		
		return false;
	}


	void convert(BLEImage dest, FileDatabase db){
		ObjectHolder<PackedFile> holderPackedFile = new ObjectHolder<PackedFile>();
		readField(dest.id,"id",db, ErrorPolicy_Fail);
		readFieldArray(dest.name,"name",db, ErrorPolicy_Warn);
		dest.ok = readFieldShort("ok",db,ErrorPolicy_Igno);
		dest.flag = readFieldShort("flag",db,ErrorPolicy_Igno);
		dest.source = readFieldShort("source",db,ErrorPolicy_Igno);
		dest.type = readFieldShort("type",db,ErrorPolicy_Igno);
		dest.pad = readFieldShort("pad",db,ErrorPolicy_Igno);
		dest.pad1 = readFieldShort("pad1",db,ErrorPolicy_Igno);
		dest.lastframe = readFieldInt("lastframe",db,ErrorPolicy_Igno);
		dest.tpageflag = readFieldShort("tpageflag",db,ErrorPolicy_Igno);
		dest.totbind = readFieldShort("totbind",db,ErrorPolicy_Igno);
		dest.xrep = readFieldShort("xrep",db,ErrorPolicy_Igno);
		dest.yrep = readFieldShort("yrep",db,ErrorPolicy_Igno);
		dest.twsta = readFieldShort("twsta",db,ErrorPolicy_Igno);
		dest.twend = readFieldShort("twend",db,ErrorPolicy_Igno);
		readFieldPtrPackedFile(holderPackedFile,"*packedfile",db, false, ErrorPolicy_Igno);
		dest.packedfile = holderPackedFile.get();
		dest.lastupdate = readFieldFloat("lastupdate",db,ErrorPolicy_Igno);
		dest.lastused = readFieldInt("lastused",db,ErrorPolicy_Igno);
		dest.animspeed = readFieldShort("animspeed",db,ErrorPolicy_Igno);
		dest.gen_x = readFieldShort("gen_x",db,ErrorPolicy_Igno);
		dest.gen_y = readFieldShort("gen_y",db,ErrorPolicy_Igno);
		dest.gen_type = readFieldShort("gen_type",db,ErrorPolicy_Igno);

		db.reader.incPtr(size);

	}

}
