import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;


public class CCacheSim extends JFrame implements ActionListener{
	private static final long serialVersionUID = 1L;
	
	private JPanel panelTop, panelLeft, panelRight, panelBottom;
	private JButton execStepBtn, execAllBtn, fileBotton, restartBtn;
	@SuppressWarnings("rawtypes")
	private JComboBox csBox, bsBox, wayBox, replaceBox, prefetchBox, writeBox, allocBox;
	@SuppressWarnings("rawtypes")
	private JComboBox dcsBox, icsBox;	//数据Cache大小和指令Cache大小
	private JRadioButton unifiedCacheButton, separateCacheButton;
	private JFileChooser fileChoose;
	
	private JLabel labelTop,labelLeft,rightLabel,bottomLabel,fileLabel,fileAddrLabel, 
		    csLabel, bsLabel, wayLabel, replaceLabel, prefetchLabel, writeLabel, allocLabel;
	private JLabel dcsLabel, icsLabel;
//	private JLabel results[];
	private JLabel resultTagLabel[][];
	private JLabel resultDataLabel[][];
	private JLabel accessTypeTagLabel, addressTagLabel, blockNumberTagLabel,  indexTagLabel, inblockAddressTagLabel, hitTagLabel;
	private JLabel accessTypeDataLabel, addressDataLabel, blockNumberDataLabel, tagDataLabel, indexDataLabel, inblockAddressDataLabel,hitDataLabel;


    //参数定义
	private String cachesize[] = { "2KB", "8KB", "32KB", "128KB", "512KB", "2MB" };
	private String scachesize[] = {"1KB", "4KB", "16KB", "64KB", "256KB", "1MB" };
	private String blocksize[] = { "16B", "32B", "64B", "128B", "256B" };
	private String way[] = { "直接映象", "2路", "4路", "8路", "16路", "32路" };
	private String replace[] = { "LRU", "FIFO", "RAND" };
	private String pref[] = { "不预取", "不命中预取" };
	private String write[] = { "写回法", "写直达法" };
	private String alloc[] = { "按写分配", "不按写分配" };
	private String typename[] = { "读数据", "写数据", "读指令" };
//	private String hitname[] = {"不命中", "命中" };
	
	
	//右侧结果显示
//	private String rightLable[]={"访问总次数：","读指令次数：","读数据次数：","写数据次数："};
	private String resultTags[][] = {
			{"访问总次数:", "不命中次数", "不命中率:"},
			{"读指令次数:", "不命中次数", "不命中率:"},
			{"读指令次数:", "不命中次数", "不命中率:"},
			{"写数据次数:", "不命中次数", "不命中率:"}
	};
	
	//打开文件
	private File file;
	
	//分别表示左侧几个下拉框所选择的第几项，索引从 0 开始
	private int csIndex, bsIndex, wayIndex, replaceIndex, prefetchIndex, writeIndex, allocIndex, dcsIndex, icsIndex;
	
	private final int MAX_INSTRUCTION = 10000000;
	private int cacheType = 0;
	//其它变量定义
	//...
	//自定义变量及函数
	
	private static String toBinary(int num){
		char[] binaryString = new char[32];
		int i = 32;
		char[] digits = {'0','1'};
		do{
			binaryString[--i] = digits[num & 1]; 
			num = num >>> 1;
		}while (i>0);
		return new String(binaryString);
	}
	
	private class Instruction{
		int type;
		int tag;
		int index;
		int blockAddr;
		int offset;
		String addr;
		
