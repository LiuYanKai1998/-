package ext.technicalconditions.util;

import wt.iba.value.*;

import java.rmi.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


import com.ptc.core.htmlcomp.util.TypeHelper;
import com.ptc.core.lwc.common.view.AttributeDefinitionReadView;
import com.ptc.core.lwc.common.view.ConstraintDefinitionReadView;
import com.ptc.core.lwc.common.view.ConstraintRuleDefinitionReadView;
import com.ptc.core.lwc.common.view.EnumerationDefinitionReadView;
import com.ptc.core.lwc.common.view.PropertyValueReadView;
import com.ptc.core.lwc.common.view.TypeDefinitionReadView;
import com.ptc.core.lwc.common.view.ConstraintDefinitionReadView.RuleDataObject;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;
import com.ptc.core.meta.common.AttributeTypeIdentifier;
import com.ptc.core.meta.common.DiscreteSet;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.impl.WCTypeIdentifier;
import com.ptc.core.meta.server.TypeIdentifierUtility;
import com.ptc.windchill.enterprise.copy.server.CoreMetaUtility;

import java.io.File;


import wt.iba.constraint.AttributeConstraint;
import wt.iba.constraint.ConstraintGroup;
import wt.iba.constraint.IBAConstraintException;
import wt.iba.definition.DefinitionLoader;
import wt.iba.definition.litedefinition.*;
import wt.iba.definition.ReferenceDefinition;
import wt.iba.definition.service.IBADefinitionHelper;
import wt.iba.value.litevalue.*;
import wt.iba.value.service.IBAValueHelper;
import wt.iba.value.service.IBAValueDBService;
import wt.iba.value.service.LoadValue;
import wt.iba.value.service.StandardIBAValueService;
import wt.clients.widgets.NumericToolkit;
import wt.csm.navigation.CSMClassificationNavigationException;
import wt.csm.navigation.ClassificationNode;
import wt.csm.navigation.litenavigation.ClassificationNodeDefaultView;
import wt.csm.navigation.litenavigation.ClassificationNodeNodeView;
import wt.csm.navigation.litenavigation.ClassificationStructDefaultView;
import wt.csm.navigation.service.ClassificationHelper;
import wt.csm.navigation.service.ClassificationService;
import wt.csm.businessentity.*;
import wt.fc.*;
import wt.iba.definition.service.StandardIBADefinitionService;
import wt.lite.AbstractLiteObject;
import wt.method.RemoteMethodServer;
import wt.part.WTPart;
import wt.pds.StatementSpec;
import wt.query.DateHelper;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionHelper;
import wt.type.ClientTypedUtility;
import wt.type.TypeDefinitionReference;
import wt.type.TypedUtility;
import wt.util.*;
import wt.units.service.QuantityOfMeasureDefaultView;

/**
 * 软属性操作类
 */
public class IBAUtil {

	private static final String CLASSNAME = IBAUtil.class.getName();

	private static final Logger logger = Logger.getLogger(CLASSNAME);
	
