package com.batuhanturnay.leaveflow.common.export;

/**
 * A response body that can also be downloaded as CSV or Excel. Controllers return it as usual;
 * content negotiation ({@code Accept} header or {@code ?format=csv|xlsx}) picks JSON,
 * {@link CsvHttpMessageConverter} or {@link XlsxHttpMessageConverter}.
 * <p>
 * The method names deliberately avoid the {@code get} prefix so Jackson leaves them out of the JSON.
 */
public interface Exportable {

	/** File name without extension, e.g. {@code leave-balances-2026-06-15}. */
	String fileName();

	Table table();

}
