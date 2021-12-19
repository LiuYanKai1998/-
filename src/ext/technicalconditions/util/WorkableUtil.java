package ext.technicalconditions.util;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ptc.core.components.util.OidHelper;
import com.ptc.core.htmlcomp.jstable.SandboxStatusHelper;
import com.ptc.core.htmlcomp.jstable.ServerStatusHelper;

import wt.access.AccessControlHelper;
import wt.access.AccessPermission;
import wt.epm.workspaces.EPMWorkspace;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.WTReference;
import wt.fc.collections.WTCollection;
import wt.folder.Folder;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerRef;
import wt.locks.LockException;
import wt.locks.LockHelper;
import wt.org.WTPrincipal;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.util.WTException;
import wt.util.WTRuntimeException;
import wt.vc.wip.CheckoutLink;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;

/**
 * Workable(允许检出检入)接口子类相关操作
 * @author ZhongBinpeng
 * @date   2019年9月25日
 */
public class WorkableUtil {

    private static final Logger log = LoggerFactory.getLogger(WorkableUtil.class);

    /**
     * 执行检出
     * @param workable
     * @param note
     * @return
     * @throws WTException
     */
    public static Workable doCheckOut(Workable workable, String note) throws WTException {
        Workable wrkObj = null;
        try {
            if (WorkInProgressHelper.isCheckedOut(workable, SessionHelper.manager.getPrincipal())) {//是否当前用户检出
                if (WorkInProgressHelper.isWorkingCopy(workable)) {
                    wrkObj = workable;
                } else {
                    wrkObj = WorkInProgressHelper.service.workingCopyOf(workable);
                }
            } else {
                note = StringUtils.trimToEmpty(note);
                Folder folder = WorkInProgressHelper.service.getCheckoutFolder();
                CheckoutLink checkoutLink = WorkInProgressHelper.service.checkout(workable, folder,
                        note);
                if (checkoutLink != null) {
                    wrkObj = checkoutLink.getWorkingCopy();
                }
            }
        } catch (Exception e) {
            log.error("doCheckOut error...", e);
            throw new WTException(e);
        }
        return wrkObj;
    }

    /**
     * 撤销检出
     * @param workable
     * @return
     * @throws WTException
     */
    public static Workable undoCheckout(Workable workable) throws WTException {
        Workable undoCheckoutObj = null;
        if (workable != null) {
            if (!WorkInProgressHelper.isCheckedOut(workable,
                    SessionHelper.manager.getPrincipal())) {
                throw new WTException("对象[" + workable + "] 没有检出或者被其他用户检出。");
            }
            try {
                if (!WorkInProgressHelper.isWorkingCopy(workable)) {
                    workable = WorkInProgressHelper.service.workingCopyOf(workable);
                }
                undoCheckoutObj = WorkInProgressHelper.service.undoCheckout(workable);
            } catch (Exception e) {
                log.error("undoCheckout error...", e);
                throw new WTException("Obj[" + workable + "] undo check out error.");
            }
        }
        return undoCheckoutObj;
    }

    /**
     * 执行检入
     * @param workable
     * @return
     * @throws WTException
     */
    public static Workable doCheckIn(Workable workable) throws WTException {
        return doCheckIn(workable, "自动检入");
    }

    /**
     * 执行检入
     * @param workable 要检入的对象
     * @param comments 检入备注
     * @return
     * @throws WTException
     */
    public static Workable doCheckIn(Workable workable, String comments) throws WTException {
        Workable checkedObj = workable;
        try {
            if (WorkInProgressHelper.isCheckedOut(workable, SessionHelper.manager.getPrincipal())) {//当前用户检出，则检入
                checkedObj = (Workable) WorkInProgressHelper.service.checkin(workable, comments);
            } else {
                throw new WTException("对象[" + workable + "] 没有检出或者被其他用户检出。");
            }
        } catch (Exception e) {
            log.error("doCheckIn error...", e);
            throw new WTException(e);
        }
        return checkedObj;
    }

    /**
     * 是否为检出状态
     * @param workable
     * @return
     * @throws WTException
     */
    public static boolean isCheckedOut(Workable workable) throws WTException {
        return WorkInProgressHelper.isCheckedOut(workable);
    }

