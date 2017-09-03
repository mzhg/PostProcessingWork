package assimp.importer.xfile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

final class XFileTest {

	public static void main(String[] args) {
//		String filepath = "D:\\SDK\\assimp\\assimp-3.1.1\\test\\models\\X\\test_cube_compressed.x";
//		try {
//			ByteBuffer buffer = FileUtils.loadBinary(new File(filepath), false);
//			System.out.println("remaing = " + buffer.remaining());
//			XFileParser parser = new XFileParser(buffer);
//			System.out.println(parser.getImportedData());
//			
////			parser.getImportedData().printMeshes();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
//		byte[] bytes = new byte[2];
//		AssUtil.getBytes((short)7936, bytes, 0);
//		System.out.println(Arrays.toString(bytes));
//		System.out.println(AssUtil.getShort(bytes, 0));
//		System.out.println(AssUtil.getShortBE(bytes, 0));
		
		try {
			FileInputStream filein = new FileInputStream(new File("test_data.x"));
//			CheckedInputStream checkedInput = new CheckedInputStream(filein, new CRC32());
//			ZipInputStream in = new ZipInputStream(checkedInput);
//			if(in.getNextEntry() != null){
//				ByteArrayOutputStream out = new ByteArrayOutputStream(10000);
//				byte[] tmp = new byte[256];
//				int len;
//				while((len = in.read(tmp)) != -1){
//					System.out.println("len = " + len);
//					out.write(tmp, 0, len);
//				}
//				byte[] array = out.toByteArray();
//				System.out.println(array.length);
//			}
//			
//			
//			in.close();
			
//			BZip2CompressorInputStream in = new BZip2CompressorInputStream(filein, true);
			ZipArchiveInputStream in = new ZipArchiveInputStream(filein);
			in.getNextZipEntry();
			ByteArrayOutputStream out = new ByteArrayOutputStream(10000);
			byte[] tmp = new byte[256];
			int len;
			while((len = in.read(tmp)) != -1){
				System.out.println("len = " + len);
				out.write(tmp, 0, len);
			}
			in.close();
			byte[] array = out.toByteArray();
			System.out.println(array.length);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
