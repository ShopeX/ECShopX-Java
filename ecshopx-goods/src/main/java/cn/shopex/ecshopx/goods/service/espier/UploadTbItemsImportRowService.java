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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.TbItemsUrlImportRow;
import cn.shopex.ecshopx.goods.mapper.TbItemsUrlImportRowMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UploadTbItemsImportRowService {

	private final TbItemsUrlImportRowMapper tbItemsUrlImportRowMapper;

	public UploadTbItemsImportRowService(TbItemsUrlImportRowMapper tbItemsUrlImportRowMapper) {
		this.tbItemsUrlImportRowMapper = tbItemsUrlImportRowMapper;
	}

	public void acceptRow(long companyId, long operatorId, long distributorId, long supplierId, long merchantId, Map<String, Object> row) {
		String itemUrl = trim(row.get("item_url"));
		String categoryId = trim(row.get("category_id"));
		if (!StringUtils.hasText(itemUrl)) {
			throw new BadRequestException("商品链接必填");
		}
		if (itemUrl.length() > 255) {
			throw new BadRequestException("商品链接过长");
		}
		if (!StringUtils.hasText(categoryId)) {
			throw new BadRequestException("类目ID必填");
		}
		if (categoryId.length() > 255) {
			throw new BadRequestException("类目ID过长");
		}
		String iid = extractTaobaoNumIid(itemUrl);
		if (!StringUtils.hasText(iid)) {
			throw new BadRequestException("商品链接错误");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		TbItemsUrlImportRow rec = new TbItemsUrlImportRow();
		rec.setCompanyId(companyId);
		rec.setItemUrl(itemUrl);
		rec.setTaobaoItemId(iid);
		rec.setTaobaoCategoryId(categoryId.trim());
		rec.setCreated(now);
		try {
			tbItemsUrlImportRowMapper.insert(rec);
		} catch (DataAccessException ex) {
			throw new BadRequestException("淘宝商品导入暂存失败，请确认已创建数据表 goods_tb_items_url_import");
		}
	}

	private static String extractTaobaoNumIid(String itemUrl) {
		try {
			int q = itemUrl.indexOf('?');
			if (q < 0) {
				return "";
			}
			String query = itemUrl.substring(q + 1);
			Map<String, String> params = parseQuery(query);
			String raw = params.get("id");
			if (!StringUtils.hasText(raw)) {
				return "";
			}
			return URLDecoder.decode(raw.trim(), StandardCharsets.UTF_8).trim();
		} catch (Exception e) {
			return "";
		}
	}

	private static Map<String, String> parseQuery(String query) {
		Map<String, String> m = new LinkedHashMap<>();
		for (String part : query.split("&")) {
			if (!StringUtils.hasText(part)) {
				continue;
			}
			int eq = part.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String k = part.substring(0, eq).trim();
			String v = part.substring(eq + 1).trim();
			m.put(k, v);
		}
		return m;
	}

	private static String trim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
