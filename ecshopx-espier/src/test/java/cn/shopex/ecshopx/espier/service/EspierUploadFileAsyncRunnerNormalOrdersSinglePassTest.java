package cn.shopex.ecshopx.espier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadHeaderCatalog;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTableHandler;
import cn.shopex.ecshopx.espier.service.upload.ImportDataJobDispatchPort;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EspierUploadFileAsyncRunnerNormalOrdersSinglePassTest {

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
	void runInline_dispatchesSingleChunkFor501NormalOrderRows() throws Exception {
		when(registry.requireHandler("normal_orders")).thenReturn(tableHandler);
		when(tableHandler.getHeaderTitle(1L, 0L)).thenReturn(EspierUploadHeaderCatalog.NORMAL_ORDERS());

		byte[] xlsx = buildNormalOrdersXlsx(501);
		when(fileStorageService.get(eq("file"), eq("rel/orders.xlsx"))).thenReturn(xlsx);

		LinkedHashMap<String, Object> waitRow = new LinkedHashMap<>();
		waitRow.put("id", 5001L);
		waitRow.put("handle_status", "wait");
		waitRow.put("company_id", 1L);
		waitRow.put("operator_id", 2L);
		waitRow.put("distributor_id", 0L);
		waitRow.put("supplier_id", 0L);
		waitRow.put("merchant_id", 0L);
		waitRow.put("file_type", "normal_orders");
		waitRow.put("operator_type", "admin");

		when(uploadeFileRepository.getInfoById(5001L)).thenReturn(waitRow);

		EspierUploadFileAsyncRunner runner =
				new EspierUploadFileAsyncRunner(
						uploadeFileRepository, registry, fileStorageService, new ObjectMapper(), importDataJobDispatchPort);

		LinkedHashMap<String, Object> persisted = new LinkedHashMap<>(waitRow);
		runner.runInline(persisted, "rel/orders.xlsx", "normal_orders");

		verify(uploadeFileRepository)
				.updateOneBy(eq(Map.of("id", 5001L)), argThat(m -> Integer.valueOf(1).equals(m.get("left_job_num"))));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Map<String, Object>>> paramsCap = ArgumentCaptor.forClass(List.class);
		verify(importDataJobDispatchPort, times(1))
				.dispatchImportDataChunk(
						eq(true),
						any(),
						paramsCap.capture(),
						any(),
						anyInt(),
						any());
		assertEquals(501, paramsCap.getValue().size());
	}

	private static byte[] buildNormalOrdersXlsx(int dataRows) throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			var sh = wb.createSheet();
			var header = sh.createRow(0);
			header.createCell(0).setCellValue("订单号");
			header.createCell(1).setCellValue("快递单号");
			header.createCell(2).setCellValue("快递公司");
			for (int i = 0; i < dataRows; i++) {
				var r = sh.createRow(i + 1);
				r.createCell(0).setCellValue("1000" + i);
				r.createCell(1).setCellValue("SF" + i);
				r.createCell(2).setCellValue("顺丰");
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			return bos.toByteArray();
		}
	}
}
