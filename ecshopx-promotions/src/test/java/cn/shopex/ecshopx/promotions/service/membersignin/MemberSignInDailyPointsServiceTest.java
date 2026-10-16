package cn.shopex.ecshopx.promotions.service.membersignin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDailyPoints;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninDailyPointsMapper;
import java.util.LinkedHashMap;
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
class MemberSignInDailyPointsServiceTest {

	@Mock private MemberSigninDailyPointsMapper dailyPointsMapper;
	@Mock private MessageSource messageSource;

	private MemberSignInDailyPointsService service;

	@BeforeEach
	void setUp() {
		when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(2));
		service = new MemberSignInDailyPointsService(dailyPointsMapper, messageSource);
	}

	@Test
	void getDailyPoints_returnsZerosWhenMissingRow() {
		when(dailyPointsMapper.selectById(1L)).thenReturn(null);

		Map<String, Object> result = service.getDailyPoints(1L);

		assertThat(result)
				.containsEntry("points_mon", 0)
				.containsEntry("points_sun", 0)
				.containsEntry("rule_desc", "");
	}

	@Test
	void saveDailyPoints_insertsWhenMissing() {
		when(dailyPointsMapper.selectById(1L)).thenReturn(null);
		Map<String, Object> input = Map.of(
				"points_mon", 10,
				"points_tue", 0,
				"points_wed", 5,
				"rule_desc", "<p>每日签到一次</p>");

		Map<String, Object> result = service.saveDailyPoints(1L, input, Locale.SIMPLIFIED_CHINESE);

		ArgumentCaptor<MemberSigninDailyPoints> captor = ArgumentCaptor.forClass(MemberSigninDailyPoints.class);
		verify(dailyPointsMapper).insert(captor.capture());
		MemberSigninDailyPoints saved = captor.getValue();
		assertThat(saved.getCompanyId()).isEqualTo(1L);
		assertThat(saved.getPointsMon()).isEqualTo(10);
		assertThat(saved.getPointsWed()).isEqualTo(5);
		assertThat(saved.getPointsSun()).isZero();
		assertThat(saved.getRuleDesc()).isEqualTo("<p>每日签到一次</p>");
		assertThat(result)
				.containsEntry("points_mon", 10)
				.containsEntry("rule_desc", "<p>每日签到一次</p>");
	}

	@Test
	void saveDailyPoints_updatesExistingRow() {
		MemberSigninDailyPoints existing = new MemberSigninDailyPoints();
		existing.setCompanyId(2L);
		existing.setPointsMon(1);
		when(dailyPointsMapper.selectById(2L)).thenReturn(existing);

		service.saveDailyPoints(2L, Map.of("points_mon", 20), Locale.SIMPLIFIED_CHINESE);

		verify(dailyPointsMapper).updateById(existing);
		assertThat(existing.getPointsMon()).isEqualTo(20);
		assertThat(existing.getRuleDesc()).isEmpty();
	}

	@Test
	void saveDailyPoints_rejectsOversizedRuleDesc() {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("rule_desc", "x".repeat(65536));

		assertThatThrownBy(() -> service.saveDailyPoints(1L, input, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void saveDailyPoints_rejectsNegativeValue() {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("points_mon", -1);

		assertThatThrownBy(() -> service.saveDailyPoints(1L, input, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void saveDailyPoints_rejectsDecimalValue() {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("points_fri", 1.5);

		assertThatThrownBy(() -> service.saveDailyPoints(1L, input, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class);
	}
}
