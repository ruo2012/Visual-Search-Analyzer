package pku.eecs.a1000010284;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class StopWords									//ͣ�ô�
{
	private static Set<String> setStopWord = new HashSet<String>();
	private static final String FILENAME = "stopwords.txt";
	
	
	public static void init()							//����ͣ�ô�
	{
		try
		{
			File file = new File(FILENAME);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String szStopWord;
			while ((szStopWord = bufferedReader.readLine()) != null) setStopWord.add(szStopWord);
			
			bufferedReader.close();
			fileReader.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static boolean isStopWord(String szWord)		//���ͣ�ô�
	{
		return setStopWord.contains(szWord);
	}
	
}
