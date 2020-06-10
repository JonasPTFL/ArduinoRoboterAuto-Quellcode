/*
    RoboterAuto - Informatik Projekt
    Autor: Jonas Pollpeter
    Version: 4.0
    Quellcode für: Roboterauto Arduino
*/
/////////////////// libraries
#include            <SoftwareSerial.h>

/////////////////// compilation constants
#define             ENA 5 // left engine control
#define             ENB 6 // right engine control
#define             IN1 7 // left 1
#define             IN2 8 // left 2
#define             IN3 9 // right 1
#define             IN4 11 // right 2
#define             ECHO A4
#define             TRIG A5
#define             GREEN_LED 10
#define             FRONT_LIGHTS_LEDS 12
#define             REAR_LIGHTS_LEDS 13

/////////////////// constants
const     String    clientConnectedMessage = "CT.";
const     String    clientDisconnectedMessage = "CF.";
const     String    distanceRequestMessage = "D?";
const     String    lightsRequestMessage = "L?";
const     String    changeModeStartString = "M";

const     int       greenLedPWMLightValue = 20;
const     long      distanceMeasurementInterval = 100;
const     long      lightsBlinkInterval = 300;

/////////////////// objects
SoftwareSerial      ESPSerial(2, 3); // RX | TX

/////////////////// variables
String              command = "";

boolean             obstacleExisting;
boolean             frontLightsOn = true;
boolean             frontLightsToggleOn;
boolean             rearLightsOn = true;
boolean             statusLightOn = true;

boolean             clientConnected;

unsigned  long      previousDistanceMeasurementMillis = 0;
unsigned  long      previousLightsBlinkMillis = 0;

int                 carSpeedLeft = 0;
int                 carSpeedRight = 0;
int                 lastMeasurement = 0;
int                 carMode = 1; // 1: normal control for every client, 2: controlling together mode, 3: contrariwise controls, 4: keep distance mode

/////////////////// setup
void setup(void) {
  pinMode(TRIG, OUTPUT);
  pinMode(ECHO, INPUT);
  pinMode(GREEN_LED, OUTPUT);
  pinMode(FRONT_LIGHTS_LEDS, OUTPUT);
  pinMode(REAR_LIGHTS_LEDS, OUTPUT);
  ESPSerial.begin(9600);

  digitalWrite(FRONT_LIGHTS_LEDS, HIGH);
  digitalWrite(REAR_LIGHTS_LEDS, HIGH);
}

