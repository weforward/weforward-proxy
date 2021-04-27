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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.TimeUtil;
import cn.weforward.proxy.Resource;

/**
 * 文件资源
 * 
 * @author daibo
 *
 */
public class FileResource implements Resource {
	/** 文件 */
	protected File m_File;
	/** 最后修改时间 */
	protected String m_LastModified;
	/** 是否存在 */
	protected boolean m_Exists;

	public FileResource(String path) throws IOException {
		this(new File(path));
	}

	public FileResource(File file) throws IOException {
		m_File = file;
		// 软链接的文件是会变的，不是软链接的文件不会修改
		if (file.exists()) {
			m_Exists = true;
			if (StringUtil.eq(file.getAbsolutePath(), file.getCanonicalPath())) {
				m_LastModified = parseModified();
			}
		}
	}

	private String parseModified() {
		return TimeUtil.formatGMT(new Date(m_File.lastModified()));
	}

	private File getFile() {
		return m_File;
	}

	@Override
	public String getName() {
		return getFile().getName();
	}

	@Override
	public boolean exists() {
		if (m_Exists) {
			return true;
		}
		if (getFile().exists()) {
			m_Exists = true;
			return true;
		}
		return false;
	}

	@Override
	public String getLastModified() {
		return null == m_LastModified ? parseModified() : m_LastModified;
	}

	@Override
	public InputStream getStream() throws IOException {
		try {
			return new FileInputStream(m_File);
		} catch (FileNotFoundException e) {
			m_Exists = false;// 手动删除文件时会出现
			throw e;
		}
	}

	@Override
	public String toString() {
		return m_File.toString();
	}
}
