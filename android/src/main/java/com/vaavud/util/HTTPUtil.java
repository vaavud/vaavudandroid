package com.vaavud.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class HTTPUtil {

	public static String appendQueryParameter(String url, String name, String value) {
		try {
			String parameter = URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");

			if (url.indexOf('?') == -1) {
				// no existing '?', so append it
				return url + "?" + parameter;
			}
			else if (url.charAt(url.length() - 1) == '&') {
				// already a trailing '&', so no need to append
				return url + parameter;
			}
			else {
				// already an existing '?' and no trailing '&', so append '&'
				return url + "&" + parameter;
			}
		}
		catch (UnsupportedEncodingException e) {
			//Log.e("HTTPUtil", "Unsupported encoding", e);
			return "";
		} 
	}

	private HTTPUtil() {
	}
}
