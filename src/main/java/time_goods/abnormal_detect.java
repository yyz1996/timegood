package time_goods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class abnormal_detect {

    //时间粒度
    static int[] delta_t ={1,5,10,15,20,25,26,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100,105,110,115,120,125,130};
    //商品名称
    static String product="simulate_data_3";
    //修正后的文件
    static String filename_replaced_data="abnormal_zinc_replaced_1.xls";
    //推荐时间粒度
    static double Threshold=0.65;

    static double pre_price[][]=new double[260][10];//原始数据
    static double price[][]=new double[260][10];//归一化后的数据
    static double meanprice[]=new double[10];//存放每年的均值
    static double dist[][][]=new double[delta_t.length][10][10];
    static double r[][][]=new double[delta_t.length][10][10];
    static double Dist[]=new double[delta_t.length];
    static double Corr[]=new double[delta_t.length];
    static String year[]=new String[10];

    public static void main(String[] args)
    {
        //读取数据到数组
        String filename_data = "D:\\D_文档\\项目\\数据\\" + product + "_test.xls";
        //String filename_data ="D:\\Java\\Test\\"+product+".xls";
        read_data_price(filename_data, pre_price,year);
        //对数据进行归一化处理
        get_normalized_price(pre_price,price);
        //read_data_price(filename_data,price,year);
        //获取每年的均值
        get_data_meanprice(price,meanprice);

        for(int num = 0; num < delta_t.length; num++)
        {
            int dt = delta_t[num];
            int n = 260 / dt;
            if(n*dt<260)
            {
                n=n+1;
            }

            for (int i = 0; i < 10; i++)
            {
                for (int j = i + 1; j < 10; j++)
                {
                    dist[num][i][j] = get_dist(price, i, j, dt, n);//求欧式距离
                    r[num][i][j] = get_r(price, meanprice, i, j, dt, n);//求相关系数
                }
            }

            //在该时间粒度条件下，执行标准化
            Dist[num] = standard_dist(num, dist);
            Corr[num] = standard_r(num, r);
            System.out.println(delta_t[num]+":"+Corr[num]);
        }
        //根据阈值选定合适的时间粒度
        int time_granularity = recommend_time(Corr, delta_t, Threshold);
        if(time_granularity!=0)
        {
            System.out.println(product + "在阈值为" + Threshold + "时，推荐的时间粒度为");
            System.out.println(time_granularity);
        }
        else
        {
            System.out.println(product + "在阈值为" + Threshold + "时，无可推荐时间粒度");
        }

        //*************************************************************************************
        //*************************************************************************************
        //检测异常时段
        //time_granularity=26;
        int sequence_num=260/time_granularity;//根据推荐时间粒度划分子序列
        if(time_granularity*sequence_num!=260)
        {
            sequence_num=sequence_num+1;
        }

        Struct_Degree Degree[][] =new Struct_Degree[sequence_num][10];//记录每年每个子序列的异常度
        for(int i=0;i<sequence_num;i++)
        {
            for(int j=0;j<10;j++)
            {
                Degree[i][j]=new Struct_Degree();
            }
        }

        double th[]=new double[10];//记录每年的异常度阈值
        int abnormal_degree_num[]=new int[10];//记录每年子序列中异常子序列的数目

        for(int i=0;i<10;i++)
        {
            double price_per_year[] = new double[260];//存放某一年的价格数据
            for (int k = 0; k < 260; k++)
            {
                price_per_year[k] = price[k][i];//获取某一年的价格数据
            }

            Struct_Degree Degree_per_year[] = new Struct_Degree[sequence_num];//存放某一年每个子序列的异常度
            for (int k = 0; k < sequence_num; k++)
            {
                Degree_per_year[k] = new Struct_Degree();
            }

            get_nearest_dist(price_per_year, time_granularity, sequence_num, Degree_per_year);//计算某年每个子序列的异常度
            sort_down(Degree_per_year, sequence_num);//按异常度从大到小排序

            for (int k = 0; k < sequence_num; k++)
            {
                Degree[k][i].setIndex(Degree_per_year[k].getIndex());
                Degree[k][i].setNearest_dist(Degree_per_year[k].getNearest_dist());
            }

            th[i]=get_Degree_Threshold(Degree_per_year,sequence_num,0.8);//计算每年的异常度阈值，如该年无异常子序列，则值为负无穷

            int flag=0;
            for(int k=0;k<sequence_num;k++)
            {
                if(th[i]!=Double.NEGATIVE_INFINITY)//如果该年的异常度阈值不为负无穷，则该年存在异常子序列
                {
                    if(Degree_per_year[k].getNearest_dist()>=th[i])//计算异常度>=异常度阈值的子序列的个数，即异常子序列的个数
                    {
                        flag=flag+1;
                    }
                }
            }
            abnormal_degree_num[i]=flag;//记录每年异常子序列的个数
        }

        System.out.println("在子序列长度为"+time_granularity+"时检测子序列异常：");
        for(int i=0;i<10;i++)
        {
            System.out.println("检测"+year[i]+"年各个子序列：");
            if(abnormal_degree_num[i]>0)//判断该年是否有异常子序列
            {
                for(int k=0;k<abnormal_degree_num[i];k++)
                {
                    System.out.println(year[i] + "第" + (Degree[k][i].getIndex() + 1) + "段异常，异常度为:" + Degree[k][i].getNearest_dist());
                }
            }
            else
            {
                System.out.println("无异常子序列");
            }
        }

        //**************************************************************************
        //**************************************************************************
        //寻找异常子序列中异常时段
        ArrayList<Struct_Abnormal> abnormal_points=new ArrayList<Struct_Abnormal>();
        for(int i=0;i<10;i++)
        {
            int abnormal_num=abnormal_degree_num[i];//获取每年异常子序列的数目
            if(abnormal_num>0)//判断该年是否出现异常子序列
            {
                double abnormal_sequence[]=new double[time_granularity*10];//存放十年内该时段的子序列
                for(int k=0;k<abnormal_num;k++)
                {
                    int index=Degree[k][i].getIndex();//获取某个异常子序列的序号
                    get_abnormal_sequence(price,abnormal_sequence,time_granularity,sequence_num,index);//获取十年内所有该时段的子序列，存放于abnormal_sequence数组内

                    ArrayList<DataNode> dpoints = new ArrayList<DataNode>();//将abnormal_sequence数组内出现的工作日的数据以DataNode结构存放于链表内
                    for(int t=0;t<abnormal_sequence.length;t++)
                    {
                        if(sequence_num*time_granularity!=260)
                        {
                            if(index<sequence_num-1)
                            {
                                int temp=t%time_granularity;
                                int temp_1=t/time_granularity;
                                double[] temp_point={index*time_granularity+temp,abnormal_sequence[t]};
                                dpoints.add(new DataNode(year[temp_1]+"年"+String.valueOf(index*time_granularity+temp+1)+"日",temp_point,index*time_granularity+temp+1));
                            }
                            else
                            {
                                int temp=t%time_granularity;
                                int temp_1=t/time_granularity;
                                double[] temp_point={260-time_granularity+temp,abnormal_sequence[t]};
                                dpoints.add(new DataNode(year[temp_1]+"年"+String.valueOf(260-time_granularity+temp+1)+"日",temp_point,260-time_granularity+temp+1));
                            }
                        }
                        else
                        {
                            int temp=t%time_granularity;
                            int temp_1=t/time_granularity;
                            double[] temp_point={index*time_granularity+temp,abnormal_sequence[t]};
                            dpoints.add(new DataNode(year[temp_1]+"年"+String.valueOf(index*time_granularity+temp+1)+"日",temp_point,index*time_granularity+temp+1));
                        }
                    }

                    LOF lof=new LOF();
                    List<DataNode> nodeList=lof.get_OutLineNode(dpoints);//计算每个点的离群因子
                    System.out.println("检测"+year[i]+"第"+(index+1)+"段异常子序列：");
                    Collections.sort(nodeList,new LOF.LofComparator());//从大到小排序

                    double avg_lof=0;
                    double std_lof=0;
                    for(DataNode node:nodeList)
                    {
                        String temp = node.getNodeName().substring(0, 4);
                        if (temp.equals(year[i]))
                        {
                            avg_lof = avg_lof + node.getLof();
                        }
                    }
                    avg_lof=avg_lof/time_granularity;
                    for(DataNode node:nodeList)
                    {
                        String temp = node.getNodeName().substring(0, 4);
                        if (temp.equals(year[i]))
                        {
                            std_lof = std_lof + Math.pow((node.getLof()-avg_lof),2);
                        }
                    }
                    std_lof=Math.sqrt(std_lof/time_granularity);
                    for(DataNode node:nodeList)
                    {
                        String temp=node.getNodeName().substring(0,4);
                        if(temp.equals(year[i]))
                        {
                            if((node.getLof()-avg_lof)/std_lof>2.5)//判断是否是异常点，阈值自己设置
                            {

                                abnormal_points.add(new Struct_Abnormal(year[i],node.getIndex()));//将其加入异常点集合
                                System.out.println(node.getNodeName() + "的离群因子:" + node.getLof());
                            }
                        }
                    }

                }
            }

        }
        //**************************************************************************
        //**************************************************************************
        //将检测到的异常点用均值替代
        data_replace(abnormal_points,pre_price);
        write_replaced_data(filename_replaced_data,pre_price,year,260,10);

    }

    public static void read_data_price(String fileName,double data[][],String year[])
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
                //year[k-1]=hssfRow.getCell(k).getStringCellValue();
            }
            for(int i=1;i<rowLength;i++)//去掉第一行表头
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=1;j<colLength;j++)//去掉第一列
                {
                    HSSFCell cell=row.getCell(j);//获得指定单元格
                    data[i-1][j-1]=cell.getNumericCellValue();
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

    public static void get_normalized_price(double previous_price[][],double normalized_price[][])
    {
        int row_length= previous_price.length;
        int column_length=previous_price[0].length;
        double mean[]=new double[column_length];
        double var[]=new double[column_length];
        get_data_meanprice(previous_price,mean);
        get_data_varprice(previous_price,var,mean);
       for(int j=0;j<column_length;j++)
       {
           for(int i=0;i<row_length;i++)
           {
               normalized_price[i][j]=(previous_price[i][j]-mean[j])/Math.sqrt(var[j]);
           }
       }
    }

    public static void get_data_varprice(double data[][],double var[],double mean[])
    {
        int row_length= data.length;
        int column_length=data[0].length;
        for(int i=0;i<column_length;i++)
        {
            double var_sum=0;
            for(int j=0;j<row_length;j++)
            {
                var_sum=var_sum+Math.pow((data[j][i]-mean[i]),2);
            }
            var[i]=var_sum/row_length;
        }
    }

    public static void get_data_meanprice(double data[][],double mean_data[])
    {//计算每年的平均价格
        int row_length= data.length;
        int column_length=data[0].length;
        for(int i=0;i<column_length;i++)
        {
            double column_sum=0;
            for(int j=0;j<row_length;j++)
            {
                column_sum=column_sum+data[j][i];
            }
            mean_data[i]=(double)(Math.round(column_sum*100/row_length)/100.0);//保留两位小数
            //System.out.println(meanprice[i]);
        }
    }

    private static double get_dist(double[][] data,int i, int j, int dt, int n)
    {	// 求欧式距离
        double sumdist=0;
        for(int t=0;t<n;t++) {
            double xti=get_xti(data,i,dt,n,t);//第i年第t个时间段的平均价格
            double xtj=get_xti(data,j,dt,n,t);//第j年第t个时间段的平均价格
            sumdist=sumdist+Math.pow(xti-xtj,2);
        }
        sumdist=Math.sqrt(sumdist);
        //System.out.println("在时间粒度 "+dt+" 的情况下，第"+i+"和第"+j+"年的欧式距离为：");
        //System.out.println(sumdist);
        return sumdist;
    }

    private static double get_r(double data[][],double mean_data[],int i, int j, int dt, int n)
    {	// 求相关系数
        double rij=0;
        double sum_shang = 0;//公式上部分的求和
        double sum_zuoxia=0;//公式左下部分的求和
        double sum_youxia=0;//公式右下部分的求和
        for(int t=0;t<n;t++) {
            double xti=get_xti(data,i,dt,n,t);
            double xtj=get_xti(data,j,dt,n,t);
            sum_shang=sum_shang + ((xti-mean_data[i])*(xtj-mean_data[j]));
            sum_zuoxia=sum_zuoxia+Math.pow(xti-mean_data[i], 2);
            sum_youxia=sum_youxia+Math.pow(xtj-mean_data[j], 2);
        }
        rij=sum_shang/(Math.sqrt(sum_zuoxia)*Math.sqrt(sum_youxia));
        //System.out.println("在时间粒度 "+dt+" 的情况下，第"+i+"和第"+j+"年的相关系数为：");
        //System.out.println(rij);
        return rij;
    }

    private static double get_xti(double data[][],int i, int dt, int n,int t)
    {
        double sum_xti=0;
        if(n*dt!=260)
        {
            if(t<n-1)
            {
                for(int k=t*dt;k<(t+1)*dt;k++)
                {
                    sum_xti=sum_xti+data[k][i];
                }
            }
            else
            {
                for(int k=260-dt;k<260;k++)
                {
                    sum_xti=sum_xti+data[k][i];
                }
            }
        }
        else
        {
            for(int k=t*dt;k<(t+1)*dt;k++)
            {
                sum_xti=sum_xti+data[k][i];
            }
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

    public static int recommend_time(double data[],int delta_t[],double Threshold)
    {
         int time_granularity=0;
        for(int i=0;i<data.length;i++)
        {
            if(data[i]>=Threshold)
            {
                time_granularity=delta_t[i];
                break;
            }
        }
        return time_granularity;
    }

    public static void get_nearest_dist(double data[],int SIZE,int num,Struct_Degree Degree[])
    {//最邻近非自我匹配距离，得到异常度。
        double x[]=new double[SIZE];
        double y[]=new double[SIZE];

        for(int i=0;i<num;i++)
        {
            double nearest_dist=Double.POSITIVE_INFINITY;
            get_sub_sequence(data,x,SIZE,num,i);
            for(int j=0;j<num;j++)
            {
                if(i!=j)
                {
                    get_sub_sequence(data,y,SIZE,num,j);
                    double dis=get_DTW_dis(x,y);
                    if(dis<nearest_dist)
                    {
                        nearest_dist=dis;
                        Degree[i].setIndex(i);
                        Degree[i].setNearest_dist(nearest_dist);
                    }
                }
            }
        }

    }

    public static void get_sub_sequence(double data[],double sub_data[],int SIZE,int len,int index)
    {//获取子序列
        if(SIZE*len!=data.length)
        {
            if(index<len-1)
            {
                for(int i=index*SIZE;i<(index+1)*SIZE;i++)
                {
                    sub_data[i-index*SIZE]=data[i];
                }
            }
            else
            {
                for(int i= data.length-SIZE;i< data.length;i++)
                {
                    sub_data[i-data.length+SIZE]=data[i];
                }
            }
        }
        else
        {
            for(int i=index*SIZE;i<(index+1)*SIZE;i++)
            {
                sub_data[i-index*SIZE]=data[i];
            }
        }
    }

    public static double get_DTW_dis(double Q[],double U[])
    {//求两个时间序列Q，U的动态时间弯曲距离
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
                    R[i][j]=D[i][j]+Math.min(Math.min(R[i][j-1],R[i-1][j-1]),R[i-1][j]);//查找最优路径
                }
            }
        }
        DTW_distance=R[Q.length-1][U.length-1];//最终两个时间序列的动态时间弯曲距离可由累计距离表示
        return DTW_distance;
    }

    public static void sort_down(Struct_Degree data[],int len)
    {//按异常度降序排列子序列
        for(int i=0;i<len-1;i++)
        {
            for(int j=0;j<len-i-1;j++)
            {
                if(data[j].getNearest_dist()<data[j+1].getNearest_dist())
                {
                    int index=data[j+1].getIndex();
                    double dist=data[j+1].getNearest_dist();
                    data[j+1].setIndex(data[j].getIndex());
                    data[j+1].setNearest_dist(data[j].getNearest_dist());
                    data[j].setIndex(index);
                    data[j].setNearest_dist(dist);
                }
            }
        }
    }

    public static void get_abnormal_sequence(double data[][],double abnormal_data[],int SIZE,int num,int index)
    {
        if(SIZE*num!=data.length)
        {
            if(index<num-1)
            {
                for(int i=0;i<10;i++)
                {
                    for(int j=index*SIZE;j<(index+1)*SIZE;j++)
                    {
                        int temp=j-index*SIZE;
                        abnormal_data[i*SIZE+temp]=data[j][i];
                    }
                }
            }
            else
            {
                for(int i=0;i<10;i++)
                {
                    for(int j=data.length-SIZE;j<data.length;j++)
                    {
                        int temp=j+SIZE-data.length;
                        abnormal_data[i*SIZE+temp]=data[j][i];
                    }
                }
            }
        }
        else
        {
            for(int i=0;i<10;i++)
            {
                for(int j=index*SIZE;j<(index+1)*SIZE;j++)
                {
                    int temp=j-index*SIZE;
                    abnormal_data[i*SIZE+temp]=data[j][i];
                }
            }
        }
    }

    public static double get_Degree_Threshold(Struct_Degree data[],int num,double e)
    { //th:最小的异常度阈值  e:降低幅度的阈值
        double temp[]=new double[num];
        double t=Double.NEGATIVE_INFINITY;
        for(int i=0;i<num;i++)
        {
            if(i<num-1)
            {
                temp[i]=(data[i].getNearest_dist()-data[i+1].getNearest_dist())/(data[i].getNearest_dist());
            }
            else
            {
                temp[i]=(data[i].getNearest_dist()-data[0].getNearest_dist())/(data[i].getNearest_dist());
            }
            if(temp[i]>=e)
            {
                t=data[i].getNearest_dist();
                break;
            }
        }
        return t;
    }


    // 效果并不好
    public static void data_replace(ArrayList<Struct_Abnormal> abnormal_points,double data[][])
    {
        double mean[]=new double[10];
        get_data_meanprice(data,mean);
        for(Struct_Abnormal node:abnormal_points)
        {
            int index=node.getIndex();
            int year=Integer.valueOf(node.getYear())-2008;
            data[index-1][year]=mean[year];
        }
    }

    public static void write_replaced_data(String filename,double data[][],String year[],int row_num,int line_num)
    {
        try
        {
            HSSFWorkbook workbook=new HSSFWorkbook();//产生工作簿对象
            HSSFSheet sheet = workbook.createSheet();//产生工作表对象
            workbook.setSheetName(0,"sheet1");//设置第一个工作表的名称为sheet1

            HSSFRow row0= sheet.createRow((short)0);//生成第一行（表头）
            HSSFCell cell0,cell;
            cell0=row0.createCell((short)0);//生成该行第一个单元格
            cell0.setCellValue("time");
            for(int i=0;i< year.length;i++)
            {
                cell0=row0.createCell((short)i+1);//生成该行第一个单元格
                cell0.setCellValue(Integer.valueOf(year[i]));
            }

            for(int i=1;i<row_num+1;i++)
            {
                HSSFRow row= sheet.createRow((short)i);//生成第i行
                cell0=row.createCell((short)0);
                cell0.setCellValue(i);
                for(int j=1;j<line_num+1;j++)
                {
                    cell=row.createCell((short)j);
                    cell.setCellValue(data[i-1][j-1]);
                }
            }
            FileOutputStream fOut = new FileOutputStream(filename);//在项目的当前目录下输出文件
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
