package cn.shopex.ecshopx.adapay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.adapay.domain.AdapaySubmitLicense;
import cn.shopex.ecshopx.adapay.mapper.AdapaySubmitLicenseMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAccountServiceTest {

	@Mock
	private AdapaySubmitLicenseMapper adapaySubmitLicenseMapper;

	@Mock
	private AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;

	private OpenAccountService newService() {
		return new OpenAccountService(adapaySubmitLicenseMapper, adapayPaymentSettingRedisReader);
	}

	@Test
	@DisplayName("分支 1：列表为空时不读 payment setting、不抛错")
	void scheduleGetSubmitLicenseStatus_emptyList_exitsQuietly() {
		when(adapaySubmitLicenseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
		newService().scheduleGetSubmitLicenseStatus();
		verify(adapayPaymentSettingRedisReader, never()).getPaymentSetting(ArgumentMatchers.anyLong());
	}

	@Test
	@DisplayName("分支 3-2：非空列表在 getPaymentSetting(首条) 后抛 ResourceException，且无写库")
	void scheduleGetSubmitLicenseStatus_nonEmpty_throwsAfterFirstRowPaymentSetting() {
		AdapaySubmitLicense r1 = new AdapaySubmitLicense();
		r1.setId(1L);
		r1.setCompanyId(100L);
		r1.setSubApiKey("k1");
		r1.setAuditStatus("I");
		AdapaySubmitLicense r2 = new AdapaySubmitLicense();
		r2.setId(2L);
		r2.setCompanyId(200L);
		r2.setSubApiKey("k2");
		r2.setAuditStatus("I");
		when(adapaySubmitLicenseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r1, r2));
		when(adapayPaymentSettingRedisReader.getPaymentSetting(100L)).thenReturn(Map.of("m", 1));
		OpenAccountService svc = newService();
		assertThatThrownBy(svc::scheduleGetSubmitLicenseStatus)
				.isInstanceOf(ResourceException.class)
				.hasMessage("暂不支持开户流程");
		verify(adapayPaymentSettingRedisReader, times(1)).getPaymentSetting(eq(100L));
		verify(adapayPaymentSettingRedisReader, never()).getPaymentSetting(eq(200L));
		verify(adapaySubmitLicenseMapper, never()).updateById(any(AdapaySubmitLicense.class));
	}

	@Test
	@DisplayName("分支 3-2：仅对循环首条 companyId 调用 getPaymentSetting")
	void scheduleGetSubmitLicenseStatus_order_firstCompanyOnly() {
		AdapaySubmitLicense first = new AdapaySubmitLicense();
		first.setId(5L);
		first.setCompanyId(999L);
		first.setAuditStatus("I");
		when(adapaySubmitLicenseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(first));
		when(adapayPaymentSettingRedisReader.getPaymentSetting(999L)).thenReturn(Collections.emptyMap());
		assertThatThrownBy(() -> newService().scheduleGetSubmitLicenseStatus())
				.isInstanceOf(ResourceException.class);
		ArgumentCaptor<Long> cap = ArgumentCaptor.forClass(Long.class);
		verify(adapayPaymentSettingRedisReader, times(1)).getPaymentSetting(cap.capture());
		assertThat(cap.getValue()).isEqualTo(999L);
	}
}
