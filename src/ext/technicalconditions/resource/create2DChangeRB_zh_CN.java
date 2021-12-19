/**
 * 
 */
package ext.technicalconditions.resource;

import wt.util.resource.RBEntry;
import wt.util.resource.RBPseudo;
import wt.util.resource.WTListResourceBundle;

/**
 * rbInfo文件标准写法
 * 1、继承自WTListResourceBundle
 * 2、类名为XxxRB_语言代码,没有语言代码的文件为默认内容XxxRB
 * @author ZhongBinpeng
 * @date   2019年10月11日
 */
public class create2DChangeRB_zh_CN extends WTListResourceBundle {
	
	
	//1.菜单描述文字
	@RBEntry("创建二维图纸更改单")
	public static final String PRIVATE_2DCHANGE_1 = "TwoDimensionalPattern.create2DChange.description";
	//2.菜单标题文字
	@RBEntry("创建二维图纸更改单")
	public static final String PRIVATE_2DCHANGE_2 = "TwoDimensionalPattern.create2DChange.title";
	//3.菜单图标
	//图标相对根路径为/netmarkets/images
	@RBEntry("activity.png")
	@RBPseudo(false)
	public static final String PRIVATE_2DCHANGE_3 = "TwoDimensionalPattern.create2DChange.icon";
	//4.鼠标移到菜单上显示的文字
	@RBEntry("创建二维图纸更改单")
	public static final String PRIVATE_2DCHANGE_4 = "TwoDimensionalPattern.create2DChange.tooltip";
	
	@RBEntry("height=300,width=300")
	public static final String PRIVATE_2DCHANGE_5 = "TwoDimensionalPattern.create2DChange.moreurlinfo";
	@RBEntry("创建二维图纸更改单")
	public static final String PRIVATE_2DCHANGE_6 = "create2DChange.title";
	@RBEntry("创建二维图纸更改单成功")
	public static final String PRIVATE_2DCHANGE_7 = "create2DChangeSuccess";
	@RBEntry("创建二维图纸更改单失败")
	public static final String PRIVATE_2DCHANGE_8 = "create2DChangeFailure";

	
}
