package time_goods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class nearest_dist {

    static String product="abnormal_data_10";

    static double price[]=new double[260];
    static  int SIZE=26;

    public static void main(String[] args)
    {
        //读取数据到price[]数组
        String path="D:\\数据\\abnormal\\"+product+".xls";
        read_data(path,price);
        get_normalized(price);

        int num= price.length/SIZE;
        if(num*SIZE!= price.length)
        {
            num=num+1;
        }

        Struct_Degree Degree[] =new Struct_Degree[num];
        for(int i=0;i<num;i++)
        {
            Degree[i]=new Struct_Degree();
        }

        get_nearest_dist(price,SIZE,num,Degree);

        sort_down(Degree,num);

        double Threshold=get_Threshold(Degree,num,Degree[num-1].getNearest_dist(),0.8);
        System.out.println("Threshold:"+Threshold);

        for(int i=0;i<num;i++)
        {
            System.out.println((Degree[i].getIndex()+1)+":"+Degree[i].getNearest_dist());

        }

        /*double abnormal_num=2;
        double abnormal_sequence[]=new double[SIZE];
        for(int i=0;i<abnormal_num;i++)
        {
            int index=Degree[i].getIndex();
            get_sub_sequence(price,abnormal_sequence,SIZE,num,index);

            ArrayList<DataNode> dpoints = new ArrayList<DataNode>();
            for(int k=0;k<abnormal_sequence.length;k++)
            {
                double[] temp={index*SIZE+k+1,abnormal_sequence[k]};
                dpoints.add(new DataNode(String.valueOf(index*SIZE+k+1),temp));
            }

            LOF lof=new LOF();
            List<DataNode> nodeList=lof.get_OutLineNode(dpoints);

            Collections.sort(nodeList,new LOF.LofComparator());
            for(DataNode node:nodeList)
            {
                System.out.println(node.getNodeName() + ":" + node.getLof());
            }
            System.out.println("*******************");
        }*/

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

    public static void get_normalized(double data[])
    {
        double mean=get_mean(data);
        double var=get_var(data,mean);
        for(int i=0;i< data.length;i++)
        {
            data[i]=(data[i]-mean)/Math.sqrt(var);
        }
    }

    public static double get_mean(double data[])
    {
        double sum=0;
        for(int i=0;i< data.length;i++)
        {
            sum=sum+data[i];
        }
        sum=sum/ data.length;
        return sum;
    }

    public static double get_var(double data[],double mean)
    {
        double sum=0;
        for(int i=0;i< data.length;i++)
        {
            sum=sum+Math.pow(data[i]-mean,2);
        }
        return sum;
    }

    public static void get_nearest_dist(double data[],int SIZE,int num,Struct_Degree Degree[])
    {
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

    public static void get_sub_sequence(double price[],double data[],int SIZE,int len,int index)
    {
        if(SIZE*len!=price.length)
        {
            if(index<len-1)
            {
                for(int i=index*SIZE;i<(index+1)*SIZE;i++)
                {
                    data[i-index*SIZE]=price[i];
                }
            }
            else
            {
                for(int i= price.length-SIZE;i< price.length;i++)
                {
                    data[i-price.length+SIZE]=price[i];
                }
            }
        }
        else
        {
            for(int i=index*SIZE;i<(index+1)*SIZE;i++)
            {
                data[i-index*SIZE]=price[i];
            }
        }
    }

    public static double get_DTW_dis(double Q[],double U[])
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
                    R[i][j]=D[i][j]+Math.min(Math.min(R[i][j-1],R[i-1][j-1]),R[i-1][j]);//查找最优路径
                }
            }
        }
        DTW_distance=R[Q.length-1][U.length-1];//最终两个时间序列的动态时间弯曲距离可由累计距离表示
        return DTW_distance;
    }

    public static void sort_down(Struct_Degree data[],int len)
    {
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

    public static double get_Threshold(Struct_Degree data[],int num,double th,double e)
    {
        double temp[]=new double[num];
        double t=-1;
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
            if(data[i].getNearest_dist()<=th || temp[i]>=e)
            {
                t=data[i].getNearest_dist();
                break;
            }
        }
        return t;
    }

}
