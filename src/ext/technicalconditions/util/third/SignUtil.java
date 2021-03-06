package ext.technicalconditions.util.third;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.ptc.wvs.server.util.Util;

import ext.wis.util.ContentUtil;
import ext.wis.util.DocUtil;
import ext.wis.util.IBAUtil;
import ext.wis.util.ObjectInfoUtil;
import ext.wis.util.WISPLMConstants;
import ext.wis.util.WTUtil;
import ext.wis.util.third.JsonUtils;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentHolder;
import wt.content.ContentItem;
import wt.content.ContentRoleType;
import wt.content.ContentServerHelper;
import wt.content.FormatContentHolder;
import wt.doc.WTDocument;
import wt.epm.EPMAuthoringAppType;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentType;
import wt.fc.ObjectReference;
import wt.fc.WTObject;
import wt.method.RemoteAccess;
import wt.representation.Representation;
import wt.representation.StandardRepresentationService;
import wt.util.WTException;
import wt.util.WTProperties;

public class SignUtil implements RemoteAccess {
	
	private static final int SIGN_PICTURE_LENGTH = 60;
	private static final int SIGN_PICTURE_WIDTH = 20;
	private static String SIGNIMAGE_FOLDERPATH = null;
	public static void main(String[] args) throws Exception {
		
	}
	
	private static Logger logger = Logger.getLogger(SignUtil.class);
	//??????????????????
	public static BaseFont bf;
	public static String fontPath;
	
