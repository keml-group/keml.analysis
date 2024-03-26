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
	
	CellStyle defaultStyle;
	CellStyle instructionStyle;
	CellStyle factStyle;
	CellStyle origLLMStyle;
	CellStyle origOtherStyle;
	CellStyle trustStyle;
	CellStyle distrustStyle;

	public WorkbookController() {

		wb = new XSSFWorkbook();
		sheet = wb.createSheet("Trust");
	    Row headers = sheet.createRow(0);
	    
		Cell start = headers.createCell(0);
		defaultStyle = start.getCellStyle();
		start.setCellValue("TimeStamp");
		headers.createCell(1).setCellValue("IsInstruction");
		headers.createCell(2).setCellValue("Message");
		headers.createCell(3).setCellValue("InitialTrust");
		headers.createCell(4).setCellValue("CurrentTrust");

		// additional styles:
		// ************** isFact *************
	    factStyle = wb.createCellStyle();
	    factStyle.setAlignment(HorizontalAlignment.CENTER);
	    factStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
	    factStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	    
	    // ************* is Instruction **********
	    instructionStyle = wb.createCellStyle();
	    instructionStyle.setAlignment(HorizontalAlignment.CENTER);
	    instructionStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
	    instructionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	    
	    // *************** origin LLM style**************
	    origLLMStyle = wb.createCellStyle();
	    origLLMStyle.setAlignment(HorizontalAlignment.CENTER);
	    origLLMStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
	    origLLMStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND); 
	    
	    // *************** origin Other style**************
	    origOtherStyle = wb.createCellStyle();
	    origOtherStyle.setAlignment(HorizontalAlignment.CENTER);
	    origOtherStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
	    origOtherStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	    
	    // *************** Trust ****************
	    trustStyle = wb.createCellStyle();
	    trustStyle.setAlignment(HorizontalAlignment.CENTER);
	    trustStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
	    trustStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	    
	    // *************** Distrust *************
	    distrustStyle = wb.createCellStyle();
	    distrustStyle.setAlignment(HorizontalAlignment.CENTER);
	    distrustStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
	    distrustStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	}
	
	public void putData(List<NewInformation> newInfos, List<PreKnowledge> preKnowledge) {
//		style.setWrapText(true);
		int offset=1; // adapted in loop
		for (int i=0; i < preKnowledge.size(); i++) {
			PreKnowledge pre = preKnowledge.get(i);
			Row r = sheet.createRow(offset++);
			r.createCell(0).setCellValue(-1);
			Cell instr = r.createCell(1);
			instr.setCellValue(pre.isIsInstruction());
			colorByIsInstruction(instr, pre.isIsInstruction());
			Cell msg = r.createCell(2);
			msg.setCellValue(pre.getMessage());
			colorByOrigin(msg, false);
			setAndColorByValue(r.createCell(3),pre.getInitialTrust());
			setAndColorByValue(r.createCell(4),pre.getCurrentTrust());		
		}
		for (int i=0; i< newInfos.size();i++) {
			NewInformation info = newInfos.get(i);
			Row r = sheet.createRow(offset++);
			r.createCell(0).setCellValue(info.getTiming());
			Cell instr = r.createCell(1);
			instr.setCellValue(info.isIsInstruction());
			colorByIsInstruction(instr, info.isIsInstruction());
			Cell msg = r.createCell(2);
			msg.setCellValue(info.getMessage());
			colorByOrigin(msg, info.getSourceConversationPartner().getName().equals("LLM"));
			setAndColorByValue(r.createCell(3),info.getInitialTrust());
			setAndColorByValue(r.createCell(4),info.getCurrentTrust());
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
	}
	
	public void write(String file) throws IOException {
		String path = FilenameUtils.removeExtension(file) + "-trust.xlsx";
		try(FileOutputStream o = new FileOutputStream(path)) {
			wb.write(o);
			wb.close();
		}

	}

}
