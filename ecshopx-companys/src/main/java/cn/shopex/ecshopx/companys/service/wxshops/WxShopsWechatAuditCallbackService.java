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

import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxShopsWechatAuditCallbackService {

	private final WxShopsMapper wxShopsMapper;

	public WxShopsWechatAuditCallbackService(WxShopsMapper wxShopsMapper) {
		this.wxShopsMapper = wxShopsMapper;
	}

	/**
	 * Applies store audit callback data keyed by {@code audit_id}. When no matching row exists,
	 * returns without updating (same as legacy behavior).
	 */
	public boolean applyWxShopsAddEvent(Map<String, Object> data) {
		return applyStoreAuditByAuditIdStatusErrmsg(data);
	}

	/**
	 * Applies modify-store-audit callback: {@code audit_id}, {@code status}, {@code errmsg} (listener maps
	 * WeChat {@code reason} to {@code errmsg}).
	 */
	public boolean applyWxShopsUpdateEvent(Map<String, Object> data) {
		return applyStoreAuditByAuditIdStatusErrmsg(data);
	}

	private boolean applyStoreAuditByAuditIdStatusErrmsg(Map<String, Object> data) {
		String auditId = stringVal(data.get("audit_id"));
		if (auditId.isEmpty()) {
			return true;
		}
		WxShops row =
				wxShopsMapper.selectOne(
						new LambdaQueryWrapper<WxShops>()
								.eq(WxShops::getAuditId, auditId)
								.last("LIMIT 1"));
		if (row == null) {
			return true;
		}
		Integer st = intStatus(data.get("status"));
		if (st != null) {
			row.setStatus(st);
		}
		row.setErrmsg(stringVal(data.get("errmsg")));
		row.setUpdated((int) Instant.now().getEpochSecond());
		wxShopsMapper.updateById(row);
		return true;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static Integer intStatus(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
