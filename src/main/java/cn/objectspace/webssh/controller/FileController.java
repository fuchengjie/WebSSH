package cn.objectspace.webssh.controller;

import cn.objectspace.webssh.pojo.HostData;
import cn.objectspace.webssh.util.FileUtil;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
public class FileController {

    @GetMapping("upload")
    public String uploadHtml(@RequestParam String host,
                             @RequestParam Integer port,
                             @RequestParam String username,
                             @RequestParam String password, Model model){
        model.addAttribute("host", host);
        model.addAttribute("port", port);
        model.addAttribute("username", username);
        model.addAttribute("password", password);
        return "fronted/upload";
    }
    @PostMapping("upload")
    @ResponseBody
    public Object upload(@RequestParam("multipartFile") MultipartFile multipartFile,
                         @RequestParam String host,
                         @RequestParam Integer port,
                         @RequestParam String username,
                         @RequestParam String password) throws IOException, JSchException, SftpException {
        HostData data = new HostData();
        data.setHost(host);
        data.setPort(port);
        data.setUsername(username);
        data.setPassword(password);
        FileUtil.upload(data, multipartFile);
        return "";
    }

}
