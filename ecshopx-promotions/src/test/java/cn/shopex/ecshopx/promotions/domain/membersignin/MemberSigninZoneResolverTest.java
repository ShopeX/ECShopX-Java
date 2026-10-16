package cn.shopex.ecshopx.promotions.domain.membersignin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class MemberSigninZoneResolverTest {

	@Test
	void blankPropertyUsesSystemDefault() {
		MemberSigninZoneResolver resolver = new MemberSigninZoneResolver("");
		assertThat(resolver.resolveZone(1L)).isEqualTo(ZoneId.systemDefault());
	}

	@Test
	void configuredPropertyIsUsed() {
		MemberSigninZoneResolver resolver = new MemberSigninZoneResolver("Asia/Shanghai");
		assertThat(resolver.resolveZone(38L).getId()).isEqualTo("Asia/Shanghai");
	}
}
