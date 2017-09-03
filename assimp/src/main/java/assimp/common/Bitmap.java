package assimp.common;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Defines bitmap format helper for textures.<p>
 * Used for file formats which embed their textures into the model file.
 */
public class Bitmap {
	protected static final int HEADER_SIZE =
			AssUtil.SIZE_OF_SHORT +  // type
			AssUtil.SIZE_OF_INT +    // size
			AssUtil.SIZE_OF_SHORT +  // reserved1
			AssUtil.SIZE_OF_SHORT +  // reserved2
			AssUtil.SIZE_OF_INT;
	
	protected static final int DIB_SIZE = AssUtil.sizeof(DIB.class);
	protected static final int BYTES_PER_PIXEL = 4;
	private static byte[] padding_data = new byte[4];
	
	public static void save(Texture texture, File file){
		if(file != null) {
			Header header = new Header();
			DIB dib = new DIB();

			dib.size = DIB_SIZE;
			dib.width = texture.mWidth;
			dib.height = texture.mHeight;
			dib.planes = 1;
			dib.bits_per_pixel = 8 * BYTES_PER_PIXEL;
			dib.compression = 0;
			dib.image_size = (((dib.width * BYTES_PER_PIXEL) + 3) & 0x0000FFFC) * dib.height;
			dib.x_resolution = 0;
			dib.y_resolution = 0;
			dib.nb_colors = 0;
			dib.nb_important_colors = 0;

			header.type = 0x4D42; // 'BM'
			header.offset = HEADER_SIZE + DIB_SIZE;
			header.size = header.offset + dib.image_size;
			header.reserved1 = 0;
			header.reserved2 = 0;

			try(BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))){
				writeHeader(header, out);
				writeDIB(dib, out);
				writeData(texture, out);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected static void writeHeader(Header header, OutputStream out) throws IOException{
		byte[] data = new byte[4];
		int size;
		size = AssUtil.getBytes(header.type, data, 0); 		out.write(data, 0, size);
		size = AssUtil.getBytes(header.size, data, 0); 		out.write(data, 0, size);
		size = AssUtil.getBytes(header.reserved1, data, 0); out.write(data, 0, size);
		size = AssUtil.getBytes(header.reserved2, data, 0); out.write(data, 0, size);
		size = AssUtil.getBytes(header.offset, data, 0); 	out.write(data, 0, size);
		
	}

	protected static void writeDIB(DIB dib, OutputStream out) throws IOException{
		byte[] data = new byte[4];
		int size;
		size = AssUtil.getBytes(dib.size, data, 0); 	out.write(data, 0, size);
		size = AssUtil.getBytes(dib.width, data, 0); 	out.write(data, 0, size);
		size = AssUtil.getBytes(dib.height, data, 0);	out.write(data, 0, size);
		size = AssUtil.getBytes(dib.planes, data, 0); 	out.write(data, 0, size);
		size = AssUtil.getBytes(dib.bits_per_pixel, data, 0); out.write(data, 0, size);
		size = AssUtil.getBytes(dib.compression, data, 0); 	  out.write(data, 0, size);
		size = AssUtil.getBytes(dib.image_size, data, 0); 	  out.write(data, 0, size);
		size = AssUtil.getBytes(dib.x_resolution, data, 0);   out.write(data, 0, size);
		size = AssUtil.getBytes(dib.y_resolution, data, 0);   out.write(data, 0, size);
		size = AssUtil.getBytes(dib.nb_colors, data, 0);      out.write(data, 0, size);
		size = AssUtil.getBytes(dib.nb_important_colors, data, 0); out.write(data, 0, size);
	}

	protected static void writeData(Texture texture, OutputStream out) throws IOException{
		final int padding_offset = 4;
		final int padding = (padding_offset - ((BYTES_PER_PIXEL * texture.mWidth) % padding_offset)) % padding_offset;
		byte[] pixel = new byte[BYTES_PER_PIXEL];

		for(int i = 0; i < texture.mHeight; ++i) {
			for(int j = 0; j < texture.mWidth; ++j) {
//				const aiTexel& texel = texture.pcData[(texture.mHeight - i - 1) * texture.mWidth + j]; // Bitmap files are stored in bottom-up format
//
//				pixel[0] = texel.r;
//				pixel[1] = texel.g;
//				pixel[2] = texel.b;
//				pixel[3] = texel.a;
//
//				file.Write(pixel, mBytesPerPixel, 1);
				
				final int texel = texture.pcData.get((texture.mHeight - i - 1) * texture.mWidth + j);  // Bitmap files are stored in bottom-up format
				AssUtil.getBytes(texel, pixel, 0);
				out.write(pixel);
			}

			out.write(padding_data, 0, padding);
		}
	}
			
			
	protected static class Header{
		
		protected short type;
		protected int   size;
		protected short reserved1;
		protected short reserved2;
		protected int   offset;
	}
	
	protected static class DIB{
		protected int size;

		protected int width;

		protected int height;

		protected short planes;

		protected short bits_per_pixel;

		protected int compression;

		protected int image_size;

		protected int x_resolution;

		protected int y_resolution;

		protected int nb_colors;

		protected int nb_important_colors;
	}
}
