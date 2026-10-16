package cn.shopex.ecshopx.promotions.service.membersignin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninReminder;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninReminderMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberSignInReminderServiceTest {

	@Mock private MemberSigninReminderMapper reminderMapper;
	@Mock private MessageSource messageSource;
	@Mock private MemberSignInReminderPushService reminderPushService;

	private MemberSignInReminderService service;

	@BeforeEach
	void setUp() {
		when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(2));
		service = new MemberSignInReminderService(reminderMapper, messageSource, reminderPushService);
	}

	@Test
	void saveReminder_requiresSubscribeWhenEnabling() {
		when(reminderMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		assertThatThrownBy(
						() ->
								service.saveReminder(
										1L, 2L, Map.of("enabled", true), Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void subscribeCallback_enablesReminderWhenAccepted() {
		when(reminderMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> out =
				service.subscribeCallback(
						1L,
						2L,
						Map.of("template_id", "tmpl-1"),
						Locale.SIMPLIFIED_CHINESE);

		assertThat(out.get("enabled")).isEqualTo(true);
		assertThat(out.get("subscribe_accepted")).isEqualTo(true);
		assertThat(out.get("subscribed")).isEqualTo(true);
		verify(reminderMapper).insert(any(MemberSigninReminder.class));
	}

	@Test
	void subscribeCallback_requiresTemplateIdWhenAccepted() {
		assertThatThrownBy(
						() ->
								service.subscribeCallback(
										1L,
										2L,
										Map.of("accepted", true),
										Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class);
	}
}
