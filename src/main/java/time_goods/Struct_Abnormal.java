package time_goods;

public class Struct_Abnormal {

    private String year;
    private int index;

    public Struct_Abnormal(String year,int index)
    {
        this.year=year;
        this.index=index;
    }

    public String getYear()
    {
        return year;
    }
    public int getIndex()
    {
        return index;
    }
    public void setYear(String year)
    {
        this.year=year;
    }
    public void setIndex(int index)
    {
        this.index=index;
    }
}
