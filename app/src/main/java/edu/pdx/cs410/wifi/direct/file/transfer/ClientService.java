
package edu.pdx.cs410.wifi.direct.file.transfer;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.app.IntentService;
import android.content.Intent;
import android.os.Message;
import android.os.ResultReceiver;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class ClientService extends IntentService {

    private boolean serviceEnabled;

    private int port;
    private File fileToSend;
    private ResultReceiver clientResult;
    private WifiP2pDevice targetDevice;
    private WifiP2pInfo wifiInfo;
    private int udpCode;

    public ClientService() {
        super("ClientService");
        serviceEnabled = true;

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        port = intent.getExtras().getInt("port");
        fileToSend = (File) intent.getExtras().get("fileToSend");
        clientResult = (ResultReceiver) intent.getExtras().get("clientResult");
        targetDevice = (WifiP2pDevice) intent.getExtras().get("targetDevice");
        wifiInfo = (WifiP2pInfo) intent.getExtras().get("wifiInfo");
        udpCode = (Integer) intent.getExtras().get("udpCode");

        if (!wifiInfo.isGroupOwner) {
            InetAddress targetIP = wifiInfo.groupOwnerAddress;
            Socket clientSocket;
            DatagramSocket udpSocket;
            OutputStream os ;

            if (udpCode == 1) {
                //udp sender code
                try {
                    udpSocket = new DatagramSocket();
                    byte[] buffer = new byte[1452];
                    FileInputStream fis = new FileInputStream(fileToSend);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    int bytesRead;
                    bytesRead = bis.read(buffer, 0, buffer.length);

                    while (bytesRead > 0) {
                        DatagramPacket packet = new DatagramPacket(buffer,
                                buffer.length, targetIP, 9750);
                        udpSocket.send(packet);
//                        bytesSent += buffer.length;
                        bytesRead = bis.read(buffer, 0, buffer.length);
                        Thread.sleep(250,0);
                        //todo exception handling
                    }

                    buffer = "\0\0".getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer,
                            buffer.length, targetIP, 9750);
                    udpSocket.send(packet);
                    udpSocket.close();
                    signalActivity("File Transfer Complete:" + String.valueOf(System.currentTimeMillis()));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                //TCP sender code
                try {

                    clientSocket = new Socket(targetIP, port);
                    os = clientSocket.getOutputStream();
                    PrintWriter pw = new PrintWriter(os);
                    InputStream is = clientSocket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);

                    signalActivity("About to start handshake");

                    byte[] buffer = new byte[4096];
                    FileInputStream fis = new FileInputStream(fileToSend);
                    BufferedInputStream bis = new BufferedInputStream(fis);

                    String fileName =  fileToSend.getName() + '\n';
                    byte[] fileNameBytes = fileName.getBytes();

                    os.write(fileNameBytes, 0, fileNameBytes.length);

                    while (true) {

                        int bytesRead = bis.read(buffer, 0, buffer.length);
                        if (bytesRead == -1) {
                            break;
                        }
                        os.write(buffer, 0, bytesRead);
                        os.flush();
                    }
                    fis.close();
                    bis.close();
                    br.close();
                    isr.close();
                    is.close();
                    pw.close();
                    os.close();

                    clientSocket.close();

                    signalActivity("File Transfer Complete, sent file: " + fileToSend.getName());


                } catch (IOException e) {
                    signalActivity(e.getMessage());
                } catch (Exception e) {
                    signalActivity(e.getMessage());

                }
            }

        } else {
            signalActivity("This device is a group owner, therefore the IP address of the " +
                    "target device cannot be determined. File transfer cannot continue");
        }


        clientResult.send(port, null);
    }


    public void signalActivity(String message) {
        Bundle b = new Bundle();
        b.putString("message", message);
        clientResult.send(port, b);
    }


    public void onDestroy() {
        serviceEnabled = false;
        stopSelf();
    }

}