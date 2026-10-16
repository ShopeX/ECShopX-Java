package cn.shopex.ecshopx.promotions.api.front.v1;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import org.springframework.util.StringUtils;

final class MemberSignInFrontSupport {

	private MemberSignInFrontSupport() {}

	@SuppressWarnings("unchecked")
	static Map<String, Object> readH5AuthClaims(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> map) {
			return (Map<String, Object>) map;
		}
		return Collections.emptyMap();
	}

	static long readCompanyId(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number number) {
			companyId = number.longValue();
		} else if (companyAttr instanceof String text && StringUtils.hasText(text)) {
			try {
				companyId = Long.parseLong(text.trim());
			} catch (NumberFormatException ex) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}

	static long readUserId(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaims(request);
		Object value = claims.get("user_id");
		if (value == null) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		try {
			long userId = value instanceof Number number ? number.longValue() : Long.parseLong(value.toString().trim());
			if (userId <= 0L) {
				throw new UnauthorizedException("Unable to authenticate user.");
			}
			return userId;
		} catch (NumberFormatException ex) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
	}
}
