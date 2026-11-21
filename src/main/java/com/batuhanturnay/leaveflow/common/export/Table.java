package com.batuhanturnay.leaveflow.common.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A report reduced to what every file format needs: a sheet title, column headers and rows.
 * Cell values are {@link String}, {@link Number}, {@link java.time.LocalDate}, enums or {@code null}.
 */
public record Table(String title, List<String> headers, List<List<Object>> rows) {

	public Table {
		headers = List.copyOf(headers);
		int columns = headers.size();
		rows = rows.stream().map(row -> {
			if (row.size() != columns) {
				throw new IllegalArgumentException(
						"Row has %d cells but the table has %d columns".formatted(row.size(), columns));
			}
			// copied without List.copyOf, which rejects the null cells of empty values
			return Collections.unmodifiableList(new ArrayList<>(row));
		}).toList();
	}

}
