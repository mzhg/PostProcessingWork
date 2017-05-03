package jet.opengl.postprocessing.core.light;

final class ColorModulation {
	
	static final int HORI0 = 0;
	static final int HORI1 = 1;
	static final int HORI2 = 2;
	static final int HORI3 = 3;
	static final int STAR0 = 4;
	static final int STAR1 = 5;
	static final int STAR2 = 6;

	private float[] star_modulation1st = new float[16];
	private float[] star_modulation2nd = new float[16];
	private float[] star_modulation3rd = new float[16];

	final float[] filmic_ghost_modulation1st = new float[16];
	final float[] filmic_ghost_modulation2nd = new float[16];
	final float[] camera_ghost_modulation1st = new float[16];
	final float[] camera_ghost_modulation2nd = new float[16];

	private float[] hori_modulation1st = new float[16];
	private float[] hori_modulation2nd = new float[16];
	private float[] hori_modulation3rd = new float[16];
	
	final float[][] hori_passes = new float[7][];
	
	
	public ColorModulation() {
		fillData();
		makeColorMoluations();
		
		// clear the cache.
		star_modulation1st = null;
		star_modulation2nd = null;
		star_modulation3rd = null;
		
		hori_modulation1st = null;
		hori_modulation2nd = null;
		hori_modulation3rd = null;
		
//		System.out.println("STAR0: " + Arrays.toString(hori_passes[STAR0]));
//		System.out.println("STAR1: " + Arrays.toString(hori_passes[STAR1]));
//		System.out.println("STAR2: " + Arrays.toString(hori_passes[STAR2]));
		
//		hori_passes[STAR0] = new float[]{
//				0.25f, 0.2375f, 0.225f, 0.25f, 0.17999999f, 0.17999999f, 0.16199999f, 0.225f, 0.18224998f, 0.16402498f, 0.18224998f, 0.20249999f, 0.16402498f, 0.16402498f, 0.14762247f, 0.18224998f
//		};
//		
//		hori_passes[STAR1] = new float[]{
//				0.25f, 0.225f, 0.2f, 0.25f, 0.16402498f, 0.098414995f, 0.08201249f, 0.16402498f, 0.05380839f, 0.05380839f, 0.032285035f, 0.10761678f, 0.04236442f, 0.016945768f, 0.04236442f, 0.070607364f
//		};
//		hori_passes[STAR2] = new float[]{
//				0.25f, 0.25f, 0.25f, 0.25f, 0.046325486f, 0.027795292f, 0.027795292f, 0.046325486f, 0.0051505216f, 0.0051505216f, 0.003090313f, 0.008584202f, 9.544017E-4f, 5.7264103E-4f, 9.544017E-4f, 0.0015906694f
//		};
	}
	
	public static void main(String[] args) {
		new ColorModulation();
	}
	
