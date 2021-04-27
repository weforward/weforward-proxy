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
import java.io.InputStream;
import java.util.Date;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.Destroyable;
import cn.weforward.common.sys.Shutdown;
import cn.weforward.common.util.TimeUtil;
import cn.weforward.proxy.Resource;

/**
 * jar资源
 * 
 * @author daibo
 *
 */
public class JarResource implements Resource, Destroyable {
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(JarResource.class);
	/** jar文件 */
	protected JarFile m_File;
	/** jar实体 */
	protected JarEntry m_JarEntry;
	/** 名称 */
	protected String m_Name;
	/** 修改时间 */
	protected String m_LastModified;

	public JarResource(JarFile jarFile, String entry) throws IOException {
		m_File = jarFile;
		int index = entry.lastIndexOf(File.separator) + 1;
		m_Name = entry.substring(index);
		m_JarEntry = m_File.getJarEntry(entry);
		m_LastModified = parseModified();
		Shutdown.register(this);
	}

	private String parseModified() {
		return null == m_JarEntry ? null : TimeUtil.formatGMT(new Date(m_JarEntry.getLastModifiedTime().toMillis()));
	}

	@Override
	public String getName() {
		return m_Name;
	}

	@Override
	public boolean exists() {
		return m_JarEntry != null;
	}

	@Override
	public String getLastModified() {
		return m_LastModified;
	}

	@Override
	public InputStream getStream() throws IOException {
		return m_File.getInputStream(m_JarEntry);
	}

	@Override
	public String toString() {
		return m_File.toString() + "!" + m_JarEntry.toString();
	}

	@Override
	public void destroy() {
		if (null != m_File) {
			try {
				m_File.close();
			} catch (IOException e) {
				_Logger.warn("忽略关闭异常", e);
			}
		}
	}
}
