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

class SearchProxy extends Proxy													//�����ô��������
{
	public SearchProxy(String szHost, int nPort) {								//HTTP����������ַ�Ͷ˿ں�
		super(Proxy.Type.HTTP, new InetSocketAddress(szHost, nPort));
	}
}

public abstract class SearchEngine	implements Runnable							//�������������
{	
	class TrustAnyTrustManager implements X509TrustManager {					//�Ͱ�ȫ�����й�����
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
			return true;														//���ε���֤��ȫ��������
		}
	}
	
	class LinkParser implements Runnable										//��������
	{
		Integer nCount;
		String szLink;
		
		LinkParser(String szLink)												//�����Ӻ͵�ǰ�����������Ź���
		{
			this.szLink = szLink;
			this.nCount = SearchEngine.this.nCount;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				if (set.contains(szLink)) return;								//Ϊ�˷�ֹ��ٶȻ�����ظ�����
				set.add(szLink);												//�������ӱ�
				
				String szHtml = download(szLink);								//����HTML
				if (szHtml == null) return;
				
				VisualSearchAnalyzer.mapSearchResult.put(szLink, new SearchResult(szClass, nCount, szLink, segment(parse(szHtml)), szHtml));					
																				//ȥHTML,���ķִʣ����������
				final String szTip = szClass + " Item " + nCount + " Downloaded(" + szLink + ")";
				
				if (!VisualSearchAnalyzer.bDownloading) return;
				if (dlmResult == null)
				{
					System.out.println(szTip);//�����ʾ
				}
				else
				{
					SwingUtilities.invokeLater(new Runnable() {					//�̸߳���GUI  
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
	
	HashSet<String> set;														//��ַ����
	ExecutorService pool;														//�̳߳�
	SearchProxy proxy = null;													//Ĭ�ϴ���Ϊ��
	Integer nCount, nTotal;														//�Ѿ���ʼ���ص�������Ŀ�����ܹ���Ŀ��
	String szKeyword, szClass;													//�ؼ��ʣ�����������
	static DefaultListModel dlmResult = null;
	
	public SearchEngine(SearchProxy proxy)
	{
		this.proxy = proxy;														//���ô��������
	}
	
	public void close()															//�ر��̳߳�
	{
		if (pool == null) return;
		
		if (!pool.isShutdown())	pool.shutdown();
		pool.shutdownNow();
	}
	
	protected String download(String szUrl) throws UnsupportedEncodingException	//����
	{	
		try {
			final int BUFFER_SIZE = 2097152;
			int nByteIndex = 0, nByteCount;
			
			byte bytes[] = new byte[BUFFER_SIZE]; 
			String szHtml, szContentType, szEncoding;
			
			Pattern pat;
			Matcher mat;
			
			if (szUrl.subSequence(0, 5).equals("https"))						//��ȫ����
			{
				SSLContext context = SSLContext.getInstance("SSL");
				context.init(null,new TrustManager[] { new TrustAnyTrustManager() }, new SecureRandom());
				
				URL url = new URL(szUrl);
				HttpsURLConnection connection;
				
				if (proxy == null) connection = (HttpsURLConnection) url.openConnection();
				else connection = (HttpsURLConnection) url.openConnection(proxy);
				connection.setSSLSocketFactory(context.getSocketFactory());
				connection.setHostnameVerifier(new TrustAnyHostnameVerifier());	//����һ����֤
				connection.connect();
				
				szContentType = connection.getContentType();					//��ȡ��ʽ
				pat = Pattern.compile("[^;]*");
				mat = pat.matcher(szContentType);
				if (!mat.find()) return null;
				szContentType = mat.group().toString();
				if (!szContentType.equals("application/json") && !szContentType.equals("text/html")) 
					return null;												//������json��html
				
				InputStream input = connection.getInputStream();
			    nByteCount = input.read(bytes, nByteIndex, BUFFER_SIZE);
			    
			    while (nByteCount != -1) {										//html���浽�ֽ�����
			      nByteIndex += nByteCount;
			      nByteCount = input.read(bytes, nByteIndex, 1);
			    }
			    
			    szHtml = new String(bytes, 0, nByteIndex, "utf-8");				//��תΪutf-8
				
				pat = Pattern.compile("charset=[^>]*");
				mat = pat.matcher(szHtml);
				if (!mat.find()) return szHtml;
				
				szEncoding = mat.group().toString().substring(8);
				pat = Pattern.compile("[a-zA-Z0-9\\-]+");
				mat = pat.matcher(szEncoding);
				if (mat.find()) szEncoding = mat.group().toString();
				else return szHtml;
				
				return new String(bytes, 0, nByteIndex, szEncoding);			//��ȡ���뼯������ת��
			}
			else
			{
				URL url = new URL(szUrl);
				URLConnection connection;
				
				if (proxy == null) connection = url.openConnection();
				else connection = url.openConnection(proxy);
				szContentType = connection.getContentType();					//�˲�����ͬ��ȫ����
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
	
	public boolean isTerminated()													//�����Ƿ���ֹ
	{
		return pool != null && pool.isTerminated(); 
	}	
	
	protected String parse(String szHtml)
	{
        try {
        	String szText = szHtml.replaceAll("<script(?:[^<]++|<(?!/script>))*+</script>", "");//ȥ��<script>��ǩ
            Parser parser = Parser.createParser(szText, "utf-8");
            StringBean visitor = new StringBean();
            
            visitor.setLinks(false);
            visitor.setReplaceNonBreakingSpaces(true);
            visitor.setCollapse(true);
        	
        	parser.visitAllNodesWith(visitor);
			return visitor.getStrings();											//ȥ������html��ǩ
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
		pool = Executors.newFixedThreadPool(8);										//����ͬʱ7���߳�
				
		nCount = 0;
		try
		{
			szKeyword = URLEncoder.encode(szKeyword, "utf-8");						//ת�������ؼ���
		}
		catch(UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}

		this.szKeyword = szKeyword;
		this.nTotal = nTotal;
		
		pool.execute(this);
	}
	
	private List<String> segment(String szText) throws IOException					//���ķִ�
	{
		List<String> lstSegmented = new ArrayList<String>();
		StringReader reader = new StringReader(szText);
		IKSegmenter segmenter = new IKSegmenter(reader, true);;
		Lexeme lexeme = null;
		
		while ((lexeme = segmenter.next()) != null)
		{
			String szWord = lexeme.getLexemeText();
			if (!StopWords.isStopWord(szWord)) lstSegmented.add(szWord);			//ȥ��ͣ�ô�
		}
		
		return lstSegmented;
	}
	
	public static void setList(DefaultListModel dlmResult_)
	{
		dlmResult = dlmResult_;
	}
}