package pku.eecs.a1000010284;

import java.util.HashMap;
import java.util.Map;

public class TfIdfVector
{
	private static Map<String, Double> mapWord;								//单词表，单词在语料库中出现次数
	private static SearchResult[] arrSearchResult;
	
	public static void getTfIdf()											//获取TF-IDF
	{
		getWordList();														//获取单词表
		
		Map<String, Double> mapTemp = new HashMap<String, Double>();
		for (SearchResult searchResult : arrSearchResult)					//枚举搜索结果
		{
			mapTemp.clear();
			for (String szWord : mapWord.keySet())							//统计每一个单词在搜索条目中出现个数
			{
				for (String szSegmented : searchResult.lstSegmented)
				{
					if (szSegmented.equals(szWord))
					{
						if (mapTemp.containsKey(szWord))
						{
							double fCount = mapTemp.get(szWord);
							mapTemp.put(szWord, fCount + 1);	//计数加一
						}
						else
						{
							mapTemp.put(szWord, 1.0);
						}
					}
				}
			}
			
			double fMaxCount = 0.0;											//用频数最高的词作归一化
			for (double fCount : mapTemp.values())
			{
				if (fCount > fMaxCount) fMaxCount = fCount;
			}
			
			for (String szWord : mapTemp.keySet())
			{
				double fIdf = Math.log(arrSearchResult.length / mapWord.get(szWord)) / Math.log(10);//计算IDF
				double fTf = mapTemp.get(szWord) / fMaxCount;				//计算TF
				
				searchResult.mapTfIdf.put(szWord, fTf * fIdf);				//加入TF-IDF值
			}	
		}
	}
	
	private static void getWordList()										//获取属性词典
	{
		arrSearchResult = VisualSearchAnalyzer.arrSearchResult;
		VisualSearchAnalyzer.mapWord = mapWord  = new HashMap<String, Double>();
		
		final double MINCOUNT = 50.0;										//单词最少出现频数
		Map<String, Double> mapTemp = new HashMap<String, Double>();
		
		for (SearchResult searchResult : arrSearchResult)
		{
			for (String szWord : searchResult.lstSegmented)
			{
				if (mapTemp.containsKey(szWord))							//包含单词
				{
					double fCount = mapTemp.get(szWord);
					mapTemp.put(szWord, fCount + 1);						//计数加一
				}
				else
				{
					mapTemp.put(szWord, 1.0);								//计数为一
				}
			}
		}
		
		for (String szWord : mapTemp.keySet())
		{
			if (mapTemp.get(szWord) > MINCOUNT) mapWord.put(szWord, 0.0);	//DF降维，超过指定值的加入单词列表，计数初始为零
		}
		
		for (SearchResult searchResult : arrSearchResult)
		{
			for (String szWord : mapWord.keySet())
			{
				if (searchResult.lstSegmented.contains(szWord))
				{
					double fCount = mapWord.get(szWord);
					mapWord.put(szWord, fCount + 1);						//计数加一
				}
			}
		}
	}
}