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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WechatAuthMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatAuthQueryService {

	private static final Logger log = LoggerFactory.getLogger(WechatAuthQueryService.class);

	private final WechatAuthMapper wechatAuthMapper;
	private final ShopMenuService shopMenuService;
	private final ObjectMapper objectMapper;

	public WechatAuthQueryService(
			WechatAuthMapper wechatAuthMapper, ShopMenuService shopMenuService, ObjectMapper objectMapper) {
		this.wechatAuthMapper = wechatAuthMapper;
		this.shopMenuService = shopMenuService;
		this.objectMapper = objectMapper;
	}

	public WechatAuth requireWechatAuthWithAliasForKf(String authorizerAppid) {
		String trimmed = authorizerAppid == null ? "" : authorizerAppid.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		WechatAuth row = wechatAuthMapper.selectById(trimmed);
		if (row == null) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		return row;
	}

	public String getAuthorizerAppid(Long companyId) {
		if (companyId == null) {
			return null;
		}
		try {
			LambdaQueryWrapper<WechatAuth> w = new LambdaQueryWrapper<>();
			w.eq(WechatAuth::getCompanyId, companyId)
					.eq(WechatAuth::getBindStatus, "bind")
					.eq(WechatAuth::getServiceTypeInfo, 2);
			WechatAuth one = wechatAuthMapper.selectOne(w);
			return one != null ? one.getAuthorizerAppid() : null;
		} catch (DataAccessException e) {
			log.warn("wechat_auth 查询失败，按未绑定公众号处理: companyId={}", companyId, e);
			return null;
		}
	}

	/**
	 * 开放平台 WOA：按 Query 原始字符串绑定 {@code company_id}（与 ORM 按字符串匹配数值列行为一致）。
	 */
	public Optional<WechatAuth> findWoaWechatAuthForCompanyQuery(String companyIdRaw) {
		if (companyIdRaw == null) {
			return Optional.empty();
		}
		try {
			LambdaQueryWrapper<WechatAuth> w = new LambdaQueryWrapper<>();
			w.apply("company_id = {0}", companyIdRaw)
					.eq(WechatAuth::getBindStatus, "bind")
					.eq(WechatAuth::getServiceTypeInfo, 2);
			WechatAuth one = wechatAuthMapper.selectOne(w);
			return Optional.ofNullable(one);
		} catch (DataAccessException e) {
			log.warn("wechat_auth 查询失败，按未绑定公众号处理: companyIdRaw={}", companyIdRaw, e);
			return Optional.empty();
		}
	}

	public String getAuthorizerAppidForWoaQuery(String companyIdRaw) {
		return findWoaWechatAuthForCompanyQuery(companyIdRaw)
				.map(WechatAuth::getAuthorizerAppid)
				.orElse(null);
	}

	public Optional<WechatAuth> findBoundAuthByCompanyAndAuthorizerAppid(long companyId, String authorizerAppid) {
		if (authorizerAppid == null) {
			return Optional.empty();
		}
		String trimmed = authorizerAppid.trim();
		try {
			LambdaQueryWrapper<WechatAuth> w = new LambdaQueryWrapper<>();
			w.eq(WechatAuth::getCompanyId, companyId)
					.eq(WechatAuth::getAuthorizerAppid, trimmed)
					.eq(WechatAuth::getBindStatus, "bind")
					.isNull(WechatAuth::getDeletedAt)
					.last("LIMIT 1");
			return Optional.ofNullable(wechatAuthMapper.selectOne(w));
		} catch (DataAccessException e) {
			log.warn("wechat_auth 绑定查询失败: companyId={}, appid={}", companyId, trimmed, e);
			return Optional.empty();
		}
	}

	public boolean isAuthorizerAppidBoundToCompany(long companyId, String authorizerAppid) {
		if (!StringUtils.hasText(authorizerAppid)) {
			return false;
		}
		try {
			LambdaQueryWrapper<WechatAuth> w = new LambdaQueryWrapper<>();
			w.eq(WechatAuth::getCompanyId, companyId)
					.eq(WechatAuth::getAuthorizerAppid, authorizerAppid)
					.eq(WechatAuth::getBindStatus, "bind")
					.isNull(WechatAuth::getDeletedAt);
			return wechatAuthMapper.selectCount(w) > 0;
		} catch (DataAccessException e) {
			log.warn("wechat_auth 绑定校验查询失败: companyId={}, appid={}", companyId, authorizerAppid, e);
			return false;
		}
	}

	public Optional<String> findPrincipalNameByCompanyAndAppid(long companyId, String authorizerAppid) {
		if (!StringUtils.hasText(authorizerAppid)) {
			return Optional.empty();
		}
		String appid = authorizerAppid.trim();
		try {
			LambdaQueryWrapper<WechatAuth> w = new LambdaQueryWrapper<>();
			w.eq(WechatAuth::getCompanyId, companyId)
					.eq(WechatAuth::getAuthorizerAppid, appid)
					.eq(WechatAuth::getBindStatus, "bind")
					.isNull(WechatAuth::getDeletedAt)
					.last("LIMIT 1");
			WechatAuth one = wechatAuthMapper.selectOne(w);
			if (one == null || !StringUtils.hasText(one.getPrincipalName())) {
				return Optional.empty();
			}
			String name = one.getPrincipalName().trim();
			return StringUtils.hasText(name) ? Optional.of(name) : Optional.empty();
		} catch (DataAccessException e) {
			log.warn("wechat_auth 查询主体失败: companyId={}, appid={}", companyId, appid, e);
			return Optional.empty();
		}
	}

	public String findBoundMiniProgramAuthorizerAppid(long companyId) {
		try {
			LambdaQueryWrapper<WechatAuth> w = new LambdaQueryWrapper<>();
			w.eq(WechatAuth::getCompanyId, companyId)
					.eq(WechatAuth::getBindStatus, "bind")
					.eq(WechatAuth::getServiceTypeInfo, 3)
					.isNull(WechatAuth::getDeletedAt)
					.last("LIMIT 1");
			WechatAuth one = wechatAuthMapper.selectOne(w);
			return one != null ? one.getAuthorizerAppid() : null;
		} catch (DataAccessException e) {
			log.warn("wechat_auth 小程序授权查询失败: companyId={}", companyId, e);
			return null;
		}
	}

	public List<WechatAuth> listBoundMiniProgramsForCompany(long companyId) {
		try {
			LambdaQueryWrapper<WechatAuth> w = new LambdaQueryWrapper<>();
			w.eq(WechatAuth::getCompanyId, companyId)
					.eq(WechatAuth::getBindStatus, "bind")
					.eq(WechatAuth::getServiceTypeInfo, 3)
					.isNull(WechatAuth::getDeletedAt);
			List<WechatAuth> list = wechatAuthMapper.selectList(w);
			return list != null ? list : Collections.emptyList();
		} catch (DataAccessException e) {
			log.warn("wechat_auth 小程序授权列表查询失败: companyId={}", companyId, e);
			return Collections.emptyList();
		}
	}

	public boolean isMiniProgramWxaBoundToCompany(long companyId, String wxaAppId) {
		if (!StringUtils.hasText(wxaAppId)) {
			return false;
		}
		try {
			LambdaQueryWrapper<WechatAuth> w = new LambdaQueryWrapper<>();
			w.eq(WechatAuth::getCompanyId, companyId)
					.eq(WechatAuth::getAuthorizerAppid, wxaAppId.trim())
					.eq(WechatAuth::getBindStatus, "bind")
					.isNull(WechatAuth::getDeletedAt);
			return wechatAuthMapper.selectCount(w) > 0;
		} catch (DataAccessException e) {
			log.warn("wechat_auth 小程序绑定校验查询失败: companyId={}, wxaAppId={}", companyId, wxaAppId, e);
			return false;
		}
	}

	/**
	 * 按 JWT 中的 authorizer_appid 查询授权基础信息，并合并公司菜单类型文案。
	 */
	public Map<String, Object> getAuthorizerInfo(long companyId, String authorizerAppidFromJwt) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		String appid = authorizerAppidFromJwt == null ? null : authorizerAppidFromJwt.trim();
		WechatAuth row = null;
		if (appid != null && !appid.isEmpty()) {
			try {
				LambdaQueryWrapper<WechatAuth> w = new LambdaQueryWrapper<>();
				w.eq(WechatAuth::getAuthorizerAppid, appid)
						.isNull(WechatAuth::getDeletedAt)
						.last("LIMIT 1");
				row = wechatAuthMapper.selectOne(w);
			} catch (DataAccessException e) {
				log.warn("wechat_auth getAuthorizerInfo 查询失败: appid={}", appid, e);
			}
		}
		if (row != null) {
			result.put("authorizer_appid", row.getAuthorizerAppid());
			result.put("authorizer_appsecret", row.getAuthorizerAppsecret());
			result.put("nick_name", row.getNickName());
			result.put("head_img", row.getHeadImg());
			result.put("service_type_info", row.getServiceTypeInfo());
			result.put("verify_type_info", row.getVerifyTypeInfo());
			result.put("user_name", row.getUserName());
			result.put("signature", row.getSignature());
			result.put("principal_name", row.getPrincipalName());
			result.put("alias", row.getAlias());
			result.put("qrcode_url", row.getQrcodeUrl());
			result.put("func_info", row.getFuncInfo());
			result.put("is_direct", row.getIsDirect());
			result.put("business_info", readJsonColumnOrNull(row.getBusinessInfo()));
			result.put("miniprograminfo", readJsonColumnOrNull(row.getMiniprograminfo()));
		}
		Map<String, Object> menuTypeMap = shopMenuService.getMenuTypeByCompanyId(companyId);
		Object rawStr = menuTypeMap == null ? null : menuTypeMap.get("menu_type_str");
		String menuTypeStr =
				(rawStr != null && StringUtils.hasText(String.valueOf(rawStr).trim()))
						? String.valueOf(rawStr).trim()
						: shopMenuService.resolveProductModelKeyForCompany(companyId);
		result.put("menu_type", menuTypeStr);
		return result;
	}

	/**
	 * 将库中 JSON 文本列解析为 Jackson 原生结构；非法或空白则 {@code null}。
	 */
	public Object jsonColumnToJavaObject(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return null;
		}
		String trimmed = raw.trim();
		try {
			return objectMapper.readValue(trimmed, Object.class);
		} catch (Exception e) {
			return null;
		}
	}

	private Object readJsonColumnOrNull(String raw) {
		return jsonColumnToJavaObject(raw);
	}
}
