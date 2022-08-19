package cn.objectspace.webssh.controller;

import cn.objectspace.webssh.constant.ConstantPool;
import cn.objectspace.webssh.pojo.HostData;
import cn.objectspace.webssh.service.WebSSHService;
import cn.objectspace.webssh.util.IpUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class RouterController {
    @Resource
    WebSSHService webSSHService;

    @GetMapping("/")
    public String index() {
        return "fronted/index";
    }

    /*
     * 获取本机的ip，让前端通过这个ip来创建websocket
     * @Author: fuchengjie
     * @date 2022-8-15
     * @return ip地址
     */
    @PostMapping("getIp")
    @ResponseBody
    public Object getIp() throws SocketException {
        Map<String, String> map= new HashMap<>();
        map.put("ip", IpUtil.getLocalIp4Address().get().getHostAddress());
        return map;
    }

    /*
     * 得到前端返回连接的数据,并将数据传送到另一个页面
     * @param data 得到前端表格传入的参数
     * @Author: fuchengjie
     * @date 2022-8-15
     * @return 返回模拟终端的页面
     */
    @PostMapping("/connect")
    public String connect(HostData data, Model model) throws Exception {
        // 添加参数，这样thymeleaf可以解析到对象的值,注意添加的值要是Map形式的，否则前端无法解析就会出错
        model.addAttribute("data", data);
        ConstantPool.SSH_DATA = data;
        return "fronted/terminal";
    }

    /*
     * 在正式连接前先测试是否可以连接
     * @param data 得到前端表格传入的参数
     * @Author: fuchengjie
     * @date 2022-8-15
     * @return 是否连接成功
     */
    @PostMapping("/testConnect")
    @ResponseBody
    public Object testConnect(HostData data) {
        return webSSHService.testConnect(data);
    }
}
