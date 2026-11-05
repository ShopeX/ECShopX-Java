/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.espier.api.admin.v1;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.common.annotation.Activated;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpgradeControllerActivatedAnnotationTest {

	@Test
	@DisplayName("升级相关写操作与检测端点须带 Activated 菜单别名；agreement 保持无 Activated")
	void privilegedUpgradeRoutes_requireActivatedRouteAlias_agreementRemainsOpen() throws Exception {
		assertActivated("detectVersion", "espier.system.detect_version");
		assertActivated("upgrade", "espier.system.upgrade", HttpServletRequest.class);
		assertActivated("rollback", "espier.system.rollback");
		assertActivated("changelog", "espier.system.changelog");

		Method agreement = UpgradeController.class.getDeclaredMethod("getAgreement");
		assertThat(agreement.getAnnotation(Activated.class)).isNull();
	}

	private static void assertActivated(String methodName, String expectedAlias, Class<?>... params)
			throws NoSuchMethodException {
		Method method = UpgradeController.class.getDeclaredMethod(methodName, params);
		Activated activated = method.getAnnotation(Activated.class);
		assertThat(activated).as(methodName).isNotNull();
		assertThat(activated.routeAlias()).isEqualTo(expectedAlias);
	}
}
