package com.example.sftpconnection;

import com.jcraft.jsch.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.*;

@RestController
@RequestMapping("/sftp")
@Configuration
@EnableScheduling
public class ServiceSFTP {

    @GetMapping("/sendFile")
    @Scheduled(cron = "0 0/56 11 * * *")
    public void SFTP(){
        String configPath = System.getProperty("user.dir") + "/config.json";
        Iterator<JSONObject> memberList = getMember(configPath);

        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String path = String.format("C:\\Users\\COMCOM\\Documents\\BT20\\%s\\" , formattedDate);

        while (memberList.hasNext()) {
            JSONObject jsonMember = memberList.next();
            String Name = jsonMember.get("Name").toString();
            System.out.println("Sending files to :" + Name);
            String IP = jsonMember.get("IP").toString();
            String User = jsonMember.get("User").toString();
            String Pass = jsonMember.get("Password").toString();
            int Port = ((Number) jsonMember.get("Port")).intValue();
            String sourcePath = path + jsonMember.get("Source Path").toString();
            String destinationPath = jsonMember.get("Destination Path").toString();
            File dir = new File(sourcePath);
            File[] directoryListing = dir.listFiles();
            send(IP, Port, User, Pass, destinationPath, directoryListing);
        }


    }


    static Iterator<JSONObject> getMember(String s) {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(s));
            JSONObject jsonObject = (JSONObject) obj;
            JSONArray companyList = (JSONArray) jsonObject.get("Member List");
            Iterator<JSONObject> iterator = companyList.iterator();
            return iterator;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean send(String IP, int Port, String User, String Password, String destinationPath, File[] files) {
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        System.out.println("preparing the host information for sftp.");

        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try {
            if (files != null) {
                JSch jsch = new JSch();
                session = jsch.getSession(User, IP, Port);
                session.setPassword(Password);
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();
                System.out.println("Host connected.");
                channel = session.openChannel("sftp");
                channel.connect();
                System.out.println("sftp channel opened and connected.");
                channelSftp = (ChannelSftp) channel;
                channelSftp.cd(destinationPath);
                channelSftp.mkdir(formattedDate);
                channelSftp.cd(formattedDate);
                for (File file : files) {
                    channelSftp.put(new FileInputStream(file), file.getName());
                }
                System.out.println("Succeeded");
            }
            else{
                // Handle the case where dir is not really a directory.
                // Checking dir.isDirectory() above would not be sufficient
                // to avoid race conditions with another process that deletes
                // directories.
            }
        } catch (Exception ex) {
            System.out.println("Exception found while transfer the response.");
            System.out.println(ex.toString());
        } finally {
            try{
                channelSftp.exit();
                // System.out.println("sftp Channel exited.");
                channel.disconnect();
                // System.out.println("Channel disconnected.");
                session.disconnect();
                // System.out.println("Host Session disconnected.");
                return  true;
            } catch (Exception ex) {
                System.out.println(ex.toString());
                return  false;
            }

        }
    }

}