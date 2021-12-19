package ext.technicalconditions.util;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import wt.fc.IdentityHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.folder.Cabinet;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.folder.Foldered;
import wt.folder.SubFolder;
import wt.folder.SubFolderIdentity;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerRef;
import wt.method.RemoteAccess;
import wt.pom.Transaction;
import wt.session.SessionServerHelper;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;


public class FolderUtil implements RemoteAccess{

	
	private static final String CLASSNAME = FolderUtil.class.getName();

	private static final Logger logger 	  = Logger.getLogger(FolderUtil.class);
	
	/**
	 * 获取以parentFolder为最顶层的文件夹结构,并封装为一个Map对象返回
	 * @param containerName
	 * @param parentFolder
	 * @return
	 * @throws WTException 
	 * @throws InvocationTargetException 
	 * @throws RemoteException 
	 */
	public static Map<String,Object> getContainerFolder(WTContainer container,Folder parentFolder) throws RemoteException, InvocationTargetException, WTException{

		Map<String,Object>  rootMap = new HashMap<String,Object>();
		Cabinet cabinet = null;
		if(parentFolder == null){
			cabinet     = getRootFolder(container);
			parentFolder= cabinet;
		}else{
			cabinet   = (Cabinet) parentFolder.getCabinetReference().getObject();
			container = parentFolder.getContainer();
		}
		rootMap.put("CONTAINERNAME", container.getName());
		rootMap.put("CONTAINEROID", (WTUtil.getWTObjectOid((WTObject)container)));
		rootMap.put("CABINETOID", (WTUtil.getWTObjectOid((WTObject)cabinet)));
		rootMap.put("FULLTYPE", (WTUtil.getType((WTObject)parentFolder)));
		rootMap.put("NAME", parentFolder.getName());

		rootMap.put("FULLPATH",parentFolder.getFolderPath());
		rootMap.put("OID",WTUtil.getWTObjectOid((WTObject)parentFolder));
		
		List<SubFolder> subFolders   = getSubFolders(parentFolder,false);
		List<Map<String,Object>>  subFoldersMap = new ArrayList<Map<String,Object>>();
		for(SubFolder subFolder : subFolders){
			Map<String,Object> subFolderMap = getContainerFolder(container,subFolder);
			subFoldersMap.add(subFolderMap);
		}
		Collections.sort(subFoldersMap, new Comparator<Map<String,Object>>(){
			@Override
			public int compare(Map<String,Object> arg0, Map<String,Object> arg1) {
				String arg0Name =  (String) arg0.get("NAME");
				String arg1Name =  (String) arg1.get("NAME");
				return arg0Name.compareTo(arg1Name);
			}			
		});
		rootMap.put("SUBFOLDERSMAP", subFoldersMap);
		return rootMap;
	}
	
	/**
	 * 移动对象到新文件夹
	 * @param foldered
	 * @param newFolderPath
	 * @param containerName
	 * @throws RemoteException
	 * @throws InvocationTargetException
	 * @throws WTException
	 */
	public static void moveToNewFolder(Foldered foldered,String newFolderPath,String containerName) throws RemoteException, InvocationTargetException, WTException{
    	boolean accessEnforced = false;
		try {
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
			FolderHelper.service.changeFolder(foldered, FolderUtil.getFolderByPath(newFolderPath, containerName));
		}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
	}
	/**
	 * 重命名文件夹
	 * @param subFolder
	 * @param newName
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public static void rename(SubFolder subFolder,String newName) throws WTException, WTPropertyVetoException{
    	//boolean accessEnforced = false;
    	Transaction tx = null;
		try {
			//accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
			tx = new Transaction();
			tx.start();
			String folderName =subFolder.getName();
			//如果name和原文件夹名称相同,则不执行任何逻辑,直接返回操作成功即可
			if(newName.equals(folderName)){
				return;
			}
			//修改名称
			SubFolderIdentity  si = (SubFolderIdentity) subFolder.getIdentificationObject();
			si.setName(newName);
			IdentityHelper.service.changeIdentity(subFolder, si);
			PersistenceServerHelper.manager.update(subFolder);
			tx.commit();
			tx = null;
		}finally{
			//SessionServerHelper.manager.setAccessEnforced(accessEnforced);
			if(tx != null){
				tx.rollback();
			}
			
		}
	}
	
	/**
	 * 获取对象文件夹路径
	 * @param foldered
	 * @return
	 */
	public static String getObjectFolderPath(Foldered foldered){
		return getObjectFolder(foldered).getFolderPath();
	}
	
	/**
	 * 获取对象文件夹
	 * @param foldered
	 * @return
	 */
	public static Folder getObjectFolder(Foldered foldered){
		Folder folder = (Folder) foldered.getParentFolder().getObject();
		return folder;
	}
	/**
	 * 获取容器根文件夹
	 * @param containerName
	 * @return
	 * @throws WTException
	 * @throws RemoteException
	 * @throws InvocationTargetException
	 */
	public static Cabinet getRootFolder(String containerName) throws WTException, RemoteException, InvocationTargetException{
		WTContainerRef containerRef = null;
		WTContainer container = WTUtil.getContainerByName(containerName);
		if(container == null){
			logger.error("未找到容器:" + containerName);
			return null;
		}
		containerRef = WTContainerRef.newWTContainerRef(container);
		Cabinet rootFolder = ((WTContainer) containerRef.getObject()).getDefaultCabinet();
		return rootFolder;
	}
	
