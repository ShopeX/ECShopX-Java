package cn.shopex.ecshopx.promotions.service.membersignin;

import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDailyPoints;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDailyPointsPayload;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninDailyPointsRules;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninDailyPointsMapper;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberSignInDailyPointsService {

	private final MemberSigninDailyPointsMapper dailyPointsMapper;
	private final MessageSource messageSource;

	public MemberSignInDailyPointsService(
			MemberSigninDailyPointsMapper dailyPointsMapper, MessageSource messageSource) {
		this.dailyPointsMapper = dailyPointsMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getDailyPoints(long companyId) {
		MemberSigninDailyPoints row = dailyPointsMapper.selectById(companyId);
		return MemberSigninDailyPointsPayload.toMap(row);
	}

	@Transactional
	public Map<String, Object> saveDailyPoints(
			long companyId, Map<String, Object> input, Locale locale) {
		Map<String, Integer> parsed =
				MemberSigninDailyPointsRules.parseNonNegativeWeekdayPoints(input, messageSource, locale);
		String ruleDesc =
				MemberSigninDailyPointsRules.parseRuleDesc(
						input == null ? null : input.get(MemberSigninDailyPointsPayload.RULE_DESC),
						messageSource,
						locale);
		int now = (int) Instant.now().getEpochSecond();
		MemberSigninDailyPoints row = dailyPointsMapper.selectById(companyId);
		if (row == null) {
			row = new MemberSigninDailyPoints();
			row.setCompanyId(companyId);
			MemberSigninDailyPointsPayload.applyToEntity(row, parsed, ruleDesc);
			row.setUpdated(now);
			dailyPointsMapper.insert(row);
		} else {
			MemberSigninDailyPointsPayload.applyToEntity(row, parsed, ruleDesc);
			row.setUpdated(now);
			dailyPointsMapper.updateById(row);
		}
		return MemberSigninDailyPointsPayload.toMap(row);
	}
}
