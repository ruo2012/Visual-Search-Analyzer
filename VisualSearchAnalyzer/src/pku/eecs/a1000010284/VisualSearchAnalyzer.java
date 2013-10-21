package pku.eecs.a1000010284;

import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JList;
import javax.swing.JScrollPane;
import java.awt.Rectangle;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualSearchAnalyzer extends JFrame {
	static Map<Integer, Vector<Integer> > mapCluster = new HashMap<Integer, Vector<Integer> >();//各个聚类的成员
	static Map<String, SearchResult> mapSearchResult = new HashMap<String, SearchResult>();
	static Map<String, Double> mapWord;
	
	static SearchResult[] arrSearchResult;
	
	static final long serialVersionUID = 1L;
	final static String
		BING_APP_ID = "070A7926696BA6380FA16F43A6F7539121646EB3",				//注册的Bing App ID
		GOOGLE_APP_ID = "AIzaSyC0M9f7DULWgg6Cjc3DkesKLMVvzg1Ecw0",				//注册的Google App ID
		GOOGLE_CSE_ID = "000468045191923653790:fipigt6yjw8";					//注册的Google Custom Search ID
	SearchEngine google, bing, baidu, yahoo;									//搜索引擎
	static Monitor monitor;														//监视爬虫线程
	static int K;																//聚类个数
	static boolean bDownloading;												//正在下载，正在聚类
	
	private JPanel jContentPane = null;
	private JLabel lblKeyword = null;
	private JLabel lblCount = null;
	private JCheckBox chkGoogle = null;
	private JCheckBox chkBing = null;
	private JCheckBox chkBaidu = null;
	private JCheckBox chkYahoo = null;
	private JButton btnSearch = null;
	private JCheckBox chkSogou = null;
	private JLabel lblSearch = null;
	private JLabel lblSogouHost = null;
	private JLabel lblSogouPort = null;
	private JLabel lblGaePort = null;
	private JLabel lblGaeHost = null;
	private JCheckBox chkGae = null;
	private JTextField txtCount = null;
	private JTextField txtSogouPort = null;
	private JTextField txtGaePort = null;
	private JTextField txtKeyword = null;
	private JTextField txtSogouHost = null;
	private JTextField txtGaeHost = null;
	private JScrollPane panResult = null;
	private JList lstResult = null;
	DefaultListModel dlmResult = null;
	private JLabel lblK = null;
	private JTextField txtK = null;
	private class Monitor extends Thread
	{
		@Override
		public void run()
		{
			boolean bStopped;
			while (bDownloading)												//正在下载
			{
				bStopped = true;
				if (chkGoogle.isSelected()) bStopped &= google.isTerminated();
				if (chkBing.isSelected()) bStopped &= bing.isTerminated();
				if (chkBaidu.isSelected()) bStopped &= baidu.isTerminated();
				if (chkYahoo.isSelected()) bStopped &= yahoo.isTerminated();
				
				if (bStopped) bDownloading = false;								//一旦运行线程都结束
			}
			
			SwingUtilities.invokeLater(new Runnable() {							//线程更新GUI  
                public void run() {  
                	btnSearch.setEnabled(false);
                	dlmResult.addElement("Downloading Completed.");
        			dlmResult.addElement("Creating Vectors...");
                }  
            });				
			
			arrSearchResult = mapSearchResult.values().toArray(new SearchResult[0]);//搜索结果数组
			TfIdfVector.getTfIdf();
			
			
			SwingUtilities.invokeLater(new Runnable() {							//线程更新GUI  
                public void run() {  
                	dlmResult.addElement("All Vectors Created.");
        			dlmResult.addElement("Clustering....");
                }  
            });	
			
			
			KMeans.cluster(K);													//聚类
			
			SwingUtilities.invokeLater(new Runnable() {							//线程更新GUI  
                public void run() {  
                	dlmResult.addElement("Clustering Completed.");
        			dlmResult.addElement("Outputing...");
                }  
            });	
			
			
			for (int i = 0; i < K; ++i)											//创建各目录
			{
				if (mapCluster.get(i) == null) continue;
				File dir = new File(Integer.toString(i));
				dir.mkdirs();
				for (File file : dir.listFiles()) file.delete();				//删除各文件
				
				try																//输出统计
				{
					FileOutputStream stream = new FileOutputStream(new File(i + "/info.txt"));
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "utf-8"));
					
					writer.write(mapCluster.get(i).size());
					for (String szWord : mapWord.keySet()) writer.write("\t" + szWord);
					writer.write("\r\n");
					
					for (int x : mapCluster.get(i))
					{
						writer.write(arrSearchResult[x].toString());
						for (String szWord : mapWord.keySet())
						{
							double fTfIdf = 0;
							if (arrSearchResult[x].mapTfIdf.containsKey(szWord)) fTfIdf = arrSearchResult[x].mapTfIdf.get(szWord);
							writer.write("\t" + fTfIdf);
						}
						writer.write("\r\n");
					}
					
					writer.flush();
					writer.close();
				}
				catch (IOException e) { }
			}
			
			for (SearchResult searchResult : arrSearchResult) searchResult.output();
			
			
			SwingUtilities.invokeLater(new Runnable() {							//线程更新GUI  
                public void run() {  
                	dlmResult.addElement("Output Completed.");
                	btnSearch.setEnabled(true);
                }  
            });	
			
			VisualSearchAnalyzer.this.stop();
		}
	}

	protected void start()
	{
		mapSearchResult.clear();												//清空已下载搜索结果
		String szKeyword, szCount, szSogouHost, szSogouPort, szGaeHost, szGaePort, szK;
		int nCount, nSogouPort, nGaePort;
		
		SearchProxy sogouProxy, gaeProxy;
		
		sogouProxy = gaeProxy = null;		
		szKeyword = txtKeyword.getText();					
		if (szKeyword.equals(""))												//关键词不能为空
		{
			JOptionPane.showMessageDialog(null, "Please input keyword", "Search Analyzer", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		try
		{
			szCount = txtCount.getText();										//个数必须为正整数
			nCount = Integer.parseInt(szCount);
			if (nCount <= 0) throw new Exception();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Please input valid count", "Search Analyzer", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		try
		{
			szK = txtK.getText();												//聚类必须为正整数且小于COUNT
			K = Integer.parseInt(szK);
			if (K <= 0 || K >= nCount) throw new Exception();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Please input valid K", "Search Analyzer", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (chkSogou.isSelected())												//启用sogou代理
		{
			szSogouHost = txtSogouHost.getText();
			if (szSogouHost.equals(""))
			{
				JOptionPane.showMessageDialog(null, "Please input Sogou Proxy Host", "Search Analyzer", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			try
			{
				szSogouPort = txtSogouPort.getText();
				nSogouPort = Integer.parseInt(szSogouPort);
				if (nSogouPort <= 0) throw new Exception();
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(null, "Please input valid Sogou Proxy Port", "Search Analyzer", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			sogouProxy = new SearchProxy(szSogouHost, nSogouPort);
		}
		
		if (chkGae.isSelected())												//启用gae代理
		{
			szGaeHost = txtGaeHost.getText();
			if (szGaeHost.equals(""))
			{
				JOptionPane.showMessageDialog(null, "Please input GAE Proxy Host", "Search Analyzer", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			try
			{
				szGaePort = txtGaePort.getText();
				nGaePort = Integer.parseInt(szGaePort);
				if (nGaePort <= 0) throw new Exception();
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(null, "Please input valid GAE Proxy Port", "Search Analyzer", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			gaeProxy = new SearchProxy(szGaeHost, nGaePort);
		}
		
		txtKeyword.setEnabled(false);											//设置控件属性
		txtCount.setEnabled(false);
		txtK.setEnabled(false);
		
		chkGoogle.setEnabled(false);
		chkBing.setEnabled(false);
		chkBaidu.setEnabled(false);
		chkYahoo.setEnabled(false);
		
		chkSogou.setEnabled(false);
		txtSogouHost.setEnabled(false);
		txtSogouPort.setEnabled(false);
		
		chkGae.setEnabled(false);
		txtGaeHost.setEnabled(false);
		txtGaePort.setEnabled(false);
		
		btnSearch.setText("Stop");

		dlmResult.clear();														//清空列表
		
		if (chkGoogle.isSelected())												//启动各引擎
		{
			google = new Google(GOOGLE_APP_ID, GOOGLE_CSE_ID, gaeProxy);
			google.search(szKeyword, nCount);
		}
		
		
		if (chkBing.isSelected())
		{
			bing = new Bing(BING_APP_ID, gaeProxy);
			bing.search(szKeyword, nCount);
		}
		
		if (chkBaidu.isSelected())
		{
			baidu = new Baidu(sogouProxy);
			baidu.search(szKeyword, nCount);
		}
		
		if (chkYahoo.isSelected())
		{
			yahoo = new Yahoo(gaeProxy);
			yahoo.search(szKeyword, nCount);
		}
		
		bDownloading = true;													//正在下载不在聚类
		
		monitor = new Monitor();												//启动监视线程
		monitor.setDaemon(true);
		monitor.start();
	}

	protected void stop() {
		// TODO Auto-generated method stub
				
		SwingUtilities.invokeLater(new Runnable() {							//线程更新GUI  
            public void run()
            {  
				txtKeyword.setEnabled(true);								//设置控件属性
				txtCount.setEnabled(true);
				txtK.setEnabled(true);
				
				chkGoogle.setEnabled(true);
				chkBing.setEnabled(true);
				chkBaidu.setEnabled(true);
				chkYahoo.setEnabled(true);
				
				chkSogou.setEnabled(true);
				txtSogouHost.setEnabled(chkSogou.isSelected());
				txtSogouPort.setEnabled(chkSogou.isSelected());
				
				chkGae.setEnabled(true);
				txtGaeHost.setEnabled(chkGae.isSelected());
				txtGaePort.setEnabled(chkGae.isSelected());
		
				btnSearch.setText("Start!");
            }
		});
				
		if (chkGoogle.isSelected()) google.close();						//关闭各引擎
		if (chkBing.isSelected()) bing.close();
		if (chkBaidu.isSelected()) baidu.close();
		if (chkYahoo.isSelected()) yahoo.close();

	}

	
	/**
	 * This method initializes chkGoogle	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getChkGoogle() {
		if (chkGoogle == null) {
			chkGoogle = new JCheckBox();
			chkGoogle.setText("Google");
			chkGoogle.setBounds(new Rectangle(89, 21, 64, 24));
			chkGoogle.setSelected(true);
		}
		return chkGoogle;
	}

	/**
	 * This method initializes chkBing	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getChkBing() {
		if (chkBing == null) {
			chkBing = new JCheckBox();
			chkBing.setText("Bing");
			chkBing.setBounds(new Rectangle(153, 21, 50, 24));
			chkBing.setSelected(true);
		}
		return chkBing;
	}

	/**
	 * This method initializes chkBaidu	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getChkBaidu() {
		if (chkBaidu == null) {
			chkBaidu = new JCheckBox();
			chkBaidu.setSelected(true);
			chkBaidu.setBounds(new Rectangle(203, 21, 57, 24));
			chkBaidu.setText("Baidu");
		}
		return chkBaidu;
	}

	/**
	 * This method initializes chkYahoo	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getChkYahoo() {
		if (chkYahoo == null) {
			chkYahoo = new JCheckBox();
			chkYahoo.setText("Yahoo");
			chkYahoo.setBounds(new Rectangle(260, 21, 60, 24));
			chkYahoo.setSelected(true);
		}
		return chkYahoo;
	}

	/**
	 * This method initializes btnSearch	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnSearch() {
		if (btnSearch == null) {
			btnSearch = new JButton();
			btnSearch.setText("Start!");
			btnSearch.setLocation(new Point(325, 20));
			btnSearch.setSize(new Dimension(65, 26));
			
			btnSearch.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent arg0) {
					// TODO Auto-generated method stub
					if (btnSearch.getText().equals("Start!")) start();
					else bDownloading = false;
				}
			});
		}
		return btnSearch;
	}

	/**
	 * This method initializes chkSogou	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getChkSogou() {
		if (chkSogou == null) {
			chkSogou = new JCheckBox();
			chkSogou.setText("Sogou Proxy");
			chkSogou.setBounds(new Rectangle(0, 46, 97, 24));
			chkSogou.setSelected(true);
			chkSogou.addItemListener(new ItemListener()
			{
				@Override
				public void itemStateChanged(ItemEvent arg0) {
					// TODO Auto-generated method stub
					txtSogouHost.setEnabled(chkSogou.isSelected());
					txtSogouPort.setEnabled(chkSogou.isSelected());
				}
			});
		}
		return chkSogou;
	}

	/**
	 * This method initializes chkGae	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getChkGae() {
		if (chkGae == null) {
			chkGae = new JCheckBox();
			chkGae.setText("GAE Proxy");
			chkGae.setLocation(new Point(0, 70));
			chkGae.setSize(new Dimension(97, 24));
			chkGae.setSelected(true);
			chkGae.addItemListener(new ItemListener()
			{
				@Override
				public void itemStateChanged(ItemEvent arg0) {
					// TODO Auto-generated method stub
					txtGaeHost.setEnabled(chkGae.isSelected());
					txtGaePort.setEnabled(chkGae.isSelected());
				}
			});
		}
		return chkGae;
	}

	/**
	 * This method initializes txtCount	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getTxtCount() {
		if (txtCount == null) {
			txtCount = new JTextField(5);
			txtCount.setSize(new Dimension(36, 20));
			txtCount.setLocation(new Point(308, 0));
		}
		return txtCount;
	}

	/**
	 * This method initializes txtSogouPort	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getTxtSogouPort() {
		if (txtSogouPort == null) {
			txtSogouPort = new JTextField(5);
			txtSogouPort.setText("8081");
			txtSogouPort.setBounds(new Rectangle(332, 46, 59, 24));
		}
		return txtSogouPort;
	}

	/**
	 * This method initializes txtGaePort	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getTxtGaePort() {
		if (txtGaePort == null) {
			txtGaePort = new JTextField(5);
			txtGaePort.setText("8086");
			txtGaePort.setBounds(new Rectangle(332, 70, 59, 24));
		}
		return txtGaePort;
	}

	/**
	 * This method initializes txtKeyword	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getTxtKeyword() {
		if (txtKeyword == null) {
			txtKeyword = new JTextField(25);
			txtKeyword.setBounds(new Rectangle(56, 0, 214, 20));
		}
		return txtKeyword;
	}

	/**
	 * This method initializes txtSogouHost	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getTxtSogouHost() {
		if (txtSogouHost == null) {
			txtSogouHost = new JTextField(14);
			txtSogouHost.setText("127.0.0.1");
			txtSogouHost.setBounds(new Rectangle(139, 46, 148, 24));
		}
		return txtSogouHost;
	}

	/**
	 * This method initializes txtGaeHost	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getTxtGaeHost() {
		if (txtGaeHost == null) {
			txtGaeHost = new JTextField(14);
			txtGaeHost.setText("127.0.0.1");
			txtGaeHost.setSize(new Dimension(148, 24));
			txtGaeHost.setLocation(new Point(139, 70));
		}
		return txtGaeHost;
	}

	/**
	 * This method initializes panResult	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getPanResult() {
		if (panResult == null) {
			panResult = new JScrollPane();
			panResult.setViewportView(getLstResult());
			panResult.setBounds(new Rectangle(0, 94, 391, 183));
		}
		return panResult;
	}

	/**
	 * This method initializes lstResult	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList getLstResult() {
		if (lstResult == null) {
			lstResult = new JList();
			dlmResult = new DefaultListModel();
			
			lstResult.setModel(dlmResult);
			SearchEngine.setList(dlmResult);
		}
		return lstResult;
	}

	/**
	 * This method initializes txtK	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getTxtK() {
		if (txtK == null) {
			txtK = new JTextField(5);
			txtK.setSize(new Dimension(25, 20));
			txtK.setLocation(new Point(364, 0));
		}
		return txtK;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		StopWords.init();													//初始化停用词表
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				VisualSearchAnalyzer thisClass = new VisualSearchAnalyzer();
				thisClass.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				thisClass.setVisible(true);
			}
		});
	}

	/**
	 * This is the default constructor
	 */
	public VisualSearchAnalyzer() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(396, 305);
		this.setResizable(false);
		this.setContentPane(getJContentPane());
		this.setTitle("Visual Search Analyzer");
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			lblK = new JLabel();
			lblK.setText("K:");
			lblK.setLocation(new Point(348, 2));
			lblK.setSize(new Dimension(16, 16));
			lblGaeHost = new JLabel();
			lblGaeHost.setText("Host:");
			lblGaeHost.setBounds(new Rectangle(102, 74, 29, 16));
			lblGaePort = new JLabel();
			lblGaePort.setText("Port:");
			lblGaePort.setLocation(new Point(290, 74));
			lblGaePort.setPreferredSize(new Dimension(32, 16));
			lblGaePort.setSize(new Dimension(32, 16));
			lblSogouPort = new JLabel();
			lblSogouPort.setText("Port:");
			lblSogouPort.setLocation(new Point(290, 50));
			lblSogouPort.setPreferredSize(new Dimension(32, 16));
			lblSogouPort.setSize(new Dimension(32, 16));
			lblSogouHost = new JLabel();
			lblSogouHost.setText("Host:");
			lblSogouHost.setBounds(new Rectangle(102, 50, 29, 16));
			lblSearch = new JLabel();
			lblSearch.setText("Search Engine:");
			lblSearch.setBounds(new Rectangle(0, 25, 89, 16));
			lblSearch.setHorizontalTextPosition(SwingConstants.TRAILING);
			lblCount = new JLabel();
			lblCount.setText("Count:");
			lblCount.setLocation(new Point(270, 2));
			lblCount.setSize(new Dimension(36, 16));
			lblKeyword = new JLabel();
			lblKeyword.setText("Keyword:");
			lblKeyword.setBounds(new Rectangle(0, 2, 53, 16));
			jContentPane = new JPanel();
			jContentPane.setLayout(null);
			jContentPane.add(lblKeyword, null);
			jContentPane.add(lblCount, null);
			jContentPane.add(getChkGoogle(), null);
			jContentPane.add(getChkBing(), null);
			jContentPane.add(getChkBaidu(), null);
			jContentPane.add(getChkYahoo(), null);
			jContentPane.add(getBtnSearch(), null);
			jContentPane.add(getChkSogou(), null);
			jContentPane.add(lblSearch, null);
			jContentPane.add(lblSogouHost, null);
			jContentPane.add(lblSogouPort, null);
			jContentPane.add(lblGaePort, null);
			jContentPane.add(lblGaeHost, null);
			jContentPane.add(getChkGae(), null);
			jContentPane.add(getTxtCount(), null);
			jContentPane.add(getTxtSogouPort(), null);
			jContentPane.add(getTxtGaePort(), null);
			jContentPane.add(getTxtKeyword(), null);
			jContentPane.add(getTxtSogouHost(), null);
			jContentPane.add(getTxtGaeHost(), null);
			jContentPane.add(getPanResult(), null);
			jContentPane.add(lblK, null);
			jContentPane.add(getTxtK(), null);
		}
		return jContentPane;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"


class SearchResult									//搜索结果类，保存搜索条目信息
{
	List<String> lstSegmented;
	Map<String, Double> mapTfIdf = new HashMap<String, Double>();//TF-IDF表
	
	String szSearchEngine, szUrl, szHtml;
	int nCount, nMean;
	
	public SearchResult(String szSearchEngine, int nCount, String szUrl, List<String> lstSegmented, String szHtml)
	{
		this.szSearchEngine = szSearchEngine;
		this.nCount = nCount;
		this.szUrl = szUrl;
		this.lstSegmented = lstSegmented;
		this.szHtml = szHtml;
	}
	
	public void output()
	{
		try
		{
			String szEncoding = "utf-8";						//默认UTF-8保存
			
			Pattern pat = Pattern.compile("charset=[^>]*");
			Matcher mat = pat.matcher(szHtml);
			if (mat.find())
			{
				szEncoding = mat.group().toString().substring(8);
				pat = Pattern.compile("[a-zA-Z0-9\\-]+");
				mat = pat.matcher(szEncoding);
				
				if (mat.find()) szEncoding = mat.group().toString();//获取编码
			}
			
			FileOutputStream stream = new FileOutputStream(new File(nMean + "/" + this + ".html"));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, szEncoding));
			
			writer.write(szHtml);								//保存HTML
			
			writer.flush();
			writer.close();
		}
		catch (IOException e) { }
	}
	
	@Override
	public String toString()
	{
		return szSearchEngine + "_" + nCount;
	}
}
