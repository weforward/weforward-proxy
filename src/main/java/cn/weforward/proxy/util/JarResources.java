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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * jar文件资源组
 * 
 * @author daibo
 *
 */
public class JarResources {
	/** 资源组 */
	private static final Map<String, JarResource> RESOURCES = new ConcurrentHashMap<String, JarResource>();
	/** jar组 */
	private static final Map<String, JarFile> JARS = new ConcurrentHashMap<String, JarFile>(1);

	/**
	 * 获取资源
	 * 
	 * @param jar   jar包
	 * @param entry 实体
	 * @return jar资源
	 * @throws IOException IO异常
	 */
	public static JarResource get(String jar, String entry) throws IOException {
		String path = jar + "!" + entry;
		JarResource r = RESOURCES.get(path);
		if (null == r) {
			r = new JarResource(getJarFile(jar), entry);
			JarResource old = RESOURCES.putIfAbsent(path, r);
			if (null != old) {
				r = old;// 被人抢先了，用别人的吧
			}
		}
		return r;
	}

	/* 获取jar包 */
	private static JarFile getJarFile(String jar) throws IOException {
		JarFile file = JARS.get(jar);
		if (null == file) {
			file = new JarFile(jar);
			JarFile old = JARS.putIfAbsent(jar, file);
			if (null != old) {
				file.close();
				file = old;
			}
		}
		return file;
	}
}
