package cn.shopex.ecshopx.espier.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadRowContext;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTableHandler;
import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class UploadFileImportDataServiceOnImportFinishedTest {

	@Mock
	private UploadeFileRepository uploadeFileRepository;

	@Mock
	private EspierUploadFileHandlerRegistry registry;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private PlatformTransactionManager platformTransactionManager;

	@Test
	void mergeFinish_whenLeftReachesZero_callsOnImportFinishedOnce() {
		EspierUploadTableHandler tableSpy = spy(new StubTableHandler());

		when(registry.requireHandler("search_synonym")).thenReturn(tableSpy);

		LinkedHashMap<String, Object> uploadRow = new LinkedHashMap<>();
		uploadRow.put("left_job_num", 1);
		uploadRow.put("handle_message", null);
		uploadRow.put("relation_id", 0L);
		when(uploadeFileRepository.getInfoById(7001L)).thenReturn(uploadRow);
		when(uploadeFileRepository.updateOneBy(any(), any())).thenReturn(new LinkedHashMap<>());

		TransactionStatus status = mock(TransactionStatus.class);
		when(platformTransactionManager.getTransaction(any())).thenReturn(status);
		doNothing().when(platformTransactionManager).commit(status);

		UploadFileImportDataService service =
				new UploadFileImportDataService(
						uploadeFileRepository,
						new ObjectMapper(),
						registry,
						fileStorageService,
						platformTransactionManager);

		LinkedHashMap<String, Object> uploadFileInfo = new LinkedHashMap<>();
		uploadFileInfo.put("id", 7001L);
		uploadFileInfo.put("company_id", 99L);
		uploadFileInfo.put("operator_id", 1L);
		uploadFileInfo.put("distributor_id", 0L);
		uploadFileInfo.put("supplier_id", 0L);
		uploadFileInfo.put("merchant_id", 0L);
		uploadFileInfo.put("file_type", "search_synonym");
		uploadFileInfo.put("operator_type", "admin");
		uploadFileInfo.put("relation_id", 0L);
		uploadFileInfo.put("storage_path", "");

		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("term", "手机");
		row.put("__excel_row__", 2);

		service.handleImportData(uploadFileInfo, List.of(row), Map.of(), 1, List.of());

		verify(tableSpy).onImportFinished(eq(99L), eq("search_synonym"), eq(1), eq(0));
	}

	private static final class StubTableHandler implements EspierUploadTableHandler {

		@Override
		public String supportedFileType() {
			return "search_synonym";
		}

		@Override
		public void check(MultipartFile file) {}

		@Override
		public UploadHeaderTitle getHeaderTitle(long companyId) {
			return new UploadHeaderTitle(Map.of(), Map.of());
		}

		@Override
		public void handleRow(EspierUploadRowContext ctx, Map<String, Object> row) {}
	}
}
