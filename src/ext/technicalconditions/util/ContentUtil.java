package ext.technicalconditions.util;

import java.beans.PropertyVetoException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.forms.FormResultAction;
import com.ptc.netmarkets.model.NmObjectHelper;

import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentHolder;
import wt.content.ContentItem;
import wt.content.ContentRoleType;
import wt.content.ContentServerHelper;
import wt.content.FormatContentHolder;
import wt.content.HolderToContent;
import wt.epm.EPMDocument;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTSet;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.query.WhereExpression;
import wt.session.SessionServerHelper;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.util.WTRuntimeException;

public class ContentUtil {

    private static final Logger log = LoggerFactory.getLogger(ContentUtil.class);

/*    *//**
     * 获取对象主内容及附件信息
     * @param oid
     * @return
     * @throws WTRuntimeException
     * @throws WTException
     * @throws PropertyVetoException
     *//*
    public static List<Map<String, String>> getContentsForDisplay(String oid)
            throws WTRuntimeException, WTException, PropertyVetoException {
        ContentHolder fch = (ContentHolder) WTUtil.getObjectByOid(oid);
        List<ApplicationData> appData = ContentUtil.getPrimaryAndSecondary(fch);
        List<Map<String, String>> lists = new ArrayList<Map<String, String>>();
        for (ApplicationData app : appData) {
            Map<String, String> map = new HashMap<String, String>();
            String fileName = app.getFileName();
            String contentOid = WTUtil.getWTObjectOid(app);
            String downloadPath = GACEConstants.webBaseURL
                    + "extRest/content/downloadContent?contentHolderOid=" + oid
                    + "&applicationDataOid=" + contentOid;
            String contentRoleType = app.getRole().toString();
            map.put("fileName", fileName);
            map.put("downloadPath", downloadPath);
            map.put("applicationDataOid", contentOid);
            map.put("contentRoleType", contentRoleType);
            map.put("contentHolderOid", oid);
            lists.add(map);
        }
        return lists;
    }*/

    public static List<ContentItem> getContentsByRole(ContentHolder contentHolder,
            ContentRoleType[] contentRoleTypes) throws WTException, PropertyVetoException {
        List<ContentItem> contentItems = new ArrayList<ContentItem>();

        contentHolder = ContentHelper.service.getContents(contentHolder);
        if (contentRoleTypes == null || contentRoleTypes.length == 0) {
            List<ContentItem> items = ContentHelper.getContentListAll(contentHolder);
            contentItems.addAll(items);
        } else {
            for (ContentRoleType contentRoleType : contentRoleTypes) {
                QueryResult qr = ContentHelper.service.getContentsByRole(contentHolder,
                        contentRoleType);
                while (qr.hasMoreElements()) {
                    Object obj = qr.nextElement();
                    if (obj instanceof ContentItem) {
                        ContentItem contentItem = (ContentItem) obj;
                        contentItems.add(contentItem);
                    }
                }
            }
        }
        return contentItems;
    }

	public static FormatContentHolder updatePrimaryFile(File file,FormatContentHolder holder) throws WTException, PropertyVetoException, FileNotFoundException, IOException{
		ContentItem contentItem = ContentHelper.service.getPrimary(holder);
		ApplicationData primary = null;
		if (contentItem != null) {
			primary = (ApplicationData) contentItem;
			log.debug("找到已存在的主内容,删除:" + primary.getFileName());
			ContentServerHelper.service.deleteContent(holder,primary);
		}else{
			log.debug("未找到已存在的主内容");
		}
		ApplicationData newAppData = ApplicationData.newApplicationData(holder);	        
		newAppData.setFileName(file.getName()); //更新文件名
		newAppData.setUploadedFromPath(file.getParent());
		newAppData.setRole(ContentRoleType.PRIMARY);
        ContentHolder ch = wt.content.ContentHelper.service.getContents(holder);
        ContentServerHelper.service.updateContent(ch, newAppData, file.getPath());
	return holder;
}
	
    /**
     * 更新ContentHoder内容，删除已存在的同名内容
     * @param holder
     * @param fileName
     * @param filePath
     * @param fileDesc
     * @param contentRole
     * @return
     * @throws Exception
     */
    public static ContentHolder addApplicationData(ContentHolder holder, String fileName,
            Object filePath, String fileDesc, String contentRole) throws Exception {
        if (holder == null) {
            throw new WTException("内容载体不能为空");
        }
        if (filePath == null) {
            throw new WTException("内容输入不能为空");
        }
        if (StringUtils.isBlank(contentRole)) {
            throw new WTException("内容类型不能为空");
        }
        ContentRoleType contentRoleType = ContentRoleType.toContentRoleType(contentRole);
        return addApplicationData(holder, fileName, filePath, fileDesc, contentRoleType);
    }

