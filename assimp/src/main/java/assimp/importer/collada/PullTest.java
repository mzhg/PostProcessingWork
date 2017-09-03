package assimp.importer.collada;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

final class PullTest {

	public static void main(String[] args) throws XmlPullParserException, IOException {
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser pullParser = new KXmlParser()/*factory.newPullParser()*/;
		
		Map<String, String> map = null;
		List<Map<String, String>> list = null;
		String currentTag = null;
		
		pullParser.setInput(new FileInputStream("test_res/saxtest.xml"), null);
		int event = pullParser.next();
		while(event != XmlPullParser.END_DOCUMENT){
			switch (event) {
			case XmlPullParser.START_DOCUMENT:
				System.out.println("---- stat document ----");
				list = new ArrayList<Map<String,String>>();
				currentTag = pullParser.getName();
				break;
			case XmlPullParser.START_TAG:
				currentTag = pullParser.getName();
				if(currentTag.equals("stu")){
					map = new HashMap<String, String>();
					int count = pullParser.getAttributeCount();
					for(int i = 0; i < count; i++){
						map.put(pullParser.getAttributeName(i), pullParser.getAttributeValue(i));
					}
				}
				
				if(currentTag.equals("sex") || currentTag.equals("name") || currentTag.equals("age"))
					map.put(currentTag, pullParser.nextText());
				
				break;
			case XmlPullParser.TEXT:
				if(pullParser.getText() != null)
					System.out.println("text = " + pullParser.getText());
				break;
			case XmlPullParser.END_TAG:
				if(map != null && pullParser.getName().equals("stu"))
					list.add(map);
				map = null;
				break;
			case XmlPullParser.END_DOCUMENT:
				System.out.println("---- end document ----");
				break;
			default:
				break;
			}
			
			event = pullParser.next();
		}
		((Closeable)pullParser).close();
		System.out.println(list);
	}
}
