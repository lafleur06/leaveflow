package com.batuhanturnay.leaveflow.common.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class TableWriterTests {

	@Test
	void quotesCsvFieldsOnlyWhenNeeded() {
		assertThat(CsvTableWriter.field("plain")).isEqualTo("plain");
		assertThat(CsvTableWriter.field("Ayşe Yılmaz")).isEqualTo("Ayşe Yılmaz");
		assertThat(CsvTableWriter.field("Kaya, Mehmet")).isEqualTo("\"Kaya, Mehmet\"");
		assertThat(CsvTableWriter.field("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
	}

	@Test
	void neutralisesTextThatSpreadsheetsWouldRunAsAFormula() {
		assertThat(CsvTableWriter.field("=HYPERLINK(1)")).isEqualTo("'=HYPERLINK(1)");
		assertThat(CsvTableWriter.field("+90 555")).isEqualTo("'+90 555");
		assertThat(CsvTableWriter.field("-1+2")).isEqualTo("'-1+2");
		assertThat(CsvTableWriter.field("@SUM(A1)")).isEqualTo("'@SUM(A1)");
		assertThat(CsvTableWriter.field("\t=1")).isEqualTo("'\t=1");
		assertThat(CsvTableWriter.field("=1,2")).isEqualTo("\"'=1,2\"");
	}

	@Test
	void writesNumbersNullsAndLineBreaksAsCsv() {
		assertThat(CsvTableWriter.field(-5)).isEqualTo("-5"); // a real number is not a formula
		assertThat(CsvTableWriter.field(null)).isEmpty();
		assertThat(CsvTableWriter.field("two\nlines")).isEqualTo("\"two\nlines\"");
		assertThat(CsvTableWriter.field(LocalDate.of(2026, 6, 15))).isEqualTo("2026-06-15");
	}

	@Test
	void csvStartsWithAByteOrderMarkAndUsesCrlf() {
		Table table = new Table("t", List.of("a", "b"), List.of(List.of(1, "x"), Arrays.asList(null, "y")));

		byte[] bytes = CsvTableWriter.write(table);

		assertThat(Arrays.copyOf(bytes, 3)).containsExactly(0xEF, 0xBB, 0xBF);
		assertThat(new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8)).isEqualTo("a,b\r\n1,x\r\n,y\r\n");
	}

	@Test
	void excelKeepsTypesAndNeverWritesFormulas() throws Exception {
		Table table = new Table("A title that is far too long for an Excel sheet name",
				List.of("Name", "Days", "Start"),
				List.of(List.of("=HYPERLINK(\"http://example.com\")", 3, LocalDate.of(2026, 7, 20)),
						Arrays.asList(null, null, null)));

		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(XlsxTableWriter.write(table)))) {
			Sheet sheet = workbook.getSheetAt(0);
			assertThat(sheet.getSheetName()).hasSizeLessThanOrEqualTo(31);
			assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
			Row row = sheet.getRow(1);
			assertThat(row.getCell(0).getCellType()).isEqualTo(CellType.STRING);
			assertThat(row.getCell(1).getNumericCellValue()).isEqualTo(3);
			assertThat(row.getCell(2).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 7, 20));
			assertThat(sheet.getRow(2).getCell(0).getCellType()).isEqualTo(CellType.BLANK);
		}
	}

	@Test
	void rowsMustMatchTheHeaders() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new Table("t", List.of("a", "b"), List.of(List.of("only one"))));
	}

}