		public Instruction(int type, String addr){
			this.type = type;
			this.addr = addr;
			
			int temp = Integer.parseInt(addr, 16);
			String binaryAddr = toBinary(temp);
			
			if(cacheType == 0 && unifiedCache != null){
				this.tag = Integer.parseInt(binaryAddr.substring(0, 32-unifiedCache.blockOffset-unifiedCache.setOffset),2);
				this.index = Integer.parseInt(binaryAddr.substring(32-unifiedCache.blockOffset-unifiedCache.setOffset, 32-unifiedCache.blockOffset), 2);
				this.blockAddr = Integer.parseInt(binaryAddr.substring(0, 32-unifiedCache.blockOffset), 2);
				this.offset = Integer.parseInt(binaryAddr.substring(32-unifiedCache.blockOffset), 2);
			}
			if(cacheType == 1 && dataCache !=null && instructionCache != null){
				if(type == 0 || type == 1){
					this.tag = Integer.parseInt(binaryAddr.substring(0, 32-dataCache.blockOffset-dataCache.setOffset),2);
					this.index = Integer.parseInt(binaryAddr.substring(32-dataCache.blockOffset-dataCache.setOffset, 32-dataCache.blockOffset), 2);
					this.blockAddr = Integer.parseInt(binaryAddr.substring(0, 32-dataCache.blockOffset), 2);
					this.offset = Integer.parseInt(binaryAddr.substring(32-dataCache.blockOffset), 2);
				}
				else if(type == 2){
					this.tag = Integer.parseInt(binaryAddr.substring(0, 32-instructionCache.blockOffset-instructionCache.setOffset),2);
					this.index = Integer.parseInt(binaryAddr.substring(32-instructionCache.blockOffset-instructionCache.setOffset, 32-instructionCache.blockOffset), 2);
					this.blockAddr = Integer.parseInt(binaryAddr.substring(0, 32-instructionCache.blockOffset), 2);
					this.offset = Integer.parseInt(binaryAddr.substring(32-instructionCache.blockOffset), 2);
				}
			}
		}
	}
	
	private Instruction instructions[];
	private int isize;
	private int pc;
	
	
	/*
	 * cache block 类
	 */
	private class CacheBlock{
		int tag;
		boolean dirty;
		int usedtime;
		int reachtime;
		
		public CacheBlock(int tag){
			this.tag = tag;
			dirty = false;
			usedtime = -1;
			reachtime = -1;
		}
	}
	
	private int log2(int x){
		return (int)(Math.log(x)/Math.log(2));
	}
	
	/*
	 * cache 类
	 */
	private class Cache{
		private CacheBlock cache[][];
		private int cacheSize;
		private int blockSize;
		private int blockNum;
		private int blockOffset;
		private int blockPerSet;
		private int setNum;
		private int setOffset;
		
		public Cache(int csize, int bsize){
			cacheSize = csize;
			blockSize = bsize;
			
			blockNum = cacheSize / blockSize;
			blockOffset = log2(blockSize);
			
			blockPerSet = 1 << wayIndex;
			setNum = blockNum / blockPerSet;
			setOffset = log2(setNum);
			
			cache = new CacheBlock[setNum][blockPerSet];
			
			for(int i=0; i<setNum; i++){
				for(int j=0; j<blockPerSet; j++){
					cache[i][j] = new CacheBlock(-1);
				}
			}
			
		}
		
		public boolean read(int tag, int index){
			for(int i=0; i<blockPerSet; i++){
				if(cache[index][i].tag == tag){		//命中
					cache[index][i].usedtime = pc % isize;
					return true;
				}
			}
			return false;
		}
		
		public boolean write(int tag, int index){
			for(int i=0; i<blockPerSet; i++){
				if(cache[index][i].tag == tag){		//命中
					cache[index][i].usedtime = pc % isize;
					if(writeIndex == 0){			//写回法
						cache[index][i].dirty = true;
					}
					else if(writeIndex == 1){		//写直达法
						memoryWriteTime++;
					}
					return true;
				}
			}
			return false;
		}
		
		public void prefetch(int nextBlockAddr){
			int nextTag = nextBlockAddr / (1<<setOffset);
			int nextIndex = nextBlockAddr / (1<<blockOffset) % (1<<setOffset);
			
			replace(nextTag, nextIndex);
		}
		
		public void replace(int tag, int index){
			int toReplace = 0;
			if(replaceIndex == 0){		//LRU
				for(int i=1; i<blockPerSet; i++){
					if(cache[index][toReplace].usedtime>cache[index][i].usedtime){
						toReplace = i;
					}
				}
			}
			else if(replaceIndex == 1){		//FIFO
				for(int i=1; i<blockPerSet; i++){
					if(cache[index][toReplace].reachtime>cache[index][i].reachtime){
						toReplace = i;
					}
				}
			}
			else if(replaceIndex == 2){		//RAND
				toReplace = (int)Math.random()*blockPerSet;
			}
			loadBlock(tag,index,toReplace);
		}
		
