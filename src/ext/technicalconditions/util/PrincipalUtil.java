package ext.technicalconditions.util;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wt.fc.ObjectVectorEnumerator;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.federation.PrincipalManager.DirContext;
import wt.inf.container.ExchangeContainer;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.inf.team.ContainerTeam;
import wt.inf.team.ContainerTeamHelper;
import wt.inf.team.ContainerTeamManaged;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.org.DirectoryContextProvider;
import wt.org.OrganizationServicesHelper;
import wt.org.WTGroup;
import wt.org.WTOrganization;
import wt.org.WTPrincipal;
import wt.org.WTUser;
import wt.pom.Transaction;
import wt.project.Role;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

/**
 * 
 * 用户、组、角色、团队等对象的操作帮助类
 * 20200716,增加创建用户的代码
 * 
 */
@SuppressWarnings("unchecked")
public class PrincipalUtil implements RemoteAccess {

	private static final String CLASSNAME = PrincipalUtil.class.getName();
	private static Logger logger = LoggerFactory.getLogger(PrincipalUtil.class);

	/**
	 * 根据用户登录名得到用户
	 * @throws InvocationTargetException 
	 * @throws RemoteException 
	 */
	public static WTUser getUserByName(String name) throws WTException, RemoteException, InvocationTargetException {
		if(!RemoteMethodServer.ServerFlag){
			Class 	cla[] = {String.class};
			Object  obj[] = {name};
			return (WTUser) RemoteMethodServer.getDefault().invoke("getUserByName",CLASSNAME,null,cla,obj);
		}
		boolean accessEnforced = false;
		try{
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
			if(name == null || "".equals(name)){
				return null;
			}
			Enumeration enu = OrganizationServicesHelper.manager.findUser(WTUser.NAME, name);
			WTUser user = null;
			if (enu.hasMoreElements()) {
				user = (WTUser) enu.nextElement();
				return user;
			}
		}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
		return null;
	}