	private void makeColorMoluations(){
		int s;
		int n;
		float dec = 0.96f;
		float[] colorCoeff = new float[16];
		//4 passes to generate 256 pixel blur in each direction
		//1st pass
    	n=1;
    	for (s=0; s<4; s+=1) {
    		colorCoeff[s*4+0] = (float) (hori_modulation1st[s*4+0] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+1] = (float) (hori_modulation1st[s*4+1] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+2] = (float) (hori_modulation1st[s*4+2] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+3] = (float) (hori_modulation1st[s*4+3] * Math.pow((dec), Math.pow((4),n-1)*s));
    	}
    	hori_passes[0] = colorCoeff;
    	
    	// 2nd pass
    	colorCoeff = new float[16];
    	n=2;
    	for (s=0; s<4; s+=1) {
    		colorCoeff[s*4+0] = (float) (hori_modulation1st[s*4+0] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+1] = (float) (hori_modulation1st[s*4+1] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+2] = (float) (hori_modulation1st[s*4+2] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+3] = (float) (hori_modulation1st[s*4+3] * Math.pow((dec), Math.pow((4),n-1)*s));
    	}
    	hori_passes[1] = colorCoeff;
    	
    	// 3rd pass
    	n=3;
    	colorCoeff = new float[16];
    	for (s=0; s<4; s+=1) {
    		colorCoeff[s*4+0] = (float) (hori_modulation2nd[s*4+0] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+1] = (float) (hori_modulation2nd[s*4+1] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+2] = (float) (hori_modulation2nd[s*4+2] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+3] = (float) (hori_modulation2nd[s*4+3] * Math.pow((dec), Math.pow((4),n-1)*s));
    	}
    	hori_passes[2] = colorCoeff;
    	
    	// 4th pass
    	n=4;
    	colorCoeff = new float[16];
    	for (s=0; s<4; s+=1) {
    		colorCoeff[s*4+0] = (float) (hori_modulation3rd[s*4+0] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+1] = (float) (hori_modulation3rd[s*4+1] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+2] = (float) (hori_modulation3rd[s*4+2] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+3] = (float) (hori_modulation3rd[s*4+3] * Math.pow((dec), Math.pow((4),n-1)*s));
    	}
    	hori_passes[3] = colorCoeff;
    	
    	//3 passes to generate 64 pixel blur in each direction
    	dec = 0.9f;
    	//1st pass
    	colorCoeff = new float[16];
    	n=1;
    	for (s=0; s<4; s+=1) {
    		colorCoeff[s*4+0] = (float) (star_modulation1st[s*4+0] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+1] = (float) (star_modulation1st[s*4+1] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+2] = (float) (star_modulation1st[s*4+2] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+3] = (float) (star_modulation1st[s*4+3] * Math.pow((dec), Math.pow((4),n-1)*s));
    	}
    	hori_passes[4] = colorCoeff;
    	
    	// 2nd pass
    	colorCoeff = new float[16];
    	n=2;
    	for (s=0; s<4; s+=1) {
    		colorCoeff[s*4+0] = (float) (star_modulation2nd[s*4+0] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+1] = (float) (star_modulation2nd[s*4+1] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+2] = (float) (star_modulation2nd[s*4+2] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+3] = (float) (star_modulation2nd[s*4+3] * Math.pow((dec), Math.pow((4),n-1)*s));
    	}
    	hori_passes[5] = colorCoeff;
    	
    	// 3rd pass
    	colorCoeff = new float[16];
    	n=3;
    	for (s=0; s<4; s+=1) {
    		colorCoeff[s*4+0] = (float) (star_modulation3rd[s*4+0] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+1] = (float) (star_modulation3rd[s*4+1] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+2] = (float) (star_modulation3rd[s*4+2] * Math.pow((dec), Math.pow((4),n-1)*s));
    		colorCoeff[s*4+3] = (float) (star_modulation3rd[s*4+3] * Math.pow((dec), Math.pow((4),n-1)*s));
    	}
    	hori_passes[6] = colorCoeff;
	}
	