    /**
     * 更新ContentHoder内容
     * @param holder
     * @param fileName
     * @param filePath
     * @param fileDesc
     * @param contentRole
     * @return
     * @throws Exception
     */
    public static ContentHolder addApplicationData(ContentHolder holder, String fileName,
            Object filePath, String fileDesc, ContentRoleType contentRoleType) throws Exception {
        holder = ContentHelper.service.getContents(holder);
        if (ContentRoleType.PRIMARY.equals(contentRoleType)) {
            QueryResult existQr = ContentHelper.service.getContentsByRole(holder,
                    ContentRoleType.PRIMARY);
            if (existQr != null && existQr.size() > 0) {
                if (existQr.hasMoreElements()) {
                    ContentItem existContentItem = (ContentItem) existQr.nextElement();
                    ContentServerHelper.service.deleteContent(holder, existContentItem);
                    PersistenceServerHelper.manager.update(holder);
                    holder = (ContentHolder) PersistenceHelper.manager.refresh(holder);
                }
            }
        } else {
            String checkedFileName = fileName;
            if (StringUtils.isBlank(fileName)) {
                if (filePath instanceof String) {
                    String filePathStr = (String) filePath;
                    filePathStr = filePathStr.replace("\\", "/");
                    if (filePathStr.contains("/")) {
                        checkedFileName = filePathStr.substring(filePathStr.lastIndexOf("/") + 1);
                    }
                }
            }
            log.debug("checkedFileName : " + checkedFileName);
            QueryResult existQr = ContentHelper.service.getContentsByRole(holder, contentRoleType);
            if (existQr != null && existQr.size() > 0) {
                while (existQr.hasMoreElements()) {
                    ContentItem existContentItem = (ContentItem) existQr.nextElement();
                    if (existContentItem instanceof ApplicationData) {
                        ApplicationData existedAppData = (ApplicationData) existContentItem;
                        String appName = existedAppData.getFileName();
                        if (checkedFileName.equalsIgnoreCase(appName)) {//删除已经存在的同名内容
                            ContentServerHelper.service.deleteContent(holder, existedAppData);
                            PersistenceServerHelper.manager.update(holder);
                            holder = (ContentHolder) PersistenceHelper.manager.refresh(holder);
                            break;
                        }
                    }
                }
            }
        }

        ApplicationData applicationData = ApplicationData.newApplicationData(holder);
        applicationData.setRole(contentRoleType);
        applicationData.setDescription(fileDesc);
        if (StringUtils.isNotBlank(fileName)) {
            applicationData.setFileName(fileName);
        }
        if (filePath instanceof String) {
            String filePathStr = ((String) filePath).replace("\\", "/");
            ContentServerHelper.service.updateContent(holder, applicationData, filePathStr);
        } else if (filePath instanceof InputStream) {
            ContentServerHelper.service.updateContent(holder, applicationData,
                    (InputStream) filePath);
        } else {
            throw new WTException("filePath is invalid...");
        }

        PersistenceServerHelper.manager.update(applicationData);

        try {
            if (holder instanceof FormatContentHolder) {
                holder = ContentServerHelper.service
                        .updateHolderFormat((FormatContentHolder) holder);
            }
        } catch (Exception e) {
            log.error("updateHolderFormat error...",e);
        }

        holder = (ContentHolder) PersistenceHelper.manager.refresh(holder);
        return holder;
    }

    /**
     * 删除附件或主内容对象
     * @param contentHolder
     * @param data
     * @throws WTException 
     * @throws WTPropertyVetoException 
     */
    public static ContentHolder deleteApplicationData(ContentHolder contentHolder,
            ApplicationData data) throws WTPropertyVetoException, WTException {
        ContentServerHelper.service.deleteContent(contentHolder, data);
        contentHolder = (ContentHolder) PersistenceHelper.manager.refresh(contentHolder);
        return contentHolder;
    }

    /**
     * 删除指定文件名称的附件
     * @param contentHolder
     * @param fileName
     * @return
     * @throws WTException 
     * @throws PropertyVetoException 
     */
    public static ContentHolder deleteSecondaryFile(ContentHolder contentHolder, String fileName)
            throws WTException, PropertyVetoException {
        List<ContentItem> contentItems = getContentsByRole(contentHolder,
                new ContentRoleType[] { ContentRoleType.SECONDARY });
        for (ContentItem contentItem : contentItems) {
            if (contentItem instanceof ApplicationData
                    && ((ApplicationData) contentItem).getFileName().equalsIgnoreCase(fileName)) {
                contentHolder = deleteApplicationData(contentHolder, (ApplicationData) contentItem);
            }
        }
        return contentHolder;
    }

