package pku.eecs.a1000010284;

import org.json.JSONArray;
import org.json.JSONObject;

public class Bing extends SearchEngine													//必应搜索引擎
{
	final static String
		API = "http://api.bing.net/json.aspx?AppId=BING_APP_ID&Version=2.2&Market=en-US&Sources=web&Web.Count=10&Query=";
	
	String szAPI;
	
	public Bing(String szAppId, SearchProxy proxy)
	{
		super(proxy);
		szAPI = API.replace("BING_APP_ID", szAppId);
	}
	
	@Override
	public void run()
	{
		JSONArray jsonArray = null;
		
		String szJson, szLink;
		int nIndex = 0;
		
		try
		{
			while (nCount < nTotal)														//内容大致同Google
			{
				if (jsonArray == null || nIndex >= jsonArray.length())
				{
					if (jsonArray != null && jsonArray.length() < 10) break;
					szJson = download(szAPI + szKeyword + "&Web.Offset=" + nCount);
					jsonArray = new JSONObject(szJson).getJSONObject("SearchResponse").getJSONObject("Web").getJSONArray("Results");
					
					if (jsonArray.length() == 0) break;
					nIndex = 0;
				}
				
				szLink = jsonArray.getJSONObject(nIndex++).getString("Url");
				pool.execute(new Thread(new LinkParser(szLink)));
				
				++nCount;
			}
		}
		catch(Exception e) {
			//e.printStackTrace();
		}

		pool.shutdown();
	}
}

