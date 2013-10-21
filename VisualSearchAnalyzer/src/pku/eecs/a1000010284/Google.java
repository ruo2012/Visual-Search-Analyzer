package pku.eecs.a1000010284;

import org.json.JSONArray;
import org.json.JSONObject;

public class Google extends SearchEngine
{
	final static String
		API = "https://www.googleapis.com/customsearch/v1?key=GOOGLE_APP_ID&cx=GOOGLE_CSE_ID&q=";
	
	String szAPI;
	
	public Google(String szAppId, String szCseId, SearchProxy proxy)					//谷歌搜索引擎
	{
		super(proxy);
		szAPI = API.replace("GOOGLE_APP_ID", szAppId).replace("GOOGLE_CSE_ID",szCseId);//将用户的id放入api url
	}

	@Override
	public void run()
	{	
		JSONArray jsonArray = null;
		
		String szJson, szLink;
		int nIndex = 0;		
		try
		{			
			while (nCount < nTotal)														//下载指定数目的链接
			{
				if (jsonArray == null || nIndex >= jsonArray.length())					//没有json数组或者已读取玩
				{
					if (jsonArray != null && jsonArray.length() < 10) break;			//如果最后一个json不足10个网址
					szJson = download(szAPI + szKeyword + "&start=" + (nCount + 1));
					jsonArray = new JSONObject(szJson).getJSONArray("items");			//获取json的数据
					
					if (jsonArray.length() == 0) break;
					nIndex = 0;
				}
				
				szLink = jsonArray.getJSONObject(nIndex++).getString("link");			//获取链接
				pool.execute(new Thread(new LinkParser(szLink)));						//将下载链接放入线程池中
				
				++nCount;
			}
		}
		catch(Exception e) {
			//e.printStackTrace();
		}
		
		pool.shutdown();																//关闭线程池
	}
}