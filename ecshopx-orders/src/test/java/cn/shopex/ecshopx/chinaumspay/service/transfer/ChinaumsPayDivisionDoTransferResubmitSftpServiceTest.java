package cn.shopex.ecshopx.chinaumspay.service.transfer;

import static cn.shopex.ecshopx.chinaumspay.service.DivisionErrorLogResubmitService.IS_RESUBMIT_SUCC;
import static cn.shopex.ecshopx.chinaumspay.service.DivisionErrorLogResubmitService.IS_RESUBMIT_WAITING;
import static cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionDoTransferSftpService.FILE_TYPE_DIVISION;
import static cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionDoTransferSftpService.FILE_TYPE_TRANSFER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionErrorLog;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadDetail;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadLog;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionErrorLogMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadDetailMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadLogMapper;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalArtifactWriterPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteUploadPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ChinaumsPayDivisionDoTransferResubmitSftpServiceTest {

	@Mock
	private ChinaumspayDivisionErrorLogMapper errorLogMapper;
	@Mock
	private ChinaumspayDivisionUploadDetailMapper detailMapper;
	@Mock
	private ChinaumspayDivisionUploadLogMapper uploadLogMapper;
	@Mock
	private ChinaumsDivisionLocalArtifactWriterPort local;
	@Mock
	private ChinaumsDivisionRemoteUploadPort remote;

	private ChinaumsPayDivisionDoTransferResubmitSftpService service;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private ListAppender<ILoggingEvent> listAppender;
	private Logger svcLogger;

	@BeforeEach
	void setUp() {
		lenient()
				.doAnswer(
						invocation -> {
							ChinaumspayDivisionUploadLog l = invocation.getArgument(0);
							if (l.getId() == null) {
								l.setId(9001L);
							}
							return 1;
						})
				.when(uploadLogMapper)
				.insert(any(ChinaumspayDivisionUploadLog.class));
		lenient().when(detailMapper.updateById(any(ChinaumspayDivisionUploadDetail.class))).thenReturn(1);
		lenient().when(errorLogMapper.update(any(), any())).thenReturn(1);
		lenient().doNothing().when(local).put(anyString(), anyString());
		service =
				new ChinaumsPayDivisionDoTransferResubmitSftpService(
						errorLogMapper, detailMapper, uploadLogMapper, local, remote, objectMapper);
		setField(service, "umsGroupNo", "G01");
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		svcLogger = (Logger) LoggerFactory.getLogger(ChinaumsPayDivisionDoTransferResubmitSftpService.class);
		svcLogger.addAppender(listAppender);
	}

	@AfterEach
	void tear() {
		svcLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("plan §5: getErrorlogResubmitCount 计数 WAITING")
	@SuppressWarnings("rawtypes")
	void count_waiting() {
		org.mockito.Mockito.when(errorLogMapper.selectCount(any(Wrapper.class))).thenReturn(4L);
		assertThat(service.getErrorlogResubmitCount()).isEqualTo(4L);
	}

	@Test
	@DisplayName("plan §5: A,B,B-1,B-2 无待重试 return false 无 upload_log 成功插入")
	void b2_noCompanyData() {
		org.mockito.Mockito.when(errorLogMapper.selectList(any()))
				.thenReturn(List.of());
		assertThat(service.doTransferResubmitSftp(7L)).isFalse();
		verify(uploadLogMapper, never()).insert(any(ChinaumspayDivisionUploadLog.class));
		assertThat(listAppender.list)
				.anyMatch(
						m -> m.getLevel() == Level.INFO
								&& m.getFormattedMessage() != null
								&& m.getFormattedMessage().contains("company_id:7")
								&& m.getFormattedMessage().contains("没有需要重试的划付数据"));
	}

	@Test
	@DisplayName("plan §5: 单企成功 A-H 中 D、E 两次 upload_log + 指令行 + F SUCC (upload_detail 更新分支 §8)")
	void happyPath_twoFiles_updateDetailTimes() throws Exception {
		ChinaumspayDivisionErrorLog el = new ChinaumspayDivisionErrorLog();
		el.setId(11L);
		el.setCompanyId(1L);
		el.setUploadDetailId(100L);
		el.setDivisionId(200L);
		el.setIsResubmit(IS_RESUBMIT_WAITING);
		org.mockito.Mockito.when(errorLogMapper.selectList(any())).thenReturn(List.of(el));
		ChinaumspayDivisionUploadDetail tr = new ChinaumspayDivisionUploadDetail();
		tr.setId(100L);
		tr.setFileType(FILE_TYPE_TRANSFER);
		tr.setDetail("{\"division_id\":200,\"distributor_id\":5,\"foo\":\"v\"}");
		ChinaumspayDivisionUploadDetail dv = new ChinaumspayDivisionUploadDetail();
		dv.setId(101L);
		dv.setFileType(FILE_TYPE_DIVISION);
		dv.setDetail(
				"{\"division_id\":201,\"distributor_id\":5,\"payee\":\"p\",\"bank_name\":\"b\",\"bank_code\":\"c\",\"bank_account\":\"a\"}");
		org.mockito.Mockito.when(detailMapper.selectBatchIds(anyList())).thenReturn(List.of(tr, dv));
		ChinaumspayDivisionUploadDetail trFromDb = new ChinaumspayDivisionUploadDetail();
		trFromDb.setId(100L);
		trFromDb.setTimes(1);
		ChinaumspayDivisionUploadDetail dvFromDb = new ChinaumspayDivisionUploadDetail();
		dvFromDb.setId(101L);
		dvFromDb.setTimes(2);
		org.mockito.Mockito.when(detailMapper.selectById(100L)).thenReturn(trFromDb);
		org.mockito.Mockito.when(detailMapper.selectById(101L)).thenReturn(dvFromDb);
		assertThat(service.doTransferResubmitSftp(1L)).isTrue();
		verify(local, atLeastOnce()).put(any(), any());
		verify(remote, atLeastOnce()).uploadData(anyLong(), any(), any(), any());
		verify(remote, atLeastOnce()).uploadSign(anyLong(), any(), any(), any());
		ArgumentCaptor<ChinaumspayDivisionUploadLog> logCap = ArgumentCaptor.forClass(ChinaumspayDivisionUploadLog.class);
		verify(uploadLogMapper, org.mockito.Mockito.times(2))
				.insert(logCap.capture());
		List<ChinaumspayDivisionUploadLog> logs = logCap.getAllValues();
		ChinaumspayDivisionUploadLog tLog = logs.stream()
				.filter(x -> FILE_TYPE_TRANSFER.equals(x.getFileType()))
				.findFirst()
				.orElseThrow();
		assertThat(tLog.getFileContent()).startsWith("G01|1\n");
		// 与 ChinaumsPayDivisionDoTransferSftpService.writeLineWithDivisionTweak 一致：dStr + "0" + detailId + "0" + times 的 Java 字符串衔接
		assertThat(tLog.getFileContent()).contains("200010002");
		ChinaumspayDivisionUploadLog dLog = logs.stream()
				.filter(x -> FILE_TYPE_DIVISION.equals(x.getFileType()))
				.findFirst()
				.orElseThrow();
		assertThat(dLog.getFileContent()).startsWith("G01|1\n");
		assertThat(dLog.getFileContent()).contains("201010103");
		ArgumentCaptor<ChinaumspayDivisionErrorLog> rowForUpdate = ArgumentCaptor.forClass(ChinaumspayDivisionErrorLog.class);
		verify(errorLogMapper).update(rowForUpdate.capture(), any());
		assertThat(rowForUpdate.getValue().getIsResubmit()).isEqualTo(IS_RESUBMIT_SUCC);
		assertThat(listAppender.list)
				.anyMatch(
						m -> m.getFormattedMessage() != null
								&& m.getFormattedMessage().contains("reDivsionData===>"));
		assertThat(listAppender.list)
				.anyMatch(
						m -> m.getFormattedMessage() != null
								&& m.getFormattedMessage().contains("localFile:"));
	}

	@Test
	@DisplayName("plan §5: H 上送失败 ResourceException 与 重新提交划付上传失败 日志")
	void failure_wraps() {
		ChinaumspayDivisionErrorLog el = new ChinaumspayDivisionErrorLog();
		el.setId(1L);
		el.setCompanyId(1L);
		el.setUploadDetailId(100L);
		el.setDivisionId(200L);
		el.setIsResubmit(IS_RESUBMIT_WAITING);
		org.mockito.Mockito.when(errorLogMapper.selectList(any())).thenReturn(List.of(el));
		ChinaumspayDivisionUploadDetail tr = new ChinaumspayDivisionUploadDetail();
		tr.setId(100L);
		tr.setFileType(FILE_TYPE_TRANSFER);
		tr.setDetail("{\"division_id\":200,\"distributor_id\":1}");
		org.mockito.Mockito.when(detailMapper.selectBatchIds(anyList())).thenReturn(List.of(tr));
		ChinaumspayDivisionUploadDetail trFromDb = new ChinaumspayDivisionUploadDetail();
		trFromDb.setId(100L);
		trFromDb.setTimes(0);
		org.mockito.Mockito.when(detailMapper.selectById(100L)).thenReturn(trFromDb);
		doThrow(new RuntimeException("sftp-boom"))
				.when(remote)
				.uploadData(anyLong(), any(), any(), any());
		assertThatThrownBy(() -> service.doTransferResubmitSftp(1L))
				.isInstanceOf(ResourceException.class);
		assertThat(listAppender.list)
				.anyMatch(
						m -> m.getFormattedMessage() != null
								&& m.getFormattedMessage().contains("重新提交划付上传失败"));
	}
}
