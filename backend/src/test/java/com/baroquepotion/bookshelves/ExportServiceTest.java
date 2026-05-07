package com.baroquepotion.bookshelves;

import com.baroquepotion.bookshelves.api.dto.ExportRequest;
import com.baroquepotion.bookshelves.api.dto.ExportResponse;
import com.baroquepotion.bookshelves.application.ExportExecutor;
import com.baroquepotion.bookshelves.application.ExportService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportServiceTest {

    @Test
    void createExportDelegatesToConfiguredExecutor() {
        ExportExecutor executor = mock(ExportExecutor.class);
        ExportService service = new ExportService(executor);
        ExportRequest request = new ExportRequest("/tmp/exports", true, false, false);
        ExportResponse expected = new ExportResponse(
                "/tmp/exports/2026-04-09T00-00-00Z",
                "/tmp/exports/2026-04-09T00-00-00Z/manifest.json",
                "2026-04-09T00-00-00Z",
                "4",
                Map.of("portableJsonl", true, "postgresDump", false),
                Map.of("book", 1),
                List.of()
        );
        when(executor.createExport(request)).thenReturn(expected);

        ExportResponse response = service.createExport(request);

        assertThat(response).isEqualTo(expected);
        verify(executor).createExport(request);
    }
}
