
package edu.pdx.cs410.wifi.direct.file.transfer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import android.os.Bundle;


import android.app.IntentService;
import android.content.Intent;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;

public class ServerService extends IntentService {

    private boolean serviceEnabled;

    private int port;
    private File saveLocation;
    private ResultReceiver serverResult;
    DatagramSocket udpSocket1;
    public int udpCode1;

    public ServerService() {
        super("ServerService");
        serviceEnabled = true;

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            port = intent.getExtras().getInt("port");
            udpCode1 =(Integer) intent.getExtras().get("udpRecCode");
            saveLocation = (File) intent.getExtras().get("saveLocation");
            serverResult = (ResultReceiver) intent.getExtras().get("serverResult");
        } catch (NullPointerException e) {
            System.out.println("serverService is generating a null pointer exception and the try block is for that");
        }

        ServerSocket welcomeSocket ;
        Socket socket;

        if (udpCode1 != 1) {

            try {

                welcomeSocket = new ServerSocket(port);
                while (serviceEnabled)
                //while(true && serviceEnabled)
                {

                    socket = welcomeSocket.accept();
                    InputStream is = socket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    OutputStream os = socket.getOutputStream();
                    PrintWriter pw = new PrintWriter(os);
                    String inputData = "";
                    signalActivity("About to start handshake");


                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    String savedAs = "";
                    char nextChar;
                    while ('\n' != (nextChar = (char) is.read())) {
                        savedAs += nextChar;
                    }

                    File file = new File(saveLocation, "TCP-"+System.currentTimeMillis() + savedAs);

                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    while (true) {
                        bytesRead = is.read(buffer, 0, buffer.length);
                        if (bytesRead == -1) {
                            break;
                        }
                        bos.write(buffer, 0, bytesRead);
                        bos.flush();

                    }

                    bos.close();
                    socket.close();

                    signalActivity("File Transfer Complete-" +System.currentTimeMillis());
                }

            } catch (IOException e) {
                signalActivity(e.getMessage());

            } catch (Exception e) {
                signalActivity(e.getMessage());

            }
        } else {

            //udp receiver

            try {
                udpSocket1 = new DatagramSocket(9750);
                byte[] buf = new byte[1452];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                udpSocket1.receive(dp);
                File file1 = new File(saveLocation, "UDP" +"-"+ String.valueOf(System.currentTimeMillis()));
                FileOutputStream fos1 = new FileOutputStream(file1);
                BufferedOutputStream bos1 = new BufferedOutputStream(fos1);

                while (dp.getLength() != 0) {
                    byte[] buf1;
                    buf1 = dp.getData();

                    if (buf1.length == 2 && "\0\0".equals(new String(buf1))) {
                        break;
                    }

                    bos1.write(buf1, 0, buf1.length);
                    udpSocket1.receive(dp);
                }

                bos1.flush();
                bos1.close();
                fos1.flush();
                fos1.close();
                udpSocket1.close();
                signalActivity("File Transfer Complete-"+ String.valueOf(System.currentTimeMillis()));

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                udpSocket1.close();
            }
            //Signal that operation is complete
            serverResult.send(port, null);


        }
    }


    public void signalActivity(String message) {
        Bundle b = new Bundle();
        b.putString("message", message);
        serverResult.send(port, b);
    }


    public void onDestroy() {
        serviceEnabled = false;
        stopSelf();
    }

    public void setUDPCode(){
        udpCode1 = 1;
    }

}
