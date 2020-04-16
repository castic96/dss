package eu.europa.esig.dss.pades.validation.suite.revocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.enumerations.RevocationOrigin;
import eu.europa.esig.dss.enumerations.RevocationType;
import eu.europa.esig.dss.enumerations.TimestampLocation;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.pades.validation.suite.AbstractPAdESTestValidation;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.validationreport.jaxb.SATimestampType;
import eu.europa.esig.validationreport.jaxb.SignatureAttributesType;
import eu.europa.esig.validationreport.jaxb.SignatureIdentifierType;

public class PAdESDssAndVriTest extends AbstractPAdESTestValidation {

	@Override
	protected DSSDocument getSignedDocument() {
		return new InMemoryDocument(getClass().getResourceAsStream("/validation/Signature-P-BG_BOR-2.pdf"));
	}
	
	@Override
	protected void verifySourcesAndDiagnosticData(List<AdvancedSignature> advancedSignatures,
			DiagnosticData diagnosticData) {
		super.verifySourcesAndDiagnosticData(advancedSignatures, diagnosticData);
		
		List<SignatureWrapper> signatures = diagnosticData.getSignatures();
		
		SignatureWrapper signature = signatures.get(0);
		assertEquals(2, signature.foundRevocations().getRelatedRevocationData().size());
		assertEquals(0, signature.foundRevocations().getOrphanRevocationData().size());
		assertEquals(0, signature.foundRevocations().getRelatedRevocationsByType(RevocationType.CRL).size());
		assertEquals(0, signature.foundRevocations().getOrphanRevocationsByType(RevocationType.CRL).size());
		assertEquals(2, signature.foundRevocations().getRelatedRevocationsByType(RevocationType.OCSP).size());
		assertEquals(0, signature.foundRevocations().getOrphanRevocationsByType(RevocationType.OCSP).size());
		assertEquals(0, signature.foundRevocations().getRelatedRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.REVOCATION_VALUES).size() +
				signature.foundRevocations().getOrphanRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.REVOCATION_VALUES).size());
		assertEquals(0, signature.foundRevocations().getRelatedRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.TIMESTAMP_VALIDATION_DATA).size() +
				signature.foundRevocations().getOrphanRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.TIMESTAMP_VALIDATION_DATA).size());
		assertEquals(1, signature.foundRevocations().getRelatedRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.DSS_DICTIONARY).size() +
				signature.foundRevocations().getOrphanRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.DSS_DICTIONARY).size());
		assertEquals(1, signature.foundRevocations().getRelatedRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.VRI_DICTIONARY).size() +
				signature.foundRevocations().getOrphanRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.VRI_DICTIONARY).size());
		assertEquals(1, signature.foundRevocations().getRelatedRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.ADBE_REVOCATION_INFO_ARCHIVAL).size() +
				signature.foundRevocations().getOrphanRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.ADBE_REVOCATION_INFO_ARCHIVAL).size());
		
		List<TimestampWrapper> timestamps = signature.getTimestampList();
		assertNotNull(timestamps);
		assertEquals(2, timestamps.size());
		List<TimestampWrapper> docTimestamps = signature.getTimestampListByLocation(TimestampLocation.DOC_TIMESTAMP);
		assertNotNull(docTimestamps);
		assertEquals(1, docTimestamps.size());
	}
	
	@Override
	protected void checkSigningCertificateValue(DiagnosticData diagnosticData) {
		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		assertFalse(signature.isSigningCertificateIdentified());
	}
	
	@Override
	protected void checkSignatureLevel(DiagnosticData diagnosticData) {
		assertTrue(diagnosticData.isTLevelTechnicallyValid(diagnosticData.getFirstSignatureId()));
		assertFalse(diagnosticData.isALevelTechnicallyValid(diagnosticData.getFirstSignatureId()));
	}
	
	@Override
	protected void validateETSISignatureAttributes(SignatureAttributesType signatureAttributes) {
		// TODO Auto-generated method stub
		super.validateETSISignatureAttributes(signatureAttributes);
		
		assertNotNull(signatureAttributes);
		List<Object> attributesList = signatureAttributes.getSigningTimeOrSigningCertificateOrDataObjectFormat();
		assertTrue(Utils.isCollectionNotEmpty(attributesList));
		List<SATimestampType> foundTimestamps = new ArrayList<>();
		int docTimestampsCounter = 0;
		int sigTimestampsCounter = 0;
		int archiveTimestampsCounter = 0;
		for (Object object : attributesList) {
			JAXBElement<?> element = (JAXBElement<?>) object;
			if (element.getValue() instanceof SATimestampType) {
				SATimestampType saTimestamp = (SATimestampType) element.getValue();
				assertTrue(Utils.isCollectionNotEmpty(saTimestamp.getAttributeObject()));
				assertNotNull(saTimestamp.getTimeStampValue());
				foundTimestamps.add(saTimestamp);
			}
			if (element.getName().getLocalPart().equals("ArchiveTimeStamp")) {
				archiveTimestampsCounter++;
			}
			if (element.getName().getLocalPart().equals("DocTimeStamp")) {
				docTimestampsCounter++;
			}
			if (element.getName().getLocalPart().equals("SignatureTimeStamp")) {
				sigTimestampsCounter++;
			}
		}
		assertEquals(2, foundTimestamps.size());
		assertEquals(1, sigTimestampsCounter);
		assertEquals(1, docTimestampsCounter);
		assertEquals(0, archiveTimestampsCounter);
	}
	
	@Override
	protected void validateETSISignatureIdentifier(SignatureIdentifierType signatureIdentifier) {
		super.validateETSISignatureIdentifier(signatureIdentifier);
		
		assertNotNull(signatureIdentifier);
		assertFalse(signatureIdentifier.isDocHashOnly());
		assertFalse(signatureIdentifier.isHashOnly());
		
		assertNotNull(signatureIdentifier.getSignatureValue());
	}
	
	@Override
	protected void checkPdfRevision(DiagnosticData diagnosticData) {
		super.checkPdfRevision(diagnosticData);
		
		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		assertEquals("ПОДПИСАН ОТ", signature.getReason());
	}

}
