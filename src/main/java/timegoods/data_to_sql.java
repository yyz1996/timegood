package timegoods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class data_to_sql {

    static double Aprice[]=new double[2695];
    static Date date[]=new Date[2695];;
    static String name;//商品名称
    static String goodid;//商品编号

    public static void main(String[] args)
    {
        String filename_data = "D:\\实验室项目\\实验数据\\农副产品价格.xls";
        read_data_price(filename_data);//读取价格数据到相应的数组
        //白糖 10005S2020
//        菜籽粕 10006S2020
        //玉米 10003Y2020
//        菜籽油 10007S2020
//        棉花一号 10008S2020
//        油菜籽 10009S2020
//        早籼稻 10010S2020
//        活猪 10013S2020
//        鸡蛋 10014S2020
//        可可 10015S2020
//        小麦 10002X2020
//        强筋小麦 10002Q2020
//        焦炭 20001A2018
//            动力煤20002W2010
//        铁矿石 20003A2018
//            焦煤20004M2020
//        动力煤20005D2020
//                天然气20006T2020
//        甲醇 30002A2019
//            尿素硝酸铵30004S2018
//        氯化钾30005K2020
//        天然橡胶（TSR20）30006TSR20
//            聚乙烯30007X2020
//        原油（中）30008Y2020
//        螺纹钢 40001S2019
//        黄金99.99 40004J2020
//        白银99.99 40004Y2018
//            铝40006L2020
//        锌40006X2019
//        铜 40006T2019
//            镍40006N2019
//        铅40006Q2019
//                锡40006Z2019

        for(int i=0;i<Aprice.length;i++) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String time1 = sdf.format(new Date(String.valueOf(date[i])));
           // System.out.println(time1);
            System.out.println("("+Aprice[i]+",'"+goodid+"','"+name+"',"+"'"+time1+"'"+"),");
        }

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
            System.out.println("总行数: "+rowLength);
            System.out.println("总列数: "+colLength);

            for(int i=0;i<18;i++)//第一行编号，第二行名称，第三行数据
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=0;j<colLength;j++)//去掉第一列 日期
                {
                    if(j==0){//第一列
                        if(i>1){
                            HSSFCell cell=row.getCell(j);//获得指定单元格
                            date[i-2]=cell.getDateCellValue();//获得日期
                            //System.out.println(date[i-1]);
                        }
                    }
                    if(j==14){//第 列
                        if(i==0){//第一行编号
                            HSSFCell cell=row.getCell(j);//获得指定单元格
                            goodid=cell.getStringCellValue();
                        }
                        if(i==1){//第二行名称
                            HSSFCell cell=row.getCell(j);//获得指定单元格
                            name=cell.getStringCellValue();
                        }
                        if(i>1){
                            HSSFCell cell=row.getCell(j);//获得指定单元格
                            Aprice[i-2]=cell.getNumericCellValue();
                            //System.out.println(date[i-1]);
                        }


                    }
                }
            }


            //System.out.println(price[259][0]);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }





}
