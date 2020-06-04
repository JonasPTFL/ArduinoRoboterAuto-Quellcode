/*
    RoboterAuto - Informatik Projekt
    Autor: Jonas Pollpeter
    Version: 4.0
    Code für: Fernbedienung ESP Modul
*/
/////////////////// libraries
#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

/////////////////// constants
const char *ssid = "RoboterAuto";
const char *password = "JonasRoboterProjekt";
const String serverDisconnectedMessage = "SF.";
const String serverConnectedMessage = "ST.";
const String serverConnectedMessage = "ST.";

/////////////////// objects
IPAddress ServerIP(192, 168, 4, 1);
IPAddress ip(192, 168, 4, 3);
IPAddress gateway(192, 168, 4, 3);
IPAddress subnet(255, 255, 255, 0);
WiFiUDP udp;

/////////////////// variables
String packetToSend;
char incomingPacket[255];

void setup() {
  Serial.begin(9600);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  WiFi.config(ip, gateway, subnet);
  udp.begin(2000);
}

void loop() {
  boolean looped = false;
  while (WiFi.status() != WL_CONNECTED) {
    if (!looped) {
      Serial.print(serverDisconnectedMessage);
      looped = true;
    }
    delay(250);
  }
  if (looped) {
    Serial.print(serverConnectedMessage);
  }
  if (!udp.parsePacket()) {
    if (Serial.available() > 0) {
      while (Serial.available() > 0) {
        packetToSend += ((char)Serial.read());
        delay(1);
      }
      udp.beginPacket(ServerIP, 2000);
      udp.print(packetToSend);
      udp.endPacket();
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
