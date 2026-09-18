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

package cn.shopex.ecshopx.hfpay.service.payment;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfPayCfcaKernelService {

	private final ObjectMapper objectMapper;

	@Value("${ecshopx.hfpay.cfcalog-config:}")
	private String cfcaLogConfigPath;

	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	public HfPayCfcaKernelService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String signAttachedPkcs7Base64(String jsonUtf8Payload, String pfxPath, String pfxPassword) {
		ensureCfcaConfigIfSet();
		try {
			KeyStore ks = KeyStore.getInstance("PKCS12");
			try (InputStream in = Files.newInputStream(Path.of(pfxPath))) {
				ks.load(in, pfxPassword.toCharArray());
			}
			String alias = ks.aliases().hasMoreElements() ? ks.aliases().nextElement() : null;
			if (alias == null) {
				throw new ResourceException("签名错误");
			}
			PrivateKey privateKey = (PrivateKey) ks.getKey(alias, pfxPassword.toCharArray());
			Certificate[] chain = ks.getCertificateChain(alias);
			if (chain == null || chain.length == 0) {
				throw new ResourceException("签名错误");
			}
			X509Certificate cert = (X509Certificate) chain[0];
			List<X509Certificate> certList = new ArrayList<>();
			for (Certificate c : chain) {
				certList.add((X509Certificate) c);
			}
			JcaCertStore certStore = new JcaCertStore(certList);

			CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
			ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(privateKey);
			DigestCalculatorProvider digProv = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
			JcaSignerInfoGeneratorBuilder signerInfoGenBuilder = new JcaSignerInfoGeneratorBuilder(digProv);
			gen.addSignerInfoGenerator(signerInfoGenBuilder.build(contentSigner, cert));
			gen.addCertificates(certStore);

			byte[] payload = jsonUtf8Payload.getBytes(StandardCharsets.UTF_8);
			CMSTypedData msg = new CMSProcessableByteArray(payload);
			CMSSignedData signedData = gen.generate(msg, true);
			return Base64.getEncoder().encodeToString(signedData.getEncoded());
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("签名错误");
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> decryptResponseCheckValue(String checkValueBase64, String caCerPath, String oca31CerPath) {
		ensureCfcaConfigIfSet();
		try {
			byte[] cmsBytes = Base64.getDecoder().decode(checkValueBase64.replaceAll("\\s", ""));
			CMSSignedData cms = new CMSSignedData(cmsBytes);
			SignerInformationStore sis = cms.getSignerInfos();
			if (sis == null || sis.getSigners().isEmpty()) {
				throw new ResourceException("解密错误");
			}
			SignerInformation signerInfo = sis.getSigners().iterator().next();
			Store<X509CertificateHolder> certStoreBc = cms.getCertificates();
			Collection<X509CertificateHolder> matches = certStoreBc.getMatches(signerInfo.getSID());
			if (matches == null || matches.isEmpty()) {
				throw new ResourceException("解密错误");
			}
			X509CertificateHolder signerHolder = matches.iterator().next();
			if (!signerInfo.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(signerHolder))) {
				throw new ResourceException("解密错误");
			}
			JcaX509CertificateConverter conv = new JcaX509CertificateConverter().setProvider("BC");
			X509Certificate signerCert = conv.getCertificate(signerHolder);
			if (!verifyPkixToTrustedRoots(signerCert, cms, caCerPath, oca31CerPath)) {
				throw new ResourceException("解密错误");
			}
			CMSProcessable sc = cms.getSignedContent();
			if (sc == null) {
				throw new ResourceException("解密错误");
			}
			byte[] plain = (byte[]) sc.getContent();
			return objectMapper.readValue(plain, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("解密错误");
		}
	}

	private void ensureCfcaConfigIfSet() {
		if (!StringUtils.hasText(cfcaLogConfigPath)) {
			return;
		}
		Path p = Path.of(cfcaLogConfigPath);
		if (!Files.isRegularFile(p)) {
			throw new ResourceException("签名错误");
		}
	}

	private boolean verifyPkixToTrustedRoots(X509Certificate signerCert, CMSSignedData cms, String caCerPath, String ocaCerPath) {
		try {
			Set<TrustAnchor> anchors = new HashSet<>();
			anchors.add(new TrustAnchor(loadX509(caCerPath), null));
			anchors.add(new TrustAnchor(loadX509(ocaCerPath), null));

			X509CertSelector selector = new X509CertSelector();
			selector.setCertificate(signerCert);

			PKIXBuilderParameters params = new PKIXBuilderParameters(anchors, selector);
			params.setRevocationEnabled(false);

			List<Certificate> certs = new ArrayList<>();
			certs.add(signerCert);
			Store<X509CertificateHolder> st = cms.getCertificates();
			Selector<X509CertificateHolder> all = new Selector<>() {
				@Override
				public boolean match(X509CertificateHolder obj) {
					return true;
				}

				@Override
				public Object clone() {
					return this;
				}
			};
			JcaX509CertificateConverter conv = new JcaX509CertificateConverter().setProvider("BC");
			for (X509CertificateHolder h : (Collection<X509CertificateHolder>) st.getMatches(all)) {
				X509Certificate c = conv.getCertificate(h);
				if (!certs.contains(c)) {
					certs.add(c);
				}
			}

			CertStore collection = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certs));
			params.addCertStore(collection);

			CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
			PKIXCertPathBuilderResult r = (PKIXCertPathBuilderResult) cpb.build(params);
			return r != null && r.getCertPath() != null;
		} catch (Exception e) {
			return false;
		}
	}

	private static X509Certificate loadX509(String path) throws Exception {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		try (InputStream in = Files.newInputStream(Path.of(path))) {
			return (X509Certificate) cf.generateCertificate(in);
		}
	}
}
