package cn.shopex.ecshopx.merchant.service.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingSaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantSettlementApplyMemberResolveServiceTest {

	@Mock
	private MerchantSettlementApplyMapper merchantSettlementApplyMapper;

	@Mock
	private MerchantBaseSettingSaveService merchantBaseSettingSaveService;

	@InjectMocks
	private MerchantSettlementApplyMemberResolveService service;

	@Test
	void resolveAuth_reusesExistingApplyByCompanyAndUserId() {
		MerchantSettlementApply existing = new MerchantSettlementApply();
		existing.setId(88L);
		existing.setCompanyId(10L);
		existing.setUserId(200L);
		existing.setAuditStatus("1");
		when(merchantSettlementApplyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

		MerchantWxappAuthAttributes auth = service.resolveAuth(10L, 200L);

		assertEquals(10L, auth.companyId());
		assertEquals(88L, auth.accountId());
		verify(merchantSettlementApplyMapper, never()).insert(any(MerchantSettlementApply.class));
	}

	@Test
	void resolveAuth_reusesApprovedApplyWithoutCreating() {
		MerchantSettlementApply approved = new MerchantSettlementApply();
		approved.setId(9L);
		approved.setCompanyId(10L);
		approved.setUserId(200L);
		approved.setAuditStatus("2");
		when(merchantSettlementApplyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(approved);

		MerchantWxappAuthAttributes auth = service.resolveAuth(10L, 200L);

		assertEquals(9L, auth.accountId());
		verify(merchantSettlementApplyMapper, never()).insert(any(MerchantSettlementApply.class));
		verify(merchantBaseSettingSaveService, never()).loadCompanyBaseSetting(any(Long.class));
	}

	@Test
	void resolveAuth_createsApplyWhenMissingAndPlatformOpen() {
		when(merchantSettlementApplyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		when(merchantBaseSettingSaveService.loadCompanyBaseSetting(10L))
				.thenReturn(Map.of("status", Boolean.TRUE));
		when(merchantSettlementApplyMapper.insert(any(MerchantSettlementApply.class))).thenAnswer(inv -> {
			MerchantSettlementApply row = inv.getArgument(0);
			row.setId(501L);
			return 1;
		});

		MerchantWxappAuthAttributes auth = service.resolveAuth(10L, 200L);

		assertEquals(10L, auth.companyId());
		assertEquals(501L, auth.accountId());

		ArgumentCaptor<MerchantSettlementApply> captor = ArgumentCaptor.forClass(MerchantSettlementApply.class);
		verify(merchantSettlementApplyMapper).insert(captor.capture());
		MerchantSettlementApply created = captor.getValue();
		assertEquals(10L, created.getCompanyId());
		assertEquals(200L, created.getUserId());
		assertEquals("1", created.getAuditStatus());
		assertEquals(true, created.isDisabled());
		assertEquals("", created.getMobile() == null ? "" : created.getMobile());
	}

	@Test
	void resolveAuth_rejectsWhenPlatformClosed() {
		when(merchantSettlementApplyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		when(merchantBaseSettingSaveService.loadCompanyBaseSetting(10L))
				.thenReturn(Map.of("status", Boolean.FALSE));

		assertThrows(ResourceException.class, () -> service.resolveAuth(10L, 200L));
		verify(merchantSettlementApplyMapper, never()).insert(any(MerchantSettlementApply.class));
	}
}
