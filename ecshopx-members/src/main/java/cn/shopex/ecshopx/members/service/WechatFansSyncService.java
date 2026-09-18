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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.common.dispatch.SyncWechatFansDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserGetService;
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserGetService.UserGetPage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WechatFansSyncService {

	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final StringRedisTemplate redis;
	private final OfficialAccountUserGetService officialAccountUserGetService;
	private final SyncWechatFansDispatchPublisher syncWechatFansDispatchPublisher;

	public WechatFansSyncService(
			@Qualifier("membersStringRedisTemplate") StringRedisTemplate redis,
			OfficialAccountUserGetService officialAccountUserGetService,
			SyncWechatFansDispatchPublisher syncWechatFansDispatchPublisher) {
		this.redis = redis;
		this.officialAccountUserGetService = officialAccountUserGetService;
		this.syncWechatFansDispatchPublisher = syncWechatFansDispatchPublisher;
	}

	public void syncWechatFans(String authorizerAppidOrNull, long companyId, String companyIdRawForSha1) {
		long nowSec = Instant.now().getEpochSecond();
		ZoneId zone = ZoneId.systemDefault();
		String ymd = YMD.format(LocalDate.now(zone));
		String key = "syncUsersCount" + ymd + ":" + sha1HexUtf8(companyIdRawForSha1);

		Long size = redis.opsForList().size(key);
		if (size != null && size >= 3) {
			throw new ResourceException("一天只能同步三次粉丝");
		}

		String last = redis.opsForList().index(key, -1);
		if (last != null && !last.isBlank()) {
			long lastSec = Long.parseLong(last.trim());
			if (nowSec - 600 < lastSec) {
				throw new ResourceException("十分钟内只能同步一次");
			}
		}

		String appid = authorizerAppidOrNull == null ? "" : authorizerAppidOrNull.trim();
		if (appid.isEmpty()) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定");
		}

		redis.opsForList().rightPush(key, String.valueOf(nowSec));
		long expireAt =
				LocalDate.now(zone).atTime(23, 59, 59).atZone(zone).toEpochSecond();
		redis.expireAt(key, java.time.Instant.ofEpochSecond(expireAt));

		UserGetPage firstPage = officialAccountUserGetService.listOpenIdsForSync(appid, null);
		long total = firstPage.total();
		int pages = (int) Math.ceil(total / 10000.0);

		String nextOpenId = null;
		for (int pageNo = 1; pageNo <= pages; pageNo++) {
			UserGetPage page = officialAccountUserGetService.listOpenIdsForSync(appid, nextOpenId);
			List<String> openids = page.openids();
			if (openids != null && !openids.isEmpty()) {
				syncWechatFansDispatchPublisher.publish(buildSyncWechatFansPayload(companyId, appid, page));
			}
			nextOpenId = page.nextOpenid();
		}
	}

	private static Map<String, Object> buildSyncWechatFansPayload(
			long companyId, String authorizerAppid, UserGetPage page) {
		Map<String, Object> m = new HashMap<>();
		m.put("company_id", companyId);
		m.put("authorizer_appid", authorizerAppid);
		m.put("count", page.count());
		m.put("open_ids", page.openids());
		return m;
	}

	private static String sha1HexUtf8(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