	public static Cabinet getRootFolder(WTContainer container) throws WTException, RemoteException, InvocationTargetException{
		WTContainerRef containerRef = null;
		if(container == null){
			return null;
		}
		containerRef = WTContainerRef.newWTContainerRef(container);
		Cabinet rootFolder = ((WTContainer) containerRef.getObject()).getDefaultCabinet();
		return rootFolder;
	}
	
	public static Folder getFolderByPath(String folderPath,String containerName) throws RemoteException, InvocationTargetException, WTException{
		boolean accessEnforced = false;
		try{
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            WTContainer container = WTUtil.getContainerByName(containerName);
            if(container == null){
            	throw new WTException("容器不存在:" + containerName);
            }
            WTContainerRef containerRef = WTContainerRef.newWTContainerRef(container);
            try{
            	return FolderHelper.service.getFolder(folderPath, containerRef);
            }catch(Exception e){
            	e.printStackTrace();
            	return null;
            }
		}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
	}
	
	public static Folder getFolder(String oid) throws WTException{
		WTObject obj  = WTUtil.getWTObject(oid);
		Folder folder = null;
		if(obj instanceof WTContainer){
			WTContainerRef containerRef = WTContainerRef.newWTContainerRef((WTContainer)obj);
			Cabinet rootFolder = ((WTContainer) containerRef.getObject()).getDefaultCabinet();
			return rootFolder;
		}else if(obj instanceof Folder){
			return (Folder)obj;
		}else{
			throw new WTException("不支持将此类型转换为Folder对象:" + oid);
		}
	}
	
	/**
	 * 获取文件夹及子文件夹
	 * @param parent
	 * @param allLevel
	 * @return
	 * @throws WTException
	 */
	public static List<SubFolder> getSubFolders(Folder parent,boolean allLevel) throws WTException{
		List<SubFolder> result = new ArrayList<SubFolder>();
		QueryResult qr = FolderHelper.service.findSubFolders(parent);
		while(qr.hasMoreElements()){
			Object obj = qr.nextElement();
			if(obj instanceof SubFolder){
				result.add((SubFolder)obj);
				if(allLevel){
					result.addAll(getSubFolders((SubFolder)obj,allLevel));
				}
			}
		}
		return result;
	}
	
	/**
	 * 创建多级文件夹
	 * @param folderentry 设置为null即可
	 * @param folderPath  "/Default/文档/工艺文件/产品明细表"
	 * @param wtcontainer
	 * @throws WTException
	 * @throws Exception
	 */
	public static SubFolder createSubFolder(String folderPath,String containerName) throws WTException, Exception{
		boolean accessEnforced = false;
		try{
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            WTContainer container = WTUtil.getContainerByName(containerName);
            if(container == null){
            	throw new WTException("容器不存在:" + containerName);
            }
            return createSubFolder(folderPath,container);
		}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
	}
	
	public static SubFolder createSubFolder(String folderPath,WTContainer container) throws WTException, Exception{
		boolean accessEnforced = false;
		try{
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
			if (!folderPath.startsWith("/")) {
				folderPath = "/" + folderPath;
			}
			if (!folderPath.equalsIgnoreCase("/Default") && !folderPath.startsWith("/Default")) {
				folderPath = "/Default" + folderPath;
			}
			String nextfolder[] = folderPath.split("/");
			ArrayList<String> list = new ArrayList<String>();
			for (int p = 0; p < nextfolder.length; p++) {
				if (nextfolder[p] != null && !nextfolder[p].trim().equals("") && !nextfolder[p].trim().equals("Default")) {
					list.add(nextfolder[p]);
				}
			}
			FolderEntry folderentry = null;
			return createSubFolder(list, WTContainerRef.newWTContainerRef(container),folderentry);
		}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
	}
	/**
	 * 
	 * @param list
	 * @param wtContainerRef
	 * @return
	 * @throws WTException
	 */
	private static SubFolder createSubFolder(List<String> list, WTContainerRef wtContainerRef,FolderEntry folderentry) throws WTException, Exception {
		SubFolder subFolder = null;
		String path = ((WTContainer) wtContainerRef.getObject()).getDefaultCabinet().getFolderPath();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			Folder folder = null;
			try {
				folder = FolderHelper.service.getFolder(path, wtContainerRef);
				path = path + "/" + list.get(i);
				QueryResult result = FolderHelper.service.findSubFolders(folder);
				if (!checkFolderExits(result, list.get(i))){
					subFolder = FolderHelper.service.createSubFolder(path, wtContainerRef);
				}
			} catch (WTException e) {
				e.printStackTrace();
				throw new WTException(e.getLocalizedMessage());
			}
		}
		return subFolder;
	}
	
	/**
	 * 判断folder是否存在
	 * 
	 * @param result
	 * @param str
	 * @return
	 */
	private static boolean checkFolderExits(QueryResult result, String str) throws WTException, Exception {
		if (result == null)
			return false;
		while (result.hasMoreElements()) {
			Object obj = result.nextElement();
			if (obj instanceof SubFolder) {
				SubFolder subFolder = (SubFolder) obj;
				if (subFolder.getName().equals(str))
					return true;
			} else {
				return false;
			}
		}
		return false;
	}
}
