package com.jmeter.protocol.soap.sampler;

import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import javax.activation.DataHandler;
import javax.swing.JOptionPane;
import javax.xml.namespace.QName;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.protocol.http.control.AuthManager;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.util.JMeterStopThreadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jmeter.protocol.soap.control.gui.AttachmentDefinition;
import com.jmeter.sampler.util.SOAPMessages;
import com.jmeter.sampler.util.SOAPUtils;
import com.jmeter.sampler.util.StringDataSource;
import com.jmeter.sampler.util.StringDataSource.StringInputStream;

public class MtomSOAPSampler extends AbstractSampler {

	private static final long serialVersionUID = -2412344727364753799L;

	private static final Logger log = LoggerFactory.getLogger(MtomSOAPSampler.class);

	public static final String XML_DATA = "HTTPSamper.xml_data";
	public static final String URL_DATA = "SoapSampler.URL_DATA";
	public static final String MAIN_CONTENT_ID = "SoapSampler.MAIN_CONTENT_ID";
	private static final String ATTACHMENT_COUNT = "SOAPAttachmentCount";
	private static final String ATTACHMENT_DATA = "SOAPAttachmentData";
	private static final String ATTACHMENT_AS_RESPONSE = "SOAPAttachmentAsResponse";
	private static final String ATTACHMENT_RESPONSE_MODE = "SOAPAttachmentAsResponseMode";
	private static final String ATTACHMENT_RESPONSE_CT = "SOAPAttachmentAsResponseCT";
	private static final String ATTACHMENT_RESPONSE_CID = "SOAPAttachmentAsResponseCID";
	private static final String USE_RELATIVE_PATHS = "SOAPUseRelativePaths";
	private static final String UPDATE_ATTACHMENT_REFS = "SOAPUpdateAttachmentReferences";
	private static final String PROTOCOL_VERSION = "SoapProtocolVersion";
	public static final int ATTACHMENT_AS_RESPONSE_CONTENTTYPE = 0;
	public static final int ATTACHMENT_AS_RESPONSE_CONTENTID = 1;
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String MTOM_CONTENT_TYPE = "application/xop+xml";

	private transient AuthManager authManager;
	private transient CookieManager cookieManager;
	private transient HeaderManager headerManager;

	public void setXmlData(String data) {
		this.setProperty(XML_DATA, data);
	}

	public String getXmlData() {
		return this.getPropertyAsString(XML_DATA);
	}

	public void setAttachmentAsResponseMode(int mode) {
		this.setProperty(ATTACHMENT_RESPONSE_MODE, "" + mode);
	}

	public int getAttachmentAsResponseMode() {
		return this.getPropertyAsInt(ATTACHMENT_RESPONSE_MODE);
	}

	public void setAttachmentAsResponseContentType(String contentType) {
		this.setProperty(ATTACHMENT_RESPONSE_CT, contentType);
	}

	public String getAttachmentAsResponseContentType() {
		return this.getPropertyAsString(ATTACHMENT_RESPONSE_CT);
	}

	public void setAttachmentAsResponseContentID(String contentId) {
		this.setProperty(ATTACHMENT_RESPONSE_CID, contentId);
	}

	public String getAttachmentAsResponseContentID() {
		return this.getPropertyAsString(ATTACHMENT_RESPONSE_CID);
	}

	AttachmentDefinition getAttachmentDefinition() {
		return new AttachmentDefinition();
	}

	public void setAttachments(ArrayList<AttachmentDefinition> attachments) {
		this.setProperty(ATTACHMENT_COUNT, "" + attachments.size());

		for(int i = 0; i < attachments.size(); ++i) {
			AttachmentDefinition atd = attachments.get(i);
			String baseName = ATTACHMENT_DATA + "_" + i + "_";
			this.setProperty(baseName + "attachment", atd.attachment);
			this.setProperty(baseName + "contentid", atd.contentID);
			this.setProperty(baseName + "contenttype", atd.contentType);
			this.setProperty(baseName + "type", "" + atd.type);
		}

	}

	public ArrayList<AttachmentDefinition> getAttachments() {
		ArrayList<AttachmentDefinition> attachments = new ArrayList<>();
		int count = this.getPropertyAsInt(ATTACHMENT_COUNT);

		for(int i = 0; i < count; ++i) {
			AttachmentDefinition atd = new AttachmentDefinition();
			String baseName = ATTACHMENT_DATA + "_" + i + "_";
			atd.attachment = this.getPropertyAsString(baseName + "attachment");
			atd.contentID = this.getPropertyAsString(baseName + "contentid");
			atd.contentType = this.getPropertyAsString(baseName + "contenttype");
			atd.type = this.getPropertyAsInt(baseName + "type");
			attachments.add(atd);
		}

		return attachments;
	}

