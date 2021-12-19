package ext.technicalconditions.workflow;

import java.beans.PropertyVetoException;

import org.apache.log4j.Logger;

import ext.technicalconditions.util.ContentUtil;
import ext.technicalconditions.util.WfUtil;
import ext.technicalconditions.util.WorkableUtil;
import wt.doc.WTDocument;
import wt.fc.ObjectReference;
import wt.fc.WTObject;
import wt.method.RemoteAccess;
import wt.util.WTException;
import wt.vc.wip.Workable;


/**
 *更改控制流程优化工作流操作相关代码
 *
 */
public class TechCooDocWorkflowHelper implements RemoteAccess{
	
	private static final String CLASSNAME = TechCooDocWorkflowHelper.class.getName();
	private static final Logger logger    = Logger.getLogger(CLASSNAME);
	
	
	/**
	 * 技术协调单,编制/修改环节选择"提交审阅"路由完成校验
	 * 1、文档不能为检出状态;
	 * 2、会签者、批准者必须选择了用户。
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
			logger.debug("技术协调单流程的提交审阅环节校验开始:" + primaryBusinessObject.getDisplayIdentifier());
			WTDocument doc = (WTDocument) primaryBusinessObject;
			//1.检出状态校验
			if(WorkableUtil.isCheckedOut(doc)){
				errorMsg +=("当前技术协调单处于检出状态，请检入后再提交编制任务\n");
			}
			//2.参与者校验:会签者和批准者不能为空
			if(!WfUtil.hasUserInRole(self, primaryBusinessObject, "HUIQIAN")){
				errorMsg +="请为会签者选择用户。";
			}
			if(!WfUtil.hasUserInRole(self, primaryBusinessObject, "APPROVER")){
				errorMsg +="请为批准者选择用户。";
			}
		}catch(Exception e){
			logger.error("技术协调单文档流程的提交审阅环节校验出错",e);
		}
		if(!"".equals(errorMsg)){
			throw new WTException(errorMsg);
		}
	}
	
	
	/**
	 * 技术协调单,编制/修改环节选择"取消审阅"路由完成校验
	 * 1、文档不能为检出状态。
	 * @description
	 * @param self
	 * @param primaryBusinessObject
	 * @throws WTException 
	 */
	public static void cancelReviewValidate(ObjectReference self,WTObject primaryBusinessObject) throws WTException{
		if(WorkableUtil.isCheckedOut((Workable)primaryBusinessObject)){
			throw new WTException("当前技术协调单处于检出状态，请检入后再提交编制任务\n");
		}
	}
	

	
	
	
	
}