	private void fillData(){
		//color modulation coefficients for star streak & ghost image
		for (int i=0;i<16; i++) {
		   star_modulation1st[i]=0.25f;
		   star_modulation2nd[i]=0.25f;
		   star_modulation3rd[i]=0.25f;
		   hori_modulation1st[i]=0.5f;
		   hori_modulation2nd[i]=0.5f;
		   hori_modulation3rd[i]=0.5f;
		   filmic_ghost_modulation1st[i] = 1.0f;
		   filmic_ghost_modulation2nd[i] = 1.0f;
		   camera_ghost_modulation1st[i] = 1.0f;
		   camera_ghost_modulation2nd[i] = 1.0f;
		}
		//star
	    colorModulationRedShift(star_modulation1st, 1.0f, 0.95f, 0.9f,0);
		colorModulationRedShift(star_modulation1st, 0.8f, 1.0f, 0.9f,1);
		colorModulationRedShift(star_modulation1st, 0.9f, 0.9f, 1.0f,2);
		colorModulationRedShift(star_modulation1st, 0.9f, 1.0f, 0.9f,3);

		colorModulationRedShift(star_modulation2nd, 1.0f, 0.9f, 0.8f,0);
	    colorModulationRedShift(star_modulation2nd, 1.0f, 0.6f, 0.5f,1);
		colorModulationRedShift(star_modulation2nd, 0.5f, 1.0f, 0.6f,2);
		colorModulationRedShift(star_modulation2nd, 0.6f, 0.4f, 1.0f,3);

		colorModulationRedShift(star_modulation3rd, 1.0f, 0.6f, 0.6f,1);
		colorModulationRedShift(star_modulation3rd, 0.6f, 1.0f, 0.6f,2);
		colorModulationRedShift(star_modulation3rd, 0.6f, 0.6f, 1.0f,3);
		
		colorModulation(hori_modulation1st, 0.2f, 0.3f, 0.95f,0);
		colorModulation(hori_modulation1st, 0.2f, 0.3f, 0.95f,1);
		colorModulation(hori_modulation1st, 0.1f, 0.2f, 0.9f,2);
		colorModulation(hori_modulation1st, 0.2f, 0.3f, 0.95f,3);

	    colorModulation(hori_modulation2nd, 0.2f, 0.3f, 0.95f,0);
		colorModulation(hori_modulation2nd, 0.1f, 0.2f, 0.9f,1);
		colorModulation(hori_modulation2nd, 0.02f, 0.1f, 0.99f,2);
		colorModulation(hori_modulation2nd, 0.02f, 0.1f, 0.99f,3);

	    colorModulation(hori_modulation3rd, 1.0f, 1.0f, 1.0f,0);
		colorModulation(hori_modulation3rd, 1.0f, 1.0f, 1.0f,1);
		colorModulation(hori_modulation3rd, 1.0f, 1.0f, 1.0f,2);
		colorModulation(hori_modulation3rd, 1.0f, 1.0f, 1.0f,3);

		//ghost camera
		colorModulationRedShift(camera_ghost_modulation1st, 1.0f, 0.9f, 0.8f,0);
	    colorModulationRedShift(camera_ghost_modulation1st, 1.0f, 0.6f, 0.5f,1);
		colorModulationRedShift(camera_ghost_modulation1st, 0.5f, 1.0f, 0.6f,2);
		colorModulationRedShift(camera_ghost_modulation1st, 1.0f, 0.7f, 0.3f,3);

		colorModulationRedShift(camera_ghost_modulation2nd, 0.2f, 0.3f, 0.7f,0);
	    colorModulationRedShift(camera_ghost_modulation2nd, 0.5f, 0.3f, 0.2f,1);
		colorModulationRedShift(camera_ghost_modulation2nd, 0.1f, 0.5f, 0.2f,2);
		colorModulationRedShift(camera_ghost_modulation2nd, 0.1f, 0.1f, 1.0f,3);

		//ghost filmic
		colorModulation(filmic_ghost_modulation1st, 0.1f, 0.1f, 1.0f,0);
	    colorModulation(filmic_ghost_modulation1st, 0.2f, 0.3f, 1.0f,1);
		colorModulation(filmic_ghost_modulation1st, 0.1f, 0.2f, 0.6f,2);
		colorModulation(filmic_ghost_modulation1st, 0.6f, 0.3f, 1.0f,3);

		colorModulation(filmic_ghost_modulation2nd, 0.6f, 0.2f, 0.2f,0);
	    colorModulation(filmic_ghost_modulation2nd, 0.2f, 0.06f, 0.6f,1);
		colorModulation(filmic_ghost_modulation2nd, 0.15f, 0.00f, 0.1f,2);
		colorModulation(filmic_ghost_modulation2nd, 0.06f, 0.00f, 0.55f,3);
	}
	
	private static void colorModulationRedShift(float[] color, float r, float g, float b, int num)
	{
		color[4*num] = color[4*num]*r;
		color[4*num+1] = color[4*num]*g;
		color[4*num+2] = color[4*num]*b;
		//color[4*num+3] = color[4*num]*a;
	}
	
	private static void colorModulation(float[] color, float r, float g, float b, int num)
	{
		color[4*num] = color[4*num+0]*r;
		color[4*num+1] = color[4*num+1]*g;
		color[4*num+2] = color[4*num+2]*b;
		//color[4*num+3] = color[4*num]*a;
	}
}
