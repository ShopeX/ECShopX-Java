package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class MemberTaskSkeletonServiceTest {

	@Mock
	private MessageSource messageSource;

	private MemberTaskSkeletonService service;

	@BeforeEach
	void setUp() {
		service = new MemberTaskSkeletonService(messageSource);
	}

	@Test
	void throwNotImplemented_zh() {
		when(messageSource.getMessage(
						eq("promotions.member_task.not_implemented"),
						eq(null),
						eq("该功能尚未开放"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("该功能尚未开放");

		assertThatThrownBy(() -> service.throwNotImplemented(Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("该功能尚未开放");
	}

	@Test
	void throwNotImplemented_en() {
		when(messageSource.getMessage(
						eq("promotions.member_task.not_implemented"),
						eq(null),
						eq("该功能尚未开放"),
						eq(Locale.US)))
				.thenReturn("This feature is not available yet");

		assertThatThrownBy(() -> service.throwNotImplemented(Locale.US))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("This feature is not available yet");
	}
}
