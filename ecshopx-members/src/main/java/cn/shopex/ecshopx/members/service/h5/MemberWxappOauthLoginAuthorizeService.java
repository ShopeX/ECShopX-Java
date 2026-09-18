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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.port.WxappOauthLoginAuthorizePort;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.members.service.h5.dto.H5LoginAttemptResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberWxappOauthLoginAuthorizeService implements WxappOauthLoginAuthorizePort {

	private static final int STATUS_WXCODE_WRIT = 0;
	private static final int STATUS_WXCODE_SWEEP = 1;
	private static final int STATUS_WXCODE_SUCCESS = 2;
	private static final int STATUS_WXCODE_ERROR = 3;
	private static final int STATUS_WXCODE_EXPIRED = 4;

	private final StringRedisTemplate membersRedis;
	private final ObjectMapper objectMapper;
	private final H5LoginOrchestrator h5LoginOrchestrator;
	private final WechatUsersMapper wechatUsersMapper;
	private final MembersAssociationsMapper membersAssociationsMapper;

	public MemberWxappOauthLoginAuthorizeService(
			@Qualifier("membersStringRedisTemplate") StringRedisTemplate membersRedis,
			ObjectMapper objectMapper,
			H5LoginOrchestrator h5LoginOrchestrator,
			WechatUsersMapper wechatUsersMapper,
			MembersAssociationsMapper membersAssociationsMapper) {
		this.membersRedis = membersRedis;
		this.objectMapper = objectMapper;
		this.h5LoginOrchestrator = h5LoginOrchestrator;
		this.wechatUsersMapper = wechatUsersMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
	}

	@Override
	public void accessTokenAuthorize(Map<String, Object> input) {
		String token = stringVal(input.get("token"));
		String appid = stringVal(input.get("appid"));
		String openId = stringVal(input.get("open_id"));
		if (isTruthyOauthStatus(input.get("status"))) {
			accessTokenSuccess(token, openId, appid);
		} else {
			accessTokenError(token);
		}
	}

	@Override
	public String findUnionidByWxappOpenId(String authorizerAppid, String openId) {
		WechatUsers wu = wechatUsersMapper.selectOne(new LambdaQueryWrapper<WechatUsers>()
				.eq(WechatUsers::getAuthorizerAppid, authorizerAppid)
				.eq(WechatUsers::getOpenId, openId)
				.last("LIMIT 1"));
		if (wu == null || !StringUtils.hasText(wu.getUnionid())) {
			return null;
		}
		return wu.getUnionid().trim();
	}

	@Override
	public boolean accessTokenSweep(String unionId, String token) {
		if (!StringUtils.hasText(unionId)) {
			return false;
		}
		MembersAssociations assoc = membersAssociationsMapper.selectOne(new LambdaQueryWrapper<MembersAssociations>()
				.eq(MembersAssociations::getUnionid, unionId)
				.last("LIMIT 1"));
		if (assoc == null) {
			return false;
		}
		try {
			String redisKey = "member:oauth:login:" + token;
			String raw = membersRedis.opsForValue().get(redisKey);
			if (!StringUtils.hasText(raw)) {
				throw new ResourceException("授权失败");
			}
			Map<String, Object> info;
			try {
				info = objectMapper.readValue(raw, new TypeReference<>() {});
			} catch (Exception e) {
				throw new ResourceException("授权失败");
			}
			Object expObj = info.get("exp");
			if (expObj != null) {
				long expSec = toLongEpochSecond(expObj);
				if (Instant.now().getEpochSecond() > expSec) {
					throw new ResourceException("授权码已过期");
				}
			}
			Object statusObj = info.get("status");
			if (statusObj != null && toInt(statusObj) != STATUS_WXCODE_WRIT) {
				throw new ResourceException("授权码已被使用");
			}
			info.put("status", STATUS_WXCODE_SWEEP);
			info.put("union_id", unionId);
			persistOauthLoginJsonPreservingTtl(redisKey, info);
			return true;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() == null ? "授权失败" : e.getMessage());
		}
	}

	@Override
	public Map<String, Object> validOauthLogin(String token) {
		String suffix = (token == null) ? "" : token;
		String redisKey = "member:oauth:login:" + suffix;
		String raw = membersRedis.opsForValue().get(redisKey);
		if (!StringUtils.hasText(raw)) {
			return oauthLoginNothingResponse();
		}
		Map<String, Object> info;
		try {
			info = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return oauthLoginNothingResponse();
		}
		if (info == null) {
			return oauthLoginNothingResponse();
		}
		if (!info.containsKey("status")
				|| info.get("status") == null
				|| !info.containsKey("exp")
				|| info.get("exp") == null) {
			return oauthLoginNothingResponse();
		}
		if (!isParsableEpochSecondValue(info.get("exp"))) {
			return oauthLoginNothingResponse();
		}
		int st = toInt(info.get("status"));
		long now = Instant.now().getEpochSecond();
		long expSec = toLongEpochSecond(info.get("exp"));
		if (now > expSec && st == STATUS_WXCODE_WRIT) {
			LinkedHashMap<String, Object> expired = new LinkedHashMap<>();
			expired.put("status", Integer.valueOf(STATUS_WXCODE_EXPIRED));
			expired.put("msg", "验证过期");
			return expired;
		}
		switch (st) {
			case STATUS_WXCODE_WRIT: {
				LinkedHashMap<String, Object> m = new LinkedHashMap<>();
				m.put("status", Integer.valueOf(0));
				m.put("msg", "扫码登录失败，请刷新重试或选择其他登录方式");
				return m;
			}
			case STATUS_WXCODE_SWEEP: {
				LinkedHashMap<String, Object> m = new LinkedHashMap<>();
				m.put("status", Integer.valueOf(1));
				m.put("msg", "扫描成功");
				return m;
			}
			case STATUS_WXCODE_SUCCESS: {
				LinkedHashMap<String, Object> m = new LinkedHashMap<>();
				m.put("status", Integer.valueOf(2));
				m.put("msg", "登录成功");
				Object rawTok = info.get("token");
				if (rawTok == null) {
					m.put("token", null);
				} else if (rawTok instanceof String s) {
					m.put("token", s);
				} else {
					m.put("token", String.valueOf(rawTok));
				}
				return m;
			}
			case STATUS_WXCODE_ERROR: {
				LinkedHashMap<String, Object> m = new LinkedHashMap<>();
				m.put("status", Integer.valueOf(3));
				m.put("msg", "未授权登录");
				return m;
			}
			default:
				return oauthLoginNothingResponse();
		}
	}

	private void accessTokenSuccess(String token, String openId, String appid) {
		try {
			String redisKey = "member:oauth:login:" + token;
			String raw = membersRedis.opsForValue().get(redisKey);
			if (!StringUtils.hasText(raw)) {
				throw new ResourceException("授权失败");
			}
			Map<String, Object> info = objectMapper.readValue(raw, new TypeReference<>() {});
			int st = toInt(info.get("status"));
			if (st != STATUS_WXCODE_SWEEP) {
				throw new ResourceException("授权码已被使用");
			}
			info.put("status", STATUS_WXCODE_SUCCESS);
			Map<String, Object> credentials = new LinkedHashMap<>();
			credentials.put("auth_type", "oauth");
			credentials.put("token", token);
			credentials.put("openid", openId);
			credentials.put("appid", appid);
			H5LoginAttemptResult r = h5LoginOrchestrator.attempt(credentials);
			if (!r.success() || !StringUtils.hasText(r.jwtToken())) {
				throw new ResourceException("授权失败");
			}
			info.put("token", r.jwtToken());
			persistOauthLoginJsonPreservingTtl(redisKey, info);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() == null ? "授权失败" : e.getMessage());
		}
	}

	private void accessTokenError(String token) {
		String redisKey = "member:oauth:login:" + token;
		String raw = membersRedis.opsForValue().get(redisKey);
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("授权失败");
		}
		try {
			Map<String, Object> info = objectMapper.readValue(raw, new TypeReference<>() {});
			int st = toInt(info.get("status"));
			if (st != STATUS_WXCODE_SWEEP) {
				throw new ResourceException("授权失败");
			}
			info.put("status", STATUS_WXCODE_ERROR);
			persistOauthLoginJsonPreservingTtl(redisKey, info);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() == null ? "授权失败" : e.getMessage());
		}
	}

	/**
	 * 更新 Redis 中的 JSON 时保留该 key 的剩余 TTL，避免覆盖写入时丢失过期时间而导致 key 长期有效。
	 */
	private void persistOauthLoginJsonPreservingTtl(String redisKey, Map<String, Object> info)
			throws JsonProcessingException {
		String json = objectMapper.writeValueAsString(info);
		Long ttl = membersRedis.getExpire(redisKey, TimeUnit.SECONDS);
		if (ttl != null && ttl > 0) {
			membersRedis.opsForValue().set(redisKey, json, Duration.ofSeconds(ttl));
		} else {
			membersRedis.opsForValue().set(redisKey, json);
		}
	}

	private static Map<String, Object> oauthLoginNothingResponse() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("status", Integer.valueOf(5));
		m.put("msg", "二维码信息出错");
		return m;
	}

	/**
	 * 与 {@link #toLongEpochSecond(Object)} 一致：{@link Number} 或可被 {@link Long#parseLong(String)} 解析的字符串视为合法过期时间戳。
	 */
	private static boolean isParsableEpochSecondValue(Object expObj) {
		if (expObj instanceof Number) {
			return true;
		}
		try {
			Long.parseLong(String.valueOf(expObj).trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int toInt(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long toLongEpochSecond(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/**
	 * 判定请求体中 {@code status} 是否视为成功：缺省或 null 按 0 处理；false、0、0.0、空串、"0" 为失败；其余（含非零数值与
	 * "1"、"2" 等字符串）为成功。
	 */
	private static boolean isTruthyOauthStatus(Object rawStatus) {
		Object v = rawStatus != null ? rawStatus : 0;
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return false;
		}
		return true;
	}
}
