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

package cn.shopex.ecshopx.wsugc.service.point;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.point.domain.PointMember;
import cn.shopex.ecshopx.point.domain.PointMemberLog;
import cn.shopex.ecshopx.point.mapper.PointMemberLogMapper;
import cn.shopex.ecshopx.point.mapper.PointMemberMapper;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.domain.Setting;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.mapper.SettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UgcPostPointService {

	private static final Logger log = LoggerFactory.getLogger(UgcPostPointService.class);

	private static final List<Integer> ALL_UGC_JOURNAL_TYPES = List.of(20, 21, 22, 23, 24, 9920, 9921, 9922);

	private static final Map<Integer, JournalMeta> JOURNAL_META = Map.ofEntries(
			Map.entry(20, new JournalMeta("发布笔记", "point_post_create_get_once", "point_post_create_get_max_times_day")),
			Map.entry(21, new JournalMeta("笔记点赞", "point_post_like_get_once", "point_post_like_get_max_times_day")),
			Map.entry(22, new JournalMeta("评论笔记", "point_post_comment_get_once", "point_post_comment_get_max_times_day")),
			Map.entry(23, new JournalMeta("收藏笔记", "point_post_favorite_get_once", "point_post_favorite_get_max_times_day")),
			Map.entry(24, new JournalMeta("分享笔记", "point_post_share_get_once", "point_post_share_get_max_times_day")),
			Map.entry(9920, new JournalMeta("拒绝笔记", "point.post.refuse.get_once", "point.post.refuse.get_max_times_day")),
			Map.entry(9921, new JournalMeta("拒绝点赞笔记", "point.like.refuse.get_once", "point.like.refuse.get_max_times_day")),
			Map.entry(9922, new JournalMeta("拒绝评论笔记", "point.comment.refuse.get_once", "point.comment.refuse.get_max_times_day")));

	private final SettingMapper settingMapper;
	private final PointMemberMapper pointMemberMapper;
	private final PointMemberLogMapper pointMemberLogMapper;
	private final MemberAccountService memberAccountService;
	private final PostMapper postMapper;

	public UgcPostPointService(
			SettingMapper settingMapper,
			PointMemberMapper pointMemberMapper,
			PointMemberLogMapper pointMemberLogMapper,
			MemberAccountService memberAccountService,
			PostMapper postMapper) {
		this.settingMapper = settingMapper;
		this.pointMemberMapper = pointMemberMapper;
		this.pointMemberLogMapper = pointMemberLogMapper;
		this.memberAccountService = memberAccountService;
		this.postMapper = postMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean addUgcPoint(long postId, long userId, long companyId, int journalType, String addOrReduce) {
		boolean reduce = StringUtils.hasText(addOrReduce);
		PointParams pointParams = getPointByAction(companyId, journalType, addOrReduce);
		String actionTitle = pointParams.title();

		int point;
		int getMaxTimesDay = pointParams.getMaxTimesDay();

		if (!reduce) {
			if (!isPointEnabled(companyId)) {
				log.debug("ugc积分开启状态:未开启");
				return false;
			}
			if (!pointParams.hasOnceConfigured()) {
				log.debug("ugc积分此动作:未设置积分数量");
				return false;
			}
			point = pointParams.once();
			if (point <= 0) {
				log.debug("ugc积分此动作设置的point不大于0,journal_type:{}", journalType);
				return false;
			}
			if (journalType != 22 && journalType != 24) {
				if (!checkSamePostIdNoRecord(postId, userId, journalType, false)) {
					log.debug("ugc积分此动作此post_id，user_id已存在积分记录 post_id:{}user_id{}|journal_type:{}", postId, userId, journalType);
					return false;
				}
			}
			if (!checkPointMaxDay(companyId, userId, point)) {
				return false;
			}
			if (!checkPointActionMaxTimes(userId, journalType, getMaxTimesDay)) {
				return false;
			}
		} else {
			if (!checkSamePostIdNoRecord(postId, userId, journalType, true)) {
				log.debug("扣减积分-ugc积分此动作此post_id，user_id已存在积分记录 post_id:{}user_id{}|journal_type:{}", postId, userId, journalType);
				return false;
			}
			int oldJournalType = strip99JournalType(journalType);
			point = fetchGrantedIncomePoints(postId, userId, companyId, oldJournalType);
		}

		Map<String, Object> memberRow = memberAccountService.getMemberInfo(userId, companyId);
		Object mob = memberRow.get("mobile");
		String mobile = mob != null ? mob.toString().trim() : "";
		if (!StringUtils.hasText(mobile)) {
			return false;
		}

		point = Math.toIntExact((long) point);
		if (point <= 0) {
			return false;
		}

		boolean statusAdd = !reduce;
		Post postRow = postMapper.selectById(postId);
		String postTitle = postRow != null && postRow.getTitle() != null ? postRow.getTitle() : "";
		String record = mobile + actionTitle + postId + "【" + postTitle + "】";

		applyPointChange(userId, companyId, point, journalType, statusAdd, record, postId);
		return true;
	}

	private void applyPointChange(
			long userId,
			long companyId,
			int point,
			int journalType,
			boolean statusAdd,
			String record,
			long postId) {
		if (point != 0) {
			LambdaQueryWrapper<PointMember> q = new LambdaQueryWrapper<>();
			q.eq(PointMember::getUserId, userId).eq(PointMember::getCompanyId, companyId);
			PointMember row = pointMemberMapper.selectOne(q);
			long newBalance;
			if (statusAdd) {
				if (row == null) {
					PointMember ins = new PointMember();
					ins.setUserId(userId);
					ins.setCompanyId(companyId);
					ins.setPoint((long) point);
					pointMemberMapper.insert(ins);
					newBalance = point;
				} else {
					newBalance = row.getPoint() + point;
					LambdaUpdateWrapper<PointMember> uw = new LambdaUpdateWrapper<>();
					uw.eq(PointMember::getUserId, userId)
							.eq(PointMember::getCompanyId, companyId)
							.set(PointMember::getPoint, newBalance);
					pointMemberMapper.update(null, uw);
				}
			} else {
				if (row == null || row.getPoint() < point) {
					throw new ResourceException("积分不足");
				}
				newBalance = row.getPoint() - point;
				LambdaUpdateWrapper<PointMember> uw = new LambdaUpdateWrapper<>();
				uw.eq(PointMember::getUserId, userId)
						.eq(PointMember::getCompanyId, companyId)
						.ge(PointMember::getPoint, point)
						.set(PointMember::getPoint, newBalance);
				int affected = pointMemberMapper.update(null, uw);
				if (affected <= 0) {
					throw new ResourceException("积分不足");
				}
			}

			int now = (int) (System.currentTimeMillis() / 1000);
			PointMemberLog logRow = new PointMemberLog();
			logRow.setUserId(userId);
			logRow.setCompanyId(companyId);
			logRow.setJournalType(journalType);
			String descBase = StringUtils.hasText(record) ? record : "无记录";
			logRow.setPointDesc(descBase + "，当前剩余积分：" + newBalance);
			logRow.setIncome(statusAdd ? point : 0);
			logRow.setOutcome(statusAdd ? 0 : point);
			logRow.setOrderId(String.valueOf(postId));
			logRow.setExternalId(String.valueOf(postId));
			logRow.setCreated(now);
			logRow.setUpdated(now);
			pointMemberLogMapper.insert(logRow);
		}
	}

	private PointParams getPointByAction(long companyId, int journalType, String addOrReduce) {
		boolean reduce = StringUtils.hasText(addOrReduce);
		JournalMeta meta = JOURNAL_META.get(journalType);
		log.debug("ugc积分getPointByAction-allJournalType：|add_or_reduce:{}|journal_type:{}", addOrReduce, journalType);
		if (meta == null) {
			return new PointParams("", 0, 0);
		}
		String title = meta.title();
		if (reduce && journalType == 20) {
			title = "拒绝笔记";
		} else if (reduce && journalType == 22) {
			title = "拒绝评论";
		}
		int once = parseIntSetting(getSetting(companyId, meta.onceKey()), 0);
		int maxTimes = parseIntSetting(getSetting(companyId, meta.maxKey()), 0);
		return new PointParams(title, once, maxTimes);
	}

	private boolean checkPointActionMaxTimes(long userId, int journalType, int getMaxTimesDay) {
		int[] bounds = todayBounds();
		LambdaQueryWrapper<PointMemberLog> q = new LambdaQueryWrapper<>();
		q.eq(PointMemberLog::getUserId, userId)
				.eq(PointMemberLog::getJournalType, journalType)
				.ge(PointMemberLog::getCreated, bounds[0])
				.le(PointMemberLog::getCreated, bounds[1]);
		Long count = pointMemberLogMapper.selectCount(q);
		long c = count != null ? count : 0;
		if (c + 1 > getMaxTimesDay) {
			log.debug(
					"ugc积分当日journal_type:{},赠送 超出每日总次数限制：user_id:{}|count:{}|当前累计积分含本次赠送:{}|get_max_times_day:{}",
					journalType,
					userId,
					c,
					c + 1,
					getMaxTimesDay);
			return false;
		}
		log.debug(
				"ugc积分当日journal_type:{},赠送 没有超出每日总次数限制：user_id:{}|count:{}|当前累计积分含本次赠送:{}|get_max_times_day:{}",
				journalType,
				userId,
				c,
				c + 1,
				getMaxTimesDay);
		return true;
	}

	private boolean checkPointMaxDay(long companyId, long userId, int point) {
		int[] bounds = todayBounds();
		int pointMaxDay = parseIntSetting(getSetting(companyId, "point_max_day"), 0);
		int sum = sumIncomeToday(userId, companyId, bounds[0], bounds[1]);
		if (sum + point > pointMaxDay) {
			log.debug(
					"ugc积分当日已赠送 超出每日总数限制：user_id:{}|sum:{}|当前累计积分含本次赠送:{}|point_max_day:{}",
					userId,
					sum,
					sum + point,
					pointMaxDay);
			return false;
		}
		log.debug(
				"ugc积分当日已赠送 还没有每日总数限制：user_id:{}|sum:{}|当前累计积分含本次赠送:{}|point_max_day:{}",
				userId,
				sum,
				sum + point,
				pointMaxDay);
		return true;
	}

	private int sumIncomeToday(long userId, long companyId, int dayStart, int dayEnd) {
		LambdaQueryWrapper<PointMemberLog> q = new LambdaQueryWrapper<>();
		q.eq(PointMemberLog::getUserId, userId)
				.eq(PointMemberLog::getCompanyId, companyId)
				.in(PointMemberLog::getJournalType, ALL_UGC_JOURNAL_TYPES)
				.ge(PointMemberLog::getCreated, dayStart)
				.le(PointMemberLog::getCreated, dayEnd)
				.gt(PointMemberLog::getIncome, 0);
		List<PointMemberLog> rows = pointMemberLogMapper.selectList(q);
		int sum = 0;
		for (PointMemberLog r : rows) {
			sum += r.getIncome() != null ? r.getIncome() : 0;
		}
		return sum;
	}

	private boolean checkSamePostIdNoRecord(long postId, long userId, int journalType, boolean reduce) {
		LambdaQueryWrapper<PointMemberLog> q = new LambdaQueryWrapper<>();
		q.eq(PointMemberLog::getUserId, userId)
				.eq(PointMemberLog::getExternalId, String.valueOf(postId))
				.eq(PointMemberLog::getJournalType, journalType);
		if (!reduce) {
			q.gt(PointMemberLog::getIncome, 0);
		} else {
			q.gt(PointMemberLog::getOutcome, 0);
		}
		Long c = pointMemberLogMapper.selectCount(q);
		boolean missing = c == null || c == 0;
		if (!missing) {
			log.debug("ugc积分checkSamePostId 存在.user_id：{}|post_id：{}|journal_type:{}", userId, postId, journalType);
		} else {
			log.debug("ugc积分checkSamePostId 不存在.user_id：{}|post_id：{}|journal_type:{}", userId, postId, journalType);
		}
		return missing;
	}

	private int fetchGrantedIncomePoints(long postId, long userId, long companyId, int journalType) {
		LambdaQueryWrapper<PointMemberLog> q = new LambdaQueryWrapper<>();
		q.eq(PointMemberLog::getUserId, userId)
				.eq(PointMemberLog::getCompanyId, companyId)
				.eq(PointMemberLog::getExternalId, String.valueOf(postId))
				.eq(PointMemberLog::getJournalType, journalType)
				.gt(PointMemberLog::getIncome, 0)
				.orderByDesc(PointMemberLog::getId)
				.last("LIMIT 1");
		PointMemberLog row = pointMemberLogMapper.selectOne(q);
		return row != null && row.getIncome() != null ? row.getIncome() : 0;
	}

	private static int strip99JournalType(int journalType) {
		String s = String.valueOf(journalType).replace("99", "");
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return journalType;
		}
	}

	private boolean isPointEnabled(long companyId) {
		String v = getSetting(companyId, "point_enable").trim();
		return StringUtils.hasText(v) && !"0".equals(v) && !"false".equalsIgnoreCase(v);
	}

	private String getSetting(long companyId, String keyname) {
		Setting s = settingMapper.selectOne(new LambdaQueryWrapper<Setting>()
				.eq(Setting::getCompanyId, companyId)
				.eq(Setting::getKeyname, keyname)
				.last("LIMIT 1"));
		return s != null && s.getValue() != null ? s.getValue() : "";
	}

	private static int parseIntSetting(String raw, int defaultVal) {
		if (!StringUtils.hasText(raw)) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static int[] todayBounds() {
		ZoneId z = ZoneId.systemDefault();
		LocalDate d = LocalDate.now(z);
		int start = (int) d.atStartOfDay(z).toEpochSecond();
		int end = (int) d.atTime(23, 59, 59).atZone(z).toEpochSecond();
		return new int[] { start, end };
	}

	private record JournalMeta(String title, String onceKey, String maxKey) {}

	private record PointParams(String title, int once, int getMaxTimesDay) {
		boolean hasOnceConfigured() {
			return once > 0;
		}
	}
}
