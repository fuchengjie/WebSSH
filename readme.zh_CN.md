# 由于原作者超过两年没有更新此项目，也没有解决issue和pr，所以我就自己fork了一个，并且对前端界面进行了优化

# README.md
- en [English](readme.md)


目前添加的功能有： 
- 前端登录界面 
- 终端显示优化
- 文件传输功能，目前默认会传输到/tmp目录下，后面考虑传到家目录或者自定义目录
- 测试连接功能
- 公钥登陆，因为jsch版本问题，你的id_rsa是"-----BEGIN OPENSSH PRIVATE KEY-----"开头，那么使用命令 `ssh-keygen -p -f <privateKeyFile> -m pem` 转换一下格式

特色（feature）
- 跨平台（cross platform）
- 浏览器环境（browser-in）
- 支持上传文件（transform file）

## 图片展示
前端登陆界面
![image](https://user-images.githubusercontent.com/31361595/184635512-bdf7883b-52a1-4515-b380-6b9ba18bfa11.png)

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
