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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.WorkWechatAddressBookCallbackPort;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.workwechat.service.WorkWechatCorpUserApiService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WorkWechatAddressBookCallbackService implements WorkWechatAddressBookCallbackPort {

	private final WorkWechatCorpUserApiService workWechatCorpUserApiService;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public WorkWechatAddressBookCallbackService(
			WorkWechatCorpUserApiService workWechatCorpUserApiService,
			ShopSalespersonMapper shopSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.workWechatCorpUserApiService = workWechatCorpUserApiService;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void handleAddressBookEvent(Long companyId, Map<String, Object> eventMap) {
		if (eventMap == null || eventMap.isEmpty()) {
			return;
		}
		String changeType = stringVal(eventMap.get("ChangeType"));
		String status = stringVal(eventMap.get("Status"));
		String userId = stringVal(eventMap.get("UserID"));
		String isValidTarget;
		if ("update_user".equals(changeType) && "2".equals(status)) {
			isValidTarget = "false";
		} else if ("update_user".equals(changeType) && "1".equals(status)) {
			isValidTarget = "true";
		} else if ("delete_user".equals(changeType)) {
			isValidTarget = "delete";
		} else {
			return;
		}
		long cid = companyId == null ? 0L : companyId;
		Map<String, Object> userInfo = workWechatCorpUserApiService.getUser(cid, userId);
		String mobilePlain = stringVal(userInfo.get("mobile")).trim();
		String telephonePlain = stringVal(userInfo.get("telephone")).trim();
		boolean hasMobile = StringUtils.hasText(mobilePlain);
		boolean hasTel = StringUtils.hasText(telephonePlain);
		if (!hasMobile && !hasTel) {
			throw new ResourceException("error.");
		}
		LambdaQueryWrapper<ShopSalesperson> q =
				new LambdaQueryWrapper<ShopSalesperson>().eq(ShopSalesperson::getSalespersonType, "shopping_guide");
		if (hasMobile && hasTel) {
			String encM = sensitiveFieldEncryptor.encrypt(mobilePlain);
			String encT = sensitiveFieldEncryptor.encrypt(telephonePlain);
			q.and(w -> w.eq(ShopSalesperson::getMobile, encM).or().eq(ShopSalesperson::getMobile, encT));
		} else if (hasMobile) {
			q.eq(ShopSalesperson::getMobile, sensitiveFieldEncryptor.encrypt(mobilePlain));
		} else {
			q.eq(ShopSalesperson::getMobile, sensitiveFieldEncryptor.encrypt(telephonePlain));
		}
		ShopSalesperson sp = shopSalespersonMapper.selectOne(q.last("LIMIT 1"));
		if (sp == null || sp.getSalespersonId() == null) {
			throw new ResourceException("error.");
		}
		long nowEpoch = Instant.now().getEpochSecond();
		LambdaUpdateWrapper<ShopSalesperson> u = new LambdaUpdateWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, sp.getSalespersonId())
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide");
		if (companyId != null) {
			u.eq(ShopSalesperson::getCompanyId, companyId);
		}
		u.set(ShopSalesperson::getIsValid, isValidTarget).set(ShopSalesperson::getUpdated, nowEpoch);
		int n = shopSalespersonMapper.update(null, u);
		if (n == 0) {
			throw new ResourceException("error.");
		}
		log.info("address book callback updated salesperson_id={} is_valid={}", sp.getSalespersonId(), isValidTarget);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
