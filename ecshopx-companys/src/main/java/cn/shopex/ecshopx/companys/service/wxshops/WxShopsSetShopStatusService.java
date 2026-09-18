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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WxShopsSetShopStatusService {

	private static final int WX_SHOP_ID_REQUIRED_DINGO_STATUS_CODE = 411;

	private final WxShopsMapper wxShopsMapper;
	private final WxShopsJwtShopIdWhitelist whitelist;

	public WxShopsSetShopStatusService(WxShopsMapper wxShopsMapper, WxShopsJwtShopIdWhitelist whitelist) {
		this.wxShopsMapper = wxShopsMapper;
		this.whitelist = whitelist;
	}

	@Transactional(rollbackFor = Exception.class)
	public void setShopStatus(Object wxShopIdRaw, Object statusRaw, Map<String, Object> operatorJwt) {
		List<Long> allowed = whitelist.allowedShopIds(operatorJwt.get("shop_ids"));
		if (!allowed.isEmpty()) {
			Long idForList = idForWhitelistContains(wxShopIdRaw);
			if (idForList == null || !allowed.contains(idForList)) {
				throw new ResourceException("您没有此项操作权限", 500, 400500);
			}
		}

		long wxShopId = parsePositiveWxShopIdOrThrow(wxShopIdRaw);

		Object bound = mapOpenOrCloseStatus(statusRaw);

		WxShops row = wxShopsMapper.selectById(wxShopId);
		if (row == null) {
			throw new ResourceException("wx_shop_id=" + wxShopId + "的门店不存在");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		UpdateWrapper<WxShops> uw = new UpdateWrapper<WxShops>().eq("wx_shop_id", wxShopId);
		if (bound == null || bound instanceof Boolean || bound instanceof Number) {
			uw.set("is_open", bound);
		} else if (bound instanceof CharSequence cs) {
			// Connector/J can throw MysqlDataTruncation for CAST(? AS SIGNED) with a string bound under strict SQL
			// mode; compute the same value as MySQL 8 CAST(non-null string AS SIGNED) in-process, then bind int.
			int v = mysqlCastCharSequenceToSigned(cs);
			uw.set("is_open", v);
		} else {
			uw.set("is_open", bound);
		}
		uw.set("updated", now);
		wxShopsMapper.update(null, uw);
	}

	/**
	 * Same rules as MySQL 8 {@code CAST(non-null string AS SIGNED)} for whitespace, sign, decimal prefix, empty
	 * digit run → 0, and signed 64-bit overflow saturation; return value is {@code (int)} of that {@code long}.
	 */
	private static int mysqlCastCharSequenceToSigned(CharSequence cs) {
		long lv = mysqlCastCharSequenceToSignedLong(cs);
		return (int) lv;
	}

	private static long mysqlCastCharSequenceToSignedLong(CharSequence cs) {
		if (cs == null) {
			return 0L;
		}
		final String s = cs.toString();
		final int n = s.length();
		int i = 0;
		while (i < n && isMysqlLeadingWhitespace(s.charAt(i))) {
			i++;
		}
		if (i >= n) {
			return 0L;
		}
		boolean negative = false;
		char c = s.charAt(i);
		if (c == '+' || c == '-') {
			negative = c == '-';
			i++;
		}
		if (i >= n) {
			return 0L;
		}
		int j = i;
		while (j < n) {
			char d = s.charAt(j);
			if (d < '0' || d > '9') {
				break;
			}
			j++;
		}
		if (j == i) {
			return 0L;
		}
		String digitRun = s.substring(i, j);
		BigInteger mag = new BigInteger(digitRun);
		BigInteger signed = negative ? mag.negate() : mag;
		if (signed.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
			return Long.MAX_VALUE;
		}
		if (signed.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
			return Long.MIN_VALUE;
		}
		return signed.longValue();
	}

	private static boolean isMysqlLeadingWhitespace(char c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000b';
	}

	private static Long idForWhitelistContains(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof CharSequence cs) {
			try {
				return Long.parseLong(cs.toString().trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static Object mapOpenOrCloseStatus(Object v) {
		if (Boolean.FALSE.equals(v)) {
			return Boolean.FALSE;
		}
		if (v instanceof String s && "false".equals(s)) {
			return Boolean.FALSE;
		}
		if (Boolean.TRUE.equals(v)) {
			return Boolean.TRUE;
		}
		if (v instanceof String s && "true".equals(s)) {
			return Boolean.TRUE;
		}
		return v;
	}

	private static long parsePositiveWxShopIdOrThrow(Object v) {
		if (isMissingWxShopIdLikeInput(v)) {
			throw new BadRequestException("门店必选！", WX_SHOP_ID_REQUIRED_DINGO_STATUS_CODE);
		}
		try {
			long id;
			if (v instanceof Number n) {
				id = n.longValue();
			} else {
				id = Long.parseLong(v.toString().trim());
			}
			if (id <= 0L) {
				throw new BadRequestException("门店必选！", WX_SHOP_ID_REQUIRED_DINGO_STATUS_CODE);
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("门店必选！", WX_SHOP_ID_REQUIRED_DINGO_STATUS_CODE);
		}
	}

	private static boolean isMissingWxShopIdLikeInput(Object v) {
		if (v == null) {
			return true;
		}
		if (Boolean.FALSE.equals(v)) {
			return true;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}
}
