package cn.shopex.ecshopx.promotions.domain.membersignin;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MemberSigninBe01SchemaTest {

	@Test
	void activity_defaultsConfigVersionToOne() {
		MemberSigninActivity activity = new MemberSigninActivity();
		assertThat(activity.getConfigVersion()).isEqualTo(1L);
	}

	@Test
	void activity_hasNoPersistedStatusFields() {
		Set<String> fields = fieldNames(MemberSigninActivity.class);
		assertThat(fields).doesNotContain("status", "stoppedAt", "displayStatus");
		assertThat(fields).contains("beginTime", "endTime", "configVersion");
	}

	@Test
	void dailyPoints_defaultsWeekdayPointsToZero() {
		MemberSigninDailyPoints config = new MemberSigninDailyPoints();
		assertThat(config.getPointsMon()).isZero();
		assertThat(config.getPointsSun()).isZero();
		assertThat(config.getRuleDesc()).isEmpty();
		assertThat(fieldNames(MemberSigninDailyPoints.class)).contains("ruleDesc");
	}

	@Test
	void record_exposesUniqueKeyAndActivityFields() {
		Set<String> fields = fieldNames(MemberSigninRecord.class);
		assertThat(fields)
				.contains("companyId", "userId", "signDate", "weekday", "dailyPoints", "activityId", "activitySeq");
	}

	@Test
	void progressAndGrant_exposeUniqueKeyFields() {
		assertThat(fieldNames(MemberSigninActivityProgress.class))
				.contains("companyId", "userId", "activityId", "progressCount");
		assertThat(fieldNames(MemberSigninRewardGrant.class))
				.contains(
						"companyId",
						"userId",
						"recordType",
						"activityId",
						"tierCount",
						"signDate",
						"title",
						"status",
						"rewardSnapshot",
						"errorMessage",
						"signinRecordId");
	}

	@Test
	void reminder_exposesSubscribeFields() {
		MemberSigninReminder reminder = new MemberSigninReminder();
		assertThat(reminder.getEnabled()).isFalse();
		assertThat(reminder.getSubscribeAccepted()).isFalse();
		assertThat(fieldNames(MemberSigninReminder.class))
				.contains("subscribeTemplateId", "lastPushDate", "enabled", "subscribeAccepted");
	}

	@Test
	void memberSigninReminderScene_registeredInWxaDefinitions() {
		var def =
				cn.shopex.ecshopx.promotions.service.wxatemplate.WxaTemplateSceneDefinitions
						.findByScenesName("member_signin_reminder");
		assertThat(def).isPresent();
		assertThat(def.get().getId()).isEqualTo("6240");
		assertThat(def.get().getKeywordIdList()).containsExactly(5, 3);
		assertThat(def.get().getAllowedTemplateNames()).contains("yykweishop");
	}

	@Test
	void displayStatus_constantsMatchPrd() {
		assertThat(MemberSigninDisplayStatus.NOT_STARTED).isEqualTo("not_started");
		assertThat(MemberSigninDisplayStatus.IN_PROGRESS).isEqualTo("in_progress");
		assertThat(MemberSigninDisplayStatus.ENDED).isEqualTo("ended");
	}

	@Test
	void rewardGrantStatus_constantsMatchPrd() {
		assertThat(MemberSigninRewardGrantStatus.PENDING).isEqualTo("pending");
		assertThat(MemberSigninRewardGrantStatus.GRANTED).isEqualTo("granted");
		assertThat(MemberSigninRewardGrantStatus.FAILED).isEqualTo("failed");
	}

	private static Set<String> fieldNames(Class<?> type) {
		return Stream.of(type.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
	}
}
