package cn.shopex.ecshopx.adapay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.adapay.domain.AdapayMerchantEntry;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantEntryMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.common.port.adapay.AdapayAutoCashConfigReadWritePort;
import cn.shopex.ecshopx.common.port.adapay.AdapayDrawCashQueueEnqueuePort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdapayDrawCashServiceTest {

	private static final long NOW_SEC = 1_000_000L;
	private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(NOW_SEC), ZoneOffset.UTC);

	@Mock
	private AdapayMerchantEntryMapper merchantEntryMapper;

	@Mock
	private AdapaySettleAccountMapper settleAccountMapper;

	@Mock
	private AdapayAutoCashConfigReadWritePort autoCashPort;

	@Mock
	private AdapayDrawCashQueueEnqueuePort enqueuePort;

	@Mock
	private AdapaySubMerchantDrawCashConfigService subMerchantDrawCashConfigService;

	@Test
	@DisplayName("早退出：无商户")
	void emptyMerchants() {
		AdapayDrawCashService service = newService();
		when(merchantEntryMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<AdapayMerchantEntry>>any()))
				.thenReturn(List.of());
		assertThat(service.scheduleDrawCashQueue()).isEqualTo(0);
		verify(enqueuePort, never()).enqueueMainMerchant(anyLong());
		verify(autoCashPort, never()).putAutoCashConfig(anyLong(), any());
	}

	@Test
	@DisplayName("跳过：未开自动")
	void skipNotEnabled() {
		AdapayDrawCashService service = newService();
		AdapayMerchantEntry m = new AdapayMerchantEntry();
		m.setCompanyId(7L);
		when(merchantEntryMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<AdapayMerchantEntry>>any()))
				.thenReturn(List.of(m));
		when(autoCashPort.getAutoCashConfig(7L))
				.thenReturn(
						new LinkedHashMap<>(
								Map.of("auto_draw_cash", "N")));
		assertThat(service.scheduleDrawCashQueue()).isEqualTo(0);
		verify(subMerchantDrawCashConfigService, never()).tryAdvanceNextAutoDrawTime(any(), any());
		verify(enqueuePort, never()).enqueueMainMerchant(anyLong());
	}

	@Test
	@DisplayName("跳过：未到点")
	void skipNotYet() {
		AdapayDrawCashService service = newService();
		AdapayMerchantEntry m = new AdapayMerchantEntry();
		m.setCompanyId(7L);
		when(merchantEntryMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<AdapayMerchantEntry>>any()))
				.thenReturn(List.of(m));
		when(autoCashPort.getAutoCashConfig(7L))
				.thenReturn(
						new LinkedHashMap<>(
								Map.of(
										"auto_draw_cash",
										"Y",
										"next_time",
										NOW_SEC + 10_000L)));
		assertThat(service.scheduleDrawCashQueue()).isEqualTo(0);
		verify(subMerchantDrawCashConfigService, never()).tryAdvanceNextAutoDrawTime(any(), any());
	}

	@Test
	@DisplayName("主路径：主商户+空 settle_account_id 子账户，计数=2")
	void mainPath() {
		AdapayDrawCashService service = newService();
		AdapayMerchantEntry m = new AdapayMerchantEntry();
		m.setCompanyId(7L);
		when(merchantEntryMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<AdapayMerchantEntry>>any()))
				.thenReturn(List.of(m));
		Map<String, Object> auto = new LinkedHashMap<>();
		auto.put("auto_draw_cash", "Y");
		auto.put("next_time", NOW_SEC - 1);
		auto.put("auto_type", "day");
		auto.put("auto_time", "9:0");
		when(autoCashPort.getAutoCashConfig(7L)).thenReturn(auto);
		when(subMerchantDrawCashConfigService.tryAdvanceNextAutoDrawTime(any(), eq(CLOCK)))
				.thenReturn(true);
		AdapaySettleAccount acc = new AdapaySettleAccount();
		acc.setMemberId(11L);
		acc.setSettleAccountId("");
		when(settleAccountMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<AdapaySettleAccount>>any()))
				.thenReturn(List.of(acc));
		assertThat(service.scheduleDrawCashQueue()).isEqualTo(2);
		verify(autoCashPort, times(1)).putAutoCashConfig(eq(7L), any());
		verify(enqueuePort, times(1)).enqueueMainMerchant(7L);
		verify(enqueuePort, times(1)).enqueueSettleAccount(7L, 11L, "");
	}

	@Test
	@DisplayName("G5：类型不支持，tryAdvance 置 N 并 false，不 put、不 enqueue")
	void g5TypeUnsupported() {
		AdapayDrawCashService service = newService();
		AdapayMerchantEntry m = new AdapayMerchantEntry();
		m.setCompanyId(7L);
		when(merchantEntryMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<AdapayMerchantEntry>>any()))
				.thenReturn(List.of(m));
		Map<String, Object> auto = new LinkedHashMap<>();
		auto.put("auto_draw_cash", "Y");
		auto.put("next_time", NOW_SEC - 1);
		auto.put("auto_type", "week");
		auto.put("auto_time", "9:0");
		when(autoCashPort.getAutoCashConfig(7L)).thenReturn(auto);
		assertThat(service.scheduleDrawCashQueue()).isEqualTo(0);
		verify(autoCashPort, never()).putAutoCashConfig(anyLong(), any());
		verify(enqueuePort, never()).enqueueMainMerchant(anyLong());
	}

	private AdapayDrawCashService newService() {
		return new AdapayDrawCashService(
				merchantEntryMapper,
				settleAccountMapper,
				autoCashPort,
				enqueuePort,
				subMerchantDrawCashConfigService,
				CLOCK);
	}
}
