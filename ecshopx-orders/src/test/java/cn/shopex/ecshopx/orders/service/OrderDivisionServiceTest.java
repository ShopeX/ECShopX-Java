package cn.shopex.ecshopx.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadLogMapper;
import cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionDoTransferDownloadSftpService;
import cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionDoTransferResubmitSftpService;
import cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionDoTransferSftpService;
import cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionFormatTransferDataService;
import cn.shopex.ecshopx.orders.mapper.OrderDivisionTransferScheduleMapper;
import cn.shopex.ecshopx.orders.service.division.DivisionFormatResult;
import cn.shopex.ecshopx.orders.service.division.NeedTransferOrderRow;
import cn.shopex.ecshopx.orders.service.division.OrderDivisionRelStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class OrderDivisionServiceTest {

	private OrderDivisionTransferScheduleMapper scheduleMapper;
	private CompanysMapper companysMapper;
	private ChinaumsPayDivisionFormatTransferDataService format;
	private ChinaumsPayDivisionDoTransferSftpService doSftp;
	private ChinaumsPayDivisionDoTransferResubmitSftpService doResubmitSftp;
	private ChinaumspayDivisionUploadLogMapper uploadLogMapper;
	private ChinaumsPayDivisionDoTransferDownloadSftpService doDownloadSftp;
	private OrderDivisionService service;
	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;
	private static final int NOW = 1_000_000;

	@BeforeEach
	void setUp() {
		scheduleMapper = mock(OrderDivisionTransferScheduleMapper.class);
		companysMapper = mock(CompanysMapper.class);
		format = mock(ChinaumsPayDivisionFormatTransferDataService.class);
		doSftp = mock(ChinaumsPayDivisionDoTransferSftpService.class);
		doResubmitSftp = mock(ChinaumsPayDivisionDoTransferResubmitSftpService.class);
		uploadLogMapper = mock(ChinaumspayDivisionUploadLogMapper.class);
		doDownloadSftp = mock(ChinaumsPayDivisionDoTransferDownloadSftpService.class);
		Clock clock = Clock.fixed(Instant.ofEpochSecond(NOW), ZoneId.of("UTC"));
		service = new OrderDivisionService(
				scheduleMapper,
				companysMapper,
				format,
				doSftp,
				doResubmitSftp,
				uploadLogMapper,
				doDownloadSftp,
				clock);
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(OrderDivisionService.class);
		serviceLogger.addAppender(listAppender);
	}

	@AfterEach
	void tear() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("analysis 步骤1: 打「划付上传开始」INFO")
	void step1_startLog() {
		when(scheduleMapper.countNeedTransfer(NOW)).thenReturn(0L);
		service.scheduleTransferSftp();
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("划付上传开始"));
	}

	@Test
	@DisplayName("analysis 步骤2 count==0: 「没有需要划付的数据」；不查企业")
	void step2_noDataEarlyExit() {
		when(scheduleMapper.countNeedTransfer(NOW)).thenReturn(0L);
		assertThat(service.scheduleTransferSftp()).isTrue();
		assertThat(listAppender.list).anyMatch(
				e -> e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("没有需要划付的数据"));
		verify(companysMapper, never()).selectList(any());
	}

	@Test
	@DisplayName("analysis 步骤2 count!=0 + 步骤3: 全表选取 company_id")
	void step2_positive_step3_listCompanies() {
		when(scheduleMapper.countNeedTransfer(NOW)).thenReturn(1L);
		when(companysMapper.selectList(any())).thenReturn(List.of());
		assertThat(service.scheduleTransferSftp()).isTrue();
		verify(companysMapper, atLeastOnce()).selectList(any());
	}

	@Test
	@DisplayName("analysis 步骤2+3+4.4: 有待划付但 format 全空，则企业级无数据，不调 do")
	void step2_positiveCount_step4_4() {
		when(scheduleMapper.countNeedTransfer(NOW)).thenReturn(1L);
		Companys c = new Companys();
		c.setCompanyId(10L);
		when(companysMapper.selectList(any())).thenReturn(List.of(c));
		when(scheduleMapper.listDistributorIds(10L, NOW)).thenReturn(List.of(5L));
		when(scheduleMapper.listNeedTransferByDistributor(5L, NOW))
				.thenReturn(List.of(simpleRow(1L, 100L, 10L, "100", 5L)));
		when(format.formatTransferData(anyLong(), eq(5L), anyList())).thenReturn(null);
		assertThat(service.scheduleTransferSftp()).isTrue();
		assertThat(listAppender.list).anyMatch(
				e -> e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("company_id:10")
						&& e.getFormattedMessage().contains("没有需要划付的数据"));
		verify(doSftp, never()).doTransferSftp(anyLong(), any(), any());
	}

	@Test
	@DisplayName("analysis 步骤3+4+5+6: 主成功，「划付上传结束」且 return true")
	void happy_path_steps_3_thru_6() {
		when(scheduleMapper.countNeedTransfer(NOW)).thenReturn(1L);
		Companys c = new Companys();
		c.setCompanyId(10L);
		when(companysMapper.selectList(any())).thenReturn(List.of(c));
		when(scheduleMapper.listDistributorIds(10L, NOW)).thenReturn(List.of(5L));
		when(scheduleMapper.listNeedTransferByDistributor(5L, NOW))
				.thenReturn(List.of(simpleRow(1L, 200L, 10L, "5000", 5L)));
		DivisionFormatResult r = new DivisionFormatResult();
		r.setTransfer(new ArrayList<>());
		r.setDivision(new ArrayList<>());
		r.setDivisionId(99L);
		r.setOrderIds(List.of(200L));
		when(format.formatTransferData(anyLong(), eq(5L), anyList())).thenReturn(r);
		when(doSftp.doTransferSftp(eq(10L), any(), eq(OrderDivisionRelStatus.UPLOADED))).thenReturn(true);
		assertThat(service.scheduleTransferSftp()).isTrue();
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("划付上传结束"));
		verify(doSftp, atLeastOnce())
				.doTransferSftp(eq(10L), any(), eq(OrderDivisionRelStatus.UPLOADED));
	}

	@Test
	@DisplayName("analysis 步骤4.1+4.2+4.3.1+4.3.3: 多店拉行；一店 format 空则等效 continue")
	void steps_4_1_to_4_3_3() {
		when(scheduleMapper.countNeedTransfer(NOW)).thenReturn(1L);
		Companys c = new Companys();
		c.setCompanyId(1L);
		when(companysMapper.selectList(any())).thenReturn(List.of(c));
		when(scheduleMapper.listDistributorIds(1L, NOW)).thenReturn(List.of(9L, 8L));
		when(scheduleMapper.listNeedTransferByDistributor(9L, NOW))
				.thenReturn(List.of(simpleRow(1L, 1L, 1L, "10", 9L)));
		when(format.formatTransferData(anyLong(), eq(9L), anyList())).thenReturn(null);
		when(scheduleMapper.listNeedTransferByDistributor(8L, NOW)).thenReturn(List.of());
		when(format.formatTransferData(anyLong(), eq(8L), anyList())).thenReturn(null);
		assertThat(service.scheduleTransferSftp()).isTrue();
		verify(format, atLeastOnce()).formatTransferData(anyLong(), eq(9L), anyList());
	}

	@Nested
	@DisplayName("scheduleTransferResubmitSftp")
	class ResubmitSftp {

		@Test
		@DisplayName("plan §5: 全局无待重试 1,2,2.1,2.2 早退不 foreach")
		void globalEmpty_earlyExit() {
			when(doResubmitSftp.getErrorlogResubmitCount()).thenReturn(0L);
			assertThat(service.scheduleTransferResubmitSftp()).isTrue();
			assertThat(listAppender.list)
					.anyMatch(
							m -> m.getFormattedMessage() != null
									&& m.getFormattedMessage().contains("重新提交划付上传开始"));
			assertThat(listAppender.list)
					.anyMatch(
							m -> m.getFormattedMessage() != null
									&& m.getFormattedMessage().contains("没有需要重试的划付数据"));
			verify(companysMapper, never()).selectList(any());
			verify(doResubmitSftp, never()).doTransferResubmitSftp(anyLong());
		}

		@Test
		@DisplayName("plan §5: 有数据多企 1,2,2.1,2.3,3,4,4.1,5,6 与下游 B-2 交叉")
		void multiCompany_callsEach() {
			when(doResubmitSftp.getErrorlogResubmitCount()).thenReturn(2L);
			Companys a = new Companys();
			a.setCompanyId(10L);
			Companys b = new Companys();
			b.setCompanyId(20L);
			when(companysMapper.selectList(any())).thenReturn(List.of(a, b));
			when(doResubmitSftp.doTransferResubmitSftp(10L)).thenReturn(false);
			when(doResubmitSftp.doTransferResubmitSftp(20L)).thenReturn(true);
			assertThat(service.scheduleTransferResubmitSftp()).isTrue();
			verify(doResubmitSftp).doTransferResubmitSftp(10L);
			verify(doResubmitSftp).doTransferResubmitSftp(20L);
			assertThat(listAppender.list)
					.anyMatch(
							m -> m.getFormattedMessage() != null
									&& m.getFormattedMessage().contains("重新提交划付上传结束"));
		}
	}

	@Nested
	@DisplayName("scheduleTransferDownloadSftp")
	class DownloadSftp {

		@Test
		@DisplayName("plan §5: 1–4 无 upload_log，早退，下游 0 次")
		void emptyList_noDownstream() {
			when(uploadLogMapper.selectList(any())).thenReturn(List.of());
			assertThat(service.scheduleTransferDownloadSftp()).isTrue();
			verify(doDownloadSftp, never()).doTransferDownloadSftp(any());
			assertThat(listAppender.list)
					.anyMatch(
							e -> e.getFormattedMessage() != null
									&& e.getFormattedMessage().contains("没有需要回盘的文件"));
		}

		@Test
		@DisplayName("plan §5: 5 一条日志委托 doTransferDownloadSftp 1 次")
		void oneRow_delegatesOnce() {
			var row = new cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadLog();
			row.setId(1L);
			when(uploadLogMapper.selectList(any())).thenReturn(List.of(row));
			when(doDownloadSftp.doTransferDownloadSftp(row)).thenReturn(true);
			assertThat(service.scheduleTransferDownloadSftp()).isTrue();
			verify(doDownloadSftp, times(1)).doTransferDownloadSftp(row);
			assertThat(listAppender.list)
					.anyMatch(
							e -> e.getFormattedMessage() != null
									&& e.getFormattedMessage().contains("回盘结束"));
		}

		@Test
		@DisplayName("plan §5: 5.2 首条抛错则第二条不调用")
		void firstThrows_secondNotCalled() {
			var a = new cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadLog();
			a.setId(1L);
			var b = new cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadLog();
			b.setId(2L);
			when(uploadLogMapper.selectList(any())).thenReturn(List.of(a, b));
			when(doDownloadSftp.doTransferDownloadSftp(a)).thenThrow(new RuntimeException("x"));
			assertThrows(RuntimeException.class, () -> service.scheduleTransferDownloadSftp());
			verify(doDownloadSftp, times(1)).doTransferDownloadSftp(a);
			verify(doDownloadSftp, never()).doTransferDownloadSftp(b);
		}
	}

	@Nested
	@DisplayName("ChinaumsPayDivisionDoTransferSftpService")
	class DoSftp {

		@Test
		@DisplayName("analysis 步骤4.5.1: 空入参 return false")
		void step4_5_1_empty() {
			var uLog = mock(cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadLogMapper.class);
			var uDet = mock(cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadDetailMapper.class);
			var local = mock(cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalArtifactWriterPort.class);
			var rem = mock(cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteUploadPort.class);
			var rel = mock(cn.shopex.ecshopx.orders.mapper.OrdersRelChinaumspayDivisionMapper.class);
			ChinaumsPayDivisionDoTransferSftpService doSvc =
					new ChinaumsPayDivisionDoTransferSftpService(uLog, uDet, local, rem, rel, new ObjectMapper());
			assertFalse(
					doSvc.doTransferSftp(1L, new ArrayList<>(), OrderDivisionRelStatus.UPLOADED));
		}
	}

	private static NeedTransferOrderRow simpleRow(
			Long id, Long orderId, Long comp, String total, Long dist) {
		var n = new NeedTransferOrderRow();
		n.setId(id);
		n.setOrderId(orderId);
		n.setCompanyId(comp);
		n.setTotalFee(total);
		n.setDistributorId(dist);
		return n;
	}
}
