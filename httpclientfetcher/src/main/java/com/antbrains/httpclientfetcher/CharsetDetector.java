package com.antbrains.httpclientfetcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CharsetDetector {
	private static class NotifyObject {
		boolean found = false;
		String charset;

		public String getCharset() {
			return charset;
		}

		public void setCharset(String charset) {
			this.charset = charset;
		}

		public boolean isFound() {
			return found;
		}

		public void setFound(boolean found) {
			this.found = found;
		}

	}

	private static String getCharset(InputStream in) {
		nsDetector det = new nsDetector(3);
		final NotifyObject target = new NotifyObject();
		nsICharsetDetectionObserver cdo = new nsICharsetDetectionObserver() {
			public void Notify(String charset) {
				target.setFound(true);
				target.setCharset(charset);
			}
		};

		det.Init(cdo);

		int len;
		byte[] buf = new byte[1024];
		boolean done = false;
		boolean isAscii = true;
		try {
			while ((len = in.read(buf, 0, buf.length)) != -1) {
				if (isAscii)
					isAscii = det.isAscii(buf, len);
				if (!isAscii && !done) {
					done = det.DoIt(buf, len, false);
				}
				if (done) {
					break;
				}
			}
			det.DataEnd();// 最后要调用此方法，此时，Notify被调用。
			if (isAscii) {
				return "ASCII";
			}
			if (!target.isFound()) {
				return "GBK";
			} else {
				return target.getCharset();
			}
		} catch (IOException ex) {
			return "GBK";
		}
	}

	public static String getCharset(byte[] bytes) {
		String charset = sniffCharacterEncoding(bytes);
		if (charset != null)
			return charset;
		InputStream in = new ByteArrayInputStream(bytes);
		return getCharset(in);
	}

	private static final int CHUNK_SIZE = 2000;
	// NUTCH-1006 Meta equiv with single quotes not accepted
	private static Pattern metaPattern = Pattern.compile("<meta\\s+([^>]*http-equiv=(\"|')?content-type(\"|')?[^>]*)>",
			Pattern.CASE_INSENSITIVE);
	private static Pattern charsetPattern = Pattern.compile("charset=\\s*([a-z][_\\-0-9a-z]*)",
			Pattern.CASE_INSENSITIVE);

	private static String sniffCharacterEncoding(byte[] content) {
		int length = Math.min(content.length, CHUNK_SIZE);

		// We don't care about non-ASCII parts so that it's sufficient
		// to just inflate each byte to a 16-bit value by padding.
		// For instance, the sequence {0x41, 0x82, 0xb7} will be turned into
		// {U+0041, U+0082, U+00B7}.
		String str = "";
		try {
			str = new String(content, 0, length, Charset.forName("ASCII").toString());
		} catch (UnsupportedEncodingException e) {
			// code should never come here, but just in case...
			return null;
		}

		Matcher metaMatcher = metaPattern.matcher(str);
		String encoding = null;
		if (metaMatcher.find()) {
			Matcher charsetMatcher = charsetPattern.matcher(metaMatcher.group(1));
			if (charsetMatcher.find())
				encoding = new String(charsetMatcher.group(1));
		}

		return encoding;
	}

	/**
	 * copy from nutch
	 * 
	 * @param contentType
	 * @return
	 */
	private static String parseCharacterEncoding(String contentType) {
		if (contentType == null)
			return null;
		int start = contentType.indexOf("charset=");
		if (start < 0)
			return null;
		String encoding = contentType.substring(start + 8);
		int end = encoding.indexOf(';');
		if (end >= 0)
			encoding = encoding.substring(0, end);
		encoding = encoding.trim();
		if ((encoding.length() > 2) && (encoding.startsWith("\"")) && (encoding.endsWith("\"")))
			encoding = encoding.substring(1, encoding.length() - 1);
		return (encoding.trim());

	}
}
