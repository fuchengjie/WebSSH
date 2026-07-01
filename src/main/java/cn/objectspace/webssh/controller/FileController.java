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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class FileController {

    @GetMapping("upload")
    public String uploadHtml(@RequestParam String host,
                             @RequestParam Integer port,
                             @RequestParam String username,
                             @RequestParam String password,
                             @RequestParam(required = false, defaultValue = "") String remotePath,
                             Model model) {
        model.addAttribute("host", host);
        model.addAttribute("port", port);
        model.addAttribute("username", username);
        model.addAttribute("password", password);
        model.addAttribute("remotePath", FileUtil.normalizePath(remotePath));
        return "fronted/upload";
    }

    /**
     * 上传文件到指定远程目录
     */
    @PostMapping("upload")
    @ResponseBody
    public Object upload(@RequestParam("multipartFile") MultipartFile multipartFile,
                         @RequestParam String host,
                         @RequestParam Integer port,
                         @RequestParam String username,
                         @RequestParam String password,
                         @RequestParam(required = false, defaultValue = "") String remotePath) throws IOException, JSchException, SftpException {
        HostData data = buildHostData(host, port, username, password);
        FileUtil.upload(data, multipartFile, remotePath);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("res", true);
        result.put("msg", "上传成功");
        return result;
    }

    /**
     * 列出远程目录下的文件
     */
    @GetMapping("file/list")
    @ResponseBody
    public Object list(@RequestParam String host,
                       @RequestParam Integer port,
                       @RequestParam String username,
                       @RequestParam String password,
                       @RequestParam(required = false, defaultValue = "") String remotePath) throws JSchException, SftpException {
        HostData data = buildHostData(host, port, username, password);
        return FileUtil.list(data, remotePath);
    }

    /**
     * 下载远程文件
     */
    @GetMapping("file/download")
    public void download(@RequestParam String host,
                         @RequestParam Integer port,
                         @RequestParam String username,
                         @RequestParam String password,
                         @RequestParam(required = false, defaultValue = "") String remotePath,
                         @RequestParam String filename,
                         HttpServletResponse response) throws IOException, JSchException, SftpException {
        HostData data = buildHostData(host, port, username, password);
        FileUtil.download(data, remotePath, filename, response);
    }

    /**
     * 删除远程文件或空目录
     */
    @PostMapping("file/delete")
    @ResponseBody
    public Object delete(@RequestParam String host,
                         @RequestParam Integer port,
                         @RequestParam String username,
                         @RequestParam String password,
                         @RequestParam(required = false, defaultValue = "") String remotePath,
                         @RequestParam String filename) throws JSchException, SftpException {
        HostData data = buildHostData(host, port, username, password);
        FileUtil.delete(data, remotePath, filename);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("res", true);
        result.put("msg", "删除成功");
        return result;
    }

    private HostData buildHostData(String host, Integer port, String username, String password) {
        HostData data = new HostData();
        data.setHost(host);
        data.setPort(port);
        data.setUsername(username);
        data.setPassword(password);
        return data;
    }

}