	static{
		//???????????????-??????
		try {
			fontPath = WISPLMConstants.CODEBASE  + "ext" + File.separator + "wis" +  File.separator + "doc" +  File.separator + "font" +File.separator + "simsun.ttc";
	
			bf =  BaseFont.createFont(fontPath+",1", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
			//signImage ???????????????
			//SIGNIMAGE_FOLDERPATH = WISPLMConstants.CODEBASE + "ext"+File.separator+ "wisplm" + File.separator + "demo" + File.separator +"signImage"+File.separator;
		} catch (Exception e) {
			System.out.println(">>>>>>>>>>>>>>>?????????????????????");
			e.printStackTrace();
		}
	}
	
	/**
	 * ?????????pdf??????,???????????????????????????,????????????,?????????????????????,???????????????PdfContentByte?????????????????????
	 * @description
	 * @param sourcePDFFile ???pdf???????????????
	 * @param outPutPDFPath ?????????pdf???????????????,??????????????????????????????
	 * @param pageNo ??????
	 * @param text   ????????????
	 * @param fontSize ????????????
	 * @param X x??????
	 * @param Y y??????
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
	 * ???pdf????????????????????????
	 * @description
	 * @param text     ????????????
	 * @param fontSize ????????????
	 * @param X        x??????
	 * @param Y        y??????
	 * @param over     pdf???????????????
	 */
	public static void insertTextToPdf(String text,int fontSize,int X,int Y,PdfContentByte overContent){
		logger.debug("???????????????PDF??????,text:" + text + ",fontSize:" + fontSize + ",X??????:" + X + ",Y??????:" + Y ); 
		overContent.setFontAndSize(bf, fontSize);   
		overContent.setTextMatrix(30, 30);
		overContent.beginText();  
		overContent.showTextAligned(Element.ALIGN_LEFT,text, X, Y, 0);   
		overContent.endText();
	}
	
	/**
	 * ???PDF???????????????????????????????????????
	 * @param image
	 * @param fontSize
	 * @param X
	 * @param Y
	 * @param over
	 */
	public static void insertImageToPdf(Image image,int X,int Y,PdfContentByte overContent){
		try {
			image.scaleAbsolute(SIGN_PICTURE_LENGTH, SIGN_PICTURE_WIDTH);
			image.setAbsolutePosition(X-3,Y-7);
			overContent.addImage(image);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * ????????????????????????????????????
	 * 
	 * @param doc
	 * @return
	 * @throws WTException 
	 * @throws PropertyVetoException 
	 */
	public static boolean hasSign(WTDocument doc) throws WTException, PropertyVetoException{
		//????????????????????????pdf??????:??????_?????????_??????.pdf
		String signPDFName = getSignPDFName(doc);
		wt.content.ContentHolder contentHolder = ContentHelper.service.getContents(doc);
		Vector apps = ContentHelper.getApplicationData(contentHolder);
		for (Object object : apps) {
			ApplicationData appData = (ApplicationData) object;
			String fileName	        = appData.getFileName();
			if(signPDFName.equalsIgnoreCase(fileName)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * DWG??????pdf???????????????????????????????????????
	 * @param pdfFile
	 * @return
	 * @throws IOException
	 * @throws WTException 
	 */
	private static String  getDWGPdfPrintConfigKey(File pdfFile) throws IOException, WTException{
		PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());
		Rectangle rect = reader.getPageSize(1);
		float height1 = rect.getHeight();
		String size = "";
		int width = (int) rect.getWidth();
		if(width == PageSize.A4.getWidth()){
			size="A4_";
		}
		else if(width ==PageSize.A3.getWidth()){
			size="A3_";
		}else if(width ==PageSize.A2.getWidth()){
			size="A2_";
		}else if(width ==PageSize.A1.getWidth()){
			size="A1_";
		}else if(width ==PageSize.A0.getWidth()){
			size="A0_";
		}else{
			throw new WTException("??????????????????:" + pdfFile.getPath());
		}
		String hv = "";
		PdfDictionary  pdfDictionary = reader.getPageN(1);
		PdfNumber pdfnumber = pdfDictionary.getAsNumber(PdfName.ROTATE);
		if(pdfnumber != null && pdfnumber.intValue() == 0){
			hv = "V2"; //??????
		}else if(pdfnumber != null && pdfnumber.intValue() == 270){
			hv = "H2"; //??????
		}
		String result = "";
		if(!"".equals(size) && !"".equals(hv)){
			result =  size + hv;
		}
		logger.debug("DWGPDF??????????????????:????????????:"+ pdfFile.getName() + ",???-->" + height1 + ",???-->" + width + ",????????????:" + result);
		return result;
	}
	
	/**
	 * ??????????????????
	 * @param pbo
	 * @throws Exception
	 */
	public static void sign(ObjectReference self, WTObject pbo,String ibaSignInfo) {
		try{
			sign(pbo,ibaSignInfo);
		}catch(Exception e){
			e.printStackTrace();
			logger.error("??????????????????:" + pbo.getDisplayIdentity());
		}
	}
	
	
	/**
	 * ???????????????????????????????????????????????????????????????
	 * @param pbo
	 * @throws Exception
	 */
	public static void sign(WTObject pbo,String ibaSignInfo) throws Exception {
		logger.debug("????????????????????????:" + pbo.getDisplayIdentity());
		WTDocument doc = (WTDocument)pbo;
		//??????????????????
		String signInfoStr = IBAUtil.getIBAValue(pbo, ibaSignInfo);
		if(signInfoStr == null || "".equals(signInfoStr)){
			logger.warn("???????????????????????????,????????????:" + pbo.getDisplayIdentity()); 
			return;
		}
		//???????????????????????????PDF??????
		String tempPath = WTProperties.getLocalProperties().getProperty("wt.home") + File.separator + "codebase" + File.separator + "temp"+File.separator+System.currentTimeMillis()+File.separator;
		createFolders(tempPath);
		
		String targetFileName = tempPath + getSignPDFName(doc);
		File pdfFile 		  = getPDFFileForSign(doc,tempPath);
		if(pdfFile == null){
			logger.warn("????????????????????????????????????PDF:" +  pbo.getDisplayIdentity());
			return;
		}
		Map<String,String> signInfo = (Map<String,String>) JsonUtils.fromJson(signInfoStr, Map.class);
		//????????????????????????????????????????????????key
		String printConfigKey = WTUtil.getObjectTypeDisplay(pbo);
		if("".equals(printConfigKey) || "?????????".equals(printConfigKey)){
			logger.debug("????????????:?????????????????????????????????:" + pbo.getDisplayIdentity()); 
			return;
		}
		//??????????????????,??????,??????,?????????
		List<SignBean> signBeanList = getSignBeanList(pdfFile,printConfigKey, signInfo);
		//??????pdf??????,?????????????????????
		signDocForFixPosition(pbo,signBeanList,pdfFile);
		//??????????????????
		WTUtil.deleteFiles(tempPath);
		logger.debug("????????????:" + pbo.getDisplayIdentity());
	}
	
	public static File getPDFFileForSign(WTDocument doc,String tempPath) throws Exception{
		File pdfFile = null;
		ApplicationData appData = ContentUtil.getPrimary(doc);
		if(appData!=null&&appData.getFileName().endsWith(".pdf")){
			//??????????????????PDF?????????????????????????????????
			pdfFile = ContentUtil.downloadApplicationData(appData, tempPath,appData.getFileName(),doc);
		}else{
			//????????????????????????????????????
			pdfFile = DocUtil.getPDFFileFromRepContent(doc, tempPath);
			//?????????????????????
			/*if(pdfFile==null){
				pdfFile = new File("C:\\Users\\Administrator\\Desktop\\20170606030.pdf");
			}*/
		}
		return pdfFile;
	}
	
	/**
	 * ???????????????????????????
	 * @description
	 * @param pdfFile
	 * @param printConfigKey
	 * @param signInfo
	 * @return
	 * @throws Exception
	 */
	public static List<SignBean> getSignBeanList(File pdfFile,String printConfigKey,Map<String,String> signInfo) throws Exception{
		List<SignBean> signUtilBeanList = new ArrayList<SignBean>();
		for (String activityName : signInfo.keySet()) {
			String pageNumber = SignProperties.get(printConfigKey+"."+activityName+"."+"??????");
			int    font = 12; //??????
			try{
				font = Integer.parseInt( SignProperties.get(printConfigKey+"."+activityName+"."+"??????"));
			}catch(Exception e){
				
			}			
			PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());
			int count 		  = reader.getNumberOfPages();
			String content = signInfo.get(activityName);
			if(content==null){
				continue;
			}
			String contents[]   = content.split("\\$");
			String userName     = contents[0];
			String userFullName = contents[1];
			String time         = contents[2];
			if(pageNumber == null){
				continue;
			}
			String userNameX = SignProperties.get(printConfigKey+"."+activityName+".?????????.X??????");
			String userNameY = SignProperties.get(printConfigKey+"."+activityName+".?????????.Y??????");
			
			String timeX = SignProperties.get(printConfigKey+"."+activityName+".??????.X??????");
			String timeY = SignProperties.get(printConfigKey+"."+activityName+".??????.Y??????");
			
			String dateFormat = SignProperties.get(printConfigKey+"."+activityName+".????????????");
			dateFormat = StringUtils.trimToEmpty(dateFormat);
			if("ALL".equals(pageNumber)){
				for(int i = 1 ; i <= count ; i ++){
					SignUtil.addToSignList(userNameX, userNameY, timeX, timeY, i, font, dateFormat, userFullName,userName, time, signUtilBeanList);
				}
			}else{
				SignUtil.addToSignList(userNameX, userNameY, timeX, timeY, Integer.parseInt(pageNumber), font, dateFormat, userFullName, userName,time, signUtilBeanList);
			}
		}
		return signUtilBeanList;
	}
	
	public static void addToSignList(String userNameX,String userNameY,String timeX,String timeY,int pageNumber,int fontSize,String dateFormat,String userFullName,String userName,String time,List signUtilBeanList){

		if(userNameX != null && userNameY!= null){
			SignBean sub1 = new SignBean();
			sub1.setFontSize(fontSize);
			sub1.setPageNo(pageNumber);					
			sub1.setText(userFullName);
			sub1.setUserName(userName);
			sub1.setxPosition((int)Float.parseFloat(userNameX));
			sub1.setyPosition((int)Float.parseFloat(userNameY));
			
			signUtilBeanList.add(sub1);
		}

		if(timeX != null &&  timeY != null){
			SignBean sub2 = new SignBean();
			sub2.setFontSize(fontSize);
			sub2.setPageNo(pageNumber);
			if(dateFormat.isEmpty()){
				dateFormat = "yyyyMMdd";
			}
			if(!dateFormat.isEmpty()){
				try{
					SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
					Date date =  sdf.parse(time);
			        sdf = new SimpleDateFormat(dateFormat);
					time = sdf.format(date);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			sub2.setText(time);
			sub2.setxPosition((int)Float.parseFloat(timeX));
			sub2.setyPosition((int)Float.parseFloat(timeY));
			
			signUtilBeanList.add(sub2);
		}
	}
	
	/**
	 * ?????????????????????????????????????????????,????????????
	 * @param doc
	 * @throws WTException
	 * @throws PropertyVetoException
	 */
	public static void deleteExistSignFile(WTDocument doc){
		deleteSignFiles(doc);
	}
	public static void deleteSignFiles(WTObject doc){
		try{
			String number = ObjectInfoUtil.GetPDMNumber(doc);
			if(!(doc instanceof ContentHolder)){
				return;
			}
			wt.content.ContentHolder contentHolder = ContentHelper.service.getContents((ContentHolder) doc);
			Vector apps = ContentHelper.getApplicationData(contentHolder);
			for (Object object : apps) {
				ApplicationData appData = (ApplicationData) object;
				String fileName = appData.getFileName();
				if(fileName.startsWith(number+"_")&& fileName.endsWith("_??????.pdf")){
					ContentServerHelper.service.deleteContent(contentHolder,appData);
					logger.debug("????????????????????????:" + doc.getDisplayIdentity() + "," + fileName);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static String getSignPDFName(WTDocument doc){
		return getSignPDFFileName(doc);
	}
	
	public static String getSignPDFFileName(WTObject doc){
		String version  = ObjectInfoUtil.GetRev(doc);
		String number = "";
		try{
			number = ObjectInfoUtil.GetPDMNumber(doc);
		}catch(Exception e){
			e.printStackTrace();
		}
		String pdfFileName = number +"_"+version+"_??????.pdf";
		return pdfFileName;
	}
	
	
	/**
	 * ?????????????????????
	 * 
	 * @param directory
	 *            Feb 25, 2012 10:03:38 PM
	 */
	public static void createFolders(String directory) {
		File folder = new File(directory);
		if (!folder.exists()) {
			folder.mkdirs();
		}
	}
	
	/**
	 * ?????????pdf???????????????????????????
	 * @param pdfFile ???????????????????????????
	 * @param signUtilBeanList   signUtilBean???????????????????????????????????????????????????????????????????????????????????????
	 * @param tempPath
	 * @return
	 * @throws Exception
	 */
	public static String printTextOnPdf(File pdfFile,List<SignBean> signUtilBeanList,String targetFileName) throws Exception{
		logger.debug("???????????????PDF??????:");
		logger.debug("pdfFile:" + pdfFile);
		logger.debug("signUtilBeanList??????:" + signUtilBeanList.size());
		logger.debug("????????????:" + targetFileName); 
		if(pdfFile != null){
			PdfReader reader = null;
			PdfStamper stamp = null;
			try{
				reader = new PdfReader(pdfFile.getAbsolutePath());
				stamp = new PdfStamper(reader, new FileOutputStream(targetFileName));
				PdfContentByte over = null;
				for(SignBean signBean : signUtilBeanList){
					int pageNo    = signBean.getPageNo();
					int xPosition = signBean.getxPosition();
					int yPosition = signBean.getyPosition();
					over = stamp.getOverContent(pageNo);
					//???????????????????????????????????????????????????????????????????????????????????????????????????????????????
					String userName = signBean.getUserName();
					Image image = getSignPic(userName);
					if(image != null){
						insertImageToPdf(image,xPosition,yPosition,over);
					}else{
						//???????????????????????????
						String text  = signBean.getText();
						int fontSize = signBean.getFontSize();
						
						insertTextToPdf(text,fontSize,xPosition,yPosition,over);
					}
					
				}
			}finally{
				if(stamp != null){
					stamp.close();
				}
				if(reader != null){
					 reader.close(); 
				}
			}
		}else{
			throw new Exception("PDF ???????????????????????????");
		}
		return targetFileName;
	}
	
/*	public static void signDocForFixPosition(WTObject pbo,List<SignBean> signUtilBeanList) throws Exception{
		signDocForFixPosition(pbo,signUtilBeanList,null);
	}*/
	
	/**
	 * ??? signUtilBeanList ???????????????????????????????????????
	 * @param pbo ???????????????
	 * @param signUtilBeanList ??????????????????
	 * @throws Exception
	 */
	public static void signDocForFixPosition(WTObject pbo,List<SignBean> signUtilBeanList,File pdfFile) throws Exception{
		WTDocument doc = (WTDocument)pbo;
		logger.debug(">>>>????????????????????????");
		//???????????????????????????PDF??????
		String tempPath = WTProperties.getLocalProperties().getProperty("wt.home") + File.separator + "codebase" + File.separator + "temp"+File.separator+System.currentTimeMillis()+File.separator;
		createFolders(tempPath);
		
		String targetFileName = tempPath + getSignPDFName(doc);
		if(pdfFile == null){
			pdfFile = getPDFFileForSign(doc,tempPath);
		}
		if(null != pdfFile){
			targetFileName = printTextOnPdf(pdfFile,signUtilBeanList,targetFileName);
			deleteExistSignFile(doc);	
			File file = new File(targetFileName);
			//??????PDF????????????????????????
			if(file.exists()){
				DocUtil.addAppDataToDoc(doc, file, false);	
			}
			//??????????????????
			WTUtil.deleteFiles(tempPath);
		}
	}
	/**
	 * ?????????????????????????????????
	 * @param doc
	 * @return File
	 */
	public static File downloadSignFile(WTDocument doc,String path){
		File file = null;
		try{
			ContentHolder contentHolder = ContentHelper.service.getContents(doc);
			Vector apps = ContentHelper.getApplicationData(contentHolder);
			String signFileName = getSignPDFName(doc);
			for (Object object : apps) {
				if(object instanceof ApplicationData){
					ApplicationData app = (ApplicationData)object;
					ContentRoleType role = app.getRole();
					if(ContentRoleType.SECONDARY.equals(role)){
						String appFileName = app.getFileName();
						if(signFileName.equalsIgnoreCase(appFileName)){
							String fileName = path + File.separator + appFileName;
							ContentServerHelper.service.writeContentStream(app, fileName);
							file = new File(fileName);
						}
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return file;
	}

	public static boolean mergePdfFiles(List<String> files, String newfile,boolean deleteSource) {
		boolean retValue = false;
		Document document = null;
		try {
			if(null != files && files.size() > 0){
				document = new Document(new PdfReader(files.get(0)).getPageSize(1));
				PdfCopy copy = new PdfCopy(document, new FileOutputStream(newfile));
				document.open();
				for (int i = 0; i < files.size(); i++) {
					PdfReader reader = new PdfReader(files.get(i));
					int n = reader.getNumberOfPages();
					int j = 1;
					for (; j <= n; j++) {
						document.newPage();
						PdfImportedPage page = copy.getImportedPage(reader, j);
						copy.addPage(page);
					}
				}
				retValue = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(null != document){
				document.close();
			}			
			if(deleteSource){
				for (String string : files) {
					File f = new File(string);
					if(f.exists()){
						f.delete();
					}
				}
			}
		}
		return retValue;
	}
	
	/**
	 * ???????????????????????????????????????
	 * @param userName ???????????????
	 * @return
	 * @throws Exception 
	 */
	public static Image getSignPic(String userName) throws Exception {
		Image image = null;
		if(null != userName && !"".equals(userName)){
			//File signImage = null;
			//????????????????????????
			String picPath = SIGNIMAGE_FOLDERPATH + "transImage" +userName+".tif";
			File signImage = new File(picPath);
			if(!signImage.exists()){
				String picFullPath = SIGNIMAGE_FOLDERPATH+userName+".png";
				signImage = new File(picFullPath);
				if(signImage.exists()){
					image = Image.getInstance(picFullPath);
				}else{
					picFullPath = SIGNIMAGE_FOLDERPATH+userName+".jpg";
					signImage = new File(picFullPath);
					if(signImage.exists()){
						image = Image.getInstance(picFullPath);
					}else{
						picFullPath = SIGNIMAGE_FOLDERPATH + userName+".tif";
						signImage = new File(picFullPath);
						if(signImage.exists()){
							image = Image.getInstance(picFullPath);
						}
					}
				}
				//??????????????????????????????????????????
				if(signImage.exists()){
					File newFile = ImageConvert.imageConvert(signImage, "tif");
					if(newFile == null || !newFile.exists()){
						newFile = signImage;
					}
					image = Image.getInstance(newFile.getAbsolutePath());
				}
			}else{
				image = Image.getInstance(picPath);
			}
		}
		return image;
	}
	
	
	/**
	 * ???EPM??????????????????
	 * @param epmDoc
	 * @throws Exception
	 */
	public static void signEPMDoc(EPMDocument epmDoc,String ibaSignInfo) throws Exception {
		EPMDocumentType type = epmDoc.getDocType();
		//?????????????????????????????????
		if(type.equals(EPMDocumentType.toEPMDocumentType("CADDRAWING"))){
			String signInfoStr = IBAUtil.getIBAValue(epmDoc, ibaSignInfo);
			if(signInfoStr == null || "".equals(signInfoStr)){
				logger.debug("???????????????????????????,????????????:" + epmDoc.getDisplayIdentity()); 
				return;
			}
			//???????????????????????????PDF??????
			String tempPath = WTProperties.getLocalProperties().getProperty("wt.home") + File.separator + "codebase" + File.separator + "temp"+File.separator+System.currentTimeMillis()+File.separator;
			createFolders(tempPath);	
			File pdfFile  =  getPDFFileFromRepContent(epmDoc, tempPath);
			if(pdfFile == null){
				throw new WTException("????????????????????????????????????PDF:" + epmDoc.getDisplayIdentity());
			}
			Map<String,String> signInfo = (Map<String,String>) JsonUtils.fromJson(signInfoStr,Map.class);
			signEPMDrawing(epmDoc, pdfFile, signInfo);
			logger.debug("????????????:" + epmDoc.getDisplayIdentity());
		}
	}
	
	/**
	 * ??????EPM??????????????????--EPM???????????????????????????????????????????????????????????????????????????????????????X?????????????????????????????????????????????????????????????????????
	 * @return
	 * @throws WTException 
	 */
	public static void signEPMDrawing(EPMDocument epm,File pdfFile, Map<String, String> signInfo) throws Exception{
		int font = 12; //??????
		String targetFileName = pdfFile.getParent() + File.separator + getSignPDFFileName(epm);
		PdfReader reader = null;
		PdfStamper stamp = null;
		try{
			EPMAuthoringAppType appType = epm.getAuthoringApplication();
			String cadType = appType.toString();
			reader = new PdfReader(pdfFile.getAbsolutePath());
			stamp = new PdfStamper(reader, new FileOutputStream(targetFileName));	
			int count = reader.getNumberOfPages();		
			for(int i = 1 ; i <= count ; i ++){
				PdfContentByte over = stamp.getOverContent(i);
				Rectangle size = reader.getPageSizeWithRotation(i);
				float width = size.getWidth();
				float height = size.getHeight();
				String  tf = getEPMDrawingTF(width, height);
				System.out.println("??????:"+i+" ??????:"+tf+" : width="+ width +"  height="+height);
				for (String activityName : signInfo.keySet()) {							
					String content = signInfo.get(activityName);
					if(content==null){
						continue;
					}
					String contents[]   = content.split("\\$");
					String userName     = contents[0];
					String userFullName = contents[1];
					String time         = contents[2];
					
					try{
						font = Integer.parseInt(SignProperties.get(cadType+"."+activityName+".??????"));
					}catch(Exception e){
						System.out.println("????????????????????????????????????");
					}	
					
					String userNameX = SignProperties.get(cadType+"."+activityName+".?????????.X??????");
					String userNameY = SignProperties.get(cadType+"."+activityName+".?????????.Y??????");
					
					String timeX = SignProperties.get(cadType+"."+activityName+".??????.X??????");
					String timeY = SignProperties.get(cadType+"."+activityName+".??????.Y??????");
					
					String deptX =  SignProperties.get(cadType+"."+activityName+".??????.X??????");
					String deptY =  SignProperties.get(cadType+"."+activityName+".??????.Y??????");
					
					String dateFormat = SignProperties.get(cadType+"."+activityName+".????????????");
					dateFormat = StringUtils.trimToEmpty(dateFormat);
				
					if(userNameX != null && userNameY!= null){
						int xPosition = (int) (width - Float.parseFloat(userNameX));
						int yPosition = (int) (Float.parseFloat(userNameY));
						if("UG".equals(cadType)){
							if((tf.startsWith("A1")||tf.startsWith("A0"))&& tf.endsWith("H")){
								xPosition -= 28;
								yPosition += 28;
							}
						}else if("PROE".equals(cadType)){
							if(tf.startsWith("A2")||tf.startsWith("A1")||tf.startsWith("A0")){
								xPosition -= 15;
								yPosition += 15;
							}
						}
						Image image = getSignPic(userName);
						if(image != null){
							insertImageToPdf(image,xPosition,yPosition,over);
						}else{
							//???????????????????????????
							insertTextToPdf(userFullName,font,xPosition,yPosition,over);
						}
					}
					if(timeX != null &&  timeY != null){
						if(!dateFormat.isEmpty()){
							try{
								SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
								Date date =  sdf.parse(time);
						        sdf = new SimpleDateFormat(dateFormat);
								time = sdf.format(date);
							}catch(Exception e){
								e.printStackTrace();
							}
						}
						int xPosition = (int) (width - Float.parseFloat(timeX));
						int yPosition = (int) (Float.parseFloat(timeY));
						if("UG".equals(cadType)){
							if((tf.startsWith("A1")||tf.startsWith("A0"))&& tf.endsWith("H")){
								xPosition -= 28;
								yPosition += 28;
							}
						}else if("PROE".equals(cadType)){
							if(tf.startsWith("A2")||tf.startsWith("A1")||tf.startsWith("A0")){
								xPosition -= 15;
								yPosition += 15;
							}
						}
						//???????????????????????????
						insertTextToPdf(time,font,xPosition,yPosition,over);
					}
					
					if(deptX != null &&  deptY != null){
						String dept= "";
						int xPosition = (int) (width - Float.parseFloat(deptX));
						int yPosition = (int) (Float.parseFloat(deptY));
						if("UG".equals(cadType)){
							if((tf.startsWith("A1")||tf.startsWith("A0"))&& tf.endsWith("H")){
								xPosition -= 28;
								yPosition += 28;
							}
						}else if("PROE".equals(cadType)){
							if(tf.startsWith("A2")||tf.startsWith("A1")||tf.startsWith("A0")){
								xPosition -= 15;
								yPosition += 15;
							}
						}
						//???????????????????????????
						insertTextToPdf(dept,font,xPosition,yPosition,over);
					}
				}
			}
		}finally{
			if(stamp != null){
				stamp.close();
			}
			if(reader != null){
				 reader.close(); 
			}
		}
		deleteSignFiles(epm);	
		File file = new File(targetFileName);
		//??????PDF????????????????????????
		if(file.exists()){
			DocUtil.addAppDataToDoc((FormatContentHolder) epm, file, false);	
		}
	}
	
	public static File getPDFFileFromRepContent(EPMDocument object,String tempPath)throws Exception {
		File pdfFile = null;	
		logger.debug("----->>>>>????????????????????????????????????"+object.getDisplayIdentity()+".......");
		StandardRepresentationService srs = StandardRepresentationService.newStandardRepresentationService();
		Representation representation = srs.getDefaultRepresentation(object);
		if (representation != null) {
			if(representation.isOutOfDate()) {
				logger.debug("objRepresentation==========isOutOfDate");
				throw new WTException(object.getIdentity()+"??????????????????????????????????????????");
			}
			logger.debug("?????????????????????????????????????????????.......");
			representation = (Representation) ContentHelper.service.getContents(representation);
			Enumeration enuma = ContentHelper.getContentListAll(representation).elements();
			while (enuma != null && enuma.hasMoreElements()) {
				ContentItem item = (ContentItem) enuma.nextElement();
				if (item instanceof ApplicationData) {
					ApplicationData appData = (ApplicationData) item;
					String filename = appData.getFileName();
					String extention = Util.getExtension(filename);
					if (extention.equalsIgnoreCase("PDF")) {
						if (filename.indexOf("@") > -1) {
							filename = filename.replace("@", "\\u");
							filename = URLDecoder.decode(filename, "UTF-8");;
						}
						String fileAbsolutePath = tempPath + File.separator + filename;
						pdfFile = new File(fileAbsolutePath);
						ContentServerHelper.service.writeContentStream(appData,fileAbsolutePath);
						return pdfFile;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * ??????????????????
	 * @param width
	 * @param height
	 * @return
	 * 	A4
		width2:842.0
		height2:595.22
		A3
		width2:1191.0
		height2:842.0
		A2
		width2:1684.0
		height2:1191.0
		A1
		width2:2384.0
		height2:1684.0
		A0
		width2:3370.0
		height2:2384.0
		A0x1.25
		width:4212
		height:2384
		A0x1.5
		width:5054
		height:2384
		A0x1.75
		width:5896
		height:2384
		A0x2
		width:6740
		height:2384
	 */
	public static String getEPMDrawingTF(float width,float height){
		String direction = "H";		
		if(width < height){
			direction = "V";
			float temp = width;
			width = height;
			height = temp;
		}
		String tf = "";
		if(800 < width && width < 900 && 550 < height && height < 650){
			tf = "A4";
		}else if(800 < height && height < 900 && 1150 < width && width < 1250){
			tf = "A3";
		}else if(1625 < width && width < 1725 && 1150 < height && height < 1250){
			tf = "A2";
		}else if(1625 < height && height < 1725 && 2325 < width && width < 2425){
			tf = "A1";
		}else if(3325 < width && width < 3425 && 2325 < height && height < 2425){
			tf = "A0";
		}else if(4175 < width && width < 4275 && 2325 < height && height < 2425){
			tf = "A0x1.25";
		}else if(5000 < width && width < 5100 && 2325 < height && height < 2425){
			tf = "A0x1.5";
		}else if(5850 < width && width < 5950 && 2325 < height && height < 2425){
			tf = "A0x1.75";
		}else if(6700 < width && width < 6800 && 2325 < height && height < 2425){
			tf = "A0x2";
		}	
		tf = tf + direction;
		return tf;
	}
}
