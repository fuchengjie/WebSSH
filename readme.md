Since the original author hadn't updated the project in over two years and hadn't solved the issues and pr, I forked one myself and optimized the front-end interface.

# README.md
## zh_CN [简体中文](readme.zh_CN.md)

Currently added features are:
- Front-end login page
- Terminal display optimization
- File transfer function. By default, the file is transferred to the /tmp directory. Later, you can transfer the file to a home directory or a custom directory
- Test the connection function
- Public key login, due to jsch version issues,
  If your id_rsa starts with "-----BEGIN OPENSSH PRIVATE KEY-----", run the command 'ssh-keygen -p -f &lt; privateKeyFile&gt;  -m pem 'Converts the format

The feature
- cross platform
- browser-in
- Support for uploading files (transform file)

## picture
login
![image](https://user-images.githubusercontent.com/31361595/184635512-bdf7883b-52a1-4515-b380-6b9ba18bfa11.png)

terminal
![image](https://user-images.githubusercontent.com/31361595/184619160-1df7604d-9a88-435d-8ac2-592161d9eadf.png)

Symptom When the host cannot be connected, an error message is displayed on the login page
![image](https://user-images.githubusercontent.com/31361595/184631740-4f45d221-4fa7-4076-86b1-2d5cc4ef6dff.png)


Technical architecture diagram
![image](https://user-images.githubusercontent.com/31361595/184622254-99fe8b44-c4d1-45f0-a1c9-4c0d742490f5.png)
Problems solved:
1. The WebSocket connection between the browser and SpringBoot obtains the server address through the interface instead of being written to 127.0.0.1, so that at least multiple browsers can access the server on the same LAN. On the public network effect, I did not test, there are conditions can try.

Welcome issues and pr (No promise to do it), welcome everyone to participate in the project

<br><br><br><br><br><br><br><br><br><br>

