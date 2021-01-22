package timegoods;

import java.util.*;


public class CommodityTransaction {

	static String tid[][]=
			{{"A","B","C","D"},{"A","C","D","E"},{"A","C","F","G"},{"A","C","D","G"},
					{"Q","D","E","R"},{"A","R","G","Y"},{"H","E","K","D"},};//1.建立事务集，从数据库中导入数据。应该是一个二维数组（动态的）
	static Map<String, Integer> maph1=new HashMap<>();//2.候选一项集，建立map键值对,记录每个商品的 0支持度计数,1支持度百分比
	static Map<String, Integer> mapp1=new HashMap<>();//2.频繁一项集，建立map键值对,记录每个商品的 0支持度计数,1支持度百分比
	//static String p1goods[];//频繁一项集中的商品
	static List<String> p1goods=new ArrayList<String>();
	static Map<List<String>, Integer> maph2=new HashMap<>();//5.用满足阈值的商品频繁1项集，构建候选二项集。实现了数学中的组合方法C(n,2)即 n!/(n-2)!2!，调用计算阶乘的函数
	static Map<List<String>, Integer> mapp2=new HashMap<>();//7.剪枝，删除支持度不满足阈值条件的商品。构建频繁二项集。
	static Map<String, List<String>> mapgoods=new HashMap<>();//8.利用频繁二项集构建 关联商品集合
	static double support_threshold=0.2;//支持度阈值
	static double confidence_threshold=0.2;//置信度阈值

	public static void main(String[] args) {

		getmaph1();//3.第一次 遍历事务集，构建候选1项集。记录商品名称key，以及对应的支持度计数value，支持度百分比（将value/tid.length）

		getmapp1();//4.剪枝，删除其中支持度阈值<50%的商品，构建频繁1项集。

		getp1goods();//5.遍历频繁一项集，得到频繁一项集中的商品。并初始化候选二项集

		getmapp2();//7.剪枝，删除支持度不满足阈值条件的商品。构建频繁二项集。

		initmapgoods();//8.利用频繁二项集构建  初始化关联商品集合

		getmapgoods();//8.得到关联商品集合

		//1.建立事务集，从数据库中导入数据。应该是一个二维数组（动态的）
		//2.建立map键值对,记录每个商品的 0支持度计数,1支持度百分比
		//3.第一次 遍历事务集，构建候选1项集。记录商品名称key，以及对应的支持度计数value，支持度百分比（将value/tid.length）
		//4.剪枝，删除其中支持度阈值<50%的商品，构建频繁1项集。
		//5.用满足阈值的商品频繁1项集，构建候选二项集。实现了数学中的组合方法C(n,2)即 n!/(n-2)!2!，调用计算阶乘的函数
		//6.第二次 遍历事务集计算候选二项集的支持度计数，支持度，置信度。
		//7.剪枝，删除支持度和置信度不满足阈值条件的商品。构建频繁二项集。
		//8.利用频繁二项集构建 关联商品集合
		//

	}

	private static void getmapgoods() {//8.得到关联商品集合

		// 从频繁二项集mapp2中，提取商品名称，以及关联的商品。判断是否符合置信度阈值，符合则put进关联商品集中。

		Iterator<List<String>> iter = mapp2.keySet().iterator();
		while(iter.hasNext()){
			List<String> key = iter.next();
			String s1=key.get(0);
			String s2=key.get(1);

			if(mapp2.get(key)>=maph1.get(s1)*confidence_threshold){ //s1->s2  满足置信度阈值

				double z1=mapp2.get(key);
				double z2=tid.length;
				double support=z1/z2;
				support = (double) Math.round(support * 10000) / 10000;//保留四位小数
				double confidence=z1/maph1.get(s1);
				confidence = (double) Math.round(confidence * 10000) / 10000;//保留四位小数
				System.out.println("("+s1+","+s2+")"+"的支持度： "+support);
				System.out.println("("+s1+"->"+s2+")"+"的置信度： "+confidence);
				System.out.println();
				List<String> temp1=new ArrayList<>();
				List<String> temp2=new ArrayList<>();
				temp1.add(s2);
				temp2=mapgoods.get(s1);
				if(temp2 != null) {
					temp1.addAll(temp2);
				}
				mapgoods.put(s1, temp1);

				String str=null,str2;
				//  System.out.println("商品： "+s1+"  的关联商品为： "+str.join(",", temp1));
				str2=str.join(",", temp1);
				//  System.out.println(str2);
			}

			if(mapp2.get(key)>=maph1.get(s2)*confidence_threshold){ // s2->s1 满足置信度阈值
				double z1=mapp2.get(key);
				double z2=tid.length;
				double support=z1/z2;
				support = (double) Math.round(support * 10000) / 10000;//保留四位小数
				double confidence=z1/maph1.get(s1);
				confidence = (double) Math.round(confidence * 10000) / 10000;//保留四位小数
				System.out.println("("+s2+","+s1+")"+"的支持度： "+support);
				System.out.println("("+s2+"->"+s1+")"+"的置信度： "+confidence);
				System.out.println();
				List<String> temp1=new ArrayList<>();
				List<String> temp2=new ArrayList<>();
				temp1.add(s1);
				temp2=mapgoods.get(s2);
				if(temp2 != null) {
					temp1.addAll(temp2);
				}
				mapgoods.put(s2, temp1);
			}
		}
		System.out.println("=====得出满足支持度和置信度阈值的关联商品集合mapgoods =====\n"+mapgoods);

	}

