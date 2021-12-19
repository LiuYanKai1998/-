package ext.technicalconditions.util;

import org.apache.log4j.Logger;

import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.inf.container.WTContainer;
import wt.method.RemoteAccess;
import wt.query.QuerySpec;
import wt.query.SearchCondition;

/**
 *容器操作util类
 */
public class ContainerUtil implements RemoteAccess{
	
	private static final String CLASSNAME = ContainerUtil.class.getName();
	private static final Logger logger    = Logger.getLogger(CLASSNAME);
	
	
    public static WTContainer getContainerByName(String containerName) {
        WTContainer container = null;
        try {
            QuerySpec qs = new QuerySpec(WTContainer.class);
            SearchCondition sc = new SearchCondition(WTContainer.class, WTContainer.NAME,
                    SearchCondition.EQUAL, containerName); //创建查询条件类,WTLibrary.NAME=containerInfo.name
            qs.appendSearchCondition(sc);
            QueryResult qr = PersistenceHelper.manager.find(qs);
            if (qr.hasMoreElements()) {
                container = (WTContainer) qr.nextElement();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return container;
    }

}
