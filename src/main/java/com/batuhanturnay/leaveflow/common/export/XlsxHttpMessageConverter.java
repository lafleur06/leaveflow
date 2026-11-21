package com.batuhanturnay.leaveflow.common.export;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Excel ({@code .xlsx}) for {@link Exportable} bodies. */
@Component
public class XlsxHttpMessageConverter extends ExportHttpMessageConverter {

	public static final String XLSX_VALUE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	public static final MediaType XLSX = MediaType.parseMediaType(XLSX_VALUE);

	public XlsxHttpMessageConverter() {
		super(XLSX, "xlsx");
	}

	@Override
	byte[] render(Table table) {
		return XlsxTableWriter.write(table);
	}

}
