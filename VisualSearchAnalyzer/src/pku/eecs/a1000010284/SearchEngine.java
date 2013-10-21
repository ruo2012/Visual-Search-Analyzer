package pku.eecs.a1000010284;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.htmlparser.util.ParserException;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

class SearchProxy extends Proxy													//搜索用代理服务器
{
	public SearchProxy(String szHost, int nPort) {								//HTTP代理，给定地址和端口号
		super(Proxy.Type.HTTP, new InetSocketAddress(szHost, nPort));
	}
}

public abstract class SearchEngine	implements Runnable							//搜素引擎抽象类
{	
	class TrustAnyTrustManager implements X509TrustManager {					//和安全连接有关内容
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}
	 
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}
	 
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[] {};
		}
	}
	
	class TrustAnyHostnameVerifier implements HostnameVerifier {
		public boolean verify(String hostname, SSLSession session) {
			return true;														//屏蔽掉验证，全部返回真
		}
	}
	
	class LinkParser implements Runnable										//处理链接
	{
		Integer nCount;
		String szLink;
		
		LinkParser(String szLink)												//由链接和当前处理的链接序号构建
		{
			this.szLink = szLink;
			this.nCount = SearchEngine.this.nCount;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				if (set.contains(szLink)) return;								//为了防止如百度会出现重复链接
				set.add(szLink);												//加入链接表
				
				String szHtml = download(szLink);								//下载HTML
				if (szHtml == null) return;
				
				VisualSearchAnalyzer.mapSearchResult.put(szLink, new SearchResult(szClass, nCount, szLink, segment(parse(szHtml)), szHtml));					
																				//去HTML,中文分词，添加搜索项
				final String szTip = szClass + " Item " + nCount + " Downloaded(" + szLink + ")";
				
				if (!VisualSearchAnalyzer.bDownloading) return;
				if (dlmResult == null)
				{
					System.out.println(szTip);//输出提示
				}
				else
				{
					SwingUtilities.invokeLater(new Runnable() {					//线程更新GUI  
                        public void run() {  
                        	dlmResult.addElement(szTip);
                        }  
                    });
	
				}
			} catch (Exception e) {
			//TODO Auto-generated catch block
			//e.printStackTrace();
			}
		}
	}
	
	HashSet<String> set;														//网址集合
	ExecutorService pool;														//线程池
	SearchProxy proxy = null;													//默认代理为空
	Integer nCount, nTotal;														//已经开始下载的搜索条目数，总共条目数
	String szKeyword, szClass;													//关键词，搜索引擎名
	static DefaultListModel dlmResult = null;
	
	public SearchEngine(SearchProxy proxy)
	{
		this.proxy = proxy;														//设置代理服务器
	}
	
	public void close()															//关闭线程池
	{
		if (pool == null) return;
		
		if (!pool.isShutdown())	pool.shutdown();
		pool.shutdownNow();
	}
	
	protected String download(String szUrl) throws UnsupportedEncodingException	//下载
	{	
		try {
			final int BUFFER_SIZE = 2097152;
			int nByteIndex = 0, nByteCount;
			
			byte bytes[] = new byte[BUFFER_SIZE]; 
			String szHtml, szContentType, szEncoding;
			
			Pattern pat;
			Matcher mat;
			
			if (szUrl.subSequence(0, 5).equals("https"))						//安全链接
			{
				SSLContext context = SSLContext.getInstance("SSL");
				context.init(null,new TrustManager[] { new TrustAnyTrustManager() }, new SecureRandom());
				
				URL url = new URL(szUrl);
				HttpsURLConnection connection;
				
				if (proxy == null) connection = (HttpsURLConnection) url.openConnection();
				else connection = (HttpsURLConnection) url.openConnection(proxy);
				connection.setSSLSocketFactory(context.getSocketFactory());
				connection.setHostnameVerifier(new TrustAnyHostnameVerifier());	//屏蔽一切验证
				connection.connect();
				
				szContentType = connection.getContentType();					//获取格式
				pat = Pattern.compile("[^;]*");
				mat = pat.matcher(szContentType);
				if (!mat.find()) return null;
				szContentType = mat.group().toString();
				if (!szContentType.equals("application/json") && !szContentType.equals("text/html")) 
					return null;												//必须是json或html
				
				InputStream input = connection.getInputStream();
			    nByteCount = input.read(bytes, nByteIndex, BUFFER_SIZE);
			    
			    while (nByteCount != -1) {										//html保存到字节数组
			      nByteIndex += nByteCount;
			      nByteCount = input.read(bytes, nByteIndex, 1);
			    }
			    
			    szHtml = new String(bytes, 0, nByteIndex, "utf-8");				//试转为utf-8
				
				pat = Pattern.compile("charset=[^>]*");
				mat = pat.matcher(szHtml);
				if (!mat.find()) return szHtml;
				
				szEncoding = mat.group().toString().substring(8);
				pat = Pattern.compile("[a-zA-Z0-9\\-]+");
				mat = pat.matcher(szEncoding);
				if (mat.find()) szEncoding = mat.group().toString();
				else return szHtml;
				
				return new String(bytes, 0, nByteIndex, szEncoding);			//获取编码集后重新转换
			}
			else
			{
				URL url = new URL(szUrl);
				URLConnection connection;
				
				if (proxy == null) connection = url.openConnection();
				else connection = url.openConnection(proxy);
				szContentType = connection.getContentType();					//此部分下同安全连接
				pat = Pattern.compile("[^;]*");
				mat = pat.matcher(szContentType);
				if (!mat.find()) return null;
				szContentType = mat.group().toString();
				if (!szContentType.equals("application/json") && !szContentType.equals("text/html")) 
					return null;

				InputStream input = connection.getInputStream();
				nByteCount = input.read(bytes, nByteIndex, BUFFER_SIZE);
			    
			    while (nByteCount != -1) {
			      nByteIndex += nByteCount;
			      nByteCount = input.read(bytes, nByteIndex, 1);
			    }
			    
			    szHtml = new String(bytes, 0, nByteIndex, "utf-8");
				
				pat = Pattern.compile("charset=[^>]*");
				mat = pat.matcher(szHtml);
				if (!mat.find()) return szHtml;
				
				szEncoding = mat.group().toString().substring(8);
				pat = Pattern.compile("[a-zA-Z0-9\\-]+");
				mat = pat.matcher(szEncoding);
				if (mat.find()) szEncoding = mat.group().toString();
				else return szHtml;
				
				return new String(bytes, 0, nByteIndex, szEncoding);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return null;
		}
	}
	
	public boolean isTerminated()													//返回是否被终止
	{
		return pool != null && pool.isTerminated(); 
	}	
	
	protected String parse(String szHtml)
	{
        try {
        	String szText = szHtml.replaceAll("<script(?:[^<]++|<(?!/script>))*+</script>", "");//去除<script>标签
            Parser parser = Parser.createParser(szText, "utf-8");
            StringBean visitor = new StringBean();
            
            visitor.setLinks(false);
            visitor.setReplaceNonBreakingSpaces(true);
            visitor.setCollapse(true);
        	
        	parser.visitAllNodesWith(visitor);
			return visitor.getStrings();											//去除所有html标签
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return szHtml;
		}
	}
	
	protected void search(String szKeyword, int nTotal)
	{	
		szClass = getClass().getSimpleName();
		set = new HashSet<String>();
		pool = Executors.newFixedThreadPool(8);										//允许同时7个线程
				
		nCount = 0;
		try
		{
			szKeyword = URLEncoder.encode(szKeyword, "utf-8");						//转换搜索关键词
		}
		catch(UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}

		this.szKeyword = szKeyword;
		this.nTotal = nTotal;
		
		pool.execute(this);
	}
	
	private List<String> segment(String szText) throws IOException					//中文分词
	{
		List<String> lstSegmented = new ArrayList<String>();
		StringReader reader = new StringReader(szText);
		IKSegmenter segmenter = new IKSegmenter(reader, true);;
		Lexeme lexeme = null;
		
		while ((lexeme = segmenter.next()) != null)
		{
			String szWord = lexeme.getLexemeText();
			if (!StopWords.isStopWord(szWord)) lstSegmented.add(szWord);			//去除停用词
		}
		
		return lstSegmented;
	}
	
	public static void setList(DefaultListModel dlmResult_)
	{
		dlmResult = dlmResult_;
	}
}