	public String getURLData() {
		return this.getPropertyAsString(URL_DATA);
	}

	public void setURLData(String url) {
		this.setProperty(URL_DATA, url);
	}

	public String getMainContentId() {
		return this.getPropertyAsString(MAIN_CONTENT_ID);
	}

	public void setMainContentId(String contentId) {
		this.setProperty(MAIN_CONTENT_ID, contentId);
	}

	public void setSoapProtocolVersion(String protocolValue) {
		this.setProperty(PROTOCOL_VERSION, protocolValue);
	}

	public String getSoapProtocolVersion() {
		return this.getPropertyAsString(PROTOCOL_VERSION);
	}

	public void setTreatAttachmentAsResponse(boolean treat) {
		this.setProperty(ATTACHMENT_AS_RESPONSE, String.valueOf(treat));
	}

	public boolean getTreatAttachmentAsResponse() {
		return this.getPropertyAsBoolean(ATTACHMENT_AS_RESPONSE);
	}

	public void setUseRelativePaths(boolean use) {
		this.setProperty(USE_RELATIVE_PATHS, use, true);
	}

	public boolean getUseRelativePaths() {
		return this.getPropertyAsBoolean(USE_RELATIVE_PATHS, true);
	}

	public void setUpdateAttachmentReferences(boolean use) {
		this.setProperty(UPDATE_ATTACHMENT_REFS, use, true);
	}

