package com.batuhanturnay.leaveflow.common.export;

import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** {@code text/csv} for {@link Exportable} bodies. Spring Boot adds converter beans to Spring MVC. */
@Component
public class CsvHttpMessageConverter extends ExportHttpMessageConverter {

	public static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

	public CsvHttpMessageConverter() {
		super(TEXT_CSV, "csv");
		// a client asking for plain "text/csv" still gets "text/csv;charset=UTF-8"
		setDefaultCharset(StandardCharsets.UTF_8);
	}

	@Override
	byte[] render(Table table) {
		return CsvTableWriter.write(table);
	}

}
