package cn.shopex.ecshopx.espier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileQueuedPayload;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTableHandler;
import cn.shopex.ecshopx.espier.service.upload.ImportDataJobDispatchPort;
import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EspierUploadFileAsyncRunnerChunkDispatchTest {

	@Mock
	private UploadeFileRepository uploadeFileRepository;

	@Mock
	private EspierUploadFileHandlerRegistry registry;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private ImportDataJobDispatchPort importDataJobDispatchPort;

	@Mock
	private EspierUploadTableHandler tableHandler;

	@Test
	void runInline_dispatchesSyncChunksWithSize500AndUpdatesLeftJobNum() throws Exception {
		when(registry.requireHandler("chunk_probe")).thenReturn(tableHandler);
		when(tableHandler.getHeaderTitle(1L))
				.thenReturn(new UploadHeaderTitle(Map.of("SKU", "sku_code"), Map.of()));

		byte[] xlsx = buildXlsx(501);
		when(fileStorageService.get(eq("file"), eq("rel/chunk.xlsx"))).thenReturn(xlsx);

		LinkedHashMap<String, Object> waitRow = new LinkedHashMap<>();
		waitRow.put("id", 5001L);
		waitRow.put("handle_status", "wait");
		waitRow.put("company_id", 1L);
		waitRow.put("operator_id", 2L);
		waitRow.put("distributor_id", 0L);
		waitRow.put("supplier_id", 0L);
		waitRow.put("merchant_id", 0L);
		waitRow.put("file_type", "chunk_probe");
		waitRow.put("operator_type", "admin");

		when(uploadeFileRepository.getInfoById(5001L)).thenReturn(waitRow);

		EspierUploadFileAsyncRunner runner =
				new EspierUploadFileAsyncRunner(
						uploadeFileRepository, registry, fileStorageService, new ObjectMapper(), importDataJobDispatchPort);

		LinkedHashMap<String, Object> persisted = new LinkedHashMap<>(waitRow);
		runner.runInline(persisted, "rel/chunk.xlsx", "chunk_probe");

		verify(uploadeFileRepository)
				.updateOneBy(eq(Map.of("id", 5001L)), argThat(m -> Integer.valueOf(2).equals(m.get("left_job_num"))));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Map<String, Object>>> paramsCap = ArgumentCaptor.forClass(List.class);
		verify(importDataJobDispatchPort, times(2))
				.dispatchImportDataChunk(
						eq(true),
						any(),
						paramsCap.capture(),
						any(),
						anyInt(),
						any());
		assertEquals(500, paramsCap.getAllValues().get(0).size());
		assertEquals(1, paramsCap.getAllValues().get(1).size());
		assertTrue(
				paramsCap.getAllValues().stream()
						.allMatch(list -> list.stream().allMatch(row -> row.containsKey("__excel_row__"))));
	}

	@Test
	@DisplayName("runAfterCommit dispatches chunks with executeSynchronously=false (queued parent path)")
	void runAfterCommit_dispatchesAsyncChunks_executeSynchronouslyFalse() throws Exception {
		when(registry.requireHandler("chunk_probe")).thenReturn(tableHandler);
		when(tableHandler.getHeaderTitle(1L))
				.thenReturn(new UploadHeaderTitle(Map.of("SKU", "sku_code"), Map.of()));

		byte[] xlsx = buildXlsx(501);
		when(fileStorageService.get(eq("file"), eq("rel/chunk.xlsx"))).thenReturn(xlsx);

		LinkedHashMap<String, Object> waitRow = new LinkedHashMap<>();
		waitRow.put("id", 5001L);
		waitRow.put("handle_status", "wait");
		waitRow.put("company_id", 1L);
		waitRow.put("operator_id", 2L);
		waitRow.put("distributor_id", 0L);
		waitRow.put("supplier_id", 0L);
		waitRow.put("merchant_id", 0L);
		waitRow.put("file_type", "chunk_probe");
		waitRow.put("operator_type", "admin");

		when(uploadeFileRepository.getInfoById(5001L)).thenReturn(waitRow);

		EspierUploadFileAsyncRunner runner =
				new EspierUploadFileAsyncRunner(
						uploadeFileRepository, registry, fileStorageService, new ObjectMapper(), importDataJobDispatchPort);

		EspierUploadFileQueuedPayload payload =
				EspierUploadFileQueuedPayload.fromPersistedRow(waitRow, "rel/chunk.xlsx", "chunk_probe");
		runner.runAfterCommit(payload);

		verify(uploadeFileRepository)
				.updateOneBy(eq(Map.of("id", 5001L)), argThat(m -> Integer.valueOf(2).equals(m.get("left_job_num"))));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Map<String, Object>>> paramsCap = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Integer> sortCap = ArgumentCaptor.forClass(Integer.class);
		verify(importDataJobDispatchPort, times(2))
				.dispatchImportDataChunk(
						eq(false),
						any(),
						paramsCap.capture(),
						any(),
						sortCap.capture(),
						any());
		assertEquals(500, paramsCap.getAllValues().get(0).size());
		assertEquals(1, paramsCap.getAllValues().get(1).size());
		assertEquals(List.of(2, 1), sortCap.getAllValues());
		assertTrue(
				paramsCap.getAllValues().stream()
						.allMatch(list -> list.stream().allMatch(row -> row.containsKey("__excel_row__"))));
	}

	private static byte[] buildXlsx(int dataRows) throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			var sh = wb.createSheet();
			var header = sh.createRow(0);
			header.createCell(0).setCellValue("SKU");
			for (int i = 0; i < dataRows; i++) {
				var r = sh.createRow(i + 1);
				r.createCell(0).setCellValue("v" + i);
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			return bos.toByteArray();
		}
	}
}