	public boolean getUpdateAttachmentReferences() {
		return this.getPropertyAsBoolean(UPDATE_ATTACHMENT_REFS, true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public SampleResult sample(Entry entry) {
		String url = this.getURLData();
		String name = this.getName();

		SOAPSampleResult result = new SOAPSampleResult();
		result.setSampleLabel(name);
		result.sampleStart();

		try {
			MessageFactory ecx = null;
			if (getSoapProtocolVersion().equals("1_2")) {
				ecx = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
			} else if (getSoapProtocolVersion().equals("1_1")) {
				ecx = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
			} else {
				System.out.println("ERROR: Not allowed value in soap protocol");
			}

			MimeHeaders headers = new MimeHeaders();
			headers.addHeader(CONTENT_TYPE, MTOM_CONTENT_TYPE);
			SOAPMessage message = ecx.createMessage(headers, new StringInputStream(this.getXmlData()));
			SOAPPart soapPart = message.getSOAPPart();

			String mainContentId = getMainContentId();
			if(StringUtils.isNotEmpty(mainContentId)){
				soapPart.setContentId(mainContentId);
				String[] mimeHeader = soapPart.getMimeHeader(CONTENT_TYPE);
				ContentType contentType = ContentType.parse(mimeHeader[0]);
				contentType = contentType.withParameters(new BasicNameValuePair("start", mainContentId));
				soapPart.setMimeHeader(CONTENT_TYPE, contentType.toString());
			}

			ArrayList<AttachmentDefinition> attachments = this.getAttachments();
			Iterator<AttachmentDefinition> it = attachments.iterator();

			while(true) {
				while(it.hasNext()) {
					AttachmentDefinition attachmentDefinition = it.next();
					AttachmentPart fullRequest = null;
					String baseDir;
					if(attachmentDefinition.type == AttachmentDefinition.TYPE_RESOURCE) {
						String attachmentLocation = attachmentDefinition.attachment;
						if(attachmentLocation.indexOf("://") < 0) {
							File attachmentFile = new File(attachmentLocation);
							if(!attachmentFile.exists()) {
								baseDir = FileServer.getFileServer().getBaseDir();
								if(!baseDir.endsWith(File.separator)) {
									baseDir = baseDir + File.separator;
								}

								attachmentLocation = baseDir + attachmentLocation;
								attachmentFile = new File(attachmentLocation);
							}

							if(!attachmentFile.exists()) {
								log.warn("Ignoring invalid attachment: " + attachmentDefinition.attachment);
								continue;
							}

							attachmentLocation = "file:///" + attachmentFile.getAbsolutePath();
						}

						URL attachmentLocationUrl = new URL(attachmentLocation);
						DataHandler dataHandler = new DataHandler(attachmentLocationUrl);
						String attachmentContentType = attachmentDefinition.contentType.equals("auto") ? dataHandler.getContentType() : attachmentDefinition.contentType;
						if(!attachmentContentType.startsWith("text/") && !attachmentContentType.endsWith("/xml")) {
							fullRequest = message.createAttachmentPart(dataHandler);
							fullRequest.setContentId("<" + attachmentDefinition.contentID + ">");
							fullRequest.setMimeHeader("Content-Transfer-Encoding", "binary");
							fullRequest.setMimeHeader("Content-Disposition", "attachment");
							if(!attachmentDefinition.contentType.equals("auto")) {
								fullRequest.setContentType(attachmentDefinition.contentType);
							}
						} else {
							String respSoapPart = SOAPUtils.dataHandlerToString(dataHandler);
							CompoundVariable transformerFactory = new CompoundVariable(respSoapPart);
							String transformer = transformerFactory.execute();
							StringDataSource sourceContent = new StringDataSource(transformer, attachmentContentType);
							DataHandler sw = new DataHandler(sourceContent);
							fullRequest = message.createAttachmentPart(sw);
							fullRequest.setContentId("<" + attachmentDefinition.contentID + ">");
						}
					} else {
						CompoundVariable variable = new CompoundVariable(attachmentDefinition.attachment);
						String value = variable.execute();
						baseDir = null;
						StringDataSource dataSource;
						if(attachmentDefinition.contentType.equals("auto")) {
							dataSource = new StringDataSource(value);
						} else {
							dataSource = new StringDataSource(value, attachmentDefinition.contentType);
						}

						DataHandler dataHandler = new DataHandler(dataSource);
						fullRequest = message.createAttachmentPart(dataHandler);
						fullRequest.setContentId(attachmentDefinition.contentID);
					}

					message.addAttachmentPart(fullRequest);
				}

				if(this.getUpdateAttachmentReferences()) {
					this.updateAttachmentReferences(message);
				}

				URL endpoint = new URL(url);
				addAdditionalHeaders(message, endpoint);

				if(StringUtils.isNotEmpty(mainContentId)) {
					if(message.saveRequired()) {
						message.saveChanges();
					}
					String[] mimeHeader = message.getMimeHeaders().getHeader(CONTENT_TYPE);
					ContentType contentType = ContentType.parse(mimeHeader[0]);
					contentType = contentType.withParameters(new BasicNameValuePair("start", mainContentId));
					message.getMimeHeaders().setHeader(CONTENT_TYPE, contentType.toString());
				}

				if(log.isDebugEnabled()) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					message.writeTo(baos);
					baos.close();
					log.debug("Full request: \n" + baos.toString());
				}

				result.setRequestHeaders(SOAPUtils.headersToString(message.getMimeHeaders()));
				result.setSamplerData(SOAPUtils.soapPartToString(message.getSOAPPart()));
				SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
				SOAPConnection soapConnection = soapConnectionFactory.createConnection();
				SOAPMessage response = soapConnection.call(message, endpoint);
				result.sampleEnd();

				if(log.isDebugEnabled()) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					response.writeTo(baos);
					log.debug("Full response: \n" + baos.toString());
				}

				result.setSOAPEnvelope(SOAPUtils.soapPartToString(response.getSOAPPart()));
				String responseHeaders = SOAPUtils.headersToString(response.getMimeHeaders());
				result.setResponseHeaders(responseHeaders);
				result.setHeadersSize(responseHeaders.length());
				SOAPBody respBody = response.getSOAPBody();
				result.setResponseData(SOAPUtils.soapElementToString(respBody));
				result.setDataType("text");
				if(!respBody.hasFault()) {
					result.setResponseCodeOK();
					result.setResponseMessageOK();
					result.setSuccessful(true);
				} else {
					SOAPFault soapFault = respBody.getFault();
					result.setResponseCode(soapFault.getFaultCode() + " @ " + soapFault.getFaultActor());
					result.setResponseMessage(soapFault.getFaultString());
					result.setSuccessful(false);
					return result;
				}

				boolean attachmentFound = !this.getTreatAttachmentAsResponse();
				int attachmentMode = this.getAttachmentAsResponseMode();
				String attExpectedContentType = this.getAttachmentAsResponseContentType();
				String attExpectedContentID = this.getAttachmentAsResponseContentID();
				Object rootResult = null;
				if(this.getTreatAttachmentAsResponse()) {
					rootResult = new SampleResult();
					((SampleResult)rootResult).sampleStart();
					((SampleResult)rootResult).setContentType("text/xml");
					((SampleResult)rootResult).setResponseCodeOK();
					((SampleResult)rootResult).setResponseMessageOK();
					((SampleResult)rootResult).setSampleLabel(SOAPMessages.getResString("soap_complete_soap_message"));
					((SampleResult)rootResult).setResponseData(SOAPUtils.soapElementToString(respBody));
					((SampleResult)rootResult).setDataType("text");
					((SampleResult)rootResult).setSuccessful(true);
					((SampleResult)rootResult).sampleEnd();
					result.addSubResult((SampleResult)rootResult);
				} else {
					rootResult = result;
				}

				Iterator<AttachmentPart> attIt = response.getAttachments();

				while(attIt.hasNext()) {
					AttachmentPart ap = attIt.next();
					result.addAttachment(ap);

					int attSize = ap.getSize();
					//DataHandler dh = ap.getDataHandler();
					//InputStream is = (InputStream) ap.getContent();
					//copyInputStreamToFile(is, new File("c:/Users/userX/temp/" + ap.getContentId().replace(">", "").replace("<", "")));
					String attachmentContentType = ap.getContentType();
					String textRepresentation = null;
					boolean isBinary;
					if(!attachmentContentType.startsWith("text/") && !attachmentContentType.endsWith("/xml")) {
						isBinary = true;
						textRepresentation = "(binary data, size in bytes: " + attSize + ")";
					} else {
						isBinary = false;
						textRepresentation = SOAPUtils.attachmentToString(ap);
					}

					SampleResult subResult = new SampleResult();
					subResult.setContentType(attachmentContentType);
					subResult.setResponseCodeOK();
					subResult.setResponseMessageOK();
					subResult.setSampleLabel(ap.getContentId() + " (" + ap.getContentType() + ")");
					if (isBinary) {
						subResult.setResponseData(ap.getRawContentBytes());
						subResult.setDataType(SampleResult.BINARY);
					} else {
						subResult.setResponseData(textRepresentation, null);
						subResult.setDataType(SampleResult.TEXT);
					}
					subResult.setSuccessful(true);
					responseHeaders = SOAPUtils.headersToString(ap.getAllMimeHeaders());
					subResult.setResponseHeaders(responseHeaders);
					subResult.setHeadersSize(responseHeaders.length());
					((SampleResult)rootResult).addSubResult(subResult);
					if(!attachmentFound) {
						boolean attachmentMatched = false;
						switch(attachmentMode) {
						case 0:
							attachmentMatched = attExpectedContentType.equals(ap.getContentType());
							break;
						case 1:
							attachmentMatched = attExpectedContentID.equals(ap.getContentId());
						}

						if(attachmentMatched) {
							result.setResponseData(textRepresentation.getBytes());
							attachmentFound = true;
						}
					}
				}

				return result;
			}
		} catch (Exception e) {
			result.sampleEnd();
			result.setResponseCode(e.getClass().getName());
			result.setResponseMessage(e.getMessage());
			result.setSuccessful(false);
			log.error("Exception in SOAP communication", e);
			return result;
		}
	}

