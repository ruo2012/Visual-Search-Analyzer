package pku.eecs.a1000010284;

import org.json.JSONArray;
import org.json.JSONObject;

public class Google extends SearchEngine
{
	final static String
		API = "https://www.googleapis.com/customsearch/v1?key=GOOGLE_APP_ID&cx=GOOGLE_CSE_ID&q=";
	
	String szAPI;
	
	public Google(String szAppId, String szCseId, SearchProxy proxy)					//�ȸ���������
	{
		super(proxy);
		szAPI = API.replace("GOOGLE_APP_ID", szAppId).replace("GOOGLE_CSE_ID",szCseId);//���û���id����api url
	}

	@Override
	public void run()
	{	
		JSONArray jsonArray = null;
		
		String szJson, szLink;
		int nIndex = 0;		
		try
		{			
			while (nCount < nTotal)														//����ָ����Ŀ������
			{
				if (jsonArray == null || nIndex >= jsonArray.length())					//û��json��������Ѷ�ȡ��
				{
					if (jsonArray != null && jsonArray.length() < 10) break;			//������һ��json����10����ַ
					szJson = download(szAPI + szKeyword + "&start=" + (nCount + 1));
					jsonArray = new JSONObject(szJson).getJSONArray("items");			//��ȡjson������
					
					if (jsonArray.length() == 0) break;
					nIndex = 0;
				}
				
				szLink = jsonArray.getJSONObject(nIndex++).getString("link");			//��ȡ����
				pool.execute(new Thread(new LinkParser(szLink)));						//���������ӷ����̳߳���
				
				++nCount;
			}
		}
		catch(Exception e) {
			//e.printStackTrace();
		}
		
		pool.shutdown();																//�ر��̳߳�
	}
}