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

package cn.shopex.ecshopx.kaquan.integration;

import cn.shopex.ecshopx.common.kaquan.port.GradeCardPackageOnUpgradeTriggerPort;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageSetTriggerPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 默认走 {@link CardPackageSetTriggerPackageService#triggerPackage}，与 PHP {@code PackageSetService::triggerPackage} 对齐。
 */
@Component
@RequiredArgsConstructor
public class GradeCardPackageOnUpgradeTriggerPortImpl implements GradeCardPackageOnUpgradeTriggerPort {

	private final CardPackageSetTriggerPackageService cardPackageSetTriggerPackageService;

	@Override
	public void trigger(long companyId, long userId, long newGradeId, String triggerType, boolean reissue) {
		cardPackageSetTriggerPackageService.triggerPackage(companyId, userId, newGradeId, triggerType, reissue);
	}
}
