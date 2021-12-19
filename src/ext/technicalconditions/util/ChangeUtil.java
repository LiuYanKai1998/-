package ext.technicalconditions.util;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.windchill.enterprise.copy.server.CoreMetaUtility;

import wt.change2.AffectedActivityData;
import wt.change2.Category;
import wt.change2.ChangeException2;
import wt.change2.ChangeHelper2;
import wt.change2.ChangeItemIfc;
import wt.change2.ChangeNoticeComplexity;
import wt.change2.ChangeRecord2;
import wt.change2.Changeable2;
import wt.change2.Complexity;
import wt.change2.IssuePriority;
import wt.change2.RelevantRequestData2;
import wt.change2.ReportedAgainst;
import wt.change2.RequestPriority;
import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeIssue;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeRequest2;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentHolder;
import wt.content.ContentRoleType;
import wt.content.ContentServerHelper;
import wt.fc.ObjectNoLongerExistsException;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.collections.WTValuedHashMap;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.inf.container.WTContainerRef;
import wt.log4j.LogR;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.pds.StatementSpec;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.rule.init.InitRuleHelper;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.type.TypeDefinitionReference;
import wt.type.TypedUtility;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;


/**
 * 变更相关对象操作
 * @description
 * @author      
 * @date        
 */
public class ChangeUtil implements RemoteAccess, Serializable {
	private static final String CLASSNAME = ChangeUtil.class.getName();
    private static final Logger logger = LogR.getLogger(ChangeUtil.class.getName());
    public static final String CATEGORY = "CATEGORY"; // category
    public static final String PRIORITY = "PRIORITY"; // priority
    public static final String DESCRIPTION = "DESCRIPTION"; // description
    public static final String TYPE = "TYPE"; // type
    public static final String FOLDER = "FOLDER"; // folder
    public static final String NEED_DATE = "NEEDDATE"; // need date
    public static final String RESOLUTION_DATE = "RESOLUTION_DATE"; // solution data
    public static final String RECURRING_COST_EST = "RECURRING_COST_EST"; // recurring cost
    public static final String NON_RECURRING_COST_EST = "NON_RECURRING_COST_EST"; // no recurring cost
    public static final String COMPLEXITY = "COMPLEXITY"; // complexity
    public static final String FOLDER_DEFAULT = "/Default"; // default folder path
    public static final String TIME_STAMP_FORMAT = "yyyy-MM-dd"; // default time format

    private static final long serialVersionUID = 5091068291906960116L;
    
    /**
     * @description 通过编号查找变更通告
     * @param number 编号,精确查找
     * @param accessControlled 是否开启权限过滤,true开启,false不开启
     * @return
     * @throws WTException
     * @throws InvocationTargetException 
     * @throws RemoteException 
     */
    public static WTChangeOrder2 getChangeNoticeByNumber(String number, boolean accessControlled) throws WTException, RemoteException, InvocationTargetException {
        number = number.toUpperCase();
        WTChangeOrder2 ecn = null;
        if (!RemoteMethodServer.ServerFlag) {
            return (WTChangeOrder2) RemoteMethodServer.getDefault().invoke("getChangeNoticeByNumber",
                    CLASSNAME, null, new Class[] { String.class, boolean.class },
                    new Object[] { number, accessControlled });
        } else {
            boolean enforce = SessionServerHelper.manager.setAccessEnforced(accessControlled);
            try {
                QuerySpec spec = new QuerySpec(WTChangeOrder2.class);
                spec.appendWhere(new SearchCondition(WTChangeOrder2.class, WTChangeRequest2.NUMBER,
                        SearchCondition.EQUAL, number), new int[] { 0 });

                QueryResult qur = PersistenceHelper.manager.find((StatementSpec) spec);
                if (qur.hasMoreElements()) {
                    ecn = (WTChangeOrder2) qur.nextElement();
                }
            } finally {
                SessionServerHelper.manager.setAccessEnforced(enforce);
            }
        }
        return ecn;
    }

