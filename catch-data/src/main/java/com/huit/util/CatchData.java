package com.huit.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * 
 * @author huit
 *
 */
public class CatchData {
	private static final String urlSub = SystemConf.get("urlSub");
	private static Logger logger = LoggerFactory.getLogger(CatchData.class);
	private static String fileSavePath = SystemConf.get("fileSavePath");
	private static String url = SystemConf.get("url");
	private static Set<String> urlDownloaded = new HashSet<String>();
	private static Set<String> urlParesed = new HashSet<String>();
	static {
		File dir = new File(fileSavePath);
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		loadGetFile(dir);
		logger.info("loadFileSize:" + urlDownloaded.size());
	}

	private static void loadGetFile(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File subFile : files) {
				if (subFile.isDirectory()) {
					loadGetFile(subFile);
				} else {
					String path = subFile.getAbsolutePath().replace("\\", "/");
					urlDownloaded.add(path.replace(fileSavePath, ""));
				}
			}
		} else {
			urlDownloaded.add(file.getAbsolutePath().replace(fileSavePath, ""));
		}
	}

	public static void main(String[] args) throws Exception {
		String[] urls = SystemConf.get("urls").split(",");
		for (String url : urls) {
			if (!url.startsWith("http:")) {
				continue;
			}
			int indexBegin = url.indexOf("{");
			if (indexBegin > 0) {
				int indexEnd = url.indexOf("}", indexBegin);
				if (indexEnd > 0) {
					String[] circulationInfo = url.substring(indexBegin + 1, indexEnd).split("-");
					String head = url.substring(0, indexBegin);
					String tail = url.substring(indexEnd + 1);
					if (circulationInfo.length == 2) {
						int offset = Integer.valueOf(circulationInfo[0]);
						int count = Integer.valueOf(circulationInfo[1]);
						logger.info("download->" + url);
						for (int i = offset; i < count; i++) {
							url = head + i + tail;
							downloadHtml(url);
						}
					}
				}
			} else {
				logger.info("download->" + url);
				downloadHtml(url);
			}
		}
	}

	private static void downloadHtml(String url) {
		try {
			String html = getHtml(url);
			for (String subUrl : getSubUrl(url, html)) {
				try {
					getHtml(subUrl);
				} catch (Exception ignore) {
					logger.error("getSubUrlError->url:" + subUrl, ignore);
				}
			}
		} catch (Exception ignore) {
			logger.error("getUrlError->url:" + url, ignore);
		}
	}

	public static void writeHtml(String url, String html) {
		FileOutputStream fos = null;
		try {
			String savePath = fileSavePath + url2node(url);
			File file = new File(savePath);
			if (!file.getParentFile().exists()) {
				if (!file.getParentFile().mkdirs()) {
					logger.info("createDir->" + file.getParentFile().getAbsoluteFile());
				}
			}

			fos = new FileOutputStream(file);
			final byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
			fos.write(bom);
			fos.write(html.getBytes("UTF-8"));
			logger.info("saveFile->" + file.getAbsoluteFile());
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				fos.close();
			} catch (Exception ex) {
			}
		}
	}

	public static String getHtmlByFile(String filePath) {
		StringBuffer sb = new StringBuffer();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String data;
			while (null != (data = br.readLine())) {
				sb.append(data);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static void getData(String url, String data) {
		if (!urlParesed.contains(url)) {
			int indexBegin, indexEnd;
			String[] matchs = new String[] { "电话", "微信", "email", "请联系" };
			for (String match : matchs) {
				indexBegin = data.indexOf(match);
				if (indexBegin > 0) {
					indexEnd = data.indexOf("<", indexBegin);
					if (indexEnd > 0) {
						data = data.substring(indexBegin, indexEnd);
					}
					logger.info(url + "," + data);
				}
			}
			urlParesed.add(url2node(url));
		}
	}

	private static String url2node(String url) {
		url = url.replace("http://", "");
		url = url.replace("?", "#");
		return url;
	}

	private static String url2filePath(String url) {
		return fileSavePath + url2node(url);
	}

	private static String getHtml(String url) throws Exception {
		String html = null;
		if (urlDownloaded.contains(url2node(url))) {
			html = getHtmlByFile(url2filePath(url));
		} else {
			URL google = new URL(new String(url.toString()));
			HttpURLConnection httpConnection = (HttpURLConnection) google.openConnection();
			httpConnection.setRequestProperty("User-agent", "Mozilla/5.0");
			httpConnection.setRequestMethod("GET");
			httpConnection.setConnectTimeout(SystemConf.get("ConnectTimeout", Integer.class));
			httpConnection.setReadTimeout(SystemConf.get("ReadTimeout", Integer.class));

			StringBuffer sb = new StringBuffer();
			InputStream is = httpConnection.getInputStream();
			byte[] buf = new byte[1024];
			int len = 0;
			String data;
			String charsetName = SystemConf.get("charsetName");
			while (-1 != (len = is.read(buf))) {
				data = new String(buf, 0, len, charsetName);
				sb.append(data);
			}
			httpConnection.disconnect();
			httpConnection = null;
			writeHtml(url, sb.toString());
			html = sb.toString();
		}

		return html;
	}

	/***
	 * 获取subUrl地址
	 * 
	 * @param html
	 * @param html2
	 * @return
	 */
	private static Set<String> getSubUrl(String parentUrl, String html) {
		Set<String> subUrl = new HashSet<String>();
		parentUrl = parentUrl.substring(0, parentUrl.lastIndexOf('/') + 1);
		int indexBegin = 0, indexEnd = 0;
		do {
			indexBegin = html.indexOf(urlSub, indexEnd);
			if (indexBegin > 0) {
				indexEnd = html.indexOf("\"", indexBegin + "<a href".length());
				if (indexEnd > 0) {
					String url = html.substring(indexBegin, indexEnd);
					if (!url.startsWith("http:")) {// 相对地址
						url = parentUrl + url;
					}
					subUrl.add(url);
				}
			}
		} while (indexBegin > 0 && indexEnd > 0);
		return subUrl;
	}
}