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

package cn.shopex.ecshopx.thirdparty.service.saascert;

import cn.shopex.ecshopx.thirdparty.domain.ShopBind;
import cn.shopex.ecshopx.thirdparty.mapper.ShopBindMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * ERP 绑定节点读取（对齐 PHP {@code CertService::getErpBindNode}）。
 *
 * <p>主路径读 Redis {@code SaasErpBind:sha1(companyId)}；若缺失则从 {@code
 * thirdparty_shop_bind} 回填（修复历史「非 ecos.ome 只写库」与 Redis 丢失后重复绑定短路）。
 */
@Service
public class SaasCertErpBindReadService {

	private static final Logger log = LoggerFactory.getLogger(SaasCertErpBindReadService.class);

	private static final int BIND = 1;

	private final StringRedisTemplate companysRedisTemplate;
	private final ShopBindMapper shopBindMapper;

	public SaasCertErpBindReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ShopBindMapper shopBindMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.shopBindMapper = shopBindMapper;
	}

	public boolean isErpBound(long companyId) {
		return resolveErpBindNodeId(companyId).isPresent();
	}

	/**
	 * 解析 ERP 绑定 node_id：先 Redis，缺失时用 DB 活跃绑定回填 Redis 后再返回。
	 */
	public Optional<String> resolveErpBindNodeId(long companyId) {
		if (companyId <= 0L) {
			return Optional.empty();
		}
		String key = erpBindRedisKey(companyId);
		String fromRedis = companysRedisTemplate.opsForValue().get(key);
		if (StringUtils.hasText(fromRedis)) {
			return Optional.of(fromRedis.trim());
		}
		ShopBind row = findLatestActiveBind(companyId);
		if (row == null || !StringUtils.hasText(row.getNodeId())) {
			return Optional.empty();
		}
		String nodeId = row.getNodeId().trim();
		companysRedisTemplate.opsForValue().set(key, nodeId);
		log.info(
				"saascert backfill SaasErpBind companyId={} nodeId={} nodeType={} from thirdparty_shop_bind",
				companyId,
				nodeId,
				row.getNodeType());
		return Optional.of(nodeId);
	}

	private ShopBind findLatestActiveBind(long companyId) {
		LambdaQueryWrapper<ShopBind> q = new LambdaQueryWrapper<>();
		q.eq(ShopBind::getCompanyId, companyId)
				.eq(ShopBind::getStatus, BIND)
				.isNotNull(ShopBind::getNodeId)
				.ne(ShopBind::getNodeId, "")
				.orderByDesc(ShopBind::getUpdated)
				.orderByDesc(ShopBind::getId)
				.last("LIMIT 1");
		return shopBindMapper.selectOne(q);
	}

	public static String erpBindRedisKey(long companyId) {
		return "SaasErpBind:" + sha1Hex(String.valueOf(companyId));
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
