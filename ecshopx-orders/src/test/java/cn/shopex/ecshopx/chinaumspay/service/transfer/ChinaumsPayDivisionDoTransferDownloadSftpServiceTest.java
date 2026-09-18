package cn.shopex.ecshopx.chinaumspay.service.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionDetail;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionErrorLog;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadDetail;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadLog;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionDetailMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionErrorLogMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadDetailMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadLogMapper;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalRetFileAccessPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteDownloadPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRetSignVerifyPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrdersRelChinaumspayDivision;
import cn.shopex.ecshopx.orders.mapper.OrdersRelChinaumspayDivisionMapper;
import cn.shopex.ecshopx.orders.service.division.OrderDivisionRelStatus;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.io.IOException;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class ChinaumsPayDivisionDoTransferDownloadSftpServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), OrdersRelChinaumspayDivision.class);
	}

	@Mock
	private ChinaumspayDivisionUploadLogMapper uploadLogMapper;
	@Mock
	private ChinaumspayDivisionUploadDetailMapper uploadDetailMapper;
	@Mock
	private ChinaumspayDivisionErrorLogMapper errorLogMapper;
	@Mock
	private ChinaumspayDivisionDetailMapper divisionDetailMapper;
	@Mock
	private OrdersRelChinaumspayDivisionMapper ordersRelMapper;
	@Mock
	private ChinaumsDivisionRemoteDownloadPort downloadPort;
	@Mock
	private ChinaumsDivisionRetSignVerifyPort signPort;
	@Mock
	private ChinaumsDivisionLocalRetFileAccessPort localPort;

	@Test
	@DisplayName("5.1.3.2 本地无 ret：不更 upload_log / detail")
	void noLocalRet_noDb() throws Exception {
		var svc = newService();
		var log = newLog();
		when(localPort.exists(any())).thenReturn(false);
		assertThat(svc.doTransferDownloadSftp(log)).isTrue();
		verify(uploadLogMapper, never()).updateById(any(ChinaumspayDivisionUploadLog.class));
		verify(uploadDetailMapper, never()).updateById(any(ChinaumspayDivisionUploadDetail.class));
	}

	@Test
	@DisplayName("5.1.2 验签前抛错 -> ResourceException")
	void portThrowsResource() throws Exception {
		var svc = newService();
		var log = newLog();
		Mockito.doThrow(new IOException("io"))
				.when(downloadPort)
				.downloadFinalRetToStorage(anyLong(), any(), any(), any());
		assertThatThrownBy(() -> svc.doTransferDownloadSftp(log)).isInstanceOf(ResourceException.class);
	}

	@Test
	@DisplayName("5.1.3.5 / 5.1.3.5-A file_error：批量失败 upload_detail + orders_rel 回退 READY；无 error_log 插入")
	void fileError_batchDetail_and_revertsOrdersRel() throws Exception {
		var svc = newService();
		var log = newLog();
		log.setFileContent("100203|x\n100203|y\n");
		when(localPort.exists(any())).thenReturn(true);
		when(localPort.readStringUtf8(any()))
				.thenReturn("VERIFY_FAILED\n");
		var divRow = new ChinaumspayDivisionDetail();
		divRow.setDivisionId(10L);
		divRow.setOrderId(501L);
		when(divisionDetailMapper.selectList(any())).thenReturn(List.of(divRow));
		assertThat(svc.doTransferDownloadSftp(log)).isTrue();
		verify(uploadDetailMapper, atLeastOnce()).update(isNull(), any());
		verify(errorLogMapper, never()).insert(any(ChinaumspayDivisionErrorLog.class));
		verify(uploadLogMapper, never()).updateById(any(ChinaumspayDivisionUploadLog.class));
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<LambdaUpdateWrapper> relUw = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(ordersRelMapper, times(1)).update(isNull(), relUw.capture());
		assertThat(relUw.getValue().getParamNameValuePairs())
				.containsValue(OrderDivisionRelStatus.READY);
	}

	@Test
	@DisplayName("5.1.3.5-A 解析 id 全空：无 batch 更新")
	void doFileError_emptyIds() throws Exception {
		var svc = newService();
		var log = newLog();
		log.setFileContent("abc\n");
		when(localPort.exists(any())).thenReturn(true);
		when(localPort.readStringUtf8(any())).thenReturn("VERIFY_FAILED\n");
		assertThat(svc.doTransferDownloadSftp(log)).isTrue();
		verify(uploadDetailMapper, org.mockito.Mockito.never()).update(isNull(), any());
	}

	@Test
	@DisplayName("5.1.3.6 有汇总无明细行：不更 upload_log=1")
	void emptyData_noBackDone() throws Exception {
		var svc = newService();
		var log = newLog();
		String first =
				"g|1|1|0|0|0|0|0|0|0|0|0";
		when(localPort.exists(any())).thenReturn(true);
		when(localPort.readStringUtf8(any())).thenReturn(first + "\n");
		assertThat(svc.doTransferDownloadSftp(log)).isTrue();
		verify(uploadLogMapper, never()).updateById(any(ChinaumspayDivisionUploadLog.class));
	}

	@Test
	@DisplayName("5.1.3.7 明细 back_status=4（FAIL）：写入 error_log 1 条")
	void detailFail_backStatus4_insertsErrorLog() throws Exception {
		var svc = newService();
		var log = newLog();
		String d1 = "100203|0|0|0|0|0|银联失败|0|0|0|x|y";
		String first = "g|1|1|0|0|0|0|0|0|0|0|0";
		when(localPort.exists(any())).thenReturn(true);
		when(localPort.readStringUtf8(any())).thenReturn(first + "\n" + d1 + "\n");
		var fresh = detail(2L, ChinaumsPayDivisionDoTransferDownloadSftpService.BACK_STATUS_FAIL, "银联失败");
		fresh.setDivisionId(10L);
		when(uploadDetailMapper.selectById(2L)).thenReturn(fresh);
		assertThat(svc.doTransferDownloadSftp(log)).isTrue();
		ArgumentCaptor<ChinaumspayDivisionErrorLog> cap = ArgumentCaptor.forClass(ChinaumspayDivisionErrorLog.class);
		verify(errorLogMapper, times(1)).insert(cap.capture());
		ChinaumspayDivisionErrorLog el = cap.getValue();
		assertThat(el.getUploadDetailId()).isEqualTo(2L);
		assertThat(el.getStatus()).isEqualTo(ChinaumsPayDivisionDoTransferDownloadSftpService.BACK_STATUS_FAIL);
		assertThat(el.getErrorDesc()).isEqualTo("银联失败");
		assertThat(el.getDivisionId()).isEqualTo(10L);
		assertThat(el.getCompanyId()).isEqualTo(1L);
		assertThat(el.getType()).isEqualTo("transfer");
		verify(uploadLogMapper).updateById(any(ChinaumspayDivisionUploadLog.class));
	}

	@Test
	@DisplayName("5.1.3.7 明细 back_status=1（ONGOING）：写入 error_log 1 条")
	void detailOngoing_backStatus1_insertsErrorLog() throws Exception {
		var svc = newService();
		var log = newLog();
		String d1 = "100203|0|0|0|0|3|处理中|0|0|0|x|y";
		String first = "g|1|1|0|0|0|0|0|0|0|0|0";
		when(localPort.exists(any())).thenReturn(true);
		when(localPort.readStringUtf8(any())).thenReturn(first + "\n" + d1 + "\n");
		var fresh = detail(2L, ChinaumsPayDivisionDoTransferDownloadSftpService.BACK_STATUS_ONGOING, "处理中");
		fresh.setDivisionId(10L);
		when(uploadDetailMapper.selectById(2L)).thenReturn(fresh);
		assertThat(svc.doTransferDownloadSftp(log)).isTrue();
		ArgumentCaptor<ChinaumspayDivisionErrorLog> cap = ArgumentCaptor.forClass(ChinaumspayDivisionErrorLog.class);
		verify(errorLogMapper, times(1)).insert(cap.capture());
		assertThat(cap.getValue().getStatus()).isEqualTo(ChinaumsPayDivisionDoTransferDownloadSftpService.BACK_STATUS_ONGOING);
		assertThat(cap.getValue().getErrorDesc()).isEqualTo("处理中");
		verify(uploadLogMapper).updateById(any(ChinaumspayDivisionUploadLog.class));
	}

	@Test
	@DisplayName("5.1.3.7 成功行：回盘 log=1 且无 error 当全成功位")
	void success_updatesLog1() throws Exception {
		var svc = newService();
		var log = newLog();
		String d1 = "100203|0|0|0|0|1|ok|0|0|0|x|y";
		String first = "g|1|1|0|0|0|0|0|0|0|0|0";
		when(localPort.exists(any())).thenReturn(true);
		when(localPort.readStringUtf8(any())).thenReturn(first + "\n" + d1 + "\n");
		when(uploadDetailMapper.selectById(2L))
				.thenReturn(detail(2L, "2", "ok"));
		assertThat(svc.doTransferDownloadSftp(log)).isTrue();
		verify(errorLogMapper, never()).insert(any(ChinaumspayDivisionErrorLog.class));
		ArgumentCaptor<ChinaumspayDivisionUploadLog> c = ArgumentCaptor.forClass(ChinaumspayDivisionUploadLog.class);
		verify(uploadLogMapper).updateById(c.capture());
		assertThat(c.getValue().getBackStatus()).isEqualTo("1");
	}

	private ChinaumsPayDivisionDoTransferDownloadSftpService newService() {
		var s = new ChinaumsPayDivisionDoTransferDownloadSftpService(
				uploadLogMapper, uploadDetailMapper, errorLogMapper, divisionDetailMapper, ordersRelMapper, downloadPort, signPort, localPort);
		return s;
	}

	private static ChinaumspayDivisionUploadLog newLog() {
		var l = new ChinaumspayDivisionUploadLog();
		l.setId(9L);
		l.setCompanyId(1L);
		l.setFileName("02_G_1.txt");
		l.setLocalFilePath("chinaumsPayment/20000101");
		l.setRemoteFilePath("/upload/20000101");
		return l;
	}

	private static cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadDetail detail(long id, String back, String msg) {
		var d = new cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadDetail();
		d.setId(id);
		d.setCompanyId(1L);
		d.setDivisionId(1L);
		d.setDistributorId(1L);
		d.setFileType("transfer");
		d.setBackStatus(back);
		d.setBackStatusMsg(msg);
		return d;
	}
}
