package com.batuhanturnay.leaveflow.common.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Renders any {@link Exportable} body in one file format and offers it as a download.
 * Write-only: reports are never uploaded.
 */
abstract class ExportHttpMessageConverter extends AbstractHttpMessageConverter<Exportable> {

	private final String extension;

	ExportHttpMessageConverter(MediaType mediaType, String extension) {
		super(mediaType);
		this.extension = extension;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return Exportable.class.isAssignableFrom(clazz);
	}

	@Override
	protected boolean canRead(MediaType mediaType) {
		return false;
	}

	@Override
	protected Exportable readInternal(Class<? extends Exportable> clazz, HttpInputMessage inputMessage) {
		throw new HttpMessageNotReadableException("Reports cannot be uploaded", inputMessage);
	}

	@Override
	protected void writeInternal(Exportable report, HttpOutputMessage outputMessage) throws IOException {
		outputMessage.getHeaders()
			.setContentDisposition(ContentDisposition.attachment()
				.filename(report.fileName() + "." + extension, StandardCharsets.UTF_8)
				.build());
		outputMessage.getBody().write(render(report.table()));
	}

	abstract byte[] render(Table table);

}
