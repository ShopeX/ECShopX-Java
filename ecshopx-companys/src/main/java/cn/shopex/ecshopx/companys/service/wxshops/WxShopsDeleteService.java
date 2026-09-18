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

import cn.shopex.ecshopx.companys.domain.Resources;
import cn.shopex.ecshopx.companys.domain.ResourcesOpLog;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.ResourcesMapper;
import cn.shopex.ecshopx.companys.mapper.ResourcesOpLogMapper;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.mendian.WxMendianApiClient;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WxShopsDeleteService {

	private final WxShopsJwtShopIdWhitelist whitelist;
	private final WxShopsMapper wxShopsMapper;
	private final ResourcesMapper resourcesMapper;
	private final ResourcesOpLogMapper resourcesOpLogMapper;
	private final WxMendianApiClient wxMendianApiClient;
	private final WxShopsDeleteService self;

	public WxShopsDeleteService(
			WxShopsJwtShopIdWhitelist whitelist,
			WxShopsMapper wxShopsMapper,
			ResourcesMapper resourcesMapper,
			ResourcesOpLogMapper resourcesOpLogMapper,
			WxMendianApiClient wxMendianApiClient,
			@Lazy WxShopsDeleteService self) {
		this.whitelist = whitelist;
		this.wxShopsMapper = wxShopsMapper;
		this.resourcesMapper = resourcesMapper;
		this.resourcesOpLogMapper = resourcesOpLogMapper;
		this.wxMendianApiClient = wxMendianApiClient;
		this.self = self;
	}

	public void deleteWxShops(long wxShopId, Map<String, Object> operatorJwt, long companyId) {
		List<Long> allowed = whitelist.allowedShopIds(operatorJwt.get("shop_ids"));
		if (!allowed.isEmpty() && !allowed.contains(wxShopId)) {
			throw new ResourceException("您没有此项操作权限", 500, 400500);
		}

		WxShops row = wxShopsMapper.selectById(wxShopId);
		if (row == null) {
			throw new ResourceException("wx_shop_id=" + wxShopId + "的微信店铺不存在");
		}

		Long rowCompanyId = row.getCompanyId();
		if (!Objects.equals(companyId, rowCompanyId)) {
			throw new ResourceException("删除门店信息有误，请确认您的门店再删除.");
		}

		Object rawAid = operatorJwt.get("authorizer_appid");
		String authorizerAppid = rawAid == null ? "" : rawAid.toString().trim();
		if (authorizerAppid.isEmpty()) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}

		String poiId = row.getPoiId() == null ? "" : row.getPoiId().trim();
		if (poiId.isBlank()) {
			throw new ResourceException("删除微信门店失败.");
		}
		wxMendianApiClient.delStore(authorizerAppid, poiId);

		int nowSeconds = (int) (System.currentTimeMillis() / 1000L);
		Long resourceId = row.getResourceId();
		Long expiredAt = row.getExpiredAt();
		if (resourceId != null
				&& resourceId > 0L
				&& expiredAt != null
				&& nowSeconds < expiredAt) {
			Resources res = resourcesMapper.selectById(resourceId);
			if (res == null) {
				throw new ResourceException("要更新的资源包不存在");
			}
		}

		self.deleteLocalAfterWechat(wxShopId, companyId, nowSeconds);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteLocalAfterWechat(long wxShopId, long companyId, int nowSeconds) {
		WxShops inTx = wxShopsMapper.selectById(wxShopId);
		if (inTx == null) {
			throw new ResourceException("wx_shop_id=" + wxShopId + "的微信店铺不存在");
		}

		Long rid = inTx.getResourceId();
		if (rid != null && rid > 0L) {
			Resources res2 = resourcesMapper.selectById(rid);
			Long ex = inTx.getExpiredAt();
			if (res2 != null && ex != null && nowSeconds < ex) {
				if (res2.getLeftShopNum() == null || res2.getLeftShopNum() == 0) {
					throw new ResourceException("resource_id为" + rid + "的资源包店铺限制已达上限");
				}
				int affected =
						resourcesMapper.update(
								null,
								new LambdaUpdateWrapper<Resources>()
										.eq(Resources::getResourceId, rid)
										.gt(Resources::getLeftShopNum, 0)
										.setSql("left_shop_num = left_shop_num + 1"));
				if (affected == 0) {
					throw new ResourceException("resource_id为" + rid + "的资源包店铺限制已达上限");
				}

				ResourcesOpLog op = new ResourcesOpLog();
				op.setStoreName(inTx.getStoreName());
				op.setShopId(wxShopId);
				op.setResourceId(rid);
				op.setCompanyId(companyId);
				op.setOpType("release");
				op.setOpTime(nowSeconds);
				op.setOpNum(1);
				resourcesOpLogMapper.insert(op);
			}
		}

		int n = wxShopsMapper.deleteById(wxShopId);
		if (n == 0) {
			throw new ResourceException("wx_shop_id=" + wxShopId + "的微信店铺不存在");
		}
	}
}
