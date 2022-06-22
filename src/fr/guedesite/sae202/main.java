package fr.guedesite.sae202;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.net.UnknownHostException;

import fr.guedesite.sae202.GraphPanel.DoubleTime;

/**
 * @author Hugo Mathieu
 * 
 * Ce programme permet la liaison avec une arduino en Udp,
 * dans le bute de récupéré les informations du capteur de luminosité et de température
 * et d'en ressortir un graphique dynamique.
 */

public class main {
	
	public static main Instance;
	public GraphPanel graphTemp, graphLum; // Nos deux graphique
	

	public static void main(String[] args) {
		final int port = 8211;					// Port d'écoute de l'arduino
		final String ip = "10.33.109.120";		// Ip de l'arduino
		Instance = new main(port, ip);
	}
	
	
	public DatagramSocket client; // Socket Udp pour la réception
	public DatagramPacket output; // Packet Udp pour l'envois

	
	public main(int port, String ip) {
		this.graphTemp = GraphPanel.createAndShowGui("Température"); // On initialise nos graphique 
		this.graphLum = GraphPanel.createAndShowGui("Luminosité");
		while(true) {
			try {
				client = new DatagramSocket();
				
				// On lance en parallèle avec un thread la réception des packets qui contiennent les 
				Thread recieve = new Thread() {
					@Override
					public void run() {
						byte[] buffer = new byte[24];
				        while(true) {
				        	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				             try {
								client.receive(packet);// Fait une pause jusqu'a réception d'un paquet
								
								String received = new String(packet.getData(), 0, packet.getLength());
								System.out.println(received);
								
								// On décompose la chaine de caractère pour vérifié qu'on reçoit bien se que l'ont veut
								String first = received.substring(0, 2);
								if(first.equals("AT")) {
									
									// On décompose à nouveau pour savoir quel type de donnée a été envoyé
									String cmd = received.substring(2,3);
									if(cmd.equals("T")) {
										String value = received.substring(3);
										
										// On ajoute dans l'instance du graphique
										graphTemp.scores.add(new DoubleTime(Double.parseDouble(value), System.currentTimeMillis())); 
										// On met à jour le rendu du graphique
										graphTemp.updateValue();
										System.out.println("add "+value);
									} else if(cmd.equals("L")) {
										String value = received.substring(3);
										graphLum.scores.add(new DoubleTime(Double.parseDouble(value), System.currentTimeMillis()));
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
				recieve.start();// On lance le thread


				// On envois d'un message "update" toutes les secondes pour que l'arduino n'envois pas dans le vide des informations
				// EN faisant cela on se rapproche du TCP
				while(true) {
					send("update", ip, port);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				close(); // En cas d'erreur on ferme proprement not
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

