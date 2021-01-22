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

public class normalized_standard_corr_dist {

    //时间粒度
    static int[] delta_t ={1,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100,105,110,115,120,125,130};
    //商品名称
    static String product="simulate_data_3";

    static double price[][]=new double[260][10];
    static double meanprice[]=new double[10];
    static double normalized_price[][]=new double[260][10];
    static double normalized_meanprice[]=new double[10];

    static double dist[][][]=new double[delta_t.length][10][10];
    static double r[][][]=new double[delta_t.length][10][10];
    static double Dist[]=new double[delta_t.length];
    static double Corr[]=new double[delta_t.length];

    static String year[]=new String[10];

    public static void main(String[] args)
    {

        //读取数据到数组
        String filename_data = "D:\\D_文档\\项目\\数据\\" + product + "_test.xls";
        read_data_price(filename_data, price,year);

        get_normalized_price(price,normalized_price);
        get_data_meanprice(normalized_price,normalized_meanprice);

        for (int num = 0; num < delta_t.length; num++)
        {
            int dt = delta_t[num];
            int n = 260 / dt;
            for (int i = 0; i < 10; i++)
            {
                for (int j = i + 1; j < 10; j++)
                {
                    dist[num][i][j] = get_dist(normalized_price, i, j, dt, n);//求欧式距离
                    r[num][i][j] = get_r(normalized_price, normalized_meanprice, i, j, dt, n);//求相关系数
                }
            }

            //在该时间粒度条件下，执行 标准化
            Dist[num]=standard_dist(num,dist);
            Corr[num]=standard_r(num,r);
            System.out.println(Corr[num]);
        }

        //将生成的标准化欧式距离/标准化相关系数输出在excel
        String standard_dist_filename="normalized_standard_dist_"+product+".xls";
        String standard_corr_filename="normalized_standard_corr_"+product+".xls";
        write_standard_data(Dist,"时间粒度","归一化后标准化欧氏距离",standard_dist_filename);
        write_standard_data(Corr,"时间粒度","归一化后标准化相关系数",standard_corr_filename);

        //绘制欧式距离曲线
        String path_dist = "D:\\picture\\normalized_standard_dist\\normalized_standard_dist_"+product+".jpg";
        DefaultCategoryDataset ds_dist = (DefaultCategoryDataset) getDataset_corr_dist(Dist,product);
        getLineChart(ds_dist,path_dist,"时间粒度","归一化后标准化欧氏距离","归一化后标准化欧氏距离与时间粒度图",true);

        //绘制相关系数曲线
        String path_corr = "D:\\picture\\normalized_standard_corr\\normalized_standard_corr_"+product+".jpg";
        DefaultCategoryDataset ds_corr = (DefaultCategoryDataset) getDataset_corr_dist(Corr,product);
        getLineChart(ds_corr,path_corr,"时间粒度","归一化后标准化相关系数","归一化后标准化相关系数与时间粒度图",true);

        //绘制在不同时间粒度下的10年价格图
        for(int i=0;i<delta_t.length;i++)
        {
            int time_num=260/delta_t[i];
            double price_time_granularity[][]=new double[time_num][10];

            String path_price="D:\\picture\\normalized_price\\normalized_price_"+product+"_"+i+".jpg";
            String xlabel_price="时间";
            String ylabel_price="归一化后价格";
            String title_price="归一化后时间粒度为"+delta_t[i]+"时"+product+"十年价格走势图";
            DefaultCategoryDataset ds_price=(DefaultCategoryDataset) getDataset_price(price_time_granularity,normalized_price,delta_t[i],time_num,year);
            getLineChart(ds_price,path_price,xlabel_price,ylabel_price,title_price,false);
        }


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

    public static void get_normalized_price(double price[][],double normalized_price[][])
    {
        //将价格进行归一化处理
        //min-max标准化
        /*int row_length= price.length;
        int column_length=price[0].length;
        double min=get_min(price);
        double max=get_max(price);
        for(int j=0;j<column_length;j++)
        {
            for(int i=0;i<row_length;i++)
            {
                normalized_price[i][j]=(price[i][j]-min)/(max-min);
            }
        }*/

        //z-score标准化
        //z-score要求原始数据近似高斯分布，否则结果会很糟糕
        int row_length= price.length;
        int column_length=price[0].length;
        double mean=get_mean(price,row_length,column_length);//均值
        double variance=get_variance(price,mean,row_length,column_length);//方差
        for(int i=0;i<row_length;i++)
        {
            for(int j=0;j<column_length;j++)
            {
                normalized_price[i][j]=(price[i][j]-mean)/Math.sqrt(variance);
            }
        }
    }

    public static double get_mean(double data[][],int row,int column)
    {
        double sum=0;
        for(int i=0;i<row;i++)
        {
            for(int j=0;j<column;j++)
            {
                sum=sum+data[i][j];
            }
        }
        double mean=sum/(row*column);
        return mean;
    }

    public static double get_variance(double data[][],double mean,int row,int column)
    {
        double var=0;
        for(int i=0;i<row;i++)
        {
            for(int j=0;j<column;j++)
            {
                var=var+Math.pow((data[i][j]-mean),2);
            }
        }
        return var/(row*column);
    }

    public static double get_min(double data[][])
    {
        double min=data[0][0];
        for(int i=0;i<data.length;i++)
        {
            for(int j=0;j<data[0].length;j++)
            {
                if(data[i][j]<min)
                {
                    min=data[i][j];
                }
            }

        }
        return min;
    }

    public static double get_max(double data[][])
    {
        double max=data[0][0];
        for(int i=0;i<data.length;i++)
        {
            for(int j=0;j<data[0].length;j++)
            {
                if(data[i][j]>max)
                {
                    max=data[i][j];
                }
            }

        }
        return max;
    }

    public static void get_data_meanprice(double price[][],double meanprice[])
    {//计算每年的平均价格
        int row_length= price.length;
        int column_length=price[0].length;
        for(int i=0;i<column_length;i++)
        {
            double column_sum=0;
            for(int j=0;j<row_length;j++)
            {
                column_sum=column_sum+price[j][i];
            }
            meanprice[i]=(double)(Math.round(column_sum*100/row_length)/100.0);//保留两位小数
            //System.out.println(meanprice[i]);
        }
    }

    private static double get_dist(double[][] price,int i, int j, int dt, int n)
    {	// 求欧式距离
        double sumdist=0;
        for(int t=0;t<n;t++) {
            double xti=get_xti(price,i,dt,n,t);//第i年第t个时间段的平均价格
            double xtj=get_xti(price,j,dt,n,t);//第j年第t个时间段的平均价格
            sumdist=sumdist+Math.pow(xti-xtj,2);
        }
        sumdist=Math.sqrt(sumdist);
        //System.out.println("在时间粒度 "+dt+" 的情况下，第"+i+"和第"+j+"年的欧式距离为：");
        //System.out.println(sumdist);
        return sumdist;
    }

    private static double get_r(double price[][],double meanprice[],int i, int j, int dt, int n)
    {	// 求相关系数
        double rij=0;
        double sum_shang = 0;//公式上部分的求和
        double  sum_zuoxia=0;//公式左下部分的求和
        double sum_youxia=0;//公式右下部分的求和
        for(int t=0;t<n;t++) {
            double xti=get_xti(price,i,dt,n,t);
            double xtj=get_xti(price,j,dt,n,t);
            sum_shang=sum_shang + ((xti-meanprice[i])*(xtj-meanprice[j]));
            sum_zuoxia=sum_zuoxia+Math.pow(xti-meanprice[i], 2);
            sum_youxia=sum_youxia+Math.pow(xtj-meanprice[j], 2);
        }
        rij=sum_shang/(Math.sqrt(sum_zuoxia)*Math.sqrt(sum_youxia));
        //System.out.println("在时间粒度 "+dt+" 的情况下，第"+i+"和第"+j+"年的相关系数为：");
        //System.out.println(rij);
        return rij;
    }

    private static double get_xti(double price[][],int i, int dt, int n,int t)
    {	//返回第i年，在dt时间粒度下，分为n段，第t段的平均价格
        double sum_xti=0;
        for(int k=t*dt;k<(t+1)*dt;k++) {
            sum_xti=sum_xti+price[k][i];
        }
        sum_xti=sum_xti/dt;
        //System.out.println("在时间粒度 "+dt+" 的情况下，第"+i+"年,分为"+n+"段，第"+t+"段的平均价格为：");
        //System.out.println(sum_xti);
        return sum_xti;
    }

    public static double standard_dist(int num,double dist[][][])
    {	//标准化欧式距离
        double sum_standard_dist=0;
        for(int i=0;i<10;i++) {
            for(int j=i+1;j<10;j++){
                sum_standard_dist=sum_standard_dist+dist[num][i][j];
            }
        }
        sum_standard_dist=sum_standard_dist/((10*9)/2);
        //System.out.println("在时间粒度 "+delta_t[num]+" 的情况下,标准化的欧式距离为：");
        //System.out.println(sum_standard_dist);
        return sum_standard_dist;
    }

    private static double standard_r(int num,double r[][][])
    {
        //标准化相关系数
        double standard_r=0;
        for(int i=0;i<10;i++) {
            for(int j=i+1;j<10;j++){
                standard_r=standard_r+Math.pow(r[num][i][j], 2);
            }
        }
        standard_r=standard_r/((10*9)/2);
        //System.out.println("在时间粒度 "+delta_t[num]+" 的情况下,标准化的相关系数为：");
        //System.out.println(standard_r);
        return standard_r;
    }

    public static void write_standard_data(double data[],String columnName_0,String columnName_1,String fileName)
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

    public static Dataset getDataset_corr_dist(double data[],String row_key)
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();//设置数据
        for(int i=0;i<delta_t.length;i++)
        {
            dataset.addValue(data[i],row_key,String.valueOf(delta_t[i]));
        }
        return dataset;
    }

    public static Dataset getDataset_price(double data[][],double price[][],int dt,int time_num,String row_key[])
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();
        for(int m=0;m<10;m++)
        {
            for(int n=0;n<time_num;n++)
            {
                //返回第m年，在dt的时间粒度下，分为time_num段，第n段的平均价格
                data[n][m]=get_xti(price,m,dt,time_num,n);
                dataset.addValue(data[n][m],row_key[m],String.valueOf((n+1)*dt));
            }
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
