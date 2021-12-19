package ext.technicalconditions.workflow;

import java.beans.PropertyVetoException;
import java.rmi.RemoteException;
import java.text.ParseException;

import org.apache.log4j.Logger;

import ext.technicalconditions.util.ContentUtil;
import ext.technicalconditions.util.IBAUtil;
import ext.technicalconditions.util.WfUtil;
import ext.technicalconditions.util.WorkableUtil;
import ext.technicalconditions.util.WTUtil;
import wt.doc.WTDocument;
import wt.fc.ObjectReference;
import wt.fc.WTObject;
import wt.method.RemoteAccess;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.Versioned;
import wt.vc.wip.Workable;


/**
 *更改控制流程优化工作流操作相关代码
 *
 */
public class TwoDimChangeWorkflowHelper implements RemoteAccess{
	
	private static final String CLASSNAME = TwoDimChangeWorkflowHelper.class.getName();
	private static final Logger logger    = Logger.getLogger(CLASSNAME);
	
	
	
	/**
	 * 二维图更改单,编制/修改环节选择"提交审阅"路由完成校验
	 * 1、文档不能为检出状态;
	 * 2、校对者、审核者、会签者、审定者、批准者 必须选择了用户。
	 * 
	 * @description
	 * @param self
	 * @param primaryBusinessObject
	 * @throws PropertyVetoException 
	 * @throws WTException 
	 */
	public static void submitReviewValidate(ObjectReference self,WTObject primaryBusinessObject) throws WTException{
		String errorMsg = "";
		try{ 
			logger.debug("二维图更改单流程的提交审阅环节校验开始:" + primaryBusinessObject.getDisplayIdentifier());
			WTDocument doc = (WTDocument) primaryBusinessObject;
			//1.检出状态校验
			if(WorkableUtil.isCheckedOut(doc)){
				errorMsg +=("当前二维图更改单处于检出状态，请检入后再提交编制任务\n");
			}
			//2.参与者校验:校对者、审核者、标审者、会签者、审定者和批准者 不能为空
			if(!WfUtil.hasUserInRole(self, primaryBusinessObject, "XIAODUI")){
				errorMsg +="请为校对者选择用户。";
			}
			if(!WfUtil.hasUserInRole(self, primaryBusinessObject, "SHENHE")){
				errorMsg +="请为审核者选择用户。";
			}
			if(!WfUtil.hasUserInRole(self, primaryBusinessObject, "BidReviewer")){
				errorMsg +="请为标审者选择用户。";
			}
			if(!WfUtil.hasUserInRole(self, primaryBusinessObject, "HUIQIAN")){
				errorMsg +="请为会签者选择用户。";
			}
			if(!WfUtil.hasUserInRole(self, primaryBusinessObject, "SHENDING")){
				errorMsg +="请为审定者选择用户。";
			}
			if(!WfUtil.hasUserInRole(self, primaryBusinessObject, "APPROVER")){
				errorMsg +="请为批准者选择用户。";
			}
	
		}catch(Exception e){
			logger.error("二维图更改单流程的提交审阅环节校验出错",e);
		}
		if(!"".equals(errorMsg)){
			throw new WTException(errorMsg);
		}
	}
	
	
	/**
	 * 二维图更改单,编制/修改环节选择"取消审阅"路由完成校验
	 * 1、文档不能为检出状态。
	 * @description
	 * @param self
	 * @param primaryBusinessObject
	 * @throws WTException 
	 */
	public static void cancelReviewValidate(ObjectReference self,WTObject primaryBusinessObject) throws WTException{
		if(WorkableUtil.isCheckedOut((Workable)primaryBusinessObject)){
			throw new WTException("当前二维图更改单处于检出状态，请检入后再提交编制任务\n");
		}
	}
	

	
	
}

