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
import java.io.*;

public class DTW_distance {

    //时间粒度
    static int[] delta_t ={1,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100,105,110,115,120,125,130};
    //商品名称
    static String product="copper";

    static double price[][]=new double[260][10];
    static double DTW_dis[][][]=new double[delta_t.length][10][10];
    static double standard_DTW[]=new double[delta_t.length];

    static String year[]=new String[10];

    public static void main(String[] args)
    {
        //读取数据到数组price
        String filename_data = "D:\\D_文档\\项目\\数据\\"+product+"_test.xls";
        read_data_price(filename_data, price,year);

        for(int num=0;num<delta_t.length;num++)
        {
            int dt=delta_t[num];
            int n=260/dt;
            for(int i=0;i<10;i++)
            {
                for(int j=i+1;j<10;j++)
                {
                    double Q[]=new double[n];
                    double U[]=new double[n];
                    for(int t=0;t<n;t++)
                    {
                        Q[t]=get_element(price,i,dt,n,t);//在时间粒度为dt时，第i年的价格时间序列
                        U[t]=get_element(price,j,dt,n,t);//在时间粒度为dt时，第j年的价格时间序列
                    }
                    DTW_dis[num][i][j]=get_DTW_distance(Q,U);
                }
            }
            standard_DTW[num]=standard_DTW_distance(num,DTW_dis);
            System.out.println(standard_DTW[num]);
        }

        //将生成的动态时间弯曲距离xls文件输出到项目所在文件夹下
        String DTW_filename="standard_DTW_distance_"+product+".xls";
        write_DTW(standard_DTW,"时间粒度","标准化动态时间弯曲距离",DTW_filename);

        //绘制动态时间弯曲距离
        String path_DTW="D:\\picture\\DTW_distance\\standard_DTW_distance_"+product+".jpg";
        DefaultCategoryDataset ds_DTW = (DefaultCategoryDataset) getDataset_DTW(standard_DTW,product);
        getLineChart(ds_DTW,path_DTW,"时间粒度","标准化动态时间弯曲距离","标准化动态时间弯曲距离与时间粒度图",true);

    }

    public static void read_data_price(String fileName,double price[][],String year[])
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
            }
            for(int i=1;i<rowLength;i++)//去掉第一行表头
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=1;j<colLength;j++)//去掉第一列
                {
                    HSSFCell cell=row.getCell(j);//获得指定单元格
                    price[i-1][j-1]=cell.getNumericCellValue();
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

    public static double get_element(double price[][],int year,int dt,int n,int t)
    {//返回第year年，在dt的时间粒度下，分为n段，第t段的平均价格
        double sum_element=0;
        for(int k=t*dt;k<(t+1)*dt;k++)
        {
            sum_element=sum_element+price[k][year];
        }
        sum_element=sum_element/dt;
        return sum_element;
    }

    public static double get_DTW_distance(double Q[],double U[])
    {
        //求两个时间序列Q，U的动态时间弯曲距离
        double D[][]=new double[Q.length][U.length];
        double R[][]=new double[Q.length][U.length];
        double DTW_distance=0;
        for(int i=0;i<Q.length;i++)
        {
            for(int j=0;j<U.length;j++)
            {
                D[i][j]=Math.pow((Q[i]-U[j]),2);//求两个时间序列数据点之间的距离矩阵（欧氏距离的平方）
            }
        }
        for(int i=0;i<Q.length;i++)
        {
            for(int j=0;j<U.length;j++)
            {
                if(i==0 && j==0)
                {
                    R[i][j]=0;
                }
                else if((i==0 && j!=0)||(j==0 && i!=0))
                {
                    R[i][j]=Double.POSITIVE_INFINITY;
                }
                else
                {
                    R[i][j]=D[i][j]+get_min(R[i][j-1],R[i-1][j-1],R[i-1][j]);//查找最优路径
                }
            }
        }
        DTW_distance=R[Q.length-1][U.length-1];//最终两个时间序列的动态时间弯曲距离可由累计距离表示
        return DTW_distance;
    }

    public static double get_min(double a,double b,double c)
    {
        double min=0;
        if(a<=b && a<=c)
        {
            min=a;
        }
        else if(b<=c && b<=a)
        {
            min=b;
        }
        else if(c<=b && c<=a)
        {
            min=c;
        }
        return min;
    }

    public static double standard_DTW_distance(int num,double data[][][])
    {
        double distance=0;
        for(int i=0;i<10;i++)
        {
            for(int j=i+1;j<10;j++)
            {
                distance=distance+data[num][i][j];
            }
        }
        distance=distance/((10*9)/2);
        return distance;
    }

    public static Dataset getDataset_DTW(double data[],String row_key)
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();//设置数据
        for(int i=0;i<delta_t.length;i++)
        {
            dataset.addValue(data[i],row_key,String.valueOf(delta_t[i]));
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

    public static void write_DTW(double data[],String columnName_0,String columnName_1,String fileName)
    { //将生成的数据输出在项目的当前目录下
        try
        {
            HSSFWorkbook workbook=new HSSFWorkbook();//产生工作簿对象
            HSSFSheet sheet = workbook.createSheet();//产生工作表对象
            workbook.setSheetName(0,"sheet1");//设置第一个工作表的名称为sheet1

            HSSFRow row0= sheet.createRow((short)0);//生成第一行（表头）
            HSSFCell cell0,cell;
            cell0=row0.createCell((short)0);//生成该行第一个单元格
            cell0.setCellValue(columnName_0);//在单元格中输入内容
            cell0=row0.createCell((short)1);//生成该行第二个单元格
            cell0.setCellValue(columnName_1);

            int row_num=delta_t.length;//标准化欧式距离数组一共5行
            for(int i=1;i<=row_num;i++)
            {
                HSSFRow row= sheet.createRow((short)i);//生成第i行
                cell0=row.createCell((short)0);//每行第一个单元格为时间粒度
                cell0.setCellValue(delta_t[i-1]);
                cell = row.createCell((short)1);//每行第二个单元格为在该时间粒度下对应的标准化欧氏距离/相关系数
                cell.setCellValue(data[i-1]);
            }

            FileOutputStream fOut = new FileOutputStream(fileName);//在项目的当前目录下输出文件
            workbook.write(fOut);
            fOut.flush();
            fOut.close();

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
