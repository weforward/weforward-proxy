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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import cn.weforward.common.crypto.Base64;
import cn.weforward.common.util.FileUtil;
import cn.weforward.common.util.StringUtil;

public class Uploader {

	public static void main(String[] args) {
		if (null == args || 2 > args.length) {
			System.out.println("请先输入dir和url");
			return;
		}
		File dir = new File(FileUtil.getAbsolutePath(args[0], null));
		String url = args[1];
		System.out.println("上传地址:" + url);
		String username = null;
		String password = null;
		String authorization = null;
		if (args.length >= 4) {
			username = args[2];
			password = args[3];
			authorization = "Basic " + Base64.encode((username + ":" + password).getBytes());
		}
		for (File file : dir.listFiles()) {
			try {
				uploadFile(url, file, authorization);
				System.out.println("成功上传:" + file.getName());
			} catch (IOException e) {
				System.out.println("上传:" + file.getName() + "异常");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 文件上传的方法
	 * 
	 * @param disturl       资源链接
	 * @param file          文件
	 * @param authorization 验证信息
	 * @throws IOException IO异常
	 */
	public static void uploadFile(String disturl, File file, String authorization) throws IOException {
		String end = "\r\n";
		String twoHyphens = "--";
		String boundary = "----WeforwardBoundary" + System.currentTimeMillis();
		// String boundary = "*****";
		DataOutputStream ds = null;
		InputStream inputStream = null;
		String actionUrl = disturl + file.getName();
		try {
			URL url = new URL(actionUrl);
			// 连接类的父类，抽象类
			URLConnection urlConnection = url.openConnection();
			// http的连接类
			HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
			// 设置是否从httpUrlConnection读入，默认情况下是true;
			httpURLConnection.setDoInput(true);
			// 设置是否向httpUrlConnection输出
			httpURLConnection.setDoOutput(true);
			// Post 请求不能使用缓存
			httpURLConnection.setUseCaches(false);
			// 设定请求的方法，默认是GET
			httpURLConnection.setRequestMethod("POST");
			// 设置字符编码连接参数
			httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
			// 设置字符编码
			httpURLConnection.setRequestProperty("Charset", "UTF-8");
			// 设置请求内容类型
			httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			if (!StringUtil.isEmpty(authorization)) {
				httpURLConnection.setRequestProperty("Authorization", authorization);
			}
			httpURLConnection.setReadTimeout(30 * 60 * 1000);
			// 设置DataOutputStream
			ds = new DataOutputStream(httpURLConnection.getOutputStream());
			ds.writeBytes(twoHyphens + boundary + end);
			ds.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + file.getName() + "\"" + end);
			ds.writeBytes(end);
			FileInputStream fStream = new FileInputStream(file);
			int bufferSize = 4 * 1024;
			byte[] buffer = new byte[bufferSize];
			int length = -1;
			while ((length = fStream.read(buffer)) != -1) {
				ds.write(buffer, 0, length);
			}
			ds.writeBytes(end);
			/* close streams */
			fStream.close();
			ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
			/* close streams */
			ds.flush();
			if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new IOException(
						"HTTP Request is not success, Response code is " + httpURLConnection.getResponseCode());
			}
		} finally {
			if (ds != null) {
				try {
					ds.close();
				} catch (IOException e) {
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
