package assimp.importer.dxf;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.ImporterDesc;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import assimp.common.StreamReader;

/** DXF importer implementation.
*
*/
public class DXFImporter extends BaseImporter{
	
	static final String AI_DXF_BINARY_IDENT = "AutoCAD Binary DXF\r\n\u001a";
	
	/** default vertex color that all uncolored vertices will receive */
	static final ReadableVector4f AI_DXF_DEFAULT_COLOR = new Vector4f(0.6f,0.6f,0.6f,0.6f);
	
	// color indices for DXF - 16 are supported, the table is 
	// taken directly from the DXF spec.
	static final Vector4f g_aclrDxfIndexColors[] =
	{
		new Vector4f (0.6f, 0.6f, 0.6f, 1.0f),
		new Vector4f (1.0f, 0.0f, 0.0f, 1.0f), // red
		new Vector4f (0.0f, 1.0f, 0.0f, 1.0f), // green
		new Vector4f (0.0f, 0.0f, 1.0f, 1.0f), // blue
		new Vector4f (0.3f, 1.0f, 0.3f, 1.0f), // light green
		new Vector4f (0.3f, 0.3f, 1.0f, 1.0f), // light blue
		new Vector4f (1.0f, 0.3f, 0.3f, 1.0f), // light red
		new Vector4f (1.0f, 0.0f, 1.0f, 1.0f), // pink
		new Vector4f (1.0f, 0.6f, 0.0f, 1.0f), // orange
		new Vector4f (0.6f, 0.3f, 0.0f, 1.0f), // dark orange
		new Vector4f (1.0f, 1.0f, 0.0f, 1.0f), // yellow
		new Vector4f (0.3f, 0.3f, 0.3f, 1.0f), // dark gray
		new Vector4f (0.8f, 0.8f, 0.8f, 1.0f), // light gray
		new Vector4f (0.0f, 00.f, 0.0f, 1.0f), // black
		new Vector4f (1.0f, 1.0f, 1.0f, 1.0f), // white
		new Vector4f (0.6f, 0.0f, 1.0f, 1.0f)  // violet
	};
	
	static final String AI_DXF_ENTITIES_MAGIC_BLOCK = "$ASSIMP_ENTITIES_MAGIC";
	
