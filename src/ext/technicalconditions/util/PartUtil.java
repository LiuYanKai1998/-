package ext.technicalconditions.util;

import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.ptc.core.foundation.container.common.FdnWTContainerHelper;
import com.ptc.core.foundation.type.server.impl.TypeHelper;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.impl.WCTypeIdentifier;
import com.ptc.windchill.enterprise.copy.server.CoreMetaUtility;
import com.ptc.windchill.enterprise.part.commands.AssociationLinkObject;
import com.ptc.windchill.enterprise.part.commands.PartDocServiceCommand;

import wt.access.AccessControlHelper;
import wt.access.AccessPermission;
import wt.access.AdHocAccessKey;
import wt.clients.prodmgmt.PartHelper;
import wt.content.ApplicationData;
import wt.content.ContentItem;
import wt.content.FormatContentHolder;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentHelper;
import wt.epm.build.EPMBuildHistory;
import wt.epm.build.EPMBuildRule;
import wt.epm.structure.EPMDescribeLink;
import wt.fc.ObjectIdentifier;
import wt.fc.ObjectReference;
import wt.fc.ObjectSetVector;
import wt.fc.ObjectToObjectLink;
import wt.fc.ObjectVector;
import wt.fc.ObjectVectorIfc;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTKeyedHashMap;
import wt.fc.collections.WTKeyedMap;
import wt.fc.collections.WTSet;
import wt.fc.collections.WTValuedHashMap;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerRef;
import wt.introspection.WTIntrospector;
import wt.lifecycle.LifeCycleException;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.LifeCycleServerHelper;
import wt.lifecycle.State;
import wt.locks.LockException;
import wt.locks.LockHelper;
import wt.maturity.PromotionNotice;
import wt.maturity.PromotionNoticeConfigSpec;
import wt.method.RemoteMethodServer;
import wt.org.WTPrincipal;
import wt.org.WTPrincipalReference;
import wt.part.PartDocHelper;
import wt.part.PartType;
import wt.part.Quantity;
import wt.part.QuantityUnit;
import wt.part.Source;
import wt.part.WTPart;
import wt.part.WTPartConfigSpec;
import wt.part.WTPartDescribeLink;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartReferenceLink;
import wt.part.WTPartStandardConfigSpec;
import wt.part.WTPartSubstituteLink;
import wt.part.WTPartUsageLink;
import wt.pds.StatementSpec;
import wt.pom.PersistenceException;
import wt.query.ArrayExpression;
import wt.query.ClassAttribute;
import wt.query.OrderBy;
import wt.query.QueryException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.series.HarvardSeries;
import wt.series.IntegerSeries;
import wt.series.MultilevelSeries;
import wt.series.Series;
import wt.series.SeriesException;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.team.TeamHelper;
import wt.type.ClientTypedUtility;
import wt.type.TypeDefinitionReference;
import wt.type.TypedUtility;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.util.WTRuntimeException;
import wt.vc.Iterated;
import wt.vc.IterationIdentifier;
import wt.vc.VersionControlException;
import wt.vc.VersionControlHelper;
import wt.vc.VersionIdentifier;
import wt.vc.baseline.BaselineHelper;
import wt.vc.baseline.ManagedBaseline;
import wt.vc.config.BaselineConfigSpec;
import wt.vc.config.ConfigHelper;
import wt.vc.config.ConfigSpec;
import wt.vc.config.IteratedOrderByPrimitive;
import wt.vc.config.MultipleLatestConfigSpec;
import wt.vc.config.LatestConfigSpec;
import wt.vc.config.VersionedOrderByPrimitive;
import wt.vc.config.ViewConfigSpec;
import wt.vc.struct.StructHelper;
import wt.vc.views.Variation1;
import wt.vc.views.Variation2;
import wt.vc.views.View;
import wt.vc.views.ViewException;
import wt.vc.views.ViewHelper;
import wt.vc.views.ViewReference;
import wt.vc.wip.CheckoutLink;
import wt.vc.wip.WorkInProgressException;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;

/**
 * V20200418
 * V20200715,增加跳空升小版的方法
 */
public class PartUtil implements wt.method.RemoteAccess, java.io.Serializable {

    private static final Logger logger = Logger.getLogger(PartUtil.class);

    private static final String CLASSNAME = PartUtil.class.getName();

/*  public static final String SOURCE = "SOURCE";
    public static final String ASSEMBLY = "ASSEMBLY";
    public static final String VIEW = "VIEW";
    public static final String DEFAULT_UNIT = "DEFAULT_UNIT";
    public static final String IS_ENDITEM = "IS_ENDITEM";*/

    /**
     * 设计视图部件
     */
    public static ConfigSpec DesignConfigSpec;
    /**
     * 最新版配置规范
     */
    public static ConfigSpec latestConfigSpec;

