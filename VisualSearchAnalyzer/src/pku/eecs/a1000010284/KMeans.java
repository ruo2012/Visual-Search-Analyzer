package pku.eecs.a1000010284;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

public class KMeans
{
	private static int K, nResult;
	private static Map<Integer, Map<String, Double> > mapMean = new HashMap<Integer, Map<String, Double> >();//������������ĵ�����
	private static Map<Integer, Vector<Integer> > mapCluster;
	private static int[] arrAssignMean;												//�����������Ӧ�ľ���
	private static double [][] arrDist;												//����
	
	private static Map<String, Double> mapWord;										//���ʱ�
	private static SearchResult[] arrSearchResult;									//�������
	
	
	public static void cluster(int K_)
	{
		final int MAXITER = 25;
		int i, nIter;
		
		K = K_;
		mapCluster = VisualSearchAnalyzer.mapCluster;
		mapWord = VisualSearchAnalyzer.mapWord;
		arrSearchResult = VisualSearchAnalyzer.arrSearchResult;
		
		
		mapMean.clear();															//��ո�����
		mapCluster.clear();
		nResult = arrSearchResult.length;
		
		arrAssignMean = new int[nResult];
		arrDist = new double[nResult][K];
		
		Random random = new Random();
		Set<Integer> setInit = new HashSet<Integer>();
		while (setInit.size() < K) setInit.add(Math.abs(random.nextInt())  % nResult);
		
		i = 0;
		for (int x : setInit) mapMean.put(i++, arrSearchResult[x].mapTfIdf);
		
		for (nIter = 0; nIter < MAXITER; ++nIter)
		{
			for (i = 0; i < nResult; ++i)
			{
				for (int j = 0; j < K; ++j) arrDist[i][j] = getDist(arrSearchResult[i].mapTfIdf, mapMean.get(j));
			}																		//�������
			
			int[] arrNearestMean = new int[nResult];								//��������ľ�������
			for (i = 0; i < nResult; ++i) arrNearestMean[i] = getNearestMean(i);	//�������
			
			int nCount = 0;
			for (i = 0; i < nResult; ++i) if (arrAssignMean[i] == arrNearestMean[i]) ++nCount;
			if (nCount == nResult) break;											//ȫ��������ľ�����
			
			mapCluster.clear();
			
			for (i = 0; i < nResult; ++i)
			{
				arrAssignMean[i] = arrNearestMean[i];
				
				if (mapCluster.containsKey(arrNearestMean[i]))						//�Ѿ��������
				{
					Vector<Integer> vecMean = mapCluster.get(arrNearestMean[i]);
					vecMean.add(i);													//�����һ����
				}
				else
				{
					Vector<Integer> vecMean = new Vector<Integer>();
					vecMean.add(i);
					mapCluster.put(arrNearestMean[i], vecMean);						//������¾���
				}
			}
			
			mapMean.clear();
			
			for (i = 0; i < K; ++i)
			{
				if (!mapCluster.containsKey(i)) continue;							//�����վ���
				mapMean.put(i, getNewMean(i));										//�������
			}
		}
		
		for (i = 0; i < nResult; ++i) arrSearchResult[i].nMean = arrAssignMean[i];	//ȷ�����ھ���
	}
	
	private static double getCos(Map<String, Double> vector1, Map<String, Double> vector2)
	{
		double fProduct = 0.0, fAbs1 = 0.0, fAbs2 = 0.0;							//��������������нǵ�����
		
		for (String szWord : mapWord.keySet())
		{
			if (!vector1.containsKey(szWord) || !vector2.containsKey(szWord)) continue;
			
			double fTfIdf1 = vector1.get(szWord);
			double fTfIdf2 = vector2.get(szWord);
			
			fProduct += fTfIdf1 * fTfIdf2;
			fAbs1 += fTfIdf1 * fTfIdf1;
			fAbs2 += fTfIdf2 * fTfIdf2;
		}
		
		fAbs1 = Math.sqrt(fAbs1);
		fAbs2 = Math.sqrt(fAbs2);
		
		return fProduct / (fAbs1 * fAbs2);
	}
	
	private static double getDist(Map<String, Double> vector1, Map<String, Double> vector2)
	{
		if (vector1 == null || vector2 == null) return 4.0;
		return 1 - getCos(vector1, vector2);										//1��ȥ���Ҽ�Ϊ����
	}
	
	private static int getNearestMean(int x)
	{
		int nMin = 0;
		
		for (int i = 1; i < K; ++i)
		{
			if (arrDist[x][nMin] > arrDist[x][i]) nMin = i;
		}
		
		return nMin;
	}
	
	private static Map<String, Double> getNewMean(int i)							//�����¾������������
	{
		Vector<Integer> vecMean = mapCluster.get(i);
		double fLength = vecMean.size();
		
		Map<String, Double> mapNewMean = new HashMap<String, Double>();
		
		for (String szWord : mapWord.keySet())
		{			
			double fCount = 0.0;
			for (int x : vecMean) 
			{
				if (!arrSearchResult[x].mapTfIdf.containsKey(szWord)) continue;
				double fAdd = arrSearchResult[x].mapTfIdf.get(szWord);
				fCount += fAdd;
			}
			mapNewMean.put(szWord, fCount / fLength);								//���ֵ
		}
		
		return mapNewMean;
	}
}
