package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageRowMapperService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsRowMapperService;
import cn.shopex.ecshopx.promotions.domain.LuckyDrawActivity;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableAdminSaveRules;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntablePrizeDataSchema;
import cn.shopex.ecshopx.promotions.mapper.LuckyDrawActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.LuckyDrawActivityOutsideMultiLangReadService;
import cn.shopex.ecshopx.promotions.service.multilang.LuckyDrawActivityOutsideMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TurntableConfigServiceBe02Test {

	@Mock private LuckyDrawActivityMapper luckyDrawActivityMapper;
	@Mock private LuckyDrawActivityOutsideMultiLangWriteService multiLangWrite;
	@Mock private LuckyDrawActivityOutsideMultiLangReadService multiLangRead;
	@Mock private DiscountCardsMapper discountCardsMapper;
	@Mock private CardPackageMapper cardPackageMapper;
	@Mock private DiscountCardsRowMapperService discountCardsRowMapperService;
	@Mock private CardPackageRowMapperService cardPackageRowMapperService;
	@Mock private MessageSource messageSource;

	private TurntableConfigService service;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(2));
		service =
				new TurntableConfigService(
						luckyDrawActivityMapper,
						multiLangWrite,
						multiLangRead,
						discountCardsMapper,
						cardPackageMapper,
						discountCardsRowMapperService,
						cardPackageRowMapperService,
						objectMapper,
						messageSource);
	}

	@Test
	void down_rejectsAlreadyEnded() {
		long now = Instant.now().getEpochSecond();
		LuckyDrawActivity row = baseActivity(10L, now - 1000, now - 1, 1L);
		when(luckyDrawActivityMapper.selectOne(any(Wrapper.class))).thenReturn(row);

		assertThatThrownBy(() -> service.downLuckyDrawActivity(1L, "10"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("已结束");
	}

	@Test
	void down_rejectsNotInProgress() {
		long now = Instant.now().getEpochSecond();
		LuckyDrawActivity row = baseActivity(10L, now + 100, now + 1000, 1L);
		when(luckyDrawActivityMapper.selectOne(any(Wrapper.class))).thenReturn(row);

		assertThatThrownBy(() -> service.downLuckyDrawActivity(1L, "10"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("进行中");
	}

	@Test
	void down_setsEndTimeToNow_whenInProgress() {
		long now = Instant.now().getEpochSecond();
		LuckyDrawActivity row = baseActivity(10L, now - 10, now + 1000, 1L);
		when(luckyDrawActivityMapper.selectOne(any(Wrapper.class))).thenReturn(row);
		when(luckyDrawActivityMapper.update(any(), any())).thenReturn(1);

		service.downLuckyDrawActivity(1L, "10");

		verify(luckyDrawActivityMapper).update(any(), any());
	}

	@Test
	void copy_regeneratesPrizeIds() throws Exception {
		List<Map<String, Object>> prizes = new ArrayList<>();
		prizes.add(TurntablePrizeDataSchema.sampleThanks("old-a", 1, 100));
		LuckyDrawActivity source = baseActivity(7L, 1L, Instant.now().getEpochSecond() + 86400, 3L);
		source.setPrizeData(objectMapper.writeValueAsString(prizes));
		source.setActivityName("源活动");
		source.setCostValue(10L);
		source.setLimitTotal(0L);
		source.setLimitDay(0L);
		when(luckyDrawActivityMapper.selectOne(any(Wrapper.class))).thenReturn(source);
		when(luckyDrawActivityMapper.insert(any(LuckyDrawActivity.class)))
				.thenAnswer(
						inv -> {
							LuckyDrawActivity e = inv.getArgument(0);
							e.setId(99L);
							return 1;
						});

		Map<String, Object> out = service.copyTurntableActivity(1L, "7", "zh-CN");

		assertThat(out.get("id")).isEqualTo(99L);
		assertThat(out.get("config_version")).isEqualTo(1L);
		ArgumentCaptor<LuckyDrawActivity> cap = ArgumentCaptor.forClass(LuckyDrawActivity.class);
		verify(luckyDrawActivityMapper).insert(cap.capture());
		List<Map<String, Object>> copied =
				objectMapper.readValue(cap.getValue().getPrizeData(), List.class);
		assertThat(((Map<?, ?>) copied.get(0)).get("prize_id")).isNotEqualTo("old-a");
		assertThat(cap.getValue().getCostType()).isEqualTo(TurntableAdminSaveRules.COST_TYPE_POINT);
		assertThat(cap.getValue().getActivityName()).contains("副本");
	}

	@Test
	void save_update_rejectsVersionConflict() {
		long now = Instant.now().getEpochSecond();
		LuckyDrawActivity existing = baseActivity(5L, now - 10, now + 1000, 2L);
		existing.setPrizeData(
				"[{\"prize_id\":\"t1\",\"sort\":1,\"name\":\"谢谢\",\"type\":\"thanks\",\"probability\":100}]");
		when(luckyDrawActivityMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
		when(luckyDrawActivityMapper.selectById(5L)).thenReturn(existing);

		Map<String, Object> merged = validSaveBody(5L, now - 10, now + 2000);
		merged.put("config_version", 1L);

		assertThatThrownBy(() -> service.setTurntableConfig(1L, merged, "zh-CN"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining(TurntableAdminSaveRules.ERR_CONFIG_VERSION_CONFLICT);
	}

	@Test
	void save_update_rejectsEndedReadonly() {
		long now = Instant.now().getEpochSecond();
		LuckyDrawActivity existing = baseActivity(5L, now - 1000, now - 1, 1L);
		when(luckyDrawActivityMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

		Map<String, Object> merged = validSaveBody(5L, now - 1000, now + 2000);
		merged.put("config_version", 1L);

		assertThatThrownBy(() -> service.setTurntableConfig(1L, merged, "zh-CN"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("已结束");
	}

	@Test
	void save_create_rejectsMissingRequiredFieldsWithUnifiedMessage() {
		long now = Instant.now().getEpochSecond();
		Map<String, Object> merged = validSaveBody(null, now + 10, now + 2000);
		merged.remove("begin_time");

		assertThatThrownBy(() -> service.setTurntableConfig(1L, merged, "zh-CN"))
				.isInstanceOf(BadRequestException.class)
				.hasMessage(TurntableAdminSaveRules.FALLBACK_REQUIRED_FIELDS_CANNOT_BE_EMPTY);
	}

	@Test
	void save_create_returnsConfigVersionOne() {
		when(luckyDrawActivityMapper.insert(any(LuckyDrawActivity.class)))
				.thenAnswer(
						inv -> {
							LuckyDrawActivity e = inv.getArgument(0);
							e.setId(88L);
							return 1;
						});
		long now = Instant.now().getEpochSecond();
		Map<String, Object> merged = validSaveBody(null, now + 10, now + 2000);

		Map<String, Object> out = service.setTurntableConfig(1L, merged, "zh-CN");

		assertThat(out.get("id")).isEqualTo(88L);
		assertThat(out.get("config_version")).isEqualTo(1L);
		ArgumentCaptor<LuckyDrawActivity> cap = ArgumentCaptor.forClass(LuckyDrawActivity.class);
		verify(luckyDrawActivityMapper).insert(cap.capture());
		assertThat(cap.getValue().getConfigVersion()).isEqualTo(1L);
		assertThat(cap.getValue().getCostType()).isEqualTo(TurntableAdminSaveRules.COST_TYPE_POINT);
		assertThat(cap.getValue().getActivityType()).isEqualTo("wheel");
	}

	private static LuckyDrawActivity baseActivity(long id, long begin, long end, long version) {
		LuckyDrawActivity row = new LuckyDrawActivity();
		row.setId(id);
		row.setCompanyId(1L);
		row.setBeginTime(begin);
		row.setEndTime(end);
		row.setConfigVersion(version);
		return row;
	}

	private Map<String, Object> validSaveBody(Long id, long begin, long end) {
		Map<String, Object> merged = new LinkedHashMap<>();
		if (id != null) {
			merged.put("id", id);
		}
		merged.put("begin_time", begin);
		merged.put("end_time", end);
		merged.put("cost_value", 10);
		merged.put("limit_total", 0);
		merged.put("limit_day", 0);
		merged.put("activity_name", "测活动");
		List<Map<String, Object>> prizes = new ArrayList<>();
		prizes.add(TurntablePrizeDataSchema.sampleThanks("t1", 1, 100));
		merged.put("prize_data", prizes);
		return merged;
	}
}
