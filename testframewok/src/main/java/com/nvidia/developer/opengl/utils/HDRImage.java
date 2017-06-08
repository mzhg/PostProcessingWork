package com.nvidia.developer.opengl.utils;


import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;

public class HDRImage {

	protected int _width;
	protected int _height;
	protected int _depth;
	protected int _levelCount;
	protected int _faces;
	protected int _format = GLenum.GL_RGBA;
	protected int _internalFormat = GLenum.GL_RGBA;
	protected int _type = GLenum.GL_UNSIGNED_BYTE;
	protected int _elementSize;
	
	/** pointers to the levels */
	protected final List<float[]> _data = new ArrayList<float[]>(6);
	
	/**return the width of the image */
	public int getWidth() { return _width; }

	/** return the height of the image */
	public int getHeight() { return _height; }

	/** return the dpeth of the image (0 for images with no depth) */
	public int getDepth() { return _depth; }

	/** return the number of mipmap levels available for the image */
	public int getMipLevels() { return _levelCount; }

	/** return the number of cubemap faces available for the image (0 for non-cubemap images) */
	public int getFaces() { return _faces; }

	/** return the format of the image data (GL_RGB, GL_BGR, etc) */
	public int getFormat() { return _format; }

	/** return the suggested internal format for the data */
	public int getInternalFormat() { return _internalFormat; }

	/** return the type of the image data */
	public int getType() { return _type; }

	/** return the Size in bytes of a level of the image */
	public int getImageSize(int level){
		int w = _width >> level;
	    int h = _height >> level;
	    int d = _depth >> level;
	    w = (w > 0) ? w : 1;
	    h = (h > 0) ? h : 1;
	    d = (d > 0) ? d : 1;

	    return w*h*d*_elementSize;
	}
	
	/** return the Size in bytes of the 0 level of the image */
	public int getImageSize(){
		int w = _width;
	    int h = _height;
	    int d = _depth;
	    w = (w > 0) ? w : 1;
	    h = (h > 0) ? h : 1;
	    d = (d > 0) ? d : 1;

	    return w*h*d*_elementSize;
	}


	/** return whether the image represents a cubemap */
	public boolean isCubeMap() { return _faces > 0; }

	/** return whether the image represents a volume */
	public boolean isVolume() { return _depth > 0; }

	/** get a pointer to level data */
	public float[] getLevel( int level, int face){
		assert( level < _levelCount);
	    assert( _faces == 0 || ( face >= GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X && face <= GLenum.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z));
	    
	    face = face - GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X;

	    assert( (face*_levelCount + level) < (int)_data.size());
	    return _data.get( face*_levelCount + level);
	}
	
	public float[] getLevel(int level){
		return getLevel(level, GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X);
	}
	
