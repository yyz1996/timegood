package timegoods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class timedata_to_sql {

    static double Aprice[]=new double[2695];
    static Date date[]=new Date[2695];;
    static String name;//商品名称
    static String goodid;//商品编号

    public static void main(String[] args)
    {
        String filename_data = "D:\\实验室项目\\实验数据\\金属矿产价格.xls";
        read_data_price(filename_data);//读取价格数据到相应的数组


    }

    public static void read_data_price(String fileName)
    { //读入数据到price数组
        try{
            InputStream input=new FileInputStream(fileName);//建立输入流
            HSSFWorkbook wb=new HSSFWorkbook(input);//初始化
            HSSFSheet sheet=wb.getSheetAt(2);//获取第3个表单

            int rowLength=sheet.getPhysicalNumberOfRows();//总行数
            HSSFRow hssfRow=sheet.getRow(0);//获取第一行（表头）
            int colLength=hssfRow.getPhysicalNumberOfCells();//总列数
            System.out.println("总行数: "+rowLength);
            System.out.println("总列数: "+colLength);

            for(int i=0;i<rowLength;i++)//第一行编号，第二行名称，第三行数据
            {
                String id = null;
                String name = null;
                int timeadvise = 0;
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=0;j<colLength;j++)//去掉第一列 日期
                {
                    if(j==0){//第一列
                        HSSFCell cell=row.getCell(j);//获得指定单元格
                        id=cell.getStringCellValue();
                    }
                    if(j==1){//第一列
                        HSSFCell cell=row.getCell(j);//获得指定单元格
                        name=cell.getStringCellValue();
                    }
                    if(j==2){//第一列
                        HSSFCell cell=row.getCell(j);//获得指定单元格
                        timeadvise= (int) cell.getNumericCellValue();
                    }
                }

                System.out.println("('"+id+"','"+name+"',"+timeadvise+"),");
            }


            //System.out.println(price[259][0]);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }





}
