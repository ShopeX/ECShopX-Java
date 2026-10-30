package cn.shopex.ecshopx.selfservice.api.front.v1;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistrationActivityControllerAuthAnnotationTest {

	@Test
	@DisplayName("报名活动列表允许无 token 访问；类级仍要求登录的写接口保持 FrontAuth")
	void registrationActivityList_isFrontNoAuth_whileMutationsRemainFrontAuthOnly() throws Exception {
		assertThat(RegistrationActivityController.class.getAnnotation(FrontAuth.class)).isNotNull();

		Method list = RegistrationActivityController.class.getDeclaredMethod(
				"getRegistrationActivityList",
				HttpServletRequest.class,
				String.class,
				String.class,
				String.class,
				String.class);
		assertThat(list.getAnnotation(FrontNoAuth.class)).isNotNull();

		Method submit = RegistrationActivityController.class.getDeclaredMethod(
				"registrationSubmit",
				HttpServletRequest.class,
				long.class,
				long.class,
				long.class,
				String.class,
				Map.class);
		assertThat(submit.getAnnotation(FrontNoAuth.class)).isNull();

		Method cancel = RegistrationActivityController.class.getDeclaredMethod(
				"cancelRecord", HttpServletRequest.class, String.class, Map.class);
		assertThat(cancel.getAnnotation(FrontNoAuth.class)).isNull();
	}
}
