package plugtests;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.asic.cades.validation.ASiCContainerWithCAdESValidator;
import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.FoundCertificatesProxy;
import eu.europa.esig.dss.diagnostic.OrphanCertificateWrapper;
import eu.europa.esig.dss.diagnostic.RelatedCertificateWrapper;
import eu.europa.esig.dss.diagnostic.RevocationWrapper;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestMatcher;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignatureScope;
import eu.europa.esig.dss.enumerations.CertificateOrigin;
import eu.europa.esig.dss.enumerations.CertificateRefOrigin;
import eu.europa.esig.dss.enumerations.DigestMatcherType;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.SerializableSignatureParameters;
import eu.europa.esig.dss.model.SerializableTimestampParameters;
import eu.europa.esig.dss.spi.x509.revocation.OfflineRevocationSource;
import eu.europa.esig.dss.spi.x509.revocation.RevocationCertificateSource;
import eu.europa.esig.dss.spi.x509.revocation.RevocationToken;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSP;
import eu.europa.esig.dss.test.validation.AbstractDocumentTestValidation;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.ManifestFile;
import eu.europa.esig.dss.validation.SignatureCertificateSource;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;
import eu.europa.esig.validationreport.ValidationReportUtils;
import eu.europa.esig.validationreport.jaxb.SAContactInfoType;
import eu.europa.esig.validationreport.jaxb.SADSSType;
import eu.europa.esig.validationreport.jaxb.SAFilterType;
import eu.europa.esig.validationreport.jaxb.SAMessageDigestType;
import eu.europa.esig.validationreport.jaxb.SANameType;
import eu.europa.esig.validationreport.jaxb.SAReasonType;
import eu.europa.esig.validationreport.jaxb.SASubFilterType;
import eu.europa.esig.validationreport.jaxb.SAVRIType;
import eu.europa.esig.validationreport.jaxb.SignatureIdentifierType;
import eu.europa.esig.validationreport.jaxb.SignatureValidationReportType;
import eu.europa.esig.validationreport.jaxb.SignerInformationType;
import eu.europa.esig.validationreport.jaxb.SignersDocumentType;
import eu.europa.esig.validationreport.jaxb.ValidationReportType;
import eu.europa.esig.validationreport.jaxb.ValidationStatusType;

/**
 * This test is only to ensure that we don't have exception with valid? files
 */
public class SignaturePoolTest extends AbstractDocumentTestValidation<SerializableSignatureParameters, SerializableTimestampParameters>{
	
	private static final Logger LOG = LoggerFactory.getLogger(SignaturePoolTest.class);
	
	private static DSSDocument document;
	
	@BeforeAll
	public static void init() throws Exception {
		// preload JAXB context before validation
		ValidationReportUtils.getInstance().getJAXBContext();
	}

	private static Stream<Arguments> data() throws IOException {

		// -Dsignature.pool.folder=...

		String signaturePoolFolder = System.getProperty("signature.pool.folder", "src/test/resources/signature-pool");
		File folder = new File(signaturePoolFolder);
		Collection<File> listFiles = Utils.listFiles(folder, new String[] { "asice", "asics", "bdoc", "csig", "ddoc",
				"es3", "p7", "p7b", "p7m", "p7s", "pdf", "pkcs7", "xml", "xsig" }, true);
		Collection<Arguments> dataToRun = new ArrayList<>();
		for (File file : listFiles) {
			dataToRun.add(Arguments.of(file));
		}
		return dataToRun.stream();
	}

	@ParameterizedTest(name = "Validation {index} : {0}")
	@MethodSource("data")
	public void testValidate(File fileToTest) {
		LOG.info("Begin : {}", fileToTest.getAbsolutePath());
		document = new FileDocument(fileToTest);
		assertTimeout(ofSeconds(3L), () -> super.validate());
		LOG.info("End : {}", fileToTest.getAbsolutePath());
	}

	@Override
	protected DSSDocument getSignedDocument() {
		return document;
	}
	
