package time_goods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SAX_EditDist {

    static int[] delta_t = {5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100,105,110,115,120,125,130};

    static String product = "copper";

    static double pre_price[][]=new double[260][10];
    static double price[][] = new double[260][10];
    static double edit_similarity[][][]=new double[delta_t.length][10][10];
    static double standrad_similarity[]=new double[delta_t.length];

    static String year[] = new String[10];

    public static void main(String[] args)
    {
        String filename_data = "D:\\D_文档\\项目\\数据\\" + product + "_test.xls";
        read_data_price(filename_data, pre_price, year);
        get_normalized_price(pre_price,price);

        for (int num = 0; num < delta_t.length; num++)
        {
            int dt = delta_t[num];
            int n = 260 / dt;
            if(n*dt!=260)
            {
                n=n+1;
            }

            double slope[][] = new double[n][10];//一共n段，每段的斜率
            String sign[][] = new String[n][10];//一共n段，每段的符号

            double data[]=new double[260];//存放一年的时间序列

            for (int i = 0; i < 10; i++)
            {
                for(int k=0;k<260;k++)
                {
                    data[k]=price[k][i];//获取第i年的序列
                }
                double min=get_min(data);//获取第i年的最小值
                double max=get_max(data);//获取第i年的最大值
                for(int k=0;k<n;k++)
                {
                    int x_len=dt;
                    int y_len=dt;
                    if(n*dt!=260)
                    {
                        if(k==n-1)
                        {
                            x_len=260-k*dt;
                            y_len=260-k*dt;
                        }
                    }
                    double x[]=new double[x_len];
                    double y[]=new double[y_len];
                    get_sub_seq(data,x,y,dt,n,k);
                    double mean=get_mean(y);
                    double b=(max-min)/3;

                    slope[k][i]=get_slope(x, y);
                    sign[k][i] = get_sign(slope[k][i],mean,b,min,max);
                }
            }
            String data_1[]=new String[n];
            String data_2[]=new String[n];
            for(int i=0;i<10;i++)
            {
                for(int j=i+1;j<10;j++)
                {
                    for(int k=0;k<n;k++)
                    {
                        data_1[k]=sign[k][i];
                        data_2[k]=sign[k][j];
                    }
                    double dis=get_edit_distance(data_1,data_2,n);
                    edit_similarity[num][i][j]=1-dis/n;
                    //System.out.println("在时间粒度为"+delta_t[num]+"时，第"+i+"年和第"+j+"年的相似度是："+edit_similarity[num][i][j]);
                }
            }
            double sum_similarity=0;
            for(int i=0;i<10;i++)
            {
                for(int j=i+1;j<10;j++)
                {
                    sum_similarity=sum_similarity+edit_similarity[num][i][j];
                }
            }
            standrad_similarity[num]=sum_similarity/(10*9/2);
            System.out.println("在时间粒度为"+delta_t[num]+"时，相似度为："+standrad_similarity[num]);
        }
        //推荐时间粒度
        int index=0;
        double max_similarity=standrad_similarity[0];
        for(int i=1;i<delta_t.length;i++)
        {
            if(standrad_similarity[i]>max_similarity)
            {
                index=i;
                max_similarity=standrad_similarity[i];
            }
        }
        System.out.println(product+"在时间粒度为"+delta_t[index]+"时，最大相似度为："+max_similarity);
        //最近一段时间序列的异常检测
        ArrayList<Struct_Abnormal> abnormal_points=new ArrayList<Struct_Abnormal>();
        int time_granularity=delta_t[index];
        int sequence_num=260/time_granularity;
        int len=time_granularity;
        if(sequence_num*time_granularity!=260)
        {
            sequence_num=sequence_num+1;
            //len=260-(sequence_num-1)*time_granularity;
        }
        double abnormal_sequence[]=new double[len*10];

        for(int i=0;i<abnormal_sequence.length;i++)
        {
            int temp_index=i%len+260-len;
            int temp_year=i/len;
            abnormal_sequence[i]=price[temp_index][temp_year];
        }

        ArrayList<DataNode> dpoints = new ArrayList<DataNode>();
        for(int t=0;t<abnormal_sequence.length;t++)
        {
            int temp_index=t%len+260-len;
            int temp_year=t/len;
            double[] temp_point={temp_index,abnormal_sequence[t]};
            dpoints.add(new DataNode(year[temp_year]+"年"+String.valueOf(temp_index+1)+"日",temp_point,temp_index+1));
        }

        LOF lof=new LOF();
        List<DataNode> nodeList=lof.get_OutLineNode(dpoints);//计算每个点的离群因子
        Collections.sort(nodeList,new LOF.LofComparator());//从大到小排序

        double avg_lof=0;
        double std_lof=0;
        for(DataNode node:nodeList)
        {
            String temp = node.getNodeName().substring(0, 4);
            if (temp.equals(year[9]))
            {
                avg_lof = avg_lof + node.getLof();
            }
        }
        avg_lof=avg_lof/time_granularity;
        for(DataNode node:nodeList)
        {
            String temp = node.getNodeName().substring(0, 4);
            if (temp.equals(year[9]))
            {
                std_lof = std_lof + Math.pow((node.getLof()-avg_lof),2);
            }
        }
        std_lof=Math.sqrt(std_lof/time_granularity);
        for(DataNode node:nodeList)
        {
            String temp=node.getNodeName().substring(0,4);
            if(temp.equals(year[9]))
            {
                //System.out.println(node.getNodeName() + "的离群因子:" + node.getLof());
                if((node.getLof()-avg_lof)/std_lof>3)//判断是否是异常点
                {
                    abnormal_points.add(new Struct_Abnormal(year[9],node.getIndex()));//将其加入异常点集合
                }
            }
        }
        if(abnormal_points.isEmpty())
        {
            System.out.println("无异常点");
        }
        else
        {
            for(Struct_Abnormal node:abnormal_points)
            {
                System.out.println(year[9]+"年最近一段时间粒度内，疑似异常点：第"+node.getIndex()+"日");
            }
        }
    }

    public static void read_data_price(String fileName,double data[][],String year[])
    { //读入数据到price数组
        try{
            InputStream input=new FileInputStream(fileName);//建立输入流
            HSSFWorkbook wb=new HSSFWorkbook(input);//初始化
            HSSFSheet sheet=wb.getSheetAt(0);//获取第一个表单

            int rowLength=sheet.getPhysicalNumberOfRows();//总行数
            HSSFRow hssfRow=sheet.getRow(0);//获取第一行（表头）
            int colLength=hssfRow.getPhysicalNumberOfCells();//总列数
            //System.out.println(rowLength);
            //System.out.println(colLength);
            for(int k=1;k<colLength;k++)
            {
                int temp=(int)hssfRow.getCell(k).getNumericCellValue();
                year[k-1]=String.valueOf(temp);
                //year[k-1]=hssfRow.getCell(k).getStringCellValue();
            }
            for(int i=1;i<rowLength;i++)//去掉第一行表头
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=1;j<colLength;j++)//去掉第一列
                {
                    HSSFCell cell=row.getCell(j);//获得指定单元格
                    data[i-1][j-1]=cell.getNumericCellValue();
                }
            }
            //System.out.println(price[0][0]);
            //System.out.println(price[259][0]);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void get_normalized_price(double previous_price[][],double normalized_price[][])
    {
        int row_length= previous_price.length;
        int column_length=previous_price[0].length;
        double mean[]=new double[column_length];
        double var[]=new double[column_length];
        get_data_meanprice(previous_price,mean);
        get_data_varprice(previous_price,var,mean);
        for(int j=0;j<column_length;j++)
        {
            for(int i=0;i<row_length;i++)
            {
                normalized_price[i][j]=(previous_price[i][j]-mean[j])/Math.sqrt(var[j]);
            }
        }
    }

    public static void get_data_varprice(double data[][],double var[],double mean[])
    {
        int row_length= price.length;
        int column_length=price[0].length;
        for(int i=0;i<column_length;i++)
        {
            double var_sum=0;
            for(int j=0;j<row_length;j++)
            {
                var_sum=var_sum+Math.pow((price[j][i]-mean[i]),2);
            }
            var[i]=var_sum/row_length;
        }
    }

    public static void get_data_meanprice(double data[][],double mean_data[])
    {//计算每年的平均价格
        int row_length= data.length;
        int column_length=data[0].length;
        for(int i=0;i<column_length;i++)
        {
            double column_sum=0;
            for(int j=0;j<row_length;j++)
            {
                column_sum=column_sum+data[j][i];
            }
            mean_data[i]=(double)(Math.round(column_sum*100/row_length)/100.0);//保留两位小数
            //System.out.println(meanprice[i]);
        }
    }

    public static double get_min(double data[])
    {//获取数组data中的最小值
        double min=data[0];
        for(int i=0;i<data.length;i++)
        {
            if(data[i]<min)
            {
                min=data[i];
            }
        }
        return min;
    }

    public static double get_max(double data[])
    {//获取数组中的最大值
        double max=data[0];
        for(int i=0;i<data.length;i++)
        {
            if(data[i]>max)
            {
                max=data[i];
            }
        }
        return max;
    }

    public static double get_mean(double data[])
    {//获取数组data的平均值
        double sum=0;
        int len=data.length;
        for(int i=0;i<len;i++)
        {
            sum=sum+data[i];
        }
        return sum/len;
    }

    public static void get_sub_seq(double data[],double x[],double y[],int dt,int n,int k)
    {
        if(n*dt!=260)
        {
            if(k<n-1)
            {
                for(int t=k*dt;t<(k+1)*dt;t++)
                {
                    int temp=t-k*dt;
                    x[temp]=t+1;
                    y[temp]=data[t];
                }
            }
            else
            {
                for(int t=k*dt;t<260;t++)
                {
                    int temp=t-k*dt;
                    x[temp]=t+1;
                    y[temp]=data[t];
                }
                /*for(int t=260-dt;t<260;t++)
                {
                    int temp=t+dt-260;
                    x[temp]=t+1;
                    y[temp]=data[t];
                }*/
            }
        }
        else
        {
            for(int t=k*dt;t<(k+1)*dt;t++)
            {
                int temp=t-k*dt;
                x[temp]=t+1;
                y[temp]=data[t];
            }
        }
    }

    public static double get_slope(double x[], double y[])
    {
        //返回横坐标为x[],纵坐标为y[]的时，某段对应的直线拟合的斜率a
        int num = x.length;
        double sum_x = 0;
        double sum_y = 0;
        double sum_xy = 0;
        double sum_xx = 0;
        for (int k = 0; k < num; k++) {
            sum_x = sum_x + x[k];
            sum_y = sum_y + y[k];
            sum_xy = sum_xy + x[k] * y[k];
            sum_xx = sum_xx + x[k] * x[k];
        }
        double a = (num * sum_xy - sum_x * sum_y) / (num * sum_xx - sum_x * sum_x);
        return a;
    }

    public static String get_sign(double a,double mean,double b,double min,double max)
    {//返回当序列最小值为min，最大值为max，某段斜率为a，平均值为mean时，该段对应的符号
        String sign="";
        if(a>0)
        {
            if(min<=mean && mean<min+b)
            {
                sign="C";
            }
            else if(min+b<=mean && mean<min+2*b)
            {
                sign="F";
            }
            else if(min+2*b<=mean && mean<max)
            {
                sign="I";
            }
        }
        else if(a==0)
        {
            if(min<=mean && mean<min+b)
            {
                sign="B";
            }
            else if(min+b<=mean && mean<min+2*b)
            {
                sign="E";
            }
            else if(min+2*b<=mean && mean<max)
            {
                sign="H";
            }
        }
        else
        {
            if(min<=mean && mean<min+b)
            {
                sign="A";
            }
            else if(min+b<=mean && mean<min+2*b)
            {
                sign="D";
            }
            else if(min+2*b<=mean && mean<max)
            {
                sign="G";
            }
        }
        return sign;
    }

    public static int get_edit_distance(String data_1[],String data_2[],int data_length)
    {
        int distance=0;

        int dis[][]=new int[data_length+1][data_length+1];
        for(int i=0;i<data_length+1;i++)
        {
            dis[0][i]=i;
            dis[i][0]=i;
        }
        for(int i=1;i<data_length+1;i++)
        {
            for(int j=1;j<data_length+1;j++)
            {
                if(i>=1 && j>=1)
                {
                    int f=0;
                    if(data_1[i-1].equals(data_2[i-1]))
                    {
                        f=0;
                    }
                    else
                    {
                        f=1;
                    }
                    dis[i][j]=Math.min(Math.min(dis[i-1][j]+1,dis[i][j-1]+1),dis[i-1][j-1]+f);
                }
            }
        }
        return dis[data_length][data_length];
    }


}
