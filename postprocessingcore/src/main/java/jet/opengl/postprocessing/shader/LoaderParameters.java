package jet.opengl.postprocessing.shader;

import jet.opengl.postprocessing.util.FileLoader;

public class LoaderParameters {

	public String filepath;
	public boolean classFile;
	public boolean includeDefaultHeader = false;
	
	public boolean igoreComment = true;
	public String charset = "utf-8";
	public FileLoader fileLoader = null;

}
