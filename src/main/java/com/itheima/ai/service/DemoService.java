package com.itheima.ai.service;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DemoService {
    public static final String CATALOG="演示课程（不是黑马官方招生信息）：Java就业基础班，16周，演示价格12800元；Python与AI应用班，12周，演示价格9800元；前端开发班，12周，演示价格8800元。演示校区：北京、上海、线上。试听为本地演示预约，不会通知真实学校，也不收取费用。";
    public String respond(SessionStore.Session s,String prompt) {
        var b=s.booking;
        if(prompt.strip().equals("取消预约")) {
            b.clear(); return "已取消本地演示预约。没有向任何学校发送信息。";
        }
        if(b.containsKey("id") && (prompt.contains("预约")||prompt.contains("编号"))) return receipt(b);
        if(prompt.contains("预约") && !b.containsKey("step")) {
            b.put("step","course");
            return "这是本地演示预约，不代表真实报名。请回复要试听的课程：Java、Python 或前端。随时回复‘取消预约’退出。";
        }
        String step=b.get("step");
        if(step==null) return null;
        switch(step) {
            case "course" -> {
                String lower=prompt.toLowerCase(Locale.ROOT);
                String course=lower.contains("java")?"Java就业基础班":lower.contains("python")?"Python与AI应用班":prompt.contains("前端")?"前端开发班":null;
                if(course==null) return "请选择演示课程：Java、Python 或前端。";
                b.put("course",course);b.put("step","campus");return "请选择演示校区：北京、上海或线上。";
            }
            case "campus" -> {
                String campus=List.of("北京","上海","线上").stream().filter(prompt::contains).findFirst().orElse(null);
                if(campus==null) return "请选择北京、上海或线上。";
                b.put("campus",campus);b.put("step","name");return "请填写称呼（2至30字，可用化名）。";
            }
            case "name" -> {
                String name=prompt.strip();
                if(!name.matches("[\\p{L} ·]{2,30}")) return "请填写2至30字的称呼，可使用化名。";
                b.put("name",name);b.put("step","contact");return "请填写联系电话（11位手机号）。这是本地演示，可使用示例号码13800000000。";
            }
            case "contact" -> {
                if(!prompt.strip().matches("1[3-9]\\d{9}")) return "请输入有效格式的11位手机号，或使用演示号码13800000000。";
                b.put("contact",prompt.strip());b.put("step","confirm");
                return "请确认本地演示预约："+b.get("course")+" / "+b.get("campus")+" / "+b.get("name")+" / "+masked(b.get("contact"))+"。回复‘确认预约’提交，或‘取消预约’退出。不会联系真实学校。";
            }
            case "confirm" -> {
                if(!prompt.strip().equals("确认预约")) return "请回复‘确认预约’或‘取消预约’。";
                b.put("id","DEMO-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT));b.remove("step");return receipt(b);
            }
            default -> throw new IllegalStateException("Unknown booking step");
        }
    }
    private String masked(String phone) {return phone.substring(0,3)+"****"+phone.substring(7);}
    private String receipt(Map<String,String> b) {
        return "【本地演示预约已保存\n\n预约编号："+b.get("id")+"\n\n课程："+b.get("course")+"\n\n校区："+b.get("campus")+"\n\n称呼："+b.get("name")+"\n\n电话："+masked(b.get("contact"))+"\n\n仅用于功能演示，未向真实学校提交。】";
    }
    public String game(SessionStore.Session s,String prompt) {
        if(!s.gameStarted) {
            s.gameStarted=true;
            String reason=prompt.replaceFirst("^开始游戏[，,]?女友生气原因[:：]?", "").strip();
            String opening=reason.equals("开始游戏")||reason.isBlank()?"哼，我有点不开心。":"想到‘"+reason+"’，我就有点难过。";
            return opening+"你愿意先听听我的感受吗？\n\n原谅值：20\n\n你有10轮机会：倾听、道歉和具体行动更容易获得理解。";
        }
        if(s.gameOver) return "游戏结束，请点击重新开始。\n\n原谅值："+s.score;
        s.rounds++;
        boolean hostile=List.of("闭嘴","有病","滚","无理取闹","矫情","都是你的错").stream().anyMatch(prompt::contains);
        boolean empathy=List.of("对不起","抱歉","理解","感受","听你","我的错","不该").stream().anyMatch(prompt::contains);
        boolean action=List.of("以后","下次","一起","陪你","改正","现在","保证").stream().anyMatch(prompt::contains);
        int delta=hostile?-20:(empathy?15:0)+(action?10:0);
        if(delta==0) delta=-5;
        s.score=Math.max(0,Math.min(100,s.score+delta));
        s.gameOver=s.score==100||s.score==0||s.rounds>=10;
        String reply=hostile?"这样的说法让我更难过了，希望我们能尊重彼此。":empathy?"谢谢你愿意理解我的感受。我想看到你说的改变。":action?"有具体行动会让我更安心，也希望你理解我为什么难过。":"我更希望你认真听听我的感受，而不是敷衍过去。";
        return reply+"\n\n原谅值："+s.score+"\n\n轮次："+s.rounds+"/10"+(s.gameOver?"\n\n游戏结束："+(s.score==100?"你们和好啦！":"可以重新开始，再试一次温和沟通。") : "");
    }
}
