package ext.technicalconditions.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.windchill.enterprise.copy.server.CoreMetaUtility;

import wt.enterprise.RevisionControlled;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTList;
import wt.fc.collections.WTSet;
import wt.fc.collections.WTValuedHashMap;
import wt.folder.Folder;
import wt.folder.FolderHelper;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerRef;
import wt.lifecycle.LifeCycleException;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.State;
import wt.maturity.MaturityBaseline;
import wt.maturity.MaturityException;
import wt.maturity.MaturityHelper;
import wt.maturity.Promotable;
import wt.maturity.PromotionNotice;
import wt.method.RemoteAccess;
import wt.pom.Transaction;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionServerHelper;
import wt.util.WTException;
import wt.util.WTInvalidParameterException;
import wt.util.WTPropertyVetoException;
import wt.vc.baseline.BaselineHelper;


public class PromotionNoticeUtil implements RemoteAccess, Serializable{
	
	private static final String CLASSNAME = PromotionNoticeUtil.class.getName();
	
	
	/**
	 * 设置升级请求及其关联对象的生命周期状态
	 * @param pn
	 * @param targetState
	 */
	public static void changeState(PromotionNotice pn,String targetState) throws WTInvalidParameterException, LifeCycleException, WTException{
		boolean accessEnforced = false;
		Transaction tx = null;
		try{
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
			tx = new Transaction();
			tx.start();
			LifeCycleHelper.service.setLifeCycleState(pn,State.toState(targetState));
			List<Promotable>  pts = getPromotionNoticeItems(pn);
			for(Promotable pt : pts){
				LifeCycleHelper.service.setLifeCycleState((LifeCycleManaged)pt,State.toState(targetState));
			}
			tx.commit();
			tx=null;
		}finally{
			if(tx != null){
				tx.rollback();
			}
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
		
	}
	
	/**
	 * 通过编号和名称查找升级请求
	 * @param name	查询升级请求编号条件
	 * @param accessControlled	是否受到权限制约
	 * @return	返回结果升级请求
	 * @throws WTException 
	 */
	public static List<PromotionNotice> queryPromotionNotice(String number,String name, boolean accessControlled) throws WTException {

		List<PromotionNotice> result = new ArrayList<PromotionNotice>();
		
		boolean enforce = wt.session.SessionServerHelper.manager.setAccessEnforced(accessControlled);
		try {
			number = number == null ? "" : number.trim().toUpperCase();
			number = number.replace("*","%");
			number = number.replace('?', '_');
			name = name == null ? "" : name.trim();
			name = name.replace("*","%");
			name = name.replace('?', '_');
			QuerySpec qs = new QuerySpec(PromotionNotice.class);
	        if(!"".equals(number) ){
	            if (qs.getConditionCount() > 0){
	                qs.appendAnd();
	            }
	            qs.appendWhere(new SearchCondition(PromotionNotice.class,PromotionNotice.NUMBER, SearchCondition.LIKE, false), new int[] { 0 });
	        }
	        if(!"".equals(name) ){
	            if (qs.getConditionCount() > 0){
	                qs.appendAnd();
	            }
	            qs.appendWhere(new SearchCondition(PromotionNotice.class,PromotionNotice.NAME, SearchCondition.LIKE), new int[] { 0 });
	        }			
			QueryResult qr = PersistenceHelper.manager.find(qs);
			while (qr.hasMoreElements()){
				result.add((PromotionNotice)qr.nextElement());
			}
			return result;
		}finally{
			SessionServerHelper.manager.setAccessEnforced(enforce);
		}
	}	
	
	
	

	/**
	 * 通过升级请求关联的对象查找升级请求
	 * @param promotable	查询内容对象
	 * @return	返回结果升级请求集
	 * @throws WTException 
	 */
	public static ArrayList<PromotionNotice> getPromotionNoticeByPromotable(Promotable promotable) throws WTException {
		ArrayList<PromotionNotice> results = new ArrayList<PromotionNotice>();
		boolean enforce = wt.session.SessionServerHelper.manager.setAccessEnforced(false);
		try {
			WTCollection wtlist = new WTArrayList();
			wtlist.add(promotable);
			WTCollection promotionNotices = MaturityHelper.service.getPromotionNotices(wtlist);
			
			if (!promotionNotices.isEmpty()) {
				WTList pnList = new WTArrayList();
				pnList.addAll(promotionNotices);
				Iterator it = pnList.persistableIterator();
				while (it.hasNext()) {
					Object obj = it.next();
					if(obj instanceof PromotionNotice){
						PromotionNotice promotion = (PromotionNotice)obj;
						if(!results.contains(promotion)){
							results.add(promotion);
						}
					}
				}
			}
		}finally {
			SessionServerHelper.manager.setAccessEnforced(enforce);
		}

		return results;
	}
	
	
	/**
	 * 通过升级请求得到升级请求关联的对象
	 * @param promotion	条件升级请求
	 * @return	升级内容对象集
	 * @throws WTException 
	 * @throws MaturityException 
	 */
	public static ArrayList<Promotable> getPromotionNoticeItems(PromotionNotice promotion) throws MaturityException, WTException{
		ArrayList<Promotable> results = new ArrayList<Promotable>();
		boolean enforce = wt.session.SessionServerHelper.manager.setAccessEnforced(false);
		try {
			QueryResult qr = MaturityHelper.service.getPromotionTargets(promotion);
			while(qr.hasMoreElements()){
				Object obj = qr.nextElement();
				if(obj instanceof Promotable){
					Promotable promotable = (Promotable)obj;
					if(!results.contains(promotable)){
						results.add(promotable);
					}
				}						
			}
		} finally {
			SessionServerHelper.manager.setAccessEnforced(enforce);
		}
		return results;
	}		
	
	
	/**
	 * 创建升级请求对象
	 * @param promotionName   升级请求名称,必须
	 * @param promotionNumber 升级请求编号,如果为空,使用对象初始化规则定义的编号
	 * @param fullType 升级请求类型全名(如:wt.maturity.PromotionNotice|com.plm.EBOMPackage),如果为空,则创建为wt.maturity.PromotionNotice类型
	 * @param folderPath    存储文件夹路径,如:/Default/EBOM签审包,支持创建单级文件夹,多级请先创建
	 * @param promotionDesc 升级请求描述信息,可以为空
	 * @param container     存储容器
	 * @return
	 * @throws WTException
	 * @throws RemoteException
	 * @throws InvocationTargetException
	 * @throws WTPropertyVetoException
	 */
	public static PromotionNotice createPromotionNotice(String promotionName, String promotionNumber,String fullType,String folderPath,String promotionDesc, WTContainer container) throws WTException, RemoteException, InvocationTargetException, WTPropertyVetoException{
			PromotionNotice promotion = null;

								
			if(container == null){
				return null;
			}
	
			if(promotionName == null || promotionName.equalsIgnoreCase("")){
				throw new WTException("升级请求名称不能为空");
			}/*else{
				//如果此名称升级请求已存在，则返回该升级请求
				PromotionNotice existPromotionNotice = PromotionUtil.getPromotionRequest(name, false);
				if(existPromotionNotice != null){
					return existPromotionNotice;
				}
			}	*/
			
			if(promotionDesc == null){
				promotionDesc = "";
			}
			
			//设置默认类型  (默认：wt.maturity.PromotionNotice)	
		 	if(fullType == null || fullType.equalsIgnoreCase("")){
		 		fullType = "wt.maturity.PromotionNotice";
			}
		 	
			//设置默认文件夹  (默认：/Default)
			if(folderPath == null || folderPath.equalsIgnoreCase("")){
				folderPath = "/Default";
			}else{
				 if(!folderPath.startsWith("/Default")){
					 folderPath = "/Default/" + folderPath;
				 }
			}				
			
			promotion = PromotionNotice.newPromotionNotice(promotionName);
			
			if(promotionNumber != null && !"".equals(promotionNumber)){
				promotion.setNumber(promotionNumber);
			}
			
			//设置基线描述
			promotion.setDescription(promotionDesc);
			
			//设置基线类型
			if(fullType != null){ 
				//TypeIdentifier id = TypeIdentifierHelper.getTypeIdentifier(promotionType);
				TypeIdentifier id = CoreMetaUtility.getTypeIdentifier(fullType);
				//WCTypeIdentifier wctypeidentifier = (WCTypeIdentifier)typeidentifier;
				//TypeDefinitionReference tdr = TypedUtility.getTypeDefinitionReference(wctypeidentifier.getTypename());
				
				//promotion.setTypeDefinitionReference(paramTypeDefinitionReference);
				promotion = (PromotionNotice) CoreMetaUtility.setType(promotion, id);
			}
			
			//设置上下文
			WTContainerRef containerRef = WTContainerRef.newWTContainerRef(container);
			promotion.setContainerReference(containerRef);
			
			//设置文件夹
			Folder location = null;	
				//查询文件夹是否存在
			try {
				location = FolderHelper.service.getFolder(folderPath,containerRef);
			} catch (Exception e) {
				location = null;
			}
				//若文件夹不存在，则创建该文件夹
			if(location == null)
				location = FolderHelper.service.saveFolderPath(folderPath, containerRef);
				//设置文件夹到基线对象
			if (location != null) {
				WTValuedHashMap map = new WTValuedHashMap();
				map.put(promotion, location);
				FolderHelper.assignLocations(map);
			}					
			
			MaturityBaseline maturityBaseline = MaturityBaseline.newMaturityBaseline();
			maturityBaseline.setContainerReference(containerRef);
			maturityBaseline = (MaturityBaseline) PersistenceHelper.manager.save(maturityBaseline);
			promotion.setConfiguration(maturityBaseline);
			promotion = MaturityHelper.service.savePromotionNotice(promotion);
			promotion = (PromotionNotice) PersistenceHelper.manager.refresh(promotion);											

			return promotion;
	}
	
	
	public static PromotionNotice addPromotionNoticeItem(PromotionNotice promotion, Set<Promotable> promotables) throws RemoteException, InvocationTargetException, WTException, WTPropertyVetoException{		
		for(Promotable promotable : promotables){
			promotion = addPromotionNoticeItem(promotion,promotable);
		}
		return promotion;
	}
	
	/**
	 * 为升级请求添加内容对象
	 * @param promotion	目标升级请求
	 * @param promotable	升级请求内容对象
	 * @return	添加好的升级请求对象
	 */
	public static PromotionNotice addPromotionNoticeItem(PromotionNotice promotion, Promotable promotable) throws RemoteException, InvocationTargetException, WTException, WTPropertyVetoException{		
		boolean enforce = false;
		try{			
			enforce = wt.session.SessionServerHelper.manager.setAccessEnforced(enforce);
			promotion = (PromotionNotice) PersistenceHelper.manager.refresh(promotion);
			WTSet promotableSet	= new WTHashSet(1);
			Vector seedVec = new Vector();
			MaturityBaseline maturityBaseline = null;
			
			seedVec.add(promotable);
			
			maturityBaseline = promotion.getConfiguration();
			maturityBaseline = (MaturityBaseline)BaselineHelper.service.addToBaseline(seedVec, maturityBaseline);
			//promotion.setConfiguration(maturityBaseline);
			promotion = (PromotionNotice) PersistenceHelper.manager.save(promotion);
			
			promotableSet.add(promotable);
			MaturityHelper.service.savePromotionTargets(promotion,promotableSet);
			promotion = (PromotionNotice) PersistenceHelper.manager.refresh(promotion);	
			return promotion;
		}finally {
				SessionServerHelper.manager.setAccessEnforced(enforce);
		}
	}	
	
	
	public static PromotionNotice removeAllPromotionNoticeItems(PromotionNotice promotion) throws RemoteException, InvocationTargetException, WTPropertyVetoException, WTException{
		ArrayList<Promotable> result = getPromotionNoticeItems(promotion);
		for(Promotable promotable : result){
			promotion = removePromotionNoticeItem(promotion,promotable);
		}
		return promotion;
	}
	
	/**
	 * 为升级请求移除指定关联对象
	 * @param promotion	目标升级请求
	 * @param promotable	升级请求内容对象
	 * @return	移除好的升级请求对象
	 * @throws InvocationTargetException 
	 * @throws RemoteException 
	 * @throws WTException 
	 * @throws WTPropertyVetoException 
	 */
	public static PromotionNotice removePromotionNoticeItem(PromotionNotice promotion, Promotable promotable) throws RemoteException, InvocationTargetException, WTException, WTPropertyVetoException{		
		boolean enforce = false;
		try {
			enforce = wt.session.SessionServerHelper.manager.setAccessEnforced(enforce);
			WTSet promotableSet	= new WTHashSet(1);
			Vector seedVec = new Vector();
			MaturityBaseline maturityBaseline = null;
			
			seedVec.add(promotable);
			
			maturityBaseline = promotion.getConfiguration();
			maturityBaseline = (MaturityBaseline)BaselineHelper.service.removeFromBaseline(seedVec, maturityBaseline);
			//promotion.setConfiguration(maturityBaseline);
			PersistenceHelper.manager.save(promotion);
			
			promotableSet.add(promotable);
			MaturityHelper.service.deletePromotionTargets(promotion,promotableSet);
			promotion = (PromotionNotice) PersistenceHelper.manager.refresh(promotion);
			return promotion;
		} finally {
			SessionServerHelper.manager.setAccessEnforced(enforce);
		}
	}	
	
	 /**
	   * 更新升级请求的升级对象到当前大版本的最新小版本
	 * @throws WTPropertyVetoException 
	 * @throws InvocationTargetException 
	 * @throws RemoteException 
	   */
	  public static WTObject refreshPromotionNotice(PromotionNotice pn) throws WTException, RemoteException, InvocationTargetException, WTPropertyVetoException {
		  
		  boolean accessEnforced = false;
		  Transaction tx = null;
		  try {
			  	tx = new Transaction();
			  	tx.start();
			  	accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
				Set<Promotable> AlllatestObjectSet = new HashSet<Promotable>();
				ArrayList<Promotable> promotionItems = getPromotionNoticeItems(pn);
				
				for (Promotable promotable : promotionItems) {
					WTObject obj = (WTObject) promotable;
					obj = WTUtil.getWTObject(WTUtil.getVROid((RevisionControlled)obj));
					AlllatestObjectSet.add((Promotable)obj);
			      }
				  pn = removeAllPromotionNoticeItems(pn);
				  pn =  addPromotionNoticeItem(pn,AlllatestObjectSet);
			      pn = (PromotionNotice) PersistenceHelper.manager.refresh(pn);
			      tx.commit();
			      tx = null;
			      return pn;
		    }finally {
		     if(tx != null){
		    	 tx.rollback();
		     }
		      wt.session.SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		  }
	}
	public static void main(String[] args) throws WTException {
			
	}
}