	private static void initmapgoods() {//8.利用频繁二项集构建  初始化关联商品集合

		System.out.println("=====利用频繁二项集构建  初始化关联商品集合mapgoods =====");
		List<String> temp=null;
		for (String s : maph1.keySet()) {
			mapgoods.put(s,temp);
		}
		System.out.println("=====初始化关联商品集合mapgoods =====\n"+mapgoods);
	}

	private static void getmapp2() {//7.剪枝，删除支持度不满足阈值条件的商品。构建频繁二项集。
		mapp2.putAll(maph2);

		Iterator<List<String>> iter = mapp2.keySet().iterator();
		while(iter.hasNext()){
			List<String> key = iter.next();
			if(mapp2.get(key)<tid.length*support_threshold){
				iter.remove();
			}
		}
		System.out.println("=====剪枝后，得到频繁二项集 mapp2 =====\n"+mapp2);

	}

	private static void getmaph2(List<String> temp) {//6.第二次 遍历事务集计算候选二项集的支持度计数，支持度，置信度。

		int flag=0;
		for(int i=0;i<tid.length;i++) {
			for(int j=0;j<tid[i].length;j++) {
				if(tid[i][j].equals(temp.get(0))||tid[i][j].equals(temp.get(1))) {
					flag++;
					if(flag==2) {
						maph2.put(temp, maph2.get(temp)+1);
					}
				}
			}
			flag=0;
		}
		//System.out.println("=====第二次遍历事务集，得到候选二项集=====");
		//System.out.println(maph2);

	}

	private static void getp1goods() {//5.遍历频繁一项集，得到频繁一项集中的商品
		for (String s : mapp1.keySet()) {
			p1goods.add(s);
		}
		System.out.println("=====遍历频繁一项集，得到频繁一项集中的商品=====");
		System.out.println(p1goods);

		System.out.println("=====构建候选二项集=====");
		for(int i=0;i<p1goods.size();i++) {
			for(int j=i+1;j<p1goods.size();j++) {
				List<String> temp=new ArrayList<String>();
				temp.add(p1goods.get(i));
				temp.add(p1goods.get(j));

				maph2.put(temp, 0);//初始化候选二项集
				getmaph2(temp);//6.第二次 遍历事务集计算候选二项集的支持度计数
			}
		}

		System.out.println("=====第二次遍历事务集，得到候选二项集mphh2=====");
		System.out.println(maph2);
	}

	private static void getmapp1() {//4.剪枝，删除其中支持度阈值<50%的商品，构建频繁1项集。
		mapp1.putAll(maph1);;

		Iterator<String> iter = mapp1.keySet().iterator();
		while(iter.hasNext()){
			String key = iter.next();
			if(mapp1.get(key)<tid.length*support_threshold){
				iter.remove();
			}
		}
		System.out.println("=====剪枝后，得到频繁一项集 mapp1 =====\n"+mapp1);
	}

	private static void getmaph1() {//3.第一次 遍历事务集，构建候选1项集。记录商品名称key，以及对应的支持度计数value，支持度百分比（将value/tid.length）
		for(int i=0;i<tid.length;i++) {
			for(int j=0;j<tid[i].length;j++) {
				if(!maph1.containsKey(tid[i][j])) {
					maph1.put(tid[i][j], 1);// 首次记录map值
				}else {
					maph1.put(tid[i][j], maph1.get(tid[i][j])+1);
				}
			}
		}
		System.out.println("=====第一次遍历事务集，得到候选一项集 maph1=====\n"+maph1);
	}

}