	@SuppressWarnings("unchecked")
	private void updateAttachmentReferences(SOAPMessage message) {
		TreeSet<String> missingAttachments = new TreeSet<>();
		String ebUri = "http://www.ebxml.org/namespaces/messageHeader";
		String xlinkUri = "http://www.w3.org/1999/xlink";
		String soapUri = "http://schemas.xmlsoap.org/soap/envelope/";
		String ebManifest = "Manifest";
		String ebReference = "Reference";
		String xlinkHref = "href";
		String xlinkType = "type";
		String typeVal = "simple";
		String soapMustUnderstand = "mustUnderstand";
		String ebVersion = "version";

		AttachmentPart soapExc;
		for(Iterator<AttachmentPart> apIt = message.getAttachments(); apIt.hasNext(); missingAttachments.add("cid:" + soapExc.getContentId())) {
			soapExc = apIt.next();
			if(soapExc.getContentId().length() == 0) {
				soapExc.setContentId("attachment_" + soapExc.hashCode());
			}
		}

		try {
			ArrayList<SOAPElement> soapExc1 = new ArrayList<>();
			SOAPPart sp = message.getSOAPPart();
			SOAPEnvelope se = sp.getEnvelope();
			Iterator<String> prefIt = se.getNamespacePrefixes();
			String ebPrefix = "eb";
			String xlinkPrefix = "xlink";
			String soapPrefix = se.getPrefix();

			while(prefIt.hasNext()) {
				String manifestElem = prefIt.next();
				String body = se.getNamespaceURI(manifestElem);
				if(body.equals(ebUri)) {
					ebPrefix = manifestElem;
				} else if(body.equals(xlinkUri)) {
					xlinkPrefix = manifestElem;
				}
			}

			SOAPElement manifestElem1 = null;
			SOAPBody body1 = se.getBody();
			QName addIt;
			Iterator<SOAPElement> elem;
			if(body1 != null) {
				addIt = new QName(ebUri, ebManifest);
				elem = body1.getChildElements(addIt);
				if(elem.hasNext()) {
					manifestElem1 = elem.next();
					QName contentId = new QName(ebUri, ebReference);
					Iterator<SOAPElement> xlinkHrefName = manifestElem1.getChildElements(contentId);

					while(xlinkHrefName.hasNext()) {
						SOAPElement xlinkTypeName = xlinkHrefName.next();
						String sel = xlinkTypeName.getAttributeNS(xlinkUri, xlinkHref);
						if(!missingAttachments.contains(sel)) {
							soapExc1.add(xlinkTypeName);
						} else {
							missingAttachments.remove(sel);
						}
					}

					Iterator<SOAPElement> xlinkTypeName1 = soapExc1.iterator();

					while(xlinkTypeName1.hasNext()) {
						SOAPElement sel1 = xlinkTypeName1.next();
						sel1.detachNode();
					}
				}
			}

			if(missingAttachments.size() != 0) {
				if(body1 == null) {
					body1 = se.addBody();
				}

				if(manifestElem1 == null) {
					manifestElem1 = body1.addChildElement(ebManifest, ebPrefix, ebUri);
					addIt = new QName(soapUri, soapMustUnderstand, soapPrefix);
					QName elem1 = new QName(ebUri, ebVersion, ebPrefix);
					manifestElem1.addAttribute(addIt, "1");
					manifestElem1.addAttribute(elem1, "1.0");
				}

				Iterator<String> addIt1 = missingAttachments.iterator();

				while(addIt1.hasNext()) {
					elem = null;
					String contentId1 = addIt1.next();
					SOAPElement elem2 = manifestElem1.addChildElement(ebReference, ebPrefix, ebUri);
					QName xlinkHrefName1 = new QName(xlinkUri, xlinkHref, xlinkPrefix);
					elem2.addAttribute(xlinkHrefName1, contentId1);
					QName xlinkTypeName2 = new QName(xlinkUri, xlinkType, xlinkPrefix);
					elem2.addAttribute(xlinkTypeName2, typeVal);
				}

			}
		} catch (SOAPException e) {
			log.error("Caught exception while updating attachments", e);
			JOptionPane.showMessageDialog((Component)null, "Unable to update attachment references, see log file for details", "Error", 0);
			throw new JMeterStopThreadException("Unable to update attachment references");
		}
	}

