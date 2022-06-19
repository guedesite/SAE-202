package fr.guedesite.sae202;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.net.UnknownHostException;
import java.util.Scanner;


public class main {
	
	public static main Instance;
	public GraphPanel graphTemp, graphLum;
	

	public static void main(String[] args) {
		final int port = 8211;
		final String ip = "10.33.109.120";
		Instance = new main(port, ip);
	}
	
	
	public DatagramSocket client, clientSender;
	public DatagramPacket output;

	
	public main(int port, String ip) {
		this.graphTemp = GraphPanel.createAndShowGui("Température");
		this.graphLum = GraphPanel.createAndShowGui("Luminosité");
		while(true) {
			try {
				client = new DatagramSocket();
				Thread recieve = new Thread() {
					@Override
					public void run() {
						byte[] buffer = new byte[24];
				        while(true) {
				        	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				             try {
								client.receive(packet);
								String received = new String(packet.getData(), 0, packet.getLength());
								System.out.println(received);
								String first = received.substring(0, 2);
								if(first.equals("AT")) {
									String cmd = received.substring(2,3);
									if(cmd.equals("T")) {
										String value = received.substring(3);
										graphTemp.scores.add((double) Integer.parseInt(value));
										graphTemp.updateValue();
										System.out.println("add "+value);
									} else if(cmd.equals("L")) {
										String value = received.substring(3);
										graphLum.scores.add((double) Integer.parseInt(value));
										graphLum.updateValue();
										System.out.println("add "+value);
									} else {
										System.out.println("Unknow "+cmd);
									}
								} else {
									System.out.println("Unknow "+first);
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

				        }
				    }
				};
				recieve.start();
				clientSender = new DatagramSocket();
				while(true) {
					send("update", ip, port);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				close();
			}
		}
	}
	
	
	
	public void send(String str, String ip, int port) {
		byte[] buffer = str.getBytes();
		try {
			output = new DatagramPacket(buffer,buffer.length,InetAddress.getByName(ip),port);
			client.send(output);
			System.out.println("send "+str);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}
	
	public void close() {
		try {client.close();}catch(Exception e) {}
	}

}
