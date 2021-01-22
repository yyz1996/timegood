package time_goods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class create_data {

    static double data[][]=new double[260][10];
    //static double[] base={1.0,1.03,1.02,1.01,0.9,0.98,1.08,1.02,0.96,0.93,1.02,1.0};
    //static int[] day={22,43,66,87,109,131,151,171,192,214,237,260};
    static int[] year={2008,2009,2010,2011,2012,2013,2014,2015,2016,2017};

    public static void main(String[] args)
    {
        double temp[]=new double[260];
        /*int t=0;
        int k=0;
        while (k<12)
        {
            for(int i=t;i<day[k];i++)
            {
                temp[i]=base[k]*500+k*Math.pow((-1),k);
            }
            t=day[k];
            k=k+1;
        }
        for(int j=0;j<10;j++)
        {
            for(int i=0;i<260;i++)
            {
                Random random = new Random();
                int random_num=random.nextInt(100) + 1;
                data[i][j]=temp[i]+random_num*Math.pow((-1),j);
            }
        }
        String filename="simulation_data_test.xls";
        write_data(data,"time",year,filename);*/

        double[] price={100,110,124,135,160,170,172,180,187,190,200,211,216,228,218,210,201,197,180,165,159,148,138,124,120,103};
        int k=1;
        while (k<10)
        {
            int t=0;
           for(int i=26*(k-1);i<26*k;i++)//以26个价格数据为一个周期
           {
               temp[i]=price[t];//生成一年的260个价格数据
               t=t+1;
           }
           k=k+1;
        }
        for(int j=0;j<10;j++)
        {
            for(int i=0;i<260;i++)
            {
                Random random = new Random();
                int random_num=random.nextInt(200) -100;//从-100到100中随机选取一个数
                data[i][j]=temp[i]+random_num;//生成10年的价格数据，每年的走势类似
            }
        }
        String filename="simulation_data_test.xls";
        write_data(data,"time",year,filename);

    }

    public static void write_data(double data[][],String columnName_0,int columnName[],String fileName)
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
            for(int i=1;i<11;i++)
            {
                cell0=row0.createCell((short)i);
                cell0.setCellValue(columnName[i-1]);
            }

            int row_num=260;
            int day=1;
            for(int i=1;i<=row_num;i++)
            {
                HSSFRow row= sheet.createRow((short)i);//生成第i行
                cell0=row.createCell((short)0);//每行第一个单元格为时间粒度
                cell0.setCellValue(day);
                day=day+1;
                for(int j=1;j<11;j++)
                {
                    cell = row.createCell((short)j);
                    cell.setCellValue(data[i-1][j-1]);
                }
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
