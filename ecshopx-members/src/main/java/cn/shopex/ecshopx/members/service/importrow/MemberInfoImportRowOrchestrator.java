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
import cn.shopex.ecshopx.members.domain.MembersOffineLog;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.MembersOffineLogMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.integration.point.MemberImportPointPort;
import cn.shopex.ecshopx.members.service.MemberRelTagsBatchCreateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberInfoImportRowOrchestrator {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private static final String PASSWORD_POOL =
			"QWERTYUIOPASDFGHJKLZXCVBNM1234567890qwertyuiopasdfghjklzxcvbnm";

	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MembersOffineLogMapper membersOffineLogMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberRelTagsBatchCreateService memberRelTagsBatchCreateService;
	private final MemberImportPointPort memberImportPointPort;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public MemberInfoImportRowOrchestrator(
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			MembersOffineLogMapper membersOffineLogMapper,
			MemberTagsMapper memberTagsMapper,
			MemberRelTagsBatchCreateService memberRelTagsBatchCreateService,
			MemberImportPointPort memberImportPointPort) {
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.membersOffineLogMapper = membersOffineLogMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsBatchCreateService = memberRelTagsBatchCreateService;
		this.memberImportPointPort = memberImportPointPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public void importRow(long companyId, Map<String, Object> row) {
		String mobile = str(row.get("mobile")).trim();
		String offlineCard = str(row.get("offline_card_code")).trim();
		if (!StringUtils.hasText(mobile) && !StringUtils.hasText(offlineCard)) {
			throw new BadRequestException("手机号或实体卡号至少填写一项");
		}
		String username = require(str(row.get("username")), "姓名必填");
		String sexRaw = require(str(row.get("sex")), "性别必填");
		String gradeName = require(str(row.get("grade_name")), "会员等级必填");
		String createdRaw = require(str(row.get("created")), "入会日期必填");

		validateSizes(mobile, offlineCard, username, row);
		validateEmail(row.get("email"));
		validatePoint(row.get("point"));

		long gradeId = resolveGradeId(companyId, gradeName);
		int sex = parseSex(sexRaw);
		long createdEpoch = parseUsSlashDateToEpoch(createdRaw, "入会日期格式错误");
		if (createdEpoch > System.currentTimeMillis() / 1000L) {
			throw new BadRequestException("入会时间不得大于今日");
		}
		String birthdayDb = null;
		String birthdayRaw = str(row.get("birthday")).trim();
		if (StringUtils.hasText(birthdayRaw)) {
			long bEpoch = parseUsSlashDateToEpoch(birthdayRaw, "生日格式错误");
			if (bEpoch > System.currentTimeMillis() / 1000L) {
				throw new BadRequestException("生日时间不得大于今日");
			}
			birthdayDb = LocalDate.ofInstant(java.time.Instant.ofEpochSecond(bEpoch), ZoneId.systemDefault())
					.format(DateTimeFormatter.ISO_LOCAL_DATE);
		}

		List<Long> tagIds = resolveTagIdsIfPresent(companyId, str(row.get("tags")));

		if (StringUtils.hasText(offlineCard) && !StringUtils.hasText(mobile)) {
			insertOfflinePending(companyId, offlineCard, username, sex, gradeId, birthdayDb, row, createdEpoch);
			return;
		}

		if (StringUtils.hasText(offlineCard)) {
			Members dupCard =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.eq(Members::getOfflineCardCode, offlineCard)
									.last("LIMIT 1"));
			if (dupCard != null) {
				throw new ResourceException("实体卡号已存在会员");
			}
		}
		if (StringUtils.hasText(mobile)) {
			if (!MOBILE_PATTERN.matcher(mobile).matches()) {
				throw new BadRequestException("请填写正确的手机号");
			}
			Members dupMobile =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.eq(Members::getMobile, LegacyFixedMobileEncrypt.fixedEncryptMobile(mobile))
									.last("LIMIT 1"));
			if (dupMobile != null) {
				throw new ResourceException("手机号已存在");
			}
		}

		long now = System.currentTimeMillis() / 1000L;
		ZonedDateTime joinZ =
				LocalDate.ofInstant(Instant.ofEpochSecond(createdEpoch), ZoneId.systemDefault())
						.atStartOfDay(ZoneId.systemDefault());
		String mobileStored = StringUtils.hasText(mobile) ? LegacyFixedMobileEncrypt.fixedEncryptMobile(mobile) : "";
		String plainPassword10 = randomPassword10();
		Members m = new Members();
		m.setCompanyId(companyId);
		m.setGradeId(gradeId);
		m.setMobile(StringUtils.hasText(mobile) ? mobileStored : "");
		m.setRegionMobile(StringUtils.hasText(mobile) ? mobile : "");
		m.setMobileCountryCode("86");
		m.setPassword(passwordEncoder.encode(plainPassword10));
		if (StringUtils.hasText(offlineCard)) {
			m.setOfflineCardCode(offlineCard);
			m.setUserCardCode(offlineCard);
		} else {
			m.setUserCardCode(randomCardCode());
		}
		m.setCreated(createdEpoch);
		m.setUpdated(now);
		m.setDisabled(false);
		m.setCreatedYear(joinZ.getYear());
		m.setCreatedMonth(joinZ.getMonthValue());
		m.setCreatedDay(joinZ.getDayOfMonth());
		membersMapper.insert(m);
		long userId = m.getUserId();

		MembersInfo info = new MembersInfo();
		info.setUserId(userId);
		info.setCompanyId(companyId);
		info.setUsername(username);
		info.setSex(sex);
		info.setBirthday(birthdayDb == null ? "" : birthdayDb);
		info.setAddress(str(row.get("address")));
		info.setEmail(str(row.get("email")).trim());
		info.setOtherParams("{\"is_upload_member\":true}");
		info.setCreated(now);
		info.setUpdated(now);
		membersInfoMapper.insert(info);

		if (tagIds != null && !tagIds.isEmpty()) {
			memberRelTagsBatchCreateService.createRelTagsByUserId(userId, tagIds, companyId);
		}
		applyInitialPointIfPresent(userId, companyId, row.get("point"));
	}

	private void applyInitialPointIfPresent(long userId, long companyId, Object pointObj) {
		if (pointObj == null || str(pointObj).trim().isEmpty()) {
			return;
		}
		int point = (int) Math.round(Double.parseDouble(str(pointObj).trim()));
		if (point > 0) {
			memberImportPointPort.adjustImportPoint(userId, companyId, point, true, "会员信息导入，初始化积分");
		}
	}

	private void insertOfflinePending(
			long companyId,
			String offlineCard,
			String username,
			int sex,
			long gradeId,
			String birthdayDb,
			Map<String, Object> row,
			long createdEpoch) {
		MembersOffineLog log = new MembersOffineLog();
		log.setCompanyId(companyId);
		log.setOfflineCardCode(offlineCard);
		log.setUsername(username);
		log.setSex(sex);
		log.setGradeId(String.valueOf(gradeId));
		log.setBirthday(birthdayDb == null ? "" : birthdayDb);
		log.setAddress(str(row.get("address")));
		log.setEmail(str(row.get("email")).trim());
		log.setCreatedTime(String.valueOf(createdEpoch));
		long now = System.currentTimeMillis() / 1000L;
		log.setCreated(now);
		log.setUpdated(now);
		membersOffineLogMapper.insert(log);
	}

	private List<Long> resolveTagIdsIfPresent(long companyId, String tagsCsv) {
		if (!StringUtils.hasText(tagsCsv)) {
			return null;
		}
		String[] parts = tagsCsv.split(",");
		LinkedHashSet<String> names = new LinkedHashSet<>();
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
		Long id = membersMapper.selectGradeIdByCompanyIdAndGradeName(companyId, gradeName.trim());
		if (id == null || id <= 0L) {
			throw new ResourceException("会员等级不存在: " + gradeName);
		}
		return id;
	}

	private static void validateSizes(
			String mobile, String offlineCard, String username, Map<String, Object> row) {
		if (StringUtils.hasText(mobile) && mobile.length() > 32) {
			throw new BadRequestException("手机号长度超出限制");
		}
		if (StringUtils.hasText(offlineCard) && offlineCard.length() > 20) {
			throw new BadRequestException("实体卡号长度超出限制");
		}
		if (username.length() > 20) {
			throw new BadRequestException("姓名长度超出限制");
		}
		String sex = str(row.get("sex"));
		if (sex.length() > 6) {
			throw new BadRequestException("性别长度超出限制");
		}
		String grade = str(row.get("grade_name"));
		if (grade.length() > 8) {
			throw new BadRequestException("会员等级长度超出限制");
		}
		String addr = str(row.get("address"));
		if (addr.length() > 128) {
			throw new BadRequestException("地址长度超出限制");
		}
		String email = str(row.get("email"));
		if (email.length() > 32) {
			throw new BadRequestException("邮箱长度超出限制");
		}
	}

	private static void validateEmail(Object emailObj) {
		String email = str(emailObj).trim();
		if (!email.isEmpty() && !email.contains("@")) {
			throw new BadRequestException("邮箱格式错误");
		}
	}

	private static void validatePoint(Object pointObj) {
		if (pointObj == null || str(pointObj).trim().isEmpty()) {
			return;
		}
		try {
			double d = Double.parseDouble(str(pointObj).trim());
			if (d < 0) {
				throw new BadRequestException("积分必须大于等于0");
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException("积分格式错误");
		}
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

	/**
	 * Accepts {@code mm/dd/yyyy} (template) and {@code yyyy/MM/dd} / {@code yyyy-MM-dd} (common Excel text).
	 */
	private static long parseUsSlashDateToEpoch(String raw, String err) {
		try {
			String normalized = raw.trim().replace('-', '/');
			String[] a = normalized.split("/");
			if (a.length != 3) {
				throw new BadRequestException(err);
			}
			int p0 = Integer.parseInt(a[0].trim());
			int p1 = Integer.parseInt(a[1].trim());
			int p2 = Integer.parseInt(a[2].trim());
			int year;
			int month;
			int day;
			if (p0 > 31 || a[0].trim().length() == 4) {
				year = p0;
				month = p1;
				day = p2;
			} else {
				month = p0;
				day = p1;
				year = p2;
				if (year < 100) {
					year += 2000;
				}
			}
			LocalDate ld = LocalDate.of(year, month, day);
			return ld.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		} catch (BadRequestException e) {
			throw e;
		} catch (NumberFormatException | java.time.DateTimeException e) {
			throw new BadRequestException(err);
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

	private static String randomPassword10() {
		char[] pool = PASSWORD_POOL.toCharArray();
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (int i = pool.length - 1; i > 0; i--) {
			int j = r.nextInt(i + 1);
			char t = pool[i];
			pool[i] = pool[j];
			pool[j] = t;
		}
		String shuffled = new String(pool);
		int start = 5;
		int len = 10;
		if (shuffled.length() < start + len) {
			return shuffled;
		}
		return shuffled.substring(start, start + len);
	}

	private static String randomCardCode() {
		String chars = "QWERTYUIOPASDFGHJKLZXCVBNM1234567890qwertyuiopasdfghjklzxcvbnm";
		ThreadLocalRandom r = ThreadLocalRandom.current();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			sb.append(chars.charAt(r.nextInt(chars.length())));
		}
		return sb.toString();
	}
}
