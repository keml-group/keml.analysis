package keml.analysis;

import org.apache.poi.xssf.usermodel.*;

import keml.NewInformation;
import keml.PreKnowledge;

import org.apache.poi.ss.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

public class WorkbookController {
	
	XSSFWorkbook wb;
	Sheet sheet;
	
	XSSFCellStyle defaultStyle;
	XSSFCellStyle floatStyle;
	XSSFCellStyle instructionStyle;
	XSSFCellStyle factStyle;
	XSSFCellStyle origLLMStyle;
	XSSFCellStyle origOtherStyle;
	XSSFCellStyle trustStyle;
	XSSFCellStyle distrustStyle;
	XSSFCellStyle neutTrustStyle;

	public WorkbookController() {

		wb = new XSSFWorkbook();
		sheet = wb.createSheet("Trust");
	    Row headers = sheet.createRow(0);
	    
		Cell start = headers.createCell(0);
		defaultStyle = (XSSFCellStyle) start.getCellStyle();
		defaultStyle.setAlignment(HorizontalAlignment.CENTER);
		
		
		start.setCellValue("Time");
		headers.createCell(1).setCellValue("Message");
		headers.createCell(2).setCellValue("InitialTrust");
		headers.createCell(3).setCellValue("Current\nTrust");
		headers.createCell(4).setCellValue("#Arguments");
		headers.createCell(5).setCellValue("#Repetitions");

		
		// *********** styles *******************
		
		CellStyle floatStyle =  wb.createCellStyle();
	    floatStyle.setDataFormat(wb.createDataFormat().getFormat("0.##"));
	    
		
		// additional color styles:
		// ************** isFact *************
	    factStyle = wb.createCellStyle();
	    factStyle.setAlignment(HorizontalAlignment.CENTER);
	    factStyle.setFillForegroundColor(new XSSFColor(java.awt.Color.decode("#99CC00"), null));
	    factStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	    
	    // ************* is Instruction **********
	    instructionStyle = wb.createCellStyle();
	    instructionStyle.setAlignment(HorizontalAlignment.CENTER);
	    instructionStyle.setFillForegroundColor(new XSSFColor(java.awt.Color.decode("#FFCC00"), null));
	    instructionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	    
	    // *************** origin LLM style**************
	    origLLMStyle = wb.createCellStyle();
	    origLLMStyle.setAlignment(HorizontalAlignment.CENTER);
	    origLLMStyle.setFillForegroundColor(new XSSFColor(java.awt.Color.decode("#CCFFFF"), null));
	    origLLMStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND); 
	    
	    // *************** origin Other style**************
	    origOtherStyle = wb.createCellStyle();
	    origOtherStyle.setAlignment(HorizontalAlignment.CENTER);
	    origOtherStyle.setFillForegroundColor(new XSSFColor(java.awt.Color.decode("#FFFF99"), null));
	    origOtherStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	    
	    // *************** Trust ****************
	    trustStyle = wb.createCellStyle();
	    trustStyle.setDataFormat(floatStyle.getDataFormat());
	    trustStyle.setAlignment(HorizontalAlignment.CENTER);
	    trustStyle.setFillForegroundColor(new XSSFColor(java.awt.Color.decode("#339966"), null));
	    trustStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	    
	    // *************** Distrust *************
	    distrustStyle = wb.createCellStyle();
	    trustStyle.setDataFormat(floatStyle.getDataFormat());
	    distrustStyle.setAlignment(HorizontalAlignment.CENTER);
	    distrustStyle.setFillForegroundColor(new XSSFColor(java.awt.Color.decode("#FF5F5F"), null));
	    distrustStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	 // *************** neutral about trust *************
	    neutTrustStyle = wb.createCellStyle();
	    trustStyle.setDataFormat(floatStyle.getDataFormat());
	    neutTrustStyle.setAlignment(HorizontalAlignment.CENTER);
	    neutTrustStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
	    neutTrustStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	}
	
	public void putData(List<NewInformation> newInfos, List<PreKnowledge> preKnowledge) {
//		style.setWrapText(true);
		int offset=1; // adapted in loop
		for (int i=0; i < preKnowledge.size(); i++) {
			PreKnowledge pre = preKnowledge.get(i);
			Row r = sheet.createRow(offset++);
			Cell t = r.createCell(0);
			t.setCellValue(-1);
			colorByIsInstruction(t, pre.isIsInstruction());
			Cell msg = r.createCell(1);
			msg.setCellValue(pre.getMessage());
			colorByOrigin(msg, false);
			setAndColorByValue(r.createCell(2),pre.getInitialTrust());
			setAndColorByValue(r.createCell(3),pre.getCurrentTrust());
			r.createCell(4).setCellValue(pre.getTargetedBy().size());
			r.createCell(5).setCellValue(pre.getRepeatedBy().size());
		}
		for (int i=0; i< newInfos.size();i++) {
			NewInformation info = newInfos.get(i);
			Row r = sheet.createRow(offset++);
			Cell t = r.createCell(0);
			t.setCellValue(info.getTiming());
			colorByIsInstruction(t, info.isIsInstruction());
			Cell msg = r.createCell(1);
			msg.setCellValue(info.getMessage());
			colorByOrigin(msg, info.getSourceConversationPartner().getName().equals("LLM"));
			setAndColorByValue(r.createCell(2),info.getInitialTrust());
			setAndColorByValue(r.createCell(3),info.getCurrentTrust());
			r.createCell(4).setCellValue(info.getTargetedBy().size());
			r.createCell(5).setCellValue(info.getRepeatedBy().size());
		}	
	}
	
	private void colorByIsInstruction(Cell cell, boolean isInstruction) {
		if (isInstruction) {
			cell.setCellStyle(instructionStyle);
		} else {
			cell.setCellStyle(factStyle);
		}
	}
	
	private void colorByOrigin(Cell cell, boolean isLLM) {
		if (isLLM) {
			cell.setCellStyle(origLLMStyle);
		} else {
			cell.setCellStyle(origOtherStyle);
		}
	}
	
	private void setAndColorByValue(Cell cell, float value) {
		cell.setCellValue(value);
		if (value > 0.0f)
			cell.setCellStyle(trustStyle);
		else if (value <0.0f)
			cell.setCellStyle(distrustStyle);
		else
			cell.setCellStyle(neutTrustStyle);
	}
	
	public void write(String file) throws IOException {
		String path = FilenameUtils.removeExtension(file) + "-trust.xlsx";
		try(FileOutputStream o = new FileOutputStream(path)) {
			wb.write(o);
			wb.close();
		}

	}

}
