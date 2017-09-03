package assimp.importer.blender;

import java.util.ArrayList;

final class BLEMesh extends ElemBase{

	final ID id = new ID();

	int totface;
	int totedge;
	int totvert;
	int totloop;
	int totpoly;

	short subdiv;
	short subdivr;
	short subsurftype;
	short smoothresh;

	final ArrayList<MFace> mface = new ArrayList<>();
	final ArrayList<MTFace> mtface = new ArrayList<>();
	final ArrayList<TFace> tface = new ArrayList<>();
	final ArrayList<MVert> mvert = new ArrayList<>();
	final ArrayList<MEdge> medge = new ArrayList<>();
	final ArrayList<MLoop> mloop = new ArrayList<>();
	final ArrayList<MLoopUV> mloopuv = new ArrayList<>();
	final ArrayList<MLoopCol> mloopcol = new ArrayList<>();
	final ArrayList<MPoly> mpoly = new ArrayList<>();
	final ArrayList<MTexPoly> mtpoly = new ArrayList<>();
	final ArrayList<MDeformVert> dvert = new ArrayList<>();
	final ArrayList<MCol> mcol = new ArrayList<>();

	final ArrayList<BLEMaterial> mat = new ArrayList<>();
	
	public BLEMesh() {}
	
	public BLEMesh(BLEMesh o) {
		set(o);
	}
	
	public void set(BLEMesh o){
		id.set(o.id);
		totface = o.totface;
		totedge = o.totedge;
		totvert = o.totvert;
		totloop = o.totloop;
		totpoly = o.totpoly;
		subdiv = o.subdiv;
		subdivr = o.subdivr;
		subsurftype = o.subsurftype;
		smoothresh = o.smoothresh;
		mface.clear();
		for(int i = 0; i < o.mface.size(); i++)
			mface.add(new MFace(o.mface.get(i)));
		mtface.clear();
		for(int i = 0; i < o.mtface.size(); i++)
			mtface.add(new MTFace(o.mtface.get(i)));
		tface.clear();
		for(int i = 0; i < o.tface.size(); i++)
			tface.add(new TFace(o.tface.get(i)));
		mvert.clear();
		for(int i = 0; i < o.mvert.size(); i++)
			mvert.add(new MVert(o.mvert.get(i)));
		medge.clear();
		for(int i = 0; i < o.medge.size(); i++)
			medge.add(new MEdge(o.medge.get(i)));
		mloop.clear();
		for(int i = 0; i < o.mloop.size(); i++)
			mloop.add(new MLoop(o.mloop.get(i)));
		mloopuv.clear();
		for(int i = 0; i < o.mloopuv.size(); i++)
			mloopuv.add(new MLoopUV(o.mloopuv.get(i)));
		mloopcol.clear();
		for(int i = 0; i < o.mloopcol.size(); i++)
			mloopcol.add(new MLoopCol(o.mloopcol.get(i)));
		mpoly.clear();
		for(int i = 0; i < o.mpoly.size(); i++)
			mpoly.add(new MPoly(o.mpoly.get(i)));
		mtpoly.clear();
		for(int i = 0; i < o.mtpoly.size(); i++)
			mtpoly.add(new MTexPoly(o.mtpoly.get(i)));
		dvert.clear();
		for(int i = 0; i < o.dvert.size(); i++)
			dvert.add(new MDeformVert(o.dvert.get(i)));
		mcol.clear();
		for(int i = 0; i < o.mcol.size(); i++)
			mcol.add(new MCol(o.mcol.get(i)));
		mat.clear();
		for(int i = 0; i < o.mat.size(); i++)
			mat.add(new BLEMaterial(o.mat.get(i)));
	}
}
