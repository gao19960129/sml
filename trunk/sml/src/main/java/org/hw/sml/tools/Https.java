package org.hw.sml.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
/**
 * httpclient  get|post
 * @author wen
 *
 */
public class Https {
	
	public static final String METHOD_GET="GET";
	public static final String METHOD_POST="POST";
	private Https(String url){
		this.url=url;
	}
	public static Https newGetHttps(String url){
		return new Https(url);
	}
	public static Https newPostHttps(String url){
		return new Https(url).method(METHOD_POST);
	}
	private String method=METHOD_GET;
	private String charset="utf-8";
	private String url;
	private Header header=new Header("*/*","*/*");
	private Object body;
	private Paramer paramer=new Paramer();
	public Https charset(String charset){
		this.charset=charset;
		return this;
	}
	private Https method(String method){
		this.method=method;
		return this;
	}
	public Https head(Header header){
		this.header=header;
		return this;
	}
	
	public String getMethod() {
		return method;
	}
	public String getCharset() {
		return charset;
	}
	public String getUrl() {
		return url;
	}
	public Header getHeader() {
		return header;
	}
	public Https param(Paramer paramer){
		this.paramer=paramer;
		return this;
	}
	
	public class Paramer{
		private String queryParamStr;
		private Map<String,String> params=MapUtils.newLinkedHashMap();
		public Paramer(){}
		public Paramer(String queryParamStr){this.queryParamStr=queryParamStr;}
		public Paramer add(String name,String value){
			params.put(name,value);
			return this;
		}
		public String builder(String charset){
			if(queryParamStr==null&&params.size()>0){
				StringBuilder sb=new StringBuilder();
				int i=0;
				for(Map.Entry<String,String> entry:params.entrySet()){
					if(i>0)
					sb.append("&");
					try {
						sb.append(entry.getKey()+"="+URLEncoder.encode(entry.getValue(),charset));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					i++;
				}
				this.queryParamStr=sb.toString();
			}
			return queryParamStr;
		}
	}
	public  class Header{
		public Header(String contentType,String accept){
			this.put("Content-Type",contentType);
			this.put("Accept", accept);
		}
		private String requestCharset=charset;
		private String responseCharset=charset;
		private Map<String,String> header=MapUtils.newLinkedHashMap();
		public Header put(String name,String value){
			if(name==null||value==null){
				return this;
			}
			header.put(name, value);
			String keyToLower=name.toLowerCase().trim();
			String valueToLower=value.toLowerCase().trim();
			try{
				if(valueToLower.contains("charset")){
					if(keyToLower.equals("content-type"))
						this.requestCharset=valueToLower.split("=")[1].replace(";","");
					else if(keyToLower.equals("accept"))
						this.responseCharset=valueToLower.split("=")[1].replace(";","");
				}
				if(keyToLower.equals("content-type")||keyToLower.equals("accept")){
					if(!valueToLower.contains("charset")){
						header.put(name, value+";charset="+charset);
					}
				}
			}catch(Exception e){}
			return this;
		}
		public String getRequestCharset() {
			return requestCharset;
		}
		public String getResponseCharset() {
			return responseCharset;
		}
		
	}
	
	public byte[] query() throws IOException{
		String qps=this.paramer.builder(header.requestCharset);
		if(qps!=null&&(this.method.equals(METHOD_GET)||body!=null)) url+=(url.contains("?")?"&":"?")+qps;
		URL realUrl = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
		for(Map.Entry<String,String> entry:header.header.entrySet())
		conn.addRequestProperty(entry.getKey(),entry.getValue());
		//
		conn.setDoOutput(true);
		conn.setRequestMethod(this.method);
		InputStream is=null;
		OutputStream out=null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();  
		try{
			if(this.method.equals(METHOD_POST)){
				conn.setDoInput(true);
				out=conn.getOutputStream();
				if(body!=null){
					if(body instanceof String)
						out.write(body.toString().getBytes(header.requestCharset));
					else
						out.write((byte[])body);
				}else if(qps!=null){
					out.write(qps.getBytes());
				}
				out.flush();
			}
			is=conn.getInputStream();
			int temp=-1;
			byte[] bytes=new byte[512];
			while((temp=is.read(bytes))!=-1){
				bos.write(bytes,0,temp);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(conn!=null){
				conn.disconnect();
			}
			if(out!=null)
				out.close();
			if(is!=null)
				is.close();
			if(bos!=null)
				bos.close();
		}
		return bos.toByteArray();
	}
	public Https body(String requestBody){
		this.body=requestBody;
		return this;
	}
	public Https body(byte[] requestBody){
		this.body=requestBody;
		return this;
	}
	public String execute() throws IOException{
		return new String(query(),header.responseCharset);
	}
	
	public Object getBody() {
		return body;
	}
	public Paramer getParamer() {
		return paramer;
	}
	
}
