package ext.technicalconditions.util.third;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class ItextCreatePdf {
	
	final public static float MARGINLEFT = 33;// 页面边距
	final public static float MARGINRIGHT = 33;
	final public static float MARGINTOP = 33;
	final public static float MARGINBOTTOM = 33;
	final public static float PR_SINGLECELLHEIGHT = 28.50f;// 问题报告_单行表格列高度
	final public static float ECR_SINGLECELLHEIGHT = 28.50f;// 工程更改申请_单行表格列高度
	final public static float ECN_SINGLECELLHEIGHT = 26.50f;// 工程更改申请_单行表格列高度
	public static BaseFont BASEFONT = null;
	public static Font FONT = null;
	public static Font BOLDFONT = null;
	public static Font BIGFONT = null;
	
	static {
		// 初始化字体
		try {
			BASEFONT = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
			FONT = new Font(BASEFONT, 12f, Font.NORMAL);
			BOLDFONT = new Font(BASEFONT, 12f, Font.BOLD);
			BIGFONT = new Font(BASEFONT, 18f, Font.BOLD);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) throws IOException, DocumentException {
		createEcnPdf();
	}
	
	/**
	 * 更改实施验证单(ECN)
	 * @throws IOException
	 * @throws DocumentException
	 */
	public static void createEcnPdf() throws IOException, DocumentException {
		Document document = new Document(PageSize.A4);
		File file = new File("D:/itextpdf/更改实施验证单.pdf");
		file.createNewFile();
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("D:/itextpdf/更改实施验证单.pdf"));
		document.open();
		//格式
		float[] widths0 = new float[] { 10.0f};
		float[] widths1 = new float[] { 2.0f, 2.0f, 2.0f, 2.0f, 2.0f};
		float[] widths2 = new float[] { 3.3f, 3.3f, 3.3f};
		//标题
		PdfPTable Table0 = createTable(widths0);
		PdfPCell cell1=createMidCenterCell("更改实施验证单(ECN)", ECN_SINGLECELLHEIGHT*2,0);
		cell1.setPhrase(new Phrase("更改实施验证单(ECN)", BIGFONT));
		Table0.addCell(cell1);
		document.add(Table0);
		//正文
		PdfPTable Table1 = createTable(widths0);
		Table1.addCell(createMidLeftCell("ECN编号:"+"",ECN_SINGLECELLHEIGHT,0));
		Table1.addCell(createMidLeftCell("ECN名称:"+"",ECN_SINGLECELLHEIGHT,0));
		Table1.addCell(createMidLeftCell("更改来源:"+"",ECN_SINGLECELLHEIGHT,0));
		Table1.addCell(createMidLeftCell("拟修改的文件（含图样）号:"+"",ECN_SINGLECELLHEIGHT,0));
	    document.add(Table1);
		
	    PdfPTable Table2 = createTable(widths1);
		Table2.addCell(createMidLeftCell("序号:"+"",ECN_SINGLECELLHEIGHT,0));
		Table2.addCell(createMidLeftCell("文件编号:"+"",ECN_SINGLECELLHEIGHT,0));
		Table2.addCell(createMidLeftCell("文件名称:"+"",ECN_SINGLECELLHEIGHT,0));
		Table2.addCell(createMidLeftCell("版本:"+"",ECN_SINGLECELLHEIGHT,0));
		Table2.addCell(createMidLeftCell("备注:"+"",ECN_SINGLECELLHEIGHT,0));
	    document.add(Table2);
		
	    PdfPTable Table3 = createTable(widths0);
	    Table3.addCell(createMidLeftCell("优先级:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table3.addCell(createMidLeftCell("受影响的基线:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table3.addCell(createTopLeftCell("更改内容:"+"",ECN_SINGLECELLHEIGHT*3,0));
	    document.add(Table3);
		
	    PdfPTable Table4 = createTable(widths2);
	    Table4.addCell(createMidLeftCell("更改实施单位:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table4.addCell(createMidLeftCell("更改实施人员（签名）:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table4.addCell(createMidLeftCell("日期:"+"",ECN_SINGLECELLHEIGHT,0));
	    document.add(Table4);
	    
	    PdfPTable Table5 = createTable(widths0);
	    Table5.addCell(createMidLeftCell("更改验证情况（分为设计、工艺、实物制品、在役品等）:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table5.addCell(createTopLeftCell("设计资料更改实施和验证情况:"+"",ECN_SINGLECELLHEIGHT*3,0));
	    document.add(Table5);
	    
	    PdfPTable Table6 = createTable(widths2);
	    Table6.addCell(createMidLeftCell("更改验证单位:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table6.addCell(createMidLeftCell("更改验证人员（签名）:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table6.addCell(createMidLeftCell("日期:"+"",ECN_SINGLECELLHEIGHT,0));
	    document.add(Table6);
	    
	    PdfPTable Table7 = createTable(widths0);
	    Table7.addCell(createTopLeftCell("工艺资料更改实施和验证情况:"+"",ECN_SINGLECELLHEIGHT*3,0));
	    document.add(Table7);
	    
	    PdfPTable Table8 = createTable(widths2);
	    Table8.addCell(createMidLeftCell("更改验证单位:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table8.addCell(createMidLeftCell("更改验证人员（签名）:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table8.addCell(createMidLeftCell("日期:"+"",ECN_SINGLECELLHEIGHT,0));
	    document.add(Table8);
	    
	    PdfPTable Table9 = createTable(widths0);
	    Table9.addCell(createTopLeftCell("实物制品更改实施和验证情况:"+"",ECN_SINGLECELLHEIGHT*3,0));
	    document.add(Table9);
	    
	    PdfPTable Table10 = createTable(widths2);
	    Table10.addCell(createMidLeftCell("更改验证单位:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table10.addCell(createMidLeftCell("更改验证人员（签名）:"+"",ECN_SINGLECELLHEIGHT,0));
	    Table10.addCell(createMidLeftCell("日期:"+"",ECN_SINGLECELLHEIGHT,0));
	    document.add(Table10);
	    
	    PdfPTable Table11 = createTable(widths0);
	    Table11.addCell(createTopLeftCell("在役品:"+"",ECN_SINGLECELLHEIGHT*3,0));
	    document.add(Table11);
	    
	    
	    
	    document.close();
	    writer.close();
	}
	
	/**
	 * 
	 * 工程更改申请(ERC)
	 * @throws IOException
	 * @throws DocumentException
	 */
	public static void createEcrPdf() throws IOException, DocumentException{
		Document document = new Document(PageSize.A4);
		File file = new File("D:/itextpdf/工程更改申请.pdf");
		file.createNewFile();
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("D:/itextpdf/工程更改申请.pdf"));
		document.open();
		//格式
		float[] widths0 = new float[] { 10.0f};
		float[] widths1 = new float[] { 5.0f, 5.0f};
		float[] widths2 = new float[] { 6.0f, 4.0f};
		float[] widths3 = new float[] { 2.6f, 3.7f, 3.7f};
		float[] widths4 = new float[] { 2.6f, 1.28f, 1.28f, 1.28f, 1.28f, 1.28f, 1.0f};
		//标题
		PdfPTable Table0 = createTable(widths0);
		PdfPCell cell1=createMidCenterCell("工程更改申请(ECR)", ECR_SINGLECELLHEIGHT*2,0);
		cell1.setPhrase(new Phrase("工程更改申请(ECR)", BIGFONT));
		Table0.addCell(cell1);
		document.add(Table0);
		//正文
		PdfPTable Table1 = createTable(widths0);
		Table1.addCell(createMidLeftCell("ECR编号:"+"",ECR_SINGLECELLHEIGHT,0));
		Table1.addCell(createMidLeftCell("ECR名称:"+"",ECR_SINGLECELLHEIGHT,0));
	    document.add(Table1);
		
	    PdfPTable Table2 = createTable(widths1);
	    Table2.addCell(createMidLeftCell("型号:"+"",ECR_SINGLECELLHEIGHT,0));
	    Table2.addCell(createMidLeftCell("更改原因:"+"",ECR_SINGLECELLHEIGHT,0));
	    document.add(Table2);
	    
	    PdfPTable Table3 = createTable(widths2);
	    Table3.addCell(createMidLeftCell("ECR提出者:"+"",ECR_SINGLECELLHEIGHT,0));
	    Table3.addCell(createMidLeftCell("日期:"+"",ECR_SINGLECELLHEIGHT,0));
	    Table3.addCell(createMidLeftCell("ECR申请单位:"+"",ECR_SINGLECELLHEIGHT,0));
	    Table3.addCell(createMidLeftCell("PR编号:"+"",ECR_SINGLECELLHEIGHT,0));
	    document.add(Table3);
	    
	    PdfPTable Table4 = createTable(widths0);
	    Table4.addCell(createMidLeftCell("分发单位:"+"",ECR_SINGLECELLHEIGHT,0));
	    Table4.addCell(createTopLeftCell("问题描述:"+"",ECR_SINGLECELLHEIGHT*3,0));
	    Table4.addCell(createTopLeftCell("解决方案:"+"",ECR_SINGLECELLHEIGHT*3,0));
	    Table4.addCell(createMidLeftCell("优先级:"+"□危机□紧急□一般",ECR_SINGLECELLHEIGHT,0));
	    Table4.addCell(createMidLeftCell("受影响的基线:"+"□需求□设计发放□产品",ECR_SINGLECELLHEIGHT,0));
	    Table4.addCell(createTopLeftCell("更改影响评估:",ECR_SINGLECELLHEIGHT*3,0));
	    document.add(Table4);
	    
	    PdfPTable Table5 = createTable(widths3);
	    Table5.addCell(createMidLeftCell("提出部门审核",ECR_SINGLECELLHEIGHT*2,0));
	    Table5.addCell(createTopCenterCell("签名",ECR_SINGLECELLHEIGHT*2,0));
	    Table5.addCell(createTopCenterCell("日期",ECR_SINGLECELLHEIGHT*2,0));
	    document.add(Table5);
	    
	    PdfPTable Table6 = createTable(widths4);
	    PdfPCell cell=createMidLeftCell("相关单位会签", ECR_SINGLECELLHEIGHT,0);
		cell.setRowspan(2);
		Table6.addCell(cell);
	    //Table6.addCell(createMidLeftCell("相关单位会签",ECR_SINGLECELLHEIGHT*2,0));
	    Table6.addCell(createMidCenterCell("工艺部门",ECR_SINGLECELLHEIGHT,0));
	    Table6.addCell(createMidCenterCell("生产部门",ECR_SINGLECELLHEIGHT,0));
	    Table6.addCell(createMidCenterCell("质量部门",ECR_SINGLECELLHEIGHT,0));
	    Table6.addCell(createMidCenterCell("项目部门",ECR_SINGLECELLHEIGHT,0));
	    Table6.addCell(createMidCenterCell("客服部门",ECR_SINGLECELLHEIGHT,0));
	    Table6.addCell(createMidCenterCell("",ECR_SINGLECELLHEIGHT,0));
	    
	    Table6.addCell(createTopCenterCell("签名"+""+"日期",ECR_SINGLECELLHEIGHT*2,0));
	    Table6.addCell(createTopCenterCell("签名"+""+"日期",ECR_SINGLECELLHEIGHT*2,0));
	    Table6.addCell(createTopCenterCell("签名"+""+"日期",ECR_SINGLECELLHEIGHT*2,0));
	    Table6.addCell(createTopCenterCell("签名"+""+"日期",ECR_SINGLECELLHEIGHT*2,0));
	    Table6.addCell(createTopCenterCell("签名"+""+"日期",ECR_SINGLECELLHEIGHT*2,0));
	    Table6.addCell(createTopCenterCell("",ECR_SINGLECELLHEIGHT*2,0));
	    document.add(Table6);
	    
	    PdfPTable Table7 = createTable(widths3);
	    Table7.addCell(createTopCenterCell("项目级CCB",ECR_SINGLECELLHEIGHT*2,0));
	    Table7.addCell(createTopCenterCell("签名",ECR_SINGLECELLHEIGHT*2,0));
	    Table7.addCell(createTopCenterCell("日期",ECR_SINGLECELLHEIGHT*2,0));
	    document.add(Table7);
	    
	    document.close();
	    writer.close();
	}
	
	
	
	/**
	 * 问题报告(PR)
	 * @throws DocumentException
	 * @throws IOException
	 */
	public static void createPrPdf() throws DocumentException, IOException {
		
		Document document = new Document(PageSize.A4);
		File file = new File("D:/itextpdf/问题报告.pdf");
		file.createNewFile();
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("D:/itextpdf/问题报告.pdf"));
		document.open();
		//格式
		float[] widths0 = new float[] { 10.0f};
		float[] widths1 = new float[] { 2.0f, 8.0f};
		float[] widths2 = new float[] { 2.0f, 4.0f, 2.0f, 2.0f};
		
		//标题
		PdfPTable Table0 = createTable(widths0);
		PdfPCell cell1=createMidCenterCell("问题报告", PR_SINGLECELLHEIGHT*3,0);
		cell1.setPhrase(new Phrase("问题报告", BIGFONT));
		Table0.addCell(cell1);
		document.add(Table0);
		//正文
		PdfPTable Table1 = createTable(widths1);
		Table1.addCell(createMidCenterCell("问题报告编号",PR_SINGLECELLHEIGHT,0));
		Table1.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		Table1.addCell(createMidCenterCell("问题来源",PR_SINGLECELLHEIGHT,0));
		Table1.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		Table1.addCell(createMidCenterCell("问题对象",PR_SINGLECELLHEIGHT,0));
		Table1.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		Table1.addCell(createMidCenterCell("问题描述",PR_SINGLECELLHEIGHT*4,0));
		Table1.addCell(createTopLeftCell("", PR_SINGLECELLHEIGHT*4,0));
	    document.add(Table1);
	    
		PdfPTable Table2 = createTable(widths2);
		Table2.addCell(createMidCenterCell("问题提出人",PR_SINGLECELLHEIGHT,0));
		Table2.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		Table2.addCell(createMidCenterCell("提交日期",PR_SINGLECELLHEIGHT,0));
		Table2.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		document.add(Table2);
	    
		PdfPTable Table3 = createTable(widths1);
		Table3.addCell(createMidCenterCell("问题分析",PR_SINGLECELLHEIGHT*4,0));
		Table3.addCell(createTopLeftCell("", PR_SINGLECELLHEIGHT*4,0));
		document.add(Table3);
		
		PdfPTable Table4 = new PdfPTable(widths2);
		Table4.setTotalWidth(500);
		Table4.setLockedWidth(true);
		Table4.setHorizontalAlignment(Element.ALIGN_CENTER);
		Table4.getDefaultCell().setBorder(1);
		Table4.addCell(createMidCenterCell("问题确认人",PR_SINGLECELLHEIGHT,0));
		Table4.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		Table4.addCell(createMidCenterCell("审核日期",PR_SINGLECELLHEIGHT,0));
		Table4.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		document.add(Table4);
		
		PdfPTable Table5 = createTable(widths1);
		Table5.addCell(createMidCenterCell("处理描述",PR_SINGLECELLHEIGHT*4,0));
		Table5.addCell(createTopLeftCell("", PR_SINGLECELLHEIGHT*4,0));
		document.add(Table5);
		
		PdfPTable Table6 = createTable(widths2);
		Table6.addCell(createMidCenterCell("处理人",PR_SINGLECELLHEIGHT,0));
		Table6.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		Table6.addCell(createMidCenterCell("日期",PR_SINGLECELLHEIGHT,0));
		Table6.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		Table6.addCell(createMidCenterCell("CCB审批",PR_SINGLECELLHEIGHT,0));
		Table6.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		Table6.addCell(createMidCenterCell("日期",PR_SINGLECELLHEIGHT,0));
		Table6.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		document.add(Table6);
		
		PdfPTable Table7 = createTable(widths1);
		Table7.addCell(createMidCenterCell("问题验证描述",PR_SINGLECELLHEIGHT*4,0));
		Table7.addCell(createTopLeftCell("", PR_SINGLECELLHEIGHT*4,0));
		document.add(Table7);
	    
		PdfPTable Table8 = createTable(widths2);
		Table8.addCell(createMidCenterCell("问题报告验证人",PR_SINGLECELLHEIGHT,0));
		Table8.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		Table8.addCell(createMidCenterCell("关闭日期",PR_SINGLECELLHEIGHT,0));
		Table8.addCell(createMidLeftCell("", PR_SINGLECELLHEIGHT,0));
		document.add(Table8);
		
	    document.close();
	    writer.close();
	       
	}
	
	public static PdfPTable createTable(float[] widths) {
		PdfPTable table = new PdfPTable(widths);
		table.setTotalWidth(500);
		table.setLockedWidth(true);
		table.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.getDefaultCell().setBorder(1);
		
		
		return table;
	}
	//生成文字垂直居中水平居中的单元格
	public static PdfPCell createMidCenterCell(String value, float fixedHeight,int colspan) {
		PdfPCell cell = new PdfPCell();
		if (fixedHeight != 0) {
			cell.setFixedHeight(fixedHeight);
		}
		if (colspan > 0) {
			cell.setColspan(colspan);
		}
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setPhrase(new Phrase(value, FONT));
		return cell;
	}
	//生成文字置顶水平居中的单元格
	public static PdfPCell createTopCenterCell(String value, float fixedHeight,int colspan) {
		PdfPCell cell = new PdfPCell();
		if (fixedHeight != 0) {
			cell.setFixedHeight(fixedHeight);
		}
		if (colspan > 0) {
			cell.setColspan(colspan);
		}
		cell.setVerticalAlignment(Element.ALIGN_TOP);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setPhrase(new Phrase(value, FONT));
		return cell;
	}
	
	//生成文字垂直居中左对齐的单元格
	public static PdfPCell createMidLeftCell(String value, float fixedHeight,int colspan) {
		PdfPCell cell = new PdfPCell();
		if (fixedHeight != 0) {
			cell.setFixedHeight(fixedHeight);
		}
		if (colspan > 0) {
			cell.setColspan(colspan);
		}
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPhrase(new Phrase(value, FONT));
		return cell;
	}
	//生成文字置顶左对齐的单元格
		public static PdfPCell createTopLeftCell(String value, float fixedHeight,int colspan) {
			PdfPCell cell = new PdfPCell();
			if (fixedHeight != 0) {
				cell.setFixedHeight(fixedHeight);
			}
			if (colspan > 0) {
				cell.setColspan(colspan);
			}
			cell.setVerticalAlignment(Element.ALIGN_TOP);
			cell.setHorizontalAlignment(Element.ALIGN_LEFT);
			cell.setPhrase(new Phrase(value, FONT));
			return cell;
		}
	
}
