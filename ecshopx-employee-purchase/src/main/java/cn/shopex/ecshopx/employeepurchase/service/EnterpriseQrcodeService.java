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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EnterpriseQrcodeService {

	private static final String TEMPLATE_NAME = "yykweishop";
	private static final String PAGE = "pages/purchase/auth";

	private final EnterprisesMapper enterprisesMapper;
	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public EnterpriseQrcodeService(
			EnterprisesMapper enterprisesMapper,
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.enterprisesMapper = enterprisesMapper;
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public Map<String, Object> getEnterpriseQrcode(String enterpriseIdPath, Map<String, Object> operatorJwt) {
		if (!StringUtils.hasText(enterpriseIdPath)) {
			throw new BadRequestException("企业 ID 无效");
		}
		long enterpriseId;
		try {
			enterpriseId = Long.parseLong(enterpriseIdPath.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("企业 ID 无效");
		}
		if (enterpriseId <= 0) {
			throw new BadRequestException("企业 ID 无效");
		}

		long companyId = readCompanyId(operatorJwt);

		Enterprises row =
				enterprisesMapper.selectOne(
						new LambdaQueryWrapper<Enterprises>()
								.eq(Enterprises::getId, enterpriseId)
								.eq(Enterprises::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("未查询到员工企业信息");
		}

		String wxaAppId =
				weappAuthorizerAppidRepository
						.findAuthorizerAppid(companyId, TEMPLATE_NAME)
						.orElseThrow(() -> new ResourceException("没有绑定小程序"));

		String eid = Long.toString(enterpriseId);
		String cid = Long.toString(companyId);
		String authType = row.getAuthType();
		String t =
				(authType != null && !authType.isEmpty()) ? authType.substring(0, 1) : "";
		String c = Boolean.TRUE.equals(row.getIsEmployeeCheckEnabled()) ? "1" : "";
		String scene = "eid=" + eid + "&cid=" + cid + "&t=" + t + "&c=" + c;

		byte[] raw;
		try {
			raw = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxaAppId, scene, PAGE);
		} catch (BadRequestException | ResourceException | ForbiddenException e) {
			throw e;
		} catch (Exception e) {
			String msg = e.getMessage();
			throw new ResourceException(msg != null && !msg.isEmpty() ? msg : "获取小程序码失败");
		}

		String base64 = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(raw);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("base64Image", base64);
		return data;
	}

	private static long readCompanyId(Map<String, Object> operatorJwt) {
		Object co = operatorJwt.get("company_id");
		if (co == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(co);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}
		return companyId;
	}

	private static long toLong(Object co) {
		if (co instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(co.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
