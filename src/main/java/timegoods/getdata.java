package timegoods;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class getdata {

    static double Aprice[]=new double[250];
    static double Bprice[]=new double[250];;//AB商品的价格时间序列
    static double Cprice[]=new double[250];;//AB商品的价格时间序列
    static double Dprice[]=new double[250];;//AB商品的价格时间序列
    static double meana=0;
    static double meanb=0;
    public static void main(String[] args)
    {
        String filename_data = "D:\\实验室项目\\实验数据\\金属矿产价格.xls";

        read_data_price(filename_data);

        //求均值
        for(int i=0;i<Aprice.length;i++) {
           /* SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String time1 = sdf.format(new Date(String.valueOf(date[i])));
            System.out.println(time1);
            System.out.println("("+tempprice[i]+",'10012P2018','豆粕',"+"'"+time1+"'"+"),");*/
            meana=meana+Aprice[i];
            meanb=meanb+Bprice[i];
        }
        meana=meana/Aprice.length;
        meanb=meanb/Bprice.length;
       // System.out.println(meana+" "+meanb);
        get_r();// 求相关系数

    }

    private static void get_r() {
        // 求相关系数
        double r=0;
        double sum_shang = 0;//
        double sum_zuoxia=0;//
        double sum_youxia=0;//
        for(int i=0;i<Aprice.length;i++) {
            sum_shang=sum_shang + ((Aprice[i]-meana)*(Bprice[i]-meanb));
            sum_zuoxia=sum_zuoxia+Math.pow(Aprice[i]-meana, 2);
            sum_youxia=sum_youxia+Math.pow(Bprice[i]-meanb, 2);
        }
        r=sum_shang/(Math.sqrt(sum_zuoxia)*Math.sqrt(sum_youxia));
        r = (double) Math.round(r * 10000) / 10000;
        System.out.println("豆粕和大豆的 相关系数： "+r);

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

            for(int i=1;i<250;i++)//去掉第一行表头
            {
                HSSFRow row=sheet.getRow(i);//获取表的每一行
                for(int j=0;j<colLength;j++)//去掉第一列
                {
                    if(j==3){
                        HSSFCell cell=row.getCell(j);//获得指定单元格
                        Aprice[i-1]=cell.getNumericCellValue();
                    }
                    if(j==4){
                        HSSFCell cell=row.getCell(j);//获得指定单元格
                        Bprice[i-1]=cell.getNumericCellValue();
                    }
                    if(j==3){
                        HSSFCell cell=row.getCell(j);//获得指定单元格
                        Cprice[i-1]=cell.getNumericCellValue();
                    }
                    if(j==4){
                        HSSFCell cell=row.getCell(j);//获得指定单元格
                        Dprice[i-1]=cell.getNumericCellValue();
                    }
                }

            }

         //   System.out.println("测试Bprice[0]  "+Bprice[0]);
            //System.out.println(price[259][0]);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private static double standard_r() {
        //标准化相关系数
        return 0;
    }




}