	@Override
	public void validate() {
		// do nothing
	}
	
	@Override
	protected void checkAdvancedSignatures(List<AdvancedSignature> signatures) {
		// do nothing
	}
	
	@Override
	protected void checkNumberOfSignatures(DiagnosticData diagnosticData) {
		// skip the test
	}
	
	@Override
	protected void checkBLevelValid(DiagnosticData diagnosticData) {
		for (SignatureWrapper signatureWrapper : diagnosticData.getSignatures()) {
			assertTrue(Utils.isCollectionNotEmpty(signatureWrapper.getDigestMatchers()));
		}
	}
	
	@Override
	protected void checkMessageDigestAlgorithm(DiagnosticData diagnosticData) {
		for (SignatureWrapper signatureWrapper : diagnosticData.getSignatures()) {
			assertTrue(Utils.isCollectionNotEmpty(signatureWrapper.getDigestMatchers()));
			for (XmlDigestMatcher xmlDigestMatcher : signatureWrapper.getDigestMatchers()) {
				if (xmlDigestMatcher.isDataFound()) {
					assertNotNull(xmlDigestMatcher.getDigestMethod());
					assertNotNull(xmlDigestMatcher.getDigestValue());
				}
			}
		}
	}
	
	@Override
	protected void checkSigningCertificateValue(DiagnosticData diagnosticData) {
		for (SignatureWrapper signatureWrapper : diagnosticData.getSignatures()) {
			CertificateWrapper signingCertificate = signatureWrapper.getSigningCertificate();
			if (signingCertificate != null) {
				String signingCertificateId = signingCertificate.getId();
				String certificateDN = diagnosticData.getCertificateDN(signingCertificateId);
				String certificateSerialNumber = diagnosticData.getCertificateSerialNumber(signingCertificateId);
				assertEquals(signingCertificate.getCertificateDN(), certificateDN);
				assertEquals(signingCertificate.getSerialNumber(), certificateSerialNumber);
				assertTrue(Utils.isCollectionNotEmpty(signatureWrapper.getCertificateChain()));
				assertNotNull(signatureWrapper.getDigestAlgorithm());
			}
		}
	}
	
	@Override
	protected void checkSignatureLevel(DiagnosticData diagnosticData) {
		for (SignatureWrapper signatureWrapper : diagnosticData.getSignatures()) {
			assertNotNull(signatureWrapper.getSignatureFormat());
		}
	}
	
	@Override
	protected void checkTimestamps(DiagnosticData diagnosticData) {
		for (TimestampWrapper timestampWrapper : diagnosticData.getTimestampList()) {
			if (timestampWrapper.getSigningCertificate() != null) {
				assertTrue(Utils.isCollectionNotEmpty(timestampWrapper.getCertificateChain()));
				if (timestampWrapper.isSignatureValid()) {
					assertNotNull(timestampWrapper.getDigestAlgorithm());
				}
			}
			assertTrue(Utils.isCollectionNotEmpty(timestampWrapper.getTimestampedObjects()));
		}
	}
	
	@Override
	protected void checkSignatureScopes(DiagnosticData diagnosticData) {
		for (SignatureWrapper signatureWrapper : diagnosticData.getSignatures()) {
			for (XmlSignatureScope signatureScope : signatureWrapper.getSignatureScopes()) {
				assertNotNull(signatureScope.getScope());
				assertNotNull(signatureScope.getSignerData());
				assertNotNull(signatureScope.getSignerData().getDigestAlgoAndValue());
				assertNotNull(signatureScope.getSignerData().getDigestAlgoAndValue().getDigestMethod());
				assertNotNull(signatureScope.getSignerData().getDigestAlgoAndValue().getDigestValue());
			}
		}
	}
	
	@Override
	protected void checkSignatureIdentifier(DiagnosticData diagnosticData) {
		for (SignatureWrapper signatureWrapper : diagnosticData.getSignatures()) {
			assertNotNull(signatureWrapper.getSignatureValue());
		}
	}
	
