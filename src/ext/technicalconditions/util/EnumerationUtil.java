package ext.technicalconditions.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ptc.core.lwc.common.view.EnumerationDefinitionReadView;
import com.ptc.core.lwc.common.view.EnumerationEntryReadView;
import com.ptc.core.lwc.common.view.EnumerationMembershipReadView;
import com.ptc.core.lwc.common.view.PropertyHolderHelper;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;

import wt.access.NotAuthorizedException;
import wt.inf.container.WTContainerException;
import wt.method.RemoteAccess;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.util.WTException;

public class EnumerationUtil implements RemoteAccess {

    private static final Logger log = LoggerFactory.getLogger(EnumerationUtil.class);

    /**
     * 获取指定全局枚举所有可选值的内码集
     * @param enumAttributeName
     * @param isAll
     */
    public static ArrayList<String> getEnumKeys(String enumAttributeName, boolean isAll)
            throws WTException {
        ArrayList<String> resultList = new ArrayList<String>();
        Map<String, String> enuMap = getEnumSetByName(enumAttributeName, isAll);
        Set keySet = enuMap.keySet();
        resultList.addAll(keySet);
        return resultList;
    }

    /**
     * 获取全局枚举值
     * @param enumAttributeName
     * @param includeAll
     * @return
     * @throws WTException
     */
    public static Map<String, String> getEnumSetByName(String enumAttributeName, boolean isAll)
            throws WTException {
        Map<String, String> result = new LinkedHashMap<String, String>();
        boolean accessEnforced = false;
        try {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            EnumerationDefinitionReadView edr = TypeDefinitionServiceHelper.service
                    .getEnumDefView(enumAttributeName);
            return getEnumSet(edr,isAll);
        } catch (Exception e) {
            log.error("getEnumSetByName error...", e);
            throw new WTException(e, "Get Enumeration [" + enumAttributeName + "] failed.");
        } finally {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
        }
    }
    
    /**
     * 获取全局枚举值
     * @param enumAttributeName
     * @param includeAll
     * @return
     * @throws WTException
     */
    public static Map<String, String> getEnumSet(EnumerationDefinitionReadView edr, boolean isAll)
            throws WTException {
        Map<String, String> result = new LinkedHashMap<String, String>();
        boolean accessEnforced = false;
        try {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            if (edr != null) {
                //Map<String, EnumerationEntryReadView> views = edr.getAllEnumerationEntries();
                //Set<String> keysOfView = views.keySet();

                Collection<EnumerationMembershipReadView> allMemberships = edr.getAllMemberships();
                if (allMemberships != null && !allMemberships.isEmpty()) {
                    Comparator<EnumerationMembershipReadView> membershipsComparator = edr
                            .getEnumerationMembershipComparator(SessionHelper.getLocale());

                    List<EnumerationMembershipReadView> membershipsList = new ArrayList<EnumerationMembershipReadView>(
                            allMemberships);
                    Collections.sort(membershipsList, membershipsComparator);

                    for (EnumerationMembershipReadView emrv : membershipsList) {
                        EnumerationEntryReadView view = emrv.getMember();
                        if (view != null) {
                            String enumKey = view.getName();
                            String enumName = PropertyHolderHelper.getDisplayName(view,
                                    SessionHelper.getLocale());
                            String selectable = view.getPropertyValueByName("selectable").getValue()
                                    .toString();
                            if (isAll) {
                                result.put(enumKey, enumName);
                            } else if ("true".equalsIgnoreCase(selectable)) {
                                result.put(enumKey, enumName);
                            }
                        }
                    }
                }
            }
        }finally {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
        }
        return result;
    }

    /**
     * 取全局枚举显示名称
     * @param enumDef 枚举名称
     * @param key 内部值
     * @return
     */
    public static String getEnumSetDisplayName(String enumDef, String key) {
        String displayName = "";
        if (key == null || key.equals("")) {
            return displayName;
        }
        boolean accessEnforced = false;
        try {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
            EnumerationDefinitionReadView edr = TypeDefinitionServiceHelper.service
                    .getEnumDefView(enumDef);
            if (edr != null) {
                Map<String, EnumerationEntryReadView> views = edr.getAllEnumerationEntries();
                if (views != null) {
                    EnumerationEntryReadView view = views.get(key);
                    displayName = PropertyHolderHelper.getDisplayName(view,
                            SessionHelper.getLocale());
                }
            }
        } catch (NotAuthorizedException e) {
            log.error("getEnumSetDisplayName NotAuthorizedException error...", e);
        } catch (WTContainerException e) {
            log.error("getEnumSetDisplayName WTContainerException error...", e);
        } catch (WTException e) {
            log.error("getEnumSetDisplayName WTException error...", e);
        } finally {
            accessEnforced = SessionServerHelper.manager.setAccessEnforced(accessEnforced);
        }
        return displayName;
    }

}
