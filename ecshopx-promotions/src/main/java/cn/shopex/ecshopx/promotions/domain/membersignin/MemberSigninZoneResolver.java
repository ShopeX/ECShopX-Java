package cn.shopex.ecshopx.promotions.domain.membersignin;

import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 会员签到「业务日」时区：与部署环境一致，禁止在业务代码中写死 {@code Asia/Shanghai}。
 *
 * <p>未配置 {@code ecshopx.business.zone-id} 时使用 {@link ZoneId#systemDefault()}（JVM / 容器 TZ）。
 */
@Component
public class MemberSigninZoneResolver {

	private final ZoneId defaultZone;

	@Autowired
	public MemberSigninZoneResolver(@Value("${ecshopx.business.zone-id:}") String zoneIdProperty) {
		this(resolveConfiguredZone(zoneIdProperty));
	}

	MemberSigninZoneResolver(ZoneId defaultZone) {
		this.defaultZone = defaultZone == null ? ZoneId.systemDefault() : defaultZone;
	}

	public ZoneId resolveZone(long companyId) {
		// 公司级时区后续可接入；一期与全局业务日配置一致
		return defaultZone;
	}

	private static ZoneId resolveConfiguredZone(String zoneIdProperty) {
		if (!StringUtils.hasText(zoneIdProperty)) {
			return ZoneId.systemDefault();
		}
		try {
			return ZoneId.of(zoneIdProperty.trim());
		} catch (Exception ex) {
			return ZoneId.systemDefault();
		}
	}
}
