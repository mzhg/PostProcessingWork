package assimp.importer.blender;

final class BLEMaterial extends ElemBase{

	final ID id = new ID();

	float r,g,b;
	float specr,specg,specb;
	short har;
	float ambr,ambg,ambb;
	float mirr,mirg,mirb;
	float emit;
	float alpha;
	float ref;
	float translucency;
	float roughness;
	float darkness;
	float refrac;

	Group group;

	short diff_shader;
	short spec_shader;
	
	final MTex[] mtex = new MTex[18];
	
	public BLEMaterial() {}
	
	public BLEMaterial(BLEMaterial o) {
		set(o);
	}
	
	public void set(BLEMaterial o){
		id.set(o.id);
		r = o.r;
		g = o.g;
		b = o.b;
		specr = o.specr;
		specg = o.specg;
		specb = o.specb;
		har = o.har;
		ambr = o.ambr;
		ambg = o.ambg;
		ambb = o.ambb;
		mirr = o.mirr;
		mirg = o.mirg;
		mirb = o.mirb;
		emit = o.emit;
		alpha = o.alpha;
		ref = o.ref;
		translucency = o.translucency;
		roughness = o.roughness;
		darkness = o.darkness;
		refrac = o.refrac;
		group = o.group;
		diff_shader = o.diff_shader;
		spec_shader = o.spec_shader;
		for(int i = 0; i < o.mtex.length; i++)
			mtex[i] = new MTex(o.mtex[i]);
	}
}
