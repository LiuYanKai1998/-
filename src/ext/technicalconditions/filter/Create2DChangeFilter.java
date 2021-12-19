package ext.technicalconditions.filter;

import org.apache.log4j.Logger;

import com.ptc.core.ui.validation.DefaultSimpleValidationFilter;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationStatus;

import ext.technicalconditions.contants.BusinessConstants;
import wt.doc.WTDocument;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.part.WTPartMaster;

/**
 * 创建二维图更改单过滤器
 * 1、当前二维图样文档为"已发布"状态
 * 
 */
/**
 * 第一步 ：继承DefaultSimpleValidationFilter
 */
public class Create2DChangeFilter extends DefaultSimpleValidationFilter {

	private static final Logger logger = Logger.getLogger(Create2DChangeFilter.class);

	/**
	 * 第二步:重写preValidateAction方法
	 */
	public UIValidationStatus preValidateAction(UIValidationKey key, UIValidationCriteria criteria) {
		UIValidationStatus status = UIValidationStatus.HIDDEN;
		try {
			// 获取部件对象
			WTReference objectRef = criteria.getContextObject();
			WTObject obj = (WTObject) objectRef.getObject();
			WTDocument twoDimDoc = (WTDocument) obj;
			String state = BusinessConstants.State.RELEASED;
			
			//1.开始权限校验
			logger.debug("创建二维图更改单校验开始");
			//2.二维图样文档状态为已发布
			String twoDimDocState=twoDimDoc.getState().toString();
			if (state.equals(twoDimDocState)) {
				logger.debug(twoDimDoc.getName()+"二维图样状态为"+twoDimDocState);
				status = UIValidationStatus.ENABLED;
			}else{
				logger.debug(twoDimDoc.getName()+"二维图样状态不是已发布");
			}
			
		} catch (Exception e) {
			logger.error("创建二维图更改单权限控制出错:" + e.getMessage());
			e.printStackTrace();
			status = UIValidationStatus.DISABLED;
		}
		return status;
	}
}
