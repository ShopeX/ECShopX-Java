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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.service.domain.CompanyDomainLookupProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOrderSourceFromResolveService {

	private final CompanyDomainLookupProperties domainLookupProperties;

	private final CompanysMapper companysMapper;

	public WxappOrderSourceFromResolveService(
			CompanyDomainLookupProperties domainLookupProperties, CompanysMapper companysMapper) {
		this.domainLookupProperties = domainLookupProperties;
		this.companysMapper = companysMapper;
	}

	public String resolve(long companyId, HttpServletRequest request, Map<String, Object> authLikeParams) {
		if (authLikeParams != null) {
			if (StringUtils.hasText(stringVal(authLikeParams.get("wxapp_appid")))
					|| StringUtils.hasText(stringVal(authLikeParams.get("wxa_appid")))) {
				return "wxapp";
			}
			if (StringUtils.hasText(stringVal(authLikeParams.get("alipay_appid")))) {
				return "aliapp";
			}
		}
		String hostRaw = request.getHeader("Origin");
		if (!StringUtils.hasText(hostRaw)) {
			return "unknow";
		}
		String host = hostRaw.replace("http://", "").replace("https://", "");
		String pcSuffix = Pattern.quote(domainLookupProperties.getPcDomainSuffix());
		String h5Suffix = Pattern.quote(domainLookupProperties.getH5DomainSuffix());
		if (Pattern.compile("^s(\\d+)" + pcSuffix + "$").matcher(host).matches()) {
			return "pc";
		}
		if (Pattern.compile("^m(\\d+)" + h5Suffix + "$").matcher(host).matches()) {
			return "h5";
		}
		long pcHits =
				companysMapper.selectCount(new LambdaQueryWrapper<Companys>()
						.eq(Companys::getCompanyId, companyId)
						.eq(Companys::getPcDomain, host));
		if (pcHits > 0) {
			return "pc";
		}
		long h5Hits =
				companysMapper.selectCount(new LambdaQueryWrapper<Companys>()
						.eq(Companys::getCompanyId, companyId)
						.eq(Companys::getH5Domain, host));
		if (h5Hits > 0) {
			return "h5";
		}
		return "unknow";
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