	@Override
	protected void checkContainerInfo(DiagnosticData diagnosticData) {
		if (diagnosticData.getContainerInfo() != null) {
			assertNotNull(diagnosticData.getContainerType());
		}
	}
	
	@Override
	protected void checkSigningDate(DiagnosticData diagnosticData) {
		// do nothing
	}
	
	@Override
	protected void verifySourcesAndDiagnosticData(List<AdvancedSignature> advancedSignatures, DiagnosticData diagnosticData) {
		for (AdvancedSignature advancedSignature : advancedSignatures) {
			SignatureWrapper signatureWrapper = diagnosticData.getSignatureById(advancedSignature.getId());

			SignatureCertificateSource certificateSource = advancedSignature.getCertificateSource();
			FoundCertificatesProxy foundCertificates = signatureWrapper.foundCertificates();

			// Tokens
			assertEquals(new HashSet<>(certificateSource.getKeyInfoCertificates()).size(),
					foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.KEY_INFO).size() + 
					foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.KEY_INFO).size());
			assertEquals(new HashSet<>(certificateSource.getCertificateValues()).size(),
					foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.CERTIFICATE_VALUES).size() + 
					foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.CERTIFICATE_VALUES).size());
			assertEquals(new HashSet<>(certificateSource.getTimeStampValidationDataCertValues()).size(),
					foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.TIMESTAMP_VALIDATION_DATA).size() + 
					foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.TIMESTAMP_VALIDATION_DATA).size());
			assertEquals(new HashSet<>(certificateSource.getAttrAuthoritiesCertValues()).size(),
					foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.ATTR_AUTORITIES_CERT_VALUES).size() + 
					foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.ATTR_AUTORITIES_CERT_VALUES).size());
			assertEquals(new HashSet<>(certificateSource.getSignedDataCertificates()).size(),
					foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.SIGNED_DATA).size() + 
					foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.SIGNED_DATA).size());
			assertEquals(new HashSet<>(certificateSource.getDSSDictionaryCertValues()).size(),
					foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.DSS_DICTIONARY).size() + 
					foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.DSS_DICTIONARY).size());
			assertEquals(new HashSet<>(certificateSource.getVRIDictionaryCertValues()).size(),
					foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.VRI_DICTIONARY).size() + 
					foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.VRI_DICTIONARY).size());
			assertEquals(0, foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.BASIC_OCSP_RESP).size() + 
					foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.BASIC_OCSP_RESP).size());

			// Refs
			assertEquals(certificateSource.getSigningCertificateRefs().size(),
					getUniqueRelatedCertificateRefsAmount(foundCertificates.getRelatedCertificatesByRefOrigin(CertificateRefOrigin.SIGNING_CERTIFICATE)) + 
					getUniqueOrphanCertificateRefsAmount(foundCertificates.getOrphanCertificatesByRefOrigin(CertificateRefOrigin.SIGNING_CERTIFICATE)) );
			assertEquals(certificateSource.getAttributeCertificateRefs().size(),
					getUniqueRelatedCertificateRefsAmount(foundCertificates.getRelatedCertificatesByRefOrigin(CertificateRefOrigin.ATTRIBUTE_CERTIFICATE_REFS)) + 
					getUniqueOrphanCertificateRefsAmount(foundCertificates.getOrphanCertificatesByRefOrigin(CertificateRefOrigin.ATTRIBUTE_CERTIFICATE_REFS)));
			assertEquals(certificateSource.getCompleteCertificateRefs().size(),
					getUniqueRelatedCertificateRefsAmount(foundCertificates.getRelatedCertificatesByRefOrigin(CertificateRefOrigin.COMPLETE_CERTIFICATE_REFS)) + 
					getUniqueOrphanCertificateRefsAmount(foundCertificates.getOrphanCertificatesByRefOrigin(CertificateRefOrigin.COMPLETE_CERTIFICATE_REFS)));

			List<TimestampToken> timestamps = advancedSignature.getAllTimestamps();
			for (TimestampToken timestampToken : timestamps) {
				TimestampWrapper timestampWrapper = diagnosticData.getTimestampById(timestampToken.getDSSIdAsString());

				certificateSource = timestampToken.getCertificateSource();
				foundCertificates = timestampWrapper.foundCertificates();

				// Tokens
				assertEquals(new HashSet<>(certificateSource.getKeyInfoCertificates()).size(), 
						foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.KEY_INFO).size() + 
						foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.KEY_INFO).size());
				assertEquals(new HashSet<>(certificateSource.getCertificateValues()).size(), 
						foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.CERTIFICATE_VALUES).size() + 
						foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.CERTIFICATE_VALUES).size());
				assertEquals(new HashSet<>(certificateSource.getTimeStampValidationDataCertValues()).size(), 
						foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.TIMESTAMP_VALIDATION_DATA).size() + 
						foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.TIMESTAMP_VALIDATION_DATA).size());
				assertEquals(new HashSet<>(certificateSource.getAttrAuthoritiesCertValues()).size(), 
						foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.ATTR_AUTORITIES_CERT_VALUES).size() + 
						foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.ATTR_AUTORITIES_CERT_VALUES).size());
				assertEquals(new HashSet<>(certificateSource.getSignedDataCertificates()).size(),
						foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.SIGNED_DATA).size() + 
						foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.SIGNED_DATA).size());
				assertEquals(new HashSet<>(certificateSource.getDSSDictionaryCertValues()).size(), 
						foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.DSS_DICTIONARY).size() + 
						foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.DSS_DICTIONARY).size());
				assertEquals(new HashSet<>(certificateSource.getVRIDictionaryCertValues()).size(), 
						foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.VRI_DICTIONARY).size() + 
						foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.VRI_DICTIONARY).size());
				assertEquals(0, foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.BASIC_OCSP_RESP).size() + 
						foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.BASIC_OCSP_RESP).size());

				// Refs
				assertEquals(certificateSource.getSigningCertificateRefs().size(),
						getUniqueRelatedCertificateRefsAmount(foundCertificates.getRelatedCertificatesByRefOrigin(CertificateRefOrigin.SIGNING_CERTIFICATE)) + 
						getUniqueOrphanCertificateRefsAmount(foundCertificates.getOrphanCertificatesByRefOrigin(CertificateRefOrigin.SIGNING_CERTIFICATE)) );
				assertEquals(certificateSource.getAttributeCertificateRefs().size(),
						getUniqueRelatedCertificateRefsAmount(foundCertificates.getRelatedCertificatesByRefOrigin(CertificateRefOrigin.ATTRIBUTE_CERTIFICATE_REFS)) + 
						getUniqueOrphanCertificateRefsAmount(foundCertificates.getOrphanCertificatesByRefOrigin(CertificateRefOrigin.ATTRIBUTE_CERTIFICATE_REFS)));
				assertEquals(certificateSource.getCompleteCertificateRefs().size(),
						getUniqueRelatedCertificateRefsAmount(foundCertificates.getRelatedCertificatesByRefOrigin(CertificateRefOrigin.COMPLETE_CERTIFICATE_REFS)) + 
						getUniqueOrphanCertificateRefsAmount(foundCertificates.getOrphanCertificatesByRefOrigin(CertificateRefOrigin.COMPLETE_CERTIFICATE_REFS)));
			}

			OfflineRevocationSource<OCSP> ocspSource = advancedSignature.getOCSPSource();
			Set<RevocationToken<OCSP>> allRevocationTokens = ocspSource.getAllRevocationTokens();
			for (RevocationToken<OCSP> revocationToken : allRevocationTokens) {
				RevocationCertificateSource revocationCertificateSource = revocationToken.getCertificateSource();
				if (revocationCertificateSource != null) {
					RevocationWrapper revocationWrapper = diagnosticData.getRevocationById(revocationToken.getDSSIdAsString());
					foundCertificates = revocationWrapper.foundCertificates();

					assertEquals(revocationCertificateSource.getCertificates().size(), 
							foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.BASIC_OCSP_RESP).size() + 
							foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.BASIC_OCSP_RESP).size());
					assertEquals(revocationCertificateSource.getAllCertificateRefs().size(), foundCertificates.getRelatedCertificateRefs().size() + 
							foundCertificates.getOrphanCertificateRefs().size());
				}
			}
		}
	}
	
	private int getUniqueRelatedCertificateRefsAmount(List<RelatedCertificateWrapper> certificates) {
		int refCounter = 0;
		List<String> certIds = new ArrayList<>();
		for (RelatedCertificateWrapper certificateWrapper : certificates) {
			if (!certIds.contains(certificateWrapper.getId())) {
				certIds.add(certificateWrapper.getId());
				refCounter += certificateWrapper.getReferences().size();
			}
		}
		return refCounter;
	}
	
	private int getUniqueOrphanCertificateRefsAmount(List<OrphanCertificateWrapper> certificates) {
		int refCounter = 0;
		List<String> certIds = new ArrayList<>();
		for (OrphanCertificateWrapper certificateWrapper : certificates) {
			if (!certIds.contains(certificateWrapper.getId())) {
				certIds.add(certificateWrapper.getId());
				refCounter += certificateWrapper.getReferences().size();
			}
		}
		return refCounter;
	}
	
	@Override
	protected void verifyOriginalDocuments(SignedDocumentValidator validator, DiagnosticData diagnosticData) {
		List<String> signatureIdList = diagnosticData.getSignatureIdList();
		for (String signatureId : signatureIdList) {
			if (diagnosticData.isBLevelTechnicallyValid(signatureId) && isNotInvalidManifest(validator) && signsDocuments(diagnosticData)) {
				List<DSSDocument> retrievedOriginalDocuments = validator.getOriginalDocuments(signatureId);
				assertTrue(Utils.isCollectionNotEmpty(retrievedOriginalDocuments));
			}
		}
	}
	
	private boolean isNotInvalidManifest(SignedDocumentValidator validator) {
		if (validator instanceof ASiCContainerWithCAdESValidator) {
			ASiCContainerWithCAdESValidator asicValidator = (ASiCContainerWithCAdESValidator) validator;
			List<ManifestFile> manifestFiles = asicValidator.getManifestFiles();
			for (ManifestFile manifestFile : manifestFiles) {
				if (Utils.isCollectionEmpty(manifestFile.getEntries())) {
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean signsDocuments(DiagnosticData diagnosticData) {
		for (SignatureWrapper signatureWrapper : diagnosticData.getSignatures()) {
			boolean containsDocumentDigestMatcher = false;
			for (XmlDigestMatcher digestMatcher : signatureWrapper.getDigestMatchers()) {
				DigestMatcherType type = digestMatcher.getType();
				if (!DigestMatcherType.KEY_INFO.equals(type) && !DigestMatcherType.REFERENCE.equals(type) && 
						!DigestMatcherType.SIGNED_PROPERTIES.equals(type)) {
					containsDocumentDigestMatcher = true;
					break;
				}
			}
			if (!containsDocumentDigestMatcher) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected void validateValidationStatus(ValidationStatusType signatureValidationStatus) {
		assertNotNull(signatureValidationStatus);
		assertNotNull(signatureValidationStatus.getMainIndication());
	}
	
	@Override
	protected void validateSignerInformation(SignerInformationType signerInformation) {
		if (signerInformation != null) {
			assertNotNull(signerInformation.getSignerCertificate());
			assertTrue(Utils.isStringNotEmpty(signerInformation.getSigner()));
		}
	}

	@Override
	protected void validateETSISignatureIdentifier(SignatureIdentifierType signatureIdentifier) {
		assertNotNull(signatureIdentifier);
		assertNotNull(signatureIdentifier.getId());
		if (signatureIdentifier.getDigestAlgAndValue() != null) {
			assertNotNull(signatureIdentifier.getDigestAlgAndValue().getDigestMethod());
			assertNotNull(signatureIdentifier.getDigestAlgAndValue().getDigestValue());
		}
		assertNotNull(signatureIdentifier.getSignatureValue());
	}
	
	@Override
	protected void validateETSIMessageDigest(SAMessageDigestType md) {
		if (md != null) {
			assertTrue(Utils.isArrayNotEmpty(md.getDigest()));
		}
	}

	@Override
	protected void validateETSIFilter(SAFilterType filterType) {
		if (filterType != null) {
			assertTrue(Utils.isStringNotBlank(filterType.getFilter()));
		}
	}

	@Override
	protected void validateETSISubFilter(SASubFilterType subFilterType) {
		if (subFilterType != null) {
			assertTrue(Utils.isStringNotBlank(subFilterType.getSubFilterElement()));
		}
	}

	@Override
	protected void validateETSIContactInfo(SAContactInfoType contactTypeInfo) {
		if (contactTypeInfo != null) {
			assertTrue(Utils.isStringNotBlank(contactTypeInfo.getContactInfoElement()));
		}
	}

	@Override
	protected void validateETSISAReasonType(SAReasonType reasonType) {
		if (reasonType != null) {
			assertTrue(Utils.isStringNotBlank(reasonType.getReasonElement()));
		}
	}

	@Override
	protected void validateETSISAName(SANameType nameType) {
		if (nameType != null) {
			assertTrue(Utils.isStringNotBlank(nameType.getNameElement()));
		}
	}

	@Override
	protected void validateETSIDSSType(SADSSType dss) {
		if (dss != null) {
			assertTrue( (dss.getCerts() != null && Utils.isCollectionNotEmpty(dss.getCerts().getVOReference())) || 
					(dss.getCRLs() != null && Utils.isCollectionNotEmpty(dss.getCRLs().getVOReference())) || 
					(dss.getOCSPs() != null && Utils.isCollectionNotEmpty(dss.getOCSPs().getVOReference())) );
		}
	}

	@Override
	protected void validateETSIVRIType(SAVRIType vri) {
		if (vri != null) {
			assertTrue( (vri.getCerts() != null && Utils.isCollectionNotEmpty(vri.getCerts().getVOReference())) || 
					(vri.getCRLs() != null && Utils.isCollectionNotEmpty(vri.getCRLs().getVOReference())) || 
					(vri.getOCSPs() != null && Utils.isCollectionNotEmpty(vri.getOCSPs().getVOReference())) );
		}
	}
	
	@Override
	protected void validateETSISignerDocuments(List<SignersDocumentType> signersDocuments) {
		// do nothing
	}
	
	@Override
	protected void checkReportsSignatureIdentifier(Reports reports) {
		DiagnosticData diagnosticData = reports.getDiagnosticData();
		ValidationReportType etsiValidationReport = reports.getEtsiValidationReportJaxb();
		
		if (Utils.isCollectionNotEmpty(diagnosticData.getSignatures())) {
			for (SignatureValidationReportType signatureValidationReport : etsiValidationReport.getSignatureValidationReport()) {
				SignatureWrapper signature = diagnosticData.getSignatureById(signatureValidationReport.getSignatureIdentifier().getId());
				
				SignatureIdentifierType signatureIdentifier = signatureValidationReport.getSignatureIdentifier();
				assertNotNull(signatureIdentifier);
				
				assertNotNull(signatureIdentifier.getSignatureValue());
				assertTrue(Arrays.equals(signature.getSignatureValue(), signatureIdentifier.getSignatureValue().getValue()));
			}
		}
	}
	
	

}