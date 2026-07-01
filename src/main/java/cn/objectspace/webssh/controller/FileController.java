package cn.objectspace.webssh.controller;

import cn.objectspace.webssh.pojo.HostData;
import cn.objectspace.webssh.util.FileUtil;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class FileController {

    /**
     * 保留独立文件管理页入口，便于从终端页之外单独打开文件管理功能。
     */
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
     * 上传文件到当前远程目录，并把最终目录和文件名返回给前端用于明确提示。
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

        Map<String, Object> result = new HashMap<>();
        result.put("res", true);
        result.put("msg", "上传成功");
        result.put("remotePath", FileUtil.normalizePath(remotePath));
        result.put("filename", multipartFile.getOriginalFilename());
        return result;
    }

    /**
     * 读取指定远程目录下的文件状态，前端据此展示文件名、大小、类型和修改时间。
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
     * 在当前远程目录下创建子目录。
     */
    @PostMapping("file/mkdir")
    @ResponseBody
    public Object mkdir(@RequestParam String host,
                        @RequestParam Integer port,
                        @RequestParam String username,
                        @RequestParam String password,
                        @RequestParam(required = false, defaultValue = "") String remotePath,
                        @RequestParam String dirname) throws JSchException, SftpException {
        HostData data = buildHostData(host, port, username, password);
        FileUtil.mkdir(data, remotePath, dirname);

        Map<String, Object> result = new HashMap<>();
        result.put("res", true);
        result.put("msg", "目录创建成功");
        return result;
    }

    /**
     * 下载当前远程目录中的单个文件。
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
     * 删除当前远程目录中的文件或空目录。
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

        Map<String, Object> result = new HashMap<>();
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

    /**
     * 文件管理接口直接把可读错误返回给前端，避免页面只显示 500 而没有原因。
     */
    @ExceptionHandler({IllegalArgumentException.class, JSchException.class, SftpException.class, IOException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleFileError(Exception e) {
        return e.getMessage();
    }
}
