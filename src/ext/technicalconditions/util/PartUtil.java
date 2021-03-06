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
 * V20200715,??????????????????????????????
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
     * ??????????????????
     */
    public static ConfigSpec DesignConfigSpec;
    /**
     * ?????????????????????
     */
    public static ConfigSpec latestConfigSpec;

    static {
        try {
            /**???????????????????????????**/
            View DesignView = ViewHelper.service.getView("Design");
            DesignConfigSpec = WTPartStandardConfigSpec.newWTPartStandardConfigSpec(DesignView,null);
            latestConfigSpec = new LatestConfigSpec();
        } catch (WTException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * ???????????????????????????
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
     * ???????????????????????????????????????,????????????????????????????????????????????????
     * ????????????BOM 
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
                //??????BOM??????
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
                        logger.debug("????????????????????????,??????????????????,????????????:" + part2.getDisplayIdentity());
                        result.add(part2);
                    }
                    logger.debug("????????????????????????,????????????:" + part.getDisplayIdentity());
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
     * ?????????????????????link??????????????????
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
            logger.debug("???:" + ((WTPart) link.getRoleAObject()).getDisplayIdentity());
            logger.debug("???:" + ((WTPartMaster) link.getRoleBObject()).getNumber());
            WTCollection collection = WTPartHelper.service.getSubstituteLinks(link);
            for (Object object : collection) {
                if (object instanceof ObjectReference) {
                    ObjectReference objref = (ObjectReference) object;
                    Object obj = objref.getObject();
                    if (obj instanceof WTPartSubstituteLink) {
                        WTPartSubstituteLink substituteLink = (WTPartSubstituteLink) obj;
                        //?????????????????????????????????????????????link
                        //WTPartUsageLink      currentLink    = (WTPartUsageLink) substituteLink.getRoleAObject();
                        WTPartMaster partMaster = (WTPartMaster) substituteLink.getRoleBObject();
                        logger.debug("????????????:" + partMaster.getNumber());
                        list.add(partMaster);
                    }
                }
            }
        }
        return list;
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????Design????????????
     * 11:06:22 AM
     * @param partNumber ????????????,??????
     * @param partName   ????????????,??????
     * @param view	     ????????????????????????null?????????Design????????????
     * @param fullType   ?????????????????????????????????,?????????null,?????????wt.part.WTPart??????
     * @param containerName       ???????????????,??????
     * @param state       ????????????????????????
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
     * ??????????????????
     * @description
     * @param fullType    ??????????????????:wt.part.WTPart|com.wisplm.StandardPart
     * @param defaultType ??????fullType???????????????,?????????????????????
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
     * ??????????????????????????????????????????????????????????????????????????????
     * 
     * @param partNumber ????????????
     * @param viewName   ????????????
     * @param states     ????????????????????????
     * @param accessControlled ????????????
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
     * ????????????Master,?????????????????????????????????????????????
     *
     * @param master   ??????master
     * @param viewName ??????????????? Design,Planning,Manufacturing
     * @return ?????????????????????????????????Part
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
     * ????????????master,????????????????????????
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
     * ??????????????????????????????
     * @param number ??????????????????,?????????????????????;??????,????????????*???????????????
     * @return 
     * @throws WTException 
     * 2011-11-24 ??????07:54:20
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
     * ?????????????????????,????????????????????????
     * @param parent ??????
     * @param child  ??????
     * @throws WTException 
     */
    public static void createUsageLink(WTPart parent, WTPart child) throws WTException {
        if(getPartUsageLink(parent,(WTPartMaster)child.getMaster()) == null){
        	WTPartUsageLink partLink = WTPartUsageLink.newWTPartUsageLink(parent,(WTPartMaster) child.getMaster());
            PersistenceServerHelper.manager.insert(partLink);
        }
    }

    /**
     * ???????????????????????????usagelink??????
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
     * ?????????????????????????????????????????????-???????????????
     * ??????11:50:06
     * @param doc
     * @return
     * @throws WTException
     */
    public static List getDesParts(WTDocument doc) throws WTException {
        return PartDocServiceCommand.getAssociatedDescParts(doc).getObjectVectorIfc().getVector();
    }

    /**
     * ?????????????????????????????????????????????
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
     * ???????????????oid?????????????????????????????????????????????
     * 
     * @param oid
     *            ???:OR:wt.part.WTPart:11111
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
     * ??????????????????checkoutPart -- checkinPart??????????????????
     * ???????????????????????????????????????????????????????????????????????????
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
     * ??????????????????checkoutPart -- checkinPart
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
                    // ??????????????????
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
     * ???????????????????????????????????????????????????????????????CheckOutObject-checkinObject?????????,??????
     * ???????????????????????????????????????????????????????????????????????????
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
                    // ??????????????????
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
     * ?????????????????????
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
     * ?????????????????????BOM????????????
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
            	//???????????????????????????
            	childPart = getLatestPartByViewName(childNumber,viewName);
            }
            if(childPart == null){
            	continue;
            }*/
        }
        return result;
    }

    /**
     * ??????????????????????????????,?????????????????????1(??????)
     * @param baseline ??????
     * @param childPart ??????
     * @throws WTException ??????03:07:14
     */
    public static WTPartUsageLink getAmountByChildAndBaseLine(ManagedBaseline baseline,
            WTPart childPart) throws WTException {
        WTPartUsageLink uses[] = wt.clients.prodmgmt.PartHelper.getUsedBy(childPart);
        for (int i = 0; i < uses.length; i++) {
            Iterated it = uses[i].getUsedBy();
            String parentNumber = ((WTPart) it).getNumber();
            System.out.println("??????????????????????????????" + ((WTPart) it).getDisplayIdentifier().toString());
            //??????????????????????????????,????????????????????????
            if (baseLineContainsPart(baseline, parentNumber)) {
                Quantity q = uses[i].getQuantity();
                Double amount = q.getAmount();
                String unit = q.getUnit().getDisplay(Locale.SIMPLIFIED_CHINESE);
                System.out.println("?????????" + amount + "   ??????" + unit);
                return uses[i];
            }
        }
        return null;
    }

    /**
     * ????????????????????????????????????????????????
     * @param baseline
     * @param partNumber
     * @return
     * @throws WTException ??????03:12:12
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
     * ???????????????????????????"Design"???????????????
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
     * ???????????????design??????????????????
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
     * ?????????????????????
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
     * ???????????????????????????
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
     * ????????????????????????????????????
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
     * ???????????????????????????
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
     * ???????????????????????????,???????????????????????????????????????????????????,??????????????????????????????????????????
     * ???????????????,?????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ??????11:14:21
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
     * ???????????????????????????????????????
     * ??????03:56:59
     * @param partOid 
     * @param docType ?????????????????????????????????WTDocument,XieTiaoDan????????????????????????????????????""??????null???????????????????????????
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
                SessionHelper.manager.setAdministrator(); // ????????????????????????
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
     * ????????????????????????????????????
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
                        logger.debug("??????????????????-----" + doc.getDisplayIdentity());
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
     * ?????????????????????????????????????????????,??????????????????
     * 4:50:42 PM
     * @param containerOid null?????????????????????
     * @param viewName null?????????????????????
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
     * ??????????????????Oid
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
     * ?????????????????????EpmDocument?????????2D???3D
     */
    public static Vector<EPMDocument> getAssEpmDocument(WTPart part) throws WTException {
        logger.debug("?????????????????????EPM??????:" + part.getDisplayIdentity());
        Vector<EPMDocument> vec = new Vector<EPMDocument>();
        Collection coll = PartDocServiceCommand.getAssociatedCADDocumentsAndLinks(part);
        Object[] obj = coll.toArray();
        for (Object tem : obj) {
            AssociationLinkObject link = (AssociationLinkObject) tem;
            logger.debug("??????EPM??????:" + link.getCadObject().getDisplayIdentity());
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
     * ??????????????????????????????????????????,??????????????????
     * @description
     * @param iterated
     * @param iteration
     * @throws VersionControlException
     * @throws WTPropertyVetoException
     * @throws WTException
     */
    public static void newIteration(Iterated iterated, String iteration) throws VersionControlException, WTPropertyVetoException, WTException{
    	//?????????????????????
    	iterated = (Iterated) VersionControlHelper.service.newIteration(iterated);
    	if(!(iterated.getIterationInfo().getIdentifier().getValue()+"").equals(iteration)){
        	//???????????????????????????
        	changeIteration(iterated,iteration);
    	}
    }
    
    /**
     * ?????????????????????,?????????A.2??????A.3,??????A.2??????
     * @param iterated
     * @param s ????????????,???1,2???
     * 2011-8-24 ??????05:17:10
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
     * ????????????
     * ???????????????????????????????????????
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
     * ??????????????????:??????????????????????????????,??????????????????????????????????????????
     * @param part
     * @param version ?????????,????????????????????????
     * @param revision ?????????
     * @return
     * @throws VersionControlException
     * @throws WTPropertyVetoException
     * @throws WTException 
     * 2011-8-25 ??????02:52:20
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
     * ??????????????????????????????
     * @param part
     * @throws WorkInProgressException
     * @throws WTPropertyVetoException
     * @throws PersistenceException
     * @throws WTException 
     * 2011-8-24 ??????05:19:38
     */
    public static WTPart newIteration(WTPart part) throws WorkInProgressException,
            WTPropertyVetoException, PersistenceException, WTException {
        if (!(WorkInProgressHelper.isCheckedOut(part))) {
            part = (WTPart) WorkableUtil.doCheckOut(part,"??????");
        }
        if (WorkInProgressHelper.isCheckedOut(part))
            part = (WTPart) WorkInProgressHelper.service.checkin(part, "??????");

        return part;
    }


    /**
     * ???????????????????????????
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
        if (wtpart != null) { // ???P?????????????????????M??????
            wtpart = (WTPart) LifeCycleServerHelper.service.setState((LifeCycleManaged) wtpart,
                    State.toState(state));
            newPart = (WTPart) ViewHelper.service.newBranchForView(wtpart, viewname);
            newPart = (WTPart) PersistenceHelper.manager.store(newPart);
        }
        return newPart;
    }
    
    /**
     * ????????????????????????
     * @description
     * @param wtpart   ????????????????????????
     * @param state    ????????????????????????????????????
     * @param viewname ????????????
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
            //?????????????????????CATDrawing ???CAD??????
            //	            if("CATDrawing".equals(epm.getDocType().toString())){
            //	            	vResult.add(epm);
            //	            }
        }
        return vResult;
    }

    /**
    * ???????????????????????????
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
     * ???????????????????????????????????????BOM????????????
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
     * ?????????????????????????????????BOM????????????(????????????????????????)
     * @description
     * @param parentPart ???????????????
     * @param config     ????????????
     * @param currentLevel ?????????????????????
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
            //??????
            map.put("level", (currentLevel + 1) + "");
            map.put("link", link);
            //????????????
            map.put("part", childPart);
            map.put("parent", parentPart);
            //????????????
            map.put("amount", link.getQuantity().getAmount() + "");
            //??????????????????
            map.put("typeDisplay", WTUtil.getObjectTypeDisplay(childPart));
            result.add(map);
            logger.debug("??????:" + parentPart.getDisplayIdentity() + "??????:" + map);
            result.addAll(getAllSubPartsAndLevel(childPart, config, currentLevel + 1));
        }
        return result;
    }

    /**
     * ??????????????????????????????????????????link
     * ????????????????????????????????????,????????????????????????????????????????????????(OOTB??????:????????????????????????,??????????????????)
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
     * ??????????????????????????????????????????link
     * ?????????????????????????????????,????????????????????????????????????????????????(OOTB??????:????????????????????????,??????????????????)
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
     * ?????????????????????
     * @param parentPart
     * @return
     * @throws WTException
     */
    public static QueryResult getLatestSubParts(WTPart parentPart) throws WTException {
    	return getSubParts(parentPart,latestConfigSpec);
    }
    
    /**
     * ??????????????????????????????
     * ?????????????????????????????????,????????????????????????????????????????????????(OOTB??????:????????????????????????,??????????????????)
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
     * ?????????????????????????????????????????????????????????
     * @description
     * @param parentPart ??????
     * @param viewName   ??????,??????????????????????????????
     * @param state      ??????????????????
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
     * ??????????????????????????????,????????????????????????
     * @description
     * @author     
     * @param allParts ????????????????????????,???????????????null??????
     * @param parentPart ???????????????(??????)
     * @param configSpec ????????????(??????????????????)
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
            logger.debug("??????EBOM??????,?????????:" + parentPart.getDisplayIdentity());
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
     * ??????????????????????????????,????????????????????????
     * @description
     * @param allParts   ????????????????????????,???????????????null??????
     * @param parentPart ???????????????(??????)
     * @param viewName   ??????????????????
     * @param state      ??????????????????????????????
     * @return
     * @throws Exception
     */
    public static Set<WTPart> getBOMParts(Set<WTPart> allParts, WTPart parentPart, String viewName,
            String state) throws Exception {
        boolean accessEnforced = false;
        try {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            logger.debug("??????EBOM??????,?????????:" + parentPart.getDisplayIdentity());
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
     * ????????????????????????
     * @param viewName
     * @return
     * @throws WTException
     * @throws WTPropertyVetoException 
     * 2011-9-26 ??????09:28:00
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
     * ?????????????????????????????????,??????A??????,201111120
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
     * ????????????????????????????????????????????????
     */
    public static QueryResult getAllVersionLatestPart(WTPartMaster wtpartmaster)
            throws WTException {
        QueryResult qr = VersionControlHelper.service.allVersionsOf(wtpartmaster);
        qr = new MultipleLatestConfigSpec().process(qr);
        return qr;
    }

    /**
     * ??????????????????????????????????????????????????????
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
     * ??????????????????,????????????????????????????????????
     * @param number
     * @param version
     * @return
     * @throws WTException 
     * 2011-11-24 ??????08:08:26
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
     * ????????????????????????????????????????????????
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
        //??????????????????????????????0
        if (amount == 0)
            amount = 1;
        return String.valueOf(amount);
    }
    
    /**
     * ?????????????????????????????????link
     * @description
     * @author      ZhongBinpeng
     * @param partentPart ??????
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
     * ???????????????????????????????????????????????????
     * @param strNumber
     * @param strVersion
     * @param strIterationId
     * @param strView
     * @return
     * @throws WTException 
     * 2011-11-14 ??????11:30:52
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
            //???????????????????????????????????????
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
     * ???????????????????????????????????????
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
        System.out.println("???????????????EPM,??????:" + part.getName() + ",ida2a2:"
                + part.getPersistInfo().getObjectIdentifier().getId());
        return "";
    }

    /**
     * ??????epm???????????????????????????
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
        System.out.println("EPMDocument????????????,??????:" + epm.getName());
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
     * ???????????????????????????EPM????????????
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

            // ????????????(IteratedDescribeLink)
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
     * ???Part?????????????????????????????????
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
     * ??????????????????????????????
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
                // ??????????????????key
                TypeIdentifier typeIdentifier = FdnWTContainerHelper.toTypeIdentifier(newType);
                TypeHelper.setType(part, typeIdentifier);
                PersistenceServerHelper.manager.update(part);
            }
        }
    }

 

    /**
     * ??????Part?????????EPMDocument Link??????
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
     * ???????????????,??????????????????????????????????????????
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
	 * ????????????
	 * @param number ??????
	 * @param name   ??????
	 * @param containerName ????????????
	 * @param viewName ????????????
	 * @param isEndItem ???????????????
	 * @param folderPath ????????????????????????
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
		//??????WTPart(??????)??????
		WTPart part = WTPart.newWTPart();
		//??????????????????
		part.setTypeDefinitionReference(getTypeDefinitionReference("wt.part.WTPart"));
		//????????????
		part.setName(name.trim());
		//????????????
		part.setNumber(number.trim().toUpperCase());
		//??????????????????
		part.setSource(Source.getSourceDefault());
		//??????????????????
		part.setDefaultUnit(QuantityUnit.getQuantityUnitDefault());
		//????????????
		ViewReference vr = getPartViewReference("Design");
		part.setView(vr);
		//?????????????????????
		part.setEndItem(false);
		//??????????????????
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
		//?????????????????????
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
	 * ??????????????????
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
	 * ???????????????????????????
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
	 * ?????????????????????
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
	 * @Description: ????????????????????????????????????
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
	 * @Description: ????????????????????????
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
	 * @Description: ????????????????????????????????????
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
	 * @Description: ????????????????????????
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