    /**
     * 添加附件
     */
    public static ContentHolder addSecondaryFile(ContentHolder holder, File file,
            String newFileName) throws WTException, WTPropertyVetoException,
            java.beans.PropertyVetoException, IOException {
        ApplicationData applicationdata = ApplicationData.newApplicationData(holder);
        applicationdata.setRole(ContentRoleType.SECONDARY);
        if (!StringUtils.isEmpty(newFileName)) {
            applicationdata.setFileName(newFileName);
        } else {
            applicationdata.setFileName(file.getName());
        }
        applicationdata.setUploadedFromPath(file.getParent());
        wt.content.ContentHolder ch = wt.content.ContentHelper.service.getContents(holder);
        ContentServerHelper.service.updateContent(ch, applicationdata, file.getPath());
        if (!StringUtils.isEmpty(newFileName)) {
            applicationdata.setFileName(newFileName);
        } else {
            applicationdata.setFileName(file.getName());
        }
        PersistenceServerHelper.manager.update(applicationdata);
        return holder;
    }

    /**
     * 获取对象的主内容及附件ApplicationData
     */
    public static List<ApplicationData> getPrimaryAndSecondary(ContentHolder contentHolder)
            throws WTException, PropertyVetoException {
        List<ApplicationData> result = new ArrayList<ApplicationData>();
        List<ContentItem> contentItems = getContentsByRole(contentHolder,
                new ContentRoleType[] { ContentRoleType.PRIMARY, ContentRoleType.SECONDARY });
        for (ContentItem contentItem : contentItems) {
            if (contentItem instanceof ApplicationData && !result.contains(contentItem)) {
                result.add((ApplicationData) contentItem);
            }
        }
        return result;
    }
    
    public static String getPrimaryContentFileName(ContentHolder contentHolder) throws WTException, PropertyVetoException{
    	ApplicationData applicationData = getPrimary(contentHolder);
    	if(applicationData!= null){
    		return applicationData.getFileName();
    	}
    	return "";
    }
    
    
    public static ApplicationData getPrimary(ContentHolder contentHolder)
            throws WTException, PropertyVetoException {
        List<ApplicationData> result = new ArrayList<ApplicationData>();
        List<ContentItem> contentItems = getContentsByRole(contentHolder,
                new ContentRoleType[] { ContentRoleType.PRIMARY });
        for (ContentItem contentItem : contentItems) {
            if (contentItem instanceof ApplicationData && !result.contains(contentItem)) {
                return (ApplicationData) contentItem;
            }
        }
        return null;
    }
    public static List<ApplicationData> getSecondarys(ContentHolder contentHolder)
            throws WTException, PropertyVetoException {
        List<ApplicationData> result = new ArrayList<ApplicationData>();
        List<ContentItem> contentItems = getContentsByRole(contentHolder,
                new ContentRoleType[] { ContentRoleType.SECONDARY });
        for (ContentItem contentItem : contentItems) {
            if (contentItem instanceof ApplicationData && !result.contains(contentItem)) {
                result.add((ApplicationData) contentItem);
            }
        }
        return result;
    }

    public static InputStream getInputStream(ApplicationData appData, ContentHolder contentHolder) throws WTException, PropertyVetoException {
        InputStream inputstream = null;
        boolean accessEnforced = false;
        try {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            if(appData != null) {
                appData = validateAppDataHolderToContent(appData, contentHolder);
                if(appData == null) {
                    throw new WTException("检查ApplicationData["+appData+"]无效");
                }
                inputstream = ContentServerHelper.service.findContentStream(appData);
            }
        } finally {
            SessionServerHelper.manager.setAccessEnforced(accessEnforced);
        }
        return inputstream;
    }

    public static File downloadApplicationData(String applicationDataOid, String contentHolderOid,
            String folderPath, String fileName)
            throws WTException, IOException, PropertyVetoException {
        boolean accessEnforced = false;
        try {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            ApplicationData appData = (ApplicationData) WTUtil.getObjectByOid(applicationDataOid);
            
            ContentHolder contentHolder = null;
            if (contentHolderOid != null && !"".equals(contentHolderOid)) {
                contentHolder = (ContentHolder) WTUtil.getObjectByOid(contentHolderOid);
            }
            appData = validateAppDataHolderToContent(appData, contentHolder);
            if(appData == null) {
                throw new WTException("检查ApplicationData["+appData+"]无效");
            }
            
            if (StringUtils.isBlank(fileName)) {
                fileName = appData.getFileName();
            }
            
            File file = ContentUtil.downloadApplicationData(appData, folderPath, fileName,
                    contentHolder);
            return file;
        } finally {
            SessionServerHelper.manager.setAccessEnforced(accessEnforced);
        }
    }
    
