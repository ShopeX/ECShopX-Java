package cn.shopex.ecshopx.hfpay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.hfpay.HfPayQry008Port;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.hfpay.HfpayDistributorWithdrawSuccessEventPublishPort;
import cn.shopex.ecshopx.hfpay.domain.HfpayCashRecord;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCashRecordMapper;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HfpayCashRecordServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), HfpayCashRecord.class);
	}

	@Mock
	private HfpayCashRecordMapper cashRecordMapper;

	@Mock
	private HfPayPaymentSettingService paymentSettingService;

	@Mock
	private HfPayQry008Port qry008Port;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private HfpayDistributorWithdrawSuccessEventPublishPort hfpayDistributorWithdrawSuccessEventPublishPort;

	@Mock
	private ObjectProvider<Clock> clockProvider;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private HfpayCashRecordService hfpayCashRecordService;

	private final ZoneId zone = ZoneId.of("Asia/Shanghai");
	private final Clock fixedClock = Clock.fixed(Instant.parse("2024-06-20T10:00:00Z"), zone);

	@BeforeEach
	void setClock() {
		doReturn(fixedClock).when(clockProvider).getIfAvailable();
		doReturn(fixedClock).when(clockProvider).getIfAvailable(any());
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	@Test
	@DisplayName("§3 2.1: count=0 早退 return true，无 UPDATE")
	void section3_2_1_countZero_returnsTrue() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
		assertThat(hfpayCashRecordService.scheduleCheckStatus()).isTrue();
		verify(cashRecordMapper, never()).update(isNull(), any(Wrapper.class));
		verify(qry008Port, never()).qry008(any(), any());
	}

	@Test
	@DisplayName("§3 2.2 & §3 3: fileNum=ceil(count/500)，分页 size=fileNum 非 500，循环 fileNum 次")
	@SuppressWarnings("unchecked")
	void section3_2_2_and_3_paginationUsesFileNumNot500() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1000L);
		when(cashRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
				.thenAnswer(inv -> {
					Page<HfpayCashRecord> page = inv.getArgument(0);
					page.setRecords(Collections.emptyList());
					return page;
				});
		assertThat(hfpayCashRecordService.scheduleCheckStatus()).isTrue();
		ArgumentCaptor<Page<HfpayCashRecord>> pageCap = ArgumentCaptor.forClass(Page.class);
		verify(cashRecordMapper, times(2)).selectPage(pageCap.capture(), any(Wrapper.class));
		assertThat(pageCap.getAllValues()).hasSize(2);
		assertThat(pageCap.getAllValues().get(0).getSize()).isEqualTo(2);
		assertThat(pageCap.getAllValues().get(1).getSize()).isEqualTo(2);
		assertThat(pageCap.getAllValues().get(0).getSize()).isNotEqualTo(500L);
	}

	@Test
	@DisplayName("§3 4.1: 未获锁且 GET 未过期，不调 qry008、不 UPDATE")
	void section3_4_1_lockBusy_skipQuery() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		HfpayCashRecord row = sampleRow(1L, 9L, "O1", "20240618");
		when(cashRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
				.thenAnswer(inv -> {
					Page<HfpayCashRecord> p = inv.getArgument(0);
					p.setRecords(List.of(row));
					return p;
				});
		when(valueOperations.setIfAbsent(eq("hfpay:1"), any(String.class))).thenReturn(false);
		when(valueOperations.get("hfpay:1")).thenReturn(String.valueOf(Instant.now(fixedClock).getEpochSecond() + 100));
		assertThat(hfpayCashRecordService.scheduleCheckStatus()).isTrue();
		verify(qry008Port, never()).qry008(any(), any());
		verify(cashRecordMapper, never()).update(isNull(), any(Wrapper.class));
	}

	@Test
	@DisplayName("§3 4.1.1: 未获锁、GET 已过期则 DEL，本行不 UPDATE")
	void section3_4_1_1_lockBusyExpired_deletesKeyNoUpdate() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		HfpayCashRecord row = sampleRow(1L, 9L, "O1", "20240618");
		when(cashRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
				.thenAnswer(inv -> {
					Page<HfpayCashRecord> p = inv.getArgument(0);
					p.setRecords(List.of(row));
					return p;
				});
		when(valueOperations.setIfAbsent(eq("hfpay:1"), any(String.class))).thenReturn(false);
		when(valueOperations.get("hfpay:1")).thenReturn("1");
		assertThat(hfpayCashRecordService.scheduleCheckStatus()).isTrue();
		verify(companysRedisTemplate, atLeastOnce()).delete("hfpay:1");
		verify(qry008Port, never()).qry008(any(), any());
		verify(cashRecordMapper, never()).update(isNull(), any(Wrapper.class));
	}

	@Test
	@DisplayName("§3 4.2: C00000 且 trans_stat 非 S，不 UPDATE、删锁")
	void section3_4_2_pendingNotSuccess_noUpdateDeletesLock() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		HfpayCashRecord row = sampleRow(1L, 9L, "O1", "20240618");
		when(cashRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
				.thenAnswer(inv -> {
					Page<HfpayCashRecord> p = inv.getArgument(0);
					p.setRecords(List.of(row));
					return p;
				});
		when(valueOperations.setIfAbsent(eq("hfpay:1"), any(String.class))).thenReturn(true);
		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("mer_cust_id", "M1");
		when(paymentSettingService.loadForCompany(9L)).thenReturn(setting);
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("resp_code", "C00000");
		q.put("trans_stat", "F");
		when(qry008Port.qry008(any(), any())).thenReturn(q);
		assertThat(hfpayCashRecordService.scheduleCheckStatus()).isTrue();
		verify(companysRedisTemplate, atLeastOnce()).delete("hfpay:1");
		verify(cashRecordMapper, never()).update(isNull(), any(Wrapper.class));
	}

	@Test
	@DisplayName("§3 4.2.1: C00000+S 更新成功并发成功事件")
	void section3_4_2_1_success_publishesEvent() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		HfpayCashRecord row = sampleRow(1L, 9L, "O1", "20240618");
		row.setTransAmt(1000);
		when(cashRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
				.thenAnswer(inv -> {
					Page<HfpayCashRecord> p = inv.getArgument(0);
					p.setRecords(List.of(row));
					return p;
				});
		when(valueOperations.setIfAbsent(eq("hfpay:1"), any(String.class))).thenReturn(true);
		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("mer_cust_id", "M1");
		when(paymentSettingService.loadForCompany(9L)).thenReturn(setting);
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("resp_code", "C00000");
		q.put("trans_stat", "S");
		q.put("trans_amt", "12.50");
		q.put("resp_desc", "ok");
		when(qry008Port.qry008(any(), any())).thenReturn(q);
		assertThat(hfpayCashRecordService.scheduleCheckStatus()).isTrue();
		verify(cashRecordMapper, times(1)).update(isNull(), any(Wrapper.class));
		verify(hfpayDistributorWithdrawSuccessEventPublishPort, times(1))
				.publishSyncAfterScheduleWithdrawSuccess(eq(1L), eq(9L), eq(1L), eq(1000), eq("O1"));
	}

	@Test
	@DisplayName("§3 4.2.2: 非 C00000/C00001/C00002 的失败码 → cash_status=3")
	void section3_4_2_2_definiteFailure_updatesStatus3() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		HfpayCashRecord row = sampleRow(1L, 9L, "O1", "20240618");
		when(cashRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
				.thenAnswer(inv -> {
					Page<HfpayCashRecord> p = inv.getArgument(0);
					p.setRecords(List.of(row));
					return p;
				});
		when(valueOperations.setIfAbsent(eq("hfpay:1"), any(String.class))).thenReturn(true);
		when(paymentSettingService.loadForCompany(9L))
				.thenReturn(new LinkedHashMap<>(Map.of("mer_cust_id", "M1")));
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("resp_code", "C99999");
		q.put("resp_desc", "fail");
		q.put("trans_amt", "0");
		when(qry008Port.qry008(any(), any())).thenReturn(q);
		assertThat(hfpayCashRecordService.scheduleCheckStatus()).isTrue();
		verify(cashRecordMapper, times(1)).update(isNull(), any(Wrapper.class));
		verify(hfpayDistributorWithdrawSuccessEventPublishPort, never())
				.publishSyncAfterScheduleWithdrawSuccess(anyLong(), anyLong(), anyLong(), anyInt(), any());
	}

	@Test
	@DisplayName("§3 4.2.3: C00001 不置 2/3，不 UPDATE")
	void section3_4_2_3_pendingCodes_noUpdate() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		HfpayCashRecord row = sampleRow(1L, 9L, "O1", "20240618");
		when(cashRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
				.thenAnswer(inv -> {
					Page<HfpayCashRecord> p = inv.getArgument(0);
					p.setRecords(List.of(row));
					return p;
				});
		when(valueOperations.setIfAbsent(eq("hfpay:1"), any(String.class))).thenReturn(true);
		when(paymentSettingService.loadForCompany(9L))
				.thenReturn(new LinkedHashMap<>(Map.of("mer_cust_id", "M1")));
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("resp_code", "C00001");
		when(qry008Port.qry008(any(), any())).thenReturn(q);
		assertThat(hfpayCashRecordService.scheduleCheckStatus()).isTrue();
		verify(cashRecordMapper, never()).update(isNull(), any(Wrapper.class));
	}

	@Test
	@DisplayName("§3 4.2.3: C00002 不置 2/3，不 UPDATE")
	void section3_4_2_3_c00002_pendingCodes_noUpdate() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		HfpayCashRecord row = sampleRow(1L, 9L, "O1", "20240618");
		when(cashRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
				.thenAnswer(inv -> {
					Page<HfpayCashRecord> p = inv.getArgument(0);
					p.setRecords(List.of(row));
					return p;
				});
		when(valueOperations.setIfAbsent(eq("hfpay:1"), any(String.class))).thenReturn(true);
		when(paymentSettingService.loadForCompany(9L))
				.thenReturn(new LinkedHashMap<>(Map.of("mer_cust_id", "M1")));
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("resp_code", "C00002");
		when(qry008Port.qry008(any(), any())).thenReturn(q);
		assertThat(hfpayCashRecordService.scheduleCheckStatus()).isTrue();
		verify(cashRecordMapper, never()).update(isNull(), any(Wrapper.class));
	}

	@Test
	@DisplayName("§3 4.2.4: qry008 抛异常则向上冒泡")
	void section3_4_2_4_qry008Throws_propagates() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		HfpayCashRecord row = sampleRow(1L, 9L, "O1", "20240618");
		when(cashRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
				.thenAnswer(inv -> {
					Page<HfpayCashRecord> p = inv.getArgument(0);
					p.setRecords(List.of(row));
					return p;
				});
		when(valueOperations.setIfAbsent(eq("hfpay:1"), any(String.class))).thenReturn(true);
		when(paymentSettingService.loadForCompany(9L))
				.thenReturn(new LinkedHashMap<>(Map.of("mer_cust_id", "M1")));
		when(qry008Port.qry008(any(), any())).thenThrow(new ResourceException("boom"));
		assertThatThrownBy(() -> hfpayCashRecordService.scheduleCheckStatus())
				.isInstanceOf(ResourceException.class);
	}

	@Test
	@DisplayName("§3 1: 构造筛选 cash_status=1 且 created_at<=now-2d")
	@SuppressWarnings("unchecked")
	void section3_1_filterByCashStatusAndCutoff() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
		hfpayCashRecordService.scheduleCheckStatus();
		ArgumentCaptor<Wrapper> cap = ArgumentCaptor.forClass(Wrapper.class);
		verify(cashRecordMapper).selectCount(cap.capture());
		LambdaQueryWrapper<HfpayCashRecord> w = (LambdaQueryWrapper<HfpayCashRecord>) cap.getValue();
		assertThat(w.getSqlSegment()).contains("cash_status");
		assertThat(w.getSqlSegment()).contains("created");
		assertThat(w.getParamNameValuePairs().values()).contains(1);
		LocalDateTime cut =
				w.getParamNameValuePairs().values().stream()
						.map(v -> (v instanceof LocalDateTime) ? (LocalDateTime) v : null)
						.filter(Objects::nonNull)
						.findFirst()
						.orElseThrow();
		// 与 HfpayCashRecordService#scheduleCheckStatus 中 now-2d 对齐（JVM 默认时区；与 ObjectProvider stub 是否生效无关）
		LocalDateTime ref = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime().minusDays(2);
		assertThat(Duration.between(cut, ref).abs().getSeconds()).isLessThanOrEqualTo(2L);
	}

	@Test
	@DisplayName("§3 5: 多行多分支后仍 return true")
	void section3_5_mixedRows_returnsTrue() {
		when(cashRecordMapper.selectCount(any(Wrapper.class))).thenReturn(2L);
		HfpayCashRecord a = sampleRow(1L, 9L, "O1", "20240618");
		HfpayCashRecord b = sampleRow(2L, 9L, "O2", "20240618");
		when(cashRecordMapper.selectPage(any(Page.class), any(Wrapper.class)))
				.thenAnswer(inv -> {
					Page<HfpayCashRecord> p = inv.getArgument(0);
					p.setRecords(List.of(a, b));
					return p;
				});
		when(valueOperations.setIfAbsent(eq("hfpay:1"), any(String.class))).thenReturn(false);
		when(valueOperations.get("hfpay:1")).thenReturn("1");
		when(valueOperations.setIfAbsent(eq("hfpay:2"), any(String.class))).thenReturn(false);
		when(valueOperations.get("hfpay:2")).thenReturn("1");
		assertThat(hfpayCashRecordService.scheduleCheckStatus()).isTrue();
	}

	private static HfpayCashRecord sampleRow(long id, long companyId, String orderId, String hfOrderDate) {
		HfpayCashRecord r = new HfpayCashRecord();
		r.setHfpayCashRecordId(id);
		r.setCompanyId(companyId);
		r.setDistributorId(1L);
		r.setOrderId(orderId);
		r.setHfOrderDate(hfOrderDate);
		return r;
	}
}