/////////////////// loop
void loop(void) {
  unsigned long currentMillis = millis();
  if (ESPSerial.available()) {
    while (ESPSerial.available() > 0) {
      command += ((char)ESPSerial.read());
      delay(1);
    }
    command.replace("\n", "");
    char lastChar = command.charAt(command.length() - 1);
    switch (lastChar) {
      case '!':
        if (carMode == 4) {
          command = "";
          break; // cancel if car is in keep distance mode
        }
        carSpeedLeft = command.substring(command.indexOf("L") + 1, command.indexOf("R")).toInt();
        carSpeedRight = command.substring(command.indexOf("R") + 1, command.length()).toInt();
        if (carMode == 3) { // to achieve a contrariwise control when enabled
          carSpeedLeft = -carSpeedLeft;
          carSpeedRight = -carSpeedRight;
        }
        // just allow to rotate or drive backward if obstacle is existing to prevent a collision, but it is also checked in distanceMeasurementInterval, but mybe sometimes to late...
        if (!collisionImpending()) {
          boolean controlsRight = command.charAt(0) == 'M'; // command.charAt(0) == 'M' because app sends 'm' before command when two player is enabled, to identify who sent the command
          if (carMode != 2 || controlsRight) {
            // right engine
            analogWrite(ENB, abs(carSpeedRight));
            boolean vorwardRight = carSpeedRight > 0;
            digitalWrite(IN3, !vorwardRight);
            digitalWrite(IN4, vorwardRight);
          }
          if (carMode != 2 || !controlsRight) {
            // left enigne
            analogWrite(ENA, abs(carSpeedLeft));
            boolean vorwardLeft = carSpeedLeft > 0;
            digitalWrite(IN1, vorwardLeft);
            digitalWrite(IN2, !vorwardLeft);
          }
        }
        command = "";
        break;

      case '.':
        if (command.equals(clientDisconnectedMessage)) {
          clientConnected = false;
          stopEngine();
          if (statusLightOn)  {
            for (int i = greenLedPWMLightValue; i >= 0; i--) {
              analogWrite(GREEN_LED, i);
              delay(4);
            }
          }
        } else if (command.equals(clientConnectedMessage)) {
          clientConnected = true;
          if (statusLightOn) {
            for (int i = 0; i <= greenLedPWMLightValue; i++) {
              analogWrite(GREEN_LED, i);
              delay(4);
            }
          }
        } else if (command.startsWith(changeModeStartString)) {
          carMode = command.substring(command.indexOf(changeModeStartString) + 1, command.indexOf(".")).toInt();
          stopEngine();
          if (statusLightOn) {
            for (int i = 0; i < carMode; i++) { // blink led to show new mode
              analogWrite(GREEN_LED, clientConnected ? 0 : greenLedPWMLightValue);
              delay(100);
              analogWrite(GREEN_LED, clientConnected ? greenLedPWMLightValue : 0);
              delay(100);
            }
          }
        }
        command = "";
        break;

      case '$':
        frontLightsOn = command.substring(command.indexOf("F") + 1, command.indexOf("R")).toInt() == 1;
        rearLightsOn = command.substring(command.indexOf("R") + 1, command.indexOf("S")).toInt() == 1;
        statusLightOn = command.substring(command.indexOf("S") + 1, command.length()).toInt() == 1;
        digitalWrite(FRONT_LIGHTS_LEDS, frontLightsOn);
        digitalWrite(REAR_LIGHTS_LEDS, rearLightsOn);
        analogWrite(GREEN_LED, statusLightOn && clientConnected ? greenLedPWMLightValue : 0);
        sendLightInformation();
        command = "";
        break;

      case '?':
        if (command.equals(distanceRequestMessage)) {
          String response = "D" + String(getDistance()) + ".";
          ESPSerial.print(response);
          command = "";
        } else if (command.equals(lightsRequestMessage)) {
          sendLightInformation();
          command = "";
        }
        break;
    }
  }
  if (currentMillis - previousDistanceMeasurementMillis >= distanceMeasurementInterval) {
    previousDistanceMeasurementMillis = currentMillis;
    lastMeasurement = getDistance();
    obstacleExisting = lastMeasurement <= 20;
    if (collisionImpending()) {
      stopEngine();
    }
    if (frontLightsOn) {
      if (obstacleExisting) {
        digitalWrite(FRONT_LIGHTS_LEDS, frontLightsToggleOn);
        frontLightsToggleOn = !frontLightsToggleOn;
      } else {
        digitalWrite(FRONT_LIGHTS_LEDS, HIGH);
      }
    }
  }
  if (carMode == 4) {
    if (lastMeasurement > 30 && lastMeasurement < 100) {
      // drive backward
      digitalWrite(ENA, HIGH);
      digitalWrite(ENB, HIGH);
      digitalWrite(IN1, HIGH);
      digitalWrite(IN2, LOW);
      digitalWrite(IN3, LOW);
      digitalWrite(IN4, HIGH);
    } else if (lastMeasurement < 20) {
      // drive forward
      digitalWrite(ENA, HIGH);
      digitalWrite(ENB, HIGH);
      digitalWrite(IN1, LOW);
      digitalWrite(IN2, HIGH);
      digitalWrite(IN3, HIGH);
      digitalWrite(IN4, LOW);
    } else {
      stopEngine();
    }
  }
}


/////////////////// getDistance-method
int getDistance() {
  digitalWrite(TRIG, LOW);
  delayMicroseconds(5);
  digitalWrite(TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG, LOW);
  float echoTime = pulseIn(ECHO, HIGH);
  return (int)(echoTime / 58);
}

/////////////////// collisionImpending-method
boolean collisionImpending() {
  return obstacleExisting && carSpeedLeft > 0 && carSpeedRight > 0;
}

/////////////////// stopEngine-method
void stopEngine() {
  digitalWrite(ENA, LOW);
  digitalWrite(ENB, LOW);
}

/////////////////// sendLightInformation-method
void sendLightInformation() {
  String response = "F" + String(frontLightsOn) + "R" + String(rearLightsOn) + "S" + String(statusLightOn);
  ESPSerial.print(response);
}
