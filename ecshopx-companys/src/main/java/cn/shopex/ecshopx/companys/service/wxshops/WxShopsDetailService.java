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
import cn.shopex.ecshopx.companys.domain.Resources;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.ResourcesMapper;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxShopsDetailService {

	private final WxShopsMapper wxShopsMapper;
	private final ResourcesMapper resourcesMapper;
	private final ObjectMapper objectMapper;
	private final String qqmapKey;

	public WxShopsDetailService(
			WxShopsMapper wxShopsMapper,
			ResourcesMapper resourcesMapper,
			ObjectMapper objectMapper,
			@Value("${common.qqmap-key:}") String qqmapKey) {
		this.wxShopsMapper = wxShopsMapper;
		this.resourcesMapper = resourcesMapper;
		this.objectMapper = objectMapper;
		this.qqmapKey = qqmapKey;
	}

	public Map<String, Object> getWxShopsDetail(long wxShopId) {
		WxShops row = wxShopsMapper.selectById(wxShopId);
		if (row == null) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("resource_id", null);
			return out;
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("wx_shop_id", row.getWxShopId());
		out.put("map_poi_id", row.getMapPoiId());
		out.put("store_name", row.getStoreName());
		out.put("poi_id", row.getPoiId());
		out.put("lng", row.getLng());
		out.put("lat", row.getLat());
		out.put("address", row.getAddress());
		out.put("category", row.getCategory());

		String rawPic = row.getPicList();
		if (rawPic == null || rawPic.isBlank()) {
			out.put("pic_list", null);
		} else {
			try {
				out.put("pic_list", objectMapper.readValue(rawPic, Object.class));
			} catch (JsonProcessingException ex) {
				out.put("pic_list", Collections.emptyList());
			}
		}

		out.put("contract_phone", row.getContractPhone());
		out.put("distributor_id", row.getDistributorId());
		out.put("hour", row.getHour());
		out.put("add_type", row.getAddType());
		out.put("credential", row.getCredential());
		out.put("company_name", row.getCompanyName());
		out.put("qualification_list", row.getQualificationList());
		out.put("card_id", row.getCardId());
		out.put("status", row.getStatus());
		out.put("errmsg", row.getErrmsg());
		out.put("company_id", row.getCompanyId());
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		out.put("resource_id", row.getResourceId());
		out.put("expired_at", row.getExpiredAt());

		Integer dom = row.getIsDomestic();
		out.put("is_domestic", (dom != null && dom != 0) ? dom : 1);
		out.put("country", row.getCountry());
		out.put("city", row.getCity());
		Integer direct = row.getIsDirectStore();
		out.put("is_direct_store", (direct != null && direct != 0) ? direct : 1);
		Boolean open = row.getIsOpen();
		out.put("is_open", Boolean.TRUE.equals(open) ? open : true);

		out.put("qqmapimg", buildQqMapImgUrl(row.getLat(), row.getLng()));

		long nowSec = Instant.now().getEpochSecond();
		Long resId = row.getResourceId();
		Long exp = row.getExpiredAt();
		if (resId != null && resId > 0L && exp != null && exp > nowSec) {
			Resources res = resourcesMapper.selectById(resId);
			if (res == null) {
				throw new ResourceException("resource_id=" + resId + "的资源包不存在！");
			}
			out.put("resource_name", res.getResourceName());
		} else {
			out.put("resource_id", null);
		}
		return out;
	}

	private String buildQqMapImgUrl(String latStr, String lngStr) {
		String lat = latStr != null && StringUtils.hasText(latStr) ? latStr.trim() : "";
		String lng = lngStr != null && StringUtils.hasText(lngStr) ? lngStr.trim() : "";
		String latlng = lat + "," + lng;
		String key = qqmapKey != null ? qqmapKey : "";
		return "http://apis.map.qq.com/ws/staticmap/v2/?"
				+ "key="
				+ key
				+ "&size=500x249"
				+ "&zoom=16"
				+ "&center="
				+ latlng
				+ "&markers=color:blue|label:A|"
				+ latlng;
	}
}