    /**
     * 重新获取ApplicationData
     * @param appData
     * @param contentHolder
     * @return
     * @throws WTException
     * @throws PropertyVetoException
     */
    public static ApplicationData validateAppDataHolderToContent(ApplicationData appData,
            ContentHolder contentHolder) throws WTException, PropertyVetoException {
        if (appData != null) {
            if (contentHolder == null) {
                contentHolder = queryContentLinkByContentItem(appData);
            }
            HolderToContent holderLink = appData.getHolderLink();
            if (holderLink == null) {
                ApplicationData checkedAppData = appData;
                appData = null;
                List<ContentItem> allContentItems = getContentsByRole(contentHolder, null);
                for (ContentItem content : allContentItems) {
                    if (content instanceof ApplicationData) {
                        ApplicationData tempAppData = (ApplicationData) content;
                        if (tempAppData.equals(checkedAppData)) {
                            appData = tempAppData;
                            break;
                        }
                    }
                }
            }
            if (appData == null) {
                throw new WTException("[" + contentHolder + "]未找到指定ApplicationData对象:" + appData);
            }
            holderLink = appData.getHolderLink();
            if (holderLink == null) {
                throw new WTException("ApplicationData对象获取HolderToContent对象失败:" + appData);
            }
        }
        
        return appData;
    }

    /**
     * 下载单个文件
     * @param applicationData
     * @param folderPath
     * @param contentHolder contentHolder仅用于处理epm文件名
     * @throws WTException
     * @throws IOException
     */
    public static File downloadApplicationData(ApplicationData applicationData, String folderPath,
            String fileName, ContentHolder contentHolder) throws WTException, IOException {
        boolean accessEnforced = false;
        try {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            if (!(folderPath.endsWith("/") || folderPath.endsWith("\\"))) {
                folderPath = folderPath + File.separator;
            }
            
            File tempFolder = new File(folderPath);
            if(!tempFolder.exists()) {
                tempFolder.mkdirs();
            }
            
            if (fileName == null || "".equals(fileName)) {
                fileName = applicationData.getFileName();
            }
            if (contentHolder != null && contentHolder instanceof EPMDocument) {
                fileName = replaceEpmFileName(fileName, (EPMDocument) contentHolder);
            }
            ContentServerHelper.service.writeContentStream(applicationData, folderPath + fileName);
            return new File(folderPath + fileName);
        } finally {
            SessionServerHelper.manager.setAccessEnforced(accessEnforced);
        }
    }

    /**
     * 获取指定角色、指定文件名称的文件
     * @param holder
     * @param contentRoleTypes
     * @param fileName
     * @return 
     * @throws WTException
     * @throws PropertyVetoException
     */
    public static ApplicationData getApplicationData(ContentHolder holder,
            ContentRoleType[] contentRoleTypes, String fileName)
            throws WTException, PropertyVetoException {
        List<ContentItem> contentItems = ContentUtil.getContentsByRole(holder, contentRoleTypes);
        ApplicationData appData = null;
        for (ContentItem item : contentItems) {
            if (item instanceof ApplicationData
                    && ((ApplicationData) item).getFileName().equalsIgnoreCase(fileName)) {
                appData = (ApplicationData) item;
            }
        }
        return appData;
    }

    public static String replaceEpmFileName(String fileName, EPMDocument epm) {
        String cadName = epm.getCADName();
        fileName = fileName.replaceFirst("\\{\\$CAD_NAME\\}", cadName);
        if (cadName.contains(".")) {
            cadName = cadName.substring(0, cadName.lastIndexOf("."));
        }
        fileName = fileName.replaceFirst("\\{\\$CAD_NAME_NO_EXT\\}", cadName);
        return fileName;
    }

    /**
     * 获取主内容或附件的下载链接
     * @param contentHolder
     * @param appData
     * @return
     */
    public static String getDownloadURL(ContentHolder contentHolder, ApplicationData appData)
            throws WTException {
        String fileName = appData.getFileName();
        if (contentHolder instanceof EPMDocument) {
            replaceEpmFileName(fileName, (EPMDocument) contentHolder);
        }
        return ContentHelper.getDownloadURL(contentHolder, appData, false, appData.getFileName())
                .toExternalForm();
    }

