package timegoods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
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

public class timetest {
    //商品名称
    //推荐时间粒度
    static double Threshold=0.8;
    static List<Double> pricetest=new ArrayList<Double>();//商品价格时间序列
    static String product="农副产品价格";

    public static void main(String[] args)
    {
        //读取数据到数组
        String filename_data = "D:\\实验室项目\\问题反馈\\DFT\\test_data_5.xls";
        //String filename_data = "D:\\实验室项目\\实验数据\\test2.xls";
        //String filename_data = "D:\\实验室项目\\实验数据\\"+product+".xls";
        read_data_price(filename_data);

        int n_length=pricetest.size();//1.时间序列的总长度 n

        // 发现商品价格的波动周期： 自我匹配，非自我匹配
        //1.时间序列的总长度 n
        //2.选取时间粒度：ti(滑动窗口长度)，ti=[1,2,3,4,.......,n/2]
        //3.将序列分割为：pi段，pi=n/ti
        // 考虑到价格数据的时效性（即 近期价格比远期价格更有价格 ），因此需要从后往前分割序列，（从后往前进行度量）
        //4.若pi为小数，向下取整， 从后往前进行序列分割，（丢失部分信息，保证有效性）
        //  若pi为小数，向上取整， 最前面的一段价格需要填充，（从前往后取滑动窗口长度 作为 第一段）
        //  若pi为整数，直接进行序列分割。
        //5.第j=1,2,...,pi段 每段之间进行相似性度量，
        //6. 并标准化相似度

        List<Double> R_ti=new ArrayList<Double>();// ti时间粒度下的 标准化相似度
        double R_ti2[]=new double[n_length/2];

        for (int ti=1;ti<n_length/2;ti++){// 2.选取时间粒度：ti(滑动窗口长度)，ti=[1,2,3,4,.......,n/2]

            //3.将序列分割为：pi段，pi=n/ti
            int pi;
            if(n_length%ti==0) pi=n_length/ti;
            else{
                pi=n_length/ti;//4.若pi为小数，向下取整， 从后往前进行序列分割，（丢失部分信息，保证有效性）
                // pi=n_length/ti+1;//若pi为小数，向上取整， 最前面的一段价格需要填充，（从前往后取滑动窗口长度 作为 第一段）
            }

            List<Double> R_jk=new ArrayList<Double>();

            // 5.第j=1,2,...,pi段 每段之间进行相似性度量，
            int i1=0;
            for(int j=0;j<pi;j++){
                for(int k=j+1;k<pi;k++){
                    R_jk.add(i1,get_R_jk(ti,pi,j,k));// ti时间粒度下，第j段和第k段进行相似性度量
                    //System.out.println("第"+j+"段和第"+k+"段的相似性为 : "+R_jk.get(i1));
                    i1++;
                }
            }

            //6. 并标准化相似度
            double sum=0;
            for(int i2=0;i2<R_jk.size();i2++){
                sum=sum+R_jk.get(i2);// 标准化相似度
                //sum=sum+Math.pow(R_jk.get(i2),2);// 标准化相似度 平方处理
            }
            //R_ti.add(ti,sum/R_jk.size()); // 标准化相似度
            //System.out.println("时间粒度为："+ti+"时的标准化相似度为： "+R_ti.get(ti));
            R_ti2[ti]=sum/R_jk.size();
            System.out.println("时间粒度为： "+ti+"  时的标准化相似度为： "+ R_ti2[ti]);
            System.out.println("################################################");
        }

        //将生成的标准化欧式距离/标准化相关系数输出在excel
        String standard_corr_filename="D:\\实验室项目\\实验数据\\时间粒度图表\\standard_corr_"+product+".xls";
        write_standard_data(R_ti2,"时间粒度","标准化相关系数",standard_corr_filename);

        //绘制相关系数曲线
        String path_corr = "D:\\实验室项目\\实验数据\\时间粒度图表\\"+product+"时间粒度图.jpg";
        DefaultCategoryDataset ds_corr = (DefaultCategoryDataset) getDataset_corr_dist(R_ti2,"商品名称X");//设置绘图数据
        getLineChart(ds_corr,path_corr,"时间粒度","标准化相关系数",product+"标准化相关系数与时间粒度图",true);//绘图

        //绘制在不同时间粒度下的10年价格图

        //根据阈值选定合适的时间粒度
    }

