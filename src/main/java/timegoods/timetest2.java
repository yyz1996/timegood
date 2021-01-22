package timegoods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class timetest2 {
    //商品名称
    //推荐时间粒度
    static double Threshold=0.8;
    static List<Double> pricetest=new ArrayList<Double>();//商品价格时间序列
    static List<Double> price_normalization=new ArrayList<Double>();//归一化后的商品价格时间序列
    static List<Double> price_PAA=new ArrayList<Double>();//PAA滤波后的商品价格时间序列
    static List<Integer> zero=new ArrayList<Integer>();//PAA滤波后的商品价格时间序列
    static String product="农副产品价格";

    public static void main(String[] args)
    {
        //读取数据到数组
        String filename_data = "D:\\实验室项目\\实验数据\\农副产品价格.xls";
        read_data_price(filename_data);

        System.out.println("时间序列的长度为： "+pricetest.size());//1.时间序列的总长度 n
        System.out.println("第一个时间点商品价格为： "+pricetest.get(0));//1.时间序列的总长度 n
        int t=5;//PAA滤波的尺度，5天表示一周

        get_PAA(t,pricetest,price_PAA);
        get_normalized(price_PAA,price_normalization);//平均值归一化

        int k=0;
        for(int i=0;i<price_normalization.size()-1;i++){//获取零点值的集合
            if(price_normalization.get(i)*price_normalization.get(i+1)<0){//最小二乘法求零点
                System.out.println("第 "+(k+1)+" 个零点位置为： "+i);//
                zero.add(k++,i);
            }
        }
        System.out.println("第3个零点位置为： "+zero.get(2) );
        System.out.println(product+" 推荐的时间粒度为： "+zero.get(2)*t );

//        get_normalized(pricetest,price_normalization);//平均值归一化
//        get_PAA(t,price_normalization,price_PAA);
        // PAA滤波
        //1.时间序列的总长度 n
        //2.选取PAA的时间粒度：ti(滤波长度)，
        //3.将序列分割为：pi段，pi=n/ti
        //4.若pi为小数，向下取整， 从后往前进行序列分割，（丢失部分信息，保证有效性）
        //5.第j=1,2,...,pi段 每段进行取平均值处理


//
//        for (int ti=1;ti<n_length/2;ti++){// 2.选取时间粒度：ti(滑动窗口长度)，ti=[1,2,3,4,.......,n/2]
//
//            //3.将序列分割为：pi段，pi=n/ti
//            int pi;
//            if(n_length%ti==0) pi=n_length/ti;
//            else{
//                pi=n_length/ti;//4.若pi为小数，向下取整， 从后往前进行序列分割，（丢失部分信息，保证有效性）
//                // pi=n_length/ti+1;//若pi为小数，向上取整， 最前面的一段价格需要填充，（从前往后取滑动窗口长度 作为 第一段）
//            }
//
//            List<Double> R_jk=new ArrayList<Double>();
//
//            // 5.第j=1,2,...,pi段 每段之间进行相似性度量，
//            int i1=0;
//            for(int j=0;j<pi;j++){
//                for(int k=j+1;k<pi;k++){
//                    R_jk.add(i1,get_R_jk(ti,pi,j,k));// ti时间粒度下，第j段和第k段进行相似性度量
//                    //System.out.println("第"+j+"段和第"+k+"段的相似性为 : "+R_jk.get(i1));
//                    i1++;
//                }
//            }
//
//        }

        //将生成的标准化欧式距离/标准化相关系数输出在excel
        String standard_corr_filename="D:\\实验室项目\\实验数据\\时间粒度测试\\standard_corr_"+product+".xls";
        //write_standard_data(R_ti2,"时间粒度","标准化相关系数",standard_corr_filename);

        //绘制平均归一化处理后的价格曲线
        String path_corr = "D:\\实验室项目\\实验数据\\时间粒度测试\\"+product+"时间粒度图.jpg";
        DefaultCategoryDataset ds_corr = (DefaultCategoryDataset) getDataset_price(price_normalization,"商品名称X");//设置绘图数据
        getLineChart(ds_corr,path_corr,"时间粒度","平均归一化价格",product+"平均归一化与时间粒度图",true);//绘图

        //绘制PAA滤波后的价格曲线
        DefaultCategoryDataset ds_corr2 = (DefaultCategoryDataset) getDataset_price(price_PAA,"商品名称X");//设置绘图数据
        getLineChart(ds_corr2,path_corr,"时间粒度","PAA处理后的价格",product+"PAA处理后的价格与时间粒度图",true);//绘图


    }

    private static void get_PAA(int ti,List<Double> data1, List<Double> data2) {//PAA滤波

        //ti当前为 5
        int pi=data1.size()/ti; //将序列分割为：pi段，pi=n/ti
        //对每段进行求均值
        for(int j=0;j<pi;j++){//计算ti时间粒度下，第j段的平均价格
            double sum=0;
            for(int i=0;i<ti;i++){
                sum=sum+data1.get(j*ti+i);
            }
            sum=sum/ti;
            data2.add(j,sum);//将结果存储
        }
      //   System.out.println("测试price_PAA[0]:  "+price_PAA.get(0));
        System.out.println("######已将价格进行PAA滤波！#######");
    }


    static public void get_normalized(List<Double> data1,List<Double> data2) // 平均归一化
    {
        double mean=0;
        double var=0;
        for(int i=0;i<data1.size();i++)
        {
            mean=mean+data1.get(i);
        }
        mean=mean/ data1.size(); //得到均值
        for(int i=0;i< data1.size();i++)
        {
            var=var+Math.pow((data1.get(i)-mean),2);
        }
        var=var/ data1.size(); //得到方差
        for(int i=0;i< data1.size();i++)
        {
            data2.add(i,(data1.get(i)-mean)/Math.sqrt(var));//得到平均归一化的结果
        }
        System.out.println("######已将价格进行平均归一化！#######");
    }


    public static void read_data_price(String fileName)
    { //读入数据到price数组
        try{
            InputStream input=new FileInputStream(fileName);//建立输入流
            HSSFWorkbook wb=new HSSFWorkbook(input);//初始化
            HSSFSheet sheet=wb.getSheetAt(0);//获取第二个表单

            int rowLength=sheet.getPhysicalNumberOfRows();//总行数
            HSSFRow hssfRow=sheet.getRow(0);//获取第一行（表头）
            int colLength=hssfRow.getPhysicalNumberOfCells();//总列数
            System.out.println("总行数"+rowLength);
            System.out.println("总列数"+colLength);

            //rowLength=835;//
            for(int i=1;i<500;i++)//去掉第一行表头
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=0;j<colLength;j++)//去掉第一列
                {
                    if(j==1){//第 列
                        if(i==1){
                            HSSFCell cell=row.getCell(j);//获得指定单元格
                            product=cell.getStringCellValue();
                            System.out.println(product);
                        }
                        else{
                            HSSFCell cell=row.getCell(j);//获得指定单元格
                            if(cell==null){
                                break;
                            }else{
                                pricetest.add(i-2,cell.getNumericCellValue());
                            }
                        }

                    }
                }
            }
            //System.out.println("测试pricetest[0]  "+pricetest.get(0));
            //System.out.println(price[259][0]);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }



    //将生成的数据输出在 项目的当前目录下
    public static void write_standard_data(double R_ti2[],String columnName_0,String columnName_1,String fileName)
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

            int row_num=pricetest.size()/2;//标准化相关系数 数组行数
            for(int i=1;i<row_num;i++)
            {
                HSSFRow row= sheet.createRow((short)i);//生成第i行
                cell0=row.createCell((short)0);//每行第一个单元格为时间粒度
                cell0.setCellValue(i);
                cell = row.createCell((short)1);//每行第二个单元格为在该时间粒度下对应的标准化欧氏距离/相关系数
                cell.setCellValue(R_ti2[i]);
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

    public static Dataset getDataset_price(List<Double> price1, String row_key)//设置绘图数据
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();//设置数据
        for(int i=0;i<price1.size();i++)
        {
            dataset.addValue(price1.get(i),row_key,String.valueOf(i));
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

            //画图
            ChartPanel chart_f = new ChartPanel(chart,true);
            JFrame jf = new JFrame();
            jf.add(chart_f);
            jf.setVisible(true);
            jf.setSize(1207, 500);
            jf.setLocationRelativeTo(null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static int recommend_time(double data[],int delta_t[],int time_granularity,double Threshold)
    {// 推荐的时间粒度为

        return time_granularity;
    }

}
