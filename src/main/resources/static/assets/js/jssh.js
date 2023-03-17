// 在前端打开模拟终端

function openTerminal(operate, host, port, username, password, id) {
    return internalFunc({
        operate: operate, // 指令，默认为connect
        host: host,// IP
        port: port,// 端口号
        username: username,// 用户名
        password: password // 密码
    });

    function internalFunc(options) {
        // 新建客户端的socket
        const client = new WSSHClient();
        const terminal = new Terminal({
            cols: 100,
            // rows: _this.cols, // 不指定可以自适应屏幕
            cursorBlink: true, // 光标闪烁
            cursorStyle: "block", // 光标样式  null | 'block' | 'underline' | 'bar'
            scrollback: 800, //回滚
            tabStopWidth: 8, //制表宽度
            screenKeys: true,
            onResize: true
        });

        terminal.on('data', function (data) {
            // 键盘输入时的回调函数
            client.sendClientData(data);
        });

        // true代表是否聚集在光标的位置 ，不加会有warning
        terminal.open(document.getElementById(id), true);

        // 在页面上显示连接中...
        terminal.write('Connecting...\r\n');

        // 执行连接操作
        client.connect({
            onError: function (error) {
                // 连接失败
                terminal.write('Error: ' + error + '\r\n');
            }, onConnect: function () {
                // 连接成功
                client.sendInitData(options);
            }, onClose: function () {
                // 连接关闭
                terminal.write("\r\nconnection has closed...");
            }, onData: function (data) {
                // 收到数据时
                terminal.write(data);
            }
        });

        return terminal;
    }
}



// websocket前端代码，负责发送字符串和处理结果
class WSSHClient {
    // 获取springboot服务器地址
    getHost() {
        const wssProtocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
        return wssProtocol + window.location.host + '/webssh'; // js可以直接获取服务器的地址
    }

    connect(options) {
        const host = this.getHost();

        // 浏览器不支持websock就直接退出
        if (window.WebSocket) {
            this._connection = new WebSocket(host);
        } else {
            options.onError('WebSocket Not Supported');
            return;
        }

        this._connection.onopen = function () {
            options.onConnect();
        };

        this._connection.onmessage = function (evt) {
            const data = evt.data.toString();
            //data = base64.decode(data);
            options.onData(data);
        };

        this._connection.onclose = function () {
            options.onClose();
        };
    }

    // 发送json序列化数据
    send(data) {
        this._connection.send(JSON.stringify(data));
    }

    // 连接参数
    sendInitData(options) {
        this._connection.send(JSON.stringify(options));
    }

    // 发送指令
    sendClientData(data) {
        this._connection.send(JSON.stringify({
            "operate": "command", "command": data
        }))
    }
}