	public boolean loadHDRIFromFile(String file){
		Dimension size = new Dimension();
		
		try {
			BufferedInputStream in = new BufferedInputStream(FileUtils.open(file));
			rgbe_header_info header = new rgbe_header_info();
			
			if(RGBE_ReadHeader(in, size, header) != 0){
				System.err.println(file + ": read header error!");
				in.close();
				return false;
			}
			
			float[] data = new float[size.getWidth() * size.getHeight() * 3];
			
			if(RGBE_ReadPixels_RLE(in, data, 0, size.getWidth(), size.getHeight()) != 0){
				System.err.println(file + ": read pixles data error!");
				in.close();
				return false;
			}
			
			//set all the parameters
		    _width = size.getWidth();
		    _height = size.getHeight();
		    _depth = 0;
		    _levelCount = 1;
		    _type = GLenum.GL_FLOAT;
		    _format = GLenum.GL_RGB;
			// GL_RGB32F_ARB 0x8815
		    _internalFormat = 0x8815;
		    _faces = 0;
		    _elementSize = 12;
		    _data.add( data);

		    //hdr images come in upside down
		    flipSurface( data, _width, _height, _depth);
		    in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	protected void flipSurface(float[] surf, int width, int height, int depth){
		int lineSize;

	    if(depth == 0)
	    	depth = 1;

	    lineSize = _elementSize * width/4;
	    int sliceSize = lineSize * height;
	    
	    float[] tempBuf = new float[lineSize];
	    
	    for ( int ii = 0; ii < depth; ii++) {
	        int top =  ii*sliceSize;
	        int bottom = top + (sliceSize - lineSize);
	    
	        for ( int jj = 0; jj < (height >> 1); jj++) {
//	            memcpy( tempBuf, top, lineSize);
//	            memcpy( top, bottom, lineSize);
//	            memcpy( bottom, tempBuf, lineSize);
	        	
	        	System.arraycopy(surf, top, tempBuf, 0, lineSize);
	        	System.arraycopy(surf, bottom, surf, top, lineSize);
	        	System.arraycopy(tempBuf, 0, surf, bottom, lineSize);

	            top += lineSize;
	            bottom -= lineSize;
	        }
	    }
	}
	
	public boolean convertCrossToCubemap(){
		//can't already be a cubemap
	    if (isCubeMap())
	        return false;

	    //mipmaps are not supported
	    if (_levelCount != 1)
	        return false;

	    //this function only supports vertical cross format for now (3 wide by 4 high)
	    if (  (_width / 3 != _height / 4) || (_width % 3 != 0) || (_height % 4 != 0) || (_depth != 0))
	        return false;
	    
	    int fWidth = _width / 3;
	    int fHeight = _height / 4;
	    int _elementSize = this._elementSize / 4;

	    float[] data = _data.get(0);
	    //remove the old pointer from the vector
	    _data.remove(0);
	    
	    float[] face = new float[fWidth * fHeight * _elementSize];
	    int ptr;
	    
	 // positive X
	    ptr = 0;
	    for (int j=0; j<fHeight; j++) {
//	        memcpy( ptr, &data[((_height - (fHeight + j + 1))*_width + 2 * fWidth) * _elementSize], fWidth*_elementSize);
	    	System.arraycopy(data, ((_height - (fHeight + j + 1))*_width + 2 * fWidth) * _elementSize, face, ptr, fWidth*_elementSize);
	        ptr += fWidth*_elementSize;
	    }
	    _data.add(face);
	    
	 // negative X
	    face = new float[ fWidth * fHeight * _elementSize];
	    ptr = 0;
	    for (int j=0; j<fHeight; j++) {
//	        memcpy( ptr, &data[(_height - (fHeight + j + 1))*_width*_elementSize], fWidth*_elementSize);
	    	System.arraycopy(data, (_height - (fHeight + j + 1))*_width*_elementSize, face, ptr, fWidth*_elementSize);
	        ptr += fWidth*_elementSize;
	    }
	    _data.add(face);
	    
	    // positive Y
	    face = new float[ fWidth * fHeight * _elementSize];
	    ptr = 0;
	    for (int j=0; j<fHeight; j++) {
//	        memcpy( ptr, &data[((4 * fHeight - j - 1)*_width + fWidth)*_elementSize], fWidth*_elementSize);
	    	System.arraycopy(data, ((4 * fHeight - j - 1)*_width + fWidth)*_elementSize, face, ptr, fWidth*_elementSize);
	        ptr += fWidth*_elementSize;
	    }
	    _data.add(face);

	    // negative Y
	    face = new float[ fWidth * fHeight * _elementSize];
	    ptr = 0;
	    for (int j=0; j<fHeight; j++) {
//	        memcpy( ptr, &data[((2*fHeight - j - 1)*_width + fWidth)*_elementSize], fWidth*_elementSize);
	    	System.arraycopy(data, ((2*fHeight - j - 1)*_width + fWidth)*_elementSize, face, ptr, fWidth*_elementSize);
	        ptr += fWidth*_elementSize;
	    }
	    _data.add(face);

	    // positive Z
	    face = new float[ fWidth * fHeight * _elementSize];
	    ptr = 0;
	    for (int j=0; j<fHeight; j++) {
//	        memcpy( ptr, &data[((_height - (fHeight + j + 1))*_width + fWidth) * _elementSize], fWidth*_elementSize);
	    	System.arraycopy(data, ((_height - (fHeight + j + 1))*_width + fWidth) * _elementSize, face, ptr, fWidth*_elementSize);
	        ptr += fWidth*_elementSize;
	    }
	    _data.add(face);

	    // negative Z
	    face = new float[ fWidth * fHeight * _elementSize];
	    ptr = 0;
	    for (int j=0; j<fHeight; j++) {
	        for (int i=0; i<fWidth; i++) {
//	            memcpy( ptr, &data[(j*_width + 2 * fWidth - (i + 1))*_elementSize], _elementSize);
	        	System.arraycopy(data, (j*_width + 2 * fWidth - (i + 1))*_elementSize, face, ptr, _elementSize);
	            ptr += _elementSize;
	        }
	    }
	    _data.add(face);

	    //set the new # of faces, width and height
	    _faces = 6;
	    _width = fWidth;
	    _height = fHeight;
	    
	    return true;
	}
	
	public static void fp32toFp16(float[] pt, int pt_offset, short[] out, int out_offset, int width, int height)
	{
		for (int i=0;i<width*height;i++) {
			out[i*3 + out_offset] = Numeric.convertFloatToHFloat(pt[i*3 + pt_offset]);
			out[i*3+1 + out_offset] = Numeric.convertFloatToHFloat(pt[i*3+1 + pt_offset]);
			out[i*3+2 + out_offset] = Numeric.convertFloatToHFloat(pt[i*3+2] + pt_offset);
		}
	}
	
	public static void fp32toFp16(float[] pt, int pt_offset, short[] out, int out_offset, int length)
	{
		for (int i=0;i<length;i++) {
			out[i] = Numeric.convertFloatToHFloat(pt[i]);
		}
	}

	// //////////////////////////////////////////////////////////////
	// --------------------------- rgbe ---------------------------//
	/*
	 * This file contains code to read and write four byte rgbe file format
	 * developed by Greg Ward. It handles the conversions between rgbe and
	 * pixels consisting of floats. The data is assumed to be an array of
	 * floats. By default there are three floats per pixel in the order red,
	 * green, blue. (RGBE_DATA_??? values control this.) Only the mimimal header
	 * reading and writing is implemented. Each routine does error checking and
	 * will return a status value as defined below. This code is intended as a
	 * skeleton so feel free to modify it to suit your needs.
	 * 
	 * (Place notice here if you modified the code.) Modified by NVIDIA to
	 * support Android, May 2013
	 * 
	 * posted to http://www.graphics.cornell.edu/~bjw/ written by Bruce Walter
	 * (bjw@graphics.cornell.edu) 5/26/95 based on code written by Greg Ward
	 */
	/* flags indicating which fields in an rgbe_header_info are valid */
	private static final int RGBE_VALID_PROGRAMTYPE = 0x01;
	private static final int RGBE_VALID_GAMMA = 0x02;
	private static final int RGBE_VALID_EXPOSURE = 0x04;

	/* return codes for rgbe routines */
	private static final int RGBE_RETURN_SUCCESS = 0;
	private static final int RGBE_RETURN_FAILURE = -1;

	private static final int RGBE_DATA_RED = 0;
	private static final int RGBE_DATA_GREEN = 1;
	private static final int RGBE_DATA_BLUE = 2;
	/* number of floats per pixel */
	private static final int RGBE_DATA_SIZE = 3;

	private static final int rgbe_read_error = 0;
	private static final int rgbe_write_error = 1;
	private static final int rgbe_format_error = 2;
	private static final int rgbe_memory_error = 3;

	/* default error routine. change this to change error handling */
	static int rgbe_error(int rgbe_error_code, String msg) {
		switch (rgbe_error_code) {
		case rgbe_read_error:
			System.err.println("RGBE read error");
			break;
		case rgbe_write_error:
//			NvLogger.e("RGBE write error");
			break;
		case rgbe_format_error:
//			NvLogger.e("RGBE bad file format: %s\n", msg);
			System.err.println("RGBE bad file format: " + msg);
			break;
		default:
		case rgbe_memory_error:
//			NvLogger.e("RGBE error: %s\n", msg);
			System.err.println("RGBE error: " + msg);
		}
		return RGBE_RETURN_FAILURE;
	}

	/* standard conversion from float pixels to rgbe pixels */
	/* note: you can remove the "inline"s if your compiler complains about it */
	static void float2rgbe(byte[] rgbe, float red, float green, float blue) {
		float v;
		int e;

		v = red;
		if (green > v)
			v = green;
		if (blue > v)
			v = blue;
		if (v < 1e-32) {
			rgbe[0] = rgbe[1] = rgbe[2] = rgbe[3] = 0;
		} else {
			// v = (float)frexp(v,&e) * 256.0f/v;
			e = Numeric.getExponent(v) + 1;
			v = v / Numeric.ldexp(1.0f, e) * 256.0f / v;
			rgbe[0] = (byte) (red * v);
			rgbe[1] = (byte) (green * v);
			rgbe[2] = (byte) (blue * v);
			rgbe[3] = (byte) (e + 128);
		}
	}

	/* standard conversion from rgbe to float pixels */
	/* note: Ward uses ldexp(col+0.5,exp-(128+8)). However we wanted pixels */
	/* in the range [0,1] to map back into the range [0,1]. */
	static void rgbe2float(float[] rgb, int offset, byte[] rgbe) {
		float f;

		if (rgbe[3] != 0) { /* nonzero pixel */
			f = (float) Numeric.ldexp(1.0, (rgbe[3] & 0xFF) - (int) (128 + 8));
			rgb[0 + offset] = (rgbe[0] & 0xff) * f;
			rgb[1 + offset] = (rgbe[1] & 0xff) * f;
			rgb[2 + offset] = (rgbe[2] & 0xff) * f;
		} else
			rgb[0 + offset] = rgb[1 + offset] = rgb[2 + offset] = 0.0f;
	}

	static int nvFGets(InputStream in, byte[] buf) {
		int size = 0;
		int v;
		int i = 0;
		try {
			while (i++ < buf.length && (v = in.read()) != -1) {
				buf[size++] = (byte) v;

				if (v == '\r' || v == '\n')
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return size;
	}

	/* minimal header reading. modify if you want to parse more information */
	static int RGBE_ReadHeader(InputStream in, Dimension size,
			rgbe_header_info info) {
		byte[] buf = new byte[128];
		int found_format;
		float tempf;
		int i;
		int rsize;

		found_format = 0;
		info.valid = 0;
		info.programtype[0] = 0;
		info.gamma = info.exposure = 1.0f;

		if ((rsize = nvFGets(in, buf)) == 0) {
			return rgbe_error(rgbe_read_error, null);
		}

		if ((buf[0] != '#') || (buf[1] != '?')) {
			/*
			 * if you want to require the magic token then uncomment the next
			 * line
			 */
			/* return rgbe_error(rgbe_format_error,"bad initial token"); */
		} else {
			info.valid |= RGBE_VALID_PROGRAMTYPE;
			for (i = 0; i < 16 - 1; i++) {
				char c = (char) (buf[i + 2] & 0xFF);
				if ((c == 0) || Character.isSpace(c))
					break;
				info.programtype[i] = (byte) c;
			}
			info.programtype[i] = 0;
			// if (NvFGets(buf,sizeof(buf)/sizeof(buf[0]),fp) == 0)
			// return rgbe_error(rgbe_read_error,NULL);

			if ((rsize = nvFGets(in, buf)) == 0) {
				return rgbe_error(rgbe_read_error, null);
			}
		}

		for (;;) {
			if ((buf[0] == 0) || (buf[0] == '\n'))
				return rgbe_error(rgbe_format_error,
						"no FORMAT specifier found");
			// else
			String _buf = new String(buf, 0, rsize);
			if (_buf.equals("FORMAT=32-bit_rle_rgbe\n"))
				break; /* format found so break out of loop */
			// else if (info && (sscanf(buf,"GAMMA=%g",&tempf) == 1)) {
			// info->gamma = tempf;
			// info->valid |= RGBE_VALID_GAMMA;
			// }
			// else if (info && (sscanf(buf,"EXPOSURE=%g",&tempf) == 1)) {
			// info->exposure = tempf;
			// info->valid |= RGBE_VALID_EXPOSURE;
			// }

			int index = _buf.indexOf('=');
			if (index >= 0) {
				String[] strs = _buf.split("=");
				if (strs[0].equals("GAMMA")) {
					info.gamma = Float.parseFloat(strs[1]);
					info.valid |= RGBE_VALID_GAMMA;
				} else if (strs[0].equals("EXPOSURE")) {
					info.exposure = Float.parseFloat(strs[1]);
					info.valid |= RGBE_VALID_EXPOSURE;
				}
			}

			if ((rsize = nvFGets(in, buf)) == 0) {
				return rgbe_error(rgbe_read_error, null);
			}
		}

		for (;;) {
			if ((rsize = nvFGets(in, buf)) == 0) {
				return rgbe_error(rgbe_read_error, null);
			}

			String _buf = new String(buf, 0, rsize);
			// if (sscanf(buf,"-Y %d +X %d",height,width) == 2)
			// break;
			if (_buf.startsWith("-Y")) {
				String[] strs = _buf.split(" ");
//				int k = 0;
//				for(String str : strs){
//					System.out.println("str" + (k++) + ": " + str);
//				}
				if (strs.length >= 4) {
					size.setHeight(Integer.parseInt(strs[1]));
					size.setWidth(Integer.parseInt(strs[3].substring(0, strs[3].length() - 1))); // delete the last char '\n'
					break;
				}
			}
		}
		return RGBE_RETURN_SUCCESS;
	}

	static int RGBE_ReadPixels(InputStream fp, float[] data, int offset,
			int numpixels) {
		byte[] rgbe = new byte[4];

		try {
			while (numpixels-- > 0) {
				if (fp.read(rgbe) < 4)
					return rgbe_error(rgbe_read_error, null);

				rgbe2float(data, offset, rgbe);
				offset += RGBE_DATA_SIZE;

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return RGBE_RETURN_SUCCESS;
	}

	static int RGBE_ReadPixels_Raw(InputStream in, byte[] data, int numpixels) {
		numpixels *= 4;

		try {
			if (in.read(data, 0, numpixels) < numpixels)
				return rgbe_error(rgbe_read_error, null);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return RGBE_RETURN_SUCCESS;
	}

	static int nvRead(InputStream in, byte[] data) {
		try {
			return in.read(data);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return 0;
	}

	static int nvRead(InputStream in, byte[] data, int offset, int len) {
		try {
			return in.read(data, offset, len);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return 0;
	}

	static int RGBE_ReadPixels_RLE(InputStream in, float[] data, int data_offset, int scanline_width, int num_scanlines) {
		byte[] rgbe = new byte[4];
		int i, count;
		byte[] buf = new byte[2];

		if ((scanline_width < 8) || (scanline_width > 0x7fff))
			/* run length encoding is not allowed so read flat */
			return RGBE_ReadPixels(in, data, data_offset, scanline_width * num_scanlines);

		byte[] scanline_buffer = null;
		int ptr, ptr_end;
		/* read in each successive scanline */
		while (num_scanlines > 0) {
			if (nvRead(in, rgbe) < 4) {
				return rgbe_error(rgbe_read_error, null);
			}

			if ((rgbe[0] != 2) || (rgbe[1] != 2) || ((rgbe[2] & 0x80) != 0)) {
				/* this file is not run length encoded */
				rgbe2float(data, data_offset, rgbe);
				data_offset += RGBE_DATA_SIZE;
				// free(scanline_buffer);
				return RGBE_ReadPixels(in, data, data_offset, scanline_width * num_scanlines - 1);
			}

			if ((((int) (rgbe[2] & 0xFF) ) << 8 | (rgbe[3] &0xFF)) != scanline_width) {
				return rgbe_error(rgbe_format_error, "wrong scanline width");
			}

			if (scanline_buffer == null)
				scanline_buffer = new byte[4 * scanline_width];

			ptr = 0;
			/* read each of the four channels for the scanline into the buffer */
			for (i = 0; i < 4; i++) {
				ptr_end = (i + 1) * scanline_width;
				while (ptr < ptr_end) {
					if (nvRead(in, buf) < buf.length)
						return rgbe_error(rgbe_read_error, null);

					if ((buf[0] & 0xFF) > 128) {
						/* a run of the same value */
						count = (buf[0] & 0xFF) - 128;
						if ((count == 0) || (count > ptr_end - ptr)) {
							return rgbe_error(rgbe_format_error, "bad scanline data");
						}
						while (count-- > 0)
							// *ptr++ = buf[1];
							scanline_buffer[ptr++] = buf[1];
					} else {
						/* a non-run */
						count = buf[0] & 0xFF;
						if ((count == 0) || (count > ptr_end - ptr)) {
							return rgbe_error(rgbe_format_error, "bad scanline data");
						}
						// *ptr++ = buf[1];
						scanline_buffer[ptr++] = buf[1];
						if (--count > 0) {
							// if (NvFRead(ptr,sizeof(*ptr)*count,1,fp) < 1) {
							if (nvRead(in, scanline_buffer, ptr, count) < count) {
								return rgbe_error(rgbe_read_error, null);
							}
							ptr += count;
						}
					}
				}
			}

			/* now convert data from buffer into floats */
			for (i = 0; i < scanline_width; i++) {
				rgbe[0] = scanline_buffer[i];
				rgbe[1] = scanline_buffer[i + scanline_width];
				rgbe[2] = scanline_buffer[i + 2 * scanline_width];
				rgbe[3] = scanline_buffer[i + 3 * scanline_width];
				rgbe2float(data, data_offset, rgbe);
				data_offset += RGBE_DATA_SIZE;
			}
			num_scanlines--;
		}

		return RGBE_RETURN_SUCCESS;
	}

	static int RGBE_ReadPixels_Raw_RLE(InputStream in, byte[] data,
			int data_offset, int scanline_width, int num_scanlines) {
		byte[] rgbe = new byte[4];
		byte[] scanline_buffer = null;
		int ptr, ptr_end;
		int i, count;
		byte[] buf = new byte[2];

		if ((scanline_width < 8) || (scanline_width > 0x7fff))
			/* run length encoding is not allowed so read flat */
			return RGBE_ReadPixels_Raw(in, data, scanline_width * num_scanlines);

		/* read in each successive scanline */
		while (num_scanlines > 0) {
			if (nvRead(in, rgbe) < 4) {
				return rgbe_error(rgbe_read_error, null);
			}

			if ((rgbe[0] != 2) || (rgbe[1] != 2) || (rgbe[2] & 0x80) != 0) {
				/* this file is not run length encoded */
				data[0 + data_offset] = rgbe[0];
				data[1 + data_offset] = rgbe[1];
				data[2 + data_offset] = rgbe[2];
				data[3 + data_offset] = rgbe[3];
				data_offset += RGBE_DATA_SIZE;
				return RGBE_ReadPixels_Raw(in, data, scanline_width
						* num_scanlines - 1);
			}

			if ((((int) (rgbe[2] & 0xFF)) << 8 | (rgbe[3] & 0xFF)) != scanline_width) {
				return rgbe_error(rgbe_format_error, "wrong scanline width");
			}

			if (scanline_buffer == null)
				scanline_buffer = new byte[4 * scanline_width];

			ptr = 0;
			/* read each of the four channels for the scanline into the buffer */
			for (i = 0; i < 4; i++) {
				ptr_end = (i + 1) * scanline_width;
				while (ptr < ptr_end) {
					if (nvRead(in, buf) < 2)
						return rgbe_error(rgbe_read_error, null);

					if ((buf[0] & 0xFF) > 128) {
						/* a run of the same value */
						count = (buf[0] & 0xFF) - 128;
						if ((count == 0) || (count > ptr_end - ptr)) {
							return rgbe_error(rgbe_format_error,
									"bad scanline data");
						}
						while (count-- > 0)
							// *ptr++ = buf[1];
							scanline_buffer[ptr++] = buf[1];
					} else {
						/* a non-run */
						count = buf[0];
						if ((count == 0) || (count > ptr_end - ptr)) {
							return rgbe_error(rgbe_format_error,
									"bad scanline data");
						}
						// *ptr++ = buf[1];
						scanline_buffer[ptr++] = buf[1];
						if (--count > 0) {
							// if (NvFRead(ptr,sizeof(*ptr)*count,1,fp) < 1) {
							if (nvRead(in, scanline_buffer, ptr, count) < count) {
								// free(scanline_buffer);
								return rgbe_error(rgbe_read_error, null);
							}
							ptr += count;
						}
					}
				}
			}

			/* copy byte data to output */
			for (i = 0; i < scanline_width; i++) {
				data[0 + data_offset] = scanline_buffer[i];
				data[1 + data_offset] = scanline_buffer[i + scanline_width];
				data[2 + data_offset] = scanline_buffer[i + 2 * scanline_width];
				data[3 + data_offset] = scanline_buffer[i + 3 * scanline_width];
				data_offset += 4;
			}
			num_scanlines--;
		}

		return RGBE_RETURN_SUCCESS;
	}

	private static final class rgbe_header_info {
		int valid; /* indicate which fields are valid */
		byte[] programtype = new byte[16]; /*
											 * listed at beginning of file to
											 * identify it after "#?". defaults
											 * to "RGBE"
											 */
		float gamma; /*
					 * image has already been gamma corrected with given gamma.
					 * defaults to 1.0 (no correction)
					 */
		float exposure; /*
						 * a value of 1.0 in an image corresponds to <exposure>
						 * watts/steradian/m^2. defaults to 1.0
						 */
	}
	// --------------------- rgbe end --------------------------------//
	// //////////////////////////////////////////////////////////////////
}
