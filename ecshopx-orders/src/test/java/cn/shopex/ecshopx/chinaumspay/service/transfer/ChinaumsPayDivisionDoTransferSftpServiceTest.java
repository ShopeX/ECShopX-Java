package cn.shopex.ecshopx.chinaumspay.service.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadDetail;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadLog;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadDetailMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadLogMapper;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalArtifactWriterPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteUploadPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrdersRelChinaumspayDivision;
import cn.shopex.ecshopx.orders.mapper.OrdersRelChinaumspayDivisionMapper;
import cn.shopex.ecshopx.orders.service.division.DivisionFormatResult;
import cn.shopex.ecshopx.orders.service.division.OrderDivisionRelStatus;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * plan §5：4.5.1～4.5.4 与多店合并（analysis §8-3）。
 */
@ExtendWith(MockitoExtension.class)
class ChinaumsPayDivisionDoTransferSftpServiceTest {

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
	private ChinaumsDivisionLocalArtifactWriterPort local;
	@Mock
	private ChinaumsDivisionRemoteUploadPort remote;
	@Mock
	private OrdersRelChinaumspayDivisionMapper relMapper;

	private final AtomicLong detailSeq = new AtomicLong(8000L);
	private ChinaumsPayDivisionDoTransferSftpService service;

	@BeforeEach
	void setUp() {
		lenient()
				.doAnswer(
						invocation -> {
							ChinaumspayDivisionUploadDetail d = invocation.getArgument(0);
							d.setId(detailSeq.incrementAndGet());
							return 1;
						})
				.when(uploadDetailMapper)
				.insert(any(ChinaumspayDivisionUploadDetail.class));
		lenient()
				.doAnswer(invocation -> 1)
				.when(uploadLogMapper)
				.insert(any(ChinaumspayDivisionUploadLog.class));
		lenient().when(relMapper.update(any(), any())).thenReturn(1);
		service =
				new ChinaumsPayDivisionDoTransferSftpService(
						uploadLogMapper, uploadDetailMapper, local, remote, relMapper, new ObjectMapper());
		setField(service, "umsGroupNo", "G01");
	}

	@Test
	@DisplayName("analysis 4.5.1: 空列表 return false")
	void emptyDivisionList() {
		assertThat(
						service.doTransferSftp(1L, new ArrayList<>(), OrderDivisionRelStatus.UPLOADED))
				.isFalse();
	}

	@Test
	@DisplayName("analysis 4.5.2+4.5.3: 两店非空，合并后上传成功，order_ids 含两家")
	void twoShops_mergedAndUploads() {
		var a = new DivisionFormatResult();
		a.setDivisionId(10L);
		a.setOrderIds(new ArrayList<>(List.of(100L)));
		a.setTransfer(new ArrayList<>(List.of(tLine(10L, 1L, "A"))));
		a.setDivision(new ArrayList<>(List.of(dLine(10L, 1L, "B"))));
		var b = new DivisionFormatResult();
		b.setDivisionId(11L);
		b.setOrderIds(new ArrayList<>(List.of(200L)));
		b.setTransfer(new ArrayList<>(List.of(tLine(11L, 2L, "C"))));
		b.setDivision(new ArrayList<>(List.of(dLine(11L, 2L, "D"))));
		assertThat(
						service.doTransferSftp(1L, List.of(a, b), OrderDivisionRelStatus.UPLOADED))
				.isTrue();
		verify(local, atLeast(2)).put(any(), any());
		verify(remote, atLeast(2)).uploadData(any(long.class), any(), any(), any());
		verify(remote, atLeast(2)).uploadSign(any(long.class), any(), any(), any());
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<LambdaUpdateWrapper> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(relMapper, atLeastOnce()).update(isNull(), cap.capture());
		assertThat(cap.getValue()).isNotNull();
	}

	@Test
	@DisplayName("analysis 4.5.4: 上送失败包装 ResourceException，日志含 划付上传失败")
	void uploadFailure_wraps() {
		var one = new DivisionFormatResult();
		one.setDivisionId(1L);
		one.setOrderIds(List.of(1L));
		one.setTransfer(new ArrayList<>(List.of(tLine(1L, 1L, "A"))));
		one.setDivision(new ArrayList<>(List.of(dLine(1L, 1L, "B"))));
		doThrow(new RuntimeException("sftp down"))
				.when(remote)
				.uploadData(any(long.class), any(), any(), any());
		assertThatThrownBy(
						() -> service.doTransferSftp(9L, List.of(one), OrderDivisionRelStatus.UPLOADED))
				.isInstanceOf(ResourceException.class);
	}

	private static Map<String, Object> tLine(Object divisionId, long distId, String mark) {
		Map<String, Object> m = new HashMap<>();
		m.put("division_id", divisionId);
		m.put("distributor_id", distId);
		m.put("enterpriseid", "E" + mark);
		m.put("type", 0);
		m.put("fee", 1L);
		return m;
	}

	private static Map<String, Object> dLine(Object divisionId, long distId, String mark) {
		Map<String, Object> m = tLine(divisionId, distId, mark);
		m.put("payee", "平台");
		m.put("bank_name", "b");
		m.put("bank_code", "c");
		m.put("bank_account", "a");
		return m;
	}
}
