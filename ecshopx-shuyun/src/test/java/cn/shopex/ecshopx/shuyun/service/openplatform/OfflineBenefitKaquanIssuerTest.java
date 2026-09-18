package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.shuyun.ShuyunOfflineBenefitCouponGrantPort;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefit;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendBatch;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendItem;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfflineBenefitKaquanIssuerTest {

	@Mock private ShuyunOfflineBenefitMapper benefitMapper;
	@Mock private ShuyunOfflineBenefitCouponGrantPort couponGrantPort;

	private OfflineBenefitKaquanIssuer issuer;

	@BeforeEach
	void setUp() {
		ShuyunOpenPlatformProperties props = new ShuyunOpenPlatformProperties();
		OfflineBenefitIssuingMemberResolver resolver = new OfflineBenefitIssuingMemberResolver(props);
		issuer = new OfflineBenefitKaquanIssuer(benefitMapper, couponGrantPort, resolver);
	}

	@Test
	void issueOkSetsCodeAndMember() {
		ShuyunOfflineBenefit benefit = new ShuyunOfflineBenefit();
		benefit.setCompanyId(1L);
		benefit.setBenefitId("b1");
		benefit.setLocalCardId(99L);
		when(benefitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(benefit);
		when(couponGrantPort.grantByCardTemplate(1L, 99L, 100L, OfflineBenefitKaquanIssuer.SOURCE_FROM))
				.thenReturn(Map.of("code", "COUPON-XYZ"));

		ShuyunOfflineBenefitSendBatch batch = new ShuyunOfflineBenefitSendBatch();
		batch.setCompanyId(1L);
		batch.setBenefitId("b1");
		ShuyunOfflineBenefitSendItem item = new ShuyunOfflineBenefitSendItem();
		item.setCustomerId("100");

		OfflineBenefitIssueResult r = issuer.issue(batch, item);
		assertTrue(r.isSuccess());
		assertEquals("COUPON-XYZ", r.getBenefitCode());
		assertEquals(100L, r.getMemberUserId());
	}

	@Test
	void issueFailsWhenMemberNotResolved() {
		ShuyunOfflineBenefit benefit = new ShuyunOfflineBenefit();
		benefit.setCompanyId(1L);
		benefit.setBenefitId("b1");
		benefit.setLocalCardId(1L);
		when(benefitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(benefit);

		ShuyunOfflineBenefitSendBatch batch = new ShuyunOfflineBenefitSendBatch();
		batch.setCompanyId(1L);
		batch.setBenefitId("b1");
		ShuyunOfflineBenefitSendItem item = new ShuyunOfflineBenefitSendItem();
		item.setCustomerId("not-numeric");

		OfflineBenefitIssueResult r = issuer.issue(batch, item);
		assertFalse(r.isSuccess());
		assertTrue(r.getFailReason().contains("未找到会员"));
		verify(couponGrantPort, never()).grantByCardTemplate(anyLong(), anyLong(), anyLong(), anyString());
	}

	@Test
	void issueFailsWhenLocalCardIdMissing() {
		ShuyunOfflineBenefit benefit = new ShuyunOfflineBenefit();
		benefit.setCompanyId(1L);
		benefit.setBenefitId("b1");
		benefit.setLocalCardId(null);
		when(benefitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(benefit);

		ShuyunOfflineBenefitSendBatch batch = new ShuyunOfflineBenefitSendBatch();
		batch.setCompanyId(1L);
		batch.setBenefitId("b1");
		ShuyunOfflineBenefitSendItem item = new ShuyunOfflineBenefitSendItem();
		item.setCustomerId("1");

		OfflineBenefitIssueResult r = issuer.issue(batch, item);
		assertFalse(r.isSuccess());
		assertTrue(r.getFailReason().contains("local_card_id"));
		verify(couponGrantPort, never())
				.grantByCardTemplate(eq(1L), anyLong(), anyLong(), anyString());
	}
}
