/*
    RoboterAuto - Informatik Projekt
    Autor: Jonas Pollpeter
    Version: 2.0
    Code für: RoboterAuto ESP Modul
*/
/////////////////// libraries
#include <SoftwareSerial.h>
#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

/////////////////// constants
const char*   ssid = "RoboterAuto";
const char*   password = "JonasRoboterProjekt";
const String  clientConnectedMessage = "CT.";
const String  clientDisconnectedMessage = "CF.";
const String  connectedDevicesRequest = "CC?";

/////////////////// objects
WiFiServer server(80);
IPAddress ServerIP(192, 168, 4, 1);
IPAddress ClientIP(192, 168, 4, 2);
WiFiUDP udp;

/////////////////// variables
String packetToSend;
char incomingPacket[255];
int connectedDevices = 0;

void setup() {
  Serial.begin(9600);

  WiFi.softAP(ssid, password);
  udp.begin(2000);
}

void loop() {
  int newValue = WiFi.softAPgetStationNum();
  if (newValue == 0 && connectedDevices > 0) { // connectedDevices was > 0, but then  all devices disconnected...
    Serial.print(clientDisconnectedMessage);
    connectedDevices = newValue;
  } else if (newValue > 0 && connectedDevices == 0) { // connectedDevices was 0, but then a device connected...
    Serial.print(clientConnectedMessage);
    connectedDevices = newValue;
  }
  if (!udp.parsePacket()) {
    if (Serial.available() > 0) {
      while (Serial.available() > 0) {
        packetToSend += ((char)Serial.read());
        delay(1);
      }
      if (packetToSend.equals(connectedDevicesRequest)) {
        Serial.print("C");
        Serial.print(connectedDevices);
        Serial.print(".");
      } else {
        udp.beginPacket(ClientIP, 2000);
        udp.print(packetToSend);
        udp.endPacket();
      }
      packetToSend = "";
    }
  } else {
    int packetLength = udp.read(incomingPacket, 255);
    if (packetLength > 0) {
      incomingPacket[packetLength] = 0;
    }
    Serial.print(incomingPacket);
    delay(20);
  }
}
