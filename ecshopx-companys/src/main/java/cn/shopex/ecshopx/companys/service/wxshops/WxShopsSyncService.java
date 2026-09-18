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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import cn.shopex.ecshopx.wechat.mendian.WxMendianApiClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxShopsSyncService {

	private final WxMendianApiClient wxMendianApiClient;
	private final WxShopsMapper wxShopsMapper;
	private final ObjectMapper objectMapper;

	public WxShopsSyncService(
			WxMendianApiClient wxMendianApiClient,
			WxShopsMapper wxShopsMapper,
			ObjectMapper objectMapper) {
		this.wxMendianApiClient = wxMendianApiClient;
		this.wxShopsMapper = wxShopsMapper;
		this.objectMapper = objectMapper;
	}

	public void syncWxShops(long companyId, String authorizerAppid) {
		checkMerchantAuditInfo(authorizerAppid);

		int limit = 10;
		int pageCount = 100;
		for (int pageNo = 0; pageNo < pageCount; pageNo++) {
			JsonNode listRoot = wxMendianApiClient.getStoreList(authorizerAppid, pageNo * limit, limit);
			int totalCount = listRoot.path("total_count").asInt(0);
			int totalPage = (int) Math.ceil(totalCount / (double) limit);
			saveWxShops(companyId, listRoot.path("business_list"));
			if (pageNo > totalPage) {
				break;
			}
		}
	}

	private void checkMerchantAuditInfo(String authorizerAppid) {
		JsonNode root = wxMendianApiClient.getMerchantAuditInfo(authorizerAppid);
		if (!WxMendianApiClient.errcodeIsZero(root.path("errcode"))) {
			throw new ResourceException("查询门店小程序失败.");
		}
		JsonNode data = root.path("data");
		if (data.isMissingNode() || data.isNull()) {
			throw new ResourceException("查询门店小程序失败.");
		}
		int status;
		JsonNode st = data.path("status");
		if (st.isMissingNode() || st.isNull()) {
			status = -999;
		} else if (st.isNumber()) {
			status = st.intValue();
		} else if (st.isTextual()) {
			String t = st.asText().trim();
			if (t.isEmpty()) {
				status = -999;
			} else {
				try {
					status = Integer.parseInt(t);
				} catch (NumberFormatException e) {
					throw new ResourceException("查询门店小程序失败.");
				}
			}
		} else {
			throw new ResourceException("查询门店小程序失败.");
		}
		switch (status) {
			case 0:
				throw new ResourceException("未提交门店小程序申请.");
			case 1:
				break;
			case 2:
				throw new ResourceException("门店小程序正在审核中，暂时不能做门店相关操作，请耐心等待.");
			case 3: {
				String reason = data.path("reason").asText("");
				throw new ResourceException("门店小程序审核失败，请检查原因，重新申请.(" + reason + ")");
			}
			case 4: {
				String reason = data.path("reason").asText("");
				throw new ResourceException("管理员拒绝，请检查原因，重新申请.(" + reason + ")");
			}
			default:
				break;
		}
	}

	private void saveWxShops(long companyId, JsonNode businessListArray) {
		if (businessListArray == null || !businessListArray.isArray()) {
			return;
		}
		for (JsonNode v : businessListArray) {
			JsonNode info = v.path("base_info");
			if (!info.isObject()) {
				continue;
			}
			String poiId = info.path("poi_id").asText(null);
			if (!StringUtils.hasText(poiId)) {
				continue;
			}
			WxShops existing =
					wxShopsMapper.selectOne(
							new LambdaQueryWrapper<WxShops>()
									.eq(WxShops::getPoiId, poiId)
									.last("LIMIT 1"));

			String storeName = info.path("business_name").asText("");
			String lng = info.path("longitude").asText("");
			String lat = info.path("latitude").asText("");
			String address =
					info.path("province").asText("")
							+ info.path("city").asText("")
							+ info.path("district").asText("")
							+ info.path("address").asText("");
			String category;
			JsonNode categoriesNode = info.path("categories");
			if (categoriesNode.isArray()) {
				List<String> parts = new ArrayList<>();
				for (JsonNode el : categoriesNode) {
					parts.add(el.asText(""));
				}
				category = String.join(":", parts);
			} else {
				category = "";
			}
			String picListJson = buildPicListJson(info.path("photo_list"));
			String contractPhone = info.path("telephone").asText("");
			String hour = info.path("open_time").asText("");
			String qualificationName = info.path("qualification_name").asText("");
			int addType = qualificationName.trim().isEmpty() ? 1 : 2;
			String companyName = qualificationName;
			String credential = info.path("qualification_num").asText("");
			int wxStatus = info.path("status").asInt(5);
			int now = (int) (System.currentTimeMillis() / 1000L);

			boolean useUpdate =
					existing != null
							&& StringUtils.hasText(existing.getPoiId())
							&& existing.getWxShopId() != null
							&& existing.getWxShopId() > 0L;
			if (useUpdate) {
				WxShops toUpdate = new WxShops();
				toUpdate.setWxShopId(existing.getWxShopId());
				toUpdate.setStoreName(storeName);
				toUpdate.setPoiId(poiId);
				toUpdate.setLng(lng);
				toUpdate.setLat(lat);
				toUpdate.setAddress(address);
				toUpdate.setCategory(category);
				toUpdate.setPicList(picListJson);
				toUpdate.setContractPhone(contractPhone);
				toUpdate.setHour(hour);
				toUpdate.setAddType(addType);
				toUpdate.setCompanyName(companyName);
				toUpdate.setCredential(credential);
				toUpdate.setStatus(wxStatus);
				toUpdate.setCompanyId(companyId);
				toUpdate.setUpdated(now);
				wxShopsMapper.updateById(toUpdate);
			} else {
				WxShops entity = new WxShops();
				entity.setStoreName(storeName);
				entity.setPoiId(poiId);
				entity.setLng(lng);
				entity.setLat(lat);
				entity.setAddress(address);
				entity.setCategory(category);
				entity.setPicList(picListJson);
				entity.setContractPhone(contractPhone);
				entity.setHour(hour);
				entity.setAddType(addType);
				entity.setCompanyName(companyName);
				entity.setCredential(credential);
				entity.setStatus(wxStatus);
				entity.setCompanyId(companyId);
				entity.setDistributorId(0L);
				entity.setCreated(now);
				entity.setUpdated(now);
				wxShopsMapper.insert(entity);
			}
		}
	}

	private String buildPicListJson(JsonNode photoList) {
		List<String> picUrls = new ArrayList<>();
		if (photoList.isArray()) {
			for (JsonNode p : photoList) {
				picUrls.add(p.path("photo_url").asText(""));
			}
		}
		try {
			return objectMapper.writeValueAsString(picUrls);
		} catch (JsonProcessingException e) {
			throw new ResourceException("门店数据序列化失败");
		}
	}
}
