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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.domain.EntryApply;
import cn.shopex.ecshopx.bspay.domain.UserEnt;
import cn.shopex.ecshopx.bspay.domain.UserIndv;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BspayHuifuIdResolver {

	private final BsPayPaymentSettingService bsPayPaymentSettingService;
	private final EntryApplyMapper entryApplyMapper;
	private final UserIndvMapper userIndvMapper;
	private final UserEntMapper userEntMapper;

	public BspayHuifuIdResolver(
			BsPayPaymentSettingService bsPayPaymentSettingService,
			EntryApplyMapper entryApplyMapper,
			UserIndvMapper userIndvMapper,
			UserEntMapper userEntMapper) {
		this.bsPayPaymentSettingService = bsPayPaymentSettingService;
		this.entryApplyMapper = entryApplyMapper;
		this.userIndvMapper = userIndvMapper;
		this.userEntMapper = userEntMapper;
	}

	public String getHuifuId(long companyId, long operatorId, String operatorType) {
		if ("admin".equals(operatorType) || "staff".equals(operatorType)) {
			Map<String, Object> cfg = bsPayPaymentSettingService.requireSettingMap(companyId);
			Object sys = cfg.get("sys_id");
			return sys == null ? null : String.valueOf(sys).trim();
		}
		if (operatorId <= 0L) {
			return null;
		}
		EntryApply ea =
				entryApplyMapper.selectOne(
						new LambdaQueryWrapper<EntryApply>()
								.eq(EntryApply::getCompanyId, companyId)
								.eq(EntryApply::getOperatorId, (int) operatorId)
								.eq(EntryApply::getOperatorType, operatorType)
								.eq(EntryApply::getStatus, "APPROVED")
								.orderByDesc(EntryApply::getId)
								.last("LIMIT 1"));
		if (ea == null || !StringUtils.hasText(ea.getUserId())) {
			return null;
		}
		try {
			long uid = Long.parseLong(ea.getUserId().trim());
			UserIndv indv = userIndvMapper.selectById(uid);
			if (indv != null && StringUtils.hasText(indv.getHuifuId())) {
				return indv.getHuifuId().trim();
			}
			UserEnt ent = userEntMapper.selectById(uid);
			if (ent != null && StringUtils.hasText(ent.getHuifuId())) {
				return ent.getHuifuId().trim();
			}
		} catch (NumberFormatException ignored) {
		}
		return null;
	}
}