		private void loadBlock(int tag, int index, int offset){
			if(writeIndex == 0 && cache[index][offset].dirty){
				//替换前先写回数据
				memoryWriteTime++;
			}
			
			cache[index][offset].tag = tag;
			cache[index][offset].usedtime = pc % isize;
			cache[index][offset].dirty = false;
			cache[index][offset].reachtime = pc % isize;
		}
		
		public void description(){
			System.out.println("cacheSize = " + cacheSize);
			System.out.println("blockSize = " + blockSize);
			System.out.println("blockNum = " + blockNum);
			System.out.println("blockOffset = " + blockOffset);
			System.out.println("blockPerSet = " + blockPerSet);
			System.out.println("setNum = " + setNum);
			System.out.println("setOffset = " + setOffset);
		}
	} 
	
	Cache unifiedCache, dataCache, instructionCache;
	
	private int readDataMissTime, readInstructionMissTime, readDataHitTime, readInstructionHitTime;
	private int writeDataHitTime, writeDataMissTime;
	private int memoryWriteTime;
	
	
	
	/*
	 * 构造函数，绘制模拟器面板
	 */
	public CCacheSim(){
		super("Cache Simulator");
		fileChoose = new JFileChooser();
		draw();
	}
	

	//响应事件，共有三种事件：
	//   1. 执行到底事件
	//   2. 单步执行事件
	//   3. 文件选择事件
	public void actionPerformed(ActionEvent e){
				
		if (e.getSource() == execAllBtn) {
			simExecAll();
		}
		if (e.getSource() == execStepBtn) {
			simExecStep(true);
		}
		if (e.getSource() == restartBtn) {
			initCache();
			readFile();
			reloadUI();
		}
		if (e.getSource() == fileBotton){
			int fileOver = fileChoose.showOpenDialog(null);
			if (fileOver == 0) {
				   String path = fileChoose.getSelectedFile().getAbsolutePath();
				   fileAddrLabel.setText(path);
				   file = new File(path);
				  
				   initCache();
				   readFile();
				   reloadUI();
			}
		}
	}
	
	/*
	 * 初始化 Cache 模拟器
	 */
	public void initCache() {
		readDataMissTime = 0;
		readInstructionMissTime = 0;
		readDataHitTime = 0;
		readInstructionHitTime = 0;
		writeDataMissTime = 0;
		writeDataHitTime = 0;
		memoryWriteTime = 0;
		
		if(cacheType == 0){
			unifiedCache = new Cache(1024*(1<<(2*csIndex+1)), 1<<(bsIndex+4));
			instructionCache = null;
			dataCache = null;
			
			System.out.println("Unified Cache:");
			unifiedCache.description();
		}
		else if(cacheType == 1){
			unifiedCache = null;
			instructionCache = new Cache(1024*(1<<(2*icsIndex)), 1<<(bsIndex+4));
			dataCache = new Cache(1024*(1<<(2*dcsIndex)), 1<<(bsIndex+4));
			
			System.out.println("Instruction Cache:");
			instructionCache.description();
			System.out.println("Data Cache:");
			dataCache.description();
		}
	}
	
