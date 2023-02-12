# 由于原作者超过两年没有更新此项目，也没有解决issue和pr，所以我就自己fork了一个，并且对前端界面进行了优化
目前添加的功能有： 
- 前端登录界面 
- 终端显示优化
- 文件传输功能，目前默认会传输到/tmp目录下，后面考虑传到家目录或者自定义目录
- 测试连接功能

特色（feature）
- 跨平台（cross platform）
- 浏览器环境（browser-in）
- 支持上传文件（transform file）

## 图片展示
前端登陆界
![image](https://user-images.githubusercontent.com/31361595/184635512-bdf7883b-52a1-4515-b380-6b9ba18bfa11.png)

终端显示优化
![image](https://user-images.githubusercontent.com/31361595/184619160-1df7604d-9a88-435d-8ac2-592161d9eadf.png)

当无法连接到主机时，登录界面显示错误信息
![image](https://user-images.githubusercontent.com/31361595/184631740-4f45d221-4fa7-4076-86b1-2d5cc4ef6dff.png)


技术架构图
![image](https://user-images.githubusercontent.com/31361595/184622254-99fe8b44-c4d1-45f0-a1c9-4c0d742490f5.png)

解决的问题：
1.浏览器和SpringBoot的WebSocket连接会通过接口获取服务器的地址，而不是直接写死为127.0.0.1，这样至少在同一个局域网中可以有多个浏览器来访问服务器；在公网上效果如何，我并没有进行测试，有条件的可以试试。

欢迎大佬们提issue和pr，欢迎大家参与项目
<br><br><br><br><br><br><br><br><br><br>

从此行开始为原作者的话
<hr />
## 2020-3-13

最近有些事挺忙的，可能暂时稍微顺延一下更新的日程(大概需要在4月1号之后)，但是一定会继续维护下去的，望多多理解。

## 启动

项目导入IDEA后可以直接进行运行，没有任何外部依赖~~

**本项目的Blog**：[使用纯Java实现一个WebSSH项目](https://blog.objectspace.cn/2020/03/10/%E4%BD%BF%E7%94%A8%E7%BA%AFJava%E5%AE%9E%E7%8E%B0%E4%B8%80%E4%B8%AAWebSSH%E9%A1%B9%E7%9B%AE/)

**注意**：

由于前端代码中没有指定终端的信息

所以需要各位自己输入这些信息，位置在webssh.html中

```javascript
openTerminal( {
  /*operate:'connect',
  host: '',//IP
  port: '',//端口号
  username: '',//用户名
  password: ''//密码*/
});
```

## 运行展示

- ### 连接

  ![连接](http://image.objectspace.cn/%E8%BF%9E%E6%8E%A5.png)

- ### 连接成功

  ![连接成功](http://image.objectspace.cn/%E8%BF%9E%E6%8E%A5%E6%88%90%E5%8A%9F.png)

- ### 命令操作

  ls命令：

  ![ls命令](http://image.objectspace.cn/ls%E5%91%BD%E4%BB%A4.png)

  vim编辑器:

  ![vim编辑器](http://image.objectspace.cn/vim%E7%BC%96%E8%BE%91%E5%99%A8.png)

  top命令：

  ![top命令](http://image.objectspace.cn/top%E5%91%BD%E4%BB%A4.png)

## 写在最后
欢迎各位大佬给我提issue，感谢！
