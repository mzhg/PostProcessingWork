package assimp.importer.collada;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

final class SAXTest {
	
	public static void main(String[] args) {
		
		Myhandler hanlder = null;
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			parser.parse("test_res/saxtest.xml", hanlder = new Myhandler("stu"));
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(hanlder.getList());
	}

	private static class Myhandler extends DefaultHandler{
		Map<String, String> map = null;
		List<Map<String, String>> list = null;
		String currentTag = null;
		String currentValue = null;
		String nodeName = null;
		
		Attributes values;
		
		Myhandler(String nodeName){
			this.nodeName = nodeName;
		}
		
		public List<Map<String, String>> getList() { return list;}
		
		//开始解析文档，即开始解析XML根元素时调用该方法
		@Override
		public void startDocument() throws SAXException {
			System.out.println("--startDocument()--");
			//初始化Map
			list = new ArrayList<>();
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if(values == null){
				values = attributes;
			}else {
				System.out.println("values == attributes ? " + (values == attributes));
			}
			
			//判断正在解析的元素是不是开始解析的元素
			System.out.println("--startElement()--"+qName);
			if(qName.equals(nodeName)){
				map = new HashMap<>();
				
				System.out.println("id = " + attributes.getValue("id"));
			}
			
			//判断正在解析的元素是否有属性值,如果有则将其全部取出并保存到map对象中，如:<person id="00001"></person>
			if(attributes != null && map != null){
				for(int i = 0; i < attributes.getLength(); i++){
					map.put(attributes.getQName(i), attributes.getValue(i));
				}
			}
			
			currentTag = qName;  // 正在解析的元素
		}
		
		@Override
		public void characters(char[] ch, int start, int length){
			System.out.println("--characters()--");
			if(currentTag != null && map != null){
				currentValue = new String(ch, start, length);
				//如果内容不为空和空格，也不是换行符则将该元素名和值和存入map中
				if(currentValue!=null&&!currentValue.trim().equals("")&&!currentValue.trim().equals("\n")){
					map.put(currentTag, currentValue);
					 System.out.println("-----"+currentTag+" "+currentValue);
				}
				
				//当前的元素已解析过，将其置空用于下一个元素的解析
				currentTag=null;
				currentValue = null;
			}
		}
		
		//每个元素结束的时候都会调用该方法
		@Override
		public void endElement(String uri, String localName, String qName) {
			System.out.println("--endElement()--"+qName);
			//判断是否为一个节点结束的元素标签
			if(qName.equals(nodeName)){
				list.add(map);
				map = null;
			}
		}
		
		//结束解析文档，即解析根元素结束标签时调用该方法
		@Override
		public void endDocument(){
			System.out.println("--endDocument()--");
		}
	}
}