    /**
     * 获取主内容下载链接
     * @param contentHolder
     * @param appData
     * @return
     * @throws WTException
     * @throws PropertyVetoException 
     */
    public static String getPrimaryDownloadURL(FormatContentHolder contentHolder)
            throws WTException, PropertyVetoException {
        if (contentHolder == null) {
            return "";
        }
        ApplicationData primaryContent = null;
        ContentItem item = ContentHelper.service.getPrimary(contentHolder);
        if (item != null && item instanceof ApplicationData) {
            primaryContent = (ApplicationData) item;
        }
        if (primaryContent != null) {
            String fileName = primaryContent.getFileName();
            if (contentHolder instanceof EPMDocument) {
                replaceEpmFileName(fileName, (EPMDocument) contentHolder);
            }
            return ContentHelper.getDownloadURL(contentHolder, primaryContent, false, fileName)
                    .toExternalForm();
        }
        return "";
    }

    /**
     * 替换文件夹命名中的特殊字符
     * @param number
     * @return
     */
    public static String replaceForFolderName(String definedName) {
        definedName = definedName.replace("/", "-");
        definedName = definedName.replace("\\", "-");
        definedName = definedName.replace("<", "-");
        definedName = definedName.replace(">", "-");
        definedName = definedName.replace(":", "-");
        definedName = definedName.replace("?", "-");
        definedName = definedName.replace("\"", "-");
        definedName = definedName.replace("|", "-");
        return definedName;
    }

    /**
     * 清除指定角色内容
     * @throws PropertyVetoException 
     * @throws WTException 
     */
    public static ContentHolder clearContentsByRole(ContentHolder contentHolder,
            ContentRoleType roleType) throws WTException, PropertyVetoException {
        List<ContentItem> contentItems = getContentsByRole(contentHolder,
                new ContentRoleType[] { roleType });
        if(contentItems != null && contentItems.size() > 0) {
            WTSet ciSet = new WTHashSet(contentItems);
            ContentServerHelper.service.deleteContent(ciSet);
        }
        contentHolder = (ContentHolder) PersistenceHelper.manager.refresh(contentHolder);
        return contentHolder;
    }
    
    public static ContentHolder queryContentLinkByContentItem(ContentItem contentItem)throws WTException{
    	ContentHolder contentHolder = null;
    	QuerySpec qs = new QuerySpec(HolderToContent.class);
    	WhereExpression contion = new SearchCondition(HolderToContent.class, "roleBObjectRef.key.id", "=", contentItem.getPersistInfo().getObjectIdentifier().getId());
    	qs.appendWhere(contion, new int[1]);
    	QueryResult qr = PersistenceHelper.manager.find(qs);
    	if ((qr != null) && (qr.hasMoreElements())) {
    		HolderToContent htc = (HolderToContent)qr.nextElement();
    		contentHolder = htc.getContentHolder();
    	}
    	return contentHolder;
    }
    
    /**
     * 将文件以流的形式推送给客户端浏览器
     * 
     * @param filePath
     * @param contentType application/zip,application/pdf,application/msexcel
     * @param response
     * @throws IOException
     */
    public void downloadFileByResponse(String filePath,String contentType,HttpServletResponse response) throws IOException{
		File file = new File(filePath);
		String fileName = file.getName();
        FileInputStream fis = new FileInputStream(file);
        
        OutputStream os         = response.getOutputStream();
        BufferedInputStream bis = new BufferedInputStream(fis);
        int length = 0;
		byte[] data = new byte[8192];
		while ((length = bis.read(data)) > 0) {
			os.write(data, 0, length);
		}
		response.setContentType(contentType);
		response.setContentLength((int) file.length());
		response.setHeader("Content-disposition","attachment;filename="+fileName+"");
        os.write(data);
        os.flush();
        os.close();
        fis.close();
        bis.close();
    }
    
    /**
     * processor中下载文件
     * @description
     * @param formResult
     * @param filePath
     * @throws IOException
     * @throws WTException
     */
    public void downloadFileByProcessor(FormResult formResult,String filePath) throws IOException, WTException{
    	File file = new File(filePath);
    	URL url = NmObjectHelper.constructOutputURL(file, file.getName());
		formResult.setForcedUrl(url.toExternalForm());
		formResult.setNextAction(FormResultAction.FORWARD);
    	//formresult.setJavascript("window.PTC.util.downloadUrl(\"" + url.toExternalForm() + "\");");
    	//formresult.setNextAction(FormResultAction.JAVASCRIPT);
    }

}
