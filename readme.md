

# README.md
- en [English](readme_en.md)


目前添加的功能有： 
- 前端登录界面 
- 终端显示优化
- 文件管理功能：支持查看远程目录、上传、下载、删除文件、创建目录，默认目录为 `/tmp/`，也可以手动切换到其他目录
- 测试连接功能
- 公钥登陆，因为jsch版本问题，你的id_rsa是"-----BEGIN OPENSSH PRIVATE KEY-----"开头，那么使用命令 `ssh-keygen -p -f <privateKeyFile> -m pem` 转换一下格式

特色（feature）
- 跨平台（cross platform）
- 浏览器环境（browser-in）
- 支持远程文件管理（file manager）

## AI 协助说明

早期这个项目里，文件上传只把文件丢到服务器默认目录，界面上也看不到远程文件状态；终端自适应和文件列表这些 issue，当时作者自己技术储备有限，尤其对 xterm.js、自适应布局和 SFTP 管理理解不够深，所以一直没有做得很完整。

这次修改由 AI 辅助完成：AI 直接重构了前后端文件管理逻辑，补上了远程目录浏览、上传位置提示、下载、删除、新建目录、终端自适应等能力。现在 AI 编程能力已经很强了，这类以前卡住很久的功能，可以直接让 AI 读代码、改代码、跑测试并验证效果。

## 图片展示
前端登录界面（AI 辅助重构）
![前端登录界面](docs/readme-login-ai.png)

终端和远程文件管理（AI 辅助重构）
![终端和远程文件管理](docs/readme-file-manager.png)

终端显示优化
![image](https://user-images.githubusercontent.com/31361595/184619160-1df7604d-9a88-435d-8ac2-592161d9eadf.png)

当无法连接到主机时，登录界面显示错误信息
![image](https://user-images.githubusercontent.com/31361595/184631740-4f45d221-4fa7-4076-86b1-2d5cc4ef6dff.png)


技术架构图
![image](https://user-images.githubusercontent.com/31361595/184622254-99fe8b44-c4d1-45f0-a1c9-4c0d742490f5.png)

解决的问题：
1.浏览器和SpringBoot的WebSocket连接会通过接口获取服务器的地址，而不是直接写死为127.0.0.1，这样至少在同一个局域网中可以有多个浏览器来访问服务器；在公网上效果如何，我并没有进行测试，有条件的可以试试。

欢迎大佬们提issue和pr（不一定完成），欢迎大家参与项目
<br><br><br><br><br><br><br><br><br><br>
