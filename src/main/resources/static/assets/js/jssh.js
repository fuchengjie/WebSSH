function WSSHClient() {
}

WSSHClient.prototype._generateEndpoint = function () {
    const wssProtocol = window.location.protocol === 'https:'? 'wss://':'ws://';

    return wssProtocol +  window.location.host + '/webssh'; // js可以直接获取服务器的地址
};

WSSHClient.prototype.connect = function (options) {
    var endpoint = this._generateEndpoint();

    if (window.WebSocket) {
        //如果支持websocket
        this._connection = new WebSocket(endpoint);
    } else {
        //否则报错
        options.onError('WebSocket Not Supported');
        return;
    }

    this._connection.onopen = function () {
        options.onConnect();
    };

    this._connection.onmessage = function (evt) {
        var data = evt.data.toString();
        //data = base64.decode(data);
        options.onData(data);
    };


    this._connection.onclose = function () {
        options.onClose();
    };
};

WSSHClient.prototype.send = function (data) {
    // json序列化数据
    this._connection.send(JSON.stringify(data));
};

WSSHClient.prototype.sendInitData = function (options) {
    //连接参数
    this._connection.send(JSON.stringify(options));
}

WSSHClient.prototype.sendClientData = function (data) {
    //发送指令
    this._connection.send(JSON.stringify({
        "operate": "command",
        "command": data
    }))
}

var client = new WSSHClient();

// 打开模拟终端
function openTerminal1(operate1, host1, port1, username1, password1, id) {

    // 黑框框
    openTerminal({
        operate: operate1,
        host: host1,// IP
        port: port1,// 端口号
        username: username1,// 用户名
        password: password1 // 密码
    });

    function openTerminal(options) {
        // 新建客户端的socket
        var client = new WSSHClient();
        var term = new Terminal({
            cols: 97,
            rows: 37,
            cursorBlink: true, // 光标闪烁
            cursorStyle: "block", // 光标样式  null | 'block' | 'underline' | 'bar'
            scrollback: 800, //回滚
            tabStopWidth: 8, //制表宽度
            screenKeys: true
        });

        term.on('data', function (data) {
            // 键盘输入时的回调函数
            client.sendClientData(data);
        });

        // true代表是否聚集在光标的位置 ，不加会有warning
        term.open(document.getElementById(id), true);

        //在页面上显示连接中...
        term.write('Connecting...\r\n');

        //执行连接操作
        client.connect({
            onError: function (error) {
                // 连接失败回调
                term.write('Error: ' + error + '\r\n');
            },
            onConnect: function () {
                // 连接成功回调
                client.sendInitData(options);
            },
            onClose: function () {
                // 连接关闭回调
                term.write("\r\nconnection has closed...");
            },
            onData: function (data) {
                // 收到数据时回调
                term.write(data);
            }
        });
    }
}