	/**
	 * 为对象设置软属性值
	 * @param obj 要设置软属性的对象
	 * @param ibaMap 属性内部名称和值的map
	 * @throws RemoteException
	 * @throws WTPropertyVetoException
	 * @throws WTException
	 * @throws ParseException
	 */
	public static void setIBAValue(WTObject obj, Map ibaMap)
			throws RemoteException, WTPropertyVetoException, WTException, ParseException {
		if (ibaMap != null && ibaMap.size() > 0) {
			Set keySet = ibaMap.keySet();
			Iterator it = keySet.iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object value = ibaMap.get(key);
				value = value == null ? "" : value;
				setIBAValue(obj, key, (String) value);
			}
		}
	}
	
	/**
	 * 为对象设置一个属性值
	 * @param obj 要设置软属性的对象
	 * @param ibaName 软属性内部名称
	 * @param ibaValue 软属性值
	 * @throws RemoteException
	 * @throws WTPropertyVetoException
	 * @throws WTException
	 * @throws ParseException
	 */
	public static void setIBAValue(WTObject obj, String ibaName, String ibaValue)
			throws RemoteException, WTPropertyVetoException, WTException, ParseException {
		IBAUtil.setIBAAnyValue(obj, ibaName, ibaValue);
		logger.debug("软属性设置成功,ibaName=" + ibaName + ",ibaValue=" + ibaValue + ",WTObject:" + obj.getDisplayIdentity());
	}

	/**
	 * 获取对象软属性值
	 * 适用于单值约束的情况
	 * @param obj
	 * @param ibaName
	 * @return
	 * @throws WTException
	 */
	public static String getIBAValue(WTObject obj, String ibaName) throws WTException {
		String value = new IBAUtil((IBAHolder) obj).getIBAValue(ibaName);
		return value == null ? "" : value;
	}

	/**
	 * 获取对象软属性值
	 * 适用于非单值约束的情况
	 * @param obj
	 * @param ibaName
	 * @return
	 * @throws WTException
	 */
	public static Vector getIBAValues(WTObject obj, String ibaName) throws WTException {
		return new IBAUtil((IBAHolder) obj).getIBAValues(ibaName);
	}

	public static AttributeDefDefaultView getAttributeDefinition10(String s) {
		AttributeDefDefaultView attributedefdefaultview = null;
		try {
			attributedefdefaultview = IBADefinitionHelper.service.getAttributeDefDefaultViewByPath(s);
			if (attributedefdefaultview == null) {
				AbstractAttributeDefinizerView abstractattributedefinizerview = DefinitionLoader
						.getAttributeDefinition(s);
				if (abstractattributedefinizerview != null)
					attributedefdefaultview = IBADefinitionHelper.service
							.getAttributeDefDefaultView((AttributeDefNodeView) abstractattributedefinizerview);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return attributedefdefaultview;
	}

	/**
	 * @param p
	 * @param key
	 * @return
	 * @throws WTException
	 */
	public static Object getObjectValue(Persistable persistable, String key) {
		Locale locale = null;
		try {
			locale = SessionHelper.getLocale();
		} catch (WTException e) {
			e.printStackTrace();
		}
		if (locale == null) {
			locale = Locale.CHINA;
		}
		if (persistable == null) {
			return null;
		}
		return getObjectValue(persistable, locale, key);
	}

	/**
	 * @param p
	 * @param key
	 * @return
	 * @throws WTException
	 */
	public static Object getObjectValue(Persistable targetObj, Locale locale, String ibaName) {
		Object value = null;
		try {
			if (!RemoteMethodServer.ServerFlag) {
				return (Object) RemoteMethodServer.getDefault().invoke("getObjectValue", CLASSNAME, null,
						new Class[] { Persistable.class, Locale.class, String.class },
						new Object[] { targetObj, locale, ibaName });
			} else {
				PersistableAdapter obj = new PersistableAdapter(targetObj, null, locale, null);
				obj.load(ibaName);
				value = obj.get(ibaName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}

	
	/**
	 * 获取指定类型指定属性的显示名称
	 * @param fullType 类型全名
	 * @param attrName 属性内部名称
	 * @return
	 * @throws WTException
	 */
	public static String getAttributeDisplayName(String fullType,String attrName) throws WTException{
        if(!fullType.startsWith("WCTYPE|")){
        	fullType = "WCTYPE|" + fullType;
        }
		TypeIdentifier	typeidentifier = ClientTypedUtility.getTypeIdentifier(fullType);
		return getAttributeDisplayName(typeidentifier,attrName);
	}
	 public static String getAttributeDisplayName(TypeIdentifier typeidentifier, String attrName) throws WTException { 
		 if(typeidentifier == null || StringUtils.isEmpty(attrName)) {
			 return "";
		 } String displayName = "";
		 TypeDefinitionReadView tv = TypeDefinitionServiceHelper.service.getTypeDefView(typeidentifier); 
		 if (tv != null) {
		  AttributeDefinitionReadView av = tv.getAttributeByName(attrName); 
		  if (av!= null) { 
			  PropertyValueReadView view =av.getPropertyValueByName("displayName"); 
			  if (view != null) {
				  	displayName= view.getValueAsString();
				  } 
			  } 
		  } 
		 return displayName; 
	}



	private static List<Map> getAllAttributes(TypeIdentifier typeidentifier) throws WTException{
		List<Map> ibaAttrs = new ArrayList<Map>();
		TypeDefinitionReadView tv = TypeDefinitionServiceHelper.service.getTypeDefView(typeidentifier);
		Set<AttributeTypeIdentifier> attrs = TypeHelper.getSoftAttributes(typeidentifier,
				WTContext.getContext().getLocale());
		for (AttributeTypeIdentifier attributeTypeIdentifier : attrs) {
			String attributeName = attributeTypeIdentifier.getAttributeName();
			String displayName = attributeName;
			AttributeDefinitionReadView av = tv.getAttributeByName(attributeName);
			
			if (av != null) {
				PropertyValueReadView view = av.getPropertyValueByName("displayName");

				if (view != null) {
					displayName = view.getValueAsString();
				} else {
					logger.debug("displayName为空,attributeName-->" + attributeName);
				}
				AttributeDefDefaultView adv = av.getIBARefView();
				if (adv != null) {
					String attributeClassName = adv.getAttributeDefinitionClassName();
					String attributeType = "String";
					if ("wt.iba.definition.IntegerDefinition".equals(attributeClassName)) {
						attributeType = "Integer";
					} else if ("wt.iba.definition.FloatDefinition".equals(attributeClassName)) {
						attributeType = "Float";
					} else if ("wt.iba.definition.TimestampDefinition".equals(attributeClassName)) {
						attributeType = "Timestamp";
					} else if ("wt.iba.definition.BooleanDefinition".equals(attributeClassName)) {
						attributeType = "Boolean";
					}
					Map ibaAttr = new HashMap();
					ibaAttr.put("typeIdentifier", typeidentifier);
					//名称
					ibaAttr.put("name", attributeName);
					//显示名称
					ibaAttr.put("displayName", displayName);
					//数据类型
					ibaAttr.put("type", attributeType);
					ibaAttr.put("value", "");
					String requried = isRequired(typeidentifier, attributeName) + "";
					ibaAttr.put("requried", requried);
					ibaAttr.put("input", "input");
					ibaAttr.put("readonly", "false");
					List<String> discreteValues = getDiscreteSet(typeidentifier, attributeName);
					if(discreteValues.size() > 0){
						ibaAttr.put("input", "select");
						ibaAttr.put("option", discreteValues);
					}
					//输入类型
					//ibaAttr.put("input","input"); //输入框,复选框,下拉菜单input|checkbox|select
					ibaAttrs.add(ibaAttr);
				} else {
					logger.debug("AttributeDefDefaultView为空,attributeName-->" + attributeName);
				}
			} else {
				logger.debug("AttributeDefinitionReadView为空,attributeName-->" + attributeName);
			}
		}
		return ibaAttrs;
		
	}
	
	/**
	 * 获取指定类型的所有软属性
	 * @param fullType
	 * @return
	 * @throws WTException
	 */
	public static List<Map> getAllAttributes(String fullType) throws WTException{
        if(!fullType.startsWith("WCTYPE|")){
        	fullType = "WCTYPE|" + fullType;
        }
		TypeIdentifier	typeidentifier = ClientTypedUtility.getTypeIdentifier(fullType);
		return getAllAttributes(typeidentifier);
	}
	
	/**
	 * 获取对象所有属性(包括属性名称,显示名称,数据类型)
	 * 
	 * @param obj
	 * @return
	 * @throws WTException
	 */
	public static List<Map> getAllAttributes(WTObject obj) throws WTException {
		TypeIdentifier typeidentifier = (TypeIdentifier) TypeIdentifierUtility.getTypeInstanceIdentifier(obj)
				.getDefinitionIdentifier();
		return getAllAttributes(typeidentifier);
	}

	/**
	 * 获取对象软属性key,显示名称,类型,值(多值之间用英文;隔开)
	 * 
	 * @param ibaHolder
	 * @return
	 * @throws WTException
	 * @throws RemoteException
	 */
	public static List<Map> getAllIBAValues(IBAHolder ibaHolder) throws WTException, RemoteException {

		List<Map> ibaAttrs = getAllAttributes((WTObject) ibaHolder);
		for (Map ibaAttr : ibaAttrs) {
			Vector values = getIBAValues((WTObject) ibaHolder, (String) ibaAttr.get("name"));
			if (values.size() > 0) {
				String value = "";
				for (int i = 0; i < values.size(); i++) {
					if (i != values.size() - 1) {
						value += values.get(i) + ";";
					} else {
						value += values.get(i);
					}

				}
				ibaAttr.put("value", value);
			}
		}
		return ibaAttrs;
	}
	
	
	/**
	 * 获取对象软属性内部名称,值(多值之间用英文;隔开)
	 * @param ibaHolder
	 * @return
	 * @throws WTException
	 * @throws RemoteException
	 */
	public static Map<String,String> getIBAKeyValueMap(IBAHolder ibaHolder) throws WTException, RemoteException {
		Map<String,String> ibaMap = new HashMap<String,String>();
		List<Map> ibaAttrs = getAllAttributes((WTObject) ibaHolder);
		for (Map ibaAttr : ibaAttrs) {
			String ibaName  = (String) ibaAttr.get("name");
			String value = "";
			Vector values = getIBAValues((WTObject) ibaHolder,ibaName);
			if (values.size() > 0) {
				for (int i = 0; i < values.size(); i++) {
					if (i != values.size() - 1) {
						value += values.get(i) + ";";
					} else {
						value += values.get(i);
					}

				}
			}
			ibaMap.put(ibaName, value);
		}
		return ibaMap;
	}
	
    private static boolean isRequired(Collection<ConstraintDefinitionReadView> constraints) {
        boolean flag = false;
        for (ConstraintDefinitionReadView cdrv : constraints) {
            ConstraintRuleDefinitionReadView crdrv = cdrv.getRule();
            if (crdrv != null) {
                String crdrvStr = crdrv.getKey();
                if (StringUtils.isNotBlank(crdrvStr) && crdrvStr.indexOf("ValueRequiredConstraint") > -1) {
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }

    public static boolean isRequired(WTObject  obj, String attrName) throws WTException {
		TypeIdentifier typeidentifier = (TypeIdentifier) TypeIdentifierUtility.getTypeInstanceIdentifier(obj)
				.getDefinitionIdentifier();
		return isRequired(typeidentifier,attrName);
    }
    
    public static boolean isRequired(String fullType, String attrName) throws WTException {
        if(!fullType.startsWith("WCTYPE|")){
        	fullType = "WCTYPE|" + fullType;
        }
		TypeIdentifier	typeidentifier = ClientTypedUtility.getTypeIdentifier(fullType);
		return isRequired(typeidentifier,attrName);
    }
    
    /**
     * 判断指定类型的属性是否为必须
     * @throws WTException 
     *
     **/
    public static boolean isRequired(TypeIdentifier typeidentifier, String attrName) throws WTException {
        TypeDefinitionReadView tv = TypeDefinitionServiceHelper.service.getTypeDefView(typeidentifier);
        AttributeDefinitionReadView av = tv.getAttributeByName(attrName);
        Collection<ConstraintDefinitionReadView> constraints = av.getAllConstraints();
        return isRequired(constraints);
    }
    
    
    /**
     * 获取属性离散/全局枚举约束条件
     * @param fullType
     * @param attributeName
     * @return
     * @throws WTException
     */
    public static List<String> getDiscreteSet(String fullType,String attributeName) throws WTException{
    	try{
            if(!fullType.startsWith("WCTYPE|")){
            	fullType = "WCTYPE|" + fullType;
            }
    		TypeIdentifier	typeidentifier = ClientTypedUtility.getTypeIdentifier(fullType);
    		TypeDefinitionReadView tv = TypeDefinitionServiceHelper.service.getTypeDefView(typeidentifier);
    		AttributeDefinitionReadView av = tv.getAttributeByName(attributeName);
        	Collection<ConstraintDefinitionReadView> constraints = av.getAllConstraints();
        	return getDiscreteSet(constraints);
    	}catch(Exception e){
    		e.printStackTrace();
    		logger.error("获取类型"+fullType+"的属性"+attributeName+"离散约束条件出错:" + e.getLocalizedMessage());
    		return new ArrayList<String>();
    	}

    }
    /**
     * 获取属性离散/全局枚举约束条件
     * @param typeidentifier
     * @param attributeName
     * @return
     * @throws WTException
     */
    public static List<String> getDiscreteSet(TypeIdentifier typeidentifier,String attributeName) throws WTException{
    	try{
    		TypeDefinitionReadView tv = TypeDefinitionServiceHelper.service.getTypeDefView(typeidentifier);
    		AttributeDefinitionReadView av = tv.getAttributeByName(attributeName);
        	Collection<ConstraintDefinitionReadView> constraints = av.getAllConstraints();
        	return getDiscreteSet(constraints);
    	}catch(Exception e){
    		e.printStackTrace();
    		logger.error("获取类型"+typeidentifier+"的属性"+attributeName+"离散约束条件出错:" + e.getLocalizedMessage());
    		return new ArrayList<String>();
    	}

    }
    
    /**
     * 获取离散/全局枚举可选值
     * @param constraints
     * @return
     * @throws WTException
     */
    private static List<String> getDiscreteSet(Collection<ConstraintDefinitionReadView> constraints) throws WTException {
        List<String> discreteSet = new ArrayList<String>();
        for (ConstraintDefinitionReadView cdrv : constraints) {
            ConstraintRuleDefinitionReadView crdrv = cdrv.getRule();
            if (crdrv != null) {
                String crdrvStr = crdrv.getKey();
                if (StringUtils.isNotBlank(crdrvStr) && crdrvStr.indexOf("DiscreteSetConstraint") > -1) {
                    RuleDataObject rdo = cdrv.getRuleDataObj();
                    EnumerationDefinitionReadView edr = rdo.getEnumDef();
                    if(edr != null){
                    	 Map<String, String> enuMap = EnumerationUtil.getEnumSet(edr,true);
                    	 discreteSet.addAll(enuMap.keySet());
                    }
                    if(rdo.getRuleData() == null){
                    	return discreteSet;
                    }
                    DiscreteSet legalValueSet = (DiscreteSet) rdo.getRuleData();
                    Object[] array = legalValueSet.getElements();
                    if (array != null) {
                        for (Object object : array) {
                            Class<? extends Object> classValue = object.getClass();
                            String valueStr = "";
                            if (classValue.isPrimitive()) {
                                valueStr = String.valueOf(object);
                            } else if (String.class.isAssignableFrom(classValue)) {
                                valueStr = (String) object;
                            } else {
                                logger.error("离散约束条件不支持[" + classValue + "]. ");
                            }
                            if (!discreteSet.contains(valueStr)) {
                                discreteSet.add(valueStr);
                            }
                        }
                    }
                    break;
                }
            }
        }
        return discreteSet;
    }
    
    public static TypeDefinitionReference getTypeDefinitionReference(String fullType,
            String defaultType) {
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
/*************************以下为上古时期代码,一般情况请勿调用***************************/
    Hashtable ibaContainer;
    Hashtable ibaOrigContainer;
    final static String UNITS = "SI";
    boolean VERBOSE = false;

    //Can not be called directly by the end user
    /////////////////////////////////////////////
    public IBAUtil() {
        ibaContainer = new Hashtable();
    }

    /**
     * IBAUtility
     * PS. The only constrator can be called by the end user
     *
     * @param ibaHolder: IBAHolder
     * @exception WTException
     */
    public IBAUtil(IBAHolder ibaHolder) throws WTException {
        super();
        try {
            initializeIBAValue(ibaHolder);

        } catch (Exception e) {
            throw new WTException(e);
        }
    }

    public String toString() {

        StringBuffer tempString = new StringBuffer();
        Enumeration enums = ibaContainer.keys();
        try {
            while (enums.hasMoreElements()) {
                String theKey = (String) enums.nextElement();
                AbstractValueView theValue = (AbstractValueView) ((Object[]) ibaContainer.get(theKey))[1];
                tempString.append(theKey + " - " + IBAValueUtility.getLocalizedIBAValueDisplayString(theValue, SessionHelper.manager.getLocale()));
                tempString.append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (tempString.toString());
    }

    /**
     * Get atribute definitions
     *
     * @return Enumeration
     */
    public Enumeration getAttributeDefinitions() {
        return ibaContainer.keys();
    }

    /**
     * Remove all attributes
     *
     * @exception WTException, WTPropertyVetoException
     */
    public void removeAllAttributes() throws WTException, WTPropertyVetoException {
        ibaContainer.clear();
    }

    /**
     * Remove attribute by name
     *
     * @param name: iba name
     * @exception WTException, WTPropertyVetoException
     */
    public void removeAttribute(String name) throws WTException, WTPropertyVetoException {
        ibaContainer.remove(name);
    }

    /**
     * Get IBA value (single)
     *
     * @param name: iba name
     * @return String
     */
    private String getIBAValue(String name) {
        String value = "";
        try {
            if (ibaContainer.get(name) != null) {
                AbstractValueView theValue = (AbstractValueView) ((Object[]) ibaContainer.get(name))[1];
                value = (IBAValueUtility.getLocalizedIBAValueDisplayString(theValue, SessionHelper.manager.getLocale()));
            }
        } catch (WTException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * Get IBA values (multi)
     *
     * @param name: iba name
     * @return Vector
     */
    private Vector getIBAValues(String name) {
        Vector vector = new Vector();
        try {
            if (ibaContainer.get(name) != null) {
                Object[] objs = (Object[]) ibaContainer.get(name);
                for (int i = 1; i < objs.length; i++) {
                    AbstractValueView theValue = (AbstractValueView) objs[i];
                    vector.addElement(IBAValueUtility.getLocalizedIBAValueDisplayString(theValue, SessionHelper.manager.getLocale()));
                }
            }
        } catch (WTException e) {
            e.printStackTrace();
        }
        return vector;
    }

    /**
     * Return multiple IBA values & dependency relationship
     *
     * @param name: iba name
     * @return Vector
     */
    private Vector getIBAValuesWithDependency(String name) {
        Vector vector = new Vector();
        try {
            if (ibaContainer.get(name) != null) {
                Object[] objs = (Object[]) ibaContainer.get(name);
                for (int i = 1; i < objs.length; i++) {
                    AbstractValueView theValue = (AbstractValueView) objs[i];
                    String[] temp = new String[3];
                    temp[0] = IBAValueUtility.getLocalizedIBAValueDisplayString(theValue, SessionHelper.manager.getLocale());
                    if ((theValue instanceof AbstractContextualValueDefaultView) && ((AbstractContextualValueDefaultView) theValue).getReferenceValueDefaultView() != null) {
                        temp[1] = ((AbstractContextualValueDefaultView) theValue).getReferenceValueDefaultView().getReferenceDefinition().getName();
                        temp[2] = ((AbstractContextualValueDefaultView) theValue).getReferenceValueDefaultView().getLocalizedDisplayString();
                    } else {
                        temp[1] = null;
                        temp[2] = null;
                    }
                    vector.addElement(temp);
                }
            }
        } catch (WTException e) {
            e.printStackTrace();
        }
        return vector;
    }

    /**
     * Get IBA value
     *
     * @param ibaHolder: IBAHolder
     * @param name: iba name
     * @return String
     */
    private static String getIBAValue(IBAHolder ibaHolder, String name) {
        String s = "";
        try {
            ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, null, null, null);
            DefaultAttributeContainer theContainer = (DefaultAttributeContainer) ibaHolder.getAttributeContainer();
            if (theContainer != null) {
                AttributeDefDefaultView[] theAtts = theContainer.getAttributeDefinitions();
                for (int i = 0; i < theAtts.length; i++) {
                    //System.out.println(theAtts[i].getDisplayName());
                    if (theAtts[i].getDisplayName().equals(name)) {
                        AbstractValueView[] theValues = theContainer.getAttributeValues(theAtts[i]);
                        if (theValues != null) {
                            for (int j = 0; j < theValues.length; j++) {
                                if (s == null) {
                                    s = IBAValueUtility.getLocalizedIBAValueDisplayString(theValues[j], SessionHelper.manager.getLocale());
                                    break;
                                } else {
                                    s += "," + IBAValueUtility.getLocalizedIBAValueDisplayString(theValues[j], SessionHelper.manager.getLocale());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    /**
     * Get IBA value with business entity
     *
     * @param name: iba name
     * @return Vector
     */
    private Vector getIBAValuesWithBusinessEntity(String name) {
        Vector vector = new Vector();
        try {
            if (ibaContainer.get(name) != null) {
                Object[] objs = (Object[]) ibaContainer.get(name);
                for (int i = 1; i < objs.length; i++) {
                    AbstractValueView theValue = (AbstractValueView) objs[i];
                    Object[] temp = new Object[2];
                    temp[0] = IBAValueUtility.getLocalizedIBAValueDisplayString(theValue, SessionHelper.manager.getLocale());
                    if ((theValue instanceof AbstractContextualValueDefaultView) && ((AbstractContextualValueDefaultView) theValue).getReferenceValueDefaultView() != null) {
                        ReferenceValueDefaultView referencevaluedefaultview = ((AbstractContextualValueDefaultView) theValue).getReferenceValueDefaultView();
                        ObjectIdentifier objectidentifier = ((wt.iba.value.litevalue.DefaultLiteIBAReferenceable) referencevaluedefaultview.getLiteIBAReferenceable()).getObjectID();
                        Persistable persistable = ObjectReference.newObjectReference(objectidentifier).getObject();
                        temp[1] = (BusinessEntity) persistable;
                    } else {
                        temp[1] = null;
                    }
                    vector.addElement(temp);
                }
            }
        } catch (WTException e) {
            e.printStackTrace();
        }
        return vector;
    }
    private Properties getIBAValues() {
            Properties p =new Properties();
		    try{
		        Hashtable hash = getAllIBAValues();
                Enumeration en = hash.keys();
                while (en.hasMoreElements()) {
	                  String key = (String) en.nextElement();
                      String value = getIBAValue(key);
                      p.put( key , value );
                }
		    }catch(Exception e){

		    }
		    return p;
	}
    private Hashtable getAllIBAValues()
         {
            return ibaContainer;
    }

    /**
     * Get IBA BusinessEntity
     *
     * @param name: iba name
     * @return BusinessEntity
     */
    private BusinessEntity getIBABusinessEntity(String name) {
        BusinessEntity value = null;
        try {
            if (ibaContainer.get(name) != null) {
                AbstractValueView theValue = (AbstractValueView) ((Object[]) ibaContainer.get(name))[1];
                ReferenceValueDefaultView referencevaluedefaultview = (ReferenceValueDefaultView) theValue;
                ObjectIdentifier objectidentifier = ((wt.iba.value.litevalue.DefaultLiteIBAReferenceable) referencevaluedefaultview.getLiteIBAReferenceable()).getObjectID();
                Persistable persistable = ObjectReference.newObjectReference(objectidentifier).getObject();
                value = (BusinessEntity) persistable;
            }
        } catch (WTException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * Get IBA BusinessEntities
     *
     * @param name: iba name
     * @return Vector
     */
    private Vector getIBABusinessEntities(String name) {
        Vector vector = new Vector();
        try {
            if (ibaContainer.get(name) != null) {
                Object[] objs = (Object[]) ibaContainer.get(name);
                for (int i = 1; i < objs.length; i++) {
                    AbstractValueView theValue = (AbstractValueView) objs[i];
                    ReferenceValueDefaultView referencevaluedefaultview = (ReferenceValueDefaultView) theValue;
                    ObjectIdentifier objectidentifier = ((wt.iba.value.litevalue.DefaultLiteIBAReferenceable) referencevaluedefaultview.getLiteIBAReferenceable()).getObjectID();
                    Persistable persistable = ObjectReference.newObjectReference(objectidentifier).getObject();
                    vector.addElement(persistable);
                }
            }
        } catch (WTException e) {
            e.printStackTrace();
        }
        return vector;
    }

    /**
     * Get abstract value view
     *
     * @param theDef
     * @param value
     * @return AbstractValueView
     * @exception WTException, WTPropertyVetoException
     */
    private AbstractValueView getAbstractValueView(AttributeDefDefaultView theDef, String value) throws WTException, WTPropertyVetoException {
        if (value == null || value.trim().equals("null") || value.trim().equals("")) {
            System.out.println("IBA value:" + value + " is illegal. Add IBA value failed!!");
            throw new WTException("Error : IBA Name = " + theDef.getName() + ", value is null.Add IBA value failed!!");
        }
        String name = theDef.getName();
        String value2 = null;
        AbstractValueView ibaValue = null;

        if (theDef instanceof UnitDefView) {
            value = value + " " + getDisplayUnits((UnitDefView) theDef, UNITS);
            //System.out.println(value);
        } else if (theDef instanceof ReferenceDefView) {
            value2 = value;
            value = ((ReferenceDefView) theDef).getReferencedClassname();
        }

        ibaValue = internalCreateValue(theDef, value, value2);
        if (ibaValue == null) {
            System.out.println("IBA value:" + value + " is illegal. Add IBA value failed!!");
            throw new WTException("Error : IBA Name = " + theDef.getName() + ", value is null.Add IBA value failed!!");
            //return;
        }

        if (ibaValue instanceof ReferenceValueDefaultView) {
            if (VERBOSE) {
                System.out.println("Before find original reference : " + name + " has key=" + ibaValue.getKey());
            }
            ibaValue = getOriginalReferenceValue(name, ibaValue);
            if (VERBOSE) {
                System.out.println("After find original reference : " + name + " has key=" + ibaValue.getKey());
            }
        }
        ibaValue.setState(AbstractValueView.NEW_STATE);
        return ibaValue;
    }

    /**
     * Get original referece value
     *
     * @param name
     * @param ibaValue
     * @return AbstractValueView
     * @exception IBAValueException
     */
    private AbstractValueView getOriginalReferenceValue(String name, AbstractValueView ibaValue) throws IBAValueException {
        Object[] objs = (Object[]) ibaOrigContainer.get(name);
        if (objs != null && (ibaValue instanceof ReferenceValueDefaultView)) {
            int businessvaluepos = 1;
            for (businessvaluepos = 1; businessvaluepos < objs.length; businessvaluepos++) {
                if (((AbstractValueView) objs[businessvaluepos]).compareTo(ibaValue) == 0) {
                    ibaValue = (AbstractValueView) objs[businessvaluepos];
                    break;
                }
            }
        }
        return ibaValue;
    }

    /**
     * Get DefDefaultValue
     *
     * @param name
     * @return AttributeDefDefaultView
     * @exception WTException
     */
    private AttributeDefDefaultView getDefDefaultView(String name) throws WTException {
        AttributeDefDefaultView theDef = null;
        Object[] obj = (Object[]) ibaContainer.get(name);
        if (obj != null) {
            theDef = (AttributeDefDefaultView) obj[0];
        } else {
            theDef = getAttributeDefinition9(name);
        }
        if (theDef == null) {
            System.out.println("IBA name:" + name + " is illegal. Add IBA value failed!!");
            throw new WTException("Error : IBA Name = " + name + " not existed .Add IBA value failed!!");

        }
        return theDef;
    }

    /**
     * Get attribute definition
     *
     * @param s
     * @param flag
     * @return AttributeDefDefaultView
     */
    public static AttributeDefDefaultView getAttributeDefinition(String s, boolean flag) {
        AttributeDefDefaultView attributedefdefaultview = null;
        try {
            attributedefdefaultview = IBADefinitionHelper.service.getAttributeDefDefaultViewByPath(s);
            if (attributedefdefaultview == null) {
                AbstractAttributeDefinizerView abstractattributedefinizerview = DefinitionLoader.getAttributeDefinition(s);
                if (abstractattributedefinizerview != null) {
                    attributedefdefaultview = IBADefinitionHelper.service.getAttributeDefDefaultView((AttributeDefNodeView) abstractattributedefinizerview);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return attributedefdefaultview;
    }

    /**
     * Get IBA Value view
     *
     * @param dac: DefaultAttributeContainer
     * @param ibaName: iba name
     * @param ibaClass
     * @return AbstractValueView
     * @exception WTException
     */
    public static AbstractValueView getIBAValueView(DefaultAttributeContainer dac, String ibaName, String ibaClass) throws WTException {
        AbstractValueView aabstractvalueview[] = null;
        AbstractValueView avv = null;
        aabstractvalueview = dac.getAttributeValues();
        for (int j = 0; j < aabstractvalueview.length; j++) {
            String thisIBAName = aabstractvalueview[j].getDefinition().getName();
            String thisIBAValue = IBAValueUtility.getLocalizedIBAValueDisplayString(aabstractvalueview[j], Locale.CHINA);
            String thisIBAClass = (aabstractvalueview[j].getDefinition()).getAttributeDefinitionClassName();
            if (thisIBAName.equals(ibaName) && thisIBAClass.equals(ibaClass)) {
                avv = aabstractvalueview[j];
                break;
            }
        }
        return avv;
    }

    /**
     * Get IBA Value view
     *
     * @param dac: DefaultAttributeContainer
     * @param ibaName: iba name
     * @param ibaClass
     * @return Vector
     * @exception WTException
     */
    public static Vector getIBAValueViews(DefaultAttributeContainer dac, String ibaName, String ibaClass) throws WTException {
        AbstractValueView aabstractvalueview[] = null;
        AbstractValueView avv = null;
        Vector vResult = new Vector();
        aabstractvalueview = dac.getAttributeValues();
        for (int j = 0; j < aabstractvalueview.length; j++) {
            String thisIBAName = aabstractvalueview[j].getDefinition().getName();
            String thisIBAClass = (aabstractvalueview[j].getDefinition()).getAttributeDefinitionClassName();
            if (thisIBAName.equals(ibaName) && thisIBAClass.equals(ibaClass)) {
                avv = aabstractvalueview[j];
                vResult.add(avv);
            }
        }
        return vResult;
    }

    public static DefaultAttributeContainer getContainer(IBAHolder ibaHolder) throws WTException, RemoteException {
        ibaHolder = IBAValueHelper.service.refreshAttributeContainerWithoutConstraints(ibaHolder);
        DefaultAttributeContainer defaultattributecontainer = (DefaultAttributeContainer) ibaHolder.getAttributeContainer();
        return defaultattributecontainer;
    }

    private void setIBAValue(IBAHolder ibaHolder, String def, String newv1) throws RemoteException, WTException, WTPropertyVetoException {
        IBAHolder ibaholder = IBAValueHelper.service.refreshAttributeContainerWithoutConstraints((IBAHolder) ibaHolder);
        String oldValue = getIBAValue(ibaholder, def);
        if (VERBOSE) {
            System.out.println("udate IBA Value used default - " + def + " -oldValue- " + oldValue + " -newValue- " + newv1);
        }
        Object[] obj = (Object[]) ibaContainer.get(def);
        if (obj == null) {
            addIBAValue(def, newv1, ibaholder);
        } else {
            replaceIBAValue(def, oldValue, newv1);
        }
    }

    private void setIBAValue(String name, String value) throws WTException, WTPropertyVetoException {
        AttributeDefDefaultView theDef = getDefDefaultView(name);
        Object theValue = getAbstractValueView(theDef, value);
        //System.out.println(name + " put "+((AbstractValueView)theValue).getLocalizedDisplayString());
        Object[] temp = new Object[2];
        temp[0] = theDef;
        temp[1] = theValue;
        ibaContainer.put(name, temp);
    }

    /**
     * Set the attribute with multiple values from the list
     * @param name
     * @param values
     * @throws WTPropertyVetoException
     * @throws WTException
     */
    private void setIBAValues(String name, Vector values) throws WTPropertyVetoException, WTException {
        AttributeDefDefaultView theDef = getDefDefaultView(name);
        Object[] temp = new Object[values.size() + 1];
        temp[0] = theDef;
        for (int i = 0; i < values.size(); i++) {
            String value = (String) values.get(i);
            Object theValue = getAbstractValueView(theDef, value);
            temp[i + 1] = theValue;
        }
        ibaContainer.put(name, temp);
    }

    /**
     * Add IBA value
     *
     * @param name
     * @param value
     * @exception WTException, WTPropertyVetoException
     */
    public void addIBAValue(String name, String value) throws WTException, WTPropertyVetoException {
        Object[] obj = (Object[]) ibaContainer.get(name);
        AttributeDefDefaultView theDef = getDefDefaultView(name);
        Object theValue = getAbstractValueView(theDef, value);

        Object[] temp;
        if (obj == null) {
            temp = new Object[2];
            temp[0] = theDef;
            temp[1] = theValue;
        } else {
            temp = new Object[obj.length + 1];
            int i;
            for (i = 0; i < obj.length; i++) {
                temp[i] = obj[i];
            }
            temp[i] = theValue;
        }
        ibaContainer.put(name, temp);
    }

    /**
     * Add IBA value
     *
     * @param name: iba name
     * @param value: iba value
     * @param ibaholder
     * @exception WTException,WTPropertyVetoException
     */
    public void addIBAValue(String name, String value, IBAHolder ibaholder) throws WTException, WTPropertyVetoException {
        AttributeContainer attributecontainer = ibaholder.getAttributeContainer();
        AttributeDefDefaultView theDef = null;
        AbstractValueView ibanewValue = null;
        AbstractAttributeDefinizerView abstractattributedefinizerview = null;
        if (VERBOSE) {
            System.out.println("---IBAUtil addIBAValue-Start----");
        }
        //AttributeContainer attributecontainer = getCurrentCachedContainer();
        if (VERBOSE) {
            System.out.println("---IBAUtil addIBAValue-1----");
        }
        Object[] obj = (Object[]) ibaContainer.get(name);
        if (VERBOSE) {
            System.out.println("---IBAUtil addIBAValue-2----");
        }
        //System.out.println("Part IBA name" +name);
        if (!(name.equals("PartType"))) {
            //Object[] obj = (Object[]) ibaContainer.get(name);
            if (obj != null) {
                theDef = (AttributeDefDefaultView) obj[0];
            }
            if (theDef == null && VERBOSE) {
                if (VERBOSE) {
                    System.out.println("Part IBA Value is null. Will create a new IBA value.");
                }
            }
            if (theDef == null) {
                theDef = getAttributeDefinition9(name);
            }
            if (theDef == null) {
                if (VERBOSE) {
                    System.out.println("Cannot find IBA Definition:" + name + ". Add IBA value failed!!");
                }
                throw new WTException("Trace.. Cannot find AbstractAttributeDefinizerNodeView ! class = <" + name + ">, identifier = <" + value + "> not found.");
                //return;
            }
        } else {
            //Object[] obj = (Object[]) ibaContainer.get(name);
            abstractattributedefinizerview = wt.iba.value.service.LoadValue.getCachedAttributeDefinition(name);
            if (VERBOSE) {
                System.out.println("---IBAUtil addIBAValue-3----");
            }
        }
        if (!(name.equals("PartType"))) {
            ibanewValue = internalCreateValue(theDef, value);
        } else {
            ibanewValue = internalCreateValue(abstractattributedefinizerview, "ClassificationNode", value);
        }
        if (VERBOSE) {
            System.out.println("---IBAUtil addIBAValue-4----");
        }
        if (ibanewValue == null) {
            if (VERBOSE) {
                System.out.println("IBA value:" + value + " is illegal. Add IBA value failed!!");
            }
            throw new WTException("Trace.. class = <" + name + ">, identifier = <" + value + "> not found.");
            //return;
        }
        if (!(name.equals("PartType"))) {
            // �N�s���ȳ]�w�� NEW_STATE
            ibanewValue.setState(AbstractValueView.NEW_STATE);

            Object[] temp;
            if (obj == null) {
                temp = new Object[2];
                temp[0] = theDef;
                temp[1] = ibanewValue;
            } else {
                temp = new Object[obj.length + 1];
                int i;
                for (i = 0; i < obj.length; i++) {
                    temp[i] = obj[i];
                }
                // �[�J�s����
                temp[i] = ibanewValue;
            }
            ibaContainer.put(theDef.getDisplayName(), temp);
        } else {
            wt.iba.value.service.LoadValue.createOrUpdateAttributeValueInContainer((DefaultAttributeContainer) attributecontainer, ibanewValue);
            if (VERBOSE) {
                System.out.println("---IBAUtil addIBAValue-5----");
            }
        }
    }

    /**
     * Repace IBA value
     *
     * @param name: iba name
     * @param oldvalue: old iba value
     * @param newvalue: new iba value
     * @exception IBAValueException, WTPropertyVetoException, WTException
     */
    public void replaceIBAValue(String name, String oldvalue, String newvalue)
            throws IBAValueException, WTPropertyVetoException, WTException {
        if (oldvalue != null && oldvalue.equals(newvalue)) {
            if (VERBOSE) {
                System.out.println("IBANewUtil: replaceIBAValue: oldvalue:" + oldvalue + " equals newvalue:" + newvalue + ". No changed!");
            }
            return;
        }

        AttributeDefDefaultView theDef = null;
        AbstractValueView ibaoldValue = null;
        AbstractValueView ibanewValue = null;
        Object[] obj = (Object[]) ibaContainer.get(name);
        if (obj != null) {
            theDef = (AttributeDefDefaultView) obj[0];
        } else {
            if (VERBOSE) {
                System.out.println("Part IBA Value is null. Replace IBA value failed!! IBAName - " + name);
            }
            return;
        }
        if (theDef == null) {
            if (VERBOSE) {
                System.out.println("Cannot find IBA Definition:" + name + ". Replace IBA value failed!!");
            }
            return;
        }

        int oldvaluepos = 1;
        if (oldvalue != null) {
            //System.out.println("internalCreateValue: "+oldvalue);
            ibaoldValue = internalCreateValue(theDef, oldvalue);
            if (ibaoldValue == null) {
                if (VERBOSE) {
                    System.out.println("This IBA oldvalue" + oldvalue + " doesn't exist in Part IBA values. Replace IBA value failed!!");
                }
                return;
            }
            //System.out.println("internalCreateValue: [retrieved] : not null "+ibaoldValue);
            // ?????????
            for (oldvaluepos = 1; oldvaluepos < obj.length; oldvaluepos++) {
                if (((AbstractValueView) obj[oldvaluepos]).compareTo(ibaoldValue) == 0) {
                    ibanewValue = (AbstractValueView) obj[oldvaluepos];
                    break;
                }
            }

            if (oldvaluepos == obj.length) {
                if (VERBOSE) {
                    System.out.println("This IBA oldvalue:" + oldvalue + " is not existed in Part IBA values. Replace IBA value failed!!");
                }
                return;
            }
        } else {
            ibanewValue = (AbstractValueView) obj[oldvaluepos];
        }
        //System.out.println("IBANewUtil: replaceIBAValue: old value pos="+oldvaluepos);
        ibanewValue = internalUpdateValue(ibanewValue, newvalue);
        if (ibanewValue == null) {
            if (VERBOSE) {
                System.out.println("This IBA newvalue" + newvalue + " is illegal. Replace IBA value failed!!");
            }
            return;
        }
        // ??? CHANGED_STATE
        ibanewValue.setState(AbstractValueView.CHANGED_STATE);
        obj[oldvaluepos] = ibanewValue;

        //System.out.println("IBANewUtil: replaceIBAValue: newvalue obj["+oldvaluepos+"]="+IBAValueUtility.getLocalizedIBAValueDisplayString((AbstractValueView)obj[oldvaluepos], SessionHelper.manager.getLocale()));

        if (VERBOSE) {
            System.out.println("This IBA newvalue:" + newvalue + " : State = " + ibanewValue.getState() + " replace oldvalue:" + oldvalue + " successfully !!");
        }
        ibaContainer.put(theDef.getDisplayName(), obj);
    }

    /**
     * Internal create value
     *
     * @param theDef: AbstractAttributeDefinizerView
     * @param theValue
     * @return AbstractValueView
     */
    public AbstractValueView internalCreateValue(AbstractAttributeDefinizerView theDef, String theValue) {
        AbstractValueView theView = null;
        if (theDef instanceof FloatDefView) {
            theView = LoadValue.newFloatValue(theDef, theValue, null);
        } else if (theDef instanceof StringDefView) {
            theView = LoadValue.newStringValue(theDef, theValue);
        } else if (theDef instanceof IntegerDefView) {
            theView = LoadValue.newIntegerValue(theDef, theValue);
        } else if (theDef instanceof RatioDefView) {
            theView = LoadValue.newRatioValue(theDef, theValue, null);
        } else if (theDef instanceof TimestampDefView) {
            theValue = theValue + ".000";
            theView = LoadValue.newTimestampValue(theDef, theValue);
        } else if (theDef instanceof BooleanDefView) {
            theView = LoadValue.newBooleanValue(theDef, theValue);
        } else if (theDef instanceof URLDefView) {
            theView = LoadValue.newURLValue(theDef, theValue, null);
        } else if (theDef instanceof ReferenceDefView) {
            String referencedclassname = ((ReferenceDefView) theDef).getReferencedClassname();
            //System.out.println("referencedclassname="+referencedclassname);
            theView = LoadValue.newReferenceValue(theDef, referencedclassname, theValue);
        } else if (theDef instanceof UnitDefView) {
            theView = LoadValue.newUnitValue(theDef, theValue, null);
        }
        return theView;
    }

    /**
     * Internal update value
     *
     * @param theView
     * @param theValue
     * @return AbstractValueView
     * @exception WTPropertyVetoException, IBAValueException
     */
    public AbstractValueView internalUpdateValue(AbstractValueView theView, String theValue)
            throws WTPropertyVetoException, IBAValueException {
        if (theView instanceof FloatValueDefaultView) {
            double d = Double.valueOf(theValue).doubleValue();
            int i = NumericToolkit.countSigFigs(theValue);
            if (VERBOSE) {
                System.out.println("Float Value = " + d + "\tPrecision = " + i);
            }
            ((FloatValueDefaultView) theView).setValue(d);
            ((FloatValueDefaultView) theView).setPrecision(i);
        } else if (theView instanceof UnitValueDefaultView) {
            double d = Double.valueOf(theValue).doubleValue();
            int i = NumericToolkit.countSigFigs(theValue);
            if (VERBOSE) {
                System.out.println("Unit Value = " + d + "\tPrecision = " + i);
            }
            ((UnitValueDefaultView) theView).setValue(d);
            ((UnitValueDefaultView) theView).setPrecision(i);
        } else if (theView instanceof RatioValueDefaultView) {
            double d = Double.valueOf(theValue).doubleValue();
            //double d1 = Double.valueOf(s1).doubleValue();
            double d1 = 1.0D;
            if (VERBOSE) {
                System.out.println("Ratio Value = " + d + "\tdenominator = " + d1);
            }
            ((RatioValueDefaultView) theView).setValue(d);
            ((RatioValueDefaultView) theView).setDenominator(d1);
        } else if (theView instanceof ReferenceValueDefaultView) {
            AttributeDefDefaultView theDef = theView.getDefinition();
            if (!(theDef instanceof ReferenceDefView)) {
                //System.out.println("IBANewUtil internalUpdateValue, identifier=<" + theValue + ">, definition=<" + theDef + "> is not ReferenceDefView.");
                return null;
            }

            String referencedclassname = ((ReferenceDefView) theDef).getReferencedClassname();
            IBAReferenceable ibareferenceable = LoadValue.getIBAReferenceable(referencedclassname, theValue);
            if (ibareferenceable == null) {
                //System.out.println("IBANewUtil internalUpdateValue, class=<" + referencedclassname + ">, identifier=<" + theValue + "> not found.");
                return null;
            }
            DefaultLiteIBAReferenceable defaultliteibareferenceable = new DefaultLiteIBAReferenceable(ibareferenceable);
            ((ReferenceValueDefaultView) theView).setLiteIBAReferenceable(defaultliteibareferenceable);
        } else if (theView instanceof StringValueDefaultView) {
            if (theValue == null) {
                theValue = "";
            }
            ((StringValueDefaultView) theView).setValue(theValue);
        } else if (theView instanceof TimestampValueDefaultView) {
            Timestamp timestamp1 = new Timestamp(System.currentTimeMillis());
            if (theValue != null) {
                DateHelper datehelper = new DateHelper(theValue, "time");
                timestamp1 = new Timestamp(datehelper.getDate().getTime());
            }
            ((TimestampValueDefaultView) theView).setValue(timestamp1);
        } else if (theView instanceof BooleanValueDefaultView) {
            boolean flag = false;
            if (theValue != null) {
                if (theValue.equals("0") || theValue.equalsIgnoreCase("FALSE")) {
                    flag = false;
                } else if (theValue.equals("1") || theValue.equalsIgnoreCase("TRUE")) {
                    flag = true;
                } else {
                    //System.out.println("Unknown boolean value. only 0/1 or FALSE/TRUE allowed.");
                    return null;
                }
            }
            ((BooleanValueDefaultView) theView).setValue(flag);
        } else if (theView instanceof URLValueDefaultView) {
            if (theValue == null) {
                theValue = "";
            }
            ((URLValueDefaultView) theView).setValue(theValue);
        } else if (theView instanceof IntegerValueDefaultView) {
            long l = 0L;
            try {
                if (theValue != null) {
                    l = Long.valueOf(theValue).longValue();
                }
            } catch (NumberFormatException numberformatexception) {
                //System.out.println(numberformatexception);
                return null;
            }
            ((IntegerValueDefaultView) theView).setValue(l);
        }
        return theView;
    }

    /**
     * Set dependency
     *
     * @param sourceDef
     * @param sourceValue
     * @param businessDef
     * @param businessValue
     * @return AbstractValueView
     * @exception WTPropertyVetoException, WTException
     */
    private AbstractValueView setDependency(AttributeDefDefaultView sourceDef, AbstractValueView sourceValue, AttributeDefDefaultView businessDef, AbstractValueView businessValue) throws WTPropertyVetoException, WTException {
        String sourcename = sourceDef.getName();
        String businessname = businessDef.getName();

        if (businessValue == null) {
            throw new WTException("This Business Entity:" + businessname + " value doesn't exist in System Business Entity. Add IBA dependancy failed!!");
        }
        Object[] businessobj = (Object[]) ibaContainer.get(businessname);
        if (businessobj == null) {
            throw new WTException("Part IBA:" + businessname + " Value is null. Add IBA dependancy failed!!");
        }
        // �ˬd Business �ݩʬO�_�w�� business value
        int businessvaluepos = 1;
        for (businessvaluepos = 1; businessvaluepos < businessobj.length; businessvaluepos++) {
            if (((AbstractValueView) businessobj[businessvaluepos]).compareTo(businessValue) == 0) {
                businessValue = (AbstractValueView) businessobj[businessvaluepos];
                break;
            }
        }
        if (businessvaluepos == businessobj.length) {
            throw new WTException("This Business Entity:" + businessname + " value:" + businessValue.getLocalizedDisplayString() + " is not existed in Part IBA values. Add IBA dependancy failed!!");
        }

        if (!(businessValue instanceof ReferenceValueDefaultView)) {
            throw new WTException("This Business Entity:" + businessname + " value:" + businessValue.getLocalizedDisplayString() + " is not a ReferenceValueDefaultView. Add IBA dependancy failed!!");
        }
        ((AbstractContextualValueDefaultView) sourceValue).setReferenceValueDefaultView((ReferenceValueDefaultView) businessValue);
        if (VERBOSE) {
            System.out.println("ref obj=" + ((AbstractContextualValueDefaultView) sourceValue).getReferenceValueDefaultView().getLocalizedDisplayString());
        }
        if (VERBOSE) {
            System.out.println("ref key=" + ((AbstractContextualValueDefaultView) sourceValue).getReferenceValueDefaultView().getKey());
        }
        if (VERBOSE) {
            System.out.println("This IBA:" + sourcename + " value:" + sourceValue.getLocalizedDisplayString() + " add dependancy with Business Entity:" + businessname + " value:" + businessValue.getLocalizedDisplayString() + " successfully with state=" + sourceValue.getState() + " !!");
        }
        return sourceValue;
    }

    /**
     * Set IBA value
     *
     * @param sourcename
     * @param sourcevalue
     * @param businessname
     * @param businessvalue
     * @exception IBAValueException, WTPropertyVetoException, WTException
     */
    public void setIBAValue(String sourcename, String sourcevalue, String businessname, String businessvalue) throws IBAValueException, WTPropertyVetoException, WTException {

        AttributeDefDefaultView sourceDef = getDefDefaultView(sourcename);
        AttributeDefDefaultView businessDef = getDefDefaultView(businessname);
        AbstractValueView sourceValue = getAbstractValueView(sourceDef, sourcevalue);
        AbstractValueView businessValue = getAbstractValueView(businessDef, businessvalue);
        sourceValue = setDependency(sourceDef, sourceValue, businessDef, businessValue);
        Object[] temp = new Object[2];
        temp[0] = sourceDef;
        temp[1] = sourceValue;
        ibaContainer.put(sourcename, temp);
    }

    /**
     * Set iba attribute value
     * @param obj         object
     * @param ibaName     attribute name
     * @param newValue     attribute value
     * @return void
     * @exception WTException
     */
    public static void setIBAStringValue(WTObject obj, String ibaName, String newValue) throws WTException {
        String ibaClass = "wt.iba.definition.StringDefinition";
        //   System.out.println("ENTER..." + ibaName + "..." + newValue);
        try {
            if (obj instanceof IBAHolder) {
                IBAHolder ibaHolder = (IBAHolder) obj;
                DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
                if (defaultattributecontainer == null) {
                    defaultattributecontainer = new DefaultAttributeContainer();
                    ibaHolder.setAttributeContainer(defaultattributecontainer);
                }
                StringValueDefaultView abstractvaluedefaultview = (StringValueDefaultView) getIBAValueView(defaultattributecontainer, ibaName, ibaClass);
                if (abstractvaluedefaultview != null) {
                    abstractvaluedefaultview.setValue(newValue);
                    defaultattributecontainer.updateAttributeValue(abstractvaluedefaultview);
                } else {
                    AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
                    StringValueDefaultView abstractvaluedefaultview1 = new StringValueDefaultView((StringDefView) attributedefdefaultview, newValue);
                    defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);
                }
                ibaHolder.setAttributeContainer(defaultattributecontainer);
                StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
                ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
                //wt.iba.value.service.LoadValue.applySoftAttributes(ibaHolder);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
	 * Get classification node full path Ex. Classification: Electronic Parts \
	 * CHIPSET Return "\Electronic Parts\CHIPSET"
	 *
	 * @param Persistable
	 *            p: WTPart object
	 * @return String
	 * @exception Exception
	 **/
    public static HashMap nodesFullPath = new HashMap();
	public static String getClassificationNodeFullPath(WTObject p)
			throws Exception {
		String result = "";
		String token = File.separator;
		ClassificationNode cn = getClassificationNode(p);
		if (cn != null) {
			result = (String) nodesFullPath.get(cn.toString());
			if (result != null)
				return result;
			result = cn.getName();
			for (ClassificationNode cnParent = cn.getParent(); cnParent != null; cnParent = cnParent.getParent()) {
				result = cnParent.getName() + token + result;
			}
		}
		result = token + result;
		System.out.println("obj=" + p.getDisplayIdentity() + " node=" + cn + " result:"	+ result);
		if (cn != null)
			nodesFullPath.put(cn.toString(), result);
		return result;
	}

	/**
	 * Get classification node Ex. Classification: Electronic Parts \ CHIPSET
	 * cn.getName() == CHIPSET
	 *
	 * @param Persistable
	 *            p: WTPart object
	 * @return ClassificationNode
	 * @exception Exception
	 **/
	public static ClassificationNode getClassificationNode(WTObject p)
			throws Exception {
		ClassificationNode node = null;
		QuerySpec qs = new QuerySpec(ReferenceValue.class);
		qs.appendWhere(
				new SearchCondition(ReferenceValue.class,"theIBAHolderReference.key", "=", PersistenceHelper.getObjectIdentifier(p)), new int[] { 0 });
		QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
		while (qr.hasMoreElements()) {
			ReferenceValue rv = (ReferenceValue) qr.nextElement();
			Persistable obj = rv.getIBAReferenceableReference().getObject();
			if (obj instanceof ClassificationNode) {
				node = (ClassificationNode) obj;
			}
		}
		return node;
	}

    /**
     * Set IBA string values
     *
     * @param obj
     * @param ibaName: iba name
     * @param newValue: new value array
     * @exception WTException
     */
    public static void setIBAStringValues(WTObject obj, String ibaName, String[] newValue) throws WTException {
        String oneNewValue = "";
        try {
            if (obj instanceof IBAHolder) {
                for (int i = 0; i < newValue.length; i++) {
                    oneNewValue = newValue[i];
                    IBAHolder ibaHolder = (IBAHolder) obj;
                    DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
                    if (defaultattributecontainer == null) {
                        defaultattributecontainer = new DefaultAttributeContainer();
                        ibaHolder.setAttributeContainer(defaultattributecontainer);
                    }

                    AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
                    StringValueDefaultView abstractvaluedefaultview1 = new StringValueDefaultView((StringDefView) attributedefdefaultview, oneNewValue);
                    defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);

                    ibaHolder.setAttributeContainer(defaultattributecontainer);
                    StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
                    ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
                    //wt.iba.value.service.LoadValue.applySoftAttributes(ibaHolder);
                }
            }
            //       System.out.println("ENTER..." + ibaName + "..." + newValue.toString());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set IBA Boolean Value
     *
     * @param obj
     * @param ibaName: iba name
     * @param new Value: TRUE or FALSE
     * @exception WTException
     */
    public static void setIBABooleanValue(WTObject obj, String ibaName, boolean newValue) throws WTException {
        String ibaClass = "wt.iba.definition.BooleanDefinition";
        try {
            if (obj instanceof IBAHolder) {
                IBAHolder ibaHolder = (IBAHolder) obj;
                DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
                if (defaultattributecontainer == null) {
                    defaultattributecontainer = new DefaultAttributeContainer();
                    ibaHolder.setAttributeContainer(defaultattributecontainer);
                }
                BooleanValueDefaultView abstractvaluedefaultview = (BooleanValueDefaultView) getIBAValueView(defaultattributecontainer, ibaName, ibaClass);
                if (abstractvaluedefaultview != null) {
                    abstractvaluedefaultview.setValue(newValue);
                    defaultattributecontainer.updateAttributeValue(abstractvaluedefaultview);
                } else {
                    AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
                    BooleanValueDefaultView abstractvaluedefaultview1 = new BooleanValueDefaultView((BooleanDefView) attributedefdefaultview, newValue);
                    defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);
                }
                ibaHolder.setAttributeContainer(defaultattributecontainer);
                StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
                ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    /**
     * Set IBA Integer Value
     *
     * @param obj
     * @param ibaName: iba name
     * @param newValue: iba value
     * @exception WTException
     */
    public static void setIBAIntegerValue(WTObject obj, String ibaName, int newValue) throws WTException {
        String ibaClass = "wt.iba.definition.IntegerDefinition";
        try {
            if (obj instanceof IBAHolder) {
                IBAHolder ibaHolder = (IBAHolder) obj;
                DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
                if (defaultattributecontainer == null) {
                    defaultattributecontainer = new DefaultAttributeContainer();
                    ibaHolder.setAttributeContainer(defaultattributecontainer);
                }
                IntegerValueDefaultView abstractvaluedefaultview = (IntegerValueDefaultView) getIBAValueView(defaultattributecontainer, ibaName, ibaClass);
                if (abstractvaluedefaultview != null) {
                    abstractvaluedefaultview.setValue(newValue);
                    defaultattributecontainer.updateAttributeValue(abstractvaluedefaultview);
                } else {
                    AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
                    IntegerValueDefaultView abstractvaluedefaultview1 = new IntegerValueDefaultView((IntegerDefView) attributedefdefaultview, newValue);
                    defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);
                }
                ibaHolder.setAttributeContainer(defaultattributecontainer);
                StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
                ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set IBA Float Value
     *
     * @param theObject
     * @param theAttribute
     * @param theValue
     * @exception WTException
     */
    public static void setIBAFloatValue(WTObject theObject, String theAttribute, String theValue) throws WTException {
        IBAHolder ibaHolder = null;
        theValue = theValue.trim();
        double theFloatValue = 0.0D;
        theFloatValue = Double.valueOf(theValue).doubleValue();

        try {
            // get attribute container
            ibaHolder = (IBAHolder) theObject;
            DefaultAttributeContainer attributeContainer = (DefaultAttributeContainer) ibaHolder.getAttributeContainer();

            if (attributeContainer == null) {
                ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, null, null, null);
                attributeContainer = (DefaultAttributeContainer) ibaHolder.getAttributeContainer();
            }

            // Get attribute definition
            StandardIBADefinitionService defService = new StandardIBADefinitionService();
            AttributeDefDefaultView attributeDefinition = null;
            FloatDefView floatAttrDefinition = null;
            attributeDefinition = defService.getAttributeDefDefaultViewByPath(theAttribute);
            if (!(attributeDefinition instanceof FloatDefView)) {
                throw new WTException("IBA " + theAttribute + " is not of type Float");
            }

            floatAttrDefinition = (FloatDefView) attributeDefinition;

            // Check if the attribute is already defined
            AbstractValueView[] abstractValueView = null;
            abstractValueView = attributeContainer.getAttributeValues(floatAttrDefinition);
            if (abstractValueView.length == 0) {
                // Add new attribute value
                FloatValueDefaultView attrValue = new FloatValueDefaultView(floatAttrDefinition, theFloatValue, wt.clients.widgets.NumericToolkit.countSigFigs(theValue));
                attributeContainer.addAttributeValue(attrValue);
            } else {
                // Update current attribute value
                FloatValueDefaultView attrValue = (FloatValueDefaultView) abstractValueView[0];
                attrValue.setValue(theFloatValue);
                attributeContainer.updateAttributeValue(attrValue);
            }
            // Update IBAHolder
            ibaHolder.setAttributeContainer(attributeContainer);
            StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
            ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
        } catch (RemoteException rem) {
            throw new WTException(rem);
        } catch (WTPropertyVetoException wpve) {
            throw new WTException(wpve);
        }

        // Return IBAHolder
        //return ibaHolder;
    }

    /*

    public static void setIBAFloatValue(WTObject obj, String ibaName,float newValue ) throws WTException{
    String ibaClass = "wt.iba.definition.FloatDefinition";
    try{
    if(obj instanceof IBAHolder){
    IBAHolder ibaHolder = (IBAHolder)obj;
    DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
    if(defaultattributecontainer == null){
    defaultattributecontainer = new DefaultAttributeContainer();
    ibaHolder.setAttributeContainer(defaultattributecontainer);
    }

    String strFloatValue = String.valueOf(newValue);
    StringTokenizer st = new StringTokenizer(strFloatValue,".");
    System.out.println();
    int iFloatLength = 0;
    if(st.hasMoreElements()){
    st.nextElement();
    if(st.hasMoreElements()){
    iFloatLength = ((String)st.nextElement()).length();
    }
    }
    iFloatLength = iFloatLength+ 1;

    FloatValueDefaultView abstractvaluedefaultview = (FloatValueDefaultView)getIBAValueView(defaultattributecontainer, ibaName, ibaClass);
    if(abstractvaluedefaultview != null){
    abstractvaluedefaultview.setValue(newValue);
    abstractvaluedefaultview.setPrecision(iFloatLength);
    defaultattributecontainer.updateAttributeValue(abstractvaluedefaultview);
    }else{
    AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
    FloatValueDefaultView abstractvaluedefaultview1 = new FloatValueDefaultView((FloatDefView)attributedefdefaultview,newValue,iFloatLength);
    defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);
    }
    ibaHolder.setAttributeContainer(defaultattributecontainer);
    StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
    ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
    }
    }catch(Exception exception){
    exception.printStackTrace();
    }
    }
     */
    /**
     * Set IBA Ratio Value
     *
     * @param obj
     * @param ibaName: iba name
     * @param newValue: double value
     * @exception WTException
     */
    public static void setIBARatioValue(WTObject obj, String ibaName, double newValue) throws WTException {
        String ibaClass = "wt.iba.definition.RatioDefinition";
        try {
            if (obj instanceof IBAHolder) {
                IBAHolder ibaHolder = (IBAHolder) obj;
                DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
                if (defaultattributecontainer == null) {
                    defaultattributecontainer = new DefaultAttributeContainer();
                    ibaHolder.setAttributeContainer(defaultattributecontainer);
                }
                RatioValueDefaultView abstractvaluedefaultview = (RatioValueDefaultView) getIBAValueView(defaultattributecontainer, ibaName, ibaClass);
                if (abstractvaluedefaultview != null) {
                    abstractvaluedefaultview.setValue(newValue);

                    defaultattributecontainer.updateAttributeValue(abstractvaluedefaultview);
                } else {
                    AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
                    RatioValueDefaultView abstractvaluedefaultview1 = new RatioValueDefaultView((RatioDefView) attributedefdefaultview);
                    defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);
                }
                ibaHolder.setAttributeContainer(defaultattributecontainer);
                StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
                ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set IBA Timestamp Value
     *
     * @param obj
     * @param ibaName: iba name
     * @param newValue: timestamp value
     * @exception WTException
     */
    public static void setIBATimestampValue(WTObject obj, String ibaName, Timestamp newValue) throws WTException {
        String ibaClass = "wt.iba.definition.TimestampDefinition";
        try {
            if (obj instanceof IBAHolder) {
                IBAHolder ibaHolder = (IBAHolder) obj;
                DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
                if (defaultattributecontainer == null) {
                    defaultattributecontainer = new DefaultAttributeContainer();
                    ibaHolder.setAttributeContainer(defaultattributecontainer);
                }
                TimestampValueDefaultView abstractvaluedefaultview = (TimestampValueDefaultView) getIBAValueView(defaultattributecontainer, ibaName, ibaClass);
                if (abstractvaluedefaultview != null) {
                    abstractvaluedefaultview.setValue(newValue);

                    defaultattributecontainer.updateAttributeValue(abstractvaluedefaultview);
                } else {
                    AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
                    TimestampValueDefaultView abstractvaluedefaultview1 = new TimestampValueDefaultView((TimestampDefView) attributedefdefaultview, newValue);
                    defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);
                }
                ibaHolder.setAttributeContainer(defaultattributecontainer);
                StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
                ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set IBA URL Value
     *
     * @param obj
     * @param ibaName: iba name
     * @param newValue: URL string
     * @exception WTException
     */
    public static void setIBAURLValue(WTObject obj, String ibaName, String newValue) throws WTException {
        String ibaClass = "wt.iba.definition.URLDefinition";

        try {
            StringTokenizer st = new StringTokenizer(newValue, "$$$");
            String urlValue = "";
            String urlDesc = "";
            while (st.hasMoreElements()) {
                urlValue = st.nextToken();
                if (st.hasMoreElements()) {
                    urlDesc = st.nextToken();
                }
            }
            if (obj instanceof IBAHolder) {
                IBAHolder ibaHolder = (IBAHolder) obj;
                DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
                if (defaultattributecontainer == null) {
                    defaultattributecontainer = new DefaultAttributeContainer();
                    ibaHolder.setAttributeContainer(defaultattributecontainer);
                }
                URLValueDefaultView abstractvaluedefaultview = (URLValueDefaultView) getIBAValueView(defaultattributecontainer, ibaName, ibaClass);
                if (abstractvaluedefaultview != null) {
                    abstractvaluedefaultview.setValue(urlValue);
                    abstractvaluedefaultview.setDescription(urlDesc);
                    defaultattributecontainer.updateAttributeValue(abstractvaluedefaultview);
                } else {
                    AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
                    URLValueDefaultView abstractvaluedefaultview1 = new URLValueDefaultView((URLDefView) attributedefdefaultview, urlValue, urlDesc);
                    defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);
                }
                ibaHolder.setAttributeContainer(defaultattributecontainer);
                StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
                ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set IBA URL Values
     *
     * @param obj
     * @param ibaName: iba name
     * @param newValue: URL string array
     * @exception WTException
     * @author Allen Wang
     */
    public static void setIBAURLValues(WTObject obj, String ibaName, String[] newValue) throws WTException {
        String oneNewValue = "";
        try {
            for (int i = 0; i < newValue.length; i++) {
                oneNewValue = newValue[i];
                StringTokenizer st = new StringTokenizer(oneNewValue, "$$$");
                String urlValue = "";
                String urlDesc = "";
                while (st.hasMoreElements()) {
                    urlValue = st.nextToken();
                    if (st.hasMoreElements()) {
                        urlDesc = st.nextToken();
                    }
                }
                if (obj instanceof IBAHolder) {
                    IBAHolder ibaHolder = (IBAHolder) obj;
                    DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
                    if (defaultattributecontainer == null) {
                        defaultattributecontainer = new DefaultAttributeContainer();
                        ibaHolder.setAttributeContainer(defaultattributecontainer);
                    }

                    AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
                    URLValueDefaultView abstractvaluedefaultview1 = new URLValueDefaultView((URLDefView) attributedefdefaultview, urlValue, urlDesc);
                    defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);

                    ibaHolder.setAttributeContainer(defaultattributecontainer);
                    StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
                    ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set IBA Unit Value
     *
     * @param obj
     * @param ibaName: iba name
     * @param newValue: double value
     * @exception WTException
     */
    public static void setIBAUnitValue(WTObject obj, String ibaName, double newValue) throws WTException {
        String ibaClass = "wt.iba.definition.UnitDefinition";
        try {
            if (obj instanceof IBAHolder) {
                IBAHolder ibaHolder = (IBAHolder) obj;
                DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
                if (defaultattributecontainer == null) {
                    defaultattributecontainer = new DefaultAttributeContainer();
                    ibaHolder.setAttributeContainer(defaultattributecontainer);
                }
                UnitValueDefaultView abstractvaluedefaultview = (UnitValueDefaultView) getIBAValueView(defaultattributecontainer, ibaName, ibaClass);

                String strFloatValue = String.valueOf(newValue);
                StringTokenizer st = new StringTokenizer(strFloatValue, ".");

                int iFloatLength = 0;
                if (st.hasMoreElements()) {
                    st.nextElement();
                    if (st.hasMoreElements()) {
                        iFloatLength = ((String) st.nextElement()).length();
                    }
                }
                iFloatLength = iFloatLength + 1;
                if (abstractvaluedefaultview != null) {
                    abstractvaluedefaultview.setValue(newValue);
                    abstractvaluedefaultview.setPrecision(iFloatLength);
                    defaultattributecontainer.updateAttributeValue(abstractvaluedefaultview);
                } else {
                    AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
                    UnitValueDefaultView abstractvaluedefaultview1 = new UnitValueDefaultView((UnitDefView) attributedefdefaultview, newValue, iFloatLength);
                    defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);
                }
                ibaHolder.setAttributeContainer(defaultattributecontainer);
                StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
                ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set IBA Any Value
     *
     * @param obj
     * @param ibaName: iba name
     * @param newValue: iba value
     * @exception WTException, RemoteException, WTPropertyVetoException, ParseException
     */
     static void setIBAAnyValue(WTObject obj, String ibaName, String newValue) throws WTException, RemoteException, WTPropertyVetoException, ParseException {

        AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
        IBAHolder ibaholder = (IBAHolder) obj;

        String ibaClass = "";
        if (attributedefdefaultview instanceof FloatDefView) {
            ibaClass = "wt.iba.definition.FloatDefinition";
        } else if (attributedefdefaultview instanceof StringDefView) {
            ibaClass = "wt.iba.definition.StringDefinition";
        } else if (attributedefdefaultview instanceof IntegerDefView) {
            ibaClass = "wt.iba.definition.IntegerDefinition";
        } else if (attributedefdefaultview instanceof RatioDefView) {
            ibaClass = "wt.iba.definition.RatioDefinition";
        } else if (attributedefdefaultview instanceof TimestampDefView) {
            ibaClass = "wt.iba.definition.TimestampDefinition";
        } else if (attributedefdefaultview instanceof BooleanDefView) {
            ibaClass = "wt.iba.definition.BooleanDefinition";
        } else if (attributedefdefaultview instanceof URLDefView) {
            ibaClass = "wt.iba.definition.URLDefinition";
        } else if (attributedefdefaultview instanceof ReferenceDefView) {
            ibaClass = "wt.iba.definition.ReferenceDefinition";
        } else if (attributedefdefaultview instanceof UnitDefView) {
            ibaClass = "wt.iba.definition.UnitDefinition";
        }

        // store the new iteration (this will copy forward the obsolete set of IBA values in the database)
        //ibaholder = (IBAHolder)PersistenceHelper.manager.store( (Persistable)ibaholder );

        // load IBA values from DB (because obsolete IBA values have
        // been copied forward to new iteration by IBA persistence event handlers)
        ibaholder = IBAValueHelper.service.refreshAttributeContainer(ibaholder, "CSM", null, null);

        // clear the container to remove all obsolete IBA values and persist this
        // to remove IBA values from database
        //*deleteAllIBAValues(ibaholder );
        ibaholder = IBAValueHelper.service.refreshAttributeContainer(ibaholder, null, SessionHelper.manager.getLocale(), null);
        DefaultAttributeContainer defaultattributecontainer = (DefaultAttributeContainer) (ibaholder).getAttributeContainer();

        //    System.out.println("CLASS IS " + ibaClass);
        //AbstractValueView abstractvalueview = getIBAValueView(defaultattributecontainer,ibaName,ibaClass);
        Vector vAbstractvalueview = getIBAValueViews(defaultattributecontainer, ibaName, ibaClass);
        //if (abstractvalueview != null){
        for (int i = 0; i < vAbstractvalueview.size(); i++) {
            AbstractValueView abstractvalueview = (AbstractValueView) vAbstractvalueview.get(i);
            defaultattributecontainer.deleteAttributeValue(abstractvalueview);
            // save the new iteration with the updated set of IBA values
            //ibaholder = (IBAHolder)PersistenceHelper.manager.save( (Persistable)ibaholder );
            StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaholder, null, null, null);
            ibaholder = IBAValueHelper.service.refreshAttributeContainer(ibaholder, "CSM", null, null);
        }

        if (!newValue.equals("")) {
            if (attributedefdefaultview instanceof FloatDefView) {
                //setIBAFloatValue(obj, ibaName, Float.parseFloat(newValue));
                setIBAFloatValue(obj, ibaName, newValue);
                // System.out.println("setIBAFloatValue");
            } else if (attributedefdefaultview instanceof StringDefView) {
                if (newValue.contains("||")) {
                    String[] newMultiString = StringUtils.split(newValue,"||");//newValue.split("\\|\\|");
                    setIBAStringValues(obj, ibaName, newMultiString);
                    //  System.out.println("setIBAStringMultiValue");
                } else {
                    setIBAStringValue(obj, ibaName, newValue);
                    //   System.out.println("setIBAStringValue");
                }
            } else if (attributedefdefaultview instanceof IntegerDefView) {
                setIBAIntegerValue(obj, ibaName, Integer.parseInt(newValue));
                //   System.out.println("setIBAIntegerValue");
            } else if (attributedefdefaultview instanceof RatioDefView) {
                setIBARatioValue(obj, ibaName, Double.parseDouble(newValue));
                //  System.out.println("setIBARatioValue");
            } else if (attributedefdefaultview instanceof TimestampDefView) {
                if (!newValue.contains(":")) {
                    newValue = newValue + " 00:00:00";
                }
                /*
                String format = "yyyy-MM-dd HH:mm:ss";
                if (SessionHelper.manager.getLocale().toString().equals("zh_CN") || SessionHelper.manager.getLocale().toString().equals("zh_TW")) {
                format = "yyyy/MM/dd HH:mm:ss";

                }
                 *
                 */
                String format = "";
                if (newValue.indexOf("-") > 0) {
                    format = "yyyy-MM-dd HH:mm:ss";
                } else {
                    format = "yyyy/MM/dd HH:mm:ss";
                }
                //     System.out.println("@@@@@@@ format = " + format);
                java.text.SimpleDateFormat formats = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                java.text.SimpleDateFormat formatSource = new java.text.SimpleDateFormat(format);
                setIBATimestampValue(obj, ibaName, Timestamp.valueOf(formats.format(formatSource.parse(newValue))));

                //   System.out.println("setIBATimestampValue");
            } else if (attributedefdefaultview instanceof BooleanDefView) {
                setIBABooleanValue(obj, ibaName, Boolean.parseBoolean(newValue));
                //    System.out.println("setIBABooleanValue");
            } else if (attributedefdefaultview instanceof URLDefView) {
                setIBAURLValue(obj, ibaName, newValue);
                //      System.out.println("setIBAURLValue");
            } else if (attributedefdefaultview instanceof ReferenceDefView) {
                //      System.out.println("ReferenceDefView");
            } else if (attributedefdefaultview instanceof UnitDefView) {
                setIBAUnitValue(obj, ibaName, Double.parseDouble(newValue));
                //     System.out.println("setIBAUnitValue");
            }
        }
    }

    /**
     * Add an IBA value with dependency relation
     * @param sourcename
     * @param sourcevalue
     * @param businessname
     * @param businessvalue
     * @throws IBAValueException
     * @throws WTPropertyVetoException
     * @throws WTException
     */
    public void addIBAValue(String sourcename, String sourcevalue, String businessname, String businessvalue) throws IBAValueException, WTPropertyVetoException, WTException {
        AttributeDefDefaultView sourceDef = getDefDefaultView(sourcename);
        AttributeDefDefaultView businessDef = getDefDefaultView(businessname);
        AbstractValueView sourceValue = getAbstractValueView(sourceDef, sourcevalue);
        AbstractValueView businessValue = getAbstractValueView(businessDef, businessvalue);
        sourceValue = setDependency(sourceDef, sourceValue, businessDef, businessValue);

        Object[] obj = (Object[]) ibaContainer.get(sourcename);
        Object[] temp;
        if (obj == null) {
            temp = new Object[2];
            temp[0] = sourceDef;
            temp[1] = sourceValue;
        } else {
            temp = new Object[obj.length + 1];
            int i;
            for (i = 0; i < obj.length; i++) {
                temp[i] = obj[i];
            }
            // �[�J�s����
            temp[i] = sourceValue;
        }
        ibaContainer.put(sourcename, temp);
    }

    /**
     * initializePart() with this signature is designed to pre-populate values from an existing IBA holder.
     *
     * @exception WTException, RemoteException
     */
    private void initializeIBAValue(IBAHolder ibaHolder) throws WTException, RemoteException {
        ibaContainer = new Hashtable();
        ibaOrigContainer = new Hashtable();
        if (ibaHolder.getAttributeContainer() == null) {
            ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, null, SessionHelper.manager.getLocale(), null);
        }
        DefaultAttributeContainer theContainer = (DefaultAttributeContainer) ibaHolder.getAttributeContainer();
        if (theContainer != null) {
            AttributeDefDefaultView[] theAtts = theContainer.getAttributeDefinitions();
            for (int i = 0; i < theAtts.length; i++) {
                AbstractValueView[] theValues = theContainer.getAttributeValues(theAtts[i]);
                if (theValues != null) {
                    //Add by Somesh
                    Object[] temp = new Object[theValues.length + 1];
                    temp[0] = theAtts[i];
                    for (int j = 1; j <= theValues.length; j++) {
                        temp[j] = theValues[j - 1];
                    }
                    //End Add by Somesh
                    ibaContainer.put(theAtts[i].getName(), temp);
                    ibaOrigContainer.put(theAtts[i].getName(), temp);
                }
            }
        }
        //System.out.println("initializeIBAValue : ibaContainer = " + ibaContainer);
        //System.out.println("initializeIBAValue : ibaOrigContainer = " + ibaOrigContainer);
    }

    /**
     * suppressCSMConstraint
     *
     * @param theContainer
     * @param s
     * @return DefaultAttributeContainer
     * @exception WTException
     */
    private DefaultAttributeContainer suppressCSMConstraint(DefaultAttributeContainer theContainer, String s) throws WTException {
        //rjla 2000-11-17
        //If the part classification IBA is to be updated, we must prevent the CSM constraint
        //that makes it immutable from being applied.
        //Note that suppressing this constraint here - as the container is updated - does not
        //remove the need to suppress it again when the updated part is parted; in other words
        //you still need RemoveCSMConstraint line.
        //AttributeDefDefaultView definitions[] = theContainer.getAttributeDefinitions();
        ClassificationStructDefaultView defStructure = null;
        //defStructure = ClassificationHelper.service.getClassificationStructDefaultView("wt.csm.businessentity.BusinessEntity");
        defStructure = getClassificationStructDefaultViewByName(s);
        if (defStructure != null) {
            //ReferenceDefView ref = defStructure.getReferenceDefView();
            Vector cgs = theContainer.getConstraintGroups();
            Vector newCgs = new Vector();
            //AttributeConstraint immutable = null;
            try {
                //if (VERBOSE)
                //    System.out.println("cgs size="+cgs.size());
                for (int i = 0; i < cgs.size(); i++) {
                    ConstraintGroup cg = (ConstraintGroup) cgs.elementAt(i);
                    if (cg != null) {
                        //System.out.println(cg.getConstraintGroupLabel());
                        if (!cg.getConstraintGroupLabel().equals(wt.csm.constraint.CSMConstraintFactory.CONSTRAINT_GROUP_LABEL)) {
                            newCgs.addElement(cg);
                        } else {
                            //Enumeration enum = cg.getConstraints();
                            ConstraintGroup newCg = new ConstraintGroup();
                            newCg.setConstraintGroupLabel(cg.getConstraintGroupLabel());
                            /*
                            while (enum.hasMoreElements()){
                            AttributeConstraint ac = (AttributeConstraint)enum.nextElement();
                            if ((ac.appliesToAttrDef(ref)) && (ac.getValueConstraint() instanceof wt.iba.constraint.Immutable)){
                            immutable = ac;
                            } else {
                            newCg.addConstraint(ac);
                            }
                            } */

                            newCgs.addElement(newCg);
                        }
                    }
                }
                theContainer.setConstraintGroups(newCgs);
            } catch (wt.util.WTPropertyVetoException e) {
                e.printStackTrace();
            }
        }
        //end of CSM constraint removal, rjla 2000-11-17
        return theContainer;
    }

    /**
     * removeCSMConstraint
     *
     * @param attributecontainer
     * @return DefaultAttributeContainer
     */
    private DefaultAttributeContainer removeCSMConstraint(DefaultAttributeContainer attributecontainer) {
        Object obj = attributecontainer.getConstraintParameter();
        if (obj == null) {
            obj = new String("CSM");
        } else if (obj instanceof Vector) {
            ((Vector) obj).addElement(new String("CSM"));
        } else {
            Vector vector1 = new Vector();
            vector1.addElement(obj);
            obj = vector1;
            ((Vector) obj).addElement(new String("CSM"));
        }
        try {
            attributecontainer.setConstraintParameter(obj);
        } catch (WTPropertyVetoException wtpropertyvetoexception) {
            wtpropertyvetoexception.printStackTrace();

        }
        return attributecontainer;
    }

    /**
     * Update the IBAHolder's attribute container from the hashtable
     * @param ibaHolder
     * @return
     * @throws WTException
     * @throws WTPropertyVetoException
     * @throws RemoteException
     */
    public IBAHolder updateAttributeContainer(IBAHolder ibaHolder) throws WTException, WTPropertyVetoException, RemoteException {
        if (ibaHolder.getAttributeContainer() == null) {
            ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, null, SessionHelper.manager.getLocale(), null);
        }
        DefaultAttributeContainer defaultattributecontainer = (DefaultAttributeContainer) ibaHolder.getAttributeContainer();

        defaultattributecontainer = suppressCSMConstraint(defaultattributecontainer, getIBAHolderClassName(ibaHolder));

        AttributeDefDefaultView[] theAtts = defaultattributecontainer.getAttributeDefinitions();
        // Delete existed iba if they aren't in the hashtable of this class
        for (int i = 0; i < theAtts.length; i++) {
            AttributeDefDefaultView theDef = theAtts[i];
            if (ibaContainer.get(theDef.getName()) == null) {
                createOrUpdateAttributeValuesInContainer(defaultattributecontainer, theDef, null);
            }
        }

        // Update the iba from the hashtable to the attribute container of the iba holder
        /*
        Enumeration enum = ibaContainer.elements();
        while ( enum.hasMoreElements() ) {
        Object[] temp = (Object[]) enum.nextElement();
        AttributeDefDefaultView theDef = (AttributeDefDefaultView) temp[0];
        if (theDef instanceof ReferenceDefView) {
        AbstractValueView abstractvalueviews[] = new AbstractValueView[temp.length-1];
        for (int i=0; i<temp.length-1; i++) {
        abstractvalueviews[i] = (AbstractValueView)temp[i+1];
        }
        createOrUpdateAttributeValuesInContainer(defaultattributecontainer, theDef, abstractvalueviews);
        }
        }
         */

        Enumeration enum1 = ibaContainer.elements();
        while (enum1.hasMoreElements()) {
            Object[] temp = (Object[]) enum1.nextElement();
            AttributeDefDefaultView theDef = (AttributeDefDefaultView) temp[0];
            AbstractValueView abstractvalueviews[] = new AbstractValueView[temp.length - 1];
            for (int i = 0; i < temp.length - 1; i++) {
                abstractvalueviews[i] = (AbstractValueView) temp[i + 1];
            }
            createOrUpdateAttributeValuesInContainer(defaultattributecontainer, theDef, abstractvalueviews);
        }

        defaultattributecontainer = removeCSMConstraint(defaultattributecontainer);
        ibaHolder.setAttributeContainer(defaultattributecontainer);

        return ibaHolder;
    }

    /**
     * updateAttributeContainer
     *
     * @param  ibaHolder
     * @param flag1
     * @return IBAHolder
     * @exception WTException, WTPropertyVetoException, RemoteException
     */
    public IBAHolder updateAttributeContainer(IBAHolder ibaHolder, boolean flag1) throws WTException, WTPropertyVetoException, RemoteException {
        if (ibaHolder.getAttributeContainer() == null) {
            ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, null, SessionHelper.manager.getLocale(), null);
        }
        DefaultAttributeContainer defaultattributecontainer = (DefaultAttributeContainer) ibaHolder.getAttributeContainer();
        defaultattributecontainer = suppressCSMConstraint(defaultattributecontainer, getIBAHolderClassName(ibaHolder));
        Enumeration enum1 = ibaContainer.elements();
        while (enum1.hasMoreElements()) {
            Object[] temp = (Object[]) enum1.nextElement();
            AttributeDefDefaultView theDef = (AttributeDefDefaultView) temp[0];
            for (int j = 1; j < temp.length; j++) {
                AbstractValueView abstractvalueview = (AbstractValueView) temp[j];
                int state = abstractvalueview.getState();
                if (VERBOSE) {
                    System.out.println(theDef.getLocalizedDisplayString() + "=" + state);
                }
                if (state == AbstractValueView.NEW_STATE || state == AbstractValueView.CHANGED_STATE) {
                    LoadValue.createOrUpdateAttributeValueInContainer(defaultattributecontainer, abstractvalueview);
                } else {
                    if (state == AbstractValueView.DELETED_STATE) {
                        if (defaultattributecontainer.isDeleteValid(abstractvalueview)) {
                            defaultattributecontainer.deleteAttributeValue(abstractvalueview);
                        }
                    }
                }
            }
        }
        defaultattributecontainer = removeCSMConstraint(defaultattributecontainer);
        ibaHolder.setAttributeContainer(defaultattributecontainer);
        return ibaHolder;
    }

    /**
     * Update without checkout/checkin
     * @param ibaholder
     * @return
     */
    public boolean updateIBAHolder(IBAHolder ibaholder) throws Exception {
        IBAValueDBService ibavaluedbservice = new IBAValueDBService();
        boolean flag = true;
        try {
            PersistenceServerHelper.manager.update((Persistable) ibaholder);
            wt.iba.value.AttributeContainer attributecontainer = (wt.iba.value.AttributeContainer) ibaholder.getAttributeContainer();
            Object obj = ((DefaultAttributeContainer) attributecontainer).getConstraintParameter();
            wt.iba.value.AttributeContainer attributecontainer1 = (wt.iba.value.AttributeContainer) ibavaluedbservice.updateAttributeContainer(ibaholder, obj, null, null);
            ibaholder.setAttributeContainer((wt.iba.value.AttributeContainer) attributecontainer1);
        } catch (WTException e) {
            System.out.println("updateIBAHOlder: Couldn't update. " + e);
            flag = false;
            throw new WTException(e.toString());
        }
        return flag;
    }

    /**
     * update IBAHolder
     *
     * @param ibaholder
     * @param flag
     * @return IBAHolder
     * @exception Exception
     */
    public IBAHolder updateIBAHolder(IBAHolder ibaholder, boolean flag) throws Exception {
        if (flag) {
            // true : Update IBAHolder Without Constraints
            //ibaholder = IBAValueHelper.service.refreshAttributeContainerWithoutConstraints((IBAHolder)ibaholder);
            return LoadValue.applySoftAttributes(ibaholder);
        } else {
            // false : Update IBAHolder Check Constraints
            IBAValueDBService ibavaluedbservice = new IBAValueDBService();
            try {
                PersistenceServerHelper.manager.update((Persistable) ibaholder);
                wt.iba.value.AttributeContainer attributecontainer = (wt.iba.value.AttributeContainer) ibaholder.getAttributeContainer();
                Object obj = ((DefaultAttributeContainer) attributecontainer).getConstraintParameter();
                wt.iba.value.AttributeContainer attributecontainer1 = (wt.iba.value.AttributeContainer) ibavaluedbservice.updateAttributeContainer(ibaholder, obj, null, null);
                ibaholder.setAttributeContainer((wt.iba.value.AttributeContainer) attributecontainer1);
            } catch (WTException e) {
                System.out.println("updateIBAHOlder: Couldn't update. " + e);
                throw new WTException(e.toString());
            }
            return ibaholder;
        }
    }

    /**
     * suppressCSMConstraint
     *
     * @param theContainer
     * @return DefaultAttributeContainer
     */
    public static DefaultAttributeContainer suppressCSMConstraint(DefaultAttributeContainer theContainer) {
        AttributeDefDefaultView definitions[] = theContainer.getAttributeDefinitions();
        String classToUpdate = "wt.part.WTPart";
        ClassificationStructDefaultView defStructure = null;
        try {
            defStructure = ClassificationHelper.service.getClassificationStructDefaultView(classToUpdate);
        } catch (Exception e) {
            //ignore exception - but don't try to use result
        }
        if (defStructure != null) {
            ReferenceDefView ref = defStructure.getReferenceDefView();
            Vector cgs = theContainer.getConstraintGroups();
            Vector newCgs = new Vector();
            AttributeConstraint immutable = null;
            try {
                for (int i = 0; i < cgs.size(); i++) {
                    ConstraintGroup cg = (ConstraintGroup) cgs.elementAt(i);
                    if (cg != null) {
                        if (!cg.getConstraintGroupLabel().equals(wt.csm.constraint.CSMConstraintFactory.CONSTRAINT_GROUP_LABEL)) {
                            newCgs.addElement(cg);
                        } else {
                            Enumeration enume = cg.getConstraints();
                            ConstraintGroup newCg = new ConstraintGroup();
                            newCg.setConstraintGroupLabel(cg.getConstraintGroupLabel());
                            while (enume.hasMoreElements()) {
                                AttributeConstraint ac = (AttributeConstraint) enume.nextElement();
                                if ((ac.appliesToAttrDef(ref)) && (ac.getValueConstraint() instanceof wt.iba.constraint.Immutable)) {
                                    immutable = ac;
                                } else {
                                    newCg.addConstraint(ac);
                                }
                            }
                            newCgs.addElement(newCg);
                        }
                    }
                }
                theContainer.setConstraintGroups(newCgs);
            } catch (wt.util.WTPropertyVetoException e) {
                e.printStackTrace();
            }
        }
        //end of CSM constraint
        return theContainer;
    }

    /**
     * Referenced from method "createOrUpdateAttributeValueInContainer" of wt.iba.value.service.LoadValue.java -> modified to have multi-values support
     * @param defaultattributecontainer
     * @param theDef
     * @param abstractvalueviews
     * @throws WTException
     */
    private void createOrUpdateAttributeValuesInContainer(DefaultAttributeContainer defaultattributecontainer, AttributeDefDefaultView theDef, AbstractValueView[] abstractvalueviews) throws WTException, WTPropertyVetoException {
        if (defaultattributecontainer == null) {
            throw new IBAContainerException("wt.iba.value.service.LoadValue.createOrUpdateAttributeValueInContainer :  DefaultAttributeContainer passed in is null!");
        }
        AbstractValueView abstractvalueviews0[] = defaultattributecontainer.getAttributeValues(theDef);
        try {
            if (abstractvalueviews0 == null || abstractvalueviews0.length == 0) {
                // Original valus is empty
                for (int j = 0; j < abstractvalueviews.length; j++) {
                    AbstractValueView abstractvalueview = abstractvalueviews[j];
                    defaultattributecontainer.addAttributeValue(abstractvalueview);
                    //System.out.println("IBAUtil:"+abstractvalueview.getLocalizedDisplayString()+" in "+abstractvalueview.getDefinition().getName());
                }
            } else if (abstractvalueviews == null || abstractvalueviews.length == 0) {
                // New value is empty, so delete all existed values
                for (int j = 0; j < abstractvalueviews0.length; j++) {
                    AbstractValueView abstractvalueview = abstractvalueviews0[j];
                    defaultattributecontainer.deleteAttributeValue(abstractvalueview);
                }
            } else if (abstractvalueviews0.length <= abstractvalueviews.length) {

                // More new valuss than (or equal to) original values,
                // So update existed values and add new values
                for (int j = 0; j < abstractvalueviews0.length; j++) {
                    abstractvalueviews0[j] = LoadValue.cloneAbstractValueView(abstractvalueviews[j], abstractvalueviews0[j]);
                    //abstractvalueviews0[j] = abstractvalueviews[j];
                    abstractvalueviews0[j] = cloneReferenceValueDefaultView(abstractvalueviews[j], abstractvalueviews0[j]);

                    defaultattributecontainer.updateAttributeValue(abstractvalueviews0[j]);
                }
                for (int j = abstractvalueviews0.length; j < abstractvalueviews.length; j++) {
                    AbstractValueView abstractvalueview = abstractvalueviews[j];
                    //abstractvalueview.setState(AbstractValueView.CHANGED_STATE);
                    defaultattributecontainer.addAttributeValue(abstractvalueview);
                }
            } else if (abstractvalueviews0.length > abstractvalueviews.length) {
                // Less new values than original values,
                // So delete some values
                for (int j = 0; j < abstractvalueviews.length; j++) {
                    abstractvalueviews0[j] = LoadValue.cloneAbstractValueView(abstractvalueviews[j], abstractvalueviews0[j]);
                    abstractvalueviews0[j] = cloneReferenceValueDefaultView(abstractvalueviews[j], abstractvalueviews0[j]);
                    //abstractvalueviews0[j] = abstractvalueviews[j];
                    defaultattributecontainer.updateAttributeValue(abstractvalueviews0[j]);
                }
                for (int j = abstractvalueviews.length; j < abstractvalueviews0.length; j++) {
                    AbstractValueView abstractvalueview = abstractvalueviews0[j];
                    defaultattributecontainer.deleteAttributeValue(abstractvalueview);
                }
            }
        } catch (IBAConstraintException ibaconstraintexception) {
            ibaconstraintexception.printStackTrace();
        }
    }

    // For dependency used.
    AbstractValueView cloneReferenceValueDefaultView(AbstractValueView abstractvalueview, AbstractValueView abstractvalueview1) throws IBAValueException {
        if (abstractvalueview instanceof AbstractContextualValueDefaultView) {
            if (VERBOSE) {
                System.out.println(abstractvalueview1.getLocalizedDisplayString() + ":" + abstractvalueview.getLocalizedDisplayString());
                if (((AbstractContextualValueDefaultView) abstractvalueview1).getReferenceValueDefaultView() != null) {
                    System.out.println("Key before set=" + ((AbstractContextualValueDefaultView) abstractvalueview1).getReferenceValueDefaultView().getKey());
                }
            }

            try {
                ((AbstractContextualValueDefaultView) abstractvalueview1).setReferenceValueDefaultView(((AbstractContextualValueDefaultView) abstractvalueview).getReferenceValueDefaultView());
            } catch (WTPropertyVetoException wtpropertyvetoexception) {
                throw new IBAValueException("can't get ReferenceValueDefaultView from the Part in the database");
            }
            if (VERBOSE) {
                if (((AbstractContextualValueDefaultView) abstractvalueview1).getReferenceValueDefaultView() != null) {
                    System.out.println("Key after set=" + ((AbstractContextualValueDefaultView) abstractvalueview1).getReferenceValueDefaultView().getKey());
                }
            }

        }
        return abstractvalueview1;
    }

    /**
     * another "black-box":  pass in a string, and get back an IBA value object.
     * Copy from wt.iba.value.service.LoadValue.java -> please don't modify this method
     * @param abstractattributedefinizerview
     * @param s
     * @param s1
     * @return
     */
    private static AbstractValueView internalCreateValue(AbstractAttributeDefinizerView abstractattributedefinizerview, String s, String s1) {
        AbstractValueView abstractvalueview = null;
        if (abstractattributedefinizerview instanceof FloatDefView) {
            abstractvalueview = LoadValue.newFloatValue(abstractattributedefinizerview, s, s1);
        } else if (abstractattributedefinizerview instanceof StringDefView) {
            abstractvalueview = LoadValue.newStringValue(abstractattributedefinizerview, s);
        } else if (abstractattributedefinizerview instanceof IntegerDefView) {
            abstractvalueview = LoadValue.newIntegerValue(abstractattributedefinizerview, s);
        } else if (abstractattributedefinizerview instanceof RatioDefView) {
            abstractvalueview = LoadValue.newRatioValue(abstractattributedefinizerview, s, s1);
        } else if (abstractattributedefinizerview instanceof TimestampDefView) {
            //System.out.println("Time Value:" + s);
            //System.out.println("Time Value indesof:" + s.indexOf("��"));
            if (s.indexOf("��") != -1) {
                String format = "yyyy/MM/dd a hh:mm:ss";
                //s = s.replaceAll("�W��", "am");
                //s = s.replaceAll("�U��", "pm");
                //DateFormat mediumFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                SimpleDateFormat formatter;
                formatter = new SimpleDateFormat("yyyy/MM/dd a hh:mm:ss");

                //java.text.SimpleDateFormat formats = new java.text.SimpleDateFormat("yyyy/MM/dd a KK:mm:ss");

                java.text.SimpleDateFormat formatSource = new java.text.SimpleDateFormat(format, Locale.TAIWAN);
                try {
                    java.text.ParsePosition pos = new java.text.ParsePosition(0);
                    java.util.Date ctime = formatSource.parse(s);
                    //java.util.Date ctime = formatter.parse(s, pos);
                    //System.out.println("ctime:" + ctime.toLocaleString());
                    java.sql.Timestamp dateTime = new java.sql.Timestamp(ctime.getTime());
                    dateTime.setHours(dateTime.getHours() - 8);
                    return newTimestampValue(abstractattributedefinizerview, dateTime);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }

                //s = Timestamp.valueOf(formats.format(formatSource.parse(s)));
                //setIBATimestampValue(obj, ibaName, Timestamp.valueOf(formats.format(formatSource.parse(newValue))));
            }
            //System.out.println("Time Update Value:" + s);
            abstractvalueview = LoadValue.newTimestampValue(abstractattributedefinizerview, s);
        } else if (abstractattributedefinizerview instanceof BooleanDefView) {
            abstractvalueview = LoadValue.newBooleanValue(abstractattributedefinizerview, s);
        } else if (abstractattributedefinizerview instanceof URLDefView) {
            abstractvalueview = LoadValue.newURLValue(abstractattributedefinizerview, s, s1);
        } else if (abstractattributedefinizerview instanceof ReferenceDefView) {
            abstractvalueview = LoadValue.newReferenceValue(abstractattributedefinizerview, s, s1);
        } else if (abstractattributedefinizerview instanceof UnitDefView) {
            abstractvalueview = LoadValue.newUnitValue(abstractattributedefinizerview, s, s1);
        }

        return abstractvalueview;
    }

    /**
     * newTimestampValue
     *
     * @param def
     * @param dateTime
     * @return AbstractValueView
     */
    public static AbstractValueView newTimestampValue(AbstractAttributeDefinizerView def, java.sql.Timestamp dateTime) {
        AbstractValueView value = null;

        try {
            value = new TimestampValueDefaultView((TimestampDefView) def, dateTime);
        } catch (IBAValueException e) {
            System.out.println("Can't create timestamp value");
            //logMsg("LoadVale newTimestampValue IBAValueException:" + e);
            System.out.println(e);
            value = null;
        }
        return value;

    }

    ////////////////////////////////////////////////////////////////////////////////
    /**
     * This method is a "black-box":  pass in a string, like "Electrical/Resistance/
     * ResistanceRating" and get back a IBA definition object.
     * @param ibaPath
     * @return
     */
    public AttributeDefDefaultView getAttributeDefinition9(String ibaPath) {

        AttributeDefDefaultView ibaDef = null;
        try {
            ibaDef = IBADefinitionHelper.service.getAttributeDefDefaultViewByPath(ibaPath);
            if (ibaDef == null) {
                AbstractAttributeDefinizerView ibaNodeView = DefinitionLoader.getAttributeDefinition(ibaPath);
                if (ibaNodeView != null) {
                    ibaDef = IBADefinitionHelper.service.getAttributeDefDefaultView((AttributeDefNodeView) ibaNodeView);
                }
            }
        } catch (Exception wte) {
            wte.printStackTrace();
        }

        return ibaDef;
    }

    /**
     * getDisplayUnits
     *
     * @param unitedfview
     * @return String
     */
    public static String getDisplayUnits(UnitDefView unitdefview) {
        return getDisplayUnits(unitdefview, UNITS);
    }

    /**
     * getDisplayUnits
     *
     * @param unitdefview
     * @param s
     * @return String
     */
    public static String getDisplayUnits(UnitDefView unitdefview, String s) {
        QuantityOfMeasureDefaultView quantityofmeasuredefaultview = unitdefview.getQuantityOfMeasureDefaultView();
        String s1 = quantityofmeasuredefaultview.getBaseUnit();
        if (s != null) {
            String s2 = unitdefview.getDisplayUnitString(s);
            if (s2 == null) {
                s2 = quantityofmeasuredefaultview.getDisplayUnitString(s);
            }
            if (s2 == null) {
                s2 = quantityofmeasuredefaultview.getDefaultDisplayUnitString(s);
            }
            if (s2 != null) {
                s1 = s2;
            }
        }
        if (s1 == null) {
            return "";
        } else {
            return s1;
        }
    }

    /**
     * getClassificationStructName
     *
     * @param ibaHolder
     * @return String
     * @exception IBAConstraintException
     */
    public static String getClassificationStructName(IBAHolder ibaHolder) throws IBAConstraintException {
        String s = getIBAHolderClassName(ibaHolder);
        ClassificationService classificationservice = ClassificationHelper.service;
        ClassificationStructDefaultView aclassificationstructdefaultview[] = null;
        try {
            aclassificationstructdefaultview = classificationservice.getAllClassificationStructures();
        } catch (RemoteException remoteexception) {
            remoteexception.printStackTrace();
            throw new IBAConstraintException(remoteexception);
        } catch (CSMClassificationNavigationException csmclassificationnavigationexception) {
            csmclassificationnavigationexception.printStackTrace();
            throw new IBAConstraintException(csmclassificationnavigationexception);
        } catch (WTException wtexception) {
            wtexception.printStackTrace();
            throw new IBAConstraintException(wtexception);
        }
        for (int i = 0; aclassificationstructdefaultview != null && i < aclassificationstructdefaultview.length; i++) {
            if (s.equals(aclassificationstructdefaultview[i].getPrimaryClassName())) {
                return s;
            }
        }

        try {
            for (Class class1 = Class.forName(s).getSuperclass(); !class1.getName().equals((wt.fc.WTObject.class).getName()) && !class1.getName().equals((java.lang.Object.class).getName()); class1 = class1.getSuperclass()) {
                for (int j = 0; aclassificationstructdefaultview != null && j < aclassificationstructdefaultview.length; j++) {
                    if (class1.getName().equals(aclassificationstructdefaultview[j].getPrimaryClassName())) {
                        return class1.getName();
                    }
                }

            }

        } catch (ClassNotFoundException classnotfoundexception) {
            classnotfoundexception.printStackTrace();
        }
        return null;
    }

    /**
     * Please refer to the method "getIBAHolderClassName" of class "wt.csm.constraint.CSMConstraintFactory"
     * @param ibaholder
     * @return
     */
    private static String getIBAHolderClassName(IBAHolder ibaholder) {
        String s = null;
        if (ibaholder instanceof AbstractLiteObject) {
            s = ((AbstractLiteObject) ibaholder).getHeavyObjectClassname();
        } else {
            s = ibaholder.getClass().getName();
        }
        return s;
    }

    /**
     * Please refer to the method "getClassificationStructDefaultViewByName" of class "wt.csm.constraint.CSMConstraintFactory"
     * @param s
     * @return
     * @throws IBAConstraintException
     */
    private ClassificationStructDefaultView getClassificationStructDefaultViewByName(String s) throws IBAConstraintException {
        ClassificationService classificationservice = ClassificationHelper.service;
        ClassificationStructDefaultView aclassificationstructdefaultview[] = null;
        try {
            aclassificationstructdefaultview = classificationservice.getAllClassificationStructures();
        } catch (RemoteException remoteexception) {
            remoteexception.printStackTrace();
            throw new IBAConstraintException(remoteexception);
        } catch (CSMClassificationNavigationException csmclassificationnavigationexception) {
            csmclassificationnavigationexception.printStackTrace();
            throw new IBAConstraintException(csmclassificationnavigationexception);
        } catch (WTException wtexception) {
            wtexception.printStackTrace();
            throw new IBAConstraintException(wtexception);
        }
        for (int i = 0; aclassificationstructdefaultview != null && i < aclassificationstructdefaultview.length; i++) {
            if (s.equals(aclassificationstructdefaultview[i].getPrimaryClassName())) {
                return aclassificationstructdefaultview[i];
            }
        }

        try {
            for (Class class1 = Class.forName(s).getSuperclass(); !class1.getName().equals((wt.fc.WTObject.class).getName()) && !class1.getName().equals((java.lang.Object.class).getName()); class1 = class1.getSuperclass()) {
                for (int j = 0; aclassificationstructdefaultview != null && j < aclassificationstructdefaultview.length; j++) {
                    if (class1.getName().equals(aclassificationstructdefaultview[j].getPrimaryClassName())) {
                        return aclassificationstructdefaultview[j];
                    }
                }

            }

        } catch (ClassNotFoundException classnotfoundexception) {
            classnotfoundexception.printStackTrace();
        }
        return null;
    }

    /**
     * createReferenceValue
     *
     * @param ibaholder
     * @param classificationNode
     * @return ReferenceValue
     * @exception Exception
     */
    public static ReferenceValue createReferenceValue(IBAHolder ibaholder, IBAReferenceable classificationNode) throws Exception {
        ReferenceValue rv = null;
        //-- ibareferenceable wt.csm.navigation.ClassificationNode
        //-- ibaholder wt.part.WTPart
        ReferenceDefinition rd = getClassificationReference();
        rv = ReferenceValue.newReferenceValue(rd, ibaholder, classificationNode);
        rv = (ReferenceValue) PersistenceHelper.manager.save(rv);

        return rv;
    }

    /**
     * getClassificationReference
     *
     * @return ReferenceDefinition
     * @exception Exception
     */
    public static ReferenceDefinition getClassificationReference() throws Exception {
        ReferenceDefinition re = null;
        //--PartClassification wt.csm.navigation.ClassificationNode
        QuerySpec qs = new QuerySpec(ReferenceDefinition.class);
        qs.appendWhere(new SearchCondition(ReferenceDefinition.class, "name", "=", "PartClassification"), new int[]{0});
        qs.appendAnd();
        qs.appendWhere(new SearchCondition(ReferenceDefinition.class, "referencedClassname", "=", "wt.csm.navigation.ClassificationNode"), new int[]{0});
        QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
        if (qr.hasMoreElements()) {
            re = (ReferenceDefinition) qr.nextElement();
        }

        return re;
    }

    /**
     * getClassificationNodeByFullPath
     *
     * @param s_primaryClassName: class name,. ex. "wt.part.WTPart"
     * @param sClassifcationPath: node path like "Electronic Parts/RESISTOR/FIXED/FUSING"
     * @return ClassificationNode
     * @exception WTException, RemoteException
     */
    public static ClassificationNode getClassificationNodeByFullPath(String s_primaryClassName, String sClassifcationPath) throws WTException, RemoteException {
        ClassificationNodeNodeView cndv = getClassificationNodeNodeViewByFullPath(s_primaryClassName, sClassifcationPath);
        /*
        ObjectIdentifier objectidentifier = classificationnodedefaultview.getParentID();
        if (objectidentifier != null) {
        ClassificationNode classificationnode = (ClassificationNode)PersistenceHelper.manager.refresh(objectidentifier);
         */
        return (ClassificationNode) PersistenceHelper.manager.refresh(cndv.getObjectID());
    }

    /**
     * getClassificationNodeDefViewByFullPath
     *
     * @param s_primaryClassName: class name,. ex. "wt.part.WTPart"
     * @param sClassifcationPath: node path like "Electronic Parts/RESISTOR/FIXED/FUSING"
     * @return ClassificationNodeDefaultView
     * @exception WTException, RemoteException
     */
    public static ClassificationNodeDefaultView getClassificationNodeDefViewByFullPath(String s_primaryClassName, String sClassifcationPath) throws WTException, RemoteException {
        ClassificationNodeNodeView cnnv = getClassificationNodeNodeViewByFullPath(s_primaryClassName, sClassifcationPath);
        ClassificationNodeDefaultView cndv = ClassificationHelper.service.getClassificationNodeDefaultView(cnnv);
        return cndv;

    }

    /**
     * getClassificationNodeNodeViewByFullPath
     *
     * @param s_primaryClassName: class name,. ex. "wt.part.WTPart"
     * @param sClassifcationPath: node path like "Electronic Parts/RESISTOR/FIXED/FUSING"
     * @return ClassificationNodeNodeView
     * @exception WTException, RemoteException
     */
    public static ClassificationNodeNodeView getClassificationNodeNodeViewByFullPath(String s_primaryClassName, String sClassifcationPath) throws WTException, RemoteException {
        ClassificationNodeNodeView cnnv = getClassificationNodeNodeView(s_primaryClassName, sClassifcationPath);
        return cnnv;
    }

    /**
     * getClassificationNodeNodeView
     *
     * @param s_primaryClassName: class name,. ex. "wt.part.WTPart"
     * @param sClassifcationPath: node path like "Electronic Parts/RESISTOR/FIXED/FUSING"
     * @return ClassificationNodeNodeView
     * @exception WTException, RemoteException
     */
    public static ClassificationNodeNodeView getClassificationNodeNodeView(String s_primaryClassName, String sClassifcationPath) throws WTException, RemoteException {
        StringTokenizer st = new StringTokenizer(sClassifcationPath, "/");
        if (st.hasMoreTokens()) {
            sClassifcationPath = st.nextToken();
        }
        ClassificationStructDefaultView cStructView = ClassificationHelper.service.getClassificationStructDefaultView(s_primaryClassName);
        ClassificationNodeNodeView nodeView[] = ClassificationHelper.service.getClassificationStructureRootNodes(cStructView);
        ClassificationNodeNodeView targetNode = null;
        int k = 0;
        int level = 0;
        for (int i = 0; i < nodeView.length; i++) {
            if (!nodeView[i].getName().equals(sClassifcationPath)) {
                continue;
            }
            if (st.hasMoreTokens()) {
                targetNode = getChild(nodeView[i], st);
            } else {
                targetNode = nodeView[i];
            }
            break;
        }

        return targetNode;
    }

    /**
     * getChild
     *
     * @param nodeView
     * @param st
     * @return ClassificationNodeNodeView
     * @exception WTException, RemoteException
     */
    private static ClassificationNodeNodeView getChild(ClassificationNodeNodeView nodeView, StringTokenizer st) throws WTException, RemoteException {
        ClassificationNodeNodeView node = null;
        int j = 0;
        if (st.hasMoreTokens()) {
            String sPath = st.nextToken();
            ClassificationNodeNodeView nodeViewSet[] = ClassificationHelper.service.getClassificationNodeChildren(nodeView);
            for (j = 0; j < nodeViewSet.length; j++) {
                if (nodeViewSet[j].getName().equals(sPath)) {
                    node = getChild(nodeViewSet[j], st);
                    if (node == null) {
                        node = nodeViewSet[j];
                    }
                }
            }

        }
        return node;
    }

    /**
     * Get IBA URL Value
     *
     * @param obj
     * @param ibaName: iba name
     * @return String[0]: URL
     * @return String[1]: URL Label
     * @exception WTException
     * @author Allen Wang
     */
    public static String[] getIBAURLValue(WTObject obj, String ibaName) throws WTException {
        String[] result = new String[2];
        String ibaClass = "wt.iba.definition.URLDefinition";
        try {
            if (obj instanceof IBAHolder) {
                IBAHolder ibaHolder = (IBAHolder) obj;
                DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
                if (defaultattributecontainer == null) {
                    defaultattributecontainer = new DefaultAttributeContainer();
                    ibaHolder.setAttributeContainer(defaultattributecontainer);
                }
                URLValueDefaultView abstractvaluedefaultview = (URLValueDefaultView) getIBAValueView(defaultattributecontainer, ibaName, ibaClass);
                if (abstractvaluedefaultview != null) {
                    result[0] = abstractvaluedefaultview.getValue();
                    result[1] = abstractvaluedefaultview.getDescription();
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }

    //donated by Joseph Chen
    public static void updateIBAStringValues(WTObject holder, String ibaName, Vector values)
            throws Exception {
        if (holder instanceof IBAHolder) {
            IBAHolder ibaHolder = (IBAHolder) holder;
            DefaultAttributeContainer defaultattributecontainer = getContainer(ibaHolder);
            if (defaultattributecontainer == null) {
                defaultattributecontainer = new DefaultAttributeContainer();
                ibaHolder.setAttributeContainer(defaultattributecontainer);
            }
            AttributeDefDefaultView attributedefdefaultview = getAttributeDefinition(ibaName, false);
            //del first
            defaultattributecontainer.deleteAttributeValues(attributedefdefaultview);
            //System.out.println("############remove@@@@@@@@@@@");
            //System.out.println("newValue.length = " + newValue.length);
            String oneNewValue = "";
            for (int i = 0; i < values.size(); i++) {
                oneNewValue = (String) values.get(i);
                StringValueDefaultView abstractvaluedefaultview1 = new StringValueDefaultView((StringDefView) attributedefdefaultview, oneNewValue);
                defaultattributecontainer.addAttributeValue(abstractvaluedefaultview1);
                ibaHolder.setAttributeContainer(defaultattributecontainer);
                StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
                ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, "CSM", null, null);
                // wt.iba.value.service.LoadValue.applySoftAttributes(ibaHolder);
                //System.out.println("ENTER..." + ibaName + "..." + newValue.toString());
            }
        }
    }

    public String getPartClassificationPath(WTPart part) throws Exception {
        part = (WTPart) IBAValueHelper.service.refreshAttributeContainer(part, null, null, null);
        DefaultAttributeContainer defaultAttributeContainer = (DefaultAttributeContainer) part.getAttributeContainer();
        AttributeDefDefaultView[] definitions = defaultAttributeContainer.getAttributeDefinitions();

        for (int i = 0; i < definitions.length; i++) {
            if (definitions[i] instanceof ReferenceDefView && ((ReferenceDefView) definitions[i]).getReferencedClassname().equals("wt.csm.navigation.ClassificationNode")) {
                AbstractValueView[] theValues = defaultAttributeContainer.getAttributeValues(definitions[i]);

                if (theValues.length > 0) {
                    return getSimpleClassificationPath(((ReferenceValueDefaultView) theValues[0]).getLiteIBAReferenceable());
                }
            }
        }
        return "";
    }

    public String getSimpleClassificationPath(LiteIBAReferenceable ref) throws Exception {
        ClassificationNodeDefaultView node = ClassificationHelper.service.getClassificationNodeDefaultView(ref);
        return getSimpleClassificationPath2(node);
    }

    public String getSimpleClassificationPath2(ClassificationNodeDefaultView node) throws Exception {
        String path = "";

        // get parent object ID
        ObjectIdentifier objId = node.getParentID();
        // if not top-level node
        if (objId != null) {
            // get parent Classification node
            ClassificationNode parent = (ClassificationNode) PersistenceHelper.manager.refresh(objId);

            // find whole path.
            while (parent != null) {
                String parentName = parent.getIBAReferenceableDisplayString();
                path = parent.getName() + "/" + path;

                // get parent node
                parent = parent.getParent();
            }

        }
        String name = node.getIBAReferenceableDisplayString();
        path = path + name;
        return path;
    }
}