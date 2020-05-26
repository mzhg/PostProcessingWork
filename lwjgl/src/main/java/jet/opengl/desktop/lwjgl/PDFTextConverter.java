package jet.opengl.desktop.lwjgl;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class PDFTextConverter {

	private static String getTextFromClipBoard(){
		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable t = clip.getContents(null);
		try {
			return (String) t.getTransferData(DataFlavor.stringFlavor);
		} catch (UnsupportedFlavorException e) {
			return "";
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	public static void main(String[] args) {
		String source = getTextFromClipBoard();
		
		if(source.length() == 0)
			return;
		
		source = source.replaceAll("\n", " ");
		
		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		clip.setContents(new StringSelection(source), null);
		
		System.out.println(source);
		
		
//		double v = 3/Math.sqrt(36) * 1.96;
//		System.out.println(v);
	}
}
