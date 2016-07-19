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
 * 
 * @author huit
 *
 */
public class ParseData {
	private static Logger logger = LoggerFactory.getLogger(ParseData.class);
	private static String fileSavePath = SystemConf.get("fileSavePath");
	private static Set<String> urlDownloaded = new HashSet<String>();
	private static Set<String> urlParesed = new HashSet<String>();
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
		for (String html : urlDownloaded) {
			parseData(html, getHtmlByFile(url2filePath(html)));
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
				}
			}
		}
	}

	/*public static void getData(String url, String html) {
		if (!urlParesed.contains(url)) {
			int indexBegin, indexEnd;
			String[] keyWordExclude = SystemConf.get("keyWordExclude").split(",");
			String[] matchs = new String[] { "^(\w)+(\.\w+)*@(\w)+((\.\w{2,3}){1,3})$"};
			int indexBegin = 0, indexEnd = 0;
			do {
				for (String match : matchs) {
					
				}
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
			
			for (String match : matchs) {
				indexBegin = data.indexOf(match);
				if (indexBegin > 0) {
					indexEnd = data.indexOf("<", indexBegin);
					boolean isExclude = false;
					if (indexEnd > 0) {
						data = data.substring(indexBegin, indexEnd);
						for (String exclude : keyWordExclude) {
							if (data.contains(exclude)) {
								isExclude = true;
								break;
							}
						}
					}
					if (!isExclude) {
						logger.info(url + "->" + data);
					}
				}
			}
			urlParesed.add(url2node(url));
		}
	}*/

	private static String url2node(String url) {
		return url.replace("http://", "");
	}

	private static String url2filePath(String url) {
		return fileSavePath + url.replace("http://", "");
	}

}