    private static double get_R_jk(int ti,int pi,int j, int k) {// ti时间粒度(滑动窗口长度)下，第j段和第k段进行相似性度量
        double rjk=0;
        double sum_shang = 0;//公式上部分的求和
        double sum_zuoxia=0;//公式左下部分的求和
        double sum_youxia=0;//公式右下部分的求和

        double mean_j=0;
        double mean_k=0;
        mean_j=get_mean(j,ti);//计算ti时间粒度下，第j段的平均价格
        mean_k=get_mean(k,ti);//计算ti时间粒度下，第k段的平均价格

        for(int i=0;i<ti;i++) {
            double ai=0;//相关系数公式中的一项
            double bi=0;
            ai=pricetest.get(j*ti+i);
            bi=pricetest.get(k*ti+i);
            sum_shang=sum_shang + ((ai-mean_j)*(bi-mean_k));
            sum_zuoxia=sum_zuoxia+Math.pow(ai-mean_j, 2);
            sum_youxia=sum_youxia+Math.pow(bi-mean_k, 2);
        }
        if(sum_zuoxia==0||sum_youxia==0) return 0;
        else{
            rjk=sum_shang/(Math.sqrt(sum_zuoxia)*Math.sqrt(sum_youxia));
            return rjk;
        }
    }

    private static double get_mean(int j, int ti) {//计算ti时间粒度下，第j段的平均价格

        double sum=0;
        for(int i=0;i<ti;i++){
            sum=sum+pricetest.get(j*ti+i);
        }
        sum=sum/ti;
        return sum;
    }

    public static void read_data_price(String fileName)
    { //读入数据到price数组
        try{
            InputStream input=new FileInputStream(fileName);//建立输入流
            HSSFWorkbook wb=new HSSFWorkbook(input);//初始化
            HSSFSheet sheet=wb.getSheetAt(0);//获取第一个表单

            int rowLength=sheet.getPhysicalNumberOfRows();//总行数
            HSSFRow hssfRow=sheet.getRow(0);//获取第一行（表头）
            int colLength=hssfRow.getPhysicalNumberOfCells();//总列数
            System.out.println("总行数"+rowLength);
            System.out.println("总列数"+colLength);

            //rowLength=835;//
            for(int i=0;i<rowLength;i++)//去掉第一行表头
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=0;j<colLength;j++)//去掉第一列
                {
                    if(j==0){//第 列
                        if(i==0){
                            HSSFCell cell=row.getCell(j);//获得指定单元格
                            product=cell.getStringCellValue();
                        }
                        else{
                            HSSFCell cell=row.getCell(j);//获得指定单元格
                            if(cell==null){
                                break;
                            }else{
                                pricetest.add(i-1,cell.getNumericCellValue());
                            }
                        }

                    }
                }
            }
            System.out.println("测试pricetest[0]  "+pricetest.get(0));
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

    public static Dataset getDataset_corr_dist(double R_ti2[], String row_key)//设置绘图数据
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();//设置数据
        for(int ti=0;ti<pricetest.size()/2;ti++)
        {
           // System.out.println("########添加数据：  "+R_ti2[ti]);
            dataset.addValue(R_ti2[ti],row_key,String.valueOf(ti));
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
           // lineandshaperenderer.setBaseShapesVisible(true);
            //是否显示折线
            lineandshaperenderer.setBaseLinesVisible(true);
//            //显示折点数据
//            lineandshaperenderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
//            lineandshaperenderer.setBaseItemLabelsVisible(num_label);

            //没有数据时显示的文字说明
            categoryPlot.setNoDataMessage("没有数据显示");

            //设置面板字体
            Font labelFont = new Font("", Font.TRUETYPE_FONT, 12);

            //导出图片
            ChartUtilities.saveChartAsJPEG(new File(filePath), chart, 2400, 1000);

            //画图
            ChartPanel chart_f = new ChartPanel(chart,true);
            JFrame jf = new JFrame();
            jf.add(chart_f);
            jf.setVisible(true);
            jf.setSize(2800, 1200);
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
