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

package cn.shopex.ecshopx.companys.service.wxshops;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WxShopsSetDefaultService {

	private final WxShopsMapper wxShopsMapper;
	private final WxShopsJwtShopIdWhitelist whitelist;

	public WxShopsSetDefaultService(WxShopsMapper wxShopsMapper, WxShopsJwtShopIdWhitelist whitelist) {
		this.wxShopsMapper = wxShopsMapper;
		this.whitelist = whitelist;
	}

	@Transactional(rollbackFor = Exception.class)
	public void setDefaultWxShop(long companyId, long wxShopId, Map<String, Object> operatorJwt) {
		List<Long> allowed = whitelist.allowedShopIds(operatorJwt.get("shop_ids"));
		if (!allowed.isEmpty() && !allowed.contains(wxShopId)) {
			throw new ForbiddenException("您没有此项操作权限");
		}

		WxShops row = wxShopsMapper.selectById(wxShopId);
		if (row == null) {
			throw new ResourceException("需要更新的门店不存在");
		}

		WxShops target = wxShopsMapper.selectById(wxShopId);
		if (target == null) {
			throw new ResourceException("wx_shop_id=" + wxShopId + "的微信店铺不存在");
		}

		wxShopsMapper.update(
				null,
				new LambdaUpdateWrapper<WxShops>()
						.eq(WxShops::getCompanyId, companyId)
						.set(WxShops::getIsDefault, false));

		target.setIsDefault(true);
		target.setUpdated((int) (System.currentTimeMillis() / 1000L));
		wxShopsMapper.updateById(target);
	}
}
