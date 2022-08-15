package cn.objectspace.webssh.controller;

import cn.objectspace.webssh.constant.ConstantPool;
import cn.objectspace.webssh.pojo.WebSSHData;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RouterController {
    @GetMapping("/")
    public String index(){
        return "fronted/index";
    }

    /*
     * 得到前端返回连接的数据,并将数据传送到另一个页面
     *
     * @param data 得到前端表格传入的参数
     *
     * @param model 将前端参数注入到请求中
     *
     * @return 返回模拟终端的页面
     */
    @RequestMapping("/connect")
    public String connect(WebSSHData data, Model model) throws Exception {
        // 添加参数，这样thymeleaf可以解析到对象的值,注意添加的值要是Map形式的，否则前端无法解析就会出错
        model.addAttribute("data", data);
        ConstantPool.SSH_DATA = data;
        return "fronted/terminal";
    }
}
