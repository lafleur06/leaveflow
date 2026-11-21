package com.batuhanturnay.leaveflow.common.export;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes a {@link Table} as RFC 4180 CSV: comma separated, CRLF line endings, fields quoted when
 * they contain a comma, quote or line break.
 * <ul>
 * <li>UTF-8 with a byte order mark, so Excel shows Turkish letters (ş, ğ, ı, İ) correctly.</li>
 * <li>Text starting with {@code = + - @}, a tab or a carriage return is prefixed with {@code '}.
 * Otherwise a name like {@code =HYPERLINK(...)} typed into the system would run as a formula
 * when HR opens the file (CSV or formula injection).</li>
 * </ul>
 */
public final class CsvTableWriter {

	private static final byte[] UTF8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	private static final String LINE_END = "\r\n";

	private CsvTableWriter() {
	}

	public static byte[] write(Table table) {
		StringBuilder csv = new StringBuilder();
		appendLine(csv, List.copyOf(table.headers()));
		for (List<Object> row : table.rows()) {
			appendLine(csv, row);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes(UTF8_BOM);
		out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
		return out.toByteArray();
	}

	private static void appendLine(StringBuilder csv, List<?> cells) {
		csv.append(cells.stream().map(CsvTableWriter::field).collect(Collectors.joining(","))).append(LINE_END);
	}

	static String field(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof Number) {
			return value.toString();
		}
		String text = value.toString();
		if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
			text = "'" + text;
		}
		if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
			text = "\"" + text.replace("\"", "\"\"") + "\"";
		}
		return text;
	}

}
