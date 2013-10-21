package pku.eecs.a1000010284;

import java.util.HashMap;
import java.util.Map;

public class TfIdfVector
{
	private static Map<String, Double> mapWord;								//���ʱ����������Ͽ��г��ִ���
	private static SearchResult[] arrSearchResult;
	
	public static void getTfIdf()											//��ȡTF-IDF
	{
		getWordList();														//��ȡ���ʱ�
		
		Map<String, Double> mapTemp = new HashMap<String, Double>();
		for (SearchResult searchResult : arrSearchResult)					//ö���������
		{
			mapTemp.clear();
			for (String szWord : mapWord.keySet())							//ͳ��ÿһ��������������Ŀ�г��ָ���
			{
				for (String szSegmented : searchResult.lstSegmented)
				{
					if (szSegmented.equals(szWord))
					{
						if (mapTemp.containsKey(szWord))
						{
							double fCount = mapTemp.get(szWord);
							mapTemp.put(szWord, fCount + 1);	//������һ
						}
						else
						{
							mapTemp.put(szWord, 1.0);
						}
					}
				}
			}
			
			double fMaxCount = 0.0;											//��Ƶ����ߵĴ�����һ��
			for (double fCount : mapTemp.values())
			{
				if (fCount > fMaxCount) fMaxCount = fCount;
			}
			
			for (String szWord : mapTemp.keySet())
			{
				double fIdf = Math.log(arrSearchResult.length / mapWord.get(szWord)) / Math.log(10);//����IDF
				double fTf = mapTemp.get(szWord) / fMaxCount;				//����TF
				
				searchResult.mapTfIdf.put(szWord, fTf * fIdf);				//����TF-IDFֵ
			}	
		}
	}
	
	private static void getWordList()										//��ȡ���Դʵ�
	{
		arrSearchResult = VisualSearchAnalyzer.arrSearchResult;
		VisualSearchAnalyzer.mapWord = mapWord  = new HashMap<String, Double>();
		
		final double MINCOUNT = 50.0;										//�������ٳ���Ƶ��
		Map<String, Double> mapTemp = new HashMap<String, Double>();
		
		for (SearchResult searchResult : arrSearchResult)
		{
			for (String szWord : searchResult.lstSegmented)
			{
				if (mapTemp.containsKey(szWord))							//��������
				{
					double fCount = mapTemp.get(szWord);
					mapTemp.put(szWord, fCount + 1);						//������һ
				}
				else
				{
					mapTemp.put(szWord, 1.0);								//����Ϊһ
				}
			}
		}
		
		for (String szWord : mapTemp.keySet())
		{
			if (mapTemp.get(szWord) > MINCOUNT) mapWord.put(szWord, 0.0);	//DF��ά������ָ��ֵ�ļ��뵥���б�������ʼΪ��
		}
		
		for (SearchResult searchResult : arrSearchResult)
		{
			for (String szWord : mapWord.keySet())
			{
				if (searchResult.lstSegmented.contains(szWord))
				{
					double fCount = mapWord.get(szWord);
					mapWord.put(szWord, fCount + 1);						//������һ
				}
			}
		}
	}
}