	/*
	 * 将指令和数据流从文件中读入
	 */
	public void readFile() {
		try{
			Scanner s = new Scanner(file);
			instructions = new Instruction[MAX_INSTRUCTION];
			isize = 0;
			pc = 0;
			
			while(s.hasNextLine()){
				String line = s.nextLine();
				String[] items = line.split(" ");
				instructions[isize++] = new Instruction(Integer.parseInt(items[0].trim()), items[1].trim());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/*
	 * 重新加载界面
	 */
	private void reloadUI(){
		for(int i=0; i<4; i++){
			for(int j=0; j<2; j++){
				resultDataLabel[i][j].setText("0");
			}
			resultDataLabel[i][2].setText("0.00%");
		}
		
		accessTypeDataLabel.setText("   ");
		addressDataLabel.setText("   ");
		blockNumberDataLabel.setText("   ");
		tagDataLabel.setText("   ");
		indexDataLabel.setText("   ");
		inblockAddressDataLabel.setText("   ");
		hitDataLabel.setText("   ");
	}
	
	
	/*
	 * 模拟单步执行
	 */
	public void simExecStep(boolean oneStepExec) {
		pc %= isize;
		if(pc == 0){
			initCache();
			reloadUI();
		}
		int type = instructions[pc].type;
		int index = instructions[pc].index;
		int tag = instructions[pc].tag;
		
		boolean isHit = false;
		if(cacheType == 0){		//统一Cache
			if(type == 0){		//读数据
				isHit = unifiedCache.read(tag, index);
				if(isHit){
					readDataHitTime++;
				}
				else{
					readDataMissTime++;
					unifiedCache.replace(tag, index);
				}
			}
			else if(type == 1){		//写数据
				isHit = unifiedCache.write(tag, index);
				if(isHit){
					writeDataHitTime++;
				}
				else{
					writeDataMissTime++;
					if(allocIndex == 0){		//按写分配
						unifiedCache.replace(tag, index);
						unifiedCache.write(tag, index);
					}
					else if(allocIndex == 1){	//不按写分配
						memoryWriteTime++;
					}
				}
			}
			else if(type == 2){			//读指令
				isHit = unifiedCache.read(tag, index);
				if(isHit){
					readInstructionHitTime++;
				}
				else{
					readInstructionMissTime++;
					unifiedCache.replace(tag, index);
					if(prefetchIndex == 0){		//不预取
						;
					}
					else if(prefetchIndex == 1){	//不命中预取
						unifiedCache.prefetch(instructions[pc].blockAddr+1);
					}
				}
			}
			
		}
		else if(cacheType == 1){	//独立Cache
			if(type == 0){		//读数据
				isHit = dataCache.read(tag, index);
				if(isHit){
					readDataHitTime++;
				}
				else{
					readDataMissTime++;
					dataCache.replace(tag, index);
				}
			}
			else if(type == 1){		//写数据
				isHit = dataCache.write(tag, index);
				if(isHit){
					writeDataHitTime++;
				}
				else{
					writeDataMissTime++;
					if(allocIndex == 0){		//按写分配
						dataCache.replace(tag, index);
						dataCache.write(tag, index);
					}
					else if(allocIndex == 1){	//不按写分配
						memoryWriteTime++;
					}
				}
			}
			else if(type == 2){		//读指令
				isHit = instructionCache.read(tag, index);
				if(isHit){
					readInstructionHitTime++;
				}
				else{
					readInstructionMissTime++;
					instructionCache.replace(tag, index);
					if(prefetchIndex == 0){		//不预取
						;
					}
					else if(prefetchIndex == 1){
						instructionCache.prefetch(instructions[pc].blockAddr+1);
					}
				}
			}
		}
		if(oneStepExec || pc == isize - 1){
			updateGUI(instructions[pc],isHit);
		}
		pc++;
	}
	
	DecimalFormat df = new DecimalFormat("0.00%");
	
	private void updateGUI(Instruction inst, boolean hit){
		int totalMissTime = readInstructionMissTime + readDataMissTime + writeDataMissTime;
		int totalVisitTime = totalMissTime + readInstructionHitTime + readDataHitTime + writeDataHitTime;
		double missRate;
		resultDataLabel[0][0].setText(totalVisitTime + "");
		resultDataLabel[0][1].setText(totalMissTime + "");
		if(totalVisitTime > 0){
			missRate = ((double) totalMissTime / (double) totalVisitTime);
			resultDataLabel[0][2].setText(df.format(missRate));
		}
		
		resultDataLabel[1][0].setText((readInstructionHitTime + readInstructionMissTime) + "");
		resultDataLabel[1][1].setText(readInstructionMissTime + "");
		if(readInstructionMissTime + readInstructionHitTime > 0){
			missRate = ((double) readInstructionMissTime / (double) (readInstructionMissTime + readInstructionHitTime));
			resultDataLabel[1][2].setText(df.format(missRate));
		}
		
		resultDataLabel[2][0].setText((readDataHitTime + readDataMissTime) + "");
		resultDataLabel[2][1].setText(readDataMissTime + "");
		if(readDataMissTime + readDataHitTime > 0){
			missRate = ((double) readDataMissTime / (double) (readDataMissTime + readDataHitTime));
			resultDataLabel[2][2].setText(df.format(missRate));
		}
		
		resultDataLabel[3][0].setText((writeDataHitTime + writeDataMissTime) + "");
		resultDataLabel[3][1].setText(writeDataMissTime + "");
		if(writeDataMissTime + writeDataHitTime > 0){
			missRate = ((double) writeDataMissTime / (double) (writeDataMissTime + writeDataHitTime));
			resultDataLabel[3][2].setText(df.format(missRate));
		}
		accessTypeDataLabel.setText(typename[inst.type]);
		addressDataLabel.setText(inst.addr);
		blockNumberDataLabel.setText(inst.blockAddr + "");
		tagDataLabel.setText(inst.tag + "");
		indexDataLabel.setText(inst.index + "");
		inblockAddressDataLabel.setText(inst.offset + "");
		
		if(hit){
			hitDataLabel.setText("命中");
		}
		else{
			hitDataLabel.setText("不命中");
		}
	}
	
	
	
	/*
	 * 模拟执行到底
	 */
	public void simExecAll() {
		while(pc < isize){
			simExecStep(false);
		}
		System.out.println(isize);
	}

	
	public static void main(String[] args) {
		new CCacheSim();
	}
	
	private void unifiedCacheEnabled(boolean enabled){
		unifiedCacheButton.setSelected(enabled);
		csLabel.setEnabled(enabled);
		csBox.setEnabled(enabled);
	}
	
	private void separateCacheEnabled(boolean enabled){
		separateCacheButton.setSelected(enabled);
		icsLabel.setEnabled(enabled);
		dcsLabel.setEnabled(enabled);
		icsBox.setEnabled(enabled);
		dcsBox.setEnabled(enabled);
	}
	
	/**
	 * 绘制 Cache 模拟器图形化界面
	 * 无需做修改
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void draw() {
		//模拟器绘制面板
		setLayout(new BorderLayout(5,5));
		panelTop = new JPanel();
		panelLeft = new JPanel();
		panelRight = new JPanel();
		panelBottom = new JPanel();
		panelTop.setPreferredSize(new Dimension(800, 50));
		panelLeft.setPreferredSize(new Dimension(300, 450));
		panelRight.setPreferredSize(new Dimension(500, 450));
		panelBottom.setPreferredSize(new Dimension(800, 100));
		panelTop.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelLeft.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelRight.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelBottom.setBorder(new EtchedBorder(EtchedBorder.RAISED));

		//*****************************顶部面板绘制*****************************************//
		labelTop = new JLabel("Cache Simulator");
		labelTop.setAlignmentX(CENTER_ALIGNMENT);
		panelTop.add(labelTop);

		
		//*****************************左侧面板绘制*****************************************//
		labelLeft = new JLabel("Cache 参数设置");
		labelLeft.setPreferredSize(new Dimension(300, 40));
		
		//cache 大小设置
		csLabel = new JLabel("总大小");
		csLabel.setPreferredSize(new Dimension(80, 30));
		csBox = new JComboBox(cachesize);
		csBox.setPreferredSize(new Dimension(90, 30));
		csBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				csIndex = csBox.getSelectedIndex();
			}
		});
		
