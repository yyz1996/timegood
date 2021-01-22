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

public class draw_DTW_distance {

    static int[] delta_t ={1,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100,105,110,115,120,125,130};
    static String[] product={"aluminium","copper","corn","gold","lead","nickel","platinum","rice","silver","tin","zinc"};
    static int time_num=delta_t.length;
    static int product_num= product.length;
    static double DTW_distance[][]=new double [time_num][product_num];//每列存储一种商品的标准化动态时间弯曲距离

    public static void main(String[] args)
    {
        for(int i=0;i<product_num;i++)
        {
            String filename_DTW="D:\\Java\\Test\\standard_DTW_distance_"+product[i]+".xls";
            double temp_DTW[]=new double[time_num];
            read_data(filename_DTW,temp_DTW);

            for(int j=0;j<time_num;j++)
            {
                DTW_distance[j][i]=temp_DTW[j];
            }
        }

        //绘制动态时间弯曲距离曲线
        String path_DTW="D:\\picture\\all_product_standard_DTW_distance\\product_standard_DTW_distance.jpg";
        DefaultCategoryDataset ds_DTW = (DefaultCategoryDataset) getDataSet(DTW_distance,product,delta_t,product_num,time_num);
        getLineChart(ds_DTW,path_DTW,"时间粒度","标准化动态时间弯曲距离","标准化动态时间弯曲距离与时间粒度图",false);
    }

    public static void read_data(String fileName,double data[])
    {
        try
        {
            InputStream input=new FileInputStream(fileName);//建立输入流
            HSSFWorkbook wb=new HSSFWorkbook(input);//初始化
            HSSFSheet sheet=wb.getSheetAt(0);//获取第一个表单

            int rowLength=sheet.getPhysicalNumberOfRows();//总行数
            HSSFRow hssfRow=sheet.getRow(0);
            int colLength=hssfRow.getPhysicalNumberOfCells();//总列数
            //System.out.println(rowLength);
            //System.out.println(colLength);
            for(int i=1;i<rowLength;i++)//去掉第一行表头
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=1;j<colLength;j++)//去掉第一列
                {
                    HSSFCell cell=row.getCell(j);//获得指定单元格
                    data[i-1]=cell.getNumericCellValue();
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public static Dataset getDataSet(double data[][],String product[],int delta_t[],int product_num,int time_num)
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();
        for(int i=0;i<product_num;i++)
        {
            for(int j=0;j<time_num;j++)
            {
                dataset.addValue(data[j][i],product[i],String.valueOf(delta_t[j]));
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
