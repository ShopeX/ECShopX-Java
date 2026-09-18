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

package cn.shopex.ecshopx.members.service.importrow;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.integration.point.MemberImportPointPort;
import cn.shopex.ecshopx.members.service.MemberRelTagsBatchCreateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberUpdateImportRowOrchestrator {

	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberRelTagsBatchCreateService memberRelTagsBatchCreateService;
	private final MemberImportPointPort memberImportPointPort;

	public MemberUpdateImportRowOrchestrator(
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			MemberTagsMapper memberTagsMapper,
			MemberRelTagsBatchCreateService memberRelTagsBatchCreateService,
			MemberImportPointPort memberImportPointPort) {
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsBatchCreateService = memberRelTagsBatchCreateService;
		this.memberImportPointPort = memberImportPointPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public void importRow(long companyId, Map<String, Object> row) {
		String mobile = require(str(row.get("mobile")), "手机号必填");
		if (mobile.length() > 32) {
			throw new BadRequestException("手机号长度超出限制");
		}
		validateOptionalLengths(row);
		validatePoint(row.get("point"));

		List<Long> tagIds = resolveTagIdsIfPresent(companyId, str(row.get("tags")));

		String mobileStored = LegacyFixedMobileEncrypt.fixedEncryptMobile(mobile);
		Members member =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getMobile, mobileStored)
								.last("LIMIT 1"));
		if (member == null) {
			throw new BadRequestException("会员数据不存在");
		}
		long userId = member.getUserId();

		MembersInfo info =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.eq(MembersInfo::getUserId, userId)
								.last("LIMIT 1"));
		if (info == null) {
			throw new ResourceException("会员资料不存在");
		}

		boolean touchedInfo = false;
		if (StringUtils.hasText(str(row.get("username")))) {
			info.setUsername(str(row.get("username")).trim());
			touchedInfo = true;
		}
		if (StringUtils.hasText(str(row.get("sex")))) {
			info.setSex(parseSex(str(row.get("sex")).trim()));
			touchedInfo = true;
		}
		if (StringUtils.hasText(str(row.get("email")))) {
			String em = str(row.get("email")).trim();
			if (!em.contains("@")) {
				throw new BadRequestException("邮箱格式错误");
			}
			info.setEmail(em);
			touchedInfo = true;
		}
		if (touchedInfo) {
			info.setUpdated(System.currentTimeMillis() / 1000L);
			membersInfoMapper.updateById(info);
		}

		LambdaUpdateWrapper<Members> uw = new LambdaUpdateWrapper<>();
		uw.eq(Members::getCompanyId, companyId).eq(Members::getUserId, userId);
		boolean touchedMember = false;
		if (StringUtils.hasText(str(row.get("grade_name")))) {
			long gid = resolveGradeId(companyId, str(row.get("grade_name")).trim());
			uw.set(Members::getGradeId, gid);
			touchedMember = true;
		}
		if (StringUtils.hasText(str(row.get("disabled")))) {
			int d = parseDisabled(str(row.get("disabled")).trim());
			if (d != 2) {
				uw.set(Members::getDisabled, d == 1);
				touchedMember = true;
			}
		}
		if (touchedMember) {
			uw.set(Members::getUpdated, System.currentTimeMillis() / 1000L);
			int n = membersMapper.update(null, uw);
			if (n == 0) {
				throw new ResourceException("会员更新失败");
			}
		}

		if (tagIds != null && !tagIds.isEmpty()) {
			memberRelTagsBatchCreateService.createRelTagsByUserId(userId, tagIds, companyId);
		}
		applyPointAdjustmentIfPresent(userId, companyId, row.get("point"));
	}

	private void applyPointAdjustmentIfPresent(long userId, long companyId, Object pointObj) {
		if (pointObj == null || str(pointObj).trim().isEmpty()) {
			return;
		}
		double raw = Double.parseDouble(str(pointObj).trim());
		if (raw == 0) {
			return;
		}
		boolean plus = raw > 0;
		int point = (int) Math.round(Math.abs(raw));
		if (point > 0) {
			memberImportPointPort.adjustImportPoint(userId, companyId, point, plus, "会员信息导入，修改会员积分");
		}
	}

	private static void validatePoint(Object pointObj) {
		if (pointObj == null || str(pointObj).trim().isEmpty()) {
			return;
		}
		try {
			Double.parseDouble(str(pointObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("积分格式错误");
		}
	}

	private List<Long> resolveTagIdsIfPresent(long companyId, String tagsCsv) {
		if (!StringUtils.hasText(tagsCsv)) {
			return null;
		}
		String[] parts = tagsCsv.split(",");
		Set<String> names = new LinkedHashSet<>();
		for (String p : parts) {
			String t = p.trim();
			if (!t.isEmpty()) {
				names.add(t);
			}
		}
		if (names.isEmpty()) {
			return null;
		}
		List<Long> ids = new ArrayList<>();
		for (String name : names) {
			MemberTags tag =
					memberTagsMapper.selectOne(
							new LambdaQueryWrapper<MemberTags>()
									.eq(MemberTags::getCompanyId, companyId)
									.eq(MemberTags::getTagName, name)
									.last("LIMIT 1"));
			if (tag == null) {
				throw new ResourceException("标签不存在: " + name);
			}
			ids.add(tag.getTagId());
		}
		return ids;
	}

	private long resolveGradeId(long companyId, String gradeName) {
		Long id = membersMapper.selectGradeIdByCompanyIdAndGradeName(companyId, gradeName);
		if (id == null || id <= 0L) {
			throw new ResourceException("会员等级不存在: " + gradeName);
		}
		return id;
	}

	private static int parseSex(String sexRaw) {
		if ("男".equals(sexRaw)) {
			return 1;
		}
		if ("女".equals(sexRaw)) {
			return 2;
		}
		if ("未知".equals(sexRaw)) {
			return 0;
		}
		throw new BadRequestException("性别只能为男、女或未知");
	}

	private static int parseDisabled(String raw) {
		if ("否".equals(raw)) {
			return 0;
		}
		if ("是".equals(raw)) {
			return 1;
		}
		return 2;
	}

	private static void validateOptionalLengths(Map<String, Object> row) {
		String u = str(row.get("username"));
		if (u.length() > 20) {
			throw new BadRequestException("姓名长度超出限制");
		}
		String s = str(row.get("sex"));
		if (s.length() > 6) {
			throw new BadRequestException("性别长度超出限制");
		}
		String g = str(row.get("grade_name"));
		if (g.length() > 8) {
			throw new BadRequestException("会员等级长度超出限制");
		}
		String d = str(row.get("disabled"));
		if (d.length() > 1) {
			throw new BadRequestException("禁用状态长度超出限制");
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static String require(String v, String err) {
		if (!StringUtils.hasText(v)) {
			throw new BadRequestException(err);
		}
		return v;
	}
}
