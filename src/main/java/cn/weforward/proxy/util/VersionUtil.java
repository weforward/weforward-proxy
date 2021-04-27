/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.proxy.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cn.weforward.common.util.FileUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.UrlUtil;

/**
 * 版本工具类
 * 
 * @author daibo
 *
 */
public class VersionUtil {
	/** 日志记录器 */
	public final static org.slf4j.Logger _Logger = LoggerFactory.getLogger(VersionUtil.class);

	/**
	 * 从jar中获取主版本
	 * 
	 * @param clazz 类
	 * @return 版本号
	 */
	public static String getMainVersionByJar(Class<?> clazz) {
		return getVersionByJar(clazz, "Main-Version");
	}

	/**
	 * 从pom.xml获取主版本
	 * 
	 * @return 版本号
	 */
	public static String getMainVersionByPom() {
		File file = new File(FileUtil.getAbsolutePath("pom.xml", null));
		if (file.exists()) {
			try {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
				NodeList list = doc.getElementsByTagName("properties");
				for (int i = 0; i < list.getLength(); i++) {
					Node node = list.item(i);
					NodeList childlist = node.getChildNodes();
					for (int j = 0; j < childlist.getLength(); j++) {
						Node child = childlist.item(j);
						if (StringUtil.eq(child.getNodeName(), "main.version")) {
							return child.getTextContent();
						}
					}
				}

			} catch (SAXException | ParserConfigurationException | IOException e) {
				_Logger.warn("解析xml异常", e);
			}
		}
		return "";
	}

	/**
	 * 从jar中获取实现版本
	 * 
	 * @param clazz 类
	 * @return 版本号
	 */
	public static String getImplementationVersionByJar(Class<?> clazz) {
		return getVersionByJar(clazz, "Implementation-Version");
	}

	/**
	 * 从pom.xml获取实现版本
	 * 
	 * @return 版本号
	 */
	public static String getImplementationVersionByPom() {
		File file = new File(FileUtil.getAbsolutePath("pom.xml", null));
		if (file.exists()) {
			try {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
				NodeList list = doc.getElementsByTagName("version");
				if (list.getLength() > 0) {
					Node node = list.item(0);
					return node.getTextContent();
				}

			} catch (SAXException | ParserConfigurationException | IOException e) {
				_Logger.warn("解析xml异常", e);
			}
		}
		return "";
	}

	/* 从jar中获取对应key的属性 */
	private static String getVersionByJar(Class<?> clazz, String key) {
		if (null == clazz) {
			return "";
		}
		String jar = null;
		try {
			CodeSource cs = clazz.getProtectionDomain().getCodeSource();
			if (null != cs) {
				URL url = cs.getLocation();
				if (null != url) {
					jar = UrlUtil.decodeUrl(url.getFile());
				}
			}
		} catch (SecurityException e) {
		}
		if (null == jar) {
			return "";
		}
		// 去除 file:
		if (jar.startsWith("file:/")) {
			jar = jar.substring(5);
		}
		// 去除 !com.xx.xx.xx.class
		int idx = jar.indexOf('!');
		if (-1 != idx) {
			jar = jar.substring(0, idx);
		}
		try {
			// 去除路径及.jar部分
			idx = jar.lastIndexOf('/');
			if (-1 == idx) {
				idx = jar.lastIndexOf('\\');
			}
			String name;
			if (-1 != idx) {
				name = jar.substring(idx + 1);
			} else {
				name = jar;
			}
			idx = name.lastIndexOf('.');
			if (-1 == idx) {
				// 应该不是jar文件吧
				_Logger.warn(clazz + " Not in JAR file: " + jar);
				return "";
			}
			name = name.substring(0, idx);
			JarFile jf = new JarFile(jar);
			Manifest mf = jf.getManifest();
			Attributes attributes = mf.getMainAttributes();
			jf.close();
			return StringUtil.toString(attributes.getValue(key));
		} catch (IOException e) {
			_Logger.warn("getJarVersion failed. " + clazz + " for " + jar, e);
		}
		return "";
	}

}