    public static WTUser getUserById(String s)throws WTException, RemoteException, InvocationTargetException{
		if(!RemoteMethodServer.ServerFlag){
			Class 	cla[] = {String.class};
			Object  obj[] = {s};
			return (WTUser) RemoteMethodServer.getDefault().invoke("getUserById",CLASSNAME,null,cla,obj);
		}
		 WTUser wtuser = null;
		 boolean accessEnforced = false;
		try{
			accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
	        QuerySpec queryspec = null;
	        QueryResult queryresult = null;
	        queryspec = new QuerySpec(wt.org.WTUser.class);
	        if(s.equalsIgnoreCase("wcadmin"))
	        {
	            s = "Administrator";
	        }
	        SearchCondition searchcondition = new SearchCondition(wt.org.WTUser.class, "name", "LIKE", s, false);
	        queryspec.appendSearchCondition(searchcondition);
	        queryresult = PersistenceHelper.manager.find(queryspec);
	        if(queryresult.hasMoreElements())
	        {
	            wtuser = (WTUser)queryresult.nextElement();
	        }
		}finally{
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
        return wtuser;
    }
    
	/**
	 * 根据用户全名得到用户
	 */
	public static List getUserByFullName(String fullName) throws WTException {
		List userList = new ArrayList();
		Enumeration enu = OrganizationServicesHelper.manager.findUser(WTUser.FULL_NAME, fullName);
		if (enu.hasMoreElements()) {
			WTUser user = (WTUser) enu.nextElement();
			userList.add(user);
		}
		logger.debug("全名为{}的用户数{}", fullName, userList.size() + "");
		return userList;
	}

	/**
	 * 判断用户是否为组织管理员
	 * 
	 * @return
	 */
	public static boolean isOrgAdministator(WTPrincipal principal, String orgName) {
		try {
			DirectoryContextProvider dcp = WTContainerHelper.service.getExchangeContainer().getContextProvider();
			WTOrganization org = OrganizationServicesHelper.manager.getOrganization(orgName, dcp);
			if (org == null) {
				logger.error("系统中不存在名为'" + orgName + "'的组织！");
				return false;
			}
			if (principal == null) {
				logger.error("WTPrincipal参数为空");
				return false;
			}
			WTContainerRef wtcontainerref = WTContainerHelper.service.getOrgContainerRef(org);
			if (wtcontainerref != null) {
				if (WTContainerHelper.service.isAdministrator(wtcontainerref, principal)) {
					return true;
				}
			} else {
				logger.error("WTContainerRef参数为空");
			}
		} catch (WTException e) {
			logger.error("判断用户是否为组织管理员出错,userName:" + principal.getName() + ",orgName:" + orgName + ".", e);
		}
		return false;
	}

	/**
	 * 判断用户是否为站点管理员
	 * 
	 * @return
	 */
	public static boolean isSiteAdministator(WTPrincipal user) {
		try {
			return WTContainerHelper.service.isAdministrator(WTContainerHelper.service.getExchangeRef(), user);
		} catch (Exception e) {
			logger.error("判断是否为站点管理员出错", e);
			return false;
		}
	}
	
	/**
	 * 判断用户是否为站点管理员
	 * 
	 * @return
	 */
	public static boolean isSiteAdministator() {
		try {
			WTPrincipal  principal = SessionHelper.manager.getPrincipal();
			return WTContainerHelper.service.isAdministrator(WTContainerHelper.service.getExchangeRef(), principal);
		} catch (Exception e) {
			logger.error("判断是否为站点管理员出错", e);
			return false;
		}
	}

	/**
	 * 判断指定用户是否在指定容器的团队中
	 * 
	 * @param user
	 *            指定用户
	 * @param container
	 *            指定容器
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static boolean isContainerMember(WTPrincipal user, WTContainer container) {
		try {
			ContainerTeam containerteam = ContainerTeamHelper.service.getContainerTeam((ContainerTeamManaged) container);
			Vector<?> vector = containerteam.getRoles();
			for (Iterator<?> iterator = vector.iterator(); iterator.hasNext();) {
				
				Role role = (Role) iterator.next();
				//
				List<WTPrincipal> rolePrincipalList = containerteam.getAllPrincipalsForTarget(role);
				//如果principal是组或第一级用户
				if(rolePrincipalList.contains(user)){
					return true;
				}
				if(user instanceof WTUser){
					List userlist = getContainerMemberByRole(container,role.toString());
					if(userlist.contains((WTUser)user)){
						return true;
					}
				}
			}
		} catch (WTException e) {
			logger.error("判断用户是否在指定团队中出错,user:"+user +",container:" + container, e);
		}
		return false;
	}
	
	public static boolean isContainerRoleMember(WTPrincipal user, WTContainer container,String roleName) {
		try {
			List list = getContainerMemberByRole(container,roleName);
			if(list != null && list.size() > 0 && list.contains(user)){
				return true;
			}
		} catch (Exception e) {
			logger.error("判断用户是否在指定团队角色中出错,user:"+user +",container:" + container, e);
		}
		return false;
	}
	
	/**
	 * 取用户交集
	 * @param users
	 * @param anNotherUsers
	 * @return 
	 * Mar 7, 2012 10:01:15 PM
	 */
	public static List getSameUsers(List users,List anNotherUsers){
		List newUsers = new ArrayList();
		for(Iterator it = users.iterator();it.hasNext();){
			WTPrincipal p = (WTPrincipal) it.next();
			if(anNotherUsers.contains(p)&& !newUsers.contains(p)){
				newUsers.add(p);
			}
		}
		return newUsers;
	}
	
	
	/**
	 * 依据容器和角色名称查找角色里所有的用户
	 */
	public static List getContainerMemberByRole(WTContainer container,String roleName){
		try{
			WTGroup group = ContainerTeamHelper.service.findContainerTeamGroup((ContainerTeamManaged) container, ContainerTeamHelper.ACCESS_GROUPS,roleName);
			Vector userVector = getUsersByGroup(group);	
			return userVector;
		}catch(Exception e){
			logger.error("依据容器查找角色里的用户出错,container:" + container.getName() +",roleName:" + roleName,e);
		}
		return new Vector();
	}
	
	public static WTGroup getGroupByName(String grpName) throws WTException{
		if(grpName == null){
			return null;
		}
		grpName = grpName.replace("*","%").replace('?', '_');
		QuerySpec grpSpec = new QuerySpec(WTGroup.class);
		SearchCondition condition = new SearchCondition(WTGroup.class,WTGroup.NAME,SearchCondition.LIKE, grpName);
		grpSpec.appendWhere(condition, new int[0]);
		QueryResult qr = PersistenceHelper.manager.find(grpSpec);
		if(qr != null && qr.size() > 0){
			return (WTGroup)qr.nextElement();
		}
		return null;
	}
	
	public static boolean isInGroup(String groupName,WTPrincipal principal) throws WTException{
		WTGroup group = getGroupByName(groupName);
		if(group != null && group.isMember(principal)){
			return true;
		}
		return false;
	}
	
	public static boolean isInGroup(String groupName) throws WTException{
		//获取当前用户
		WTPrincipal principal = SessionHelper.manager.getPrincipal();
		return isInGroup(groupName,principal);
	}

	/**
	 * 获取容器团队里所有的成员,不包含访客
	 */
	public static Set getContainerMembers(WTContainer container) throws WTException{
		return getContainerMembers(container,false);
	}
	
	
	/**
	 * 获取容器团队里所有的成员
	 * @param container
	 * @param containGuest 是否包含访客
	 */
	public static Set getContainerMembers(WTContainer container,boolean containGuest) throws WTException{
		Set members = new HashSet();
		List roleList = getAllRolesFromWTContainer(container);
		if(containGuest){
			roleList.add(Role.toRole("GUEST"));
		}
		for(int i = 0 ; i < roleList.size();i++){
			Role role = (Role) roleList.get(i);
			String roleName = role.toString();
			List list = getContainerMemberByRole(container,roleName);
			//log.debug("角色:" + role.getDisplay(Locale.SIMPLIFIED_CHINESE)+",用户数:" + list.size());
			members.addAll(list);
		} 
		return members;
	}
	
	/**
	 * 获取团队里所有的角色(不包含访客)
	 */
	public static List getAllRolesFromWTContainer(WTContainer container) throws WTException{
		ContainerTeam ct = ContainerTeamHelper.service.getContainerTeam((ContainerTeamManaged)container);
		Vector vector = ct.getRoles();
		return vector;
	}
	
	/**
	 * 判断组及其子组是否有任一用户
	 * @param group
	 * @return
	 * @throws WTException
	 */
	public static boolean hasUserInGroup(WTGroup group) throws WTException{
		WTUser user = getAnyUserByGroup(group,null);
		if(user != null){
			return true;
		}
		return false;
	}
	
	
	/**
	 * 获取组及其子组下所有的成员
	 * @throws WTException 
	 */
	public static Vector getUsersByGroup(WTGroup group) throws WTException{
		return getUsersByGroup(group,null);
	}
	
	public static Vector getUsersByGroup(String groupName) throws WTException{
		WTGroup group = getGroupByName(groupName);
		if(group == null){
			logger.warn("未找到组:" + groupName);
			return null;
		}
		return getUsersByGroup(group);
	}

	public static WTUser getAnyUserByGroup(WTGroup group,WTUser user) throws WTException{

		ObjectVectorEnumerator ov = (ObjectVectorEnumerator) group.members();
		while(ov.hasMoreElements()){
			Object obj = ov.nextElement();
			if(obj instanceof WTPrincipal){
				WTPrincipal pricipal =  (WTPrincipal) obj;
				if(pricipal instanceof WTGroup){
					WTGroup childGroup = (WTGroup) pricipal;
					getUsersByGroup(childGroup,null);
				}else if(pricipal instanceof WTUser){
					user = (WTUser) pricipal;
					return user;
				}
				
			}
		}
		return null;
	}
	
	private static Vector getUsersByGroup(WTGroup group,Vector userVector) throws WTException{
		if(userVector == null){
			userVector = new Vector();
		}
		if(group == null){
			return new Vector();
		}
		ObjectVectorEnumerator ov = (ObjectVectorEnumerator) group.members();
		while(ov.hasMoreElements()){
			Object obj = ov.nextElement();
			if(obj instanceof WTPrincipal){
				WTPrincipal pricipal =  (WTPrincipal) obj;
				if(pricipal instanceof WTGroup){
					WTGroup childGroup = (WTGroup) pricipal;
					getUsersByGroup(childGroup,userVector);
				}else if(pricipal instanceof WTUser){
					WTUser user = (WTUser) pricipal;
					if(!userVector.contains(user)){
						userVector.add(user);
					}	
				}
				
			}
		}
		return userVector;
	}
	
	/**
	 * TODO 20190906,获取用户所属部门,具体业务逻辑待定
	 * @param user
	 * @return
	 * @throws WTException
	 */
	public static String getGACEUserDept(WTUser user) throws WTException{
		if(user == null){
			user = (WTUser) SessionHelper.manager.getPrincipal();
		}
		return "";
	}
	
	
	//用OOTB API模糊查询用户 避免查询出已删除的用户
	public static Set<WTUser> getLikeUser(String name,String fullName) throws WTException{
		boolean flag = false;
		Set<WTUser> userSet1 = new HashSet<WTUser>();
		try{
			flag = SessionServerHelper.manager.setAccessEnforced(flag);
			name = StringUtils.trimToNull(name);
			fullName = StringUtils.trimToNull(fullName);
			if(name == null && fullName == null ) {
				name = "*";
			}
			DirectoryContextProvider provider = OrganizationServicesHelper.manager.newDirectoryContextProvider(OrganizationServicesHelper.manager.getDirectoryServiceNames(), null);
			if(name != null){
				Enumeration enu = OrganizationServicesHelper.manager.findLikeUsers(WTUser.NAME, name, provider);			
				while(enu.hasMoreElements()){
					WTUser user = (WTUser) enu.nextElement();
					userSet1.add(user);
				}
			}
			Set<WTUser> userSet2 = new HashSet<WTUser>();
			if(fullName != null){
				Enumeration enu2 = OrganizationServicesHelper.manager.findLikeUsers(WTUser.FULL_NAME, fullName, provider);			
				while(enu2.hasMoreElements()){
					WTUser user = (WTUser) enu2.nextElement();
					userSet2.add(user);
				}
			}
			//条件同时满足时取交集
			if(userSet1.size() > 0 && userSet2.size() > 0){
				userSet1.retainAll(userSet2);
			}else if(userSet1.size() == 0 && userSet2.size() > 0){
				userSet1.addAll(userSet2);
			}				
		}finally{
			SessionServerHelper.manager.setAccessEnforced(flag);
		}
		return userSet1;
	}

/*	private static final String DN_BEFORE      = "uid=";

	private static final String DN_AFTER       = ",ou=people,cn=administrativeldap,cn=windchill_9.1,o=ptc";

	private static final String LDAPSERVICE    = "cn.sac.Ldap";
	private static String defaultAdapter = null;
	private static String userSearchBase = null;
	static{
		try{
			defaultAdapter = DirContext.getDefaultJNDIAdapter ();
            userSearchBase = DirContext.getJNDIAdapterSearchBase (defaultAdapter);
		}catch(Exception e){
			e.printStackTrace();
		}
	}*/

	/**
	 * 创建一个新用户
	 * 
	 * @param userid
	 * @param username
	 * @return
	 * @throws WTException 
	 * @throws WTPropertyVetoException 
	 * @throws InvocationTargetException 
	 * @throws RemoteException 
	 */
	public static WTUser createNewUser(String name,String fullName,String password) throws WTException, WTPropertyVetoException, RemoteException, InvocationTargetException {
		if (!RemoteMethodServer.ServerFlag) {
			Class<?> aclass[] = { String.class, String.class ,String.class};
			Object aobj[] = {name,fullName,password};
			return (WTUser) RemoteMethodServer.getDefault().invoke("createNewUser", CLASSNAME, null, aclass, aobj);
		}
		boolean accessEnforced = SessionServerHelper.manager.setAccessEnforced(false);
		WTUser user = null;
		try {
			user = OrganizationServicesHelper.manager.getAuthenticatedUser(name);
			if (user == null) {
				ExchangeContainer exChangeContainer = (ExchangeContainer) WTContainerHelper.getExchangeRef().getObject();
				user = WTUser.newWTUser(name, exChangeContainer.getContextProvider());
				user.setAttributes(OrganizationServicesHelper.manager.createAttributeHolder());
				user.getAttributes().addValue("userPassword", password);
				user.setFullName(fullName);
				user.setEMail("");
				user.setOrganizationName("");
				user.setAllowLDAPSynchronization(true);
				user.setLocale(Locale.CHINA);
				user = (WTUser) OrganizationServicesHelper.manager.createPrincipal(user);
			}
		}finally {
			// reset access enforce
			SessionServerHelper.manager.setAccessEnforced(accessEnforced);
		}
		return user;
	}
	   
	/**
	 * 更新用户信息
	 * 
	 * @param userid
	 * @param username
	 * @return
	 * @throws WTException 
	 * @throws InvocationTargetException 
	 * @throws RemoteException 
	 */
	public static void updateUser(String userid, String username) throws WTException, RemoteException, InvocationTargetException {
		logger.debug(">>>updateUserInfo start..." + userid + "=" + username + "=");
		// check server flag
		if (!RemoteMethodServer.ServerFlag) {
			Class<?> aclass[] = { String.class, String.class };
			Object aobj[] = { userid, username };
			RemoteMethodServer.getDefault().invoke("updateUser", CLASSNAME, null, aclass, aobj);
			return;
		}
		// check parameters
		StringBuffer sb = new StringBuffer();
		if (userid == null || username == null) {
			sb.append("Parameters userid and username could not be null.");
			return;
		}

		// set access enforced false
		boolean accessFlag = SessionServerHelper.manager.setAccessEnforced(false);
		Transaction transaction = null;
		try {
			// get Authenticated User
			WTUser authUser = OrganizationServicesHelper.manager.getAuthenticatedUser(userid);
			if (authUser == null) {
				// if no user, return fail
				sb.append("User ").append(userid).append(" not exist in PDM, could not update its full name.");
			} else {
				// initial transaction
				transaction = new Transaction();
				transaction.start();
				authUser.setFullName(username);
				//修改用户密码user.getAdditionalAttributes().addValue("userPassword", "");
				// update principal
				OrganizationServicesHelper.manager.updatePrincipal(authUser);
				// commit transaction
				transaction.commit();
				transaction = null;
			}
		} finally {
			// reset access enforce
			SessionServerHelper.manager.setAccessEnforced(accessFlag);
			// roll back transaction
			if (transaction != null) {
				transaction.rollback();
			}
		}
		logger.debug(">>>updateUserInfo end." + sb);
	}
	
	public static void main(String s[]){
		
	}
}
