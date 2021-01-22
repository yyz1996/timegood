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

public class model_matching_distance {

    //时间粒度
    static int[] delta_t = {5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100,105,110,115,120,125,130};
    //商品名称
    static String product = "copper";

    static double price[][] = new double[260][10];

    static int dis_sum[][][]=new int[delta_t.length][10][10];
    static double dis_standard[]=new double[delta_t.length];

    static String year[] = new String[10];

    public static void main(String[] args) {
        //读取数据到数组
        String filename_data = "D:\\D_文档\\项目\\数据\\" + product + "_test.xls";
        read_data_price(filename_data, price, year);

        for (int num = 0; num < delta_t.length; num++)
        {
            int dt = delta_t[num];
            int n = 260 / dt;

            double x[] = new double[dt];//每一段的x坐标
            double y[] = new double[dt];//每一段的y坐标
            double slope[][] = new double[n][10];//一共n段，每段的斜率
            String sign[][] = new String[n][10];//一共n段，每段的符号
            int dis[][][]=new int[n][10][10];//一共n段，每段中任意两年的距离

            double data[]=new double[260];//存放一年的时间序列

            for (int i = 0; i < 10; i++)
            {
                for(int k=0;k<260;k++)
                {
                    data[k]=price[k][i];//获取第i年的序列
                }
                double min=get_min(data);//获取第i年的最小值
                double max=get_max(data);//获取第i年的最大值
                for (int t = 0; t < n; t++)
                {
                    for (int j = t*dt; j < (t + 1) * dt; j++)
                    {
                        int temp=j-t*dt;
                        x[temp] = j+1;//获取第i年第t段的横坐标
                        y[temp] = data[j];//获取第i年第t段的纵坐标

                        double mean=get_mean(y);//获取第i年第t段的平均值
                        double b=(max-min)/3;

                        slope[t][i] = get_slope(x, y);//获取第i年第t段的斜率
                        sign[t][i] = get_sign(slope[t][i],mean,b,min,max);//获取第i年第t段的符号
                    }
                }
            }

            for(int t=0;t<n;t++)
            {
                for(int i=0;i<10;i++)
                {
                    for(int j=i+1;j<10;j++)
                    {
                        dis[t][i][j]=get_dis(sign,i,j,t);//获取第t段，第i年和第j年的距离
                    }
                }
            }

            //**********************************************************************************************************
            //**********************************************************************************************************
            //如何计算标准化模式匹配距离，不确定正确与否
            for(int i=0;i<10;i++)
            {
                for(int j=i+1;j<10;j++)
                {
                    dis_sum[num][i][j]=get_dist_sum(dis,i,j,n);//获取第num个时间粒度下，第i年和第j年的总距离（总距离=每段距离之和）
                    dis_sum[num][i][j]=dis_sum[num][i][j]/n;//为了消除段数不同对总距离的影响
                }
            }
        }

        for(int i=0;i< delta_t.length;i++)
        {
            dis_standard[i]=get_dis_standard(dis_sum,i);
        }
        //**************************************************************************************************************
        //**************************************************************************************************************

        //将生成的标准化模式匹配距离输出在excel
        String standard_dis_filename="standard_model_matching_distance_"+product+".xls";
        write_data(dis_standard,"时间粒度","标准化模式匹配距离",standard_dis_filename);

        //绘制模式匹配距离曲线
        String path_dis = "D:\\picture\\model_matching_dis\\model_matching_dis_"+product+".jpg";
        DefaultCategoryDataset ds = (DefaultCategoryDataset) getDataset_dis(dis_standard,product);
        getLineChart(ds,path_dis,"时间粒度","标准化模式匹配距离","标准化模式匹配距离与时间粒度图",true);
    }

    public static void read_data_price(String fileName, double price[][], String year[])
    { //读入数据到price数组
        try {
            InputStream input = new FileInputStream(fileName);//建立输入流
            HSSFWorkbook wb = new HSSFWorkbook(input);//初始化
            HSSFSheet sheet = wb.getSheetAt(0);//获取第一个表单

            int rowLength = sheet.getPhysicalNumberOfRows();//总行数
            HSSFRow hssfRow = sheet.getRow(0);//获取第一行（表头）
            int colLength = hssfRow.getPhysicalNumberOfCells();//总列数
            //System.out.println(rowLength);
            //System.out.println(colLength);
            for (int k = 1; k < colLength; k++) {
                int temp = (int) hssfRow.getCell(k).getNumericCellValue();
                year[k - 1] = String.valueOf(temp);
            }
            for (int i = 1; i < rowLength; i++)//去掉第一行表头
            {
                HSSFRow row = sheet.getRow(i);//获取表的每一行
                for (int j = 1; j < colLength; j++)//去掉第一列
                {
                    HSSFCell cell = row.getCell(j);//获得指定单元格
                    price[i - 1][j - 1] = cell.getNumericCellValue();
                }
            }
            //System.out.println(price[0][0]);
            //System.out.println(price[259][0]);
        } catch (IOException e) {
            e.printStackTrace();
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

    public static int get_dis(String sign[][],int i,int j,int t)
    {//返回第t段，第i年和第j年的距离
        int z=0;
        if(sign[t][i]==sign[t][j])//如果符号一致，距离为0
            z=0;
        else
            z=1;//否则，距离为1
        return z;
    }

    public static int get_dist_sum(int dis[][][],int i,int j,int n)
    {//返回共有n段的第i年和第j年的总距离
        int sum=0;
        for(int k=0;k<n;k++)
        {
            sum=sum+dis[k][i][j];//总距离=每段距离之和
        }
        return sum;
    }

    public static double get_dis_standard(int dis_sum[][][],int num)
    {
        double sum=0;
        for(int i=0;i<10;i++)
        {
            for(int j=i+1;j<10;j++)
            {
                sum=sum+dis_sum[num][i][j];
            }
        }
        sum=sum/((9+10)/2);
        return sum;
    }

    public static void write_data(double data[],String columnName_0,String columnName_1,String fileName)
    {
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

            int row_num=delta_t.length;
            for(int i=1;i<=row_num;i++)
            {
                HSSFRow row= sheet.createRow((short)i);//生成第i行
                cell0=row.createCell((short)0);//每行第一个单元格为时间粒度
                cell0.setCellValue(delta_t[i-1]);
                cell = row.createCell((short)1);//每行第二个单元格为在该时间粒度下对应的模式匹配距离
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

    public static Dataset getDataset_dis(double data[], String row_key)
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
}

