package ext.technicalconditions.util.third;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
//import org.dom4j.Element;

import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.ptc.cipjava.intdict;

public class ESignature {
	
	private static Logger logger = Logger.getLogger(ESignature.class);
	public static BaseFont BASEFONT;
	public static Font FONT = null;
	public static Font BOLDFONT = null;
	public static Font BIGFONT = null;
	static {
		// 初始化字体
		try {
			BASEFONT = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
			FONT = new Font(BASEFONT, 12f, Font.NORMAL);
			BOLDFONT = new Font(BASEFONT, 12f, Font.BOLD);
			BIGFONT = new Font(BASEFONT, 18f, Font.BOLD);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		Map<String, List<String>> coordinateMap = readXMLtoCoordinate();
		for (String key : coordinateMap.keySet()) {
			if(key.equals("bianzhi")){
				coordinateMap.get(key).get(1);
				int x = Integer.parseInt(coordinateMap.get(key).get(0), 10);
				int y = Integer.parseInt(coordinateMap.get(key).get(1), 10);
//				System.out.println(x);
//				System.out.println(y);
				insertTextToPdf("D:/itextpdf/问题报告.pdf","D:/itextpdf/问题报告7.pdf",1, "刘砚凯",20,x,y);
			}
		    //List<String> str = coordinateMapKey.get(in);//得到每个key多对用value的值
		}
		//insertTextToPdf("D:/itextpdf/问题报告.pdf","D:/itextpdf/问题报告2.pdf",1, "刘砚凯",30,150,150);
		
		
		
		
	}
	
	public static Map<String, List<String>> readXMLtoCoordinate() throws DocumentException{
		
		//被读取xml文件的路径
		String path = "src/ext/technicalconditions/util/third/Test.xml";
		Map<String, List<String>> coordinate = new HashMap<String, List<String>>();
		
		//创建SAXReader对象用于读取xml文件
		SAXReader reader = new SAXReader();
		//读取xml文件，获得Document对象
		Document doc = reader.read(new File(path));
		//获取根元素
		org.dom4j.Element root = doc.getRootElement();
		//System.out.println(root.getName()+" = "+root.getStringValue());
		//4.获取根元素下的所有子元素（通过迭代器）
        Iterator<Element> it = root.elementIterator();
        while(it.hasNext()){
        	List<String> xy = new ArrayList<>();
        	org.dom4j.Element e = (org.dom4j.Element) it.next();
            xy.add((String) e.element("x").getData());
            xy.add((String) e.element("y").getData());
            coordinate.put(e.getName(), xy);
        }
        //System.out.println(coordinate);

		return coordinate;
		
	}
	
	
	
	
	
	/**
	 * 读取源pdf文件,在指定页码写入文字,只写一次,如果有多项内容,应一次获取PdfContentByte后循环执行写入
	 * @description
	 * @param sourcePDFFile 源pdf文件全路径
	 * @param outPutPDFPath 生成的pdf文件全路径,不能和源文件路径相同
	 * @param pageNo 页码
	 * @param text   文字内容
	 * @param fontSize 字体大小
	 * @param X x坐标
	 * @param Y y坐标
	 * @throws Exception
	 */
	public static void insertTextToPdf(String sourcePDFFile,String outPutPDFPath,int pageNo,String text,int fontSize,int X,int Y) throws Exception{
		PdfReader reader = null;
		PdfStamper stamp = null;
		try{
			reader 				= new PdfReader(sourcePDFFile);
			stamp 				= new PdfStamper(reader,new FileOutputStream(outPutPDFPath));
			PdfContentByte overContent = stamp.getOverContent(pageNo);
			insertTextToPdf(text,fontSize,X,Y,overContent);
		}finally{
			if(stamp != null){
				stamp.close();
			}
			if(reader != null){
				 reader.close(); 
			}
		}
	}
	
	/**
	 * 向pdf指定位置写入文字
	 * @description
	 * @param text     文字内容
	 * @param fontSize 字体大小
	 * @param X        x坐标
	 * @param Y        y坐标
	 * @param over     pdf指定页对象
	 */
	public static void insertTextToPdf(String text,int fontSize,int X,int Y,PdfContentByte overContent){
		logger.debug("写入内容到PDF文件,text:" + text + ",fontSize:" + fontSize + ",X坐标:" + X + ",Y坐标:" + Y ); 
		overContent.setFontAndSize(BASEFONT, fontSize);   
		overContent.setTextMatrix(30, 30);
		overContent.beginText();  
		overContent.showTextAligned(Element.ALIGN_LEFT,text, X, Y, 0);   
		overContent.endText();
	}
	

}
