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

package cn.shopex.ecshopx.kaquan.service.discount.dto;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * H5 卡券列表：请求参数与 H5 租户 / 可选登录态。
 */
public class WxappGetCardListParams {

	private static final String MSG_UNAUTHORIZED = "无权访问该API,非法访问！";

	private final int pageNo;
	private final int pageSize;
	private final String cardType;
	private final String endDate;
	private final String distributorId;
	private final Long itemId;
	private final String cardIdCsv;
	private final String workUserId;
	private final long companyId;
	private final Long userId;
	private final Integer gradeId;

	public WxappGetCardListParams(
			int pageNo,
			int pageSize,
			String cardType,
			String endDate,
			String distributorId,
			Long itemId,
			String cardIdCsv,
			String workUserId,
			long companyId,
			Long userId,
			Integer gradeId) {
		this.pageNo = pageNo;
		this.pageSize = pageSize;
		this.cardType = cardType;
		this.endDate = endDate;
		this.distributorId = distributorId;
		this.itemId = itemId;
		this.cardIdCsv = cardIdCsv;
		this.workUserId = workUserId;
		this.companyId = companyId;
		this.userId = userId;
		this.gradeId = gradeId;
	}

	public static WxappGetCardListParams fromRequest(
			HttpServletRequest request,
			int pageNo,
			int pageSize,
			String cardType,
			String endDate,
			String distributorId,
			Long itemId,
			String cardIdCsv,
			String workUserid,
			String userWorkId) {
		long companyId = parsePositiveCompanyIdFromRequest(request);
		String workUser = StringUtils.hasText(workUserid) ? workUserid.trim() : null;
		if (workUser == null && StringUtils.hasText(userWorkId)) {
			workUser = userWorkId.trim();
		}
		Long userId = null;
		Integer gradeId = null;
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (rawClaims instanceof Map<?, ?> cm) {
			userId = parseOptionalUserId(cm.get("user_id"));
			gradeId = parseOptionalInteger(cm.get("grade_id"));
		}
		return new WxappGetCardListParams(
				pageNo,
				pageSize,
				cardType,
				endDate,
				distributorId,
				itemId,
				cardIdCsv,
				workUser,
				companyId,
				userId,
				gradeId);
	}

	private static long parsePositiveCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException(MSG_UNAUTHORIZED);
			}
		} else {
			throw new UnauthorizedException(MSG_UNAUTHORIZED);
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException(MSG_UNAUTHORIZED);
		}
		return companyId;
	}

	private static Long parseOptionalUserId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer parseOptionalInteger(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public int getPageNo() {
		return pageNo;
	}

	public int getPageSize() {
		return pageSize;
	}

	public String getCardType() {
		return cardType;
	}

	public String getEndDate() {
		return endDate;
	}

	public String getDistributorId() {
		return distributorId;
	}

	public Long getItemId() {
		return itemId;
	}

	public String getCardIdCsv() {
		return cardIdCsv;
	}

	public String getWorkUserId() {
		return workUserId;
	}

	public long getCompanyId() {
		return companyId;
	}

	public Long getUserId() {
		return userId;
	}

	public Integer getGradeId() {
		return gradeId;
	}
}
