package DFT;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Example2 {

    static String product="test_data_5";
    static int NUM = 260*5;
    static double deltaTime = 1;//采样频率
    static double data[] = new double[NUM];

    public static void main(String[] args)
    {
        //String path="D:\\实验室项目\\问题反馈\\DFT\\test_data_5.xls";
        String path="D:\\实验室项目\\实验数据\\仿真数据.xls";
        read_data(path,data);

        Complex[] result = Dft.goertzelSpectrum(data);//DFT变换

        double frequency[]=new double[NUM];
        for(int i=0;i<NUM;i++)
        {
            frequency[i]=i*(0.5 / deltaTime / (result.length - 1));//频率
        }

        for (int i = 0; i < result.length; i++)
        {
            System.out.println("序号："+i+" :原数据："+data[i]+" 频率：" + frequency[i]+ "  幅值：" +  result[i].abs());
        }

        //求三个候选周期（峰值所对应的频率的倒数）
        int candidate_index[]=new int[3];
        long candidate_cycle[]=new long[3];
        get_candidate_cycle(result,candidate_index);//获取候选峰值对应的序号
        for(int i=0;i<candidate_cycle.length;i++)
        {
            candidate_cycle[i]=Math.round(1/frequency[candidate_index[i]]);//周期等于峰值对应频率的倒数（四舍五入取整）
            System.out.println("候选周期："+candidate_cycle[i]);
        }

        //求自相关系数
        get_normalized(data);
        double self_corr[]=new double[NUM/2];
        for(int i=0;i<self_corr.length;i++)
        {
            self_corr[i]=get_self_corr(data,i+1);
        }
        for(int i=0;i<candidate_cycle.length;i++)
        {
            int temp=(int)candidate_cycle[i]-1;
            if(self_corr[temp]>self_corr[temp+1] && self_corr[temp]>self_corr[temp-1])//判断是否是峰值，真正的周期所在位置应为峰值
            {
                System.out.println("在"+candidate_cycle[i]+"下，自相关系数为："+self_corr[temp]+",是峰值");
            }
            else
            {
                System.out.println("在"+candidate_cycle[i]+"下，自相关系数为："+self_corr[temp]+",不是峰值");
            }
        }

        //画dft图
        CategoryDataset dataset_dft = createDataset_dft(frequency,result);
        JFreeChart freeChart = createChart(dataset_dft,"dft","频率","幅值");
        ChartPanel chart_f = new ChartPanel(freeChart,true);
        JFrame jf = new JFrame();
        jf.add(chart_f, BorderLayout.WEST);
        jf.setVisible(true);
        jf.setSize(1400, 600);
        jf.setLocationRelativeTo(null);

        //画自相关系数图
        CategoryDataset dataset_self_corr = createDataset_self_corr(self_corr);
        JFreeChart freeChart_self_corr = createChart(dataset_self_corr,"self_corr","后移位数","自相关系数");
        ChartPanel chart_self_corr = new ChartPanel(freeChart_self_corr,true);
        JFrame jf_self_corr = new JFrame();
        jf.add(chart_self_corr, BorderLayout.EAST);
        jf.setVisible(true);
        jf.setSize(1400, 600);
        jf.setLocationRelativeTo(null);
    }

    static public void read_data(String filepath,double data[])
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
            for(int i=0;i<rowLength;i++)
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=0;j<colLength;j++)
                {
                    HSSFCell cell=row.getCell(j);//获得指定单元格
                    data[k]=cell.getNumericCellValue();
                    k++;
                }
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
            var=var+Math.pow((data[i]-mean),2);
        }
        var=var/ data.length;
        for(int i=0;i< data.length;i++)
        {
            data[i]=(data[i]-mean)/Math.sqrt(var);
        }
    }

    static public void get_candidate_cycle(Complex data[],int candidate_index[])
    {//得到候选周期
        List<java.lang.Integer> index=new ArrayList<>();
        for(int i=1;i<data.length-1;i++)
        {
            if(is_peak_value(data,i)==1)//判断哪些幅值是峰值，将峰值所在的序号记录下来
            {
                index.add(i);
            }
        }
        for(int i=0;i<index.size()-1;i++)//按幅值大小，从大到小排序
        {
            for(int j=0;j<index.size()-1-i;j++)
            {
                if(data[index.get(j)].abs()<data[index.get(j+1)].abs())
                {
                    int temp=index.get(j);
                    index.set(j,index.get(j+1));
                    index.set(j+1,temp);
                }
            }
        }

        for(int i=0;i<candidate_index.length;i++)//将前k个（最大的k个）峰值对应的序号记录，用于之后求候选周期
        {
            candidate_index[i]=index.get(i);
        }
    }

    static public int is_peak_value(Complex data[],int index)
    {//判断是否是峰值
        int flag=0;
        if(data[index-1].abs()<data[index].abs() && data[index+1].abs()<data[index].abs())
        {
            flag=1;
        }
        return flag;
    }

    static public double get_self_corr(double data[],int size)
    {//求自相关系数
        double data_1[]=new double[data.length-size];
        double data_2[]=new double[data.length-size];
        for(int i=0;i< data_1.length;i++)
        {
            data_1[i]=data[i];
            data_2[i]=data[i+size];
        }

        double mean1=0;
        double mean2=0;
        for(int i=0;i< data_1.length;i++)
        {
            mean1=mean1+data_1[i];
            mean2=mean2+data_2[i];
        }
        mean1=mean1/data_1.length;
        mean2=mean2/data_2.length;

        double x=0;
        double left=0;
        double right=0;
        for(int i=0;i<data_1.length;i++)
        {
            x=x+(data_1[i]-mean1)*(data_2[i]-mean2);
            left=left+Math.pow((data_1[i]-mean1),2);
            right=right+Math.pow((data_2[i]-mean2),2);
        }
        double corr=x/Math.sqrt(left*right);
        return corr;
    }

    public static CategoryDataset createDataset_dft(double row_key[],Complex data[])
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();//设置数据
        for(int i=0;i<data.length;i++)
        {
            dataset.addValue(data[i].abs(),"",String.valueOf(row_key[i]));
        }
        return dataset;
    }

    public static CategoryDataset createDataset_self_corr(double data[])
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();//设置数据
        for(int i=0;i<data.length;i++)
        {
            dataset.addValue(data[i],"",String.valueOf(i+1));
        }
        return dataset;
    }

    static public JFreeChart createChart(CategoryDataset categoryDateset,String title,String x_label,String y_label)
    {
        JFreeChart jfreechart = ChartFactory.createLineChart(title,x_label,y_label,categoryDateset, PlotOrientation.VERTICAL, false,false,false);
        CategoryPlot plot = (CategoryPlot)jfreechart.getPlot();
        plot.setBackgroundAlpha(0.5f);
        plot.setForegroundAlpha(1.0f);

        LineAndShapeRenderer renderer = (LineAndShapeRenderer)plot.getRenderer();
        renderer.setBaseShapesVisible(false);//每个数据的小方块标记
        renderer.setBaseLinesVisible(true);//数据点之间连线
        renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setBaseItemLabelsVisible(false);//数据标签
        return jfreechart;
    }
}
