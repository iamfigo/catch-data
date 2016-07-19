package com.huit.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * java -cp catch-data-jar-with-dependencies.jar com.huit.util.ParseData
 * 
 * @author huit
 *
 */
public class ParseData {
	private static Logger logger = LoggerFactory.getLogger(ParseData.class);
	private static String fileSavePath = SystemConf.get("fileSavePath");
	private static Set<String> urlDownloaded = new HashSet<String>();
	private static Set<String> catchData = new HashSet<String>();
	private static Set<String> catchEmail = new HashSet<String>();
	private static Pattern emailPatten = Pattern.compile(SystemConf.get("emailPatten"));
	private static Pattern mobilePatten = Pattern.compile(SystemConf.get("mobilePatten"));
	private static Set<String> catchMobile = new HashSet<String>();
	private static String[] keyWordExclude = SystemConf.get("keyWordExclude").split(",");
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
		for (String url : urlDownloaded) {
			parseEmail(url, getHtmlByFile(url2filePath(url)));
			parseMobile(url, getHtmlByFile(url2filePath(url)));
		}
		logger.info("catchEmail->size:" + catchEmail.size() + " data:" + catchEmail);
		logger.info("catchMobile->size:" + catchMobile.size() + " data:" + catchMobile);
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

	public static void parseEmail(String url, String html) {
		Matcher m = emailPatten.matcher(html);
		String data;
		while (m.find()) {
			data = m.group();
			catchEmail.add(data);
			logger.debug(url + "->" + data);
		}
	}

	public static void parseMobile(String url, String html) {
		Matcher m = mobilePatten.matcher(html);
		String data;
		while (m.find()) {
			data = m.group();
			catchMobile.add(data);
			logger.debug(url + "->" + data);
		}
	}

	public static void parseData(String url, String html) {
		String[] matchs = SystemConf.get("keyWordPatten").split(",");
		String vfp;
		for (String match : matchs) {
			Pattern p = Pattern.compile(match);
			Matcher m = p.matcher(html);
			while (m.find()) {
				vfp = m.group();
				boolean isExclude = false;
				for (String exclude : keyWordExclude) {
					if (vfp.contains(exclude)) {
						isExclude = true;
						break;
					}
				}
				if (!isExclude) {
					logger.info(url + "->" + vfp);
					catchData.add(vfp);
				}
			}
		}
	}

	private static String url2node(String url) {
		return url.replace("http://", "");
	}

	private static String url2filePath(String url) {
		return fileSavePath + url.replace("http://", "");
	}

}