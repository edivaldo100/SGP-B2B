package com.edivaldo.pedidos.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
public class IsAlive {
    @Value("${server.port}")
    private String serverPort;

    @GetMapping("/isAlive")
    public String isAlive() {

        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown-host";
        }
        System.out.println("=================   Is Alive  ==========================");
        System.out.println("=================   Is Alive  ==========================");
        System.out.println("UP - Host: " + hostname +" --- ON in port: " + serverPort);
        System.out.println("=================   Is Alive  ==========================");
        System.out.println("=================   Is Alive  ==========================");
        return "UP - Host: " + hostname +" --- ON in port: " + serverPort;

    }

}
