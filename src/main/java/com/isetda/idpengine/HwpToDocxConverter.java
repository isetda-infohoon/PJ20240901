package com.isetda.idpengine;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ControlType;
import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlPicture;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.ParaText;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.Document;

import java.io.*;

public class HwpToDocxConverter {
    private static final Logger log = LogManager.getLogger((ConfigLoader.class));

    public void convert(String hwpPath, String docxPath) throws Exception {
        try {
            log.info("HWP 변환 시작: " + hwpPath);

            HWPFile hwpFile = HWPReader.fromFile(hwpPath); // hwp 읽기
            XWPFDocument docx = new XWPFDocument(); // docx 생성

            for (Section section : hwpFile.getBodyText().getSectionList()) {
                log.info("현재 섹션 문단 수: {}", section.getParagraphCount());

                java.util.Iterator<Paragraph> it = section.iterator();
                while (it.hasNext()) {
                    Paragraph hwpPara = it.next();
                    try {
                        if (hwpPara.getText() != null) {
                            int charCount = hwpPara.getText().getCharList().size();
                            String text = hwpPara.getText().getNormalString(0, charCount);
                            if (text != null && !text.trim().isEmpty()) {
                                docx.createParagraph().createRun().setText(text.trim());
                            }
                        }

                        if (hwpPara.getControlList() != null) {
                            for (Control control : hwpPara.getControlList()) {
                                if (control.getType() == ControlType.Table) {
                                    processTable((ControlTable) control, docx);
                                } else if (control.getType() == ControlType.Gso && control instanceof ControlPicture) {
                                    processPicture((ControlPicture) control, hwpFile, docx);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.info("문단 처리 중 개별 오류 발생: {}", e.getMessage());
                    }
                }
            }

            File outputFile = new File(docxPath);
            if (outputFile.isDirectory()) {
                docxPath = new File(outputFile, "converted_result.docx").getAbsolutePath();
            }

            try (FileOutputStream fos = new FileOutputStream(docxPath)) {
                docx.write(fos);
                log.info("DOCX 저장 완료: " + docxPath);
            }
        } catch (Exception e) {
            log.info("HWP 변환 중 오류 발생: {}", e.getMessage());
        }
    }

    /* HWP -> DOCX 변환 */
    // 표 변환
    private static void processTable(ControlTable hwpTable, XWPFDocument docx) throws UnsupportedEncodingException {
        try {
            XWPFTable docxTable = docx.createTable();
            boolean isFirstRow = true;

            for (Row hwpRow : hwpTable.getRowList()) {
            //for (int r = 0; r < hwpTable.getRowList().size(); r++) {
//                Row hwpRow = hwpTable.getRowList().get(r);
//                XWPFTableRow docxRow = (r == 0) ? docxTable.getRow(0) : docxTable.createRow();
//                for (int c = 0; c < hwpRow.getCellList().size(); c++) {
//                    Cell hwpCell = hwpRow.getCellList().get(c);
//                    XWPFTableCell docxCell = (c < docxRow.getTableCells().size()) ? docxRow.getCell(c) : docxRow.createCell();
//
//                    StringBuilder sb = new StringBuilder();
//                    if (hwpCell.getParagraphList() != null) {
//                        for (int pIdx = 0; pIdx < hwpCell.getParagraphList().getParagraphCount(); pIdx++) {
//                            Paragraph cp = hwpCell.getParagraphList().getParagraph(pIdx);
//                            if (cp.getText() != null) {
//                                sb.append(cp.getText().getNormalString(0, cp.getText().getCharList().size()));
//                            }
//                        }
//                    }
//                    docxCell.setText(sb.toString());
//                }
                XWPFTableRow docxRow = isFirstRow ? docxTable.getRow(0) : docxTable.createRow();
                isFirstRow = false;

                int cellIdx = 0;
                for (Cell hwpCell : hwpRow.getCellList()) {
                    XWPFTableCell docxCell = (cellIdx < docxRow.getTableCells().size()) ? docxRow.getCell(cellIdx) : docxRow.createCell();
                    StringBuilder sb = new StringBuilder();

                    int cpCount = hwpCell.getParagraphList().getParagraphCount();
                    for (int pIdx = 0; pIdx < cpCount; pIdx++) {
                        Paragraph cp = hwpCell.getParagraphList().getParagraph(pIdx);
                        if (cp != null && cp.getText() != null) {
                            sb.append(cp.getText().getNormalString(0, cp.getText().getCharList().size()));
                        }
                    }
                    docxCell.setText(sb.toString());
                    cellIdx++;
                }
            }
        } catch (Exception e) {
            log.info("TABLE 변환 중 오류 발생: {}", e.getMessage());
        }
    }

    // 이미지 변환
    private void processPicture(ControlPicture hwpPic, HWPFile hwpFile, XWPFDocument docx) throws IOException, InvalidFormatException {
        try {
            // 이미지 추출
            int targetBinId = hwpPic.getShapeComponentPicture().getPictureInfo().getBinItemID();
            byte[] imageData = null;

            int currentIndex = 1;

            for (kr.dogfoot.hwplib.object.bindata.EmbeddedBinaryData binData : hwpFile.getBinData().getEmbeddedBinaryDataList()) {
                if (currentIndex == targetBinId) {
                    imageData = binData.getData();
                    break;
                }
                currentIndex++;
            }

            if (imageData != null) {
                long emuWidth = hwpPic.getHeader().getWidth() * 127L;
                long emuHeight = hwpPic.getHeader().getHeight() * 127L;

                XWPFRun run = docx.createParagraph().createRun();
                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
                    run.addPicture(bais, Document.PICTURE_TYPE_PNG, "img_" + targetBinId, (int) emuWidth, (int) emuHeight);
                    log.info("이미지 삽입 성공: bin id - {}", targetBinId);
                }
            } else {
                log.info("이미지 데이터를 찾지 못함: bin id - {}", targetBinId);
            }
        } catch (Exception e) {
            log.info("IMAGE 변환 중 오류 발생: {}", e.getMessage());
        }
    }
}
