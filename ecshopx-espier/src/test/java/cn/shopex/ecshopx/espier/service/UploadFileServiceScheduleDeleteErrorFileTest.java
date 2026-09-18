package cn.shopex.ecshopx.espier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.common.cron.EspierScheduledUploadSourceFileRemover;
import cn.shopex.ecshopx.espier.service.EspierUploadFileAsyncRunner;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileJobEnqueuePort;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileStorageWriter;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UploadFileServiceScheduleDeleteErrorFileTest {

	@Mock
	private EspierUploadFileHandlerRegistry registry;

	@Mock
	private UploadeFileRepository uploadeFileRepository;

	@Mock
	private OperatorsQueryService operatorsQueryService;

	@Mock
	private EspierUploadFileJobEnqueuePort espierUploadFileJobEnqueuePort;

	@Mock
	private EspierUploadFileAsyncRunner espierUploadFileAsyncRunner;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private EspierScheduledUploadSourceFileRemover scheduledUploadSourceFileRemover;

	private EspierUploadFileStorageWriter storageWriter;
	private UploadFileService uploadFileService;
	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;

	@BeforeEach
	void setUp() {
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
		listAppender = new ListAppender<>();
		listAppender.start();
		serviceLogger = (Logger) LoggerFactory.getLogger(UploadFileService.class);
		serviceLogger.setLevel(Level.DEBUG);
		serviceLogger.addAppender(listAppender);
	}

	@AfterEach
	void tearDown() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("§3: 1,2,3,4,6 — 无候选行；remover 未调用")
	void noCandidates_returnsZero_andNeverLists() {
		when(uploadeFileRepository.countScheduleDeleteErrorFileCandidates(anyLong())).thenReturn(0L);
		assertThat(uploadFileService.scheduleDeleteErrorFile()).isZero();
		verify(uploadeFileRepository, never()).listScheduleDeleteErrorFileCandidates(anyLong(), anyInt(), anyInt());
		verify(scheduledUploadSourceFileRemover, never()).removeRelativePath(any());
	}

	@Test
	@DisplayName("§3: 1,2,3,5,5.1,5.2,5.2.1,5.2.2,5.2.3,6 — 分页两页，无 errorLine")
	void twoPages_noErrorLine_processedZero() {
		when(uploadeFileRepository.countScheduleDeleteErrorFileCandidates(anyLong())).thenReturn(150L);
		when(uploadeFileRepository.listScheduleDeleteErrorFileCandidates(anyLong(), eq(1), eq(100)))
				.thenReturn(page(100, false));
		when(uploadeFileRepository.listScheduleDeleteErrorFileCandidates(anyLong(), eq(2), eq(100)))
				.thenReturn(page(50, false));
		assertThat(uploadFileService.scheduleDeleteErrorFile()).isZero();
		verify(uploadeFileRepository, times(1)).listScheduleDeleteErrorFileCandidates(anyLong(), eq(1), eq(100));
		verify(uploadeFileRepository, times(1)).listScheduleDeleteErrorFileCandidates(anyLong(), eq(2), eq(100));
		verify(scheduledUploadSourceFileRemover, never()).removeRelativePath(any());
	}

	@Test
	@DisplayName("§3: 5.2.3 — 跳过无 truthy errorLine")
	void skipWhenNoErrorLine() {
		when(uploadeFileRepository.countScheduleDeleteErrorFileCandidates(anyLong())).thenReturn(1L);
		when(uploadeFileRepository.listScheduleDeleteErrorFileCandidates(anyLong(), eq(1), eq(100)))
				.thenReturn(page(1, false));
		assertThat(uploadFileService.scheduleDeleteErrorFile()).isZero();
		verify(scheduledUploadSourceFileRemover, never()).removeRelativePath(any());
	}

	@Test
	@DisplayName("§3: 1,2,3,5,5.1,5.2,5.2.1,5.2.2,5.2.3,6 — 删除成功")
	void deleteSuccess_incrementsProcessedAndUsesWriterPath() {
		when(uploadeFileRepository.countScheduleDeleteErrorFileCandidates(anyLong())).thenReturn(1L);
		Map<String, Object> row =
				baseRow(7L, "member_info", 1_700_000_000, "a.xlsx", Map.of("errorLine", 1));
		when(uploadeFileRepository.listScheduleDeleteErrorFileCandidates(anyLong(), eq(1), eq(100)))
				.thenReturn(singlePage(List.of(row)));
		String expected =
				storageWriter.buildRelativePathForImport("member_info", 7L, "a.xlsx", 1_700_000_000);
		assertThat(uploadFileService.scheduleDeleteErrorFile()).isEqualTo(1);
		verify(scheduledUploadSourceFileRemover, times(1)).removeRelativePath(eq(expected));
	}

	@Test
	@DisplayName("§3: 5.2.3 — delete 抛错吞掉，debug 日志，仍计 processed")
	void deleteFailure_logsDebug_stillCountsProcessed() {
		when(uploadeFileRepository.countScheduleDeleteErrorFileCandidates(anyLong())).thenReturn(1L);
		Map<String, Object> row =
				baseRow(7L, "member_info", 1_700_000_000, "a.xlsx", Map.of("errorLine", 1));
		when(uploadeFileRepository.listScheduleDeleteErrorFileCandidates(anyLong(), eq(1), eq(100)))
				.thenReturn(singlePage(List.of(row)));
		doThrow(new RuntimeException("io")).when(scheduledUploadSourceFileRemover).removeRelativePath(any());
		assertThat(uploadFileService.scheduleDeleteErrorFile()).isEqualTo(1);
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("删除上传文件处理错误信息文件失败"));
	}

	@Test
	@DisplayName("§3: 1,2,3,5,5.1,5.2,5.2.1,5.2.2,5.2.3,6 — truthy 仅第三行")
	void truthyOnlyThirdRow() {
		when(uploadeFileRepository.countScheduleDeleteErrorFileCandidates(anyLong())).thenReturn(3L);
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(baseRow(1L, "t", 100, "x1.xlsx", new LinkedHashMap<>(Map.of("other", 1))));
		rows.add(baseRow(1L, "t", 100, "x2.xlsx", Map.of("errorLine", 0)));
		rows.add(baseRow(1L, "t", 100, "x3.xlsx", Map.of("errorLine", 1)));

		when(uploadeFileRepository.listScheduleDeleteErrorFileCandidates(anyLong(), eq(1), eq(100)))
				.thenReturn(singlePage(rows));
		ArgumentCaptor<String> pathCap = ArgumentCaptor.forClass(String.class);
		assertThat(uploadFileService.scheduleDeleteErrorFile()).isEqualTo(1);
		verify(scheduledUploadSourceFileRemover, times(1)).removeRelativePath(pathCap.capture());
		assertThat(pathCap.getValue())
				.isEqualTo(storageWriter.buildRelativePathForImport("t", 1L, "x3.xlsx", 100));
	}

	private static LinkedHashMap<String, Object> page(int size, boolean withErrorLine) {
		List<Map<String, Object>> list = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			Map<String, Object> msg =
					withErrorLine ? Map.of("errorLine", 1) : Map.of("note", 1);
			list.add(baseRow(1L, "t", 100, "f.xlsx", msg));
		}
		return singlePage(list);
	}

	private static LinkedHashMap<String, Object> singlePage(List<Map<String, Object>> list) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", (long) list.size());
		data.put("list", list);
		return data;
	}

	private static Map<String, Object> baseRow(
			long companyId,
			String fileType,
			int created,
			String fileName,
			Map<String, Object> handleMessage) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", String.valueOf(companyId));
		m.put("file_type", fileType);
		m.put("created", created);
		m.put("file_name", fileName);
		m.put("handle_message", handleMessage);
		return m;
	}
}
