package cn.shopex.ecshopx.espier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.EspierExportHistoryZipFileRemover;
import cn.shopex.ecshopx.espier.domain.ExportLog;
import cn.shopex.ecshopx.espier.mapper.ExportLogMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportLogServiceScheduleDeleteHistoryFileTest {

	@Mock
	private ExportLogMapper exportLogMapper;

	@Mock
	private EspierExportHistoryZipFileRemover historyZipFileRemover;

	@InjectMocks
	private ExportLogService exportLogService;

	@Test
	@DisplayName("§3: 1,2,3,3.1,3.2,3.3,3.4,3.5,4 — 无待删行")
	void noRows_returnsZero_andNoRemoverOrDelete() {
		when(exportLogMapper.selectList(any())).thenReturn(Collections.emptyList());
		assertThat(exportLogService.scheduleDeleteHistoryFile()).isZero();
		verify(historyZipFileRemover, never()).removeExportHistoryZipObjectKey(any());
		verify(exportLogMapper, never()).deleteById(anyLong());
	}

	@Test
	@DisplayName("§3: 1,2,3,3.1–3.4,3.5,3.6,3.6.1,3.6.2,3.6.3,3.7,4 — 单条全链路")
	void oneRow_removesZipThenDeletes() {
		ExportLog row = new ExportLog();
		row.setLogId(77L);
		row.setFileName("a.zip");
		long oldEnough = (System.currentTimeMillis() / 1000) - 4 * 3600;
		row.setFinishTime(oldEnough);
		when(exportLogMapper.selectList(any())).thenReturn(List.of(row));
		assertThat(exportLogService.scheduleDeleteHistoryFile()).isEqualTo(1);
		verify(historyZipFileRemover, times(1)).removeExportHistoryZipObjectKey("export/zip/a.zip");
		verify(exportLogMapper, times(1)).deleteById(77L);
	}

	@Test
	@DisplayName("§3: 1,2,3,3.1,3.2,3.3,3.6.2 — fileName 为 null 仍拼出 export/zip/ 键")
	void nullFileName_usesEmptySuffixInKey() {
		ExportLog row = new ExportLog();
		row.setLogId(1L);
		row.setFileName(null);
		row.setFinishTime((System.currentTimeMillis() / 1000) - 4 * 3600);
		when(exportLogMapper.selectList(any())).thenReturn(List.of(row));
		assertThat(exportLogService.scheduleDeleteHistoryFile()).isEqualTo(1);
		verify(historyZipFileRemover, times(1)).removeExportHistoryZipObjectKey("export/zip/");
		verify(exportLogMapper, times(1)).deleteById(1L);
	}

	@Test
	@DisplayName("§3: 1,2,3,3.1,3.2,3.3,3.4 — 时间窗与 LIMIT：仅一条满足时处理一条")
	void boundary_lteSelect_oneProcessed() {
		ExportLog inWindow = new ExportLog();
		inWindow.setLogId(10L);
		inWindow.setFileName("b.zip");
		inWindow.setFinishTime(1000L);
		when(exportLogMapper.selectList(any())).thenReturn(List.of(inWindow));
		assertThat(exportLogService.scheduleDeleteHistoryFile()).isEqualTo(1);
		verify(historyZipFileRemover, times(1)).removeExportHistoryZipObjectKey("export/zip/b.zip");
	}

	@Test
	@DisplayName("§3: 3.2 — 积压 >100：首次至多 100 次删除，第二次可继续")
	void backlog_over100_twoBatches() {
		List<ExportLog> first = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			ExportLog e = new ExportLog();
			e.setLogId((long) i);
			e.setFileName("f" + i + ".zip");
			e.setFinishTime(1L);
			first.add(e);
		}
		List<ExportLog> second = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			ExportLog e = new ExportLog();
			e.setLogId(1000L + i);
			e.setFileName("g" + i + ".zip");
			e.setFinishTime(1L);
			second.add(e);
		}
		when(exportLogMapper.selectList(any())).thenReturn(first, second);
		assertThat(exportLogService.scheduleDeleteHistoryFile()).isEqualTo(100);
		assertThat(exportLogService.scheduleDeleteHistoryFile()).isEqualTo(50);
		verify(exportLogMapper, times(150)).deleteById(anyLong());
		verify(historyZipFileRemover, times(150)).removeExportHistoryZipObjectKey(any());
	}

	@Test
	@DisplayName("§3: 3.1–3.3 — finish_time 为 null 的行不出现在候选集则不计数")
	void nullFinishTime_excludedByQuery_simulatedEmpty() {
		when(exportLogMapper.selectList(any())).thenReturn(Collections.emptyList());
		assertThat(exportLogService.scheduleDeleteHistoryFile()).isZero();
		verify(exportLogMapper, never()).deleteById(anyLong());
	}

	@Test
	@DisplayName("§3: 3.6.2 — 存删抛错时异常上抛，本行未删库")
	void removerThrows_doesNotDeleteRow() {
		ExportLog row = new ExportLog();
		row.setLogId(9L);
		row.setFileName("x.zip");
		row.setFinishTime(1L);
		when(exportLogMapper.selectList(any())).thenReturn(List.of(row));
		doThrow(new RuntimeException("storage")).when(historyZipFileRemover).removeExportHistoryZipObjectKey(any());
		assertThatThrownBy(() -> exportLogService.scheduleDeleteHistoryFile()).isInstanceOf(RuntimeException.class);
		verify(exportLogMapper, never()).deleteById(anyLong());
	}
}
