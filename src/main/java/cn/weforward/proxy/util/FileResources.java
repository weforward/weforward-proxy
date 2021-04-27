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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件资源组
 * 
 * @author daibo
 *
 */
public class FileResources {
	/** 资源组 */
	private static final Map<String, FileResource> RESOURCES = new ConcurrentHashMap<String, FileResource>();

	/**
	 * 获取资源
	 * 
	 * @param path 路径
	 * @return 文件资源
	 * @throws IOException IO异常
	 */
	public static FileResource get(String path) throws IOException {
		FileResource r = RESOURCES.get(path);
		if (null == r) {
			r = new FileResource(new File(path));
			FileResource old = RESOURCES.putIfAbsent(path, r);
			if (null != old) {
				r = old;// 被人抢先了，用别人的吧
			}
		}
		return r;
	}

}
