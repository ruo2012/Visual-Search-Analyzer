package pku.eecs.a1000010284;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Yahoo extends SearchEngine										//雅虎搜索引擎，类似百度
{
	final static String
		SITE = "http://search.yahoo.com",
		SEARCH = "http://search.yahoo.com/search?p=KEY_WORD&toggle=1&cop=mss&ei=UTF-8&fr=yfp-t-701";
	
	public Yahoo(SearchProxy proxy)
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
				szHtml = download(szUrl);
				if (szHtml == null) break;
				
				Pattern pat = Pattern.compile("<a id=\"link-[0-9]+\"[^>]*href=\"[^\"]*");
				Matcher mat = pat.matcher(szHtml);
				
				while (nCount < nTotal && mat.find())
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
				
				pat = Pattern.compile("/search\\?p=[^\"]*&b=" + (i + 1) + "[^\"]*");
				mat = pat.matcher(szHtml);
				
				if (mat.find()) szUrl = SITE + mat.group().toString();
				else break;
			}
		}
		catch(Exception e) {
			//e.printStackTrace();
		}
		
		pool.shutdown();
	}	
}