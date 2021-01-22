package time_goods;

/*
* （1）给定一个长度为m的时间序列
* （2）归一化，使其均值为0，标准差为1
* （3）确定滑动窗口长度n，则子序列的长度为n
* （4）对n维子序列进行PAA线段拟合转换为w维
* （5）确定断点a值，将m-n+1个n维子序列分别映射成m-n+1个长度为w的单词
* （6）实现表2的算法：
	对于外循环：扫描数组的最右列找到最小的计数，记录发生最小计数时间的所有SAX单词的索引，将其提供给外循环以首先搜索，外循环用完该集合后，将以随机顺序访问其余的候选对象
	对于内循环：对于外部循环的每个候选，找到其SAX词，用该SAX值遍历利查找树，找到此处的子序列映射，这几个子序列将在内循环中首先访问。在此步骤后，将以随机顺序访问其余子序列
*/

public class HOT_SAX {

    static int m=260;
    static int n=100;
    static int a=3;
    static int w=10;
    static double price[]=new double[m];

    public static void main(String[] args)
    {
        String sax_array[][]=new String[m-n+1][2];//第一列存放映射的单词，第二列存放该单词出现的次数

    }



    public static void get_PAA_and_Word(double data[],String word[],int w,double beta[])
    {//PAA将n维子序列转换为w维（n需维w整数倍）,用每一段的均值替代
     //判断转换后的数值与beta[]中的数值的大小，将其转换为长度为w的单词
        double data_PAA[]=new double[w];
        int num=data.length/w;
        for(int i=0;i<w;i++)
        {
            double sum=0;
            for(int k=i*num;k<(i+1)*num;k++)
            {
                sum=sum+data[k];
            }
            data_PAA[i]=sum/num;

            String init_word="a";
            for(int j=0;j<beta.length;j++)
            {
                if(data_PAA[i]<beta[j+1] && data_PAA[i]>=beta[j])
                {
                    word[i]=init_word+i;
                    break;
                }
            }
        }
    }





}
