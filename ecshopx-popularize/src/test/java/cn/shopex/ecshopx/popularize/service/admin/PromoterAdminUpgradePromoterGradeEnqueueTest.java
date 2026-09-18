package cn.shopex.ecshopx.popularize.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.dispatch.UpgradePromoterGradeJobDispatchPublisher;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.popularize.service.PromoterGradeUpgradeTriggerService;
import cn.shopex.ecshopx.popularize.service.PromoterMemberRelRemoveService;
import cn.shopex.ecshopx.popularize.service.PromoterRelRemoveService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromoterAdminUpgradePromoterGradeEnqueueTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Promoter.class);
	}

	@Mock
	private PromoterMapper promoterMapper;

	@Mock
	private MemberAccountService memberAccountService;

	@Mock
	private UpgradePromoterGradeJobDispatchPublisher upgradePromoterGradeJobDispatchPublisher;

	@Test
	void whenRelRemoveWithNewParent_thenEnqueueUpgradeForNewParentUserMemberAndOldPid() {
		long companyId = 1L;
		long userId = 300L;
		long newUserId = 500L;
		long oldParentRecordId = 700L;

		PromoterGradeUpgradeTriggerService triggerService =
				new PromoterGradeUpgradeTriggerService(upgradePromoterGradeJobDispatchPublisher);
		PromoterRelRemoveService relRemoveService =
				new PromoterRelRemoveService(promoterMapper, memberAccountService, triggerService);

		Promoter userInfo = new Promoter();
		userInfo.setUserId(userId);
		userInfo.setCompanyId(companyId);
		userInfo.setPid(oldParentRecordId);
		userInfo.setDisabled(0);

		Promoter newParent = new Promoter();
		newParent.setId(50L);
		newParent.setUserId(newUserId);
		newParent.setCompanyId(companyId);
		newParent.setPromoterName("ParentName");
		newParent.setPid(null);

		when(promoterMapper.selectOne(any())).thenReturn(userInfo, newParent);
		when(memberAccountService.findMobileStored(eq(companyId), eq(newUserId))).thenReturn("13900000000");
		when(promoterMapper.update(any(), any())).thenReturn(1);

		relRemoveService.relRemove(companyId, userId, newUserId);

		InOrder order = inOrder(upgradePromoterGradeJobDispatchPublisher);
		order.verify(upgradePromoterGradeJobDispatchPublisher).enqueueUpgradePromoterGrade(companyId, newUserId);
		order.verify(upgradePromoterGradeJobDispatchPublisher).enqueueUpgradePromoterGrade(companyId, userId);
		order.verify(upgradePromoterGradeJobDispatchPublisher).enqueueUpgradePromoterGrade(companyId, oldParentRecordId);
	}

	@Test
	void whenMemberRelRemove_thenEnqueueThreeTimesWithExpectedIds() {
		long companyId = 1L;
		long userId = 200L;
		long newUserId = 600L;
		long oldPid = 900L;

		PromoterGradeUpgradeTriggerService triggerService =
				new PromoterGradeUpgradeTriggerService(upgradePromoterGradeJobDispatchPublisher);
		PromoterMemberRelRemoveService memberRelRemoveService =
				new PromoterMemberRelRemoveService(promoterMapper, memberAccountService, triggerService);

		Promoter userInfo = new Promoter();
		userInfo.setUserId(userId);
		userInfo.setCompanyId(companyId);
		userInfo.setPid(oldPid);
		userInfo.setIsPromoter(0);

		Promoter pdata = new Promoter();
		pdata.setId(99L);
		pdata.setUserId(newUserId);
		pdata.setCompanyId(companyId);
		pdata.setIsPromoter(1);
		pdata.setDisabled(0);

		when(promoterMapper.selectOne(any())).thenReturn(userInfo, pdata);
		when(memberAccountService.findMobileStored(eq(companyId), eq(newUserId))).thenReturn("13800000000");
		when(promoterMapper.update(any(), any())).thenReturn(1);

		memberRelRemoveService.updateMemberPromoterRemove(companyId, userId, newUserId);

		InOrder order = inOrder(upgradePromoterGradeJobDispatchPublisher);
		order.verify(upgradePromoterGradeJobDispatchPublisher).enqueueUpgradePromoterGrade(companyId, newUserId);
		order.verify(upgradePromoterGradeJobDispatchPublisher).enqueueUpgradePromoterGrade(companyId, userId);
		order.verify(upgradePromoterGradeJobDispatchPublisher).enqueueUpgradePromoterGrade(companyId, oldPid);
	}
}
