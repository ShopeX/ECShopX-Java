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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.domain.CommunityActivity;
import cn.shopex.ecshopx.community.domain.CommunityChief;
import cn.shopex.ecshopx.community.domain.CommunityChiefDistributor;
import cn.shopex.ecshopx.community.domain.dto.CommunityChiefListRowDto;
import cn.shopex.ecshopx.community.mapper.CommunityActivityMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefDistributorMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefMapper;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.ShopRelMember;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.ShopRelMemberMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CommunityChiefService {

	private static final Logger log = LoggerFactory.getLogger(CommunityChiefService.class);

	private final CommunityChiefMapper communityChiefMapper;
	private final CommunityChiefDistributorMapper communityChiefDistributorMapper;
	private final ShopRelMemberMapper shopRelMemberMapper;
	private final MemberAccountService memberAccountService;
	private final MembersMapper membersMapper;
	private final CommunityChiefDistributorListQueryService communityChiefDistributorListQueryService;
	private final ObjectMapper objectMapper;
	private final CommunityActivityMapper communityActivityMapper;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	public CommunityChiefService(
			CommunityChiefMapper communityChiefMapper,
			CommunityChiefDistributorMapper communityChiefDistributorMapper,
			ShopRelMemberMapper shopRelMemberMapper,
			MemberAccountService memberAccountService,
			MembersMapper membersMapper,
			CommunityChiefDistributorListQueryService communityChiefDistributorListQueryService,
			ObjectMapper objectMapper,
			CommunityActivityMapper communityActivityMapper,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService) {
		this.communityChiefMapper = communityChiefMapper;
		this.communityChiefDistributorMapper = communityChiefDistributorMapper;
		this.shopRelMemberMapper = shopRelMemberMapper;
		this.memberAccountService = memberAccountService;
		this.membersMapper = membersMapper;
		this.communityChiefDistributorListQueryService = communityChiefDistributorListQueryService;
		this.objectMapper = objectMapper;
		this.communityActivityMapper = communityActivityMapper;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
	}

	/**
	 * H5 团长接口：从 JWT 解析团长 id。优先使用 {@code chief_id}；否则用 {@code user_id} 或 {@code sub} 作为会员 id，
	 * 在指定公司下查询 {@code community_chief}。
	 */
	public long countByCompanyAndUserId(long companyId, long userId) {
		if (companyId <= 0L || userId <= 0L) {
			return 0L;
		}
		return communityChiefMapper.selectCount(
				new LambdaQueryWrapper<CommunityChief>()
						.eq(CommunityChief::getCompanyId, companyId)
						.eq(CommunityChief::getUserId, userId));
	}

	/**
	 * H5 JWT：优先使用 {@code mobile} claim；缺失则从 {@code members} 按公司 + 会员 id 读取手机号。
	 *
	 * @throws BadRequestException 无法得到非空手机号时
	 */
	public String resolveAuthMobile(long companyId, Map<String, Object> claims) {
		if (claims == null || claims.isEmpty()) {
			throw new BadRequestException("手机号不能为空");
		}
		Object mobRaw = claims.get("mobile");
		String mobile = mobRaw == null ? "" : String.valueOf(mobRaw).trim();
		if (StringUtils.hasText(mobile)) {
			return mobile;
		}
		long uid = parseUserIdLoose(claims.get("user_id"));
		if (uid <= 0L) {
			uid = parseUserIdLoose(claims.get("sub"));
		}
		if (uid <= 0L || companyId <= 0L) {
			throw new BadRequestException("手机号不能为空");
		}
		Members row =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, uid)
								.last("LIMIT 1"));
		if (row != null && StringUtils.hasText(row.getMobile())) {
			return row.getMobile().trim();
		}
		throw new BadRequestException("手机号不能为空");
	}

	public long resolveChiefIdForH5(long companyId, Map<String, Object> claims) {
		return resolveChiefIdForH5Internal(companyId, claims, false);
	}

	/**
	 * 与 {@link #resolveChiefIdForH5(long, Map)} 相同的团长解析（JWT {@code chief_id} 优先，否则按会员 id 回表），
	 * 但拒绝访问时抛 {@link ResourceException}（422），供需与 Dingo 资源错误体一致的业务接口使用。
	 */
	public long resolveChiefIdForCashWithdrawalApply(long companyId, Map<String, Object> claims) {
		return resolveChiefIdForH5Internal(companyId, claims, true);
	}

	private long resolveChiefIdForH5Internal(long companyId, Map<String, Object> claims, boolean resourceOnDeny) {
		if (claims == null || claims.isEmpty()) {
			throwChiefAccessDenied(resourceOnDeny);
		}
		long chiefFromJwt = parseUserIdLoose(claims.get("chief_id"));
		if (chiefFromJwt > 0L) {
			return chiefFromJwt;
		}
		long memberUserId = parseUserIdLoose(claims.get("user_id"));
		if (memberUserId <= 0L) {
			memberUserId = parseUserIdLoose(claims.get("sub"));
		}
		if (memberUserId <= 0L) {
			throwChiefAccessDenied(resourceOnDeny);
		}
		CommunityChief row =
				communityChiefMapper.selectOne(
						new LambdaQueryWrapper<CommunityChief>()
								.eq(CommunityChief::getCompanyId, companyId)
								.eq(CommunityChief::getUserId, memberUserId)
								.last("LIMIT 1"));
		if (row == null || row.getChiefId() == null || row.getChiefId() <= 0L) {
			throwChiefAccessDenied(resourceOnDeny);
		}
		return row.getChiefId();
	}

	private static void throwChiefAccessDenied(boolean resourceOnDeny) {
		if (resourceOnDeny) {
			throw new ResourceException("只有团长才能操作");
		}
		throw new ForbiddenException("只有团长才能操作");
	}

	/**
	 * H5 部分接口：仅从 JWT {@code chief_id} 解析团长主键，不回表 {@code community_chief}。
	 * 与 {@link #resolveChiefIdForH5(long, Map)} 并列；后者在 claim 缺失时会用会员 id 补全。
	 */
	public long requireChiefIdFromJwtOnly(Map<String, Object> claims) {
		if (claims == null || claims.isEmpty()) {
			throw new ForbiddenException("只有团长才能操作");
		}
		long id = parseUserIdLoose(claims.get("chief_id"));
		if (id <= 0L) {
			throw new ForbiddenException("只有团长才能操作");
		}
		return id;
	}

	/**
	 * H5 团长提现账户：按团长主键读取 {@code community_chief}，校验公司一致后返回支付宝/银行卡展示字段。
	 */
	public Map<String, Object> getCashWithdrawalAccountPayloadForH5(long companyId, long chiefId) {
		if (chiefId <= 0L) {
			throw new ResourceException("团长信息获取失败");
		}
		CommunityChief row = communityChiefMapper.selectById(chiefId);
		if (row == null) {
			throw new ResourceException("团长信息获取失败");
		}
		if (row.getCompanyId() == null || !row.getCompanyId().equals(companyId)) {
			throw new ResourceException("团长信息获取失败");
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("alipay_name", row.getAlipayName());
		data.put("alipay_account", row.getAlipayAccount());
		data.put("bank_name", row.getBankName());
		data.put("bankcard_no", row.getBankcardNo());
		return data;
	}

	/**
	 * H5 更新提现账户：仅按 {@code chief_id} 条件更新 {@code community_chief} 指定字段；{@code fieldUpdates} 为空时不访问数据库。
	 * 键仅限 {@code alipay_name}、{@code alipay_account}、{@code bank_name}、{@code bankcard_no}（snake_case），其余键忽略。
	 */
	public void updateCashWithdrawalAccountFieldsForH5(long chiefId, Map<String, String> fieldUpdates) {
		if (fieldUpdates == null || fieldUpdates.isEmpty()) {
			return;
		}
		int nowInt = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<CommunityChief> uw = new LambdaUpdateWrapper<>();
		uw.eq(CommunityChief::getChiefId, chiefId);
		boolean anyKnownField = false;
		for (Map.Entry<String, String> e : fieldUpdates.entrySet()) {
			String key = e.getKey();
			String value = e.getValue() == null ? "" : e.getValue();
			switch (key) {
				case "alipay_name" -> {
					uw.set(CommunityChief::getAlipayName, value);
					anyKnownField = true;
				}
				case "alipay_account" -> {
					uw.set(CommunityChief::getAlipayAccount, value);
					anyKnownField = true;
				}
				case "bank_name" -> {
					uw.set(CommunityChief::getBankName, value);
					anyKnownField = true;
				}
				case "bankcard_no" -> {
					uw.set(CommunityChief::getBankcardNo, value);
					anyKnownField = true;
				}
				default -> {
				}
			}
		}
		if (!anyKnownField) {
			return;
		}
		uw.set(CommunityChief::getUpdatedAt, nowInt);
		communityChiefMapper.update(null, uw);
	}

	/**
	 * 管理端团长分页列表：联表查询团长及其店铺分销关联与申请相关字段，在指定公司下按姓名、手机号等条件筛选。
	 *
	 * @param distributorIdFilter {@code null} 表示不限制 {@code d.distributor_id}；非 null 时为等值条件（含 0）
	 */
	public Map<String, Object> listChiefsByDistributorJoin(
			Long distributorIdFilter,
			long companyId,
			String chiefNameOrNull,
			String chiefMobileOrNull,
			int page,
			int pageSize) {
		int pg = page < 1 ? 1 : page;
		int sz = pageSize <= 0 ? 10 : pageSize;

		long total =
				communityChiefMapper.countChiefListByDistributorJoin(
						distributorIdFilter, companyId, chiefNameOrNull, chiefMobileOrNull);

		List<CommunityChiefListRowDto> list;
		if (total == 0) {
			list = Collections.emptyList();
		} else {
			int offset = (pg - 1) * sz;
			List<Map<String, Object>> raw =
					communityChiefMapper.selectChiefListByDistributorJoin(
							distributorIdFilter, companyId, chiefNameOrNull, chiefMobileOrNull, offset, sz);
			list = new ArrayList<>(raw.size());
			for (Map<String, Object> row : raw) {
				list.add(CommunityChiefListRowDto.fromRow(row));
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", (int) total);
		data.put("list", list);
		return data;
	}

	/**
	 * 团长详情：同列表联表，固定 company + chief_id + distributor_id，最多一行。
	 * chiefId 为路径 String（调用方已 trim）；不在此解析为 long。
	 *
	 * @return 未命中时 {@link Collections#emptyList()}；命中时 {@link CommunityChiefListRowDto}
	 */
	public Object getChiefInfoByDistributorJoin(long distributorId, long companyId, String chiefId) {
		Map<String, Object> row =
				communityChiefMapper.selectChiefDetailByDistributorJoin(distributorId, companyId, chiefId);
		if (row == null || row.isEmpty()) {
			return Collections.emptyList();
		}
		return CommunityChiefListRowDto.fromRow(row);
	}

	/**
	 * H5「检查团长」：先按会员查团长信息，否则尝试按手机号绑定未占用团长行，返回与前端约定的 {@code status}/{@code result} 内层 Map。
	 */
	public Map<String, Object> buildCheckChiefResponse(long companyId, long userId, String mobile) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", Boolean.FALSE);
		body.put("result", Collections.emptyList());
		Map<String, Object> chiefData = getChiefInfoByCompanyAndUser(companyId, userId);
		if (!chiefData.isEmpty()) {
			body.put("status", Boolean.TRUE);
			body.put("result", chiefData);
			return body;
		}
		chiefData = checkBindChiefForH5(companyId, userId, mobile);
		if (!chiefData.isEmpty()) {
			body.put("status", Boolean.TRUE);
			body.put("result", chiefData);
		}
		return body;
	}

	public Map<String, Object> getChiefInfoByCompanyAndUser(long companyId, long userId) {
		CommunityChief chief =
				communityChiefMapper.selectOne(
						new LambdaQueryWrapper<CommunityChief>()
								.eq(CommunityChief::getCompanyId, companyId)
								.eq(CommunityChief::getUserId, userId)
								.last("LIMIT 1"));
		if (chief == null) {
			return Collections.emptyMap();
		}
		return getChiefInfoCore(chief, companyId);
	}

	public Map<String, Object> getChiefInfoByChiefId(long companyId, long chiefId) {
		CommunityChief chief = communityChiefMapper.selectById(chiefId);
		if (chief == null || !Objects.equals(chief.getCompanyId(), companyId)) {
			return Collections.emptyMap();
		}
		return getChiefInfoCore(chief, companyId);
	}

	public Map<String, Object> checkBindChiefForH5(long companyId, long userId, String mobile) {
		CommunityChief row =
				communityChiefMapper.selectOne(
						new LambdaQueryWrapper<CommunityChief>()
								.eq(CommunityChief::getCompanyId, companyId)
								.eq(CommunityChief::getChiefMobile, mobile)
								.eq(CommunityChief::getUserId, 0L)
								.last("LIMIT 1"));
		if (row == null) {
			return Collections.emptyMap();
		}
		int nowInt = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<CommunityChief> uw = new LambdaUpdateWrapper<>();
		uw.eq(CommunityChief::getChiefId, row.getChiefId())
				.set(CommunityChief::getUserId, userId)
				.set(CommunityChief::getUpdatedAt, nowInt);
		int updated = communityChiefMapper.update(null, uw);
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		return getChiefInfoByChiefId(companyId, row.getChiefId());
	}

	private Map<String, Object> getChiefInfoCore(CommunityChief chief, long companyId) {
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(chief.getUserId(), companyId);
		if (memberInfo != null && !memberInfo.isEmpty()) {
			String memberName = stringVal(memberInfo.get("username"));
			String memberAvatar = stringVal(memberInfo.get("avatar"));
			if (!Objects.equals(chief.getChiefName(), memberName)
					|| !Objects.equals(chief.getChiefAvatar(), memberAvatar)) {
				int nowInt = (int) (System.currentTimeMillis() / 1000L);
				LambdaUpdateWrapper<CommunityChief> uw = new LambdaUpdateWrapper<>();
				uw.eq(CommunityChief::getChiefId, chief.getChiefId())
						.set(CommunityChief::getChiefName, memberName)
						.set(CommunityChief::getChiefAvatar, memberAvatar)
						.set(CommunityChief::getUpdatedAt, nowInt);
				int n = communityChiefMapper.update(null, uw);
				if (n == 0) {
					throw new ResourceException("未查询到更新数据");
				}
				CommunityChief reloaded = communityChiefMapper.selectById(chief.getChiefId());
				if (reloaded == null) {
					throw new ResourceException("未查询到更新数据");
				}
				chief = reloaded;
			}
		}
		List<Map<String, Object>> distRows =
				communityChiefDistributorListQueryService.listFormattedDistributorRowsForChief(
						companyId, chief.getChiefId());
		Map<String, Object> payload = chiefEntityToSnakeMap(chief);
		normalizeChiefPayloadRegionsFields(payload);
		payload.put(
				"distributors",
				distRows.isEmpty() ? Collections.emptyList() : distRows.get(0));
		return payload;
	}

	private void normalizeChiefPayloadRegionsFields(Map<String, Object> payload) {
		payload.put("regions_id", normalizeRegionsJsonValueForResponse(payload.get("regions_id")));
		payload.put("regions", normalizeRegionsJsonValueForResponse(payload.get("regions")));
	}

	private Object normalizeRegionsJsonValueForResponse(Object raw) {
		if (raw == null) {
			return Collections.emptyList();
		}
		if (!(raw instanceof String s)) {
			return Collections.emptyList();
		}
		if (!StringUtils.hasText(s)) {
			return Collections.emptyList();
		}
		String trimmed = s.trim();
		try {
			Object v = objectMapper.readValue(trimmed, Object.class);
			return v != null ? v : Collections.emptyList();
		} catch (JsonProcessingException e) {
			return Collections.emptyList();
		}
	}

	private static Map<String, Object> chiefEntityToSnakeMap(CommunityChief c) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("chief_id", c.getChiefId());
		m.put("company_id", c.getCompanyId());
		m.put("chief_name", c.getChiefName());
		m.put("chief_avatar", c.getChiefAvatar());
		m.put("chief_mobile", c.getChiefMobile());
		m.put("chief_desc", c.getChiefDesc());
		m.put("chief_intro", c.getChiefIntro());
		m.put("province", c.getProvince());
		m.put("city", c.getCity());
		m.put("area", c.getArea());
		m.put("regions_id", c.getRegionsId());
		m.put("regions", c.getRegions());
		m.put("address", c.getAddress());
		m.put("lng", c.getLng());
		m.put("lat", c.getLat());
		m.put("user_id", c.getUserId());
		m.put("created_at", c.getCreatedAt());
		m.put("updated_at", c.getUpdatedAt());
		m.put("alipay_name", c.getAlipayName());
		m.put("alipay_account", c.getAlipayAccount());
		m.put("bank_name", c.getBankName());
		m.put("bankcard_no", c.getBankcardNo());
		return m;
	}

	@Transactional(rollbackFor = Exception.class)
	public void createChiefFromAdmin(long companyId, String operatorType, int distributorIdFromRequest, Map<String, Object> mergedInput) {
		if (companyId <= 0L) {
			throw new BadRequestException("参数错误");
		}
		if ("distributor".equals(operatorType) && distributorIdFromRequest <= 0) {
			throw new BadRequestException("参数错误");
		}

		int distributorId = "distributor".equals(operatorType) ? distributorIdFromRequest : 0;
		List<Integer> distributorIds = List.of(distributorId);

		long uidFromInput = parseUserIdLoose(mergedInput.get("user_id"));
		Object mobRaw = mergedInput.get("mobile");
		String mobile = mobRaw == null ? "" : String.valueOf(mobRaw).trim();
		boolean mobilePresent = StringUtils.hasText(mobile);
		if (uidFromInput == 0L && !mobilePresent) {
			throw new BadRequestException("user_id和mobile必须选一个");
		}

		long resolvedUserId = uidFromInput;
		if (mobilePresent) {
			Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobile);
			if (member != null) {
				resolvedUserId = member.getUserId();
			}
		}
		if (resolvedUserId == 0L) {
			throw new ResourceException("无效的用户");
		}

		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(resolvedUserId, companyId);
		if (memberInfo == null || memberInfo.isEmpty()) {
			throw new ResourceException("无效的会员");
		}

		Long memberUserId = toLongUserId(memberInfo.get("user_id"));
		if (memberUserId == null || memberUserId == 0L) {
			throw new ResourceException("无效的会员");
		}

		String chiefNameInput = optTrimString(mergedInput, "chief_name");
		String chiefName =
				StringUtils.hasText(chiefNameInput) ? chiefNameInput : stringVal(memberInfo.get("username"));

		String chiefAvatarInput = optTrimString(mergedInput, "chief_avatar");
		String chiefAvatar =
				StringUtils.hasText(chiefAvatarInput) ? chiefAvatarInput : stringVal(memberInfo.get("avatar"));

		String chiefMobileInput = optTrimString(mergedInput, "chief_mobile");
		String chiefMobile =
				StringUtils.hasText(chiefMobileInput) ? chiefMobileInput : stringVal(memberInfo.get("mobile"));

		String chiefDesc = optTrimString(mergedInput, "chief_desc");
		if (!StringUtils.hasText(chiefDesc)) {
			chiefDesc = stringVal(memberInfo.get("chief_desc"));
		}
		String chiefIntro = optTrimString(mergedInput, "chief_intro");
		if (!StringUtils.hasText(chiefIntro)) {
			chiefIntro = stringVal(memberInfo.get("chief_intro"));
		}

		long chiefCompanyId = extractCompanyId(memberInfo, companyId);
		ChiefProfileInput profile = new ChiefProfileInput(chiefName, chiefAvatar, chiefMobile, chiefDesc, chiefIntro);

		upsertChiefDistributorsAndShopRels(companyId, chiefCompanyId, memberUserId, distributorIds, profile);
	}

	public void upsertChiefDistributorsAndShopRels(
			long relCompanyId,
			long chiefCompanyId,
			long memberUserId,
			List<Integer> distributorIds,
			ChiefProfileInput profile) {
		try {
			doUpsertChiefDistributorsAndShopRels(relCompanyId, chiefCompanyId, memberUserId, distributorIds, profile);
		} catch (ResourceException | BadRequestException e) {
			throw e;
		} catch (Exception e) {
			log.error("community chief upsert failed, memberUserId={}", memberUserId, e);
			throw new ResourceException(e.getMessage());
		}
	}

	private void doUpsertChiefDistributorsAndShopRels(
			long relCompanyId,
			long chiefCompanyId,
			long memberUserId,
			List<Integer> distributorIds,
			ChiefProfileInput profile) {
		if (memberUserId <= 0L) {
			throw new ResourceException("无效的会员");
		}
		if (distributorIds == null || distributorIds.isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		if (profile == null) {
			throw new BadRequestException("参数错误");
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		int nowInt = (int) nowSec;

		CommunityChief existing =
				communityChiefMapper.selectOne(new LambdaQueryWrapper<CommunityChief>()
						.eq(CommunityChief::getUserId, memberUserId)
						.last("LIMIT 1"));

		long resultChiefId;
		if (existing != null) {
			LambdaUpdateWrapper<CommunityChief> cuw = new LambdaUpdateWrapper<>();
			cuw.eq(CommunityChief::getChiefId, existing.getChiefId())
					.set(CommunityChief::getCompanyId, chiefCompanyId)
					.set(CommunityChief::getChiefName, profile.chiefName())
					.set(CommunityChief::getChiefAvatar, profile.chiefAvatar())
					.set(CommunityChief::getChiefMobile, profile.chiefMobile())
					.set(CommunityChief::getChiefDesc, profile.chiefDesc())
					.set(CommunityChief::getChiefIntro, profile.chiefIntro())
					.set(CommunityChief::getUserId, memberUserId)
					.set(CommunityChief::getUpdatedAt, nowInt);
			int updated = communityChiefMapper.update(null, cuw);
			if (updated == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			resultChiefId = existing.getChiefId();
		} else {
			CommunityChief chief = new CommunityChief();
			chief.setCompanyId(chiefCompanyId);
			chief.setChiefName(profile.chiefName());
			chief.setChiefAvatar(profile.chiefAvatar());
			chief.setChiefMobile(profile.chiefMobile());
			chief.setChiefDesc(profile.chiefDesc());
			chief.setChiefIntro(profile.chiefIntro());
			chief.setUserId(memberUserId);
			chief.setCreatedAt(nowInt);
			chief.setUpdatedAt(nowInt);
			communityChiefMapper.insert(chief);
			Long newId = chief.getChiefId();
			if (newId == null || newId == 0L) {
				throw new ResourceException("团长创建失败");
			}
			resultChiefId = newId;
		}

		if (!distributorIds.isEmpty()) {
			if (existing != null) {
				communityChiefDistributorMapper.delete(new LambdaQueryWrapper<CommunityChiefDistributor>()
						.eq(CommunityChiefDistributor::getChiefId, resultChiefId));
			}
			for (Integer distributorIdVal : distributorIds) {
				CommunityChiefDistributor row = new CommunityChiefDistributor();
				row.setChiefId(resultChiefId);
				// 平台/运营端为 0；店铺端为实际 distributor_id。必须为数值（非 null），否则 MyBatis-Plus 会省略列导致 DB 报错。
				int distVal = distributorIdVal == null ? 0 : distributorIdVal;
				row.setDistributorId((long) distVal);
				row.setBoundTime(nowSec);
				communityChiefDistributorMapper.insert(row);
			}
		}

		List<Long> nonZeroShopIds = new ArrayList<>();
		for (Integer d : distributorIds) {
			if (d == null || d == 0) {
				continue;
			}
			nonZeroShopIds.add(d.longValue());
		}
		Set<Long> existingShopIds = new HashSet<>();
		if (!nonZeroShopIds.isEmpty()) {
			List<ShopRelMember> rels = shopRelMemberMapper.selectList(new LambdaQueryWrapper<ShopRelMember>()
					.eq(ShopRelMember::getCompanyId, relCompanyId)
					.eq(ShopRelMember::getUserId, memberUserId)
					.in(ShopRelMember::getShopId, nonZeroShopIds)
					.select(ShopRelMember::getShopId));
			for (ShopRelMember r : rels) {
				if (r.getShopId() != null) {
					existingShopIds.add(r.getShopId());
				}
			}
		}
		long nowMs = System.currentTimeMillis();
		for (Integer d : distributorIds) {
			if (d == null || d == 0) {
				continue;
			}
			long shopId = d.longValue();
			if (existingShopIds.contains(shopId)) {
				continue;
			}
			ShopRelMember nm = new ShopRelMember();
			nm.setCompanyId(relCompanyId);
			nm.setUserId(memberUserId);
			nm.setShopId(shopId);
			nm.setShopType("distributor");
			nm.setCreated(nowMs);
			nm.setUpdated(nowMs);
			shopRelMemberMapper.insert(nm);
		}
	}

	static long extractCompanyId(Map<String, Object> memberInfo, long fallback) {
		Object o = memberInfo.get("company_id");
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o != null) {
			try {
				return Long.parseLong(o.toString().trim());
			} catch (NumberFormatException e) {
				return fallback;
			}
		}
		return fallback;
	}

	static Long toLongUserId(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	public static long parseUserIdLoose(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String optTrimString(Map<String, Object> m, String key) {
		Object v = m.get(key);
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	/**
	 * Excel row import for binding a registered member as community chief to a distributor shop.
	 */
	@Transactional(rollbackFor = Exception.class)
	public void importChiefUploadExcelRow(long companyId, long contextDistributorId, Map<String, Object> row) {
		long rowDist = parseUserIdLoose(row.get("distributor_id"));
		long ctx = contextDistributorId > 0 ? contextDistributorId : rowDist;
		long did = parseUserIdLoose(row.get("did"));
		String mobile = optTrimString(row, "mobile");
		if (!StringUtils.hasText(mobile) || !mobile.matches("^1[3456789]\\d{9}$")) {
			throw new BadRequestException("请填写正确的手机号");
		}
		if (ctx <= 0L && did <= 0L) {
			throw new BadRequestException("请填写店铺ID");
		}
		if (ctx > 0L) {
			if (did > 0L && did != ctx) {
				throw new ResourceException("只能上传本店铺的团长信息");
			}
			did = ctx;
		}

		Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobile);
		if (member == null || member.getUserId() == null || member.getUserId() <= 0L) {
			throw new ResourceException("手机号未注册成为会员，请先注册");
		}
		long userId = member.getUserId();

		CommunityChief chief =
				communityChiefMapper.selectOne(
						new LambdaQueryWrapper<CommunityChief>()
								.eq(CommunityChief::getUserId, userId)
								.last("LIMIT 1"));

		if (chief != null) {
			CommunityChiefDistributor rel =
					communityChiefDistributorMapper.selectOne(
							new LambdaQueryWrapper<CommunityChiefDistributor>()
									.eq(CommunityChiefDistributor::getChiefId, chief.getChiefId())
									.last("LIMIT 1"));
			long boundDist = rel != null && rel.getDistributorId() != null ? rel.getDistributorId() : 0L;
			if (boundDist > 0L && did != boundDist) {
				if (ctx > 0L) {
					throw new ResourceException("该手机号已绑定成为其他店铺的团长");
				}
				long activeActs =
						communityActivityMapper.selectCount(
								new LambdaQueryWrapper<CommunityActivity>()
										.eq(CommunityActivity::getChiefId, chief.getChiefId())
										.notIn(CommunityActivity::getActivityStatus, "success", "fail"));
				if (activeActs > 0L) {
					throw new ResourceException("该团长有进行中的活动，不能更换店铺");
				}
			}
		}

		if (did > 0L) {
			Map<String, Object> dist =
					distributorRepositoryGetInfoSimpleService.getInfoSimpleByDistributorId(companyId, did);
			if (dist == null || dist.isEmpty()) {
				throw new ResourceException("无效的店铺ID");
			}
		}

		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		if (memberInfo == null || memberInfo.isEmpty()) {
			throw new ResourceException("无效的会员");
		}
		Long memberUserId = toLongUserId(memberInfo.get("user_id"));
		if (memberUserId == null || memberUserId == 0L) {
			throw new ResourceException("无效的会员");
		}

		String chiefName = stringVal(memberInfo.get("username"));
		String chiefAvatar = stringVal(memberInfo.get("avatar"));
		String chiefMobile = stringVal(memberInfo.get("mobile"));
		String chiefDesc = stringVal(memberInfo.get("chief_desc"));
		String chiefIntro = stringVal(memberInfo.get("chief_intro"));
		long chiefCompanyId = extractCompanyId(memberInfo, companyId);
		ChiefProfileInput profile = new ChiefProfileInput(chiefName, chiefAvatar, chiefMobile, chiefDesc, chiefIntro);

		int didInt = did > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) did;
		upsertChiefDistributorsAndShopRels(companyId, chiefCompanyId, memberUserId, List.of(didInt), profile);
	}
}
