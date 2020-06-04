# ArduinoRoboterAuto-Quellcode
Dies ist der Quellcode einer Projektarbeit über ein ferngesteuertes Roboterauto.

## Übersicht
1. [Android App - Roboterauto Steuerung](https://github.com/JonasPTFL/ArduinoRoboterAuto-Quellcode/tree/master/Android%20App/RASteuerung)
2. Roboterauto
   1. [Arduino Quellcode](Roboterauto/RoboterautoArduino/RoboterautoArduino.ino)
   2. [ESP Quellcode](Roboterauto/RoboterautoESP/RoboterautoESP.ino)
3. Fernbedienung
   1. [Arduino Quellcode](Fernbedienung/FernbedienungArduino/FernbedienungArduino.ino)
   2. [ESP Quellcode](Fernbedienung/FernbedienungESP/FernbedienungESP.ino)

## Anmerkung
Erstellt wurden die Arduino-Quellcode Dateien mit der offiziellen Arduino Entwicklungsumgebung.

## Verwendete Arduino-Bibliotheken 
* [`SoftwareSerial.h`](https://www.arduino.cc/en/Reference/SoftwareSerial)
* [`ESP8266WiFi.h`](https://github.com/esp8266/Arduino/tree/master/libraries/ESP8266WiFi)
* [`WiFiUdp.h`](https://github.com/esp8266/Arduino/tree/master/libraries/ESP8266WiFi)
* [`Wire.h`](https://www.arduino.cc/en/Reference/Wire)
* [`wiinunchuck.h`](https://github.com/timtro/wiinunchuck-h)
* [`SPI.h`](https://www.arduino.cc/en/Reference/SPI)
* [`MFRC522.h`](https://github.com/miguelbalboa/rfid)
## Verwendete externe Android-Bibliotheken 
* [Virtueller Joystick: `io.github.controlwear:virtualjoystick:1.10.1`](https://github.com/controlwear/virtual-joystick-android)

---
_von_ **Jonas Pollpeter**
<p align=right><i>2020 erstellt</i></p>

