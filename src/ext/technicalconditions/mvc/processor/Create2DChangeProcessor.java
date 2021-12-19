package ext.technicalconditions.mvc.processor;


import java.util.List;

import org.apache.log4j.Logger;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.FormResult;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import ext.technicalconditions.util.IBAUtil;
import ext.technicalconditions.util.WTUtil;
import wt.doc.WTDocument;
import wt.fc.WTObject;

import wt.util.WTException;
import wt.vc.Versioned;
/**
 * 创建二维图更改单processor
 *
 */
public class Create2DChangeProcessor extends com.ptc.core.components.forms.CreateObjectFormProcessor{
	
	private static final String CLASS_NAME = Create2DChangeProcessor.class.getName();
	private static final Logger logger = Logger.getLogger(CLASS_NAME);
	
	@Override
	public FormResult doOperation(NmCommandBean nmCommandBean, List<ObjectBean> objectBeans) throws WTException{		
		
		FormResult result = super.doOperation(nmCommandBean, objectBeans);
		try {
			logger.debug("开始");
			NmOid oid  = nmCommandBean.getActionOid();
			WTObject objects=(WTObject) oid.getRef();
			if (objects != null && objects instanceof WTDocument) {
				WTDocument twoDimDoc = (WTDocument) objects;
				//ObjectBean获取当前对象相关联的对象
				for (ObjectBean objBean : objectBeans) {
					Object obj = objBean.getObject();
					if (obj instanceof WTDocument) {
						WTDocument twoDimChangeDoc = (WTDocument) obj;
						//获取新建二维图更改单的VROid
						String vrOid = WTUtil.getVROid((Versioned) twoDimChangeDoc);
						logger.debug("二维图更改单VROid:" + vrOid);
						//VRoid存储到二维图纸软属性changeDocOid
						IBAUtil.setIBAValue(twoDimDoc,"changeDocOid",vrOid);
						String iBAstrString = IBAUtil.getIBAValue(twoDimDoc,"changeDocOid");
						logger.debug("获取二维图iba:" + iBAstrString);
					}
				}
			}	
		} catch (Exception e) {
			// TODO: handle exception
		}
		return result;
	}
	
	
	
	
}

