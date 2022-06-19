#include <Ethernet.h>
#include <EthernetUdp.h>
#include <Arduino.h>
#include <IRremote.hpp>
#include <FastLED.h>
#define LED_PIN 3
#define NUM_LEDS 1
#define DECODE_NEC 
#define IR_PIN 7
#define lumPin A0
#define tempPin A1

#if !defined(STR_HELPER)
#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)
#endif

byte mac[] = {
  0x90, 0xA2, 0xDA, 0x10, 0x57, 0xA6
};
IPAddress ip(10, 33, 109, 120);
unsigned int localPort = 8211;
char packetBuffer[UDP_TX_PACKET_MAX_SIZE];
boolean is_connected = false;
boolean is_on = false;
int speed_transmission = 5;
long last_loop = 0;
long last_IR = 0;
long last_update = 0;
CRGB leds[NUM_LEDS];
EthernetUDP Udp;


void setup_UDP() {
  Ethernet.begin(mac, ip);
  while (!Serial) {}

  if (Ethernet.hardwareStatus() == EthernetNoHardware) {
    Serial.println("Ethernet shield was not found.  Sorry, can't run without hardware. :(");
    while (true) {
      delay(1);
    }
  }
  if (Ethernet.linkStatus() == LinkOFF) {
    Serial.println("Ethernet cable is not connected.");
  } else {
    Serial.println("Setup UDP Done");
  }

  Udp.begin(localPort);
}

void setup_IR() {
  IrReceiver.begin(IR_PIN, ENABLE_LED_FEEDBACK);

  Serial.println(F("Ready to receive IR signals of protocols: "));
  printActiveIRProtocols(&Serial);
  Serial.println(F("at pin " STR(IR_PIN)));
  Serial.println("Setup IR Done"); 
}

void setup_LED() {
  FastLED.addLeds<WS2812, LED_PIN, GRB>(leds, NUM_LEDS);
  colorLed(0, 255, 0, 0);
  Serial.println("Setup LED Done"); 
}

void setup_TEMP() {
  pinMode(tempPin, INPUT);
  
  Serial.println("Setup TEMP Done"); 
}

void setup(){
  Serial.begin(9600);
  delay(1000);
  setup_UDP();
  setup_IR();
  setup_LED();
  setup_TEMP();
}


void colorLed(int id, int red, int green, int blue){
  leds[id] = CRGB(red, green, blue);
  FastLED.show();
}

void sendData(String value) {
  char char_array[value.length()+1];
  value.toCharArray(char_array, value.length()+1);

   Serial.println(char_array);
  Udp.beginPacket(Udp.remoteIP(), Udp.remotePort());
  Udp.write(char_array);
  Udp.endPacket();
}
 
void receiveData() {
  int packetSize = Udp.parsePacket();
  if (packetSize) {
    Serial.print("From ");
    IPAddress remote = Udp.remoteIP();
    for (int i=0; i < 4; i++) {
      Serial.print(remote[i], DEC);
      if (i < 3) {
        Serial.print(".");
      }
    }
    Serial.print(", port ");
    Serial.println(Udp.remotePort());
    Udp.read(packetBuffer, UDP_TX_PACKET_MAX_SIZE);
    Serial.println("Contents:");
    Serial.println(packetBuffer);
    last_update = millis();
    is_connected = true;
  }
}

void IR_Receive() {
  // 144 = on/off
  // 176 = augmenter vitesse transmission
  // 178 = baisser vitesse transmission
  if (IrReceiver.decode()) {
      if (millis() - 500 > last_IR){
        last_IR = millis();
        IrReceiver.printIRResultShort(&Serial);
        IrReceiver.resume();  
        int value = IrReceiver.decodedIRData.command;
        Serial.println(value);
         if (value == 144){
          if (is_on){
            is_on = false;
            colorLed(0, 255, 0, 0);       
          } else if (!is_on){
            is_on = true;
            colorLed(0, 255, 255, 0);
          }
          
         } else if (value == 176){
          if (speed_transmission < 10){
            speed_transmission++;
          } else {
             Serial.println("Already min speed");  
          }
         } else if (value == 178){
          if (speed_transmission > 0){
            speed_transmission--;
          } else {
            Serial.println("Already max speed");     }
         } else {
          Serial.println("Not implemented yet");
         }
      } else {
        IrReceiver.resume();  
      }
    }
}


int luminosite(){
  int lumReading = analogRead(lumPin);
  return lumReading; 
}

int temperature(){
  int valeur_brute = analogRead(tempPin);
  float temp = valeur_brute * (5.0 / 1023.0 * 100.0);
  return temp;
}

void sendInfo(){
  if (millis() - (speed_transmission * 100) > last_loop){
    last_loop = millis();
    sendData("ATT"+String(temperature()));
    sendData("ATL"+String(luminosite()));
  }
}

void loop() {
  IR_Receive();
  if (is_on){
    receiveData();
  }
  if (is_connected){
    if (millis() - 2000 > last_update){
      is_connected = false;
    } else {
      sendInfo();
    }
  }

}
