import com.cmsagent.schema.PageSchema;
import java.util.*;
/** Dependency-free checks; not a substitute for Maven integration tests. */
public class SchemaCheck {
    static int checks;
    static void ok(boolean value) { checks++; if(!value) throw new AssertionError("check "+checks); }
    static void fails(Runnable code) { checks++; try{code.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("Expected rejection "+checks); }
    public static void main(String[] args) {
        var title=new PageSchema.Field("title","标题","text",true,true,null);
        var price=new PageSchema.Field("price","价格","number",true,true,null);
        var flag=new PageSchema.Field("enabled","启用","boolean",true,true,null);
        var status=new PageSchema.Field("status","状态","select",true,true,List.of("draft","published"));
        var s=new PageSchema(1,"product","商品",List.of(title,price,flag,status));
        ok(s.validateData(Map.of("title","x","price",0,"enabled",false,"status","draft")).get("enabled").equals(false));
        fails(()->new PageSchema(1,"../bad","x",List.of(title)));
        fails(()->new PageSchema(1,"test","x",List.of(title,title)));
        fails(()->new PageSchema.Field("constructor","x","text",true,true,null));
        fails(()->new PageSchema.Field("test","x","select",true,true,List.of("x","x")));
        fails(()->new PageSchema.Field("test","x","text",null,true,null));
        fails(()->s.validateData(Map.of("title"," ","price",1,"enabled",false,"status","draft")));
        fails(()->s.validateData(Map.of("title","x","price",Double.NaN,"enabled",false,"status","draft")));
        fails(()->s.validateData(Map.of("title","x","price",1e13,"enabled",false,"status","draft")));
        fails(()->s.validateData(Map.of("title","x","price",0,"enabled",false,"status","unknown")));
        fails(()->s.validateData(Map.of("title","x","price",0,"enabled",false,"status","draft","extra",true)));
        fails(()->new PageSchema(2,"test","x",List.of(title)));
        System.out.println("SchemaCheck: "+checks+" checks passed");
    }
}