	protected void addAdditionalHeaders(SOAPMessage message, URL endpoint) {
		MimeHeaders headers = message.getMimeHeaders();
		if (headers == null) {
			log.error("This should never happen: Mime Headers undefined!");
			return;
		}
		if (authManager != null) {
			String authHeader = authManager.getAuthHeaderForURL(endpoint);
			if (authHeader != null && authHeader.trim().length() != 0) {
				log.debug("Add auth header "+authHeader);
				headers.setHeader("Authorization", authHeader);
			}
		}
		if (cookieManager != null) {
			String cookieHeader = cookieManager.getCookieHeaderForURL(endpoint);
			if (cookieHeader != null && cookieHeader.trim().length() != 0) {
				log.debug("Add cookies "+cookieHeader);
				headers.setHeader("Cookie", cookieHeader);
			}
		}
		if (headerManager != null) {
			for (int i = 0; i < headerManager.size(); i++) {
				Header header = headerManager.get(i);
				if (header != null && header.getName().trim().length() != 0) {
					log.debug("Add header "+header);
					headers.setHeader(header.getName(), header.getValue());
				}
			}
		}
	}

	@Override
	public void addTestElement(TestElement el) {
		if (el instanceof AuthManager) {
			authManager = (AuthManager)el;
		} else if (el instanceof CookieManager) {
			cookieManager = (CookieManager)el;
		} else if (el instanceof HeaderManager) {
			headerManager = (HeaderManager)el;
		} else {
			super.addTestElement(el);
		}
	}
}
