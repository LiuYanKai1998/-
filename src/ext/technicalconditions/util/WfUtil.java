package ext.technicalconditions.util;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ptc.core.components.rendering.guicomponents.DateDisplayComponent;
import com.ptc.core.components.rendering.guicomponents.TextDisplayComponent;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.netmarkets.workflow.NmWorkflowHelper;
import com.ptc.windchill.enterprise.workflow.WorkflowDataUtility;
import com.ptc.windchill.uwgm.common.prefs.res.newCadDocPrefsResource;

import ext.technicalconditions.util.third.JsonUtils;
import ext.technicalconditions.util.IBAUtil;

import wt.change2.ChangeActivity2;
import wt.change2.ChangeOrder2;
import wt.change2.Changeable2;
import wt.change2.WTChangeOrder2;
import wt.fc.ObjectIdentifier;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.PersistentReference;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTObject;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerRef;
import wt.lifecycle.LifeCycleException;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.State;
import wt.maturity.Promotable;
import wt.maturity.PromotionNotice;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.org.WTGroup;
import wt.org.WTPrincipal;
import wt.org.WTPrincipalReference;
import wt.org.WTUser;
import wt.part.WTPart;
import wt.project.Role;
import wt.query.ClassAttribute;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.query.SubSelectExpression;
import wt.session.SessionContext;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.team.Team;
import wt.team.TeamException;
import wt.team.TeamHelper;
import wt.team.TeamManaged;
import wt.team.TeamReference;
import wt.util.WTException;
import wt.util.WTInvalidParameterException;
import wt.util.WTPropertyVetoException;
import wt.util.WTRuntimeException;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;
import wt.workflow.WfException;
import wt.workflow.definer.UserEventVector;
import wt.workflow.definer.WfDefinerHelper;
import wt.workflow.definer.WfProcessDefinition;
import wt.workflow.definer.WfProcessTemplate;
import wt.workflow.engine.ProcessData;
import wt.workflow.engine.WfActivity;
import wt.workflow.engine.WfBlock;
import wt.workflow.engine.WfConnector;
import wt.workflow.engine.WfContainer;
import wt.workflow.engine.WfEngineHelper;
import wt.workflow.engine.WfExecutionObject;
import wt.workflow.engine.WfProcess;
import wt.workflow.engine.WfState;
import wt.workflow.engine.WfVotingEventAudit;
import wt.workflow.work.ActivityAssignmentLink;
import wt.workflow.work.WfAssignedActivity;
import wt.workflow.work.WfAssignment;
import wt.workflow.work.WfTally;
import wt.workflow.work.WorkItem;
import wt.workflow.work.WorkItemLink;
import wt.workflow.work.WorkflowHelper;

/**
 * @author ZhongBinpeng
 *
 */
@SuppressWarnings("unchecked")
public class WfUtil implements RemoteAccess{ 
	public static String PROCESS_NAME = "procName";
	public static String PROCESS_CREATOR = "procCreator";
	public static String PROCESS_DATE = "procDate";
	public static String WORK_NAME = "workName";
	public static String WORK_ASSIGNEE = "workAssignee";
	public static String WORK_ROLE = "workRole";
	public static String WORK_VOTE = "workVote";
	public static String WORK_COMMENTS = "workComments";
	public static String WORK_DEADLINE = "workDeadline";
	public static String WORK_COMPLETEDDATE = "workCompletedDate";
	private static final Logger logger = LoggerFactory.getLogger(WfUtil.class);
	private static final String CLASSNAME = WfUtil.class.getName();
	
	/**
	 * ???????????????????????????IBA
	 */
	public static void saveSignInfo(ObjectReference self,WTObject obj){
		saveSignInfo(self,obj,"SIGNINFO");
	}
	
	/**
	 * ???????????????????????????IBA
	 */
	public static void saveSignInfo(ObjectReference self,WTObject obj,String ibaSignInfo){
		try{
			Map signInfo    = WfUtil.getSignInfo(self,"yyyy/MM/dd");
			String str 	    = JsonUtils.toJsonStr(signInfo);
			IBAUtil.setIBAValue(obj,ibaSignInfo, str);			
		}catch(Exception e){
			logger.error("???????????????????????????IBA??????.");
			e.printStackTrace();
		}

	}
	
	/**
	 * ???????????????????????????IBA???????????????????????????????????????????????????
	 * Liuyk
	 * 
	 */
	public static void saveSignInfo2(ObjectReference self,WTObject obj,String ibaSignInfo){
		try{
			Map signInfo    = WfUtil.getSignInfo(self,"yyyy/MM/dd");
			//HQ?????????????????????
			List<String> HQ = new ArrayList<String>();

			
			Iterator<String> it = signInfo.keySet().iterator(); //map.keySet()????????????set????????????????????????????????????
	        while(it.hasNext()){
	        	String key = it.next();
	        	
	        	if (key.contains("??????")&&ibaSignInfo.equals("sign_Preparor")) {
					String str 	    = JsonUtils.toJsonStr((String) signInfo.get(key));
					String str1 = str.replace("\"", "");
					IBAUtil.setIBAValue(obj,"sign_Preparor", str1);
				}
	        	if (key.contains("??????")&&ibaSignInfo.equals("sign_Proofreader")) {
	        		String str 	    = JsonUtils.toJsonStr((String) signInfo.get(key));
	        		String str1 = str.replace("\"", "");
					IBAUtil.setIBAValue(obj,"sign_Proofreader", str1);
				}
	        	if (key.contains("??????")&&ibaSignInfo.equals("sign_Reviewer")) {
	        		String str 	    = JsonUtils.toJsonStr((String) signInfo.get(key));
	        		String str1 = str.replace("\"", "");
					IBAUtil.setIBAValue(obj,"sign_Reviewer", str1);
				}
	        	if (key.contains("??????")&&ibaSignInfo.equals("sign_Countersigner")) {
	        		HQ.add((String) signInfo.get(key));
					String str 	    = JsonUtils.toJsonStr(HQ);
					//?????? " [ ??? ] 
					String str1 = str.replace("\"", "").replace("[", "").replace("]", "");
					//String str1 = str.replaceAll("(?:\"|\5b|\5d)", "");
					//??????????????????????????????????????????????????????????????????
					IBAUtil.setIBAValue(obj,"sign_Countersigner", str1);
				}
	        	if (key.contains("??????")&&ibaSignInfo.equals("sign_Authorizor")) {
	        		String str 	    = JsonUtils.toJsonStr((String) signInfo.get(key));
	        		String str1 = str.replace("\"", "");
					IBAUtil.setIBAValue(obj,"sign_Authorizor", str1);
				}
	        	if (key.contains("??????")&&ibaSignInfo.equals("sign_Approver")) {
	        		String str 	    = JsonUtils.toJsonStr((String) signInfo.get(key));
	        		String str1 = str.replace("\"", "");
					IBAUtil.setIBAValue(obj,"sign_Approver", str1);
				}
	        	    
	        }
	        
	        
	        
	        
			
//			String str 	    = JsonUtils.toJsonStr(signInfo);
//			IBAUtil.setIBAValue(obj,ibaSignInfo, str);	
			
			
		}catch(Exception e){
			logger.error("???????????????????????????IBA??????.");
			e.printStackTrace();
		}

	}
	
	/**
	 * "???????????????"???"?????????????????????"iba
	 * @throws ParseException 
	 * @throws WTException 
	 * @throws WTPropertyVetoException 
	 * @throws RemoteException 
	 */
	public static void resetSignInfo(WTObject obj) throws RemoteException, WTPropertyVetoException, WTException, ParseException{
		IBAUtil.setIBAValue(obj,"sign_Preparor", "");
		IBAUtil.setIBAValue(obj,"sign_Proofreader", "");
		IBAUtil.setIBAValue(obj,"sign_Reviewer", "");
		IBAUtil.setIBAValue(obj,"sign_Countersigner", "");
		IBAUtil.setIBAValue(obj,"sign_Authorizor", "");
		IBAUtil.setIBAValue(obj,"sign_Approver", "");	
	}
	