		//cache 种类
		unifiedCacheButton = new JRadioButton("统一Cache:",true);
		unifiedCacheButton.setPreferredSize(new Dimension(100,30));
		unifiedCacheButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				separateCacheEnabled(false);
				unifiedCacheEnabled(true);
				cacheType = 0;
			}
		});
		
		separateCacheButton = new JRadioButton("独立Cache:");
		separateCacheButton.setPreferredSize(new Dimension(100,30));
		separateCacheButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				separateCacheEnabled(true);
				unifiedCacheEnabled(false);
				cacheType = 1;
			}
		});
		
		dcsLabel = new JLabel("数据Cache：");
		dcsLabel.setPreferredSize(new Dimension(80, 30));
		
		icsLabel = new JLabel("指令Cache：");
		icsLabel.setPreferredSize(new Dimension(80, 30));
		
		JLabel emptyLabel = new JLabel("");
		emptyLabel.setPreferredSize(new Dimension(100, 30));
		
		icsBox = new JComboBox(scachesize);
		icsBox.setPreferredSize(new Dimension(90, 30));
		icsBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				icsIndex = icsBox.getSelectedIndex();
			}
		});
		
		dcsBox = new JComboBox(scachesize);
		dcsBox.setPreferredSize(new Dimension(90, 30));
		dcsBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				dcsIndex = dcsBox.getSelectedIndex();
			}
		});
		
		separateCacheEnabled(false);
		unifiedCacheEnabled(true);
		
		//cache 块大小设置
		bsLabel = new JLabel("块大小");
		bsLabel.setPreferredSize(new Dimension(120, 30));
		bsBox = new JComboBox(blocksize);
		bsBox.setPreferredSize(new Dimension(160, 30));
		bsBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				bsIndex = bsBox.getSelectedIndex();
			}
		});
		
		//相连度设置
		wayLabel = new JLabel("相联度");
		wayLabel.setPreferredSize(new Dimension(120, 30));
		wayBox = new JComboBox(way);
		wayBox.setPreferredSize(new Dimension(160, 30));
		wayBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				wayIndex = wayBox.getSelectedIndex();
			}
		});
		
		//替换策略设置
		replaceLabel = new JLabel("替换策略");
		replaceLabel.setPreferredSize(new Dimension(120, 30));
		replaceBox = new JComboBox(replace);
		replaceBox.setPreferredSize(new Dimension(160, 30));
		replaceBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				replaceIndex = replaceBox.getSelectedIndex();
			}
		});
		
		//欲取策略设置
		prefetchLabel = new JLabel("预取策略");
		prefetchLabel.setPreferredSize(new Dimension(120, 30));
		prefetchBox = new JComboBox(pref);
		prefetchBox.setPreferredSize(new Dimension(160, 30));
		prefetchBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				prefetchIndex = prefetchBox.getSelectedIndex();
			}
		});
		
		//写策略设置
		writeLabel = new JLabel("写策略");
		writeLabel.setPreferredSize(new Dimension(120, 30));
		writeBox = new JComboBox(write);
		writeBox.setPreferredSize(new Dimension(160, 30));
		writeBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				writeIndex = writeBox.getSelectedIndex();
			}
		});
		
		//调块策略
		allocLabel = new JLabel("写不命中调块策略");
		allocLabel.setPreferredSize(new Dimension(120, 30));
		allocBox = new JComboBox(alloc);
		allocBox.setPreferredSize(new Dimension(160, 30));
		allocBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				allocIndex = allocBox.getSelectedIndex();
			}
		});
		
		//选择指令流文件
		fileLabel = new JLabel("选择指令流文件");
		fileLabel.setPreferredSize(new Dimension(120, 30));
		fileAddrLabel = new JLabel();
		fileAddrLabel.setPreferredSize(new Dimension(210,30));
		fileAddrLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		fileBotton = new JButton("浏览");
		fileBotton.setPreferredSize(new Dimension(70,30));
		fileBotton.addActionListener(this);
		
		panelLeft.add(labelLeft);
		
		panelLeft.add(unifiedCacheButton);
		panelLeft.add(csLabel);
		panelLeft.add(csBox);
		
		panelLeft.add(separateCacheButton);
		panelLeft.add(icsLabel);
		panelLeft.add(icsBox);
		panelLeft.add(emptyLabel);
		panelLeft.add(dcsLabel);
		panelLeft.add(dcsBox);
		
		panelLeft.add(bsLabel);
		panelLeft.add(bsBox);
		panelLeft.add(wayLabel);
		panelLeft.add(wayBox);
		panelLeft.add(replaceLabel);
		panelLeft.add(replaceBox);
		panelLeft.add(prefetchLabel);
		panelLeft.add(prefetchBox);
		panelLeft.add(writeLabel);
		panelLeft.add(writeBox);
		panelLeft.add(allocLabel);
		panelLeft.add(allocBox);
		panelLeft.add(fileLabel);
		panelLeft.add(fileAddrLabel);
		panelLeft.add(fileBotton);
		
		//*****************************右侧面板绘制*****************************************//
		//模拟结果展示区域
		rightLabel = new JLabel("模拟结果");
		rightLabel.setPreferredSize(new Dimension(500, 40));
		panelRight.add(rightLabel);
		
		resultTagLabel = new JLabel[4][3];
		resultDataLabel = new JLabel[4][3];
		
		for(int i=0; i<4; i++){
			for(int j=0; j<3; j++){
				resultTagLabel[i][j] = new JLabel(resultTags[i][j]);
				resultTagLabel[i][j].setPreferredSize(new Dimension(70,40));
				
				if(j != 2){
					resultDataLabel[i][j] = new JLabel("0");
				}
				else{
					resultDataLabel[i][j] = new JLabel("0.00");
				}
				
				resultDataLabel[i][j].setPreferredSize(new Dimension(83, 40));
				
				panelRight.add(resultTagLabel[i][j]);
				panelRight.add(resultDataLabel[i][j]);
			}
			if(i == 0){
				JLabel label = new JLabel("其中:");
				label.setPreferredSize(new Dimension(500, 40));
				panelRight.add(label);
			}
		}
		
		accessTypeTagLabel = new JLabel("访问类型:");
		addressTagLabel = new JLabel("地址:");
		blockNumberTagLabel = new JLabel("块号:");
