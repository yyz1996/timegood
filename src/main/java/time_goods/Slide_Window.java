package time_goods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.ui.RectangleInsets;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Slide_Window {

    //static int []win_size={10,20,30,40,50,60,70,80,90,100,110,120,130,140,150,160,170,180,190,200,210,220,230,240,250,260};
    static int []win_size={13,13*2,13*3,13*4,13*5,13*6,13*7,13*8,13*9,13*10,13*11,13*12,13*13,13*14,13*15,13*16,13*17,13*18,13*19,13*20};
    static String product="simulate_data_3";

    static double price[]=new double[260*3];
    //static int win_size[]=new int[255];
    static double slide_win_dis[]=new double[win_size.length];
    static double slide_win_corr[]=new double[win_size.length];

    public static void main(String[] args)
    {
        //读取数据到数组
        String filepath ="D:\\D_文档\\项目\\数据\\" + product + "_test.xls";
        read_data(filepath, price);
        //对原始数据进行z-score标准化
        get_normalized(price);

        /*for(int i=0;i<win_size.length;i++)
        {
            win_size[i]=i+5;
        }*/

        for(int num=0;num< win_size.length;num++)
        {
            int window_size=win_size[num];
            int n=(260*3)/window_size;
            if(n*window_size!=260*3)
            {
                n=n+1;
            }

            double slide_win_dis_temp[][]=new double[n][n];//存放任意两个滑动窗口内的子序列之间的欧氏距离
            double slide_win_corr_temp[][]=new double[n][n];
            double win_data_1[]=new double[window_size];//获取窗口内每一个子序列的数据
            double win_data_2[]=new double[window_size];
            for(int i=0;i<n;i++)
            {
                for(int j=i+1;j<n;j++)
                {
                    get_win_data(price,win_data_1,i,window_size,n);
                    get_win_data(price,win_data_2,j,window_size,n);

                    slide_win_dis_temp[i][j]=get_slide_win_dis(win_data_1,win_data_2);
                    slide_win_corr_temp[i][j]=get_slide_win_corr(win_data_1,win_data_2);
                }
            }
            slide_win_dis[num]=get_standard(slide_win_dis_temp);
            slide_win_corr[num]=get_standard(slide_win_corr_temp);
            System.out.println(win_size[num]+":"+slide_win_corr[num]);
        }
        //绘制曲线
        String path_dis="D:\\picture\\slide_win_similarity\\dis_"+product+".jpg";
        DefaultCategoryDataset ds_dis=(DefaultCategoryDataset) get_dataSet(slide_win_dis,product, win_size.length);
        getLineChart(ds_dis,path_dis,"滑动窗口大小","欧氏距离","滑动窗口大小与欧氏距离数图",false);

        String path_corr="D:\\picture\\slide_win_similarity\\corr_"+product+".jpg";
        DefaultCategoryDataset ds_corr=(DefaultCategoryDataset) get_dataSet(slide_win_corr,product, win_size.length);
        getLineChart(ds_corr,path_corr,"滑动窗口大小","相关系数","滑动窗口大小与相关系数图",false);

        String path_price="D:\\picture\\simulate_price\\price_"+product+".jpg";
        DefaultCategoryDataset ds_price=(DefaultCategoryDataset) get_dataSet_price(price,product, price.length);
        getLineChart(ds_price,path_price,"日","价格","价格图",false);
    }

    public static void read_data(String filepath,double price[])
    {
        try
        {
            InputStream input=new FileInputStream(filepath);//建立输入流
            HSSFWorkbook wb=new HSSFWorkbook(input);//初始化
            HSSFSheet sheet=wb.getSheetAt(0);//获取第一个表单

            int rowLength=sheet.getPhysicalNumberOfRows();//总行数
            HSSFRow hssfRow=sheet.getRow(0);//获取第一行（表头）
            int colLength=hssfRow.getPhysicalNumberOfCells();//总列数

            int k=0;
            for(int i=1;i<rowLength;i++)
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=1;j<colLength;j++)//去掉第一列
                {
                    HSSFCell cell=row.getCell(j);//获得指定单元格
                    price[k]=cell.getNumericCellValue();
                    k++;
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void get_normalized(double price[])
    {
        //z-score标准化
        double mean=0;
        double var=0;
        double sum=0;
        for(int i=0;i<price.length;i++)
        {
            sum=sum+price[i];
        }
        mean=sum/price.length;
        for(int i=0;i<price.length;i++)
        {
            var=var+Math.pow((price[i]-mean),2);
        }
        for(int i=0;i< price.length;i++)
        {
            price[i]=(price[i]-mean)/Math.sqrt(var/price.length);
        }
    }

    public static void get_win_data(double price[],double win_data[],int index,int size,int num)
    {
        if(size*num!= price.length)
        {
            if(index<num-1)
            {
                for(int k=index*size;k<(index+1)*size;k++)
                {
                    int temp=k-index*size;
                    win_data[temp]=price[k];
                }
            }
            else
            {
                for(int k= price.length-size;k< price.length;k++)
                {
                    int temp=k-price.length+size;
                    win_data[temp]=price[k];
                }
            }
        }
        else
        {
            for(int k=index*size;k<(index+1)*size;k++)
            {
                int temp=k-index*size;
                win_data[temp]=price[k];
            }
        }
    }

    public static double get_slide_win_corr(double data_1[],double data_2[])
    {
        double corr=0;
        double mean_1=0;
        double mean_2=0;
        double sum_numerator=0;
        double sum_denominator_left=0;
        double sum_denominator_right=0;
        int len= data_1.length;;
        for(int i=0;i<len;i++)
        {
            mean_1=mean_1+data_1[i];
            mean_2=mean_2+data_2[i];
        }
        mean_1=mean_1/len;
        mean_2=mean_2/len;
        for(int i=0;i<len;i++)
        {
            sum_numerator=sum_numerator+(data_1[i]-mean_1)*(data_2[i]-mean_2);
            sum_denominator_left=sum_denominator_left+Math.pow((data_1[i]-mean_1),2);
            sum_denominator_right=sum_denominator_right+Math.pow((data_2[i]-mean_2),2);
        }
        corr=sum_numerator/(Math.sqrt(sum_denominator_left)*Math.sqrt(sum_denominator_right));
        //System.out.println(corr);
        return corr;
    }

    public static double get_slide_win_dis(double data_1[],double data_2[])
    {
        /*double min_1=get_min(data_1);
        double min_2=get_min(data_2);
        double max_1=get_max(data_1);
        double max_2=get_max(data_2);
        double mean_1=get_mean(data_1,min_1,max_1);
        double mean_2=get_mean(data_2,min_2,max_2);
        double dis=0;
        int len=data_1.length;
        dis=(len-2)*Math.pow((mean_1-mean_2),2)+Math.pow((min_1-min_2),2)+Math.pow((max_1-max_2),2);
        dis=Math.sqrt(dis);
        return dis;*/
        double dis=0;
        for(int i=0;i<data_1.length;i++)
        {
            dis=dis+Math.pow((data_1[i]-data_2[i]),2);
        }
        dis=Math.sqrt(dis);
        return dis;
    }

    public static double get_min(double data[])
    {
        double min=data[0];
        for(int i=1;i<data.length;i++)
        {
            if(data[i]<min)
            {
                min=data[i];
            }
        }
        return min;
    }

    public static double get_max(double data[])
    {
        double max=data[0];
        for(int i=1;i<data.length;i++)
        {
            if(data[i]>max)
            {
                max=data[i];
            }
        }
        return max;
    }

    public static double get_mean(double data[],double min,double max)
    {//求除去极大值和极小值后的平均值
        double sum=0;
        for(int i=0;i<data.length;i++)
        {
            if(data[i]!=min && data[i]!=max)
            {
                sum=sum+data[i];
            }
        }
        double mean=sum/(data.length-2);
        return mean;
    }

    public static double get_standard(double data[][])
    {
        int line=data.length;
        int column=data[0].length;
        double sum=0;
        for(int i=0;i<line;i++)
        {
            for(int j=i+1;j<column;j++)
            {
                sum=sum+data[i][j];
            }
        }
        return sum/(((line-1)*column)/2);
    }

    public static Dataset get_dataSet(double data[], String product,int win_num)
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();
        for(int m=0;m<win_num;m++)
        {
            dataset.addValue(data[m],product,String.valueOf(win_size[m]));
        }
        return dataset;
    }

    public static Dataset get_dataSet_price(double data[],String product,int num)
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();
        for(int i=0;i<num;i++)
        {
            dataset.addValue(data[i],product,String.valueOf(i+1));
        }
        return dataset;
    }

    public static void getLineChart(DefaultCategoryDataset dataset,String filePath,String xlabel,String ylabel,String title,boolean num_label)
    { //画折线图
        try
        {
            JFreeChart chart = ChartFactory.createLineChart(title, xlabel, ylabel, dataset, PlotOrientation.VERTICAL, true, true, true);
            Font font = new Font("宋体", Font.BOLD, 12);
            chart.getTitle().setFont(font);
            chart.setBackgroundPaint(Color.WHITE);

            // 配置字体（解决中文乱码的通用方法）
            Font xfont = new Font("宋体", Font.BOLD, 12); // X轴
            Font yfont = new Font("宋体", Font.BOLD, 12); // Y轴
            Font titleFont = new Font("宋体", Font.BOLD, 12); // 图片标题
            CategoryPlot categoryPlot = chart.getCategoryPlot();
            categoryPlot.getDomainAxis().setLabelFont(xfont);
            categoryPlot.getDomainAxis().setLabelFont(xfont);
            categoryPlot.getRangeAxis().setLabelFont(yfont);
            chart.getTitle().setFont(titleFont);
            categoryPlot.setBackgroundPaint(Color.WHITE);

            //x轴网格是否可见
            categoryPlot.setDomainGridlinesVisible(true);
            //y轴网格是否可见
            categoryPlot.setRangeGridlinesVisible(true);

            //设置曲线图与xy轴的距离
            categoryPlot.setAxisOffset(new RectangleInsets(0d, 0d, 0d, 0d));

            LineAndShapeRenderer lineandshaperenderer = (LineAndShapeRenderer) categoryPlot.getRenderer();
            //是否显示折点
            lineandshaperenderer.setBaseShapesVisible(true);
            //是否显示折线
            lineandshaperenderer.setBaseLinesVisible(true);
            //显示折点数据
            lineandshaperenderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
            lineandshaperenderer.setBaseItemLabelsVisible(num_label);

            //没有数据时显示的文字说明
            categoryPlot.setNoDataMessage("没有数据显示");

            //设置面板字体
            Font labelFont = new Font("", Font.TRUETYPE_FONT, 12);

            //导出图片
            ChartUtilities.saveChartAsJPEG(new File(filePath), chart, 1207, 500);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
