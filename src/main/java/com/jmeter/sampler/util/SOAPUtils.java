package com.jmeter.sampler.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class SOAPUtils {

	private SOAPUtils() {
	}

	public static String attachmentToString(AttachmentPart ap) throws SOAPException, IOException {
		StringBuffer attachmentData = new StringBuffer();
		InputStream is = null;
		Object r = null;
		Object attachmentContent = ap.getContent();
		if(attachmentContent instanceof InputStream) {
			is = (InputStream)attachmentContent;
			r = new InputStreamReader(is);
		} else if(attachmentContent instanceof StreamSource) {
			StreamSource br = (StreamSource)attachmentContent;
			r = br.getReader();
			is = br.getInputStream();
			if(r == null && is != null) {
				r = new InputStreamReader(is);
			}
		} else {
			attachmentData.append(attachmentContent);
		}

		if(r != null) {
			BufferedReader br1 = new BufferedReader((Reader)r);
			String attLine = null;

			while((attLine = br1.readLine()) != null) {
				attachmentData.append(attLine).append("\n");
			}

			br1.close();
		}

		return attachmentData.toString();
	}

	public static String dataHandlerToString(DataHandler dh) throws IOException {
		InputStreamReader isr = new InputStreamReader(dh.getInputStream());
		BufferedReader br = new BufferedReader(isr);
		StringBuffer sb = new StringBuffer();
		String attLine = null;

		while((attLine = br.readLine()) != null) {
			sb.append(attLine).append("\n");
		}

		br.close();
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public static String headersToString(MimeHeaders mimeHeaders) {
		return headersToString(mimeHeaders.getAllHeaders());
	}

	public static String headersToString(Iterator<MimeHeader> headers) {
		StringBuffer sb = new StringBuffer();
		while (headers.hasNext()) {
			MimeHeader header = headers.next();
			sb.append(header.getName());
			sb.append(": ");
			sb.append(header.getValue());
			sb.append("\n");
		}
		return sb.toString();
	}

	public static String soapPartToString(SOAPPart part) throws TransformerException, SOAPException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		Source content = part.getContent();
		StringWriter writer = new StringWriter();
		StreamResult stream = new StreamResult(writer);
		transformer.transform(content, stream);
		return writer.toString();
	}

	public static byte[] soapElementToString(SOAPElement part) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		Source content = new DOMSource(part);
		StringWriter writer = new StringWriter();
		StreamResult stream = new StreamResult(writer);
		transformer.transform(content, stream);
		return writer.toString().getBytes();
	}

}
