package com.batuhanturnay.leaveflow.common.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Writes a {@link Table} as an Excel workbook with one sheet: bold header row that stays visible
 * while scrolling, a filter on every column, numbers and dates as real numeric cells.
 * <p>
 * Text always goes into string cells, never formulas, so values cannot be evaluated by Excel.
 */
public final class XlsxTableWriter {

	private static final int MAX_COLUMN_CHARS = 50;

	private XlsxTableWriter() {
	}

	public static byte[] write(Table table) {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(table.title()));
			CellStyle headerStyle = headerStyle(workbook);
			CellStyle dateStyle = workbook.createCellStyle();
			dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));

			int columns = table.headers().size();
			int[] widths = new int[columns];
			Row header = sheet.createRow(0);
			for (int c = 0; c < columns; c++) {
				Cell cell = header.createCell(c);
				cell.setCellValue(table.headers().get(c));
				cell.setCellStyle(headerStyle);
				widths[c] = table.headers().get(c).length();
			}

			int r = 1;
			for (List<Object> values : table.rows()) {
				Row row = sheet.createRow(r++);
				for (int c = 0; c < columns; c++) {
					Object value = values.get(c);
					Cell cell = row.createCell(c);
					switch (value) {
						case null -> cell.setBlank();
						case Number number -> cell.setCellValue(number.doubleValue());
						case LocalDate date -> {
							cell.setCellValue(date);
							cell.setCellStyle(dateStyle);
						}
						default -> cell.setCellValue(value.toString());
					}
					widths[c] = Math.max(widths[c], value == null ? 0 : value.toString().length());
				}
			}

			// Column widths from the content length; POI's autoSizeColumn needs fonts that CI servers lack.
			for (int c = 0; c < columns; c++) {
				sheet.setColumnWidth(c, (Math.min(widths[c], MAX_COLUMN_CHARS) + 3) * 256);
			}
			sheet.createFreezePane(0, 1);
			if (columns > 0) {
				sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, r - 1), 0, columns - 1));
			}

			workbook.write(out);
			return out.toByteArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not write the Excel file", ex);
		}
	}

	private static CellStyle headerStyle(Workbook workbook) {
		Font bold = workbook.createFont();
		bold.setBold(true);
		CellStyle style = workbook.createCellStyle();
		style.setFont(bold);
		return style;
	}

}