	static final ImporterDesc desc = new ImporterDesc(
		"Drawing Interchange Format (DXF) Importer",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportTextFlavour | ImporterDesc.aiImporterFlags_LimitedSupport,
		0,
		0,
		0,
		0,
		"dxf" 
	);
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) throws IOException {
		return simpleExtensionCheck(pFile, "dxf", null, null);
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		try(FileInputStream in = new FileInputStream(pFile)){
			byte[] source = AI_DXF_BINARY_IDENT.getBytes();
			byte[] compares = new byte[source.length];
			int len = in.read(compares);
			if(len == source.length && Arrays.equals(source, compares)){
				throw new DeadlyImportError("DXF: Binary files are not supported at the moment");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		LineReader reader = null;
		FileData output = new FileData();
		try(FileInputStream fin = new FileInputStream(pFile); 
				BufferedInputStream in = new BufferedInputStream(fin)){
			reader = new LineReader(new StreamReader(in, true));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// now get all lines of the file and process top-level sections
		boolean eof = false;
		while(!reader.end()) {

			// blocks table - these 'build blocks' are later (in ENTITIES)
			// referenced an included via INSERT statements.
			if (reader.is(2,"BLOCKS")) {
				parseBlocks(reader,output);
				continue;
			}
		
			// primary entity table
			if (reader.is(2,"ENTITIES")) {
				parseEntities(reader,output);
				continue;
			}

			// skip unneeded sections entirely to avoid any problems with them 
			// alltogether.
			else if (reader.is(2,"CLASSES") || reader.is(2,"TABLES")) {
				skipSection(reader);
				continue;
			}

			else if (reader.is(2,"HEADER")) {
				parseHeader(reader,output);
				continue;
			}

			// comments
			else if (reader.is(999)) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.info("DXF Comment: " + reader.value());
			}

			// don't read past the official EOF sign
			else if (reader.is(0,"EOF")) {
				eof = true;
				break;
			}

//			++reader;
			reader.next();
		}
		if (!eof) {
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("DXF: EOF reached, but did not encounter DXF EOF marker");
		}

		convertMeshes(pScene,output);

		// Now rotate the whole scene by 90 degrees around the x axis to convert from AutoCAD's to Assimp's coordinate system
//		pScene->mRootNode->mTransformation = aiMatrix4x4(
//			1.f,0.f,0.f,0.f,
//			0.f,0.f,1.f,0.f,
//			0.f,-1.f,0.f,0.f,
//			0.f,0.f,0.f,1.f) * pScene->mRootNode->mTransformation;
		Matrix4f mat = new Matrix4f();
		mat.setRow(0, 1, 0, 0, 0);
		mat.setRow(1, 0, 0, 1, 0);
		mat.setRow(2, 0, -1, 0, 0);
		mat.setRow(3, 0, 0, 0, 1);
		Matrix4f.mul(mat, pScene.mRootNode.mTransformation, pScene.mRootNode.mTransformation);
	}

	// -----------------------------------------------------
	void skipSection(LineReader reader){
		for( ;!reader.end() && !reader.is(0,"ENDSEC"); reader.next());
	}

	// -----------------------------------------------------
	void parseHeader(LineReader reader, FileData output){
		for( ;!reader.end() && !reader.is(0,"ENDSEC"); reader.next());
	}

	// -----------------------------------------------------
	void parseEntities(LineReader reader, FileData output){
		// push a new block onto the stack.
		Block block;
		output.blocks.add( block = new Block() );
//		DXF::Block& block = output.blocks.back();

		block.name = AI_DXF_ENTITIES_MAGIC_BLOCK;

		while( !reader.end() && !reader.is(0,"ENDSEC")) {
			if (reader.is(0,"POLYLINE")) {
				parsePolyLine(/*++*/reader,output);
				reader.next();
				continue;
			}

			else if (reader.is(0,"INSERT")) {
				parseInsertion(/*++*/reader,output);
				reader.next();
				continue;
			}

			else if (reader.is(0,"3DFACE") || reader.is(0,"LINE") || reader.is(0,"3DLINE")) {
				//http://sourceforge.net/tracker/index.php?func=detail&aid=2970566&group_id=226462&atid=1067632
				parse3DFace(/*++*/reader, output);
				reader.next();
				continue;
			}

//			++reader;
			reader.next();
		}

		if(DefaultLogger.LOG_OUT)
			DefaultLogger.debug(AssUtil.makeString("DXF: got ",
					block.lines.size()," polylines and ", block.insertions.size() ," inserted blocks in ENTITIES"
		));
	}

	// -----------------------------------------------------
	void parseBlocks(LineReader reader, FileData output){
		while( !reader.end() && !reader.is(0,"ENDSEC")) {
			if (reader.is(0,"BLOCK")) {
				parseBlock(reader,output);
				reader.next();
				continue;
			}
//			++reader;
			reader.next();
		}

		DefaultLogger.debug(AssUtil.makeString(("DXF: got "),
			output.blocks.size()," entries in BLOCKS"
		));
	}

	// -----------------------------------------------------
	void parseBlock(LineReader reader,  FileData output){
		// push a new block onto the stack.
		Block block;
		output.blocks.add( block = new Block() );
//		DXF::Block& block = output.blocks.back();

		while( !reader.end() && !reader.is(0,"ENDBLK")) {

			switch(reader.groupCode()) {
				case 2:
					block.name = reader.value();
					break;

				case 10:
					block.base.x = reader.valueAsFloat();
					break;
				case 20:
					block.base.y = reader.valueAsFloat();
					break;
				case 30:
					block.base.z = reader.valueAsFloat();
					break;
			}

			if (reader.is(0,"POLYLINE")) {
				parsePolyLine(reader,output);
				reader.next();
				continue;
			}

			// XXX is this a valid case?
			if (reader.is(0,"INSERT")) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("DXF: INSERT within a BLOCK not currently supported; skipping");
				for( ;!reader.end() && !reader.is(0,"ENDBLK"); reader.next());
				break;
			}

			else if (reader.is(0,"3DFACE") || reader.is(0,"LINE") || reader.is(0,"3DLINE")) {
				//http://sourceforge.net/tracker/index.php?func=detail&aid=2970566&group_id=226462&atid=1067632
				parse3DFace(reader, output);
				reader.next();
				continue;
			}
			reader.next();
		}
	}

	// -----------------------------------------------------
	void parseInsertion(LineReader reader, FileData output){
//		output.blocks.back().insertions.push_back( DXF::InsertBlock() );
//		DXF::InsertBlock& bl = output.blocks.back().insertions.back();
		InsertBlock bl;
		AssUtil.back(output.blocks).insertions.add(bl = new InsertBlock());

		while( !reader.end() && !reader.is(0)) {

			switch(reader.groupCode()) 
			{
				// name of referenced block
			case 2:
				bl.name = reader.value();
				break;

				// translation
			case 10:
				bl.pos.x = reader.valueAsFloat();
				break;
			case 20:
				bl.pos.y = reader.valueAsFloat();
				break;
			case 30:
				bl.pos.z = reader.valueAsFloat();
				break;

				// scaling
			case 41:
				bl.scale.x = reader.valueAsFloat();
				break;
			case 42:
				bl.scale.y = reader.valueAsFloat();
				break;
			case 43:
				bl.scale.z = reader.valueAsFloat();
				break;

				// rotation angle
			case 50:
				bl.angle = reader.valueAsFloat();
				break;
			}
//			reader++;
			reader.next();
		}
	}
	
	static final int DXF_POLYLINE_FLAG_CLOSED		=0x1;
	static final int DXF_POLYLINE_FLAG_3D_POLYLINE	=0x8;
	static final int DXF_POLYLINE_FLAG_3D_POLYMESH	=0x10;
	static final int DXF_POLYLINE_FLAG_POLYFACEMESH	=0x40;

	// -----------------------------------------------------
	void parsePolyLine(LineReader reader, FileData output){
//		output.blocks.back().lines.push_back( boost::shared_ptr<DXF::PolyLine>( new DXF::PolyLine() ) );
//		DXF::PolyLine& line = *output.blocks.back().lines.back();
		PolyLine line;
		AssUtil.back(output.blocks).lines.add(line = new PolyLine());

	    int iguess = 0, vguess = 0;
		while( !reader.end() && !reader.is(0,"ENDSEC")) {
		
			if (reader.is(0,"VERTEX")) {
				parsePolyLineVertex(/*++*/reader,line); reader.next();
				if (reader.is(0,"SEQEND")) {
					break;
				}
				continue;
			}

			switch(reader.groupCode())	
			{
			// flags --- important that we know whether it is a 
			// polyface mesh or 'just' a line.
			case 70:
				if (line.flags == 0)	{
					line.flags = reader.valueAsInt();
				}
				break;

			// optional number of vertices
			case 71:
				vguess = reader.valueAsInt();
//				line.positions.reserve(vguess);
				line.positions = MemoryUtil.createFloatBuffer(3 * vguess, AssimpConfig.MESH_USE_NATIVE_MEMORY);
				break;

			// optional number of faces
			case 72:
				iguess = reader.valueAsInt();
//				line.indices.reserve(iguess);
				line.indices = new IntArrayList(iguess);
				break;

			// 8 specifies the layer on which this line is placed on
			case 8:
				line.layer = reader.value();
				break;
			}

//			reader++;
			reader.next();
		}
		
		final int numPositions;
		if(line.positions != null){
			line.positions.flip();
			numPositions = line.getNumPositions();
		}else{
			numPositions = 0;
		}

		//if (!(line.flags & DXF_POLYLINE_FLAG_POLYFACEMESH))	{
		//	DefaultLogger::get()->warn((Formatter::format("DXF: polyline not currently supported: "),line.flags));
		//	output.blocks.back().lines.pop_back();
		//	return;
		//}
		
		final int indices_size = AssUtil.size(line.indices);
		final int counts_size = AssUtil.size(line.counts);

		if (vguess != 0 && numPositions != vguess) {
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn(AssUtil.makeString(("DXF: unexpected vertex count in polymesh: "),
						numPositions,", expected ", vguess
				));
		}

		if ((line.flags & DXF_POLYLINE_FLAG_POLYFACEMESH )!=0) {
			if (numPositions < 3 || indices_size < 3)	{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("DXF: not enough vertices for polymesh; ignoring");
				List<PolyLine> back = AssUtil.back(output.blocks).lines;
				back.remove(back.size() - 1);
				return;
			}

			// if these numbers are wrong, parsing might have gone wild. 
			// however, the docs state that applications are not required
			// to set the 71 and 72 fields, respectively, to valid values.
			// So just fire a warning.
			if (iguess != 0 && counts_size != iguess) {
				if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn(AssUtil.makeString(("DXF: unexpected face count in polymesh: "),
						counts_size,", expected ", iguess
				));
			}
		}
		else if (indices_size == 0 && counts_size == 0) {
			// a polyline - so there are no indices yet.
			int guess = numPositions + ((line.flags & DXF_POLYLINE_FLAG_CLOSED) != 0 ? 1 : 0);
//			line.indices.reserve(guess);
//			line.counts.reserve(guess/2);
			line.indices = AssUtil.reserve(line.indices, guess);
			line.counts = AssUtil.reserve(line.counts, guess/2);
			
			for (int i = 0; i < numPositions/2; ++i) {
				line.indices.add(i*2);
				line.indices.add(i*2+1);
				line.counts.add(2);
			}

			// closed polyline?
			if ((line.flags & DXF_POLYLINE_FLAG_CLOSED)!=0) {
				line.indices.add(numPositions-1);
				line.indices.add(0);
				line.counts.add(2);
			}
		}
	}
	
	static final int DXF_VERTEX_FLAG_PART_OF_POLYFACE = 0x80;
	static final int DXF_VERTEX_FLAG_HAS_POSITIONS    = 0x40;

	// -----------------------------------------------------
	void parsePolyLineVertex(LineReader reader, PolyLine line){
		int cnti = 0, flags = 0;
		int[] indices = new int[4];

		final Vector3f out = new Vector3f();;
		ReadableVector4f clr = AI_DXF_DEFAULT_COLOR;

		while( !reader.end() ) {

			if (reader.is(0)) { // SEQEND or another VERTEX
				break;
			}

			switch (reader.groupCode())
			{
			case 8:
					// layer to which the vertex belongs to - assume that
					// this is always the layer the top-level polyline
					// entity resides on as well.
					if(DefaultLogger.LOG_OUT && !reader.value().equals(line.layer)) {
						DefaultLogger.warn("DXF: expected vertex to be part of a polyface but the 0x128 flag isn't set");
					}
					break;

			case 70:
					flags = reader.valueAsInt();
					break;

			// VERTEX COORDINATES
			case 10: out.x = reader.valueAsFloat();break;
			case 20: out.y = reader.valueAsFloat();break;
			case 30: out.z = reader.valueAsFloat();break;

			// POLYFACE vertex indices
			case 71: 
			case 72:
			case 73:
			case 74: 
				if (cnti == 4) {
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn("DXF: more than 4 indices per face not supported; ignoring");
					break;
				}
				indices[cnti++] = reader.valueAsInt();
				break;

			// color
			case 62: 
				clr = g_aclrDxfIndexColors[reader.valueAsInt() % g_aclrDxfIndexColors.length]; 
				break;
			};
		
//			reader++;
			reader.next();
		}
		
		if (DefaultLogger.LOG_OUT && (line.flags & DXF_POLYLINE_FLAG_POLYFACEMESH) != 0 && (flags & DXF_VERTEX_FLAG_PART_OF_POLYFACE) == 0) {
			DefaultLogger.warn("DXF: expected vertex to be part of a polyface but the 0x128 flag isn't set");
		}

		if (cnti != 0) {
			line.counts.add(cnti);
			for (int i = 0; i < cnti; ++i) {
				// IMPORTANT NOTE: POLYMESH indices are ONE-BASED
				if (indices[i] == 0) {
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn("DXF: invalid vertex index, indices are one-based.");
//					--line.counts.back();
					int idx = line.counts.size() - 1;
//					line.counts.set(idx, line.counts.get(idx) - 1);
					--line.counts.elements()[idx];
					continue;
				}
				line.indices.add(indices[i]-1);
			}
		}
		else {
//			line.positions.add(out);
//			line.colors.add(clr);
			out.store(line.positions);
			clr.store(line.colors);
		}
	}

	// -----------------------------------------------------
	void parse3DFace(LineReader reader, FileData output){
		// (note) this is also used for for parsing line entities, so we
		// must handle the vertex_count == 2 case as well.

//		output.blocks.back().lines.push_back( boost::shared_ptr<DXF::PolyLine>( new DXF::PolyLine() )  );
//		DXF::PolyLine& line = *output.blocks.back().lines.back();
		PolyLine line;
		AssUtil.back(output.blocks).lines.add(line = new PolyLine());

//		aiVector3D vip[4];
//		aiColor4D  clr = AI_DXF_DEFAULT_COLOR;
		final Vector3f[] vip = new Vector3f[4];
		AssUtil.initArray(vip);
		ReadableVector4f clr = AI_DXF_DEFAULT_COLOR;
		
		boolean[] b = {false,false,false,false};
		while( !reader.end() ) {

			// next entity with a groupcode == 0 is probably already the next vertex or polymesh entity
			if (reader.groupCode() == 0) {
				break;
			}
			switch (reader.groupCode())	
			{

			// 8 specifies the layer
			case 8:	
				line.layer = reader.value();
				break;

			// x position of the first corner
			case 10: vip[0].x = reader.valueAsFloat();
				b[2] = true;
				break;

			// y position of the first corner
			case 20: vip[0].y = reader.valueAsFloat();
				b[2] = true;
				break;

			// z position of the first corner
			case 30: vip[0].z = reader.valueAsFloat();
				b[2] = true;
				break;

			// x position of the second corner
			case 11: vip[1].x = reader.valueAsFloat();
				b[3] = true;
				break;

			// y position of the second corner
			case 21: vip[1].y = reader.valueAsFloat();
				b[3] = true;
				break;

			// z position of the second corner
			case 31: vip[1].z = reader.valueAsFloat();
				b[3] = true;
				break;

			// x position of the third corner
			case 12: vip[2].x = reader.valueAsFloat();
				b[0] = true;
				break;

			// y position of the third corner
			case 22: vip[2].y = reader.valueAsFloat();
				b[0] = true;
				break;

			// z position of the third corner
			case 32: vip[2].z = reader.valueAsFloat();
				b[0] = true;
				break;

			// x position of the fourth corner
			case 13: vip[3].x = reader.valueAsFloat();
				b[1] = true;
				break;

			// y position of the fourth corner
			case 23: vip[3].y = reader.valueAsFloat();
				b[1] = true;
				break;

			// z position of the fourth corner
			case 33: vip[3].z = reader.valueAsFloat();
				b[1] = true;
				break;

			// color
			case 62: 
				clr = g_aclrDxfIndexColors[reader.valueAsInt() % g_aclrDxfIndexColors.length]; 
				break;
			};

//			++reader;
			reader.next();
		}

		// the fourth corner may even be identical to the third,
		// in this case we treat it as if it didn't exist.
		if (vip[3].equals(vip[2])) {
			b[1] = false;
		}
		
		// sanity checks to see if we got something meaningful
		if ((b[1] && !b[0]) || !b[2] || !b[3]) {
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("DXF: unexpected vertex setup in 3DFACE/LINE/FACE entity; ignoring");
//			output.blocks.back().lines.pop_back();
			Block back = AssUtil.back(output.blocks);
			int last = back.lines.size() - 1;
			back.lines.remove(last);
			return;
		}

		final int cnt = (2+(b[0]?1:0)+(b[1]?1:0));
		line.counts.add(cnt);

		for (int i = 0; i < cnt; ++i) {
			line.indices.add(line.positions.position()/3);
//			line.positions.add(vip[i]);
//			line.colors.add(clr);
			vip[i].store(line.positions);
			clr.store(line.colors);
		}
		
		line.positions.flip();
		line.colors.flip();
	}

	// -----------------------------------------------------
	void convertMeshes(Scene pScene, FileData output){
		// the process of resolving all the INSERT statements can grow the
		// polycount excessively, so log the original number.
		// XXX Option to import blocks as separate nodes?
		if (/*!DefaultLogger::isNullLogger()*/ DefaultLogger.LOG_OUT) {

			int vcount = 0, icount = 0;
//			BOOST_FOREACH (const DXF::Block& bl, output.blocks) {
			for (Block bl : output.blocks){
//				BOOST_FOREACH (boost::shared_ptr<const DXF::PolyLine> pl, bl.lines) {
				for (PolyLine pl : bl.lines){
					vcount += pl.getNumPositions();
					icount += AssUtil.size(pl.counts);
				}
			}

			DefaultLogger.debug(String.format("DXF: Unexpanded polycount is ",
				icount,", vertex count is ",vcount
			));
		}

		if (AssUtil.isEmpty(output.blocks) ) {
			throw new DeadlyImportError("DXF: no data blocks loaded");
		}

		Block entities = null;
		
		// index blocks by name
//		DXF::BlockMap blocks_by_name;
		HashMap<String, Block> blocks_by_name = new HashMap<String, Block>();
//		BOOST_FOREACH (DXF::Block& bl, output.blocks) {
		for (Block bl : output.blocks) {
//			blocks_by_name[bl.name] = &bl;
			blocks_by_name.put(bl.name, bl);
			if ( entities == null && bl.name.equals(AI_DXF_ENTITIES_MAGIC_BLOCK) ) {
				entities = bl;
			}
		}

		if (entities == null) {
			throw new DeadlyImportError("DXF: no ENTITIES data block loaded");
		}

//		typedef std::map<std::string, unsigned int> LayerMap;
//		LayerMap layers;
		
		Object2IntMap<String> layers = new Object2IntOpenHashMap<String>();
		layers.defaultReturnValue(-1);
//		std::vector< std::vector< const DXF::PolyLine*> > corr;
		ArrayList<ArrayList<PolyLine>> corr = new ArrayList<>();

		// now expand all block references in the primary ENTITIES block
		// XXX this involves heavy memory copying, consider a faster solution for future versions.
		expandBlockReferences(entities,blocks_by_name);

		int cur = 0;
		int pScene_numMeshes = 0;
//		BOOST_FOREACH (boost::shared_ptr<const DXF::PolyLine> pl, entities->lines) {
		for (PolyLine pl : entities.lines) {
			if (pl.getNumPositions() > 0) {

//				std::map<std::string, unsigned int>::iterator it = layers.find(pl->layer);
				int it = layers.getInt(pl.layer);
				if (it == -1/*layers.end()*/) {
//					++pScene->mNumMeshes;
					++pScene_numMeshes;

//					layers[pl.layer] = cur++;
					layers.put(pl.layer, cur++);

//					std::vector< const DXF::PolyLine* > pv;
//					pv.push_back(&*pl);
					ArrayList<PolyLine> pv = new ArrayList<PolyLine>();
					pv.add(pl);

					corr.add(pv);
				}
				else {
//					corr[(*it).second].push_back(&*pl);
					corr.get(it).add(pl);
				}
			}
		}

		if (pScene_numMeshes == 0) {
			throw new DeadlyImportError("DXF: this file contains no 3d data");
		}

		pScene.mMeshes = new Mesh[ pScene_numMeshes ];

//		BOOST_FOREACH(const LayerMap::value_type& elem, layers){
		for (Object2IntMap.Entry<String> elem : layers.object2IntEntrySet()) {
			final Mesh mesh =  pScene.mMeshes[elem.getIntValue()] = new Mesh();
			mesh.mName = elem.getKey();

			int cvert = 0,cface = 0;
//			BOOST_FOREACH(const DXF::PolyLine* pl, corr[elem.second]){
			for (PolyLine pl : corr.get(elem.getIntValue())){
				// sum over all faces since we need to 'verbosify' them.
//				cvert += std::accumulate(pl.counts.begin(),pl.counts.end(),0); 
				cvert += AssUtil.accumulate(pl.counts, 0, pl.counts.size(), 0);
				cface += pl.counts.size();
			}

//			aiVector3D* verts = mesh->mVertices = new aiVector3D[cvert];
//			aiColor4D* colors = mesh->mColors[0] = new aiColor4D[cvert];
			mesh.mVertices = MemoryUtil.createFloatBuffer(3 * cvert, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			mesh.mColors[0] = MemoryUtil.createFloatBuffer(4 * cvert, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			int verts = 0;
			int colors = 0;
			mesh.mFaces = new Face[cface];
			int faces = 0;

			mesh.mNumVertices = cvert;
//			mesh->mNumFaces = cface;

			int prims = 0;
			int overall_indices = 0;
//			BOOST_FOREACH(const DXF::PolyLine* pl, corr[elem.second]){
			for (PolyLine pl : corr.get(elem.getIntValue())) {

//				std::vector<unsigned int>::const_iterator it = pl->indices.begin();
				int it = 0;
//				BOOST_FOREACH(unsigned int facenumv,pl->counts) {
				for (int c = 0; c < pl.counts.size(); c++){
					int facenumv = pl.counts.getInt(c);
//					aiFace& face = *faces++;
//					face.mIndices = new unsigned int[face.mNumIndices = facenumv];
					Face face = mesh.mFaces[faces++] = Face.createInstance(facenumv);

					for (int i = 0; i < facenumv; ++i) {
//						face.mIndices[i] = overall_indices++;
						face.set(i, overall_indices++);

//						ai_assert(pl->positions.size() == pl->colors.size());
						if (it >= pl.getNumPositions()) {
							throw new DeadlyImportError("DXF: vertex index out of bounds");
						}

//						*verts++ = pl->positions[*it];
//						*colors++ = pl->colors[*it++];
						MemoryUtil.arraycopy(pl.positions, 3 * pl.indices.getInt(it), mesh.mVertices, 3 * verts++, 3);
						MemoryUtil.arraycopy(pl.colors, 4 * pl.indices.getInt(it++), mesh.mColors[0], 4 * colors++, 4);
					}

					// set primitive flags now, this saves the extra pass in ScenePreprocessor.
					switch(face.getNumIndices()) {
						case 1:
							prims |= Mesh.aiPrimitiveType_POINT;
							break;
						case 2:
							prims |= Mesh.aiPrimitiveType_LINE;
							break;
						case 3:
							prims |= Mesh.aiPrimitiveType_TRIANGLE;
							break;
						default:
							prims |= Mesh.aiPrimitiveType_POLYGON;
							break;
					}
				}
			}

			mesh.mPrimitiveTypes = prims;
			mesh.mMaterialIndex = 0;
		}

		generateHierarchy(pScene,output);
		generateMaterials(pScene,output);
	}

	// -----------------------------------------------------
	void generateHierarchy(Scene pScene, FileData output){
		// generate the output scene graph, which is just the root node with a single child for each layer.
		pScene.mRootNode = new Node();
		pScene.mRootNode.mName = ("<DXF_ROOT>");

		if (1 == pScene.getNumMeshes())	{
			pScene.mRootNode.mMeshes = new int[ /*pScene->mRootNode->mNumMeshes =*/ 1 ];
			pScene.mRootNode.mMeshes[0] = 0;
		}
		else
		{
			pScene.mRootNode.mChildren = new Node[ /*pScene->mRootNode->mNumChildren = pScene->mNumMeshes*/pScene.getNumMeshes() ];
			for (int m = 0; m < /*pScene->mRootNode->mNumChildren*/ pScene.mRootNode.mChildren.length ;++m)	{
				Node p = pScene.mRootNode.mChildren[m] = new Node();
				p.mName = pScene.mMeshes[m].mName;

				p.mMeshes = new int[/*p->mNumMeshes =*/ 1];
				p.mMeshes[0] = m;
				p.mParent = pScene.mRootNode;
			}
		}
	}

	// -----------------------------------------------------
	void generateMaterials(Scene pScene, FileData output){
		// generate an almost-white default material. Reason:
		// the default vertex color is GREY, so we are
		// already at Assimp's usual default color.
		// generate a default material
		Material pcMat = new Material();
		pcMat.addProperty(Material.AI_DEFAULT_MATERIAL_NAME, Material.AI_MATKEY_NAME,0, 0);

		Vector4f clrDiffuse = new Vector4f(0.9f,0.9f,0.9f,1.0f);
		pcMat.addProperty(clrDiffuse, Material.AI_MATKEY_COLOR_DIFFUSE, 0, 0);

		clrDiffuse.set(1.0f,1.0f,1.0f,1.0f);
		pcMat.addProperty(clrDiffuse, Material.AI_MATKEY_COLOR_SPECULAR, 0, 0);

		clrDiffuse.set(0.05f,0.05f,0.05f,1.0f);
		pcMat.addProperty(clrDiffuse, Material.AI_MATKEY_COLOR_AMBIENT, 0, 0);

//		pScene->mNumMaterials = 1;
//		pScene->mMaterials = new aiMaterial*[1];
//		pScene->mMaterials[0] = pcMat;
		pScene.mMaterials = new Material[]{pcMat};
	}

	// -----------------------------------------------------
	void expandBlockReferences(Block bl, HashMap<String, Block> blocks_by_name){
		Matrix4f tmpMat = new Matrix4f();
		Vector3f tmpVec3 = new Vector3f();
//		BOOST_FOREACH (const DXF::InsertBlock& insert, bl.insertions) {
		for(InsertBlock insert : bl.insertions) {
			// first check if the referenced blocks exists ...
//			const DXF::BlockMap::const_iterator it = blocks_by_name.find(insert.name);
//			if (it == blocks_by_name.end()) {
//				DefaultLogger::get()->error((Formatter::format("DXF: Failed to resolve block reference: "),
//					insert.name,"; skipping"
//				));
//				continue;
//			}
			
			Block bl_src = blocks_by_name.get(insert.name);
			if(bl_src == null){
				DefaultLogger.error(AssUtil.makeString("DXF: Failed to resolve block reference: ", insert.name,"; skipping"));
				continue;
			}

			// XXX this would be the place to implement recursive expansion if needed.
//			const DXF::Block& bl_src = *(*it).second;
			
//			BOOST_FOREACH (boost::shared_ptr<const DXF::PolyLine> pl_in, bl_src.lines) {
			for (PolyLine pl_in : bl_src.lines) {
//				boost::shared_ptr<DXF::PolyLine> pl_out = boost::shared_ptr<DXF::PolyLine>(new DXF::PolyLine(*pl_in));
				PolyLine pl_out = new PolyLine(pl_in);  // TODO

				if (bl_src.base.length() != 0|| insert.scale.x!=1.f || insert.scale.y!=1.f || insert.scale.z!=1.f || insert.angle !=0 || insert.pos.length() != 0) {
					// manual coordinate system transformation
					// XXX order
//					aiMatrix4x4 trafo, tmp;
//					aiMatrix4x4::Translation(-bl_src.base,trafo);
//					trafo *= aiMatrix4x4::Scaling(insert.scale,tmp);
//					trafo *= aiMatrix4x4::Translation(insert.pos,tmp);
					
					tmpMat.setIdentity();
					tmpMat.translate(-bl_src.base.x, -bl_src.base.y, -bl_src.base.z);
					tmpMat.scale(insert.scale);
					tmpMat.translate(insert.pos);

					// XXX rotation currently ignored - I didn't find an appropriate sample model.
					if (insert.angle != 0.f) {
						if(DefaultLogger.LOG_OUT)
							DefaultLogger.warn("DXF: BLOCK rotation not currently implemented");
					}

//					BOOST_FOREACH (aiVector3D& v, pl_out->positions) {
//						v *= trafo;
//					}
					
					int count = pl_out.getNumPositions();
					FloatBuffer buf = pl_out.positions;
					for(int i = 0 ; i < count; i++){
						int index = 3 * i;
						tmpVec3.x = buf.get(index+0);
						tmpVec3.y = buf.get(index+1);
						tmpVec3.z = buf.get(index+2);
						
						Matrix4f.transformVector(tmpMat, tmpVec3, tmpVec3);
						buf.put(index+0, tmpVec3.x);
						buf.put(index+1, tmpVec3.y);
						buf.put(index+2, tmpVec3.z);
					}
				}

				bl.lines.add(pl_out);
			}
		}
	}
}