//		tagTagLabel = new JLabel("标记Tag:");
		indexTagLabel = new JLabel("索引:");
		inblockAddressTagLabel = new JLabel("块内地址:");
		hitTagLabel = new JLabel("命中情况:");
		
		accessTypeDataLabel = new JLabel("  ");
		addressDataLabel = new JLabel("  ");
		blockNumberDataLabel = new JLabel("  ");
		tagDataLabel = new JLabel("  ");
		indexDataLabel = new JLabel("  ");
		inblockAddressDataLabel = new JLabel("  ");
		hitDataLabel = new JLabel("  ");
		
		accessTypeTagLabel.setPreferredSize(new Dimension(80, 40));
		accessTypeDataLabel.setPreferredSize(new Dimension(80, 40));
		addressTagLabel.setPreferredSize(new Dimension(80, 40));
		addressDataLabel.setPreferredSize(new Dimension(200, 40));
		panelRight.add(accessTypeTagLabel);
		panelRight.add(accessTypeDataLabel);
		panelRight.add(addressTagLabel);
		panelRight.add(addressDataLabel);
		
		blockNumberTagLabel.setPreferredSize(new Dimension(80, 40));
        blockNumberDataLabel.setPreferredSize(new Dimension(60, 40));
        inblockAddressTagLabel.setPreferredSize(new Dimension(60, 40));
        inblockAddressDataLabel.setPreferredSize(new Dimension(100, 40));
        
        panelRight.add(blockNumberTagLabel);
        panelRight.add(blockNumberDataLabel);
        panelRight.add(inblockAddressTagLabel);
        panelRight.add(inblockAddressDataLabel);
        
        indexTagLabel.setPreferredSize(new Dimension(60, 40));
        indexDataLabel.setPreferredSize(new Dimension(70, 40));
        hitTagLabel.setPreferredSize(new Dimension(100, 40));
        hitDataLabel.setPreferredSize(new Dimension(200, 40));
        
        panelRight.add(indexTagLabel);
        panelRight.add(indexDataLabel);
        panelRight.add(hitTagLabel);
        panelRight.add(hitDataLabel);

