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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapaySubmitLicense;
import cn.shopex.ecshopx.adapay.mapper.AdapaySubmitLicenseMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenAccountService {

	private final AdapaySubmitLicenseMapper adapaySubmitLicenseMapper;
	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;

	public void scheduleGetSubmitLicenseStatus() {
		LambdaQueryWrapper<AdapaySubmitLicense> w = new LambdaQueryWrapper<>();
		w.eq(AdapaySubmitLicense::getAuditStatus, "I").orderByAsc(AdapaySubmitLicense::getId);
		List<AdapaySubmitLicense> list = adapaySubmitLicenseMapper.selectList(w);
		if (list == null || list.isEmpty()) {
			return;
		}
		for (AdapaySubmitLicense row : list) {
			adapayPaymentSettingRedisReader.getPaymentSetting(row.getCompanyId());
			throw new ResourceException("暂不支持开户流程");
		}
	}
}
