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
import cn.shopex.ecshopx.companys.domain.Resources;
import cn.shopex.ecshopx.companys.domain.ResourcesOpLog;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.ResourcesMapper;
import cn.shopex.ecshopx.companys.mapper.ResourcesOpLogMapper;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WxShopsSetResourceService {

	private final WxShopsJwtShopIdWhitelist whitelist;
	private final WxShopsMapper wxShopsMapper;
	private final ResourcesMapper resourcesMapper;
	private final ResourcesOpLogMapper resourcesOpLogMapper;

	public WxShopsSetResourceService(
			WxShopsJwtShopIdWhitelist whitelist,
			WxShopsMapper wxShopsMapper,
			ResourcesMapper resourcesMapper,
			ResourcesOpLogMapper resourcesOpLogMapper) {
		this.whitelist = whitelist;
		this.wxShopsMapper = wxShopsMapper;
		this.resourcesMapper = resourcesMapper;
		this.resourcesOpLogMapper = resourcesOpLogMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> setShopResource(
			long companyId, long wxShopId, long resourceId, Map<String, Object> operatorJwt) {
		List<Long> allowed = whitelist.allowedShopIds(operatorJwt.get("shop_ids"));
		if (!allowed.isEmpty() && !allowed.contains(wxShopId)) {
			throw new ForbiddenException("您没有此项操作权限");
		}

		WxShops wx = wxShopsMapper.selectById(wxShopId);
		if (wx == null) {
			throw new ResourceException("需要更新的门店不存在");
		}
		final String storeNameForLog = wx.getStoreName();

		Resources res =
				resourcesMapper.selectOne(
						new LambdaQueryWrapper<Resources>()
								.eq(Resources::getCompanyId, companyId)
								.eq(Resources::getResourceId, resourceId));
		if (res == null) {
			throw new ResourceException("资源包不在");
		}

		if (!Objects.equals(resourceId, wx.getResourceId())) {
			Long resExpiredAt = res.getExpiredAt();
			wx.setResourceId(resourceId);
			wx.setExpiredAt(resExpiredAt);
			wx.setUpdated((int) (System.currentTimeMillis() / 1000L));
			wxShopsMapper.updateById(wx);

			Resources locked = resourcesMapper.selectById(resourceId);
			if (locked == null) {
				throw new ResourceException("resource_id为" + resourceId + "的资源包不存在");
			}
			if (locked.getLeftShopNum() == null || locked.getLeftShopNum() <= 0) {
				throw new ResourceException("resource_id为" + resourceId + "的资源包店铺限制已达上限");
			}
			int affected =
					resourcesMapper.update(
							null,
							new LambdaUpdateWrapper<Resources>()
									.eq(Resources::getResourceId, resourceId)
									.gt(Resources::getLeftShopNum, 0)
									.setSql("left_shop_num = left_shop_num - 1"));
			if (affected == 0) {
				throw new ResourceException("resource_id为" + resourceId + "的资源包店铺限制已达上限");
			}
			res = resourcesMapper.selectById(resourceId);
		}

		ResourcesOpLog op = new ResourcesOpLog();
		op.setStoreName(storeNameForLog);
		op.setShopId(wxShopId);
		op.setResourceId(resourceId);
		op.setCompanyId(companyId);
		op.setOpType("occupy");
		op.setOpTime((int) Instant.now().getEpochSecond());
		op.setOpNum(1);
		resourcesOpLogMapper.insert(op);

		return toResourceStatusMap(res);
	}

	private static Map<String, Object> toResourceStatusMap(Resources r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("resource_id", r.getResourceId());
		m.put("resource_name", r.getResourceName());
		m.put("company_id", r.getCompanyId());
		m.put("eid", r.getEid());
		m.put("passport_uid", r.getPassportUid());
		m.put("shop_num", r.getShopNum());
		m.put("left_shop_num", r.getLeftShopNum());
		m.put("source", r.getSource());
		m.put("available_days", r.getAvailableDays());
		m.put("active_at", r.getActiveAt());
		m.put("expired_at", r.getExpiredAt());
		m.put("active_code", r.getActiveCode());
		m.put("issue_id", r.getIssueId());
		m.put("goods_code", r.getGoodsCode());
		m.put("product_code", r.getProductCode());
		return m;
	}
}