//        tagTagLabel.setPreferredSize(new Dimension(60, 40));
//        tagDataLabel.setPreferredSize(new Dimension(70, 40));
//        
//        
//        panelRight.add(tagTagLabel);
//        panelRight.add(tagDataLabel);
        
        
		
/*		results = new JLabel[4];
		for (int i=0; i<4; i++) {
			results[i] = new JLabel("");
			results[i].setPreferredSize(new Dimension(500, 40));
		}*/
		
/*		stepLabel1 = new JLabel();
		stepLabel1.setVisible(false);
		stepLabel1.setPreferredSize(new Dimension(500, 40));
		stepLabel2 = new JLabel();
		stepLabel2.setVisible(false);
		stepLabel2.setPreferredSize(new Dimension(500, 40));
		
//		panelRight.add(rightLabel);
		for (int i=0; i<4; i++) {
			panelRight.add(results[i]);
		}
		
		panelRight.add(stepLabel1);
		panelRight.add(stepLabel2);

*/
		//*****************************底部面板绘制*****************************************//
		
		bottomLabel = new JLabel("执行控制");
		bottomLabel.setPreferredSize(new Dimension(800, 30));
		execStepBtn = new JButton("步进");
		execStepBtn.setLocation(100, 30);
		execStepBtn.addActionListener(this);
		execAllBtn = new JButton("执行到底");
		execAllBtn.setLocation(300, 30);
		execAllBtn.addActionListener(this);
		restartBtn = new JButton("复位");
		restartBtn.setLocation(500, 30);
		restartBtn.addActionListener(this);
		
		panelBottom.add(bottomLabel);
		panelBottom.add(execStepBtn);
		panelBottom.add(execAllBtn);
		panelBottom.add(restartBtn);

		add("North", panelTop);
		add("West", panelLeft);
		add("Center", panelRight);
		add("South", panelBottom);
		setSize(820, 620);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
