package cn.shopex.ecshopx.espier.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.EspierScheduledUploadSourceFileRemover;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileJobEnqueuePort;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileQueuedPayload;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileStorageWriter;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UploadFileServiceHandleUploadFileShouldQueueBranchTest {

	private static final String FILE_TYPE = "should_queue_probe";

	@Mock
	private EspierUploadFileHandlerRegistry registry;

	@Mock
	private UploadeFileRepository uploadeFileRepository;

	@Mock
	private OperatorsQueryService operatorsQueryService;

	private EspierUploadFileStorageWriter storageWriter;

	@Mock
	private EspierUploadFileJobEnqueuePort espierUploadFileJobEnqueuePort;

	@Mock
	private EspierUploadFileAsyncRunner espierUploadFileAsyncRunner;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private EspierScheduledUploadSourceFileRemover scheduledUploadSourceFileRemover;

	private UploadFileService uploadFileService;

	private EspierUploadFileHandler handler;
	private MultipartFile multipartFile;

	@BeforeEach
	void setUp() throws Exception {
		storageWriter = new EspierUploadFileStorageWriter(fileStorageService);
		uploadFileService =
				new UploadFileService(
						registry,
						uploadeFileRepository,
						operatorsQueryService,
						storageWriter,
						espierUploadFileJobEnqueuePort,
						espierUploadFileAsyncRunner,
						fileStorageService,
						scheduledUploadSourceFileRemover,
						List.of());

		handler = mock(EspierUploadFileHandler.class);
		when(handler.supportedFileType()).thenReturn(FILE_TYPE);
		when(handler.trySyncProcess(anyLong(), anyLong(), anyLong(), anyLong(), any())).thenReturn(Optional.empty());
		when(registry.requireHandler(FILE_TYPE)).thenReturn(handler);

		multipartFile = mock(MultipartFile.class);
		when(multipartFile.isEmpty()).thenReturn(false);
		when(multipartFile.getBytes()).thenReturn(new byte[] {0x01, 0x02});
		when(multipartFile.getOriginalFilename()).thenReturn("probe.xlsx");
		when(multipartFile.getSize()).thenReturn(2L);

		when(operatorsQueryService.getInfo(any())).thenReturn(Map.of("merchant_id", 0L));
		LinkedHashMap<String, Object> persisted = new LinkedHashMap<>();
		persisted.put("id", 900L);
		persisted.put("company_id", 1L);
		when(uploadeFileRepository.create(
						anyLong(),
						anyLong(),
						anyLong(),
						anyLong(),
						anyLong(),
						anyLong(),
						anyString(),
						any(),
						anyString(),
						anyBoolean(),
						anyInt()))
				.thenReturn(persisted);
	}

	@Test
	void handleUploadFile_whenShouldQueueFalse_invokesRunInline_notEnqueuePort() {
		uploadFileService.handleUploadFile(1L, 2L, "admin", 0L, 0L, 0L, FILE_TYPE, multipartFile, false);

		verify(espierUploadFileAsyncRunner).runInline(any(), anyString(), anyString());
		verify(espierUploadFileJobEnqueuePort, never()).publishAfterCommit(any(EspierUploadFileQueuedPayload.class));
	}

	@Test
	void handleUploadFile_whenShouldQueueTrue_invokesEnqueuePort_notRunInline() {
		uploadFileService.handleUploadFile(1L, 2L, "admin", 0L, 0L, 0L, FILE_TYPE, multipartFile, true);

		verify(espierUploadFileJobEnqueuePort).publishAfterCommit(any(EspierUploadFileQueuedPayload.class));
		verify(espierUploadFileAsyncRunner, never()).runInline(any(), anyString(), anyString());
	}
}