    /**
     * 
     * Create change Issue object.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-15, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param number
     *            -- String : change Issue number
     * @param name
     *            -- String : change Issue name
     * @param requester
     *            -- String : requester
     * @param attributes
     *            -- Map : contain attributes
     * @param secondaryContents
     *            -- ArrayList : secondary list (String FilePath or InputStream)
     * @param containerRef
     *            -- WTContainerRef : object container
     * @return WTChangeIssue : If the specified Numbers changeIssue object exist, then back to the exist
     *         object;otherwise, return create object
     * @throws IOException
     *             : stream exception
     * @throws PropertyVetoException
     *             : a proposed change to a property represents an unacceptable value exception
     * @throws WTException
     *             : windchill exception
     * @throws FileNotFoundException
     *             : file exception
     * @throws InvocationTargetException 
     * 
     * 
     */
    @SuppressWarnings("deprecation")
    public static WTChangeIssue createProblemReport(String number, String name, String requester,
            Map<String, String> attributes, List<?> secondaryContents, WTContainerRef containerRef)
            throws FileNotFoundException, WTException, PropertyVetoException, IOException, InvocationTargetException {
        WTChangeIssue chi = null;
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeIssue) RemoteMethodServer.getDefault().invoke(
                        "createProblemReport",
                        CLASSNAME,
                        null,
                        new Class[] { String.class, String.class, String.class, Map.class, List.class,
                                WTContainerRef.class },
                        new Object[] { number, name, requester, attributes, secondaryContents, containerRef });
            }
            String prDesc = "";
            String prType = "";
            String prFolder = "";
            String prCategory = "";
            String prProirity = "";

            if (attributes != null) {
                prDesc = attributes.get(ChangeUtil.DESCRIPTION);
                prType = attributes.get(ChangeUtil.TYPE);
                prFolder = attributes.get(ChangeUtil.FOLDER);
                prCategory = attributes.get(ChangeUtil.CATEGORY);
                prProirity = attributes.get(ChangeUtil.PRIORITY);
            }

            if (containerRef == null) {
                return null;
            }

            // set default number（Default : WTCHANGEISSUEID_SEQ）
            if (number == null || number.equalsIgnoreCase("")) {
                number = ChangeUtil.getDefaultChangeSeqNumber(WTChangeIssue.class);
            } else {
                WTChangeIssue existPR = ChangeUtil.getProblemReportByNumber(number, false);
                if (existPR != null) {
                  throw new WTException("问题报告已存在,编号:" + number);
                }
            }

            if (name == null || name.equalsIgnoreCase("")) {
            	name = number;
            }

            if (requester == null) {
            	requester = SessionHelper.manager.getPrincipal().getName();
            }

            if (prDesc == null) {
                prDesc = "";
            }

            // set default type (Default : wt.change2.WTChangeIssue)
            if (prType == null || prType.equalsIgnoreCase("")) {
                prType = "wt.change2.WTChangeIssue";
            }

            // set default folder (Default : /Default)
            if (prFolder == null || prFolder.equalsIgnoreCase("")) {
                prFolder = ChangeUtil.FOLDER_DEFAULT;
            } else {
                if (!prFolder.startsWith(ChangeUtil.FOLDER_DEFAULT)) {
                    prFolder = ChangeUtil.FOLDER_DEFAULT + "/" + prFolder;
                }
            }

            chi = WTChangeIssue.newWTChangeIssue();
            chi.setNumber(number);
            chi.setName(name);
            chi.setRequester(requester);

            // set description
            if (!"".equals(prDesc)) {
                chi.setDescription(prDesc);
            }
            // set type
            if (prType != null) {
                TypeIdentifier tid = TypeIdentifierHelper.getTypeIdentifier(prType);
                chi = (WTChangeIssue) CoreMetaUtility.setType(chi, tid);
            }

            // set context
            chi.setContainerReference(containerRef);

            // set folder
            Folder location = null;
            try {
                location = FolderHelper.service.getFolder(prFolder, containerRef);
            } catch (Exception e) {
                location = null;
            }
            if (location == null) {
                location = FolderHelper.service.saveFolderPath(prFolder, containerRef);
            }
            if (location != null) {
                WTValuedHashMap map = new WTValuedHashMap();
                map.put(chi, location);
                FolderHelper.assignLocations(map);
            }

            // set category
            Category category = null;
            try {
                category = Category.toCategory(prCategory);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.error(e.getMessage(), e);
                }
                category = Category.getCategoryDefault();
            }
            chi.setCategory(category);

            // set priority
            IssuePriority priority = null;
            try {
                priority = IssuePriority.toIssuePriority(prProirity);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                priority = IssuePriority.getIssuePriorityDefault();
            }
            chi.setIssuePriority(priority);

            chi = (WTChangeIssue) PersistenceHelper.manager.save(chi);
            chi = (WTChangeIssue) PersistenceHelper.manager.refresh(chi);

            // set secondaryContent
            if (secondaryContents != null) {
                for (int i = 0; i < secondaryContents.size(); i++) {
                    Object secondaryContent = secondaryContents.get(i);

                    ApplicationData applicationdata = ApplicationData.newApplicationData(chi);
                    applicationdata.setRole(ContentRoleType.SECONDARY);
                    if (secondaryContent instanceof String) {
                        String filePath = (String) secondaryContent;
                        applicationdata = ContentServerHelper.service.updateContent(chi, applicationdata, filePath);
                    } else if (secondaryContent instanceof InputStream) {
                        InputStream ins = (InputStream) secondaryContent;
                        applicationdata = ContentServerHelper.service.updateContent(chi, applicationdata, ins);
                    }
                }
            }
            chi = (WTChangeIssue) PersistenceServerHelper.manager.restore(chi);
        
        return chi;
    }

    /**
     * Get changeIssue by number
     * <br>
     * @param number
     *            : String number
     * @param accessControlled
     *            -- boolean : true have access controlled, false no access controlled
     * @return WTChangeIssue : changeIssue object
     * @throws WTException
     */
    @SuppressWarnings("deprecation")
    public static WTChangeIssue getProblemReportByNumber(String number, boolean accessControlled) throws WTException {
        WTChangeIssue chi = null;
        try {
            number = number.toUpperCase();
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeIssue) RemoteMethodServer.getDefault().invoke("getProblemReportByNumber",
                        CLASSNAME, null, new Class[] { String.class, boolean.class },
                        new Object[] { number, accessControlled });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(accessControlled);
                try {
                    QuerySpec spec = new QuerySpec(WTChangeIssue.class);
                    spec.appendWhere(new SearchCondition(WTChangeIssue.class, WTChangeIssue.NUMBER,
                            SearchCondition.EQUAL, number), new int[] { 0 });
                    QueryResult qur = PersistenceHelper.manager.find(spec);
                    if (qur.hasMoreElements()) {
                        chi = (WTChangeIssue) qur.nextElement();
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return chi;
    }

    /**
     * 
     * Get changeable2 collection by WTChangeIssue object.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chi
     *            -- WTChangeIssue : changeIssue object
     * @return List : changeable2 collection
     * @throws WTException
     *             : windchill exception
     * @throws ChangeException2
     *             : change exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<Changeable2> getProblemReportItems(WTChangeIssue chi) throws ChangeException2, WTException {
        List<Changeable2> results = new ArrayList<Changeable2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<Changeable2>) RemoteMethodServer.getDefault().invoke("getProblemReportItems",
                        CLASSNAME, null, new Class[] { WTChangeIssue.class }, new Object[] { chi });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeables(chi);
                    while (qur.hasMoreElements()) {
                        Object obj = qur.nextElement();
                        if (obj instanceof Changeable2) {
                            Changeable2 changeable = (Changeable2) obj;
                            if (!results.contains(changeable)) {
                                results.add(changeable);
                            }
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Get problem report by changeable object.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-27, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param changeable
     *            : Changeable2 object
     * @return List : search problem Report
     * @throws WTException
     *             : windchill exception
     * @throws ChangeException2
     *             : change exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<WTChangeIssue> getProblemReportByChangeable(Changeable2 changeable) throws ChangeException2,
            WTException {
        List<WTChangeIssue> results = new ArrayList<WTChangeIssue>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<WTChangeIssue>) RemoteMethodServer.getDefault()
                        .invoke("getProblemReportByChangeable", CLASSNAME, null,
                                new Class[] { Changeable2.class }, new Object[] { changeable });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getReportedAgainstChangeIssue(changeable);
                    while (qur.hasMoreElements()) {
                        WTChangeIssue chi = (WTChangeIssue) qur.nextElement();
                        if (!results.contains(chi)) {
                            results.add(chi);
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Get changeRequest by changeIssue.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chi
     *            : WTChangeIssue object
     * @return List : changeRequest collection
     * @throws WTException
     *             : windchill exception
     * @throws ChangeException2
     *             : change exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<WTChangeRequest2> getChangeRequestByProblemReport(WTChangeIssue chi) throws ChangeException2,
            WTException {
        List<WTChangeRequest2> results = new ArrayList<WTChangeRequest2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<WTChangeRequest2>) RemoteMethodServer.getDefault().invoke(
                        "getChangeRequestByProblemReport", CLASSNAME, null,
                        new Class[] { WTChangeIssue.class }, new Object[] { chi });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeRequest(chi);
                    while (qur.hasMoreElements()) {
                        WTChangeRequest2 chr = (WTChangeRequest2) qur.nextElement();
                        if (!results.contains(chr)) {
                            results.add(chr);
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Add changeable to WTChangeIssue.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chi
     *            : WTChangeIssue object
     * @param changeable
     *            : need add Changeable2 object
     * @return WTChangeIssue : update changeIssue object
     * @throws WTException
     *             : windchill exception
     * @throws ObjectNoLongerExistsException
     *             : object no longer exists exception
     * 
     * 
     */
    public static WTChangeIssue addChangeableToProblemReport(WTChangeIssue chi, Changeable2 changeable)
            throws ObjectNoLongerExistsException, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeIssue) RemoteMethodServer.getDefault().invoke("addChangeableToProblemReport",
                        CLASSNAME, null, new Class[] { WTChangeIssue.class, Changeable2.class },
                        new Object[] { chi, changeable });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    Vector<Changeable2> changeables = new Vector<Changeable2>();
                    changeables.add(changeable);
                    ChangeHelper2.service.storeAssociations(ReportedAgainst.class, chi, changeables);
                    chi = (WTChangeIssue) PersistenceHelper.manager.refresh(chi);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return chi;
    }

    /**
     * 
     * Add changeRequest to WTChangeIssue.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chi
     *            : WTChangeIssue object
     * @param chr
     *            : WTChangeRequest2 object
     * @return WTChangeIssue : update WTChangeIssue object
     * @throws WTException
     *             : windchill exception
     * @throws ChangeException2
     *             : change exception
     * 
     * 
     */
    public static WTChangeIssue addChangeRequestToProblemReport(WTChangeIssue chi, WTChangeRequest2 chr)
            throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeIssue) RemoteMethodServer.getDefault().invoke("addChangeRequestToProblemReport",
                        CLASSNAME, null, new Class[] { WTChangeIssue.class, WTChangeRequest2.class },
                        new Object[] { chi, chr });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    ChangeHelper2.service.saveFormalizedBy(chr, chi);
                    chi = (WTChangeIssue) PersistenceHelper.manager.refresh(chi);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return chi;
    }

    public static void deleteProblemReport(WTChangeRequest2 chr)
    throws ChangeException2, WTException {
		try {
		    if (!RemoteMethodServer.ServerFlag) {
		        RemoteMethodServer.getDefault().invoke("deleteProblemReport",
		                CLASSNAME, null, new Class[] { WTChangeRequest2.class },
		                new Object[] { chr });
		    } else {
		        boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
		        try {
		        	
		        	List<WTChangeIssue> ecrs = getProblemReportByChangeRequest(chr);
		        	for(WTChangeIssue chi : ecrs){
		        		ChangeHelper2.service.deleteFormalizedBy(chr, chi);
		        	}
		        } finally {
		            SessionServerHelper.manager.setAccessEnforced(enforce);
		        }
		    }
		} catch (RemoteException e) {
		    logger.error(e.getMessage(), e);
		} catch (InvocationTargetException e) {
		    logger.error(e.getMessage(), e);
		}
	}
    
    /**
     * 
     * remove changeable from WTChangeIssue.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chi
     *            : WTChangeIssue object
     * @param changeable
     *            : Changeable2 object
     * @return WTChangeIssue : update WTChangeIssue object
     * @throws WTException
     *             : windchill exception
     * @throws ChangeException2
     *             : change exception
     * 
     * 
     */
    public static WTChangeIssue removeChangeableFromProblemReport(WTChangeIssue chi, Changeable2 changeable)
            throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeIssue) RemoteMethodServer.getDefault().invoke("removeChangeableFromProblemReport",
                        CLASSNAME, null, new Class[] { WTChangeIssue.class, Changeable2.class },
                        new Object[] { chi, changeable });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    ChangeHelper2.service.unattachChangeable(changeable, chi, ReportedAgainst.class,
                            ReportedAgainst.CHANGE_ISSUE_ROLE);
                    chi = (WTChangeIssue) PersistenceHelper.manager.refresh(chi);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return chi;
    }

    /**
     * 
     * remove changeRequest from WTChangeIssue.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chi
     *            : WTChangeIssue object
     * @param chr
     *            : WTChangeRequest2 object
     * @return WTChangeIssue : update WTChangeIssue object
     * @throws WTException
     *             : windchill exception
     * @throws ChangeException2
     *             : change exception
     * 
     * 
     */
    public static WTChangeIssue removeChangeRequestFromProblemReport(WTChangeIssue chi, WTChangeRequest2 chr)
            throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeIssue) RemoteMethodServer.getDefault().invoke("removeChangeRequestFromProblemReport",
                        CLASSNAME, null, new Class[] { WTChangeIssue.class, WTChangeRequest2.class },
                        new Object[] { chi, chr });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    ChangeHelper2.service.deleteFormalizedBy(chr, chi);
                    chi = (WTChangeIssue) PersistenceHelper.manager.refresh(chi);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return chi;
    }

    /**
     * create changeRequest.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param number
     *            -- String : object number
     * @param name
     *            -- String : object name
     * @param attributes
     *            -- Map : attributes collection
     * @param secondaryContents
     *            -- List : String FilePath or InputStream
     * @param containerRef
     *            -- WTContainerRef : container reference
     * @return WTChangeRequest2 : If the specified Numbers of existing changeRequest, then back to the latest version of
     *         the changeRequest object; else return create changeRequest.
     * @throws WTException
     *             : windchill exception
     * @throws ObjectNoLongerExistsException
     *             : object not exist exception
     * @throws IOException
     *             : stream exception
     * @throws PropertyVetoException
     *             : property exception
     * @throws FileNotFoundException
     *             : file exception
     */
    @SuppressWarnings("deprecation")
    public static WTChangeRequest2 createChangeRequest(String number, String name, Map<String, String> attributes,
            List<?> secondaryContents, WTContainerRef containerRef) throws ObjectNoLongerExistsException, WTException,
            FileNotFoundException, PropertyVetoException, IOException {
        WTChangeRequest2 chr = null;
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeRequest2) RemoteMethodServer.getDefault().invoke("createChangeRequest",
                        CLASSNAME, null,
                        new Class[] { String.class, String.class, Map.class, List.class, WTContainerRef.class },
                        new Object[] { number, name, attributes, secondaryContents, containerRef });
            } else {
                String crDesc = "";
                String crType = "";
                String crFolder = "";
                String crCategory = "";
                String crProirity = "";
                String crNeedDate = "";
                String crResolutionDate = "";
                String crRecurringCostEst = "";
                String crNonRecurringCostEst = "";
                String crComplexity = "";

                if (attributes != null) {
                    crDesc = attributes.get(ChangeUtil.DESCRIPTION);
                    crType = attributes.get(ChangeUtil.TYPE);
                    crFolder = attributes.get(ChangeUtil.FOLDER);
                    crCategory = attributes.get(ChangeUtil.CATEGORY);
                    crProirity = attributes.get(ChangeUtil.PRIORITY);
                    crNeedDate = attributes.get(ChangeUtil.NEED_DATE);
                    crResolutionDate = attributes.get(ChangeUtil.RESOLUTION_DATE);
                    crRecurringCostEst = attributes.get(ChangeUtil.RECURRING_COST_EST);
                    crNonRecurringCostEst = attributes.get(ChangeUtil.NON_RECURRING_COST_EST);
                    crComplexity = attributes.get(ChangeUtil.COMPLEXITY);
                }

                if (containerRef == null) {
                    return null;
                }

                // set default number （Default : WTCHANGEISSUEID_SEQ）
                if (number == null || number.equalsIgnoreCase("")) {
                    number = ChangeUtil.getDefaultChangeSeqNumber(WTChangeRequest2.class);
                } else {
                    WTChangeRequest2 existCR = ChangeUtil.getChangeRequest(number, false);
                    if (existCR != null) {
                        return existCR;
                    }
                }

                if (name == null || name.equalsIgnoreCase("")) {
                    return null;
                }

                if (crDesc == null) {
                    crDesc = "";
                }

                // set default type (Default : wt.change2.WTChangeRequest2)
                if (crType == null || crType.equalsIgnoreCase("")) {
                    crType = "wt.change2.WTChangeRequest2";
                }

                // set default folder (Default : /Default)
                if (crFolder == null || crFolder.equalsIgnoreCase("")) {
                    crFolder = ChangeUtil.FOLDER_DEFAULT;
                } else {
                    if (!crFolder.startsWith(ChangeUtil.FOLDER_DEFAULT)) {
                        crFolder = ChangeUtil.FOLDER_DEFAULT + "/" + crFolder;
                    }
                }

                chr = WTChangeRequest2.newWTChangeRequest2();
                chr.setNumber(number);
                chr.setName(name);

                // set description
                if (!"".equals(crDesc)) {
                    chr.setDescription(crDesc);
                }
                // set type
                if (crType != null) {
                    TypeIdentifier typeIdentifier = TypeIdentifierHelper.getTypeIdentifier(crType);
                    chr = (WTChangeRequest2) CoreMetaUtility.setType(chr, typeIdentifier);
                }

                // set context
                chr.setContainerReference(containerRef);

                // set folder
                Folder location = null;
                try {
                    location = FolderHelper.service.getFolder(crFolder, containerRef);
                } catch (Exception e) {
                    location = null;
                }
                if (location == null) {
                    location = FolderHelper.service.saveFolderPath(crFolder, containerRef);
                }
                if (location != null) {
                    WTValuedHashMap map = new WTValuedHashMap();
                    map.put(chr, location);
                    FolderHelper.assignLocations(map);
                }

                // set category
                Category category = null;
                try {
                    category = Category.toCategory(crCategory);
                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error(e.getMessage(), e);
                    }
                    category = Category.getCategoryDefault();
                }
                chr.setCategory(category);

                // set priority
                RequestPriority priority = null;
                try {
                    priority = RequestPriority.toRequestPriority(crProirity);
                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error(e.getMessage(), e);
                    }
                    priority = RequestPriority.getRequestPriorityDefault();
                }
                chr.setRequestPriority(priority);

                // set need Date
                if (crNeedDate != null && !crNeedDate.equals("")) {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat(ChangeUtil.TIME_STAMP_FORMAT);
                        Date date = formatter.parse(crNeedDate);
                        chr.setNeedDate(new Timestamp(date.getTime()));
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }

                // set solution date
                if (crResolutionDate != null && !crResolutionDate.equals("")) {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat(ChangeUtil.TIME_STAMP_FORMAT);
                        Date date = formatter.parse(crResolutionDate);
                        chr.setNeedDate(new Timestamp(date.getTime()));
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }

                // set NonRecurringCostEst
                if (crRecurringCostEst != null && !crRecurringCostEst.equals("")) {
                    try {
                        chr.setRecurringCostEst(crRecurringCostEst);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                // set NonRecurringCostEst
                if (crNonRecurringCostEst != null && !crNonRecurringCostEst.equals("")) {
                    try {
                        chr.setNonRecurringCostEst(crNonRecurringCostEst);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                // set complexity
                Complexity complexity = null;
                if (crComplexity != null && !crComplexity.equals("")) {
                    try {
                        complexity = Complexity.toComplexity(crComplexity);
                        chr.setComplexity(complexity);
                    } catch (Exception e) {
                        complexity = Complexity.getComplexityDefault();
                        chr.setComplexity(complexity);
                        logger.error(e.getMessage(), e);
                    }
                }

                chr = (WTChangeRequest2) PersistenceHelper.manager.save(chr);
                chr = (WTChangeRequest2) PersistenceHelper.manager.refresh(chr);

                // set secondaryContents
                if (secondaryContents != null) {
                    for (int i = 0; i < secondaryContents.size(); i++) {
                        Object secondaryContent = secondaryContents.get(i);

                        ApplicationData applicationdata = ApplicationData.newApplicationData(chr);
                        applicationdata.setRole(ContentRoleType.SECONDARY);
                        if (secondaryContent instanceof String) {
                            String filePath = (String) secondaryContent;
                            applicationdata = ContentServerHelper.service.updateContent(chr, applicationdata, filePath);
                        } else if (secondaryContent instanceof InputStream) {
                            InputStream ins = (InputStream) secondaryContent;
                            applicationdata = ContentServerHelper.service.updateContent(chr, applicationdata, ins);
                        }
                    }
                }

                chr = (WTChangeRequest2) PersistenceServerHelper.manager.restore(chr);
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return chr;
    }

    /**
     * 
     * Get ChangeRequest by number.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param number
     *            -- String : object number
     * @param accessControlled
     *            -- boolean : true have access controlled, false no access controlled
     * @return WTChangeRequest2 : find WTChangeRequest2 object
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("deprecation")
    public static WTChangeRequest2 getChangeRequest(String number, boolean accessControlled) throws WTException {
        WTChangeRequest2 ecr = null;
        try {
            number = number.toUpperCase();
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeRequest2) RemoteMethodServer.getDefault().invoke("getChangeRequest",
                        CLASSNAME, null, new Class[] { String.class, boolean.class },
                        new Object[] { number, accessControlled });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(accessControlled);
                try {
                    QuerySpec spec = new QuerySpec(WTChangeRequest2.class);
                    spec.appendWhere(new SearchCondition(WTChangeRequest2.class, WTChangeRequest2.NUMBER,
                            SearchCondition.EQUAL, number), new int[] { 0 });
                    QueryResult qur = PersistenceHelper.manager.find(spec);
                    if (qur.hasMoreElements()) {
                        ecr = (WTChangeRequest2) qur.nextElement();
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return ecr;
    }

    /**
     * 
     * Get Changeable2 by changeRequest.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chr
     *            : WTChangeRequest2 object
     * @return List : Changeable2 collection
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<Changeable2> getChangeRequestItems(WTChangeRequest2 chr) throws ChangeException2, WTException {
        List<Changeable2> results = new ArrayList<Changeable2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<Changeable2>) RemoteMethodServer.getDefault().invoke("getChangeRequestItems",
                        CLASSNAME, null, new Class[] { WTChangeRequest2.class }, new Object[] { chr });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeables(chr);
                    while (qur.hasMoreElements()) {
                        Object obj = qur.nextElement();
                        if (obj instanceof Changeable2) {
                            Changeable2 changeable = (Changeable2) obj;
                            if (!results.contains(changeable)) {
                                results.add(changeable);
                            }
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Get changeRequest by Changeable2 object.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param changeable
     *            : Changeable2 object
     * @return List : changeRequest2 object collection
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<WTChangeRequest2> getChangeRequestByChangeable(Changeable2 changeable) throws ChangeException2,
            WTException {
        List<WTChangeRequest2> results = new ArrayList<WTChangeRequest2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<WTChangeRequest2>) RemoteMethodServer.getDefault()
                        .invoke("getChangeRequestByChangeable", CLASSNAME, null,
                                new Class[] { Changeable2.class }, new Object[] { changeable });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult queryResult = ChangeHelper2.service.getRelevantChangeRequests(changeable);
                    while (queryResult.hasMoreElements()) {
                        WTChangeRequest2 changeRequest = (WTChangeRequest2) queryResult.nextElement();
                        if (!results.contains(changeRequest)) {
                            results.add(changeRequest);
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Get changeIssue object by changeRequest.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chr
     *            : WTChangeRequest2 object
     * @return List<WTChangeIssue> : changeIssue object collection
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<WTChangeIssue> getProblemReportByChangeRequest(WTChangeRequest2 chr) throws ChangeException2,
            WTException {
        ArrayList<WTChangeIssue> results = new ArrayList<WTChangeIssue>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<WTChangeIssue>) RemoteMethodServer.getDefault().invoke("getProblemReportByChangeRequest",
                        CLASSNAME, null, new Class[] { WTChangeRequest2.class }, new Object[] { chr });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeIssues(chr);
                    while (qur.hasMoreElements()) {
                        WTChangeIssue chi = (WTChangeIssue) qur.nextElement();
                        if (!results.contains(chi)) {
                            results.add(chi);
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Through the change requests get related to the change notices.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chr
     *            : WTChangeRequest2 object
     * @return List : changedOrder2 object collection
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<WTChangeOrder2> getChangeNoticeByChangeRequest(WTChangeRequest2 chr) throws ChangeException2,
            WTException {
        List<WTChangeOrder2> results = new ArrayList<WTChangeOrder2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<WTChangeOrder2>) RemoteMethodServer.getDefault().invoke("getChangeNoticeByChangeRequest",
                        CLASSNAME, null, new Class[] { WTChangeRequest2.class }, new Object[] { chr });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeOrders(chr);
                    while (qur.hasMoreElements()) {
                        WTChangeOrder2 cho = (WTChangeOrder2) qur.nextElement();
                        if (!results.contains(cho)) {
                            results.add(cho);
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Add changeable to changeRequest.
     * <br> 
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chr
     *            : WTChangeRequest2 object
     * @param changeable
     *            : Changeable2 object
     * @return WTChangeRequest2 : update WTChangeRequest2 object
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    public static WTChangeRequest2 addChangeableToChangeRequest(WTChangeRequest2 chr, Changeable2 changeable)
            throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeRequest2) RemoteMethodServer.getDefault().invoke("addChangeableToChangeRequest",
                        CLASSNAME, null, new Class[] { WTChangeRequest2.class, Changeable2.class },
                        new Object[] { chr, changeable });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    Vector<Changeable2> changeables = new Vector<Changeable2>();
                    changeables.add(changeable);
                    ChangeHelper2.service.storeAssociations(RelevantRequestData2.class, chr, changeables);
                    chr = (WTChangeRequest2) PersistenceHelper.manager.refresh(chr);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return chr;
    }

    
    public static WTChangeRequest2 addChangeablesToChangeRequest(WTChangeRequest2 chr, Vector<Changeable2> changeables)
    throws ChangeException2, WTException {
		try {
		    if (!RemoteMethodServer.ServerFlag) {
		        return (WTChangeRequest2) RemoteMethodServer.getDefault().invoke("addChangeablesToChangeRequest",
		                CLASSNAME, null, new Class[] { WTChangeRequest2.class, Vector.class },
		                new Object[] { chr, changeables });
		    } else {
		        boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
		        try {
		            if(changeables != null && changeables.size() > 0){
		            	List<Changeable2> oldChanges = getChangeRequestItems(chr);
		    			for(Changeable2 obj : oldChanges){
		    				ChangeHelper2.service.unattachChangeable(obj, chr, RelevantRequestData2.class,
		                            RelevantRequestData2.CHANGE_REQUEST2_ROLE);
		    			}
		            	ChangeHelper2.service.storeAssociations(RelevantRequestData2.class, chr, changeables);
		            	chr = (WTChangeRequest2) PersistenceHelper.manager.refresh(chr);
		            }
		        } finally {
		            SessionServerHelper.manager.setAccessEnforced(enforce);
		        }
		    }
		} catch (RemoteException e) {
		    logger.error(e.getMessage(), e);
		} catch (InvocationTargetException e) {
		    logger.error(e.getMessage(), e);
		}
		return chr;
	}

    /**
     * 
     * Add ChangeNotice To ChangeRequest.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chr
     *            : WTChangeRequest2 object
     * @param cho
     *            : WTChangeOrder2 object
     * @return WTChangeRequest2 : update WTChangeRequest2 object
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    public static WTChangeRequest2 addChangeNoticeToChangeRequest(WTChangeRequest2 chr, WTChangeOrder2 cho)
            throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeRequest2) RemoteMethodServer.getDefault().invoke("addChangeNoticeToChangeRequest",
                        CLASSNAME, null, new Class[] { WTChangeRequest2.class, WTChangeOrder2.class },
                        new Object[] { chr, cho });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    ChangeHelper2.service.saveAddressedBy(chr, cho);
                    chr = (WTChangeRequest2) PersistenceHelper.manager.refresh(chr);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return chr;
    }

    /**
     * 
     * remove Changeable2 from ChangeRequest.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chr
     *            : WTChangeRequest2 object
     * @param changeable
     *            : Changeable2 object
     * @return WTChangeRequest2 : update WTChangeRequest2 object
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    public static WTChangeRequest2 removeChangeableFromChangeRequest(WTChangeRequest2 chr, Changeable2 changeable)
            throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeRequest2) RemoteMethodServer.getDefault().invoke("removeChangeableFromChangeRequest",
                        CLASSNAME, null, new Class[] { WTChangeRequest2.class, Changeable2.class },
                        new Object[] { chr, changeable });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    ChangeHelper2.service.unattachChangeable(changeable, chr, RelevantRequestData2.class,
                            RelevantRequestData2.CHANGE_REQUEST2_ROLE);
                    chr = (WTChangeRequest2) PersistenceHelper.manager.refresh(chr);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return chr;
    }

    /**
     * 
     * remove changeNotice from changeRequest.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param chr
     *            : WTChangeRequest2 object
     * @param cho
     *            : WTChangeOrder2 object
     * @return WTChangeRequest2 : update WTChangeRequest2 object
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    public static WTChangeRequest2 removeChangeNoticeToChangeRequest(WTChangeRequest2 chr, WTChangeOrder2 cho)
            throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeRequest2) RemoteMethodServer.getDefault().invoke("removeChangeNoticeToChangeRequest",
                        CLASSNAME, null, new Class[] { WTChangeRequest2.class, WTChangeOrder2.class },
                        new Object[] { chr, cho });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    ChangeHelper2.service.deleteAddressedBy(chr, cho);
                    chr = (WTChangeRequest2) PersistenceHelper.manager.refresh(chr);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return chr;
    }

    /**
     * create changeOrder2.创建变更通告
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-15, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param number
     *            -- String : changeOrder2 number
     * @param name
     *            -- String : changeOrder2 name
     * @param attributes
     *            -- Map : contain attributes,传递TYPE和FOLDER即可
     * @param secondaryContents
     *            -- List : secondary list (String FilePath or InputStream)
     * @param containerRef
     *            -- WTContainerRef : object container
     * @return WTChangeOrder2 : If the specified Numbers WTChangeOrder2 object exist, then back to the exist
     *         object;otherwise, return create object
     * @throws IOException
     *             : stream exception
     * @throws PropertyVetoException
     *             : a proposed change to a property represents an unacceptable value exception
     * @throws WTException
     *             : windchill exception
     * @throws FileNotFoundException
     *             : file exception
     * 
     */
    @SuppressWarnings({ "deprecation" })
    public static WTChangeOrder2 createChangeNotice(String number, String name, Map<String, String> attributes,
            List<?> secondaryContents, WTContainerRef containerRef) throws WTException, FileNotFoundException,
            PropertyVetoException, IOException {
        WTChangeOrder2 cho = null;
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeOrder2) RemoteMethodServer.getDefault().invoke("createChangeNotice",
                        CLASSNAME, null,
                        new Class[] { String.class, String.class, Map.class, List.class, WTContainerRef.class },
                        new Object[] { number, name, attributes, secondaryContents, containerRef });
            } else {
                String cnDesc = "";
                String cnType = "";
                String cnFolder = "";
                String cnNeedDate = "";
                String cnComplexity = "";

                if (attributes != null) {
                    cnDesc = attributes.get(ChangeUtil.DESCRIPTION);
                    cnType = attributes.get(ChangeUtil.TYPE);
                    cnFolder = attributes.get(ChangeUtil.FOLDER);
                    cnNeedDate = attributes.get(ChangeUtil.NEED_DATE);
                    cnComplexity = attributes.get(ChangeUtil.COMPLEXITY);
                }

                if (containerRef == null) {
                    return null;
                }

                // set default number （Default : WTCHANGEISSUEID_SEQ）
                if (number == null || number.equalsIgnoreCase("")) {
                    number = ChangeUtil.getDefaultChangeSeqNumber("WTCHANGEORDERID_seq");
                } else {
                    WTChangeOrder2 existCN = ChangeUtil.getChangeNoticeByNumber(number, false);
                    if (existCN != null) {
                        return existCN;
                    }
                }

                if (name == null || name.equalsIgnoreCase("")) {
                    return null;
                }

                if (cnDesc == null) {
                    cnDesc = "";
                }

                // set default type (Default : wt.change2.WTChangeOrder2)
                if (cnType == null || cnType.equalsIgnoreCase("")) {
                    cnType = "wt.change2.WTChangeOrder2";
                }
            
                cho = WTChangeOrder2.newWTChangeOrder2();
    			TypeDefinitionReference td = TypedUtility.getTypeDefinitionReference(cnType);
    			cho.setTypeDefinitionReference(td);
                
                cho.setNumber(number);
                cho.setName(name);

                // set description
                if (!"".equals(cnDesc)) {
                    cho.setDescription(cnDesc);
                }
             
                // set context
                cho.setContainerReference(containerRef);

                // set folder  foder参数为空时使用对象初始化规则里的文件夹
                Folder folder = null;
                if(cnFolder==null || "".equals(cnFolder)){              	
                	folder = (Folder) InitRuleHelper.evaluator.getValue("folder.id", cho, containerRef);
                }else{
                	if (!cnFolder.startsWith(ChangeUtil.FOLDER_DEFAULT)) {
                        cnFolder = ChangeUtil.FOLDER_DEFAULT + "/" + cnFolder;
                    }
                	try {
                		folder = FolderHelper.service.getFolder(cnFolder, containerRef);
                    } catch (Exception e) {
                    	folder = null;
                    }
                    if (folder == null) {
                    	folder = FolderHelper.service.saveFolderPath(cnFolder, containerRef);
                    }
                } 
                if (folder != null) {
                	FolderHelper.assignLocation((FolderEntry) cho, folder);
                }
   
                // set need date
                if (cnNeedDate != null && !cnNeedDate.equals("")) {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat(ChangeUtil.TIME_STAMP_FORMAT);
                        Date date = formatter.parse(cnNeedDate);
                        cho.setNeedDate(new Timestamp(date.getTime()));
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }

                // set complexity
                ChangeNoticeComplexity complexity = null;
                if (cnComplexity != null && !cnComplexity.equals("")) {
                    try {
                        complexity = ChangeNoticeComplexity.toChangeNoticeComplexity(cnComplexity);
                        cho.setChangeNoticeComplexity(complexity);
                    } catch (Exception e) {
                        complexity = ChangeNoticeComplexity.getChangeNoticeComplexityDefault();
                        cho.setChangeNoticeComplexity(complexity);
                    }
                } else {
                    complexity = ChangeNoticeComplexity.getChangeNoticeComplexityDefault();
                    cho.setChangeNoticeComplexity(complexity);
                }

                cho = (WTChangeOrder2) PersistenceHelper.manager.save(cho);
                cho = (WTChangeOrder2) PersistenceHelper.manager.refresh(cho);

                // set secondaryContents
                if (secondaryContents != null) {
                    for (int i = 0; i < secondaryContents.size(); i++) {
                        Object secondaryContent = secondaryContents.get(i);

                        ApplicationData applicationdata = ApplicationData.newApplicationData(cho);
                        applicationdata.setRole(ContentRoleType.SECONDARY);
                        if (secondaryContent instanceof String) {
                            String filePath = (String) secondaryContent;
                            applicationdata = ContentServerHelper.service.updateContent(cho, applicationdata, filePath);
                        } else if (secondaryContent instanceof InputStream) {
                            InputStream ins = (InputStream) secondaryContent;
                            applicationdata = ContentServerHelper.service.updateContent(cho, applicationdata, ins);
                        }
                    }
                }
                cho = (WTChangeOrder2) PersistenceServerHelper.manager.restore(cho);
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return cho;
    }

    /**
     * 
     * Get changeable before by changeNotice.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param cho
     *            : WTChangeOrder2 object
     * @return List<Changeable2> : Changeable2 before object collection
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<Changeable2> getChangeNoticeItemsBefore(WTChangeOrder2 cho) throws ChangeException2, WTException {
        ArrayList<Changeable2> results = new ArrayList<Changeable2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<Changeable2>) RemoteMethodServer.getDefault().invoke("getChangeNoticeItemsBefore",
                        CLASSNAME, null, new Class[] { WTChangeOrder2.class }, new Object[] { cho });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeablesBefore(cho);
                    while (qur.hasMoreElements()) {
                        Object obj = qur.nextElement();
                        if (obj instanceof Changeable2) {
                            Changeable2 changeable = (Changeable2) obj;
                            if (!results.contains(changeable)) {
                                results.add(changeable);
                            }
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Get changeable after by changeNotice.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param cho
     *            : WTChangeOrder2 object
     * @return List<Changeable2> : change after object collecgtion
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<Changeable2> getChangeNoticeItemsAfter(WTChangeOrder2 cho) throws ChangeException2, WTException {
        ArrayList<Changeable2> results = new ArrayList<Changeable2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<Changeable2>) RemoteMethodServer.getDefault().invoke("getChangeNoticeItemsAfter",
                        CLASSNAME, null, new Class[] { WTChangeOrder2.class }, new Object[] { cho });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeablesAfter(cho);
                    while (qur.hasMoreElements()) {
                        Object obj = qur.nextElement();
                        if (obj instanceof Changeable2) {
                            Changeable2 changeable = (Changeable2) obj;
                            if (!results.contains(changeable)) {
                                results.add(changeable);
                            }
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Get changeNotice by changeable before.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param changeable
     *            : Changeable2 object
     * @return List<WTChangeOrder2> : changeNotice object collection
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<WTChangeOrder2> getChangeNoticeByChangeableBefore(Changeable2 changeable)
            throws ChangeException2, WTException {
        ArrayList<WTChangeOrder2> results = new ArrayList<WTChangeOrder2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<WTChangeOrder2>) RemoteMethodServer.getDefault().invoke(
                        "getChangeNoticeByChangeableBefore", CLASSNAME, null,
                        new Class[] { Changeable2.class }, new Object[] { changeable });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getUniqueAffectingChangeOrders(changeable);
                    while (qur.hasMoreElements()) {
                        WTChangeOrder2 cho = (WTChangeOrder2) qur.nextElement();
                        if (!results.contains(cho)) {
                            results.add(cho);
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Get changeNotice by changeable after.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param changeable
     *            : Changeable2 after object.
     * @return List<WTChangeOrder2> : changeNotice object collection
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<WTChangeOrder2> getChangeNoticeByChangeableAfter(Changeable2 changeable)
            throws ChangeException2, WTException {
        ArrayList<WTChangeOrder2> results = new ArrayList<WTChangeOrder2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<WTChangeOrder2>) RemoteMethodServer.getDefault().invoke(
                        "getChangeNoticeByChangeableAfter", CLASSNAME, null,
                        new Class[] { Changeable2.class }, new Object[] { changeable });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getUniqueImplementedChangeOrders(changeable);
                    while (qur.hasMoreElements()) {
                        WTChangeOrder2 cho = (WTChangeOrder2) qur.nextElement();
                        if (!results.contains(cho)) {
                            results.add(cho);
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Through the changeNotices get associated changeRequests.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param cho
     *            : WTChangeOrder2 object
     * @return List<WTChangeRequest2> : WTChangeRequest2 object collection
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<WTChangeRequest2> getChangeRequestByChangeNotice(WTChangeOrder2 cho) throws ChangeException2,
            WTException {
        ArrayList<WTChangeRequest2> results = new ArrayList<WTChangeRequest2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<WTChangeRequest2>) RemoteMethodServer.getDefault().invoke(
                        "getChangeRequestByChangeNotice", CLASSNAME, null,
                        new Class[] { WTChangeOrder2.class }, new Object[] { cho });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeRequest(cho);
                    while (qur.hasMoreElements()) {
                        WTChangeRequest2 chr = (WTChangeRequest2) qur.nextElement();
                        if (!results.contains(chr)) {
                            results.add(chr);
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * Through the changeNotices get associated changeActivitys.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param cho
     *            : WTChangeOrder2 object
     * @return List : WTChangeActivity2 object collection
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<WTChangeActivity2> getChangeActivityByChangeNotice(WTChangeOrder2 cho) throws ChangeException2,
            WTException {
        ArrayList<WTChangeActivity2> results = new ArrayList<WTChangeActivity2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<WTChangeActivity2>) RemoteMethodServer.getDefault().invoke(
                        "getChangeActivityByChangeNotice", CLASSNAME, null,
                        new Class[] { WTChangeOrder2.class }, new Object[] { cho });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeActivities(cho);
                    while (qur.hasMoreElements()) {
                        WTChangeActivity2 cha = (WTChangeActivity2) qur.nextElement();
                        if (!results.contains(cha)) {
                            results.add(cha);
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * 
     * Add changeable before to changeNotice.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param cho
     *            : WTChangeOrder2 object
     * @param changeable
     *            : Changeable2 object
     * @param caName
     *            : changeActivity object
     * @return WTChangeOrder2 : update WTChangeOrder2 object
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    public static WTChangeOrder2 addChangeableBeforeToChangeNotice(WTChangeOrder2 cho, Changeable2 changeable,
            String caName) throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeOrder2) RemoteMethodServer.getDefault().invoke("addChangeableBeforeToChangeNotice",
                        CLASSNAME, null,
                        new Class[] { WTChangeOrder2.class, Changeable2.class, String.class },
                        new Object[] { cho, changeable, caName });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeActivities(cho);

                    WTChangeActivity2 toBeAddedCA = null;
                    while (qur.hasMoreElements()) {
                        WTChangeActivity2 cha = (WTChangeActivity2) qur.nextElement();
                        if (caName == null || caName.equals("")) {
                            // If caName is null or "", get first changeActivity
                            toBeAddedCA = cha;
                            break;
                        } else {
                            if (caName.trim().equals(cha.getName())) {
                                // if caName is exist, prepare add changeable before data
                                toBeAddedCA = cha;
                                break;
                            }
                        }
                        // If caName is not exist, get last changeActivity
                        toBeAddedCA = cha;
                    }

                    if (toBeAddedCA != null) {
                        Vector<Changeable2> changeables = new Vector<Changeable2>();
                        changeables.add(changeable);
                        ChangeHelper2.service.storeAssociations(AffectedActivityData.class, toBeAddedCA, changeables);
                    }
                    cho = (WTChangeOrder2) PersistenceHelper.manager.refresh(cho);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return cho;
    }

    /**
     * 
     * Add changeable after to changeNotice.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param cho
     *            : WTChangeOrder2 object
     * @param changeable
     *            : Changeable2 object
     * @param caName
     *            : changeActivity object
     * @return WTChangeOrder2 : update WTChangeOrder2 object
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    public static WTChangeOrder2 addChangeableAfterToChangeNotice(WTChangeOrder2 cho, Changeable2 changeable,
            String caName) throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeOrder2) RemoteMethodServer.getDefault().invoke("addChangeableAfterToChangeNotice",
                        CLASSNAME, null,
                        new Class[] { WTChangeOrder2.class, Changeable2.class, String.class },
                        new Object[] { cho, changeable, caName });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeActivities(cho);
                    WTChangeActivity2 toBeAddedCA = null;
                    while (qur.hasMoreElements()) {
                        WTChangeActivity2 cha = (WTChangeActivity2) qur.nextElement();
                        if (caName == null || caName.equals("")) {
                            // If caName is null or "", get first changeActivity
                            toBeAddedCA = cha;
                            break;
                        } else {
                            if (caName.trim().equals(cha.getName())) {
                                // if caName is exist, prepare add changeable before data
                                toBeAddedCA = cha;
                                break;
                            }
                        }
                        // If caName is not exist, get last changeActivity
                        toBeAddedCA = cha;
                    }

                    if (toBeAddedCA != null) {
                        Vector<Changeable2> changeables = new Vector<Changeable2>();
                        changeables.add(changeable);
                        ChangeHelper2.service.storeAssociations(ChangeRecord2.class, toBeAddedCA, changeables);
                    }
                    cho = (WTChangeOrder2) PersistenceHelper.manager.refresh(cho);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return cho;
    }
    //Batch add Changeable2 after to WTChangeOrder2 
    public static WTChangeOrder2 addChangeableAfterToChangeNotice(WTChangeOrder2 cho, Collection<Changeable2> changeables,
            String caName) throws ChangeException2, WTException {   
    	try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeOrder2) RemoteMethodServer.getDefault().invoke("addChangeableAfterToChangeNotice",
                        CLASSNAME, null,
                        new Class[] { WTChangeOrder2.class, Collection.class, String.class },
                        new Object[] { cho, changeables, caName });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeActivities(cho);
                    WTChangeActivity2 toBeAddedCA = null;
                    while (qur.hasMoreElements()) {
                        WTChangeActivity2 cha = (WTChangeActivity2) qur.nextElement();
                        if (caName == null || caName.equals("")) {
                            // If caName is null or "", get first changeActivity
                            toBeAddedCA = cha;
                            break;
                        } else {
                            if (caName.trim().equals(cha.getName())) {
                                // if caName is exist, prepare add changeable before data
                                toBeAddedCA = cha;
                                break;
                            }
                        }
                        // If caName is not exist, get last changeActivity
                        toBeAddedCA = cha;
                    }

                    if (toBeAddedCA != null) {
                        Vector<Changeable2> changeable = new Vector<Changeable2>();
                        changeable.addAll(changeables);
                        ChangeHelper2.service.storeAssociations(ChangeRecord2.class, toBeAddedCA, changeable);
                    }
                    cho = (WTChangeOrder2) PersistenceHelper.manager.refresh(cho);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return cho;
    }
    
    /**
     * 移除实施计划下面的所有受影响对象
     * @param cho
     * @param caName
     * @return
     * @throws ChangeException2
     * @throws WTException
     */
    public static WTChangeActivity2 removeAllChangeableBeforeToChangeActivity(WTChangeActivity2 ca) throws ChangeException2, WTException {
    	try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeActivity2) RemoteMethodServer.getDefault().invoke("removeAllChangeableBeforeToChangeActivity",
                        CLASSNAME, null,
                        new Class[] { WTChangeActivity2.class},
                        new Object[] { ca});
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                	QueryResult qr = ChangeHelper2.service.getChangeablesBefore(ca);
                	if(qr != null){
                		while(qr.hasMoreElements()){
                			Changeable2 changeable = (Changeable2) qr.nextElement();
                			ChangeHelper2.service.unattachChangeable(changeable, ca, AffectedActivityData.class,
                                    AffectedActivityData.CHANGE_ACTIVITY2_ROLE);
                		}
                	}
                	ca = (WTChangeActivity2) PersistenceHelper.manager.refresh(ca);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
    	
    	return ca;
    }
    
    /**
     * 移除实施计划下面的所有产生的对象
     * @param cho
     * @param caName
     * @return
     * @throws ChangeException2
     * @throws WTException
     */
    public static WTChangeActivity2 removeAllChangeableAfterToChangeActivity(WTChangeActivity2 ca) throws ChangeException2, WTException {
    	try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeActivity2) RemoteMethodServer.getDefault().invoke("removeAllChangeableAfterToChangeActivity",
                        CLASSNAME, null,
                        new Class[] { WTChangeActivity2.class},
                        new Object[] { ca});
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                	QueryResult qr = ChangeHelper2.service.getChangeablesAfter(ca);
                	if(qr != null){
                		while(qr.hasMoreElements()){
                			Changeable2 changeable = (Changeable2) qr.nextElement();
                			ChangeHelper2.service.unattachChangeable(changeable, ca, AffectedActivityData.class,
                                    AffectedActivityData.CHANGE_ACTIVITY2_ROLE);
                		}
                	}
                	ca = (WTChangeActivity2) PersistenceHelper.manager.refresh(ca);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
    	
    	return ca;
    }
    /**
     * 
     * remove changeable before to changeNotice.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param cho
     *            : WTChangeOrder2 object
     * @param changeable
     *            : Changeable2 object
     * @param caName
     *            : changeActivity object
     * @return WTChangeOrder2 : update WTChangeOrder2 object
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    public static WTChangeOrder2 removeChangeableBeforeToChangeNotice(WTChangeOrder2 cho, Changeable2 changeable,
            String caName) throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeOrder2) RemoteMethodServer.getDefault().invoke("removeChangeableBeforeToChangeNotice",
                        CLASSNAME, null,
                        new Class[] { WTChangeOrder2.class, Changeable2.class, String.class },
                        new Object[] { cho, changeable, caName });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeActivities(cho);
                    WTChangeActivity2 toBeRemovedCA = null;
                    while (qur.hasMoreElements()) {
                        WTChangeActivity2 cha = (WTChangeActivity2) qur.nextElement();
                        if (caName == null || caName.equals("")) {
                            // If caName is null or "", get first changeActivity
                            toBeRemovedCA = cha;
                            break;
                        } else {
                            if (caName.trim().equals(cha.getName())) {
                                // if caName is exist, prepare add changeable before data
                                toBeRemovedCA = cha;
                                break;
                            }
                        }
                        // If caName is not exist, get last changeActivity
                        toBeRemovedCA = cha;
                    }

                    if (toBeRemovedCA != null) {
                        ChangeHelper2.service.unattachChangeable(changeable, toBeRemovedCA, AffectedActivityData.class,
                                AffectedActivityData.CHANGE_ACTIVITY2_ROLE);
                    }
                    cho = (WTChangeOrder2) PersistenceHelper.manager.refresh(cho);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return cho;
    }


    /**
     * 
     * remove changeable after to changeNotice.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param cho
     *            : WTChangeOrder2 object
     * @param changeable
     *            : Changeable2 object
     * @param caName
     *            : changeActivity object
     * @return WTChangeOrder2 : update WTChangeOrder2 object
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    public static WTChangeOrder2 removeChangeableAfterToChangeNotice(WTChangeOrder2 cho, Changeable2 changeable,
            String caName) throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeOrder2) RemoteMethodServer.getDefault().invoke("removeChangeableAfterToChangeNotice",
                        CLASSNAME, null,
                        new Class[] { WTChangeOrder2.class, Changeable2.class, String.class },
                        new Object[] { cho, changeable, caName });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeActivities(cho);
                    WTChangeActivity2 toBeRemovedCA = null;
                    while (qur.hasMoreElements()) {
                        WTChangeActivity2 cha = (WTChangeActivity2) qur.nextElement();
                        if (caName == null || caName.equals("")) {
                            // If caName is null or "", get first changeActivity
                            toBeRemovedCA = cha;
                            break;
                        } else {
                            if (caName.trim().equals(cha.getName())) {
                                // if caName is exist, prepare add changeable before data
                                toBeRemovedCA = cha;
                                break;
                            }
                        }
                        // If caName is not exist, get last changeActivity
                        toBeRemovedCA = cha;
                    }

                    if (toBeRemovedCA != null) {
                        ChangeHelper2.service.unattachChangeable(changeable, toBeRemovedCA, ChangeRecord2.class,
                                ChangeRecord2.CHANGE_ACTIVITY2_ROLE);
                    }
                    cho = (WTChangeOrder2) PersistenceHelper.manager.refresh(cho);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return cho;
    }

    
    public static WTChangeOrder2 removeChangeableAfterToChangeNotice(WTChangeOrder2 cho,
            String caName) throws ChangeException2, WTException {
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeOrder2) RemoteMethodServer.getDefault().invoke("removeChangeableAfterToChangeNotice",
                        CLASSNAME, null,
                        new Class[] { WTChangeOrder2.class,String.class },
                        new Object[] { cho, caName });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeActivities(cho);
                    WTChangeActivity2 toBeRemovedCA = null;
                    while (qur.hasMoreElements()) {
                        WTChangeActivity2 cha = (WTChangeActivity2) qur.nextElement();
                        if (caName == null || caName.equals("")) {
                            // If caName is null or "", get first changeActivity
                            toBeRemovedCA = cha;
                            break;
                        } else {
                            if (caName.trim().equals(cha.getName())) {
                                // if caName is exist, prepare add changeable before data
                                toBeRemovedCA = cha;
                                break;
                            }
                        }
                        // If caName is not exist, get last changeActivity
                        toBeRemovedCA = cha;
                    }

                    if (toBeRemovedCA != null) {
                    	QueryResult qr = ChangeHelper2.service.getChangeablesAfter(toBeRemovedCA); 
                    	while(qr.hasMoreElements()){
	                        ChangeHelper2.service.unattachChangeable((Changeable2) qr.nextElement(), toBeRemovedCA, ChangeRecord2.class,
	                                ChangeRecord2.CHANGE_ACTIVITY2_ROLE);
                    	}
                    }
                    cho = (WTChangeOrder2) PersistenceHelper.manager.refresh(cho);
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return cho;
    }

    
    
    
    /**
     * 
     * Get changeActivty by number.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param number
     *            : String object number
     * @param accessControlled
     *            : boolean : true have access controlled, false no access controlled
     * @return WTChangeActivity2 : changeActivity object
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("deprecation")
    public static WTChangeActivity2 getChangeActivity(String number, boolean accessControlled) throws WTException {
        WTChangeActivity2 cha = null;
        try {
            number = number.toUpperCase();
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeActivity2) RemoteMethodServer.getDefault().invoke("getChangeActivity",
                        CLASSNAME, null, new Class[] { String.class, boolean.class },
                        new Object[] { number, accessControlled });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(accessControlled);
                try {
                    QuerySpec spec = new QuerySpec(WTChangeActivity2.class);
                    spec.appendWhere(new SearchCondition(WTChangeActivity2.class, WTChangeActivity2.NUMBER,
                            SearchCondition.EQUAL, number), new int[] { 0 });

                    QueryResult qur = PersistenceHelper.manager.find(spec);
                    if (qur.hasMoreElements()) {
                        cha = (WTChangeActivity2) qur.nextElement();
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return cha;
    }

    /**
     * 
     * Through the changeActivitys get associated changeNotices.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param cha
     *            : changeActivitys object
     * @return List<WTChangeOrder2> : WTChangeOrder2 object collection
     * @throws ChangeException2
     *             : change exception
     * @throws WTException
     *             : windchill exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<WTChangeOrder2> getChangeNoticeByChangeActivity(WTChangeActivity2 cha) throws ChangeException2,
            WTException {
        List<WTChangeOrder2> results = new ArrayList<WTChangeOrder2>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<WTChangeOrder2>) RemoteMethodServer.getDefault().invoke("getChangeNoticeByChangeActivity",
                        CLASSNAME, null, new Class[] { WTChangeActivity2.class }, new Object[] { cha });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    QueryResult qur = ChangeHelper2.service.getChangeOrder(cha);
                    while (qur.hasMoreElements()) {
                        WTChangeOrder2 cho = (WTChangeOrder2) qur.nextElement();
                        if (!results.contains(cho)) {
                            results.add(cho);
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return results;
    }

    /**
     * create changeActivity.创建实施计划
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-15, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param number
     *            -- String : changeActivity number
     * @param name
     *            -- String : changeActivity name
     * @param attributes
     *            -- Map : contain attributes
     * @param cho
     *            -- WTChangeOrder2 object
     * @return WTChangeIssue : If the specified Numbers changeActivity object exist, then back to the exist
     *         object;otherwise, return create object
     * @throws WTPropertyVetoException
     *             : a proposed change to a property represents an unacceptable value exception
     * @throws WTException
     *             : windchill exception
     * @throws FileNotFoundException
     *             : file exception
     * 
     */
    public static WTChangeActivity2 createChangeActivity(String number, String name, Map<String, String> attributes,
            WTChangeOrder2 cho) throws ChangeException2, WTException, WTPropertyVetoException {
        WTChangeActivity2 cha = null;
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeActivity2) RemoteMethodServer.getDefault().invoke("createChangeActivity",
                        CLASSNAME, null,
                        new Class[] { String.class, String.class, Map.class, WTChangeOrder2.class },
                        new Object[] { number, name, attributes, cho });
            } else {
                String caDesc = "";
                String caNeedDate = "";

                if (attributes != null) {
                    caDesc = attributes.get(ChangeUtil.DESCRIPTION);
                    caNeedDate = attributes.get(ChangeUtil.NEED_DATE);
                }

                if (cho == null) {
                    return null;
                }

                // set default number
                if (number == null || number.equalsIgnoreCase("")) {
                    number = ChangeUtil.getDefaultChangeSeqNumber("WTCHANGEACTIVITYID_seq");
                } else {
                    WTChangeActivity2 existCA = ChangeUtil.getChangeActivity(number, false);
                    if (existCA != null) {
                        return existCA;
                    }
                }

                if (name == null || name.equalsIgnoreCase("")) {
                    return null;
                }

                if (caDesc == null) {
                    caDesc = "";
                }

                cha = WTChangeActivity2.newWTChangeActivity2();
                cha.setNumber(number);
                cha.setName(name);

                // set description
                if (!"".equals(caDesc)) {
                    cho.setDescription(caDesc);
                }
                // set context
                cha.setContainerReference(cho.getContainerReference());

                // set need date
                if (caNeedDate != null && !caNeedDate.equals("")) {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat(ChangeUtil.TIME_STAMP_FORMAT);
                        Date date = formatter.parse(caNeedDate);
                        cha.setNeedDate(new Timestamp(date.getTime()));
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }

                cha = (WTChangeActivity2) ChangeHelper2.service.saveChangeActivity(cho, cha);
                cha = (WTChangeActivity2) PersistenceHelper.manager.refresh(cha);
                cho = (WTChangeOrder2) PersistenceHelper.manager.refresh(cho);
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return cha;
    }

    /**
     * 
     * Get windchill system default change number.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-13, jifeng<br>
     * <b>Comment:</b>
     * 
     * @return String : return ten number
     * @throws WTException
     * 
     * @throws NumberFormatException
     * 
     * 
     * 
     */
    public static String getDefaultChangeSeqNumber(Class<?> objclass) throws NumberFormatException, WTException {
        String bitFormat = "";
        for (int i = 0; i < 10; i++) {
            bitFormat = bitFormat + "0";
        }
        int seq = Integer.parseInt(PersistenceHelper.manager.getNextSequence(objclass));
        DecimalFormat format = new DecimalFormat(bitFormat);
        return format.format(seq);
    }

    public static String getDefaultChangeSeqNumber(String seqString) throws NumberFormatException, WTException {
        String bitFormat = "";
        for (int i = 0; i < 10; i++) {
            bitFormat = bitFormat + "0";
        }
        int seq = Integer.parseInt(PersistenceHelper.manager.getNextSequence(seqString));
        DecimalFormat format = new DecimalFormat(bitFormat);
        return format.format(seq);
    }
    /**
     * 
     * DownLoad ProblemReport, ChangeRequest or ChangeOrder2 secondary file to targetFolder.
     * <br>
     * <b>Revision History</b><br>
     * <b>Rev:</b> 1.0 - 2012-6-25, jifeng<br>
     * <b>Comment:</b>
     * 
     * @param changeitem
     *            : change Object
     * @param targetFolder
     *            : String target path
     * @return List<String> : all downLoad file full path
     * @throws PropertyVetoException
     *             : property exception
     * @throws WTException
     *             : windchill exception
     * @throws IOException
     *             : stream exception
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public static List<String> downloadAttachmentFiles(ChangeItemIfc changeitem, String targetFolder)
            throws WTException, PropertyVetoException, IOException {
        ArrayList<String> result = new ArrayList<String>();
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (List<String>) RemoteMethodServer.getDefault().invoke("downloadAttachmentFiles",
                        CLASSNAME, null, new Class[] { ChangeItemIfc.class, String.class },
                        new Object[] { changeitem, targetFolder });
            } else {
                boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
                try {
                    ContentHolder holder = ContentHelper.service.getContents((ContentHolder) changeitem);
                    QueryResult qur = ContentHelper.service.getContentsByRole(holder, ContentRoleType.SECONDARY);
                    while (qur.hasMoreElements()) {
                        Object objQr = qur.nextElement();
                        if (objQr instanceof ApplicationData) {
                            ApplicationData apd = (ApplicationData) objQr;
                            String adName = apd.getFileName();
                            InputStream is = ContentServerHelper.service.findContentStream(apd);
                            if (is != null) {
                                File downloadFolder = new File(targetFolder);
                                boolean folderExist = downloadFolder.exists();
                                if (!folderExist) {
                                    folderExist = downloadFolder.mkdirs();
                                    if (!folderExist) {
                                        return new ArrayList<String>();
                                    }
                                }

                                String targetFilePath = targetFolder + File.separator + adName;
                                File downloadFile = new File(targetFilePath);

                                FileOutputStream fos = new FileOutputStream(downloadFile);
                                byte[] buf = new byte[1024];

                                int len = 0;
                                while ((len = is.read(buf)) >= 0) {
                                    fos.write(buf, 0, len);
                                }
                                is.close();
                                result.add(targetFilePath);
                            }
                        }
                    }
                } finally {
                    SessionServerHelper.manager.setAccessEnforced(enforce);
                }
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        // ************************ Test of createProblemReport ********************************
        //
        // PDMLinkProduct product = CSCContainer.getPDMLinkProduct("Product0001", false);
        // ReferenceFactory rf = new ReferenceFactory();
        // WTContainerRef containerRef = (WTContainerRef) rf.getReference(product);

        // HashMap<String, String> attributes = new HashMap<String, String>();
        // attributes.put(ChangeUtil.FOLDER, "/Default/TestCreateProblemFolder");
        //
        // List secondaryContents = new ArrayList();
        // WTChangeIssue problem = ChangeUtil.createProblemReport("0000001", "problemReport001", "wcadmin", attributes,
        // secondaryContents, containerRef);
        // System.out.println("Result ProblemReport = " + problem);
        //
        // HashMap<String, String> noticeAttributes = new HashMap<String, String>();
        // noticeAttributes.put(ChangeUtil.FOLDER, "/Default/TestCreateNoticeFolder");
        //
        // List noticeSecondaryContents = new ArrayList();
        // WTChangeOrder2 notice = ChangeUtil.createChangeNotice("0000001", "changeNotice001", noticeAttributes,
        // noticeSecondaryContents, containerRef);
        // System.out.println("Result ChangeNotice = " + notice);

        // HashMap<String, String> requestAttributes = new HashMap<String, String>();
        // noticeAttributes.put(ChangeUtil.FOLDER, "/Default/TestCreateNoticeFolder");
        //
        // WTChangeRequest2 request = ChangeUtil.createChangeRequest("0000001", "changeRequest0002", requestAttributes,
        // noticeSecondaryContents, containerRef);
        // System.out.println("Result ChangeRequest = " + request);

        // WTChangeOrder2 notice = ChangeUtil.getChangeNoticeByNumber("0000001", false);
        // Map<String, String> activityAttributes = new HashMap<String, String>();
        // activityAttributes.put(ChangeUtil.DESCRIPTION, "description test");
        // WTChangeActivity2 activity = ChangeUtil.createChangeActivity("00000001", "activity001", activityAttributes,
        // notice);
        // System.out.println("Result activity = " + activity);
        // ************************ Test of getchangeActivity ********************************
        //
        // WTChangeActivity2 activity2 = ChangeUtil.getChangeActivity("00000001", false);
        // System.out.println("Result activity2 = " + activity2);
        //
        // WTChangeIssue problem2 = ChangeUtil.getProblemReportByNumber("0000001", false);
        // System.out.println("Result problem2 = " + problem2);
        //
        // WTChangeRequest2 request2 = ChangeUtil.getChangeRequest("0000001", false);
        // System.out.println("Result request2 = " + request2);
        //
        // WTChangeOrder2 notice = ChangeUtil.getChangeNoticeByNumber("0000001", false);

        // ************************ Test of addchangeable********************************

        // WTPart wtpart = CSCPart.getPart("0000000028", false);
        // problem2 = ChangeUtil.addChangeableToProblemReport(problem2, wtpart);
        // problem2 = ChangeUtil.addChangeRequestToProblemReport(problem2, request2);
        // System.out.println("Result problem2 = " + problem2);

        // WTPart changeable = CSCPart.getPart("0000000031", false);
        // request2 = ChangeUtil.addChangeableToChangeRequest(request2,changeable);
        // request2 = ChangeUtil.addChangeNoticeToChangeRequest(request2, notice);
        // System.out.println("Result request2 = " + request2);

        // WTPart beforewtpart = CSCPart.getPart("0000000029", false);
        // WTPart afterpart = CSCPart.getPart("0000000030", false);
        //
        // notice = ChangeUtil.addChangeableAfterToChangeNotice(notice, afterpart, "activity001");
        // notice = ChangeUtil.addChangeableBeforeToChangeNotice(notice, beforewtpart, "activity001");
        //
        // System.out.println("Result notice = " + notice);

        // ************************ Test of getchangeItems ********************************
        // List<WTChangeActivity2> activitysList = ChangeUtil.getChangeActivityByChangeNotice(notice);
        // for (int i = 0; i < activitysList.size(); i++) {
        // System.out.println("Result Activitys = " + activitysList.get(i));
        // }
        // WTPart beforewtpart = CSCPart.getPart("0000000029", false);
        // WTPart afterpart = CSCPart.getPart("0000000030", false);
        // List<WTChangeOrder2> afterChangeOrders = ChangeUtil.getChangeNoticeByChangeableAfter(afterpart);
        // for (int i = 0; i < afterChangeOrders.size(); i++) {
        // System.out.println("Result after changeOrders = " + afterChangeOrders.get(i));
        // }
        // List<WTChangeOrder2> beforeChangeOrders = ChangeUtil.getChangeNoticeByChangeableBefore(beforewtpart);
        // for (int i = 0; i < beforeChangeOrders.size(); i++) {
        // System.out.println("Result befor changeOrders = " + beforeChangeOrders.get(i));
        // }
        //
        // List<WTChangeOrder2> noticesbyac = ChangeUtil.getChangeNoticeByChangeActivity(activity2);
        // for (int i = 0; i < noticesbyac.size(); i++) {
        // System.out.println("Result noticesbyac = " + noticesbyac.get(i));
        // }
        //
        // List<WTChangeOrder2> noticesbyre = ChangeUtil.getChangeNoticeByChangeRequest(request2);
        // for (int i = 0; i < noticesbyre.size(); i++) {
        // System.out.println("Result noticesbyre = " + noticesbyre.get(i));
        // }
        //
        // WTPart changeable = CSCPart.getPart("0000000031", false);
        // List<WTChangeRequest2> requestbyca = ChangeUtil.getChangeRequestByChangeable(changeable);
        // for (int i = 0; i < requestbyca.size(); i++) {
        // System.out.println("Result request = " + requestbyca.get(i));
        // }
        //
        // List<WTChangeRequest2> requestbyno = ChangeUtil.getChangeRequestByChangeNotice(notice);
        // for (int i = 0; i < requestbyno.size(); i++) {
        // System.out.println("Result request = " + requestbyno.get(i));
        // }
        //
        // List<WTChangeRequest2> requestbypro = ChangeUtil.getChangeRequestByProblemReport(problem2);
        // for (int i = 0; i < requestbypro.size(); i++) {
        // System.out.println("Result request = " + requestbypro.get(i));
        // }
        //
        // List<Changeable2> requestItems= ChangeUtil.getChangeRequestItems(request2);
        // for (int i = 0; i < requestItems.size(); i++) {
        // System.out.println("Result requestItems = " + requestItems.get(i));
        // }
        //
        // WTPart wtpart = CSCPart.getPart("0000000028", false);
        // List<WTChangeIssue> problems= ChangeUtil.getProblemReportByChangeable(wtpart);
        // for (int i = 0; i < problems.size(); i++) {
        // System.out.println("Result problems = " + problems.get(i));
        // }
        //
        // List<WTChangeIssue> problemsbyrq= ChangeUtil.getProblemReportByChangeRequest(request2);
        // for (int i = 0; i < problemsbyrq.size(); i++) {
        // System.out.println("Result problems = " + problemsbyrq.get(i));
        // }
        //
        // List<Changeable2> problemItems= ChangeUtil.getProblemReportItems(problem2);
        // for (int i = 0; i < problemItems.size(); i++) {
        // System.out.println("Result problems = " + problemItems.get(i));
        // }
        // ************************ Test of remove ********************************
        // WTPart beforewtpart = CSCPart.getPart("0000000029", false);
        // WTPart afterpart = CSCPart.getPart("0000000030", false);
        // notice = ChangeUtil.removeChangeableAfterToChangeNotice(notice, afterpart, "activity001");
        // notice = ChangeUtil.removeChangeableBeforeToChangeNotice(notice, beforewtpart, "activity001");
        // System.out.println("Result notice = " + notice);
        //
        // WTPart changeable = CSCPart.getPart("0000000031", false);
        // request2 = ChangeUtil.removeChangeableFromChangeRequest(request2, changeable);
        // request2 = ChangeUtil.removeChangeNoticeToChangeRequest(request2, notice);
        // System.out.println("Result request2 = " + request2);
        //
        // WTPart wtpart = CSCPart.getPart("0000000028", false);
        // problem2 = ChangeUtil.removeChangeableFromProblemReport(problem2, wtpart);
        // problem2 = ChangeUtil.removeChangeRequestFromProblemReport(problem2, request2);
        // System.out.println("Result problem2 = " + problem2);

    }
    
    public static WTChangeOrder2 createChangeNotice2(String number, String name, Map<String, String> attributes,
            List secondaryContents, WTContainerRef containerRef) throws WTException, FileNotFoundException,
            PropertyVetoException, IOException {
        WTChangeOrder2 cho = null;
        try {
            if (!RemoteMethodServer.ServerFlag) {
                return (WTChangeOrder2) RemoteMethodServer.getDefault().invoke("createChangeNotice2",
                		CLASSNAME, null,
                        new Class[] { String.class, String.class, Map.class, List.class, WTContainerRef.class },
                        new Object[] { number, name, attributes, secondaryContents, containerRef });
            } else {
                String cnDesc = "";
                String cnType = "";
                String cnFolder = "";
                String cnNeedDate = "";
                String cnComplexity = "";

                if (attributes != null) {
                    cnDesc = (String) attributes.get(ChangeUtil.DESCRIPTION);
                    cnType = (String) attributes.get(ChangeUtil.TYPE);
                    cnFolder = (String) attributes.get(ChangeUtil.FOLDER);
                    cnNeedDate = (String) attributes.get(ChangeUtil.NEED_DATE);
                    cnComplexity = (String) attributes.get(ChangeUtil.COMPLEXITY);
                }

                if (containerRef == null) {
                    return null;
                }

                // set default number （Default : WTCHANGEISSUEID_SEQ）
                if (number == null || number.equalsIgnoreCase("")) {
                    number = ChangeUtil.getDefaultChangeSeqNumber(WTChangeOrder2.class);
                } else {
                    WTChangeOrder2 existCN = ChangeUtil.getChangeNoticeByNumber(number, false);
                    if (existCN != null) {
                        return existCN;
                    }
                }

                if (name == null || name.equalsIgnoreCase("")) {
                    return null;
                }

                if (cnDesc == null) {
                    cnDesc = "";
                }

                // set default type (Default : wt.change2.WTChangeOrder2)
                if (cnType == null || cnType.equalsIgnoreCase("")) {
                    cnType = "wt.change2.WTChangeOrder2";
                }

                // set default folder(Default : /Default)
                if (cnFolder == null || cnFolder.equalsIgnoreCase("")) {
                    cnFolder = "/Default";
                } else {
                    if (!cnFolder.startsWith("/Default")) {
                        cnFolder = "/Default/" + cnFolder;
                    }
                }

                cho = WTChangeOrder2.newWTChangeOrder2();
                cho.setNumber(number);
                cho.setName(name);

                // set description
                if (!"".equals(cnDesc)) {
                    cho.setDescription(cnDesc);
                }

                // set type
                if (cnType != null) {
                    TypeIdentifier typeIdentifier = TypeIdentifierHelper.getTypeIdentifier(cnType);
                    cho = (WTChangeOrder2) CoreMetaUtility.setType(cho, typeIdentifier);
                }

                // set context
                cho.setContainerReference(containerRef);

                // set folder
                Folder location = null;
                try {
                    location = FolderHelper.service.getFolder(cnFolder, containerRef);
                } catch (Exception e) {
                    location = null;
                }

                if (location == null) {
                    location = FolderHelper.service.saveFolderPath(cnFolder, containerRef);
                }
                if (location != null) {
                    WTValuedHashMap map = new WTValuedHashMap();
                    map.put(cho, location);
                    FolderHelper.assignLocations(map);
                }

                // set need date
                if (cnNeedDate != null && !cnNeedDate.equals("")) {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat(ChangeUtil.TIME_STAMP_FORMAT);
                        Date date = formatter.parse(cnNeedDate);
                        cho.setNeedDate(new Timestamp(date.getTime()));
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }

                // set complexity
                ChangeNoticeComplexity complexity = null;
                if (cnComplexity != null && !cnComplexity.equals("")) {
                    try {
                        complexity = ChangeNoticeComplexity.toChangeNoticeComplexity(cnComplexity);
                        cho.setChangeNoticeComplexity(complexity);
                    } catch (Exception e) {
                        complexity = ChangeNoticeComplexity.getChangeNoticeComplexityDefault();
                        cho.setChangeNoticeComplexity(complexity);
                    }
                } else {
                    complexity = ChangeNoticeComplexity.getChangeNoticeComplexityDefault();
                    cho.setChangeNoticeComplexity(complexity);
                }

                cho = (WTChangeOrder2) PersistenceHelper.manager.save(cho);
                cho = (WTChangeOrder2) PersistenceHelper.manager.refresh(cho);

                // set secondaryContents
                if (secondaryContents != null) {
                    for (int i = 0; i < secondaryContents.size(); i++) {
                        Object secondaryContent = secondaryContents.get(i);

                        ApplicationData applicationdata = ApplicationData.newApplicationData(cho);
                        applicationdata.setRole(ContentRoleType.SECONDARY);
                        if (secondaryContent instanceof String) {
                            String filePath = (String) secondaryContent;
                            applicationdata = ContentServerHelper.service.updateContent(cho, applicationdata, filePath);
                        } else if (secondaryContent instanceof InputStream) {
                            InputStream ins = (InputStream) secondaryContent;
                            applicationdata = ContentServerHelper.service.updateContent(cho, applicationdata, ins);
                        }
                    }
                }
                cho = (WTChangeOrder2) PersistenceServerHelper.manager.restore(cho);
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return cho;
    }
}
