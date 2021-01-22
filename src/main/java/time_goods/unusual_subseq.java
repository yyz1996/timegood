package time_goods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class unusual_subseq {
    static String product="data_1.xls";
    static int time_granularity=26;//时间粒度，即子序列长度
    static int window_size=4;//滑动窗口长度
    static double price_nearest[]=new double[260];//最近一年的价格序列

    public static void main(String[] args)
    {
        String filename="C:\\Users\\USER\\Desktop\\data\\test_data\\"+product;
        read_data(filename,price_nearest);
        //get_normalized(price_nearest);

        int n=260/time_granularity;

        double dist[]=new double[time_granularity-window_size+1];
        double data_1[]=new double[window_size];
        double data_2[]=new double[window_size];
        for(int i=0;i<dist.length;i++)
        {
            get_seq(price_nearest,data_1,n-1,i,time_granularity,window_size);

            double sum=0;
            for(int j=0;j<n;j++)
            {
                get_seq(price_nearest,data_2,j,i,time_granularity,window_size);
                sum=sum+get_seq_dist(data_1,data_2);
            }
            dist[i]=sum;
            System.out.println(i+1+(n-1)*time_granularity+":"+dist[i]);
        }
    }

    static public void read_data(String filename,double data[])
    {
        try{
            InputStream input=new FileInputStream(filename);//建立输入流
            HSSFWorkbook wb=new HSSFWorkbook(input);//初始化
            HSSFSheet sheet=wb.getSheetAt(0);//获取第一个表单

            int rowLength=sheet.getPhysicalNumberOfRows();//总行数
            HSSFRow hssfRow=sheet.getRow(0);//获取第一行（表头）

            for(int i=1;i<rowLength;i++)//去掉第一行表头
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                HSSFCell cell=row.getCell(1);//获得指定单元格
                data[i-1]=cell.getNumericCellValue();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    static public void get_normalized(double data[])
    {
        double mean=0;
        double var=0;
        for(int i=0;i< data.length;i++)
        {
            mean=mean+data[i];
        }
        mean=mean/ data.length;
        for(int i=0;i< data.length;i++)
        {
            var=var+Math.pow(data[i]-mean,2);
        }
        var=var/ data.length;
        for(int i=0;i< data.length;i++)
        {
            data[i]=(data[i]-mean)/Math.sqrt(var);
        }
    }

    static public void get_seq(double data[],double sub_seq[],int seq_index,int win_index,int seq_len,int win_size)
    {
        int seq_start=seq_index*seq_len;
        int win_start=seq_start+win_index;
        for(int i=win_start;i<win_start+win_size;i++)
        {
            sub_seq[i-win_start]=data[i];
        }
    }

    static public double get_seq_dist(double data_1[],double data_2[])
    {
        double sum=0;
        for(int i=0;i< data_1.length;i++)
        {
            sum=sum+Math.pow(data_1[i]-data_2[i],2);
        }
        return sum/ data_1.length;
    }

}
