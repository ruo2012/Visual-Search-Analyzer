package pku.eecs.a1000010284;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Baidu extends SearchEngine													//�ٶ���������
{
	final static String
		SITE = "http://www.baidu.com/",
		SEARCH = "http://www.baidu.com/s?wd=KEY_WORD";
	public Baidu(SearchProxy proxy)
	{
		super(proxy);
	}

	@Override
	public void run()
	{
		String szUrl, szHtml, szLink;
		int i = 0;
		
		try
		{
			szUrl = SEARCH.replace("KEY_WORD", szKeyword);
			
			while (nCount < nTotal)
			{
				szHtml = download(szUrl);												//���ذٶ����������ҳ��
				if (szHtml == null) break;
				
				Pattern pat = Pattern.compile("<h3 class=\"t\"><a[^>]*href=\"[^\"]*");
				Matcher mat = pat.matcher(szHtml);
				
				while (nCount < nTotal && mat.find())									//������ʽץȡ������Ŀ����
				{
					szLink = mat.group().toString();
					Pattern p = Pattern.compile("href=\"[^\"]*");
					Matcher m = p.matcher(szLink);
					
					if (m.find())
					{
						szLink = m.group().toString().substring(6);
						pool.execute(new Thread(new LinkParser(szLink)));
					}
					
					++nCount;
				}
				
				nCount = i += 10;
				
				pat = Pattern.compile("s\\?wd=[^\"]*&pn=" + i + "[^\"]*&rsv_page=1");
				mat = pat.matcher(szHtml);
				
				if (mat.find()) szUrl = SITE + mat.group().toString();					//��һҳ����
				else break;
			}
		}
		catch(Exception e) {
			//e.printStackTrace();
		}
		
		pool.shutdown();
	}	
}

