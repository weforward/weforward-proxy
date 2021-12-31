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
package cn.weforward.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.DictionaryExt;
import cn.weforward.common.crypto.Base64;
import cn.weforward.common.restful.RestfulRequest;
import cn.weforward.common.restful.RestfulResponse;
import cn.weforward.common.restful.RestfulService;
import cn.weforward.common.sys.VmStat;
import cn.weforward.common.util.StringUtil;
import cn.weforward.protocol.aio.http.RestfulServer;
import cn.weforward.protocol.aio.netty.NettyHttpServer;
import cn.weforward.proxy.util.HttpInvoker;

/**
 * 管理服务器
 * 
 * @author daibo
 *
 */
public class ManageServer extends NettyHttpServer implements RestfulService {

	/** 日志 */
	static final Logger _Logger = LoggerFactory.getLogger(ManageServer.class);

	protected String m_UserName;
	protected String m_Password;
	protected File m_Root;
	protected HttpInvoker m_Invoker;

	/**
	 * 构造
	 * 
	 * @param name 名称
	 * @param port 端口
	 * @throws Exception 异常
	 */
	public ManageServer(String name, int port, String root, String username, String password) throws Exception {
		super(port);
		setName(name);
		setAcceptThreads(VmStat._cpus);
		setWorkThreads(50);
		RestfulServer server = new RestfulServer(this);
		setHandlerFactory(server);
		m_UserName = username;
		m_Password = password;
		HttpInvoker invoker = new HttpInvoker(1, 3);
		invoker.setUserName(username);
		invoker.setPassword(password);
		m_Invoker = invoker;
		m_Root = new File(root);
	}

	public void setExecutor(Executor executor) {
		((RestfulServer) m_HandlerFactory).setExecutor(executor);
	}

	public HttpInvoker getInvoker() {
		return m_Invoker;
	}

	@Override
	public void precheck(RestfulRequest request, RestfulResponse response) throws IOException {
		String authorization = request.getHeaders().get("Authorization");
		String basic = "Basic";
		if (!StringUtil.isEmpty(authorization) && authorization.startsWith(basic)) {
			authorization = authorization.substring(basic.length());
			String code = new String(Base64.decode(authorization));
			int index = code.indexOf(":");
			String username = code.substring(0, index);
			String password = code.substring(index + 1);
			if (StringUtil.eq(username, m_UserName) && StringUtil.eq(password, m_Password)) {
				return;
			}
		}
		response.setHeader("WWW-Authenticate", basic);
		response.setStatus(RestfulResponse.STATUS_UNAUTHORIZED);
		response.openOutput().close();
		return;

	}

	@Override
	public void service(RestfulRequest request, RestfulResponse response) throws IOException {
		String path = request.getUri();
		DictionaryExt<String, String> param = request.getParams();
		String project = param.get("project");
		String version = param.get("version");
		File file = new File(m_Root.getAbsolutePath() + File.separator + project + File.separator + version
				+ File.separator + "file.zip");
		File target = file.getParentFile();
		File old = new File(target.getParentFile(), "old");
		File latest = new File(target.getParentFile(), "latest");
		File back = new File(target.getParentFile(), "back");
		if (path.endsWith("/upgrade")) {
			try {
				String url = param.get("url");
				HttpGet get = new HttpGet(url);
				HttpResponse res = getInvoker().execute(get);
				StatusLine status = res.getStatusLine();
				if (status.getStatusCode() != HttpStatus.SC_OK) {
					throw new IOException("响应码异常" + status);
				}
				saveFile(file, res.getEntity().getContent());
				unZipFiles(file, target);
				file.delete();
				if (latest.exists()) {
					latest.renameTo(old);
				}
				Files.createSymbolicLink(latest.toPath(), target.toPath());
				ok(response, "success");
			} catch (Throwable e) {
				_Logger.error("升级异常", e);
				serviceError(response, e.getMessage());
			}
		} else if (path.endsWith("/rollback")) {
			if (!latest.exists()) {
				serviceError(response, "不存在latest");
				return;
			}
			if (!old.exists()) {
				serviceError(response, "不存在old");
				return;
			}
			if (back.exists()) {
				back.delete();
			}
			latest.renameTo(back);
			old.renameTo(latest);
			ok(response, "success");
		} else {
			response.setStatus(RestfulResponse.STATUS_NOT_FOUND);
			response.openOutput().close();
		}
	}

	private void ok(RestfulResponse response, String message) throws IOException {
		response.setStatus(RestfulResponse.STATUS_OK);
		outAndClose(response, message);
	}

	private void serviceError(RestfulResponse response, String message) throws IOException {
		response.setStatus(RestfulResponse.STATUS_INTERNAL_SERVER_ERROR);
		outAndClose(response, message);
	}

	private void outAndClose(RestfulResponse response, String message) throws IOException {
		response.setHeader("Content-Type", "text/plain;charset=utf-8");
		try (OutputStream stream = response.openOutput()) {
			stream.write(message.getBytes());
		}
	}

	@Override
	public void timeout(RestfulRequest request, RestfulResponse response) throws IOException {

	}

	private void saveFile(File file, InputStream in) throws IOException {
		if (file.exists()) {
			file.delete();
		} else {
			File dir = file.getParentFile();
			dir.mkdirs();
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			byte[] bs = new byte[1024];
			int l;
			while ((l = in.read(bs)) != -1) {
				out.write(bs, 0, l);
			}
		} finally {
			try {
				if (null != out) {
					out.close();
				}
			} catch (Throwable e) {
			}
		}
	}

	/**
	 * 解压文件到指定目录 解压后的文件名，和之前一致
	 * 
	 * @param zipFile 待解压的zip文件
	 * @param descDir 指定目录
	 */
	public static void unZipFiles(File file, File pathFile) throws IOException {
		ZipFile zip = null;
		try {
			zip = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				String zipEntryName = entry.getName();
				String outPath = (pathFile.getAbsolutePath() + File.separator + zipEntryName);
				File outFile = new File(outPath);
				if (entry.isDirectory()) {
					outFile.mkdirs();
				} else {
					File parent = outFile.getParentFile();
					if (!parent.exists()) {
						parent.mkdirs();
					}
					InputStream in = zip.getInputStream(entry);
					FileOutputStream out = new FileOutputStream(outPath);
					byte[] bs = new byte[1024];
					int len;
					while ((len = in.read(bs)) > 0) {
						out.write(bs, 0, len);
					}
					in.close();
					out.close();
				}
			}
		} finally {
			if (null != zip) {
				zip.close();
			}
		}
	}

}