	/**
	 * ??????PBO???????????????????????????????????????????????????
	 */
	public static void copyPBOContainerUserToProcess(ObjectReference self,WTObject pbo,String roleKey){
		try{
			if(pbo instanceof WTContained){
				Role role = Role.toRole(roleKey);
				WfProcess process = getProcess(self);
				WTContainer container = ((WTContained)pbo).getContainer();
				List	users    = PrincipalUtil.getContainerMemberByRole(container, roleKey);
				for(int i = 0 ; i < users.size();i++){
					WTUser user = (WTUser) users.get(i);
					assignUserToRole(process,role,user);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * ??????????????????????????????
	 */
	public static void changeLifeCycle(WTObject primaryBusinessObject, String toState) throws WTInvalidParameterException, LifeCycleException, WTException, RemoteException, InvocationTargetException{
		if(primaryBusinessObject instanceof LifeCycleManaged){
			LifeCycleManaged lcm = (LifeCycleManaged)primaryBusinessObject;
			State state = State.toState(toState);
			changeLifeCycle(lcm,state);
		}
	}
	
	
	public static void changeLifeCycle(LifeCycleManaged lcm, State state) throws WTInvalidParameterException, LifeCycleException, WTException, RemoteException, InvocationTargetException{
		if(!RemoteMethodServer.ServerFlag){
			RemoteMethodServer.getDefault().invoke("changeLifeCycle", CLASSNAME, null,  new Class[]{LifeCycleManaged.class,State.class},new Object[]{lcm,state});
			return ;
		}
		boolean accessEnforced = false;
		try {
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
			LifeCycleHelper.service.setLifeCycleState(lcm,state);
		}finally {
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
	}
	
    /**
     * ???????????????????????????
     */
    public static void assignUserToWfRole(ObjectReference self,String userId,String roleName) throws WTException{
    	WTUser user = getUserByName(userId);
    	WfProcess process = getProcess(self);
        if(user != null){
        	 Role role = Role.toRole(roleName);
        	 assignUserToRole(process,role,user);
        }else{
        	throw new WTException("??????{"+userId+"}?????????????????????");
        }
   }
    
    /**
     * ?????????????????????????????????,????????????????????????
     */
    public static void assignUserToWfRole(ObjectReference self,List<String> userList,String roleName) throws WTException{
    	for (int i = 0; i < userList.size(); i++) {
			String userId = userList.get(i);
			assignUserToWfRole(self,userId,roleName);
		}
    }
    
    public static void assignUserToRole(TeamManaged tm,Role role,WTPrincipal thePrincipal) throws TeamException, WTException{
        
        Team team = TeamHelper.service.getTeam(tm);
        Enumeration penum = team.getPrincipalTarget(role);
        if(penum.hasMoreElements()){
        	Object obj = penum.nextElement();
        	if(obj instanceof WTPrincipalReference){
        		WTPrincipalReference p = (WTPrincipalReference) obj;
        		WTPrincipal tempPrincipal = p.getPrincipal();
        		long ida2a2 = thePrincipal.getPersistInfo().getObjectIdentifier().getId();
        		long ida2a2Temp = tempPrincipal.getPersistInfo().getObjectIdentifier().getId();
	        	if(ida2a2 == ida2a2Temp){
	        		logger.debug("???????????????????????????,??????????????????:" + p.getName());
	        		return;
	        	}
        	}
        }
        team.addPrincipal(role,thePrincipal);
        logger.debug("????????????({})?????????({})??????",thePrincipal.getName(),role.getDisplay(Locale.SIMPLIFIED_CHINESE));
        team = (Team) PersistenceHelper.manager.refresh(team);
        team = (Team) PersistenceHelper.manager.save(team); 
    }
/*    *//**
     * ?????????????????????????????????,????????????????????????
     *//*
    public static void assignUsersToWfRole(ObjectReference self, List principalList,String roleName) throws WTException{
		if(!RemoteMethodServer.ServerFlag){
			try {
				Class cla[] = {ObjectReference.class,List.class,String.class};
				Object obj[] = {self,principalList,roleName};
				RemoteMethodServer.getDefault().invoke("assignUsersToWfRole", CLASSNAME, null,  cla,  obj);
				return ;
			} catch (Exception e) {
				logger.error("assignUsersToWfRole,invoke error",e);
			}
		}
		boolean flag = false;
		try{
			flag = SessionServerHelper.manager.setAccessEnforced(flag);
			WfProcess wfp = getProcess(self);
	        Role role = Role.toRole(roleName);
	        Team team = TeamHelper.service.getTeam(wfp);
	        for(int i = 0 ; i < principalList.size() ; i ++){
	        	WTPrincipal p = (WTPrincipal) principalList.get(i);
	        	team.addPrincipal(role,p);
		        team = (Team) PersistenceHelper.manager.refresh(team);
		        team = (Team) PersistenceHelper.manager.save(team);
		        logger.debug("????????????({})???????????????({})??????",p.getName(),role.getDisplay(Locale.SIMPLIFIED_CHINESE));
	        }  	
		}finally{
			SessionServerHelper.manager.setAccessEnforced(flag);		
		}
   }*/

    /**
     * ??????pbo??????????????????????????????????????????????????????
     */
    public static boolean hasUserInRole(ObjectReference self,WTObject primaryBusinessObject,String roleName) throws TeamException, WTException{
		if(!hasUserInRole(primaryBusinessObject,roleName)){
			return hasUserInRole(self,roleName);
		}else{
			return true;
		}
    }
    
    public static boolean hasUserInRole(ObjectReference self,String roleName) throws TeamException, WTException{
		WfProcess wfp = getProcess(self);
		Team team = TeamHelper.service.getTeam(wfp);
		return hasUserInTeam(team,roleName);
    }
    
    public static boolean hasUserInRole(WTObject primaryBusinessObject,String roleName) throws TeamException, WTException{
        Team team = TeamHelper.service.getTeam((TeamManaged)primaryBusinessObject);
        return hasUserInTeam(team,roleName);
    }
    
    public static boolean hasUserInTeam(Team team,String roleName) throws WTException{
        boolean flag = false;
        Role role = Role.toRole(roleName);
        Enumeration penum = team.getPrincipalTarget(role);
        if(penum == null){
        	return flag;
        }
        while(penum.hasMoreElements()){
        	WTPrincipalReference principalRef = (WTPrincipalReference) penum.nextElement();
        	WTPrincipal principal = principalRef.getPrincipal();
        	if(principal instanceof WTUser){
        		flag = true;
        		break;
        	}
	   		 if (principal instanceof WTGroup) {
	   			flag = PrincipalUtil.hasUserInGroup((WTGroup)principal);
				 if(flag){
					 break;
				 }
			 }
        }
        logger.debug("??????:" + role.getDisplay(Locale.SIMPLIFIED_CHINESE)+",???????????????:" + flag);
        return flag;
    }
    
    public static Set<WTUser> getUsersFromPBOTeam(WTObject primaryBusinessObject,String roleName) throws TeamException, WTException{
    	Set<WTUser> result = null;
    	if(primaryBusinessObject instanceof TeamManaged){
    		result = getUsersFromTeam((TeamManaged)primaryBusinessObject,roleName);
    	}else{
    		result = new HashSet<WTUser>();
    	}
    	logger.debug("??????:"+primaryBusinessObject.getDisplayIdentity() + "??????:" + roleName+",?????????:" + result.size());
    	return result;
    }
    
    /**
     * ??????????????????????????????????????????
     * @param primaryBusinessObject
     * @param roleName
     * @return
     * @throws TeamException
     * @throws WTException
     */ 
    public static Set<WTUser> getUsersFromTeam(TeamManaged tm,String roleName) throws TeamException, WTException{
        Set<WTUser> result = new HashSet<WTUser>();
    	Role role = Role.toRole(roleName);
        Team team = TeamHelper.service.getTeam(tm);
        boolean flag = false;
        Enumeration penum = team.getPrincipalTarget(role);
        if(penum == null){
        	return result;
        }
        while(penum.hasMoreElements()){
        	WTPrincipalReference principalRef = (WTPrincipalReference) penum.nextElement();
        	WTPrincipal principal = principalRef.getPrincipal();
        	if(principal instanceof WTUser){
        		WTUser user = (WTUser) principal;
        		if(!result.contains(principal)){
        			result.add(user);
        		}
        	} 
	   		 if (principal instanceof WTGroup) {
	   			WTGroup group = (WTGroup)principal;
	   			result.addAll(PrincipalUtil.getUsersByGroup(group));
			 }
        }
        return result;
    }
    
    public static void removeUsersFromPBOTeam(WTObject primaryBusinessObject,String roleName) throws TeamException, WTException{
    	if(primaryBusinessObject instanceof TeamManaged){
    		TeamManaged tm = (TeamManaged) primaryBusinessObject;
    		removeUsersFromPBOTeam(tm,roleName);
    		 logger.debug("????????????,??????:"+primaryBusinessObject.getDisplayIdentity()+ "??????:" + roleName);
    	}
    }
    
    /**
     * ????????????????????????????????????
     * @param primaryBusinessObject
     * @param roleName
     * @return
     * @throws TeamException
     * @throws WTException
     */
    public static void removeUsersFromPBOTeam(TeamManaged tm,String roleName) throws TeamException, WTException{
        Set<WTUser> result = new HashSet<WTUser>();
    	Role role = Role.toRole(roleName);
        Team team = TeamHelper.service.getTeam(tm);
        boolean flag = false;
        Enumeration penum = team.getPrincipalTarget(role);
        if(penum == null){
        	return;
        }
        while(penum.hasMoreElements()){
        	WTPrincipalReference principalRef = (WTPrincipalReference) penum.nextElement();
        	WTPrincipal principal = principalRef.getPrincipal();
        	TeamHelper.service.deleteRolePrincipalMap(role, principal, team);
        }
    }   
 
    /**
	 * ??????????????????????????????????????????????????????,?????????????????????
	 */
	public static void validateUserInProcess(ObjectReference self, WTObject primaryBusinessObject,
			List roleNames) throws WTException {
		String result = "";
		for (Object object : roleNames) {
			if (!object.equals("")) {
				boolean flag = hasUserInRole(self, (String) object);
				Role role = Role.toRole((String) object);
				if (!flag) {
					result += role.getDisplay(Locale.SIMPLIFIED_CHINESE) + ",";
				}
			}
		}
		if (!result.equals("")) {
			throw new WTException("??????{" + result + "}???????????????");
		}
	}
	
    /**
	 * ??????????????????????????????????????????????????????,?????????????????????,?????????????????????;??????
	 */
	public static void validateUserInProcess(ObjectReference self, WTObject primaryBusinessObject,
			String roleNames,String split) throws WTException {
		if(roleNames == null || "".equals(roleNames)){
			return;
		}
		String roleNamesArray[] = roleNames.split(split);
		String result = "";
		for (String roleName : roleNamesArray) {
			if (!roleName.equals("")) {
				boolean flag = hasUserInRole(self, roleName);
				if(!flag){
					flag = hasUserInRole(primaryBusinessObject, roleName);
				}
				Role role = Role.toRole(roleName);
				if (!flag) {
					result += role.getDisplay(Locale.SIMPLIFIED_CHINESE) + ",";
				}
			}
		}
		if (!result.equals("")) {
			throw new WTException("????????????{" + result + "}??????");
		}
	}
	
	
    /**
     * ?????????????????????pbo????????????,??????????????????
     */
    public static void assignUsersToPboTeam(WTObject pbo,String roleName,WTPrincipal pricipal) throws WTException, RemoteException, InvocationTargetException{        
    	if(!RemoteMethodServer.ServerFlag){
       	   Class cla[] = {WTObject.class,String.class,WTPrincipal.class};
       	   Object obj[]= {pbo,roleName,pricipal}; 
       	   RemoteMethodServer.getDefault().invoke("assignUsersToPboTeam", CLASSNAME, null, cla, obj);
       	   return;
          } 
		boolean flag = false;
		try{
			 flag = SessionServerHelper.manager.setAccessEnforced(flag);
	         Role role = Role.toRole(roleName);
	         Team team = TeamHelper.service.getTeam((TeamManaged)pbo);
	         team.addPrincipal(role,pricipal);
	         team = (Team) PersistenceHelper.manager.refresh(team);
	         team = (Team) PersistenceHelper.manager.save(team);                             
	         TeamReference tr = TeamReference.newTeamReference(team);
	         TeamHelper.service.augmentRoles((LifeCycleManaged) pbo, tr);
	         logger.debug("??????WTPrincipal({})???pbo????????????({})??????",pricipal.getName(),role.getDisplay(Locale.SIMPLIFIED_CHINESE));
		}finally{
			SessionServerHelper.manager.setAccessEnforced(flag);		
		}
    }
    
	public static void completeUserTask(wt.fc.WTObject pbo,String activityName,String arole, String userName) throws WTException{
		Enumeration enumx = WorkflowHelper.service.getUncompletedWorkItems(pbo,Role.toRole(arole));
	    if (enumx != null) {
	        while (enumx.hasMoreElements()) {
	            WorkItem workItem = (WorkItem) enumx.nextElement();
				WfActivity currentActivity = (WfActivity)workItem.getSource().getObject();
				if(currentActivity.getName().equals(activityName)){
					 //????????????????????????
					WTPrincipalReference userref = workItem.getOwnership().getOwner();
					if(userref.getName().equals(userName)){
						WorkflowHelper.service.workComplete(workItem,userref, null); 
						logger.debug("????????????{"+userName+"}?????????{"+activityName+"}");
					}
				}
			}
		}
	}
    
	/**
	 * ???????????????????????????????????????????????????????????????
	 * @param pbo
	 * @param activityName
	 * @param arole
	 * @throws WTException
	 */ 
	public static void completeUserTask(wt.fc.WTObject pbo,String activityName,String arole) throws WTException{
		Enumeration enumx = WorkflowHelper.service.getUncompletedWorkItems(pbo,Role.toRole(arole));
	    if (enumx != null) {
	        while (enumx.hasMoreElements()) {
	            WorkItem workItem = (WorkItem) enumx.nextElement();
				WfActivity currentActivity = (WfActivity)workItem.getSource().getObject();
				if(currentActivity.getName().equals(activityName)){
					 //????????????????????????
					WTPrincipalReference userref = workItem.getOwnership().getOwner();
					WorkflowHelper.service.workComplete(workItem,userref, null); 
					logger.debug("????????????{"+userref.getName()+"}?????????{"+activityName+"}");
				}
			}
		}
	}
	
    /**
     * ???????????????????????????,??????????????????
     * @param self
     * @param roleName 
     */
    public static void removeUsersFromWfRole(ObjectReference self,String roleName){
		if(!RemoteMethodServer.ServerFlag){
			try {
				Class cla[] = {ObjectReference.class,String.class};
				Object obj[] = {self,roleName};
				RemoteMethodServer.getDefault().invoke("removeUsersFromWfRole", CLASSNAME, null,  cla,  obj);
				return ;
			} catch (Exception e) {
				logger.error("removeUsersFromWfRole,invoke error",e);
			}
		}
		WTPrincipal curUser = null;
		try {
			WTPrincipal administrator = SessionHelper.manager.getAdministrator();
			curUser = SessionContext.setEffectivePrincipal(administrator);
			WfProcess wfp = getProcess(self);
			if (wfp != null) { 
				Team team = (Team) ((TeamManaged) wfp).getTeamId().getObject();
				Role myRole = Role.toRole(roleName);
				TeamHelper.service.deleteRole(myRole,team);
		        team = (Team) PersistenceHelper.manager.refresh(team); 
		        team = (Team) PersistenceHelper.manager.save(team);
		        logger.debug("??????????????????{}??????",myRole.getDisplay(Locale.SIMPLIFIED_CHINESE));
			}
		}catch(Exception e){
			logger.error("??????????????????"+roleName+"????????????",e);
		}finally {
			if(curUser != null){
				SessionContext.setEffectivePrincipal(curUser);
			}
		}
    }
    
    
	/**
	 * ??????workitemid????????????????????????
	 * 11:19:02 AM
	 * @param workitemId
	 * @return
	 * @throws WTException
	 * @throws InvocationTargetException 
	 * @throws RemoteException 
	 */
	public static String getActivityName(String workitemId) throws WTException, RemoteException, InvocationTargetException{
		if(!RemoteMethodServer.ServerFlag){
			Class cla[] = {String.class};
			Object obj[] = {workitemId};
			return (String) (RemoteMethodServer.getDefault().invoke("getActivityName",CLASSNAME, null, cla, obj));
		}
    	boolean accessEnforced = false;
    	try{
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced); 
			WorkItem workItem   = (WorkItem) new ReferenceFactory().getReference(workitemId).getObject();
			WfActivity activity = (WfActivity) workItem.getSource().getObject(); 
			return activity.getName();
		}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
	}
	
	public static String getActivityName(WorkItem workItem) throws WTException, RemoteException, InvocationTargetException{
		if(!RemoteMethodServer.ServerFlag){
			Class cla[]  = {WorkItem.class};
			Object obj[] = {workItem};
			return (String) RemoteMethodServer.getDefault().invoke("getActivityName",CLASSNAME, null, cla, obj);
		}
    	boolean accessEnforced = false;
    	try{
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced); 
			WfActivity activity = (WfActivity) workItem.getSource().getObject(); 
			return activity.getName();
    	}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
	}
	
	public static WfActivity getWfActivity(WorkItem workItem) throws WTException, RemoteException, InvocationTargetException{
		if(!RemoteMethodServer.ServerFlag){
			Class cla[] = {WorkItem.class};
			Object obj[] = {workItem};
			return (WfActivity) (RemoteMethodServer.getDefault().invoke("getWfActivity",CLASSNAME, null, cla, obj));
		}
    	boolean accessEnforced = false;
    	try{
    		accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced); 
			WfActivity activity = (WfActivity) workItem.getSource().getObject(); 
			return activity;
		}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
		
	}
	
	/**
	 * ???????????????????????????
	 * @throws InvocationTargetException 
	 * @throws RemoteException 
	 */
	public static String getWfTemplateName(String workitemId)throws WTRuntimeException, WTException, RemoteException, InvocationTargetException {
		WorkItem workItem = (WorkItem) new ReferenceFactory().getReference(workitemId).getObject();
		return getWfTemplateName(workItem);
	}
	
	/**
	 * ???????????????????????????
	 * @param workitem
	 */
	public static String getWfTemplateName(WorkItem workItem)throws WTRuntimeException, WTException {
		if(!RemoteMethodServer.ServerFlag){
			Class cla[]  = {WorkItem.class};
			Object obj[] = {workItem};
			try {
				return (String) (RemoteMethodServer.getDefault().invoke("getWfTemplateName",CLASSNAME, null, cla, obj));
			} catch (Exception e) {
				logger.error("getWfTemplateName,invoke error!",e);
				e.printStackTrace();
			} 
		}
		boolean accessEnforced = false;
		try{
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
			WfActivity currentActivity = (WfActivity) workItem.getSource().getObject();
			WfProcess parentProcess = currentActivity.getParentProcess();
			WfProcessTemplate processTemplate = (WfProcessTemplate) parentProcess.getTemplate().getObject();
			String processTemplateName = processTemplate.getName();
			return processTemplateName;
		}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
	}
	
	public static Map getWfObjects(NmCommandBean nmcommandbean) throws WTException{
		WorkItem workItem = (WorkItem) nmcommandbean.getPageOid().getRefObject();
		return getWfObjects(workItem);
	}
	
	/**
	 * ???????????????????????????
	 * @param workitem
	 */
	public static Map getWfObjects(WorkItem workItem)throws WTRuntimeException, WTException {
		if(!RemoteMethodServer.ServerFlag){
			Class cla[]  = {WorkItem.class};
			Object obj[] = {workItem};
			try {
				return (Map) (RemoteMethodServer.getDefault().invoke("getWfObjects",CLASSNAME, null, cla, obj));
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		Map result = new HashMap();
		boolean accessEnforced = false;
		try{
			accessEnforced       = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
			WfActivity activity  = (WfActivity) workItem.getSource().getObject();
			
			WfProcess         process      = activity.getParentProcess();
			WfProcessTemplate template     = (WfProcessTemplate) process.getTemplate().getObject();
			String            templateName = template.getName();
        	WTObject          pbo 		   = getPrimaryBusinessObject(workItem);
        	result.put("TASKNAME", activity.getName());
			result.put("ACTIVITY", activity);
			result.put("PROCESS", process);
			result.put("TEMPLATENAME", templateName);
			result.put("PBO", pbo);
			result.put("WORKITEM", workItem);
			result.put("PROCESSOID", new ReferenceFactory().getReferenceString(process));
			return result;
		}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
	}
	
	
	/**
	 * ??????????????????
	 */
	public static WfProcess getWfProcess(WorkItem workItem)throws WTRuntimeException, WTException, RemoteException, InvocationTargetException {
		if(!RemoteMethodServer.ServerFlag){
			Class cla[] = {WorkItem.class};
			Object obj[] = {workItem};
			return (WfProcess) (RemoteMethodServer.getDefault().invoke("getWfProcess",CLASSNAME, null, cla, obj));
		}
		boolean AccessEnforced = false;
		
		try{
			AccessEnforced = SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
			WfActivity currentActivity = (WfActivity) workItem.getSource().getObject();
			WfProcess parentProcess = currentActivity.getParentProcess();
			return parentProcess;
		}finally{
			SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
		}
	}
	
	
	/**
	 * ????????????????????????(????????????)??????????????? 
	 */
	public static boolean isWorkItemComplete(String workitemId) throws WTException{
		boolean flag = true;
		if(workitemId != null){
			ReferenceFactory rf =  new ReferenceFactory();
			WorkItem workItem = (WorkItem)rf.getReference(workitemId).getObject();
			flag = workItem.isComplete();
		}	
		return flag;
	}
	
	
	/**
	 * 
	 * @param s
	 * @return
	 * @throws WTException
	 */
    public static WTUser getUserByName(String userName)throws WTException{
        WTUser wtuser = null;
        QuerySpec queryspec = null;
        QueryResult queryresult = null;
        queryspec = new QuerySpec(wt.org.WTUser.class);
        if(userName.equalsIgnoreCase("wcadmin"))
        {
        	userName = "Administrator";
        }
        SearchCondition searchcondition = new SearchCondition(wt.org.WTUser.class, "name", "LIKE", userName, false);
        queryspec.appendSearchCondition(searchcondition);
        queryresult = PersistenceHelper.manager.find(queryspec);
        if(queryresult.hasMoreElements())
        {
            wtuser = (WTUser)queryresult.nextElement();
        }
        return wtuser;
    }
    
    /**
     * PBO???????????????
     * @param workItem
     * @param throwWTexception ??????true,???????????????????????????????????????,false???????????????????????????
     * @return
     * @throws WTException
     * @throws WTPropertyVetoException
     */
    public static List<Workable> validatePBOCheckout(WorkItem workItem,boolean throwWTexception) throws WTException, WTPropertyVetoException {
    	WTObject pbo = null;
    	try{
    		PersistentReference ref = workItem.getPrimaryBusinessObject();
    		if ((ref != null) && (ref.getKey() != null) && (ref.getObject() != null)){
    			pbo = (WTObject) ref.getObject();
    		}
    	}catch(Exception e){
    		logger.error("??????PBO??????:" + e.getLocalizedMessage());
    	}
    	return validatePBOCheckout(pbo,throwWTexception);
     }
    
    //PBO???????????????
    public static List<Workable> validatePBOCheckout(WTObject pbo,boolean throwWTexception) throws WTException, WTPropertyVetoException {
        if(pbo == null){
        	return null;
        }
        List<Workable> workables = new ArrayList<Workable>();
        List<String> validateResult = new ArrayList<String>();
   		try{
   			if(pbo instanceof Workable){
   				workables.add((Workable)pbo);
   				if(pbo instanceof WTPart){
   					workables.addAll(PartUtil.getAssEpmDocument((WTPart)pbo));
   				}			
   			}else if(pbo instanceof PromotionNotice){
   				ArrayList<Promotable> promotables = PromotionNoticeUtil.getPromotionNoticeItems((PromotionNotice)pbo);
   				for(Promotable promotable : promotables){
   					workables.add((Workable)promotable);
   				}
	   		 }else if (pbo instanceof WTChangeOrder2) {
	         	   WTChangeOrder2 order = (WTChangeOrder2) pbo;
	         	   List<Changeable2> after = ChangeUtil.getChangeNoticeItemsAfter(order);
	               for (int i = 0 ; i < after.size() ; i ++) {
	             	  Changeable2 able = after.get(i);
	                   if (able instanceof Workable) {
	                	   workables.add((Workable)able);
	                   } 
	               }
	   		 }
   			for(Workable able: workables){
   				if(WorkInProgressHelper.isCheckedOut((Workable)able)){
   					validateResult.add(((WTObject)able).getDisplayIdentity()+"");
   				}
   			}
   		}catch(Exception e){
   			throw new WTException("????????????????????????:"+e.getLocalizedMessage());
   		}
   		if(throwWTexception && validateResult.size() > 0){
   			throw new WTException("?????????????????????,????????????:"+validateResult.toString());
   		}
   		return workables;
     }
    
    /**
     * ??????????????????????????????????????????
     */
	public static List getVoteList(WorkItem workItem) throws WTRuntimeException, WTException, RemoteException, InvocationTargetException{
		if (RemoteMethodServer.ServerFlag) {
			Class cla[] = { WorkItem.class};
			Object obj[] = { workItem}; 
			return (List) RemoteMethodServer.getDefault().invoke("getVoteList",CLASSNAME, null, cla, obj);
		}
		List voteList = new ArrayList();
		boolean flag = false;
		try{
			flag = SessionServerHelper.manager.setAccessEnforced(flag);
			WfActivity currentActivity = (WfActivity) workItem.getSource().getObject();
			return getVoteList(currentActivity);
		}finally{
			SessionServerHelper.manager.setAccessEnforced(flag);
		}
	}
	
    /**
     * ??????????????????????????????????????????
     */
	public static List getVoteList(WfActivity currentActivity) throws WTRuntimeException, WTException, RemoteException, InvocationTargetException{
		if (RemoteMethodServer.ServerFlag) {
			Class cla[] = { WfActivity.class};
			Object obj[] = { currentActivity}; 
			return (List) RemoteMethodServer.getDefault().invoke("getVoteList",CLASSNAME, null, cla, obj);
		}
		List voteList = new ArrayList();
		boolean flag = false;
		try{
			flag = SessionServerHelper.manager.setAccessEnforced(flag);
			UserEventVector uev = currentActivity.getUserEventList();
			if(!uev.isEmpty()){
				 Enumeration enu = uev.elements();
				 while(enu.hasMoreElements()){
					 Object obj = enu.nextElement();
					 String s = obj.toString();
					 voteList.add(s);
				 }
			 }
			return voteList;
		}finally{
			SessionServerHelper.manager.setAccessEnforced(flag);
		}
	}
	
	/**
	 * ???????????????
	 * @param pbo ??????pbo
	 * @param team_spec ???????????????,????????????null??????
	 * @param context_ref ????????????ref,??????pbo???containerRef
	 * @param processname ??????????????????
	 * @param templateName ??????????????????
	 * @param variables    ?????????????????????????????????,key???value
	 */
    public static WfProcess createProcess(WTObject pbo, Object team_spec, WTContainerRef context_ref,String processname, String templateName, Map variables) throws WTException {
    	boolean accessEnforced = false;
    	try{
    		accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
	    	WfProcess wfprocess = null;
	        WfProcessDefinition processDefinition = wt.workflow.definer.WfDefinerHelper.service.getProcessDefinition(templateName, context_ref);	        
	        wfprocess = WfEngineHelper.service.createProcess(processDefinition, team_spec, context_ref);
	        wfprocess.setName(processname);
	        ProcessData processData = wfprocess.getContext();
	        processData.setValue("primaryBusinessObject", pbo);
	        if (variables != null && !variables.isEmpty()) {
	        	Iterator keys = variables.keySet().iterator();
	        	while (keys.hasNext()) {
	        		String key = (String) keys.next();
	        		Object value = variables.get(key); 
	        		processData.setValue(key, value);
	        	}
	        }
	        PersistenceHelper.manager.save(wfprocess);
	        PersistenceHelper.manager.refresh(wfprocess);
	        wfprocess = wfprocess.start(processData, 0, true);
	        logger.debug("?????????????????????:" + wfprocess.getDisplayIdentity());
	        return wfprocess;   	   
       }finally{
    	   SessionServerHelper.manager.setAccessEnforced(accessEnforced);
       }

    }
	
    /**
	 * ??????workitemOid??????pbo??????
	 * @param workitemOid
	 */
	public static Persistable getPrimaryBusinessObject(String workitemOid) throws WTException {
		ReferenceFactory rf = new ReferenceFactory();
		WorkItem workitem = (WorkItem) rf.getReference(workitemOid).getObject();
		return getPrimaryBusinessObject(workitem);
	}
	
	public static WTObject getPrimaryBusinessObject(WorkItem paramWorkItem){
		WTObject pbo = null;
	    PersistentReference localPersistentReference = paramWorkItem.getPrimaryBusinessObject();
	    try {
	      if ((localPersistentReference != null) && (localPersistentReference.getKey() != null) && (localPersistentReference.getObject() != null)) {
	        pbo = (WTObject) localPersistentReference.getObject();
	      }
	    }
	    catch (Exception e){
	    	//e.printStackTrace();
	    	logger.error("??????PBO??????:" + paramWorkItem.getDisplayIdentity());
	    }
	    return pbo;
	  }
	
	/**
	 * ??????????????????????????????????????????????????????????????????
	 * ??????????????????,????????????????????????????????????
	 * @param self ?????????self??????,???????????????self
	 * @param event ????????????
	 */
	public static boolean anyOneSelectEvent(ObjectReference self,String event) throws WfException{
		wt.workflow.work.WfAssignedActivity activity = ((wt.workflow.work.WfAssignedActivity)self.getObject());
		Vector userEvents = activity.getUserEventList();
		logger.debug("????????????:" + activity.getName() + ",???????????????:" + userEvents.size());
		for(int i = 0 ; i < userEvents.size();i++){
			logger.debug("????????????:" + userEvents.get(i));
		} 
		Vector selectedEvents = wt.workflow.work.WfTally.any(self,userEvents);
		logger.debug("???????????????:" + selectedEvents.size());
		for(int i = 0 ; i < selectedEvents.size();i++){
			logger.debug("????????????:" + selectedEvents.get(i)); 
		} 
		logger.debug("selectedEvents.contains(event):" + selectedEvents.contains(event));
		if (selectedEvents.contains(event)){       
			return true;
		}
		return false;
	}
	
	
	/**
	 * ???????????????????????????
	 * 10:34:11 AM
	 * @param obj
	 * @return
	 */
    public static WfProcess getProcess(Object obj){
        if (obj == null) return null;
    	if(obj instanceof WfProcess){
    		return (WfProcess)obj;
    	}
        try{
    	    Persistable persistable = null;
    	    if (obj instanceof ObjectIdentifier)
    	        persistable = PersistenceHelper.manager.refresh((ObjectIdentifier)obj);
    	    else if (obj instanceof ObjectReference)
    	        persistable = ((ObjectReference)obj).getObject();
    	    else if (obj instanceof Persistable){
    	    	persistable = (Persistable)obj;
    	    }
    	    if (persistable instanceof WorkItem)
    	        persistable = ((WorkItem)persistable).getSource().getObject();
    	    if (persistable instanceof WfActivity)
    	        persistable = ((WfActivity)persistable).getParentProcess();
    	    if(persistable instanceof WfConnector)
                persistable = ((WfConnector)persistable).getParentProcessRef().getObject();
    	    if (persistable instanceof WfBlock)
    	        persistable = ((WfBlock)persistable).getParentProcess();
    	    if (persistable instanceof WfProcess)
    	        return (WfProcess)persistable;
    	    else
    	        return null;
        }
        catch (Exception e){
            System.out.println("getProcess : error");
            e.printStackTrace();
        }
        return null;
    }
    
	public static WfProcess getProcessByWorkitemOid(String WorkitemOid) throws WTRuntimeException, WTException, RemoteException, InvocationTargetException{
		if(!RemoteMethodServer.ServerFlag){
			Class cla[] = {String.class};
			Object obj[] = {WorkitemOid};
			return (WfProcess) (RemoteMethodServer.getDefault().invoke("getProcessByWorkitemOid",CLASSNAME, null, cla, obj));
		}
		boolean AccessEnforced = false;
		try{
			SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
			WorkItem workItem = (WorkItem) new ReferenceFactory().getReference(WorkitemOid).getObject();
			WfActivity currentActivity = getWfActivity(workItem);
			WfProcess parentProcess    = currentActivity.getParentProcess();
			return parentProcess;
		}finally{
			SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
		}
	}
	
	public static WTPrincipalReference getProcessCreator(WorkItem workItem) throws WTRuntimeException, WTException, RemoteException, InvocationTargetException{
		if(!RemoteMethodServer.ServerFlag){
			Class cla[] = {WorkItem.class};
			Object obj[] = {workItem};
			return (WTPrincipalReference) (RemoteMethodServer.getDefault().invoke("getProcessByWorkitem",CLASSNAME, null, cla, obj));
		}
		boolean AccessEnforced = false;
		try{
			SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
			WfProcess parentProcess    = getWfProcess(workItem);
			WTPrincipalReference pr = parentProcess.getCreator();
			return pr;
		}finally{
			SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
		}
	}
	
	/**
	 * ???????????????
	 * @param workItemId
	 * @param variableName
	 * @return
	 * @throws WTRuntimeException
	 * @throws WTException
	 */
    public static Object getLocaleVariable(String workItemId,String variableName) throws WTRuntimeException, WTException{
        boolean AccessEnforced = false;
        try{
        	AccessEnforced = SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
        	WorkItem workItem = (WorkItem)new ReferenceFactory().getReference(workItemId).getObject();
            WfActivity wfactivity = (WfActivity)workItem.getSource().getObject();
            Object obj = getVariable(wfactivity,variableName);
            if(obj == null){
            	logger.error("????????????????????????????????????:"+variableName);
            }
            return obj;
        }finally{
        	SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
        }
     }
    
	/**
	 * ??????workitemId(????????????id)?????????????????????????????????????????????
	 * @throws WTException 
	 * @throws WTRuntimeException 
	 */
    public static Object getVariable(String workItemId,String variableName) throws WTRuntimeException, WTException{
    	//id????????????
    	WorkItem workItem = (WorkItem)new ReferenceFactory().getReference(workItemId).getObject();
    	return getVariable(workItem,variableName);
     }
    
    public static Object getVariable(WorkItem workItem,String variableName) throws WTException{
        if(!RemoteMethodServer.ServerFlag){
     	   Class cla[] = {WorkItem.class,String.class};
     	   Object obj[]= {workItem,variableName};
     	  try {
			return RemoteMethodServer.getDefault().invoke("getVariable",CLASSNAME, null, cla, obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
        } 
        boolean AccessEnforced = false;
        try{
        	AccessEnforced = SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
            WfActivity wfactivity = (WfActivity)workItem.getSource().getObject();
            WfProcess process = wfactivity.getParentProcess();
            Object obj = getVariable(process,variableName);
            if(obj == null){
            	logger.error("????????????????????????:"+variableName);
            }
            return obj;
        }finally{
        	SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
        }
     }
    
    
	/**
	 * ??????????????????
	 */
    public static Object getVariable(WfExecutionObject process,String variableName){

        boolean AccessEnforced = false;
        try{
        	AccessEnforced = SessionServerHelper.manager.setAccessEnforced(false);
            ProcessData processdata = process.getContext();
            Object obj = processdata.getValue(variableName);
            if(obj == null){
            	logger.error("????????????????????????:"+variableName);
            }
            return obj;
        }finally{
        	SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
        }
     }
    
    public static Object getVariableByProcessOid(String processOid,String variableName) throws WTRuntimeException, WTException{
    	wt.workflow.engine.WfProcess process =  (WfProcess) new wt.fc.ReferenceFactory().getReference(processOid).getObject();
    	return getVariable(process,variableName);
    	
    }
    /**
     * ????????????????????????
     * @throws WTException 
     * @throws WTRuntimeException 
     * @throws InvocationTargetException 
     * @throws RemoteException 
     */
    public static boolean updateVariable(String workItemId,String variableKey,Object variableValue) throws WTRuntimeException, WTException, RemoteException, InvocationTargetException{
		boolean AccessEnforced = false;
		try{
			AccessEnforced     = SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
           /* WorkItem workItem = (WorkItem)new ReferenceFactory().getReference(workItemId).getObject();
            WfActivity wfactivity = (WfActivity)workItem.getSource().getObject();*/
            WfProcess process = getProcessByWorkitemOid(workItemId);
            updateVariable(process,variableKey,variableValue);
			return true;
		}finally{
        	SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
		}
    }
    
    
    /**
     * ????????????????????????
     * @throws WTException 
     */
    public static boolean updateVariable(WfProcess process,String variableKey,Object variableValue) throws WTException{
		boolean AccessEnforced = false;
		try{
			AccessEnforced     = SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
	    	ProcessData p = process.getContext(); 
			p.setValue(variableKey,variableValue);
			process = (WfProcess) PersistenceHelper.manager.save(process);
			PersistenceHelper.manager.refresh(process); 
			logger.debug("??????????????????,key-->" + variableKey +",value-->" + variableValue);
			logger.debug("???????????????????????????" + getVariable(process,variableKey)); 
			return true;
		}finally{
        	SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
		}
    }
    
    /**
     * ????????????????????????????????????
     * state???containerRef???????????????null
     */
    public static QueryResult getProcesses(Persistable persistable,WfState state,WTContainerRef containerRef) throws WTException {
        QueryResult qr = WfEngineHelper.service.getAssociatedProcesses(persistable, state,containerRef);
        return qr;
    }
    
    public static String getCurrentTimeStr(){
    	String format = "yyyyMMddHHmmss";
    	return getCurrentTimeStr(format);
    }
    
    public static String getCurrentTimeStr(String format){
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
		String time = sdf.format(new Date(System.currentTimeMillis()));
		return time;
    }
    
    public static String getTimeStr(Date stamp,String format){
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
		String time = sdf.format(stamp);
		return time;
    }
    
	public static QueryResult getWorkItemsByWfProcess(WfProcess process,
			Boolean queryComplete) throws WTException, RemoteException,
			InvocationTargetException {
		if (!RemoteMethodServer.ServerFlag) {
			Class cla[] = { WfProcess.class, Boolean.class };
			Object obj[] = { process, queryComplete };
			return (QueryResult) RemoteMethodServer.getDefault().invoke(
					"getWorkItemsByWfProcess", CLASSNAME, null, cla, obj);
		}

		QuerySpec qs = new QuerySpec(WorkItem.class);
		qs.setAdvancedQueryEnabled(true);

		if (queryComplete) {
			qs.appendWhere(new SearchCondition(WorkItem.class,
					WorkItem.COMPLETED_BY, false), 0);
		}

		QuerySpec subQS = new QuerySpec();
		subQS.setAdvancedQueryEnabled(true);
		int i = subQS.appendClassList(WfAssignedActivity.class, false);
		subQS.appendSelectAttribute("thePersistInfo.theObjectIdentifier.id", i,
				false);
		subQS.appendWhere(new SearchCondition(WfAssignedActivity.class,
				"parentProcessRef.key", "=", PersistenceHelper
						.getObjectIdentifier(process)));
		SubSelectExpression subSelectExpression = new SubSelectExpression(subQS);

		ClassAttribute classattribute = new ClassAttribute(WorkItem.class,
				"source.key.id");
		SearchCondition sc = new SearchCondition(classattribute, "IN",
				subSelectExpression);
		if (qs.getConditionCount() > 0) {
			qs.appendAnd();
		}
		qs.appendWhere(sc, 0);

		// ??????????????????????????????
		qs.appendOrderBy(WorkItem.class, WorkItem.MODIFY_TIMESTAMP, true);

		QueryResult qr = PersistenceServerHelper.manager.query(qs);
		return qr;
	}
	
	/**
	 * ????????????????????????,??????????????????????????????????????????????????????
	 * 
	 * ??????map,key????????????,value???userName + "$" + userFullName + "$" + hqOcompleteTime
	 */
	public static Map<String, String> getSignInfo(ObjectReference self,String timeFormat) throws RemoteException, InvocationTargetException, WTException{
		WfProcess process = getProcess(self);
		return getSignInfo(process,timeFormat);
	}
	
	/**
	 * ????????????????????????,??????????????????????????????????????????????????????
	 * 
	 * ??????map,key????????????,value??? ????????????+" " + ????????????,???????????????,????????????????????????,?????????,??????
	 */
	public static Map<String, String> getSignInfo(WfProcess process,String timeFormat) throws RemoteException, InvocationTargetException, WTException {
		if(timeFormat == null || "".equals(timeFormat)){
			 timeFormat = "yyyy-MM-dd";
		}
		Map<String, String> signInfo = new HashMap<String,String>();
		// ????????????????????????????????????
		List<Map<String, Object>> result = getProcessRoutingHistory(process);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			Map<String, Object> hmInnerWorkItem = (Map<String, Object>) iterator.next();
			String vote = 	(String) hmInnerWorkItem.get(WORK_VOTE);
			if(vote.indexOf("??????")>=0) {
				continue;
			}
			// key:?????????  value: ?????????+"  "+????????????
			Timestamp date = (Timestamp) hmInnerWorkItem.get(WORK_COMPLETEDDATE);
			String key = (String) hmInnerWorkItem.get(WORK_NAME);
			String value =  hmInnerWorkItem.get(WORK_ASSIGNEE)+" "+getTimeStr(date,timeFormat);//??????????????????
			// List???????????????????????????????????????????????????????????????????????????????????????
			// ???????????????????????????????????????????????????????????? 
			// add by zjw 201806018
			if(signInfo.containsKey(key)) {
				continue;
			}
			if (key.contains("??????")) {
				QueryResult qr = getWorkItemsByWfProcess(process, true);
				while (qr.hasMoreElements()) {
					WorkItem workItem = (WorkItem) qr.nextElement();
					WfAssignedActivity wfAssignedActivity = (WfAssignedActivity) workItem.getSource().getObject(); // ?????????????????????????????????wfAssignedActivity,?????????????????????WfAssignment
					String activityName = wfAssignedActivity.getName();
					if (!signInfo.containsKey(activityName)) {
						if (activityName.contains("??????")) {
							int tripCount = wfAssignedActivity.getTripCount();
							QuerySpec qs = new QuerySpec(WfAssignment.class, ActivityAssignmentLink.class);
							SearchCondition sc = new SearchCondition(WfAssignment.class, WfAssignment.TRIP_COUNT,"=", tripCount);
							qs.appendWhere(sc, new int[0]);
							qs.appendOrderBy(WfAssignment.class, WfAssignment.CREATE_TIMESTAMP, true);
							QueryResult wfAssignmentQR = PersistenceHelper.manager.navigate(wfAssignedActivity,ActivityAssignmentLink.ASSIGNMENT_ROLE, qs, true);
							if (wfAssignmentQR.hasMoreElements()) {
								WfAssignment assignment = (WfAssignment) wfAssignmentQR.nextElement();
								QueryResult qr2 = PersistenceHelper.manager.navigate(assignment,WorkItemLink.WORK_ITEM_ROLE, WorkItemLink.class, true);
/*									if(qr2.size() == 1){
									WorkItem hqWorkItem = (WorkItem) qr2.nextElement();
									String hqOwnerName = hqWorkItem.getOwnership().getOwner().getFullName();
									String hqOcompleteTime = getTimeStr(hqWorkItem.getModifyTimestamp(),timeFormat);
									signInfoStr += hqOwnerName + " " + hqOcompleteTime;
									//signInfo.put(activityName, signInfoStr);
									signInfo.put(activityName+1, signInfoStr);
								}else */
								if(qr2.size() >= 1){
									Vector ov = qr2.getObjectVector().getVector();
									for (int i = 0 ; i < ov.size(); i ++ ) {
										WorkItem hqWorkItem = (WorkItem) ov.get(i);
										WTPrincipalReference owner = hqWorkItem.getOwnership().getOwner();
										String userName    		   = owner.getName();
										String userFullName        = owner.getFullName();
										String hqOcompleteTime = getTimeStr(hqWorkItem.getModifyTimestamp(),timeFormat);
										//?????????
										//String signInfoStr = userName + "$" + userFullName + "$" + hqOcompleteTime;
										//??????
										String signInfoStr = userFullName + " " + hqOcompleteTime;
										signInfo.put(activityName + (i+1), signInfoStr);
									}
								}
							}
						}
					}
				}
			}else {
				signInfo.put(key, value);
			}
		}
		return signInfo;
	}
	
	
	/**
	 * ??????????????????????????????????????? WORK_ASSIGNEE=name+$+fullName
	 * @param proc
	 * @return
	 * @throws WTException
	 */
	public static List<Map<String,Object>> getProcessRoutingHistory(WfProcess proc) throws WTException {
		WorkflowDataUtility wdu = new WorkflowDataUtility();

		List<Map<String,Object>> aProcess = new ArrayList<Map<String,Object>>();
		QueryResult qsVoteEvent = NmWorkflowHelper.service.getVotingEventsForProcess(proc); // Get all completed	
		// VotingEvent
		while (qsVoteEvent.hasMoreElements()) {
			WfVotingEventAudit voteEvent = (WfVotingEventAudit) qsVoteEvent.nextElement();
			// --- Process Information ---
			String strProcessName = ((TextDisplayComponent) wdu.getDataValue("procName", proc, null)).getValue();
			String strProcessCreator = proc.getCreator().getDisplayName();
			Timestamp time = proc.getCreateTimestamp();
			// --- WorkItem Information ---
			String strWorkName = (String) wdu.getDataValue("workName",voteEvent, null);
			if(voteEvent.getRole() == null){
				continue;
			}
			WTPrincipal workAssignee = voteEvent.getAssigneeRef().getPrincipal();			
			String strWorkRole = voteEvent.getRole().getDisplay(Locale.CHINA);// ??????
			String strWorkVote = voteEvent.getEventList().size() > 0 ? (String) (voteEvent.getEventList().get(0)) : "";// ??????
			if (strWorkVote.equals("&nbsp;")){
				strWorkVote = "";
			}
			DateDisplayComponent ddcDeadline = (DateDisplayComponent) wdu.getDataValue("workDeadline", voteEvent, null);
			String strDeadline = "";
			if (ddcDeadline != null) {
				strDeadline = ddcDeadline.getDisplayValue();
			}

			Timestamp ts = voteEvent.getTimestamp();
			//voteEvent.getModifyTimestamp();			
			//String strCompletedDate = SAPIntegrationHelper.formatTime(ts,IntegrationConstants.DATE_FORMAT_PDMTOSAP);
			String strWorkComments = voteEvent.getUserComment();// ????????????
			
			Map<String,Object> hmInnerWorkItem = new HashMap<String,Object>();

			hmInnerWorkItem.put(PROCESS_NAME, strProcessName);
			hmInnerWorkItem.put(PROCESS_CREATOR, strProcessCreator);
			hmInnerWorkItem.put(PROCESS_DATE, time);
			hmInnerWorkItem.put(WORK_NAME, strWorkName);
			//?????????
			//hmInnerWorkItem.put(WORK_ASSIGNEE, workAssignee instanceof WTUser?((WTUser)workAssignee).getName()+"$"+((WTUser)workAssignee).getFullName():workAssignee.getName()+"$"+workAssignee.getName());
			//??????
			hmInnerWorkItem.put(WORK_ASSIGNEE, workAssignee instanceof WTUser?((WTUser)workAssignee).getFullName():workAssignee.getName());
			hmInnerWorkItem.put(WORK_ROLE, strWorkRole);
			hmInnerWorkItem.put(WORK_VOTE, strWorkVote);
			hmInnerWorkItem.put(WORK_COMMENTS, strWorkComments);
			hmInnerWorkItem.put(WORK_DEADLINE, strDeadline);
			hmInnerWorkItem.put(WORK_COMPLETEDDATE, ts);

			aProcess.add(hmInnerWorkItem);
		}
		return aProcess;
	}
	
	
    public static boolean isWorkItemCompleteByCurrentUser(String processOid,String activityName) throws WTException, RemoteException, InvocationTargetException{
    	WfProcess process = (WfProcess) new ReferenceFactory().getReference(processOid).getObject();
    	return isWorkItemCompleteByCurrentUser(process,activityName);
    }
    
	
	  /**
	    * ?????????????????????????????????????????????????????????????????????????????????
	    */
	    public static boolean isWorkItemCompleteByCurrentUser(WfProcess wfp,String activityName) throws WTException, RemoteException, InvocationTargetException{
	    	WTUser currentuser = (WTUser) SessionHelper.manager.getPrincipal();
	    	List<WorkItem> itemList = getWorkItemByActivityName(wfp,activityName);
	    	if(itemList.size()>0){
	    		for(int i = 0 ; i < itemList.size(); i++){
	    			WorkItem workItem = itemList.get(i);
	    			String ownerName = workItem.getOwnership().getOwner().getFullName();
	    			//??????????????????????????????????????????????????????
	    			if(ownerName.equals(currentuser.getFullName())){
	    				return workItem.isComplete();
	    				//return workItem.getStatus().toString(); COMPLETED
	    			}
	    		}
	    	}
			return false;
	    }
	  
	    /**
	     * ??????wfprocess??????????????????????????????????????????(????????????????????????)
	     */
	  	public static List<WorkItem> getWorkItemByActivityName(WfProcess process,String name)
	  			throws RemoteException, InvocationTargetException, WTException {
	  		List<WorkItem> itemLists = new ArrayList<WorkItem>();
	  		//true???????????????????????????
			QueryResult qr = getWorkItemsByWfProcess(process, false);
			while (qr.hasMoreElements()) {
				WorkItem workItem = (WorkItem) qr.nextElement();
				WfAssignedActivity wfAssignedActivity = (WfAssignedActivity) workItem.getSource().getObject(); // ?????????????????????????????????wfAssignedActivity,?????????????????????WfAssignment
				String activityName = wfAssignedActivity.getName();
				String ownerName = workItem.getOwnership().getOwner().getFullName();
				if (name.equals(activityName)) {
					int tripCount = wfAssignedActivity.getTripCount();
					QuerySpec qs = new QuerySpec(WfAssignment.class, ActivityAssignmentLink.class);
					SearchCondition sc = new SearchCondition(WfAssignment.class, WfAssignment.TRIP_COUNT, "=", tripCount);
					qs.appendWhere(sc, new int[0]);
					qs.appendOrderBy(WfAssignment.class, WfAssignment.CREATE_TIMESTAMP, true);
					QueryResult wfAssignmentQR = PersistenceHelper.manager.navigate(wfAssignedActivity,ActivityAssignmentLink.ASSIGNMENT_ROLE, qs, true);
					if (wfAssignmentQR.hasMoreElements()) {
						WfAssignment assignment = (WfAssignment) wfAssignmentQR.nextElement();
						QueryResult qr2 = PersistenceHelper.manager.navigate(assignment, WorkItemLink.WORK_ITEM_ROLE,WorkItemLink.class, true);
						while (qr2.hasMoreElements()) {
							WorkItem workItem2 = (WorkItem) qr2.nextElement();
							WfAssignedActivity wfAssignedActivity2 = (WfAssignedActivity) workItem2.getSource().getObject(); // ?????????????????????????????????wfAssignedActivity,?????????????????????WfAssignment
							String activityName2 = wfAssignedActivity2.getName();
							//logger.debug("activity:"+activityName2+"owner:"+workItem2.getOwnership().getOwner().getFullName()+"   state:"+workItem2.getStatus().toString());
							itemLists.add(workItem2);
						}
					}
				}
			}
			return itemLists;
		}
	  	
	  	/**
	  	 * ???????????????????????????
	  	 */
		public static WfAssignedActivity getRunningWfActivity(WorkItem workItem, String activityName) throws WTException, WTRuntimeException, RemoteException, InvocationTargetException {
			WfProcess process = getWfProcess(workItem);
			return getRunningWfActivity(process,activityName);
		}
		
	  	/**
	  	 * ???????????????????????????
	  	 */
		public static WfAssignedActivity getRunningWfActivity(WfProcess parentProcess, String activityName) throws WTException, RemoteException, InvocationTargetException {
			if(!RemoteMethodServer.ServerFlag){
				Class cla[] = {WfProcess.class,String.class};
				Object obj[] = {parentProcess,activityName};
				return (WfAssignedActivity) (RemoteMethodServer.getDefault().invoke("getRunningWfActivity",CLASSNAME, null, cla, obj));
			}
			boolean AccessEnforced = false;
			
			try{
				AccessEnforced = SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
				WfAssignedActivity runningActivity = null;
				// ???????????????????????????OPEN_RUNNING???????????????
				Enumeration activityEnum = WfEngineHelper.service.getProcessSteps(parentProcess, WfState.OPEN_RUNNING);
				while (activityEnum.hasMoreElements()) {
					WfActivity wfactivity = (WfActivity) activityEnum.nextElement();
					if (wfactivity.getName().equals(activityName)) {
						runningActivity = (WfAssignedActivity) wfactivity;
						return runningActivity;
					}
				}
				return null;
			}finally{
				SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
			}
		}
			
	public static void main(String[] args){
		
		try {
			RemoteMethodServer rms = RemoteMethodServer.getDefault();
			rms.setUserName("wcadmin");
			rms.setPassword("SSSSssss@@22");
			Class[] acls = { };
			Object[] aobj = {  };
			rms.invoke("test",WfUtil.class.getName(), null, acls, aobj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	
    public static void removeTeamRoleUser(WTObject pbo,ObjectReference self) throws WTException{
   	 if(pbo instanceof PromotionNotice){
   		 PromotionNotice pn =(PromotionNotice)pbo;
   		 Team team=(Team) pn.getTeamId().getObject();
   		 removeTeamRoleUser(team);
   	 }else if(pbo instanceof ChangeOrder2){
   		 ChangeOrder2 ecn=(ChangeOrder2)pbo;
   		 Team team=(Team) ecn.getTeamId().getObject();
   		 removeTeamRoleUser(team);
   	 }else if(pbo instanceof ChangeActivity2){
   		 ChangeActivity2 eca=(ChangeActivity2)pbo;
   		 Team team=(Team)eca.getTeamId().getObject();
   		 removeTeamRoleUser(team);
   	 }
    }
    
    public static void removeTeamRoleUser(Team team) throws WTException{
   	 boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
   	 List<String> list = new ArrayList<String>();
   	 list.add("SUBMITTER");
   	 list.add("REVIEWER");
   	 list.add("ASSIGNEE");
   	 try {
   		 Vector v = team.getRoles();
			 for (int i = 0; i < v.size(); i++) {
				 Role role=(Role)v.get(i);
				 String roleStr = role.toString();
				 if(list.contains(roleStr)) {
					 continue;
				 }
				 Enumeration e = team.getPrincipalTarget(role);
				 while (e.hasMoreElements()) {
					 WTPrincipalReference wtprincipalReference = (WTPrincipalReference) e.nextElement();
					 WTPrincipal tempprincipal = wtprincipalReference.getPrincipal();
					 if (tempprincipal instanceof WTGroup) {
						 continue;
					 }
					 WTUser wtuser = (WTUser) tempprincipal;
					 TeamHelper.service.deleteRolePrincipalMap(role, wtuser, team);
				 }
			 }
   	 } finally {
            SessionServerHelper.manager.setAccessEnforced(enforce);
        }
    }

   //???????????????????????????????????????
    public static Map<String,List<WTUser>> getAllUserOfWfProcess(WfProcess process) throws TeamException, WTException{
        List<WTUser> users = null;
        Team team = TeamHelper.service.getTeam(process);
        Map principalMap = team.getRolePrincipalMap();
        Map<String,List<WTUser>> roleUserListMap = new HashMap<String,List<WTUser>>();
        for (Object obj : principalMap.keySet()) {
        	users = new ArrayList<WTUser>();
			if(obj instanceof Role){
				Role role = (Role)obj;
				String roleName = role.getDisplay(Locale.SIMPLIFIED_CHINESE);
				List list = (List) principalMap.get(obj);
				for (Object object : list) {
					if(object instanceof WTPrincipalReference){
						WTPrincipalReference reference = (WTPrincipalReference)object;
						WTPrincipal principal =  (WTPrincipal) reference.getObject();
						if(principal instanceof WTUser){
							users.add((WTUser)principal);
						}
					}else if(object instanceof WTUser){
						users.add((WTUser) object);
					}
				}
				roleUserListMap.put(roleName, users);
				
			}
        }
        return roleUserListMap;
    }
    //??????????????????????????????????????????
    public static Set<WTUser> getAllUserByProcessRole(WfProcess process,String roleName) throws TeamException, WTException{
		Role role = Role.toRole(roleName);
        Team team = TeamHelper.service.getTeam(process);
        Enumeration penum = team.getPrincipalTarget(role);
        Set<WTUser> users = new HashSet<WTUser>();
        while(penum!=null && penum.hasMoreElements()){       	
        	Object obj = penum.nextElement();
        	if(obj instanceof ObjectReference){
        		obj = ((ObjectReference)obj).getObject();
        	}
        	if(obj instanceof WTUser){
        		users.add((WTUser)obj);
        	}else if(obj instanceof WTGroup){
        		WTGroup group = (WTGroup)obj;
        		Vector<WTUser> v = PrincipalUtil.getUsersByGroup(group);
        		users.addAll(v);
        	}
        }
        return users;
    }
    
	public static List<WorkItem> getUncompletedWorkItems(String userName)throws Exception {
		boolean AccessEnforced = false;
		
		try{
			AccessEnforced = SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
			WTPrincipal principal = getUserByName(userName);
			if (principal == null) {
				throw new WTException("???????????????:" + userName);
			}
			QueryResult qs = WorkflowHelper.service.getUncompletedWorkItems(principal);
			return qs.getObjectVectorIfc().getVector();
		}finally{
			SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
		}
	}
	
	public static List<WorkItem> getUncompletedWorkItems()throws Exception {
		boolean AccessEnforced = false;
		
		try{
			AccessEnforced 		= SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
			WTPrincipal current = SessionHelper.manager.getPrincipal();
			if (current == null) {
				throw new WTException("???????????????????????????");
			}
			QueryResult qs = WorkflowHelper.service.getUncompletedWorkItems(current);
			return qs.getObjectVectorIfc().getVector();
		}finally{
			SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
		}
	}
	
	public static List<WorkItem> getCompletedWorkItems()throws Exception {
		boolean AccessEnforced = false;
		
		try{
			AccessEnforced 		= SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
			WTPrincipal current = SessionHelper.manager.getPrincipal();
			if (current == null) {
				throw new WTException("???????????????????????????");
			}
			QueryResult qs = WorkflowHelper.service.getCompletedWorkItems(current);
			return qs.getObjectVectorIfc().getVector();
		}finally{
			SessionServerHelper.manager.setAccessEnforced(AccessEnforced);
		}
	}
	/**
	 * ?????? Process, WorkItem ?????? Activity ??? oid ??????PBO??????
	 * @param oid
	 * @return
	 */
	public static Persistable getPBO(String oid){
		try {
			if (!RemoteMethodServer.ServerFlag) {
				return (Persistable) RemoteMethodServer.getDefault().invoke("getPBO",CLASSNAME, null,
						new Class[] {String.class},
						new Object[] {oid});
			} else {
				boolean enforce = wt.session.SessionServerHelper.manager.setAccessEnforced(false);
				WfProcess process = null;
				
				try {
					Object obj = (WTUtil.getObjectRefByOid(oid)).getObject();
					if(obj instanceof WfProcess){
						process = (WfProcess)obj;
					}else if(obj instanceof WfActivity){
						WfActivity activity = (WfActivity)obj;
						WfContainer wfcont = (WfContainer) activity.getParentProcessRef().getObject();
						if (wfcont instanceof WfProcess) {
							process = (WfProcess) wfcont;
						}
					}else if(obj instanceof WorkItem){
						WorkItem wi = (WorkItem)obj;
						WfActivity activity = (WfActivity) wi.getSource().getObject();
						WfContainer wfcont = (WfContainer) activity.getParentProcessRef().getObject();
						if (wfcont instanceof WfProcess) {
							process = (WfProcess) wfcont;
						}						
					} 

					return getPBO(process);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				} finally {
					SessionServerHelper.manager.setAccessEnforced(enforce);
				}

				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Persistable getPBO(WfProcess process) {
		try {
			if (!RemoteMethodServer.ServerFlag) {
				return (Persistable) RemoteMethodServer.getDefault().invoke("getPBO", CLASSNAME, null,
						new Class[] {WfProcess.class},
						new Object[] {process});
			} else {
				Persistable persist = null;
				
				boolean enforce = wt.session.SessionServerHelper.manager.setAccessEnforced(false);
				try {
					persist = (Persistable) process.getContext().getValue(WfDefinerHelper.PRIMARY_BUSINESS_OBJECT);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				} finally {
					SessionServerHelper.manager.setAccessEnforced(enforce);
				}

				return persist;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}	
	
	/**
	 * ??????????????????
	 * @param self
	 * @return
	 * @throws WfException
	 */
	public static String getHQResult(ObjectReference self) throws WfException{
		String result = "";
		wt.workflow.work.WfAssignedActivity activity = ((wt.workflow.work.WfAssignedActivity)self.getObject());
		Vector userEvents     = (java.util.Vector) activity.getUserEventList();
		Vector selectedEvents = WfTally.any(self,userEvents);
		if (selectedEvents.contains("??????")){       
			result="??????";
		}else{
		    result="??????";
		}
		return result;
	}

	/**
	 * ??????PBO??????????????????????????????????????????
	 * @param pbo PBO??????
	 * @return	?????????????????? true??? ?????? false
	 */
	public static boolean validateObjectRunningProcess(WTObject pbo) {
		try {
			if (!RemoteMethodServer.ServerFlag) {
				return (Boolean) RemoteMethodServer.getDefault().invoke("validateObjectRunningProcess", CLASSNAME, null,
						new Class[] {WTObject.class},
						new Object[] {pbo});
			} else {
				boolean result = false;
				
				boolean enforce = wt.session.SessionServerHelper.manager.setAccessEnforced(false);
				try {
					Enumeration actProcess = WfEngineHelper.service.getAssociatedProcesses(pbo, WfState.OPEN_RUNNING);
					if(actProcess == null) return true;		//?????????
					
					if(actProcess.hasMoreElements()){		//?????????
						return false;
					}else{
						return true;
					}
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				} finally {
					SessionServerHelper.manager.setAccessEnforced(enforce);
				}

				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * ??????????????????????????????
	 * @description
	 * @param workItem
	 * @return
	 * @throws WTRuntimeException
	 * @throws WTException
	 * @throws RemoteException
	 * @throws InvocationTargetException
	 */
	public static List<String> getUserEvent(WorkItem workItem) throws WTRuntimeException, WTException, RemoteException, InvocationTargetException{
		if (!RemoteMethodServer.ServerFlag) {
			Class cla[] = { WorkItem.class};
			Object obj[] = { workItem}; 
			return (UserEventVector) RemoteMethodServer.getDefault().invoke("getUserEvent",
					CLASSNAME, null, cla, obj);
		}
		boolean flag = false;
		List<String> userEvents = new ArrayList<String>();
		try{
			flag = SessionServerHelper.manager.setAccessEnforced(flag);
			WfActivity currentActivity = (WfActivity) workItem.getSource().getObject(); 
			UserEventVector uev = currentActivity.getUserEventList();
			if(!uev.isEmpty()){
				 Enumeration enu = uev.elements();
				 while(enu.hasMoreElements()){
					 Object obj = enu.nextElement();
					 String s = obj.toString();
					 userEvents.add(s);
				 }
			 }
			return userEvents;
		}finally{
			SessionServerHelper.manager.setAccessEnforced(flag);
		}
	}
}