    /**
     * 是否被指定用户检出
     * @param workable 
     * @param principal
     * @return
     * @throws WTException
     */
    public static boolean isCheckedOut(Workable workable, WTPrincipal principal)
            throws WTException {
        if (principal == null) {
            principal = SessionHelper.manager.getPrincipal();
        }
        return WorkInProgressHelper.isCheckedOut(workable, principal);
    }
    
    /**
     * 判断对象是否允许被检出
     * 11:11:01 AM
     * @param workable
     * @return
     * @throws LockException
     * @throws WTException
     */
    public static boolean isCheckoutAllowed(Workable workable) throws LockException, WTException {

        return !(WorkInProgressHelper.isWorkingCopy(workable)
                || WorkInProgressHelper.isCheckedOut(workable) || LockHelper.isLocked(workable));
    }

    /**
     * 是否是工作副本
     * @param workable
     * @return
     * @throws WTException
     */
    public boolean isWorkingCopy(Workable workable) throws WTException {
        boolean isWrk = false;
        if (workable != null) {
            if (WorkInProgressHelper.isWorkingCopy(workable)) {
                isWrk = true;
            }
        }
        return isWrk;
    }

    /**
     * 获取工作副本
     * @param workable
     * @return
     * @throws WTException
     */
    public static Workable getWorkingCopyVersion(Workable workable) throws WTException {
        Workable wrkObj = workable;
        if (workable != null) {
            try {
                if (!WorkInProgressHelper.isWorkingCopy(workable)) {
                    wrkObj = WorkInProgressHelper.service.workingCopyOf(workable);
                } else {
                    wrkObj = workable;
                }
            } catch (Exception e) {
                log.error("getWorkingCopyVersion error...", e);
                throw new WTException("getWorkingCopyVersion workable[" + workable + "] error.");
            }
        }
        return wrkObj;
    }

    /**
     * 获取检出者
     * @param workable
     * @return
     * @throws WTException
     */
    public static WTPrincipal getCheckoutUser(Workable workable) throws WTException {
        WTPrincipal checkoutUser = null;
        if (workable != null) {
            try {
                ObjectReference objectRef = ObjectReference.newObjectReference(workable);
                WTContainer wtContainer = getContainer(objectRef);
                ArrayList<ObjectReference> list = new ArrayList<ObjectReference>();
                list.add(objectRef);
                WTCollection wtCollection = OidHelper.getWTCollection(list);
                ServerStatusHelper serverStatusHelper = new ServerStatusHelper(
                        wtCollection.subCollection(Workable.class), null,
                        new SandboxStatusHelper(wtCollection.subCollection(Workable.class), wtContainer), true);
                serverStatusHelper.getServerStatus(workable);
                //serverStatusHelper.getCheckedOutBy(workable);
                checkoutUser = serverStatusHelper.getCheckedOutByUser(workable);
            } catch (Exception e) {
                log.error("getCheckoutUser...", e);
            }
        }
        return checkoutUser;
    }
    
    private static WTContainer getContainer(WTReference wtRef) throws WTException {
        boolean enforce = SessionServerHelper.manager.setAccessEnforced(false);
        WTContainer wtContainer;
        try {
            wtContainer = getContainerAux(wtRef);
        } finally {
            SessionServerHelper.manager.setAccessEnforced(enforce);
        }
        if (!AccessControlHelper.manager.hasAccess(SessionHelper.getPrincipal(), wtContainer, AccessPermission.READ)) {
            wtContainer = null;
        }
        return wtContainer;
    }

    private static WTContainer getContainerAux(WTReference wtReference) throws WTException {
        if (wtReference == null) {
            return null;
        } else if (wtReference instanceof WTContainerRef) {
            return ((WTContainerRef) wtReference).getReferencedContainer();
        } else {
            try {
                Persistable per = wtReference.getObject();
                if (per instanceof WTContained) {
                    return ((WTContained) per).getContainer();
                } else {
                    return per instanceof EPMWorkspace ? ((EPMWorkspace) per).getContainer() : null;
                }
            } catch (WTRuntimeException runtimeException) {
                log.error("getContainerAux...", runtimeException);
                return null;
            }
        }
    }
}
