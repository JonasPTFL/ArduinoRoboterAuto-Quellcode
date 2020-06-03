# ArduinoRoboterAuto-Quellcode
Dies ist der Quellcode einer Projektarbeit über ein ferngesteuertes Roboterauto.

## Übersicht
1. Roboterauto
   1. [Arduino Quellcode](Roboterauto/RoboterautoArduino/RoboterautoArduino.ino)
   2. [ESP Quellcode](Roboterauto/RoboterautoESP/RoboterautoESP.ino)
2. Fernbedienung
   1. [Arduino Quellcode](Fernbedienung/FernbedienungArduino/FernbedienungArduino.ino)
   2. [ESP Quellcode](Fernbedienung/FernbedienungESP/FernbedienungESP.ino)

## Anmerkung
Erstellt wurden die Dateien mit der offiziellen Arduino Entwicklungsumgebung. Um für die ESP-Module Quellcode zu kompilieren, ist es notwendig das ESP-Paket im Boardverwalter zu installieren.

## Verwendete Bibliotheken
* [`SoftwareSerial.h`](https://www.arduino.cc/en/Reference/SoftwareSerial)
* [`ESP8266WiFi.h`](https://github.com/esp8266/Arduino/tree/master/libraries/ESP8266WiFi)
* [`WiFiUdp.h`](https://github.com/esp8266/Arduino/tree/master/libraries/ESP8266WiFi)
* [`Wire.h`](https://www.arduino.cc/en/Reference/Wire)
* [`wiinunchuck.h`](https://github.com/timtro/wiinunchuck-h)
* [`SPI.h`](https://www.arduino.cc/en/Reference/SPI)
* [`MFRC522.h`](https://github.com/miguelbalboa/rfid)

---
_von_ **Jonas Pollpeter**
<p align=right><i>2020 erstellt</i></p>

