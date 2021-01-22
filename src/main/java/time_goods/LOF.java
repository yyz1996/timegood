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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LOF {

    static String product="abnormal_data_10";

    private static int INT_K = 5;//正整数K

    static double price[]=new double[260];

    public static void main(String[] args)
    {
        //读取数据到price[]数组
        String path="D:\\数据\\abnormal\\"+product+".xls";
        read_data(path,price);

        ArrayList<DataNode> dpoints = new ArrayList<DataNode>();
        for(int i=0;i<price.length;i++)
        {
            double[] temp={i+1,price[i]};
            dpoints.add(new DataNode(String.valueOf(i+1),temp,i+1));
        }

        LOF lof=new LOF();
        //计算获得异常点
        List<DataNode> nodeList=lof.get_OutLineNode(dpoints);

        //绘制价格图
        String path_price="D:\\picture\\abnormal_data\\simulate_price\\"+product+".jpg";
        DefaultCategoryDataset ds_price = (DefaultCategoryDataset) getDateset(price,product);
        getLineChart(ds_price,path_price,"工作日","价格","价格图",false);
        //绘制计算出的异常lof值
        String path_lof="D:\\picture\\abnormal_data\\LOF_result\\"+product+".jpg";
        DefaultCategoryDataset ds_lof= (DefaultCategoryDataset) getDateset_lof(nodeList,product);
        getLineChart(ds_lof,path_lof,"工作日","lof值","价格异常点图",false);

        Collections.sort(nodeList,new LofComparator());//按异常值大小降序排序
        for(DataNode node:nodeList)
        {
            System.out.println(node.getNodeName() + ":" + node.getLof());
        }
    }

    public static void read_data(String path,double price[])
    {
        try
        {
            InputStream input=new FileInputStream(path);//建立输入流
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

    public List<DataNode> get_OutLineNode(List<DataNode> allNodes)
    {
        List<DataNode> kd_kn_List=get_kd_kn(allNodes);
        cal_reach_dis(kd_kn_List);
        cal_reach_density(kd_kn_List);
        cal_lof(kd_kn_List);

        return kd_kn_List;
    }

    private void cal_lof(List<DataNode> kd_kn_List)
    {
        //计算每个点的局部离群点因子
        for(DataNode node:kd_kn_List)
        {
            List<DataNode> temp_Nodes=node.getkNeighbor();
            double sum=0.0;
            for(DataNode temp_Node:temp_Nodes)
            {
                double reach_dis=get_reach_density(temp_Node.getNodeName(),kd_kn_List);
                sum=reach_dis/node.getReachDensity()+sum;
            }
            sum=sum/(double) INT_K;
            node.setLof(sum);
        }
    }

    private void cal_reach_density(List<DataNode> kn_kd_List)
    {
        //计算每个点的可达密度
        for(DataNode node:kn_kd_List)
        {
            List<DataNode> temp_Nodes=node.getkNeighbor();
            double sum=0.0;
            double reach_dis=0.0;
            for(DataNode temp_Node:temp_Nodes)
            {
                sum=temp_Node.getReachDis()+sum;
            }
            reach_dis=(double)INT_K/sum;
            node.setReachDensity(reach_dis);
        }
    }

    private void cal_reach_dis(List<DataNode> kd_kn_List)
    {
        //计算每个点的可达距离
        for(DataNode node:kd_kn_List)
        {
            List<DataNode> temp_Nodes=node.getkNeighbor();//获取每个点的kNeighbor
            for(DataNode temp_Node:temp_Nodes)
            {
                double k_dis=get_k_dis(temp_Node.getNodeName(),kd_kn_List);//获取temp_Node点的K-距离

                if(k_dis<temp_Node.getDistance())
                {
                    temp_Node.setReachDis(temp_Node.getDistance());//reach_dis(p,o)=max{k-distance(o),d(p,o)}
                }
                else
                {
                    temp_Node.setReachDis(k_dis);
                }
            }
        }
    }

    private double get_k_dis(String nodeName,List<DataNode> nodeList)
    {
        //获取某个点的K-距离
        double k_dis=0;
        for(DataNode node:nodeList)
        {
            if(nodeName.trim().equals(node.getNodeName().trim()))//找到对应的点
            {
                k_dis=node.getkDistance();//获取K-距离
                break;
            }
        }
        return k_dis;
    }

    private double get_reach_density(String nodeName,List<DataNode> nodeList)
    {
        //获取某个点的可达密度
        double reach_density=0;
        for(DataNode node:nodeList)
        {
            if(nodeName.trim().equals(node.getNodeName().trim()))//找到对应的点
            {
                reach_density=node.getReachDensity();//获取可达密度
                break;
            }
        }
        return reach_density;
    }

    private List<DataNode> get_kd_kn(List<DataNode> allNodes)
    {
        //计算给定点A与其他点B的欧几里得距离
        //找到A点前K位的B，记录到A的K-邻域变量（kNeighbor）
        //找到A的K距离，然后记录到A的K-距离变量（KDistance）中
        List<DataNode> kn_kd_List=new ArrayList<DataNode>();
        for(int i=0;i<allNodes.size();i++)
        {
            List<DataNode> temp_NodeList=new ArrayList<DataNode>();
            DataNode nodeA=new DataNode(allNodes.get(i).getNodeName(),allNodes.get(i).getDimensioin(),allNodes.get(i).getIndex());//获取A的名称和维度
            for(int j=0;j<allNodes.size();j++)
            {
                DataNode nodeB=new DataNode(allNodes.get(j).getNodeName(),allNodes.get(j).getDimensioin(),allNodes.get(j).getIndex());//获取B的名称和维度
                double temp_dis=get_disAB(nodeA,nodeB);//计算A和B的欧式距离
                nodeB.setDistance(temp_dis);//记录在B的Distance变量中
                temp_NodeList.add(nodeB);//将B加入临时链表中
            }

            Collections.sort(temp_NodeList,new DistComparator());//依据欧几里得距离升序排序
            for(int k=1;k<INT_K;k++)//找到B的前五位的欧几里距离点
            {
                nodeA.getkNeighbor().add(temp_NodeList.get(k));//记录在A的kNeighbor变量中
                if(k==INT_K-1)//找到B的第K位距离
                {
                    nodeA.setkDistance(temp_NodeList.get(k).getDistance());//记录在A的KDistance变量中
                }
            }
            kn_kd_List.add(nodeA);
        }
        return kn_kd_List;
    }

    private double get_disAB(DataNode A,DataNode B)
    {//计算A、B间的欧氏距离
        double dis=0.0;
        double[] dimA=A.getDimensioin();//A的维度
        double[] dimB=B.getDimensioin();//B的维度
        if(dimA.length==dimB.length)
        {
            for(int i=0;i<dimA.length;i++)
            {
                double temp=Math.pow(dimA[i]-dimB[i],2);
                dis=dis+temp;
            }
            dis=Math.sqrt(dis);//计算欧氏距离
        }
        return dis;
    }

    //升序排序
    static class DistComparator implements Comparator<DataNode> {
        public int compare(DataNode A, DataNode B) {
            //return A.getDistance() - B.getDistance() < 0 ? -1 : 1;
            if((A.getDistance()-B.getDistance())<0)
                return -1;
            else if((A.getDistance()-B.getDistance())>0)
                return 1;
            else return 0;
        }
    }

   //降序排序
   static class LofComparator implements Comparator<DataNode> {
        public int compare(DataNode A, DataNode B) {
            //return A.getLof() - B.getLof() < 0 ? 1 : -1;
            if((A.getLof()-B.getLof())<0)
                return 1;
            else if((A.getLof()-B.getLof())>0)
                return -1;
            else return 0;
        }
    }

    public static Dataset getDateset(double data[],String row_key)
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();//设置数据
        for(int i=0;i<data.length;i++)
        {
            dataset.addValue(data[i],row_key,String.valueOf(i+1));
        }
        return dataset;
    }

    public static Dataset getDateset_lof(List<DataNode> nodeList,String row_key)
    {
        DefaultCategoryDataset dataset= new DefaultCategoryDataset();
        for(DataNode node:nodeList)
        {
            dataset.addValue( node.getLof(),row_key,node.getNodeName());
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