    static {
        try {
            /**初始化设计视图部件**/
            View DesignView = ViewHelper.service.getView("Design");
            DesignConfigSpec = WTPartStandardConfigSpec.newWTPartStandardConfigSpec(DesignView,null);
            latestConfigSpec = new LatestConfigSpec();
        } catch (WTException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * 为部件添加说明文档
     * @param part
     * @param doc
     * @throws RemoteException
     * @throws InvocationTargetException
     * @throws WTException
     */
    public static void createDescribeLink(WTPart part, WTDocument doc)
            throws RemoteException, InvocationTargetException, WTException {
        if (!RemoteMethodServer.ServerFlag) {
            RemoteMethodServer.getDefault().invoke("createDescribeLink", CLASSNAME, null,
                    new Class[] { WTPart.class, WTDocument.class }, new Object[] { part, doc });
            return;
        }
        Vector<WTDocument> describedDocs = PartUtil.getDescribedDoc(part, null);
        for(WTDocument describedDoc : describedDocs){
        	if(describedDoc.equals(doc)){
        		return;
        	}
        }
        WTPartDescribeLink link = WTPartDescribeLink.newWTPartDescribeLink(part, doc);
        PersistenceServerHelper.manager.insert(link);
    }

    /**
     * 查找所有视图部件的最新小版,存在副本的同时返回副本和原始版本
     * 支持备用BOM 
     */
    public static List<WTPart> getAllViewLatestPart(String partNumber) throws WTException {
        if (!RemoteMethodServer.ServerFlag) {
            try {
                return (List<WTPart>) RemoteMethodServer.getDefault().invoke("getAllViewLatestPart",
                        CLASSNAME, null, new Class[] { String.class }, new Object[] { partNumber });
            } catch (Exception e) {
                throw new WTException(e);
            }
        }
        boolean accessControlled = false;
        try {
            accessControlled = SessionServerHelper.manager.setAccessEnforced(accessControlled);
            List<WTPart> result = new ArrayList<WTPart>();

            View[] views = ViewHelper.service.getAllViews();
            logger.debug("views >>>>>>>>>>> " + views.length);
            QueryResult qr = getParts(partNumber, null, null, null);
            /*	        ObjectVector vector  = new ObjectVector();
            for(WTPart part : partList){
            	vector.addElement(part);
            	logger.debug("part1 >>>>>>>>>> " + part.getDisplayIdentifier());
            }
            QueryResult qr 		   = new QueryResult(vector);*/
            qr = new MultipleLatestConfigSpec().process(qr);
            logger.debug("qr1 >>>>>>>> " + qr.size());

            Map<String, ObjectVector> hashMap = new HashMap<String, ObjectVector>();
            while (qr.hasMoreElements()) {
                WTPart part = (WTPart) qr.nextElement();
                //备用BOM支持
                String v1String = "";
                String v2String = "";
                Variation1 v1 = part.getVariation1();
                Variation2 v2 = part.getVariation2();
                if (v2 != null) {
                    v2String = v2.toString();
                }
                String key = part.getViewName() + v1String + v2String;
                ObjectVector viewParts = null;
                if (hashMap.get(key) != null) {
                    viewParts = hashMap.get(key);
                } else {
                    viewParts = new ObjectVector();
                }
                if (!viewParts.contains(part)) {
                    viewParts.addElement(part);
                    hashMap.put(key, viewParts);
                }
            }
            logger.debug("hashMap >>>>> " + hashMap);
            for (Iterator it = hashMap.keySet().iterator(); it.hasNext();) {
                ObjectVector temp = hashMap.get(it.next());
                QueryResult tempQR = new QueryResult(temp);
                tempQR = latestConfigSpec.process(tempQR);
                logger.debug("tempQR >>>>>>>>>>> " + tempQR.size());
                while (tempQR.hasMoreElements()) {
                    WTPart part = (WTPart) tempQR.nextElement();
                    boolean isCheckedOut = WorkInProgressHelper.isCheckedOut(part);
                    WTPart part2 = null;
                    if (isCheckedOut) {
                        boolean isWorkingCopy = WorkInProgressHelper.isWorkingCopy(part);
                        if (!isWorkingCopy) {
                            part2 = (WTPart) WorkInProgressHelper.service.workingCopyOf(part);
                        } else {
                            part2 = (WTPart) WorkInProgressHelper.service.derivedFrom(part);
                        }
                    }
                    if (part2 != null) {
                        logger.debug("查找各视图最新版,找到部件副本,一起返回:" + part2.getDisplayIdentity());
                        result.add(part2);
                    }
                    logger.debug("查找各视图最新版,返回部件:" + part.getDisplayIdentity());
                    result.add(part);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new WTException(e);
        } finally {
            accessControlled = SessionServerHelper.manager.setAccessEnforced(accessControlled);
        }
    }

    /**
     * 查找部件在所有link下的替换部件
     * @param childPart
     * @return
     * @throws WTException
     */
    public static List<WTPartMaster> getSubstitutePart(WTPart childPart) throws WTException {
        List<WTPartMaster> list = new ArrayList<WTPartMaster>();
        WTPartUsageLink[] links = PartHelper.getUsedBy(childPart);
        if (links == null) {
            return list;
        }
        for (WTPartUsageLink link : links) {
            logger.debug("父:" + ((WTPart) link.getRoleAObject()).getDisplayIdentity());
            logger.debug("子:" + ((WTPartMaster) link.getRoleBObject()).getNumber());
            WTCollection collection = WTPartHelper.service.getSubstituteLinks(link);
            for (Object object : collection) {
                if (object instanceof ObjectReference) {
                    ObjectReference objref = (ObjectReference) object;
                    Object obj = objref.getObject();
                    if (obj instanceof WTPartSubstituteLink) {
                        WTPartSubstituteLink substituteLink = (WTPartSubstituteLink) obj;
                        //当前替换件所对应部件和其父件的link
                        //WTPartUsageLink      currentLink    = (WTPartUsageLink) substituteLink.getRoleAObject();
                        WTPartMaster partMaster = (WTPartMaster) substituteLink.getRoleBObject();
                        logger.debug("替换部件:" + partMaster.getNumber());
                        list.add(partMaster);
                    }
                }
            }
        }
        return list;
    }

    /**
     * 根据编号，名称，视图，部件类型，容器来创建部件，同时设置状态为已审阅，如果传入的视图不存在，则创建Design视图部件
     * 11:06:22 AM
     * @param partNumber 部件编号,必须
     * @param partName   部件名称,必须
     * @param view	     视图名称，如果为null则创建Design视图部件
     * @param fullType   传入部件软类型的全路径,如果为null,则创建wt.part.WTPart类型
     * @param containerName       存储容器名,必须
     * @param state       初始生命周期状态
     */
    public static WTPart createNewPart(String partNumber, String partName, String view,
            String fullType, String containerName, String state, String folderPath)
            throws WTException, WTPropertyVetoException, LifeCycleException, RemoteException,
            InvocationTargetException {
        if (!RemoteMethodServer.ServerFlag) {
            return (WTPart) RemoteMethodServer.getDefault().invoke("createNewPart", CLASSNAME, null,
                    new Class[] { String.class, String.class, String.class, String.class,
                            String.class, String.class },
                    new Object[] { partNumber, partName, view, fullType, containerName, state });
        }
        boolean accessEnforced = false;
        try {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            ViewReference vm = getPartViewReference(view);
            if (vm == null) {
                vm = getPartViewReference("Design");
            }
            WTPart part = WTPart.newWTPart();
            if (fullType != null) {
                TypeDefinitionReference tdr = getTypeDefinitionReference(fullType,"wt.part.WTPart");
                if (tdr != null) {
                    part.setTypeDefinitionReference(tdr);
                }
            }
            part.setNumber(partNumber);
            part.setName(partName);
            part.setView(vm);
            part.setSource(Source.getSourceDefault());
            part.setEndItem(false);
            part.setDefaultUnit(QuantityUnit.EA);
            part.setPartType(PartType.SEPARABLE);

            WTContainer container = ContainerUtil.getContainerByName(containerName);
            WTContainerRef cref = WTContainerRef.newWTContainerRef(container);

            part.setContainerReference(cref);
            if (StringUtils.isNotBlank(folderPath)) {
                Folder location = null;
                try {
                    location = FolderHelper.service.getFolder(folderPath, cref);
                } catch (Exception e) {
                    logger.error("Get partFolder[" + folderPath + "] error...");
                    location = null;
                }
                // If folder is not exist, create folder
                if (location == null) {
                    location = FolderHelper.service.saveFolderPath(folderPath, cref);
                }
                // set object to folder
                if (location != null) {
                    WTValuedHashMap map = new WTValuedHashMap();
                    map.put(part, location);
                    FolderHelper.assignLocations(map);
                }
            }

            part.setTeamId(TeamHelper.service.createTeam(null, null, null, part));
            part = (WTPart) PersistenceHelper.manager.save(part);
            if (StringUtils.isNotBlank(state)) {
                part = (WTPart) LifeCycleHelper.service.setLifeCycleState(part,
                        State.toState(state));
            }
            return (WTPart) PersistenceHelper.manager.refresh(part);
        } finally {
            SessionServerHelper.manager.setAccessEnforced(accessEnforced);
        }
    }
    
    /**
     * 获取类型对象
     * @description
     * @param fullType    类型全路径如:wt.part.WTPart|com.wisplm.StandardPart
     * @param defaultType 如果fullType类型不存在,则返回默认类型
     * @return
     */
    public static TypeDefinitionReference getTypeDefinitionReference(String fullType,String defaultType) {
        TypeDefinitionReference tdr = null;
        try {
            TypeIdentifier typeidentifier = CoreMetaUtility.getTypeIdentifier(fullType);
            WCTypeIdentifier wctypeidentifier = (WCTypeIdentifier) typeidentifier;
            tdr = TypedUtility.getTypeDefinitionReference(wctypeidentifier.getTypename());
        } catch (Exception e) {
            logger.error("Get TypeIdentifier error by [" + fullType + "]...", e);
            TypeIdentifier typeidentifier = CoreMetaUtility.getTypeIdentifier(defaultType);
            WCTypeIdentifier wctypeidentifier = (WCTypeIdentifier) typeidentifier;
            tdr = TypedUtility.getTypeDefinitionReference(wctypeidentifier.getTypename());
        }
        return tdr;
    }

    /**
     * 通过部件编号、视图名称、生命周期状态查询最新版本部件
     * 
     * @param partNumber 部件编号
     * @param viewName   视图名称
     * @param states     生命周期状态集合
     * @param accessControlled 权限开关
     * @return
     * @throws WTException
     */
    public static WTPart getLatestPartByNoViewWithState(String partNumber, String viewName,
            List<String> states, boolean accessControlled) throws WTException {
        WTPart latestPart = null;
        if (StringUtils.isNotBlank(partNumber)) {
            boolean enforce = SessionServerHelper.manager.setAccessEnforced(accessControlled);
            try {
                partNumber = partNumber.toUpperCase();
                QuerySpec qs = new QuerySpec(WTPart.class);

                qs.appendWhere(new SearchCondition(WTPart.class, WTPart.NUMBER,
                        SearchCondition.EQUAL, partNumber), new int[] { 0 });

                qs.appendAnd();

                ClassAttribute ca = new ClassAttribute(WTPart.class, "checkoutInfo.state");
                String[] checkOutStates = new String[] { "c/i", "c/o" };
                qs.appendWhere(new SearchCondition(ca, SearchCondition.IN,
                        new ArrayExpression(checkOutStates)), new int[] { 0 });
                // new SearchCondition(WTPart.class, "checkoutInfo.state",
                // SearchCondition.NOT_EQUAL, "wrk");

                qs.appendAnd();
                qs.appendWhere(new SearchCondition(WTPart.class, WTPart.LATEST_ITERATION,
                        SearchCondition.IS_TRUE), new int[] { 0 });

                if (StringUtils.isNotBlank(viewName)) {
                    View view = ViewHelper.service.getView(viewName);
                    qs.appendAnd();
                    qs.appendWhere(
                            new SearchCondition(WTPart.class, "view.key.id", SearchCondition.EQUAL,
                                    view.getPersistInfo().getObjectIdentifier().getId()),
                            new int[] { 0 });
                }

                if (states != null && states.size() > 0) {
                    qs.appendAnd();
                    qs.appendOpenParen();
                    for (int i = 0; i < states.size(); i++) {
                        String state = states.get(i);
                        if (i > 0) {
                            qs.appendOr();
                        }
                        SearchCondition sc = new SearchCondition(WTPart.class,
                                WTPart.LIFE_CYCLE_STATE, SearchCondition.EQUAL, state);
                        qs.appendWhere(sc, new int[] { 0 });
                    }
                    qs.appendCloseParen();
                }

                // Order by version iteration
                new VersionedOrderByPrimitive().appendOrderBy(qs, 0, true);
                new IteratedOrderByPrimitive().appendOrderBy(qs, 0, true);

                logger.debug("getLatestPartByNoViewWithState qs : " + qs);
                QueryResult result = PersistenceHelper.manager.find((StatementSpec) qs);
                if (result != null && result.size() > 0) {
                    latestPart = (WTPart) result.nextElement();
                }
            } finally {
                SessionServerHelper.manager.setAccessEnforced(enforce);
            }
        }
        //log.debug("latestPart : " + latestPart);
        return latestPart;
    }

    /**
     * 通过部件Master,视图名称获取该视图最新版本部件
     *
     * @param master   部件master
     * @param viewName 视图名称： Design,Planning,Manufacturing
     * @return 指定视图里的最新版本的Part
     * @throws WTException
     */
    public static WTPart getLatestPart(WTPartMaster master, String viewName) throws WTException {
        try {
            View view = ViewHelper.service.getView(viewName);
            ConfigSpec configSpec = WTPartConfigSpec.newWTPartConfigSpec(
                    WTPartStandardConfigSpec.newWTPartStandardConfigSpec(view, null));
            WTPart part = getPartByConfig(master, configSpec);
            if (part != null) {
                return part;
            }
        } catch (WTException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    /**
     * 通过部件master,配置规范获取部件
     *
     * @param master
     * @param partConfigSpec
     * @return
     * @throws WTException
     */
    public static WTPart getPartByConfig(WTPartMaster master, ConfigSpec partConfigSpec)
            throws WTException {
        WTPart part = null;
        if (master != null && partConfigSpec != null) {
            QueryResult qr = ConfigHelper.service.filteredIterationsOf(master, partConfigSpec);
            if (qr.hasMoreElements()) {
                Object obj = qr.nextElement();
                if (obj instanceof WTPart) {
                    part = (WTPart) obj;
                    return part;
                }
            }
        }
        return part;
    }

    /**
     * 获取部件的最新版对象
     * @param number 支持模糊查询,多个部件编号用;隔开,可以使用*作为通配符
     * @return 
     * @throws WTException 
     * 2011-11-24 下午07:54:20
     */
    public static QueryResult getLatestWTParts(String number) throws WTException {
        if (number != null) {
            QuerySpec qs = new QuerySpec(WTPart.class);
            int iIndex = qs.getFromClause().getPosition(WTPart.class);

            String COMMA_REPLACEMENT_STRING = ";";
            if (number.indexOf(COMMA_REPLACEMENT_STRING) != -1) {
                number = number.replaceAll(COMMA_REPLACEMENT_STRING, ",");
            }
            if (number.indexOf(",") != -1) {
                String[] sArray = wt.util.WTStringUtilities.toArray(number, ",");
                int iSize = sArray.length;
                for (int i = 0; i < iSize; i++) {
                    String strKey = sArray[i];
                    strKey = strKey.toUpperCase();
                    if (strKey.indexOf('*') == -1) {
                        strKey = strKey + "%";
                    } else {
                        strKey = strKey.replace('*', '%');
                    }
                    SearchCondition sc = new SearchCondition(WTPart.class, WTPart.NUMBER,
                            SearchCondition.LIKE, strKey, false);
                    if (i > 0) {
                        qs.appendOr();
                    }
                    qs.appendWhere(sc, new int[iIndex]);
                }
            } else {
                number = number.toUpperCase();
                if (number.indexOf('*') == -1) {
                    number = number + "%";
                } else {
                    number = number.replace('*', '%');
                }
                SearchCondition sc = new SearchCondition(WTPart.class, WTPart.NUMBER,
                        SearchCondition.LIKE, number, false);
                qs.appendWhere(sc, new int[iIndex]);
            }
            QueryResult qr = PersistenceHelper.manager.find(qs);
            qr = latestConfigSpec.process(qr);
            return qr;
        }
        return null;
    }

    public static void main(String args[]) throws WTPropertyVetoException, LifeCycleException,
            RemoteException, WTException, InvocationTargetException {
        //wt.epm.EPMDocumentHelper.service.

        //String partOid = args[0];
        //String docOid = args[1];
        //Class cla[] = {String.class,String.class};
        //Object obj[] = {partOid,docOid};
        //invoke("getDescribedDoc",cla,obj); 

        //WTContainer container = (WTContainer) new ReferenceFactory().getReference("OR:wt.pdmlink.PDMLinkProduct:20431").getObject();
        //WTContainerRef ref = container.getContainerReference();
        WTPart part = (WTPart) new ReferenceFactory().getReference("OR:wt.part.WTPart:8416220")
                .getObject();
        Workable able = CheckOutObject(part);
        part = (WTPart) checkinObject(able);
        System.out.println("1---->" + part.getDisplayIdentifier().toString());
        System.out.println("1---->" + part.getDisplayIdentity().toString());
        part = checkoutPart(part, "");
        part = checkinPart(part, "");
        System.out.println("2---->" + part.getDisplayIdentifier().toString());
        System.out.println("2---->" + part.getDisplayIdentity().toString());
    }

    /**
     * 为部件添加子件,如果存在则不添加
     * @param parent 父件
     * @param child  子件
     * @throws WTException 
     */
    public static void createUsageLink(WTPart parent, WTPart child) throws WTException {
        if(getPartUsageLink(parent,(WTPartMaster)child.getMaster()) == null){
        	WTPartUsageLink partLink = WTPartUsageLink.newWTPartUsageLink(parent,(WTPartMaster) child.getMaster());
            PersistenceServerHelper.manager.insert(partLink);
        }
    }

    /**
     * 获取两个部件之间的usagelink关系
     */
    public static WTPartUsageLink getPartUsageLink(WTPart parentPart,
            WTPartMaster childPartMaster) {
        WTPartUsageLink partusagelink = null;
        if (childPartMaster != null) {
            try {
                QueryResult queryresult = PersistenceHelper.manager.find(WTPartUsageLink.class,
                        parentPart, WTPartUsageLink.USED_BY_ROLE, childPartMaster);
                if (queryresult != null && queryresult.size() > 0) {
                    partusagelink = (WTPartUsageLink) queryresult.nextElement();
                }
            } catch (WTException e) {
                e.printStackTrace();
            }
        }
        return partusagelink;
    }

    /**
     * 获取以当前文档为说明文档的部件-说明方部件
     * 上午11:50:06
     * @param doc
     * @return
     * @throws WTException
     */
    public static List getDesParts(WTDocument doc) throws WTException {
        return PartDocServiceCommand.getAssociatedDescParts(doc).getObjectVectorIfc().getVector();
    }

    /**
     * 获取以当前文档为参考文档的部件
     * @param doc
     * @return
     * @throws WTException
     */
    public static List getRefParts(WTDocument doc) throws WTException {
        return PartDocServiceCommand.getAssociatedRefParts(doc);
    }
    
    public static WTPart getPartByOid(String oid) throws WTRuntimeException, WTException {
        return (WTPart) new ReferenceFactory().getReference(oid).getObject();
    }

    /**
     * 通过部件的oid获取部件对象，然后再获取其状态
     * 
     * @param oid
     *            如:OR:wt.part.WTPart:11111
     * @return
     */
    public static String getPartStateByOid(String oid) {
        String state = "";
        WTPart part = null;
        try {
            WTReference wf = new ReferenceFactory().getReference(oid);
            if (wf != null) {
                part = (WTPart) wf.getObject();
                state = part.getState().toString();
            }
        } catch (WTRuntimeException e) {
            e.printStackTrace();
        } catch (WTException e) {
            e.printStackTrace();
        }
        return state;
    }

    public static WTPartMaster getWTPartMasterByNumber(String number) throws WTException {
        WTPartMaster partmaster = null;
        QuerySpec qs = null;
        QueryResult qr = null;
        qs = new QuerySpec(WTPartMaster.class);
        SearchCondition sc = new SearchCondition(WTPartMaster.class, wt.part.WTPartMaster.NUMBER,
                SearchCondition.EQUAL, number.toUpperCase());
        qs.appendSearchCondition(sc);
        qr = PersistenceHelper.manager.find(qs);
        if (qr.hasMoreElements()) {
            partmaster = (WTPartMaster) qr.nextElement();
        }
        return partmaster;
    }

    /**
     * 检出一个部件checkoutPart -- checkinPart为一组，好用
     * 检出的时候应该增加判断是否为该大版本里最新的小版本
     */
    public static WTPart checkoutPart(WTPart part, String comment) throws WTException {
        Folder folder = WorkInProgressHelper.service.getCheckoutFolder();
        System.out.println("folder = " + folder);
        try {
            CheckoutLink checkoutLink = WorkInProgressHelper.service.checkout(part, folder,
                    comment);
            checkoutLink.getOriginalCopy();
            part = (WTPart) checkoutLink.getWorkingCopy();
        } catch (WTPropertyVetoException ex) {
            ex.printStackTrace();
        }
        if (!WorkInProgressHelper.isWorkingCopy(part)) {
            part = (WTPart) WorkInProgressHelper.service.workingCopyOf(part);
        }
        return part;
    }

    /**
     * 检入一个部件checkoutPart -- checkinPart
     * @return
     */
    public static WTPart checkinPart(WTPart part, String comment) {
        if (part != null) {
            try {
                wt.org.WTPrincipal wtprincipal = wt.session.SessionHelper.manager.getPrincipal();
                if (WorkInProgressHelper.isCheckedOut(part, wtprincipal)) {
                    if (!WorkInProgressHelper.isWorkingCopy(part)) {
                        part = (WTPart) WorkInProgressHelper.service.workingCopyOf(part);
                    }
                    // 处于检出状态
                    part = (WTPart) WorkInProgressHelper.service.checkin(part, comment);
                }
            } catch (WTPropertyVetoException ex) {
                ex.printStackTrace();
            } catch (WTException ex) {
                ex.printStackTrace();
            }
        }
        return part;
    }

    /**
     * 检出对象，检出的时候判断了是否允许被检出和CheckOutObject-checkinObject为一组,好用
     * 检出的时候应该增加判断是否为该大版本里最新的小版本
     */
    public static Workable CheckOutObject(Workable workable) throws LockException, WTException {
        Workable retVal = null;
        try {
            if (WorkableUtil.isCheckoutAllowed(workable)) {
                WorkInProgressHelper.service.checkout(workable,
                        WorkInProgressHelper.service.getCheckoutFolder(),
                        "Updating attributes during load.");
                retVal = WorkInProgressHelper.service.workingCopyOf(workable);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (retVal == null)
            throw new WTException("Checkout Failed!");
        return retVal;
    }

    public static Workable checkinObject(Workable able) {
        if (able != null) {
            try {
                wt.org.WTPrincipal wtprincipal = wt.session.SessionHelper.manager.getPrincipal();
                if (WorkInProgressHelper.isCheckedOut(able, wtprincipal)) {
                    if (!WorkInProgressHelper.isWorkingCopy(able)) {
                        able = WorkInProgressHelper.service.workingCopyOf(able);
                    }
                    // 处于检出状态
                    able = WorkInProgressHelper.service.checkin(able, "");
                }
            } catch (WTPropertyVetoException ex) {
                ex.printStackTrace();
            } catch (WTException ex) {
                System.out.println(CLASSNAME + "--> checkinPart--> checkin fail!");
                ex.printStackTrace();
            }
        }
        return able;
    }

    /**
     * 获取部件的父件
     * 
     * @param part
     * @return
     * @throws wt.util.WTException
     */
    public static Set<WTPart> getParents(WTPart part) throws WTException {
        Set<WTPart> parents = new HashSet();
        if (part == null) {
            return parents;
        }
        WTPartMaster part_master = (WTPartMaster) part.getMaster();
        QueryResult queryresult = StructHelper.service.navigateUsedBy(part_master, true);
        while (queryresult.hasMoreElements()) {
            WTPart parent = (WTPart) queryresult.nextElement();
            if (parent != null) {
                parents.add(parent);
            }
        }
        return parents;
    }

    /**
     * 获取指定视图的BOM结构部件
     * 2019
     */
    public static List<WTPart> getAllChildPart(List<WTPart> result, WTPart parent, String viewName)
            throws ViewException, WTException {
        if (result == null) {
            result = new ArrayList<WTPart>();
        }
        QueryResult qr = null;
        if (viewName == null || "".equals(viewName)) {
            qr = WTPartHelper.service.getUsesWTParts(parent, latestConfigSpec);
        } else {
            View view = ViewHelper.service.getView(viewName);
            WTPartConfigSpec config = WTPartConfigSpec.newWTPartConfigSpec(
                    WTPartStandardConfigSpec.newWTPartStandardConfigSpec(view, null));
            qr = WTPartHelper.service.getUsesWTParts(parent, config);
        }
        while (qr.hasMoreElements()) {
            Persistable[] pp = (Persistable[]) qr.nextElement();
            if (pp[1] instanceof WTPart) {
                WTPart childPart = (WTPart) pp[1];
                //String childNumber   = childPart.getNumber();
                if (viewName != null && viewName.equals(childPart.getViewName())) {
                    result.add(childPart);
                    getAllChildPart(result, childPart, viewName);
                }
            }

            /*if(viewName != null && !"".equals(viewName)){
            	//子件只能取指定视图
            	childPart = getLatestPartByViewName(childNumber,viewName);
            }
            if(childPart == null){
            	continue;
            }*/
        }
        return result;
    }

    /**
     * 获取基线中子件的数量,返回字符串如：1(每个)
     * @param baseline 基线
     * @param childPart 子件
     * @throws WTException 下午03:07:14
     */
    public static WTPartUsageLink getAmountByChildAndBaseLine(ManagedBaseline baseline,
            WTPart childPart) throws WTException {
        WTPartUsageLink uses[] = wt.clients.prodmgmt.PartHelper.getUsedBy(childPart);
        for (int i = 0; i < uses.length; i++) {
            Iterated it = uses[i].getUsedBy();
            String parentNumber = ((WTPart) it).getNumber();
            System.out.println("获取数量方法中部件：" + ((WTPart) it).getDisplayIdentifier().toString());
            //判断父件是否在基线中,如果在，返回数量
            if (baseLineContainsPart(baseline, parentNumber)) {
                Quantity q = uses[i].getQuantity();
                Double amount = q.getAmount();
                String unit = q.getUnit().getDisplay(Locale.SIMPLIFIED_CHINESE);
                System.out.println("数量：" + amount + "   单位" + unit);
                return uses[i];
            }
        }
        return null;
    }

    /**
     * 判断基线中是否包含某个编号的部件
     * @param baseline
     * @param partNumber
     * @return
     * @throws WTException 下午03:12:12
     */
    public static boolean baseLineContainsPart(ManagedBaseline baseline, String partNumber)
            throws WTException {
        QueryResult qr = BaselineHelper.service.getBaselineItems(baseline);
        while (qr.hasMoreElements()) {
            Object obj = qr.nextElement();
            if (obj instanceof WTPart) {
                WTPart part = (WTPart) obj;
                if (part.getNumber().equals(partNumber)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取指定编号部件的"Design"视图最新版
     * @param partNumber
     * @return
     * @throws WTException
     */
    public static WTPart getLatestPartByViewName(String partNumber, String viewName)
            throws WTException {
        QueryResult qr = getParts(partNumber, null, null, viewName);
        qr = latestConfigSpec.process(qr);
        if (qr.hasMoreElements()) {
            return (WTPart) qr.nextElement();
        }
        return null;
    }

    /**
     * 获取部件的design视图的最新版
     */
    public static WTPart getDesignLatest(WTPartMaster master)
            throws WTException, WTPropertyVetoException {
        return getPartByConfig(master, DesignConfigSpec);
    }

    public static PromotionNoticeConfigSpec getPromotionNoticeConfigSpec(PromotionNotice pn)
            throws WTPropertyVetoException {
        return PromotionNoticeConfigSpec.newPromotionNoticeConfigSpec(pn);
    }

    /**
     * 给部件设置类型
     * 
     * @param the_part
     * @param genericType
     * @throws WTException
     */
    public static void setGenericType(WTPart the_part, String genericType) throws WTException {
        try {
            if (genericType != null) {
                the_part.setGenericType(wt.generic.GenericType.toGenericType(genericType));
            }
        } catch (WTPropertyVetoException wtpve) {
            System.out.println(wtpve.getMessage());
        }
    }

    /**
     * 给部件设置参考文档
     * 
     * @param curr_prt
     * @param wtdoc
     * @throws WTException 
     */
    public static void createPartReferenceLink(WTPart part, WTDocument wtdoc) throws WTException {
        WTDocumentMaster docMaster = (WTDocumentMaster) wtdoc.getMaster();
        WTPartReferenceLink linkObj = WTPartReferenceLink.newWTPartReferenceLink(part, docMaster);
        PersistenceServerHelper.manager.insert(linkObj);
    }

    public static void createPartReferenceLink(WTPart part, Set<WTDocument> docs)
            throws WTException {
        WTSet referenceLinkSet = new WTHashSet();
        for (WTDocument doc : docs) {
            WTDocumentMaster master = (WTDocumentMaster) doc.getMaster();
            WTPartReferenceLink link = WTPartReferenceLink.newWTPartReferenceLink(part, master);
            referenceLinkSet.add(link);
        }

        PersistenceServerHelper.manager.insert(referenceLinkSet);
    }

    /**
     * 去掉部件和文档的参考关系
     * 
     * @param wtpart
     * @param wtdocument
     * @throws wt.util.WTException
     * @return
     */
    public static WTPart removePartReferenceLink(WTPart wtpart, WTDocument doc) throws WTException {
        String s = (WTIntrospector.getLinkInfo(WTPartReferenceLink.class).isRoleA("references")
                ? "roleAObjectRef"
                : "roleBObjectRef") + "." + "key";

        QuerySpec qs = new QuerySpec(WTDocumentMaster.class, WTPartReferenceLink.class);
        if(doc != null){
            qs.appendWhere(new SearchCondition(WTPartReferenceLink.class, s, "=",
                    PersistenceHelper.getObjectIdentifier(doc.getMaster())), 1, 1);
        }
        QueryResult qr = PersistenceServerHelper.manager.expand(wtpart, "references", qs, false);
        WTSet set = new WTHashSet();
        set.addAll(qr.getObjectVector().getVector());

        PersistenceServerHelper.manager.remove(set);
        return wtpart;
    }

    /**
     * 获取部件的参考文档
     */
    public static List<WTDocument> getReferenceDocuments(WTPart thePart) throws WTException {
        List<WTDocument> docList = new ArrayList<WTDocument>();
        QueryResult qr = PersistenceHelper.manager.navigate(thePart,
                WTPartReferenceLink.REFERENCES_ROLE, WTPartReferenceLink.class, false);
        if (qr != null)
            while (qr.hasMoreElements()) {
                WTObject obj = (WTObject) qr.nextElement();
                if (obj instanceof WTPartReferenceLink) {
                    WTPartReferenceLink reflink = (WTPartReferenceLink) obj;
                    WTDocumentMaster theMaster = reflink.getReferences();
                    QueryResult result = ConfigHelper.service.filteredIterationsOf(theMaster,
                            latestConfigSpec);
                    if (result != null && result.hasMoreElements()) {
                        WTDocument doc = (WTDocument) result.nextElement();
                        docList.add(doc);
                    }
                }
            }
        return docList;
    }

    /**
     * 给部件设置说明文档,部件或者文档升版，说明关系依然存在,并且一直与最新版本文档相关联
     * 文档则不同,初始说明文档一直关联初始版本部件，后面升版后的文档则关联最新版本的部件
     * 上午11:14:21
     * @param partOid
     * @param docOid
     * @throws WTException 
     * @throws WTRuntimeException 
     */
    public static void setPartDescribe(String partOid, String docOid)
            throws WTRuntimeException, WTException {
        WTPart part = (WTPart) new ReferenceFactory().getReference(partOid).getObject();
        WTDocument doc = (WTDocument) new ReferenceFactory().getReference(docOid).getObject();
        WTPartDescribeLink linkObj = WTPartDescribeLink.newWTPartDescribeLink(part, doc);
        PersistenceServerHelper.manager.insert(linkObj);
    }

    /**
     * 查询部件指定类型的说明文档
     * 下午03:56:59
     * @param partOid 
     * @param docType 要过滤的文档类型填写如WTDocument,XieTiaoDan等逻辑标识符即可，如果为""或者null则返回所有说明文档
     */
    public static Vector<WTDocument> getDescribedDoc(String partOid, String docType)
            throws WTException {
        WTPart wtPart = (WTPart) new ReferenceFactory().getReference(partOid).getObject();
        return getDescribedDoc(wtPart, docType);
    }

    public static Vector<WTDocument> getDescribedDoc(WTPart wtPart, String docType)
            throws WTException {
        Vector<WTDocument> WTDocs = new Vector<WTDocument>();
        QueryResult qr = WTPartHelper.service.getDescribedByDocuments(wtPart);
        while (qr.hasMoreElements()) {
            WTObject objdoc = (WTObject) qr.nextElement();
            if (objdoc instanceof WTDocument) {
                WTDocument doc = (WTDocument) objdoc;
                if ("".equals(docType) || docType == null) {
                    WTDocs.add(doc);
                    continue;
                }
                String curType = TypedUtility.getExternalTypeIdentifier(doc);
                // String shortCurType = curType.substring(curType.lastIndexOf(".") + 1, curType.length());
                if (curType.contains(docType)) {
                    WTDocs.add(doc);
                }
            }
        }
        return WTDocs;
    }

    public static String removeRelatedEParts(String docoid, String partoids)
            throws QueryException, WTException, RemoteException, InvocationTargetException {
        String returnstr = "";
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (String) RemoteMethodServer.getDefault().invoke("removeRelatedEParts",
                        PartUtil.class.getName(), null, new Class[] { String.class, String.class },
                        new Object[] { docoid, partoids });
            } else {
                boolean enforce = wt.session.SessionServerHelper.manager.setAccessEnforced(false);
                SessionHelper.manager.setAdministrator(); // 切换到管理员权限
                int deletenumber = 0;
                ReferenceFactory rf = new ReferenceFactory();
                QueryResult qr;
                QueryResult qr1;
                WTDocument doc = (WTDocument) rf.getReference(docoid).getObject();

                String[] partoidsArray = partoids.split(",");
                try {
                    for (int i = 0; i < partoidsArray.length; i++) {
                        WTPart epart = (WTPart) rf.getReference(partoidsArray[i]).getObject();

                        qr = PersistenceHelper.manager.navigate(epart,
                                WTPartDescribeLink.DESCRIBED_BY_ROLE, WTPartDescribeLink.class,
                                false);
                        while (qr.hasMoreElements()) {
                            WTPartDescribeLink link = (WTPartDescribeLink) qr.nextElement();
                            if (link.getDescribedBy().equals(doc)) {
                                PersistenceServerHelper.manager.remove(link);
                                deletenumber++;
                            }
                        }

                        qr1 = PersistenceHelper.manager.navigate(epart,
                                WTPartReferenceLink.REFERENCES_ROLE, WTPartReferenceLink.class,
                                false);
                        while (qr1.hasMoreElements()) {
                            WTObject wtobject = (WTObject) qr1.nextElement();
                            if (wtobject instanceof WTPartReferenceLink) {
                                WTPartReferenceLink reflink = (WTPartReferenceLink) wtobject;
                                WTDocumentMaster theMaster = reflink.getReferences();
                                if (theMaster.getNumber().equals(doc.getNumber())) {
                                    PersistenceServerHelper.manager.remove(reflink);
                                    deletenumber++;
                                }
                            }
                        }
                    }
                    if (deletenumber == partoidsArray.length)
                        returnstr = "success";
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnstr;
    }

    /**
     * 
     * 去掉部件和文档的说明关系
     * 
     * @param partOid
     * @param docOid
     */
    public static void removePartDescribeLink(WTPart wtpart, WTDocument doc) {

        wt.pom.Transaction transaction = new wt.pom.Transaction();
        try {
            transaction.start();
            QueryResult qr = PersistenceHelper.manager.navigate(wtpart,
                    WTPartDescribeLink.DESCRIBED_BY_ROLE, WTPartDescribeLink.class, false);
            while (qr.hasMoreElements()) {
                WTPartDescribeLink link = (WTPartDescribeLink) qr.nextElement();
                if (doc != null) {
                    if (link.getDescribedBy().equals(doc)) {
                        logger.debug("移除说明文档-----" + doc.getDisplayIdentity());
                        PersistenceServerHelper.manager.remove(link);
                    }
                } else {
                    PersistenceServerHelper.manager.remove(link);
                }
            }
            transaction.commit();
            transaction = null;
        } catch (WTException e) {
            e.printStackTrace();
        } finally {
            if (transaction != null) {
                transaction.rollback();
            }
        }
    }

    /**
     * 搜索指定容器下指定视图的成品件,参数可以为空
     * 4:50:42 PM
     * @param containerOid null时查询所有容器
     * @param viewName null时查询所有视图
     * @return
     * @throws InvocationTargetException 
     * @throws RemoteException 
     */
    public static QueryResult searchEndItemByContainer(String containerOid, String viewName) throws RemoteException, InvocationTargetException {
        if (!RemoteMethodServer.ServerFlag) {
        		return(QueryResult) RemoteMethodServer.getDefault().invoke(
					"searchEndItemByContainer",
					CLASSNAME,
					null,
					new Class[] { String.class, String.class},
					new Object[] { containerOid, viewName});

    }
        try {
            QuerySpec qs = new QuerySpec(WTPart.class);
            qs.appendWhere(new SearchCondition(WTPart.class, WTPart.LATEST_ITERATION,
                    SearchCondition.IS_TRUE), new int[] { 0 });
            qs.appendAnd();
            qs.appendWhere(
                    new SearchCondition(WTPart.class, WTPart.END_ITEM, SearchCondition.IS_TRUE),
                    new int[] { 0 });
            if (viewName != null && viewName.trim().length() > 0) {
                View view = ViewHelper.service.getView(viewName);
                qs.appendAnd();
                qs.appendWhere(
                        new SearchCondition(WTPart.class, WTPart.VIEW + "." + ObjectReference.KEY,
                                SearchCondition.EQUAL, PersistenceHelper.getObjectIdentifier(view)),
                        new int[] { 0 });
            }
            if (containerOid != null && containerOid.trim().length() > 0) {
                qs.appendAnd();
                qs.appendWhere(
                        new SearchCondition(WTPart.class, WTPart.CONTAINER_ID,
                                SearchCondition.EQUAL,
                                Long.parseLong(
                                        containerOid.substring(containerOid.lastIndexOf(':') + 1))),
                        new int[] { 0 });
            }
            qs = latestConfigSpec.appendSearchCriteria(qs);
            return PersistenceHelper.manager.find(qs);
        } catch (WTException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Hashtable getEndItem(String containerOid, String viewName) throws RemoteException, InvocationTargetException {
        Vector vec = new Vector();
        Hashtable ptable = new Hashtable();
        QueryResult qr = searchEndItemByContainer(containerOid, viewName);
        vec = qr.getObjectVector().getVector();
        for (int i = vec.size() - 1; i >= 0; i--) {
            Object obj = vec.get(i);
            WTPart part = null;
            if (obj instanceof WTPart) {
                part = (WTPart) obj;
                if (!vec.contains(part.getNumber())) {
                    vec.add(part.getNumber());

                    String oid = getWTObjectOid(part);
                    ptable.put(oid, part.getName());
                }
            }
        }
        return ptable;

    }

    /**
     * 通过对象获得Oid
     * 
     * @param instance
     * @return
     * @throws WTException
     */
    public static String getWTObjectOid(Persistable instance) {
        try {
            String oid = new ReferenceFactory().getReferenceString(ObjectReference
                    .newObjectReference((instance.getPersistInfo().getObjectIdentifier())));
            return oid;
        } catch (WTException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取部件相关的EpmDocument，包括2D、3D
     */
    public static Vector<EPMDocument> getAssEpmDocument(WTPart part) throws WTException {
        logger.debug("查找部件关联的EPM文档:" + part.getDisplayIdentity());
        Vector<EPMDocument> vec = new Vector<EPMDocument>();
        Collection coll = PartDocServiceCommand.getAssociatedCADDocumentsAndLinks(part);
        Object[] obj = coll.toArray();
        for (Object tem : obj) {
            AssociationLinkObject link = (AssociationLinkObject) tem;
            logger.debug("找到EPM文档:" + link.getCadObject().getDisplayIdentity());
            vec.add(link.getCadObject());
        }
        /*		Vector<EPMDocument> vec = new Vector<EPMDocument>();
        QueryResult associates = PartDocHelper.service.getAssociatedDocuments(part);
        while (associates.hasMoreElements()) {
        	Object doc = associates.nextElement();
        	if (doc instanceof EPMDocument){
        		EPMDocument epm = (EPMDocument) doc;
                if (!vec.contains(epm)) {
                	vec.addElement(epm);
                }
        	}
        
        }*/
        return vec;
    }
    
    /**
     * 创建当前对象的指定小版本对象,可跳空升小版
     * @description
     * @param iterated
     * @param iteration
     * @throws VersionControlException
     * @throws WTPropertyVetoException
     * @throws WTException
     */
    public static void newIteration(Iterated iterated, String iteration) throws VersionControlException, WTPropertyVetoException, WTException{
    	//先升一个小版本
    	iterated = (Iterated) VersionControlHelper.service.newIteration(iterated);
    	if(!(iterated.getIterationInfo().getIdentifier().getValue()+"").equals(iteration)){
        	//再修改为指定小版本
        	changeIteration(iterated,iteration);
    	}
    }
    
    /**
     * 调整对象小版本,比如将A.2改为A.3,同时A.2消失
     * @param iterated
     * @param s 为小版本,如1,2等
     * 2011-8-24 下午05:17:10
     */
    public static void changeIteration(Iterated iterated, String s) {
        try {
            if (s != null) {
                Series series = Series.newSeries("wt.vc.IterationIdentifier", s);
                IterationIdentifier iterationidentifier = IterationIdentifier
                        .newIterationIdentifier(series);
                VersionControlHelper.setIterationIdentifier(iterated, iterationidentifier);
            }
        } catch (WTPropertyVetoException e) {
            e.printStackTrace();
        } catch (SeriesException e) {
            e.printStackTrace();
        } catch (WTException e) {
            e.printStackTrace();
        }
    }

    /**
     * 修订部件
     * 存储到当前版本所在的文件夹
     * @description
     * @param part
     * @return
     * @throws VersionControlException
     * @throws WTPropertyVetoException
     * @throws WTException
     */
    public static WTPart newVersion(WTPart part)
            throws VersionControlException, WTPropertyVetoException, WTException {
        WTPart p = (WTPart) VersionControlHelper.service.newVersion(part);
        FolderHelper.assignLocation((FolderEntry) p, FolderHelper.getFolder(part));
        PersistenceHelper.manager.save(p);
        return p;
    }
    
    /**
     * 新建修订版本:将部件调整到指定版本,新版本部件与原部件文件夹一致
     * @param part
     * @param version 大版本,必须是新的大版本
     * @param revision 小版本
     * @return
     * @throws VersionControlException
     * @throws WTPropertyVetoException
     * @throws WTException 
     * 2011-8-25 下午02:52:20
     */
    public static WTPart newVersionIteration(WTPart part, String version, String revision)
            throws VersionControlException, WTPropertyVetoException, WTException {
        WTPart p = (WTPart) VersionControlHelper.service.newVersion(part, newVersionId(version),
                newIterationId(revision));
        FolderHelper.assignLocation((FolderEntry) p, FolderHelper.getFolder(part));
        PersistenceHelper.manager.save(p);
        return p;
    }

    public static IterationIdentifier newIterationId(String iterationId)
            throws WTPropertyVetoException, WTException {
        IntegerSeries is;
        is = IntegerSeries.newIntegerSeries();
        is.setValueWithoutValidating(iterationId);
        return IterationIdentifier.newIterationIdentifier(is);
    }

    public static VersionIdentifier newVersionId(String versionId)
            throws WTPropertyVetoException, WTException {
        HarvardSeries hs = HarvardSeries.newHarvardSeries();
        hs.setValue(versionId);
        return VersionIdentifier.newVersionIdentifier(hs);
    }

    /**
     * 将部件检出检入升小版
     * @param part
     * @throws WorkInProgressException
     * @throws WTPropertyVetoException
     * @throws PersistenceException
     * @throws WTException 
     * 2011-8-24 下午05:19:38
     */
    public static WTPart newIteration(WTPart part) throws WorkInProgressException,
            WTPropertyVetoException, PersistenceException, WTException {
        if (!(WorkInProgressHelper.isCheckedOut(part))) {
            part = (WTPart) WorkableUtil.doCheckOut(part,"检出");
        }
        if (WorkInProgressHelper.isCheckedOut(part))
            part = (WTPart) WorkInProgressHelper.service.checkin(part, "检入");

        return part;
    }


    /**
     * 创建新视图的零部件
     * 
     * @param Vector
     *            reviewPartsList
     * @param String
     *            viewName return type void
     */

    public static WTPart createNewPartWithView(WTPart wtpart, String state, String viewname)
            throws WTException, WTPropertyVetoException, RemoteException,
            InvocationTargetException {
        if (!RemoteMethodServer.ServerFlag) {
            return (WTPart) RemoteMethodServer.getDefault().invoke("createNewPartWithView",
                    PartUtil.class.getName(), null, new Class[] { WTPart.class, String.class },
                    new Object[] { wtpart, viewname });
        }
        WTPart newPart = null;
        if (wtpart != null) { // 由P视图修订视图到M视图
            wtpart = (WTPart) LifeCycleServerHelper.service.setState((LifeCycleManaged) wtpart,
                    State.toState(state));
            newPart = (WTPart) ViewHelper.service.newBranchForView(wtpart, viewname);
            newPart = (WTPart) PersistenceHelper.manager.store(newPart);
        }
        return newPart;
    }
    
    /**
     * 新建部件视图版本
     * @description
     * @param wtpart   当前要操作的部件
     * @param state    新建视图版本后的目标状态
     * @param viewname 视图名称
     * @return
     * @throws WTException
     * @throws WTPropertyVetoException
     * @throws RemoteException
     * @throws InvocationTargetException
     */
    public static WTPart createNewViewPart(WTPart wtpart, String viewname)
            throws WTException, WTPropertyVetoException, RemoteException,
            InvocationTargetException {
        if (!RemoteMethodServer.ServerFlag) {
            return (WTPart) RemoteMethodServer.getDefault().invoke("createNewViewPart",
                    PartUtil.class.getName(), null, new Class[] { WTPart.class, String.class },
                    new Object[] { wtpart, viewname });
        }
        WTPart newPart = null;
        if (wtpart != null) { 
            newPart = (WTPart) ViewHelper.service.newBranchForView(wtpart, viewname);
            newPart = (WTPart) PersistenceHelper.manager.store(newPart);
        }
        return newPart;
    }
    
    
    public static Vector getWTPartEpmDoc(WTPart thePart) throws WTException {
        Vector vResult = new Vector();
        QueryResult qr1 = PersistenceHelper.manager.navigate(thePart,
                EPMDescribeLink.DESCRIBED_BY_ROLE, EPMDescribeLink.class, false);
        while (qr1.hasMoreElements()) {
            EPMDescribeLink link = (EPMDescribeLink) qr1.nextElement();
            EPMDocument epm = link.getDescribedBy();
            //只取几何类型为CATDrawing 的CAD文档
            //	            if("CATDrawing".equals(epm.getDocType().toString())){
            //	            	vResult.add(epm);
            //	            }
        }
        return vResult;
    }

    /**
    * 判断部件是否有子件
    * @param part
    * @return
    * @throws WTException
    */
    public static boolean hasChild(WTPart part, ConfigSpec config) throws WTException {
        QueryResult qr = getSubParts(part, config);
        if (qr.hasMoreElements()) {
            return true;
        }
        return false;
    }

    
    /**
     * 以当前部件为顶层件获取整个BOM上的部件
     * @description
     * @param rootPart
     * @param spec
     * @return
     * @throws WTException
     */
    public static Set<WTPart> getAllBOMParts(WTPart rootPart, ConfigSpec spec) throws WTException {
        Set<WTPart> allParts = new HashSet<WTPart>();
        List<Map> result = PartUtil.getAllSubPartsAndLevel(rootPart, spec, 0);
        for (int i = 0; i < result.size(); i++) {
            Map partMap = result.get(i);
            WTPart part = (WTPart) partMap.get("part");
            allParts.add(part);
        }
        allParts.add(rootPart);
        return allParts;
    }

    /**
     * 以深度遍历方式获取整个BOM结构信息(不包含根节点部件)
     * @description
     * @param parentPart 根节点部件
     * @param config     配置规范
     * @param currentLevel 根节点部件层级
     * @return
     * @throws WTException
     */
    public static List<Map> getAllSubPartsAndLevel(WTPart parentPart, ConfigSpec config,
            Integer currentLevel) throws WTException {
        List<Map> result = new ArrayList<Map>();
        Map<WTPart, WTPartUsageLink> childPartsLinks = PartUtil.getSubPartsAndUsageLink(parentPart,
                config);
        Set<WTPart> childParts = childPartsLinks.keySet();
        Iterator<WTPart> it = childParts.iterator();
        String viewName = null;
        if (config instanceof ViewConfigSpec) {
            viewName = ((ViewConfigSpec) config).getView().getName();
        }
        while (it.hasNext()) {
            WTPart childPart = it.next();
            if (viewName != null && !childPart.getView().getName().equals(viewName)) {
                continue;
            }
            WTPartUsageLink link = childPartsLinks.get(childPart);
            Map map = new HashMap();
            //层级
            map.put("level", (currentLevel + 1) + "");
            map.put("link", link);
            //当前部件
            map.put("part", childPart);
            map.put("parent", parentPart);
            //单装数量
            map.put("amount", link.getQuantity().getAmount() + "");
            //类型显示名称
            map.put("typeDisplay", WTUtil.getObjectTypeDisplay(childPart));
            result.add(map);
            logger.debug("父件:" + parentPart.getDisplayIdentity() + "子件:" + map);
            result.addAll(getAllSubPartsAndLevel(childPart, config, currentLevel + 1));
        }
        return result;
    }

    /**
     * 依据配置规范查询第一层父件和link
     * 如果参数存在视图配置规范,则对查询结果依据视图名称二次过滤(OOTB场景:如果没有当前视图,取上一级视图)
     * @param parentPart
     * @param configSpec
     * @return
     * @throws WTException
     */
    public static Map<WTPart, WTPartUsageLink> getParentPartsAndUsageLink(WTPart childPart,
            ConfigSpec[] configs) throws WTException {
        Map<WTPart, WTPartUsageLink> result = new HashMap<WTPart, WTPartUsageLink>();
        QueryResult qr = WTPartHelper.service
                .getUsedByWTParts((WTPartMaster) childPart.getMaster());
        String viewName = null;
        if (configs != null) {
            for (ConfigSpec config : configs) {
                qr = config.process(qr);
                if (config instanceof ViewConfigSpec) {
                    viewName = ((ViewConfigSpec) config).getView().getName();
                }
            }
        }
        while (qr.hasMoreElements()) {
            WTPart parentPart = (WTPart) qr.nextElement();
            if (viewName != null && !parentPart.getView().getName().equals(viewName)) {
                continue;
            }
            WTPartUsageLink usageLink = getPartUsageLink(parentPart,
                    (WTPartMaster) childPart.getMaster());
            result.put(parentPart, usageLink);
        }
        return result;
    }

    /**
     * 依据配置规范查询第一层子件和link
     * 如果参数为视图配置规范,则对查询结果依据视图名称二次过滤(OOTB场景:如果没有当前视图,取上一级视图)
     * @param parentPart
     * @param configSpec
     * @return
     */
    public static Map<WTPart, WTPartUsageLink> getSubPartsAndUsageLink(WTPart parentPart,
            ConfigSpec configSpec) throws WTException {
        Map<WTPart, WTPartUsageLink> result = new HashMap<WTPart, WTPartUsageLink>();
        if (configSpec == null) {
            configSpec = latestConfigSpec;
        }
        String viewName = null;
        if (configSpec instanceof ViewConfigSpec) {
            viewName = ((ViewConfigSpec) configSpec).getView().getName();
        }
        QueryResult qr2 = WTPartHelper.service.getUsesWTParts(parentPart, configSpec);
        while (qr2.hasMoreElements()) {
            Persistable[] objs = (Persistable[]) qr2.nextElement();
            if (objs[1] instanceof WTPart) {
                WTPart part = (WTPart) objs[1];
                if (viewName != null && !part.getView().getName().equals(viewName)) {
                    continue;
                }
                result.put(part, (WTPartUsageLink) objs[0]);
            }
        }
        return result;
    }

    /**
     * 
     * 获取最新版子件
     * @param parentPart
     * @return
     * @throws WTException
     */
    public static QueryResult getLatestSubParts(WTPart parentPart) throws WTException {
    	return getSubParts(parentPart,latestConfigSpec);
    }
    
    /**
     * 通过配置规范获取子件
     * 如果参数为视图配置规范,则对查询结果依据视图名称二次过滤(OOTB场景:如果没有当前视图,取上一级视图)
     * @param parentPart
     * @param configSpec
     * @return
     * @throws WTException
     */
    public static QueryResult getSubParts(WTPart parentPart, ConfigSpec configSpec)
            throws WTException {
        ReferenceFactory rf = new ReferenceFactory();
        Vector<WTPart> vResult = new Vector<WTPart>();
        if (configSpec == null) {
            configSpec = latestConfigSpec;
        }
        String viewName = null;
        if (configSpec instanceof ViewConfigSpec) {
            viewName = ((ViewConfigSpec) configSpec).getView().getName();
        }
        QueryResult qr2 = WTPartHelper.service.getUsesWTParts(parentPart, configSpec);
        while (qr2.hasMoreElements()) {
            Persistable[] objs = (Persistable[]) qr2.nextElement();
            if (objs[1] instanceof WTPart) {
                WTPart child = (WTPart) objs[1];
                if (viewName != null && !child.getView().getName().equals(viewName)) {
                    continue;
                }
                vResult.add((WTPart) objs[1]);
            }
        }
        return new QueryResult((ObjectVectorIfc) new ObjectSetVector(vResult));
    }
    
    /**
     * 查询指定视图、生命周期状态的第一层子件
     * @description
     * @param parentPart 父件
     * @param viewName   视图,为空时取最新版本过滤
     * @param state      生命周期状态
     * @return
     * @throws WTException
     * @throws WTPropertyVetoException
     */
    public static QueryResult getSubParts(WTPart parentPart, String viewName, String state)
            throws WTException, WTPropertyVetoException {
        Vector<WTPart> vResult = new Vector<WTPart>();
        ConfigSpec configSpec = null;
        if (!StringUtils.isEmpty(viewName)) {
            configSpec = PartUtil.getViewConfigSpec(viewName);
        }
        QueryResult qr = getSubParts(parentPart, configSpec);
        while (qr.hasMoreElements()) {
            WTPart childPart = (WTPart) qr.nextElement();
            if (!StringUtils.isEmpty(state)
                    && !childPart.getLifeCycleState().toString().equals(state)) {
                continue;
            }
            vResult.add(childPart);
        }
        return new QueryResult((ObjectVectorIfc) new ObjectSetVector(vResult));
    }
    
    /**
     * 递归获取所有层级部件,不包含根节点部件
     * @description
     * @author     
     * @param allParts 用于接收返回结果,调用时传递null即可
     * @param parentPart 根节点部件(父件)
     * @param configSpec 配置规范(子件过滤条件)
     * @return 
     * @throws WTException
     * @throws RemoteException
     * @throws InvocationTargetException
     */
    public static Set<WTPart> getBOMParts(Set<WTPart> allParts, WTPart parentPart,
            ConfigSpec configSpec) throws WTException, RemoteException, InvocationTargetException {
        boolean accessEnforced = false;
        try {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            logger.debug("获取EBOM部件,父节点:" + parentPart.getDisplayIdentity());
            if (allParts == null) {
                allParts = new HashSet<WTPart>();
            }
            QueryResult qr = getSubParts(parentPart, configSpec);
            while (qr.hasMoreElements()) {
                WTPart childPart = (WTPart) qr.nextElement();
                allParts.add(childPart);
                getBOMParts(allParts, childPart, configSpec);
            }
        } finally {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
        }
        return allParts;
    }
    
    /**
     * 递归获取所有层级部件,不包含根节点部件
     * @description
     * @param allParts   用于接收返回结果,调用时传递null即可
     * @param parentPart 根节点部件(父件)
     * @param viewName   视图过滤条件
     * @param state      生命周期状态过滤条件
     * @return
     * @throws Exception
     */
    public static Set<WTPart> getBOMParts(Set<WTPart> allParts, WTPart parentPart, String viewName,
            String state) throws Exception {
        boolean accessEnforced = false;
        try {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            logger.debug("获取EBOM部件,父节点:" + parentPart.getDisplayIdentity());
            if (allParts == null) {
                allParts = new HashSet<WTPart>();
            }
            ConfigSpec configSpec = PartUtil.getViewConfigSpec(viewName);
            QueryResult qr = getSubParts(parentPart, configSpec);
            while (qr.hasMoreElements()) {
                WTPart childPart = (WTPart) qr.nextElement();
                if (!StringUtils.isEmpty(state)
                        && !childPart.getLifeCycleState().toString().equals(state)) {
                    continue;
                }
                /*childPart = getLatestStateViewPart(childPart.getName(),viewName,state);*/
                allParts.add(childPart);
                getBOMParts(allParts, childPart, viewName, state);
            }
        } finally {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
        }
        return allParts;
    }

    /**
     * 获取视图配置规范
     * @param viewName
     * @return
     * @throws WTException
     * @throws WTPropertyVetoException 
     * 2011-9-26 上午09:28:00
     */
    public static ConfigSpec getViewConfigSpec(String viewName)
            throws WTException, WTPropertyVetoException {
        ConfigSpec configSpec = null;
        View view = ViewHelper.service.getView(viewName);
        WTPartStandardConfigSpec wtpartstandardconfigspec = WTPartStandardConfigSpec
                .newWTPartStandardConfigSpec();
        wtpartstandardconfigspec.setView(view);
        configSpec = wtpartstandardconfigspec;
        return configSpec;
    }

    public static ConfigSpec getBaseLineConfigSpec(ManagedBaseline baseline)
            throws WTException, WTPropertyVetoException {
        ConfigSpec configSpec = null;
        boolean access = true;
        try {
            access = SessionServerHelper.manager.setAccessEnforced(false);
            configSpec = BaselineConfigSpec.newBaselineConfigSpec(baseline);
        } finally {
            SessionServerHelper.manager.setAccessEnforced(access);
        }
        return configSpec;
    }

    /**
     * 获取部件一个大版本标识,如：A正确,201111120
     * 
     * @param part
     * @return
     */
    public static String getFirstVersionId(WTPart part) {
        try {
            MultilevelSeries ms = part.getVersionIdentifier().getSeries();
            String seriesName = MultilevelSeries.getSubseries()[ms.getLevel().intValue()];
            return Series.newSeries(seriesName).getValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "A";
    }

    /**
     * 获取部件所有大版本里的最新小版本
     */
    public static QueryResult getAllVersionLatestPart(WTPartMaster wtpartmaster)
            throws WTException {
        QueryResult qr = VersionControlHelper.service.allVersionsOf(wtpartmaster);
        qr = new MultipleLatestConfigSpec().process(qr);
        return qr;
    }

    /**
     * 通过部件编号和视图名称查找最新版部件
     * @description
     * @param number
     * @param viewName
     * @return
     * @throws Exception
     */
    public static WTPart getLatestPart(String number, String viewName) throws Exception {
        QueryResult qr = getParts(number, null, null, viewName);
        qr = latestConfigSpec.process(qr);
        if (qr.hasMoreElements()) {
            WTPart part = (WTPart) qr.nextElement();
            return part;
        }
        return null;
    }

    /**
     * 获取指定编号,指定大版的部件最新小版本
     * @param number
     * @param version
     * @return
     * @throws WTException 
     * 2011-11-24 下午08:08:26
     */
    public static WTPart getLatestPartByNumberVersion(String number, String version)
            throws WTException {
        QuerySpec qs = new QuerySpec(WTPart.class);

        String equal = SearchCondition.EQUAL;
        SearchCondition numberSC = new SearchCondition(WTPart.class, WTPart.NUMBER, equal,
                number.toUpperCase(), false);
        qs.appendWhere(numberSC, new int[0]);

        String versionColumn = wt.vc.Versioned.VERSION_INFO + "." + wt.vc.VersionInfo.IDENTIFIER
                + "." + "versionId";
        SearchCondition versionSC = new SearchCondition(WTPart.class, versionColumn, equal, version,
                false);
        qs.appendAnd();
        qs.appendWhere(versionSC, new int[0]);

        QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
        qr = latestConfigSpec.process(qr);
        if (qr.hasMoreElements()) {
            return (WTPart) qr.nextElement();
        }
        return null;
    }

    /**
     * 获取指定父件下指定子件的装配数量
     * 
     * @param partentPart
     * @param subPart
     * @return
     */
    public static String getQty(WTPart partentPart, WTPart subPart) {
        int amount = 0;
        if (partentPart != null && subPart != null) {
            try {
                QuerySpec qs = new QuerySpec(WTPartUsageLink.class);
                qs.appendWhere(
                        new SearchCondition(WTPartUsageLink.class,
                                ObjectToObjectLink.ROLE_AOBJECT_REF + "." + ObjectReference.KEY
                                        + "." + ObjectIdentifier.ID,
                                SearchCondition.EQUAL,
                                PersistenceHelper.getObjectIdentifier(partentPart).getId()),
                        new int[] { 0 });
                qs.appendAnd();
                qs.appendWhere(
                        new SearchCondition(WTPartUsageLink.class,
                                ObjectToObjectLink.ROLE_BOBJECT_REF + "." + ObjectReference.KEY
                                        + "." + ObjectIdentifier.ID,
                                SearchCondition.EQUAL,
                                PersistenceHelper.getObjectIdentifier(subPart.getMaster()).getId()),
                        new int[] { 0 });

                QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
                while (qr.hasMoreElements()) {
                    WTPartUsageLink link = (WTPartUsageLink) qr.nextElement();
                    Quantity qty = link.getQuantity();
                    amount += qty.getAmount();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //保证获取的数量不会是0
        if (amount == 0)
            amount = 1;
        return String.valueOf(amount);
    }
    
    /**
     * 通过父件获取所有子件的link
     * @description
     * @author      ZhongBinpeng
     * @param partentPart 父件
     * @return
     * @throws WTException
     */
    public static QueryResult getWTPartUsageLink(WTPart partentPart) throws WTException {
        QuerySpec qs = new QuerySpec(WTPartUsageLink.class);
        qs.appendWhere(
                new SearchCondition(WTPartUsageLink.class,
                        ObjectToObjectLink.ROLE_AOBJECT_REF + "." + ObjectReference.KEY + "."
                                + ObjectIdentifier.ID,
                        SearchCondition.EQUAL,
                        PersistenceHelper.getObjectIdentifier(partentPart).getId()),
                new int[] { 0 });
        QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
        return qr;
    }

    /**
     * 依据视图、编号、大版、小版查询部件
     * @param strNumber
     * @param strVersion
     * @param strIterationId
     * @param strView
     * @return
     * @throws WTException 
     * 2011-11-14 上午11:30:52
     */
    public static QueryResult getParts(String strNumber, String strVersion, String strIterationId,
            String strView) throws WTException {
        QueryResult qr = null;
        if (strNumber == null) {
            return null;
        }
        // latestConfigSpec latestconfigspec = null;
        strNumber = strNumber.toUpperCase();
        QuerySpec qs = new QuerySpec(WTPart.class);
        qs.setAdvancedQueryEnabled(true);
        int partIndex = qs.getFromClause().getPosition(WTPart.class);
        qs.appendWhere(new SearchCondition(WTPart.class, "master>number", "=", strNumber, false),
                new int[] { partIndex });
        if (strView != null) {
            View view = ViewHelper.service.getView(strView);
            if (view != null) {
                qs.appendAnd();
                qs.appendWhere(
                        new SearchCondition(WTPart.class, "view.key", "=",
                                PersistenceHelper.getObjectIdentifier(view)),
                        new int[] { partIndex });
            }
        }
        if (StringUtils.isNotBlank(strVersion)) {
            qs.appendAnd();
            qs.appendWhere(new SearchCondition(WTPart.class, "versionInfo.identifier.versionId",
                    "=", strVersion, false), new int[] { partIndex });
            if (StringUtils.isNotBlank(strIterationId)) {
                qs.appendAnd();
                qs.appendWhere(new SearchCondition(WTPart.class,
                        "iterationInfo.identifier.iterationId", "=", strIterationId, false),
                        new int[] { partIndex });
            } else {
                qs.appendAnd();
                SearchCondition sc = new SearchCondition(WTPart.class, Iterated.LATEST_ITERATION,
                        SearchCondition.IS_TRUE);
                qs.appendWhere(sc, new int[] { partIndex });
            }

            ClassAttribute createStampAttr = new ClassAttribute(WTPart.class,
                    "thePersistInfo.createStamp");
            qs.appendOrderBy(new OrderBy(createStampAttr, false), new int[] { partIndex });
            qr = PersistenceHelper.manager.find((StatementSpec) qs);
        } else {
            //若没有版本信息，则取最新版
            qs.appendAnd();
            SearchCondition sc = new SearchCondition(WTPart.class, Iterated.LATEST_ITERATION,
                    SearchCondition.IS_TRUE);
            qs.appendWhere(sc, new int[] { partIndex });

            qr = PersistenceHelper.manager.find((StatementSpec) qs);
            qr = latestConfigSpec.process(qr);
        }
        return qr;
    }

    /**
     * 获取部件关联的数模文件名称
     * @param part
     * @return
     * @throws PropertyVetoException 
     * @throws WTException 
     */
    public static String getEpmFileName(WTPart part) throws WTException, PropertyVetoException {
        QueryResult associates = PartDocHelper.service.getAssociatedDocuments(part);
        while (associates.hasMoreElements()) {
            Object doc = associates.nextElement();
            if (doc instanceof EPMDocument) {
                return getEpmFileName((EPMDocument) doc);
            }
        }
        System.out.println("部件无关联EPM,编号:" + part.getName() + ",ida2a2:"
                + part.getPersistInfo().getObjectIdentifier().getId());
        return "";
    }

    /**
     * 获取epm文档主内容文件名称
     * @param epm
     * @return
     * @throws WTException
     * @throws PropertyVetoException
     */
    public static String getEpmFileName(EPMDocument epm) throws WTException, PropertyVetoException {
        wt.content.ContentHolder contentholder = epm;
        contentholder = wt.content.ContentHelper.service.getContents(contentholder);
        if (contentholder instanceof FormatContentHolder) {
            ContentItem contentItem = wt.content.ContentHelper
                    .getPrimary((FormatContentHolder) contentholder);
            if (contentItem != null && contentItem instanceof ApplicationData) {
                ApplicationData app = (ApplicationData) contentItem;
                return replaceEpmFileName(app.getFileName(), epm);
            }
        }
        System.out.println("EPMDocument无主内容,编号:" + epm.getName());
        return "";
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
     * 移除部件说明文档和EPM文档关系
     * @param thePart
     * @throws WTException
     */
    public static void removeRevisePartEpmRelation(WTPart thePart) throws WTException {
        try {

            QueryResult qr1 = PersistenceHelper.manager.navigate(thePart,
                    EPMBuildRule.BUILD_SOURCE_ROLE, EPMBuildRule.class, false);
            while (qr1.hasMoreElements()) {
                EPMBuildRule link = (EPMBuildRule) qr1.nextElement();
                PersistenceServerHelper.manager.remove(link);
            }

            QueryResult qr2 = PersistenceHelper.manager.navigate(thePart,
                    EPMBuildHistory.BUILT_BY_ROLE, EPMBuildHistory.class, false);
            while (qr2.hasMoreElements()) {
                EPMBuildHistory link = (EPMBuildHistory) qr2.nextElement();
                // System.out.println("---delete link is:" + link);
                PersistenceServerHelper.manager.remove(link);
            }

            // 描述关系(IteratedDescribeLink)
            QueryResult qr3 = PersistenceHelper.manager.navigate(thePart,
                    WTPartDescribeLink.DESCRIBED_BY_ROLE, WTPartDescribeLink.class, false);
            while (qr3.hasMoreElements()) {
                WTPartDescribeLink link = (WTPartDescribeLink) qr3.nextElement();
                Iterated doc = link.getDescribedBy();
                if (doc instanceof WTDocument) {
                    WTDocument theDocument = (WTDocument) doc;
                    PersistenceServerHelper.manager.remove(link);
                }
            }

            QueryResult qr4 = PersistenceHelper.manager.navigate(thePart,
                    EPMDescribeLink.DESCRIBED_BY_ROLE, EPMDescribeLink.class, false);
            while (qr4.hasMoreElements()) {
                EPMDescribeLink link = (EPMDescribeLink) qr4.nextElement();
                PersistenceServerHelper.manager.remove(link);
            }

        } catch (WTException wte) {
            System.out.println("removeRevisePartEpmRelation Error:" + wte);
        }

    }

    /**
     * 将Part上的文档复制到新版本中
     *
     * @param oldpart old part
     * @param newpart new part
     */
    private void copyOldLink(WTPart oldpart, WTPart newpart) {
        try {
            QueryResult qr = PersistenceHelper.manager.navigate(oldpart,
                    WTPartDescribeLink.DESCRIBED_BY_ROLE, WTPartDescribeLink.class, false);
            while (qr.hasMoreElements()) {
                WTPartDescribeLink link = (WTPartDescribeLink) qr.nextElement();
                Iterated doc = link.getDescribedBy();
                if (doc instanceof WTDocument) {
                    WTPartDescribeLink newlink = WTPartDescribeLink.newWTPartDescribeLink(newpart,
                            (WTDocument) doc);
                    PersistenceServerHelper.manager.insert(newlink);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*	public static WTPart parts(String number,String version, String iteration) throws WTException {
        if (number == null)
            return null;
            QuerySpec queryspec = new QuerySpec(WTPart.class);
            queryspec.appendWhere(new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL,
                    number.toUpperCase(), false));
    
            if (version != null) {
                queryspec.appendAnd();
                queryspec.appendWhere(new SearchCondition(WTPart.class, Versioned.VERSION_IDENTIFIER + "."
                        + VersionIdentifier.VERSIONID, SearchCondition.EQUAL, version, false));
                if (iteration != null) {
                    queryspec.appendAnd();
                    queryspec.appendWhere(new SearchCondition(WTPart.class, Iterated.ITERATION_IDENTIFIER + "."
                            + IterationIdentifier.ITERATIONID, SearchCondition.EQUAL, iteration, false));
                } else {
                    queryspec.appendAnd();
                    queryspec.appendWhere(new SearchCondition(WTPart.class, Iterated.ITERATION_INFO + "."
                            + IterationInfo.LATEST, "TRUE"));
                }
            }
            QueryResult queryresult = PersistenceHelper.manager.find(queryspec);
            if (queryresult.size() > 0) {
            	return (WTPart) queryresult.nextElement();
            }
    
        return null;
    }*/

    public static boolean isCurrentVersionLatest(WTPart part) throws WTException {
        long currentID = part.getPersistInfo().getObjectIdentifier().getId();
        QueryResult qr = latestConfigSpec.process(
                getParts(part.getNumber(), part.getVersionIdentifier().getValue(), null, null));
        WTPart currentVersionLatestPart = (WTPart) qr.nextElement();
        long latestID = currentVersionLatestPart.getPersistInfo().getObjectIdentifier().getId();
        return currentID == latestID;
    }

    /**
     * 更改已存在的部件类型
     * @param newType
     * @param part
     * @throws WTException
     * @throws RemoteException
     * @throws WTPropertyVetoException
     * @throws ParseException
     */
    public static void changePartType(String newType, WTPart part)
            throws WTException, RemoteException, WTPropertyVetoException, ParseException {
        QueryResult qr = getParts(part.getNumber(), null, null, null);
        while (qr.hasMoreElements()) {
            part = (WTPart) qr.nextElement();
            ;
            String oldPartType = ClientTypedUtility.getExternalTypeIdentifier(part)
                    .replace("WCTYPE|", "");
            if (!oldPartType.equals(newType)) {
                // 更改部件类别key
                TypeIdentifier typeIdentifier = FdnWTContainerHelper.toTypeIdentifier(newType);
                TypeHelper.setType(part, typeIdentifier);
                PersistenceServerHelper.manager.update(part);
            }
        }
    }

 

    /**
     * 清空Part关联的EPMDocument Link关系
     * 
     * @param part
     * @throws Exception
     */
    public static void clearPartEPMLinks(WTPart part) throws Exception {
        if (part != null) {
            QueryResult qr = PersistenceHelper.manager.navigate(part,
                    EPMBuildRule.BUILD_SOURCE_ROLE, EPMBuildRule.class, false);
            while (qr.hasMoreElements()) {
                EPMBuildRule link = (EPMBuildRule) qr.nextElement();
                PersistenceServerHelper.manager.remove(link);
            }
            qr = PersistenceHelper.manager.navigate(part, EPMBuildHistory.BUILT_BY_ROLE,
                    EPMBuildHistory.class, false);
            while (qr.hasMoreElements()) {
                EPMBuildHistory link = (EPMBuildHistory) qr.nextElement();
                PersistenceServerHelper.manager.remove(link);
            }
            qr = PersistenceHelper.manager.navigate(part, EPMDescribeLink.DESCRIBED_BY_ROLE,
                    EPMDescribeLink.class, false);
            while (qr.hasMoreElements()) {
                EPMDescribeLink link = (EPMDescribeLink) qr.nextElement();
                PersistenceServerHelper.manager.remove(link);
            }
        }
    }

    public static void clearPartUsageLink(WTPart part) throws WTException {
        if (part != null) {
            QueryResult qr = PersistenceHelper.manager.navigate(part, WTPartUsageLink.USES_ROLE,
                    WTPartUsageLink.class, false);
            while (qr.hasMoreElements()) {
                WTPartUsageLink link = (WTPartUsageLink) qr.nextElement();
                PersistenceServerHelper.manager.remove(link);
            }
        }
    }

    /**
     * 取指定视图,指定生命周期状态的最新版部件
     * @param partNumber
     * @return
     * @throws Exception 
     */
    public static WTPart getLatestStateViewPart(String partNumber, String viewName, String state)
            throws Exception {
        SearchUtil su = new SearchUtil(WTPart.class);
        su.setConfigSpec(PartUtil.getViewConfigSpec(viewName));
        su.setNumber(partNumber);
        su.setState(state);
        QueryResult qr = su.queryObjects();
        qr = latestConfigSpec.process(qr);
        if (qr.hasMoreElements()) {
            return (WTPart) qr.nextElement();
        }
        return null;
    }
    
    

	/**
	 * 创建部件
	 * @param number 编号
	 * @param name   名称
	 * @param containerName 容器名称
	 * @param viewName 视图名称
	 * @param isEndItem 是否为成品
	 * @param folderPath 存储的文件夹路径
	 * @return
	 * @throws WTException 
	 * @throws WTPropertyVetoException 
	 */
	public static WTPart createPart(
			String number,
			String name,
			String containerName,
			String viewName,
			String folderPath) throws Exception{
		//创建WTPart(部件)对象
		WTPart part = WTPart.newWTPart();
		//设置部件类型
		part.setTypeDefinitionReference(getTypeDefinitionReference("wt.part.WTPart"));
		//设置名称
		part.setName(name.trim());
		//设置编号
		part.setNumber(number.trim().toUpperCase());
		//设置部件来源
		part.setSource(Source.getSourceDefault());
		//设置默认单位
		part.setDefaultUnit(QuantityUnit.getQuantityUnitDefault());
		//设置视图
		ViewReference vr = getPartViewReference("Design");
		part.setView(vr);
		//设置是否为成品
		part.setEndItem(false);
		//设置存储容器
		WTContainer container =  WTUtil.getContainerByName(containerName);
		WTContainerRef cref = WTContainerRef.newWTContainerRef(container);
		part.setContainerReference(cref);
		/*
		folderPath = folderPath == null ? "" : folderPath.trim();
		if (folderPath.length() > 0) {
			Folder folder = FolderHelper.service.getFolder(folderPath, cref);
			part.setContainerReference(cref);
			FolderHelper.assignLocation(part, folder);
		}*/
		Folder folder = FolderUtil.getFolderByPath(folderPath, containerName);
        if(folder == null){
        	folder = FolderUtil.createSubFolder(folderPath, containerName);
        }
        FolderHelper.assignLocation(part, folder);
		
		WTContainer cont = cref.getReferencedContainerReadOnly();
		part.setDomainRef(cont.getDefaultDomainReference());
		//持久化到数据库
		part = (WTPart) PersistenceHelper.manager.save(part);
		part = (WTPart) PersistenceHelper.manager.refresh(part);
		return part;
	}
	
	
	public static TypeDefinitionReference getTypeDefinitionReference(
			String fullType) {
		TypeIdentifier typeidentifier = CoreMetaUtility
				.getTypeIdentifier(fullType);
		WCTypeIdentifier wctypeidentifier = (WCTypeIdentifier) typeidentifier;
		TypeDefinitionReference tdr = TypedUtility
				.getTypeDefinitionReference(wctypeidentifier.getTypename());
		return tdr;
	}
	
	
	
	/**
	 * 获取视图对象
	 */
	public static ViewReference getPartViewReference(String viewStr) {
		ViewReference vrs = null;
		try {
			View aview = ViewHelper.service.getView(viewStr);
			vrs = ViewReference.newViewReference(aview);
		} catch (WTException ex) {
			ex.printStackTrace();
		}
		return vrs;
	}
	
	
	
	/**
	 * 给部件设置参考文档
	 * 
	 * @param curr_prt
	 * @param wtdoc
	 */
	public static void setPartReference(WTPart part, WTDocument wtdoc) {
		try {
			WTDocumentMaster docMaster = (WTDocumentMaster) wtdoc.getMaster();
			WTPartReferenceLink linkObj = WTPartReferenceLink
					.newWTPartReferenceLink(part, docMaster);
			PersistenceServerHelper.manager.insert(linkObj);
		} catch (WTException wte) {
			wte.printStackTrace();
		}
	}
	
	
	
	/**
	 * 给部件设置说明
	 * 
	 * @param curr_prt
	 * @param wtdoc
	 */
	public static void setPartDescribe(WTPart part, WTDocument wtdoc) {
		try {
			WTPartDescribeLink linkObj = WTPartDescribeLink
					.newWTPartDescribeLink(part, wtdoc);
			PersistenceServerHelper.manager.insert(linkObj);
		} catch (WTException wte) {
			wte.printStackTrace();
		}
	}
	
	
	/**
	 * @Description: 获取部件与文档的参考关系
	 * @param part
	 * @param doc
	 * @return
	 * @throws WTException
	 */
	public static WTPartReferenceLink getWTPartReferenceLink(WTPart part,WTDocument doc) throws WTException{
    	QueryResult qr = PersistenceHelper.manager.find(WTPartReferenceLink.class,part, 
    			WTPartReferenceLink.ROLE_AOBJECT_ROLE, doc.getMaster());
    	if(qr.hasMoreElements()){
    		return (WTPartReferenceLink) qr.nextElement();
    	}
    	return null;
    }
	
	
	
	/**
	 * @Description: 获取部件参考文档
	 * @param part
	 * @param doc
	 * @return
	 * @throws WTException
	 */
	public static List<WTDocument> getWTPartReferenceDoc(WTPart part) throws WTException{
    	List<WTDocument> list = new ArrayList<WTDocument>();
		QueryResult qr = PersistenceHelper.manager.navigate(part,WTPartReferenceLink.ROLE_AOBJECT_ROLE,WTPartReferenceLink.class);
    	while(qr.hasMoreElements()){
    		list.add((WTDocument)qr.nextElement());
    	}
    	return list;
    }
	
	
	/**
	 * @Description: 获取部件与文档的说明关系
	 * @param part
	 * @param doc
	 * @return
	 * @throws WTException
	 */
	public static WTPartDescribeLink getWTPartDescribeLink(WTPart part,WTDocument doc) throws WTException{
    	QueryResult qr = PersistenceHelper.manager.find(WTPartDescribeLink.class,part, 
    			WTPartDescribeLink.ROLE_AOBJECT_ROLE, doc);
    	if(qr.hasMoreElements()){
    		return (WTPartDescribeLink) qr.nextElement();
    	}
    	return null;
    }
	
	
	/**
	 * @Description: 获取部件说明文档
	 * @param part
	 * @param doc
	 * @return
	 * @throws WTException
	 */
	public static List<WTDocument> getWTPartDescribeDoc(WTPart part) throws WTException{
    	List<WTDocument> list = new ArrayList<WTDocument>();
        QueryResult qr = PersistenceHelper.manager.navigate(part, 
        													WTPartDescribeLink.DESCRIBED_BY_ROLE,
        												    WTPartDescribeLink.class, false);
            while (qr.hasMoreElements()) {
                WTPartDescribeLink link = (WTPartDescribeLink) qr.nextElement();
                Iterated doc = link.getDescribedBy();
                if(doc instanceof WTDocument)
                {
                     WTDocument theDocument = (WTDocument)doc;
                     list.add(theDocument);
                }
            }
    	return list;
    }
	
	
	
	
	
	
	
}


