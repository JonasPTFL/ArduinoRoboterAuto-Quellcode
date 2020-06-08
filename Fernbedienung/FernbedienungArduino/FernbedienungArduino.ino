/*
    RoboterAuto - Informatik Projekt
    Autor: Jonas Pollpeter
    Version: 4.0
    Quellcode für: Fernbedienung Arduino
*/
/////////////////// libraries
#include            <SoftwareSerial.h>
#include            <Wire.h>
#include            <wiinunchuck.h>
#include            <SPI.h>
#include            <MFRC522.h>

/////////////////// compilation constants
#define             GREEN_LED   6
#define             BLUE_LED    5
#define             RED_LED     4
#define             RST_PIN     9
#define             SS_PIN      10

/////////////////// constants
const String        serverDisconnectedMessage = "SF.";
const String        serverConnectedMessage = "ST.";
const String        lightsOnCommand = "F1R1S1$";
const String        lightsOffCommand = "F0R0S0$";
const String        driveCommandTemplate = "L%1R%2!";

const int           speedMax = 255;
const int           joystickYMin = 33;
const int           joystickYMid = 129;
const int           joystickYMax = 255;
const int           joystickXMin = 26;
const int           joystickXMid = 128;
const int           joystickXMax = 223;
const int           minValueModeDirect = 100;
const int           minValueModeDynamic = 30;
const int           minValuePitchModeGyroscope = 25;
const int           maxValuePitchModeGyroscope = 70;
const int           minValueRollModeGyroscope = 50;
const int           maxValueRollModeGyroscope = 90;
const int           delayMeasurement = 75;
const int           cButtonHoldTime = 500;

const byte          cardUID[] = {0xE2, 0xB7, 0x88, 0x1B};

/////////////////// objects
SoftwareSerial      ESPserial(2, 3); // RX | TX
MFRC522             mfrc522(SS_PIN, RST_PIN);

/////////////////// variables
String              packetToSend = "";
String              incommingPacket = "";

boolean             stoppedCommandSent = true;
boolean             lightsOn = true;
boolean             registeredChip = false;

int                 zButtonState = 0;
int                 cButtonState = 0;
int                 lastZButtonState = 1;
int                 lastCButtonState = 1;
int                 controlMode = 1; // 1: direct rotation, 2: dynamic rotation, 3: gyroscope control direct

unsigned long       lastShakeDownMillis = 0;
unsigned long       lastShakeUpMillis = 0;
unsigned long       lastCButtonPressedMillis = 0;

/////////////////// setup
void setup(void) {
  pinMode(GREEN_LED, OUTPUT);
  pinMode(BLUE_LED, OUTPUT);
  pinMode(RED_LED, OUTPUT);
  SPI.begin();
  mfrc522.PCD_Init();
  ESPserial.begin(9600);
  nunchuk_init();
  delay(100);
  nunchuk_calibrate_joy();
  delay(100);
}

/////////////////// loop
void loop(void) {
  boolean chipValid = false;
  if (mfrc522.PICC_IsNewCardPresent() && mfrc522.PICC_ReadCardSerial()) {
    chipValid = true;
    for (int i = 0; i < 4; i++) {
      if (mfrc522.uid.uidByte[i] != cardUID[i]) {
        chipValid = false;
      }
    }
    mfrc522.PICC_HaltA();
    if (chipValid) {
      registeredChip = !registeredChip;
      for (int i = registeredChip ? 0 : 255; registeredChip ? i <= 255 : i >= 0; i = registeredChip ? i + 1 : i - 1) {
        analogWrite(BLUE_LED, i);
        delay(2);
      }
    } else {
      for (int i = 0; i < 3; i++) {
        digitalWrite(BLUE_LED, HIGH);
        delay(75);
        digitalWrite(BLUE_LED, LOW);
        delay(75);
      }
      if (registeredChip) {
        digitalWrite(BLUE_LED, HIGH);
      }
    }
  }
  if (!registeredChip) {
    mfrc522.PICC_HaltA();
    delay(1000);
    return;
  }

  nunchuk_get_data();
  unsigned long currentMillis = millis();
  int joystickXRaw = nunchuk_cjoy_x();
  int joystickYRaw = nunchuk_cjoy_y();
  int joystickXValue = map(joystickXRaw - 128, -102, 96, -speedMax, speedMax);
  int joystickYValue = map(joystickYRaw - 129, -96, 97, -speedMax, speedMax);
  int roll = nunchuk_rollangle();
  int pitch = nunchuk_pitchangle();
  boolean enabled = joystickXValue > 80 || joystickXValue < -80 || joystickYValue > 80 || joystickYValue < -80;
  zButtonState = nunchuk_zbutton();
  cButtonState = nunchuk_cbutton();
  if ((joystickXRaw == 0 && joystickYRaw == 0) || (joystickXRaw == 255 && joystickYRaw == 255) || analogRead(A4) > 1000) { // nunchuck not available or problems... initialize again
    nunchuk_init();
    digitalWrite(RED_LED, HIGH);
    delay(100);
    digitalWrite(RED_LED, LOW);
    delay(100);
    return;
  }

  //////////////////////// control modes
  int carSpeedLeft, carSpeedRight;
  switch (controlMode) {
    case 1:
      // control mode 1: direct rotation
      if (joystickYValue > minValueModeDirect) {
        carSpeedLeft = map(joystickYValue, minValueModeDirect, speedMax, 150, speedMax);
        carSpeedRight = carSpeedLeft;
        stoppedCommandSent = false;
      } else if (joystickYValue < -minValueModeDirect) {
        carSpeedLeft = map(joystickYValue, -minValueModeDirect, -speedMax, -150, -speedMax);
        carSpeedRight = carSpeedLeft;
        stoppedCommandSent = false;
      } else if (joystickXValue > minValueModeDirect) {
        carSpeedLeft = map(joystickXValue, minValueModeDirect, speedMax, 200, speedMax);
        carSpeedRight = -carSpeedLeft;
        stoppedCommandSent = false;
      } else if (joystickXValue < -minValueModeDirect) {
        carSpeedLeft = map(joystickXValue, -minValueModeDirect, -speedMax, -200, -speedMax);
        carSpeedRight = -carSpeedLeft;
        stoppedCommandSent = false;
      } else if (!stoppedCommandSent) {
        sendEngineCommand(0,0);
        stoppedCommandSent = true;
      }
      if (!stoppedCommandSent) {
        sendEngineCommand(carSpeedLeft,carSpeedRight);
        delay(delayMeasurement);
      }
      break;

    case 2:
      // control mode 2: dynamic rotation
      if (joystickYValue > minValueModeDynamic) {
        carSpeedLeft = map(joystickYValue, minValueModeDynamic, speedMax, 150, speedMax);
        carSpeedRight = carSpeedLeft;
        stoppedCommandSent = false;
      } else if (joystickYValue < -minValueModeDynamic) {
        carSpeedLeft = map(joystickYValue, -minValueModeDynamic, -speedMax, -150, -speedMax);
        carSpeedRight = carSpeedLeft;
        stoppedCommandSent = false;
      } else if (!stoppedCommandSent) {
        sendEngineCommand(0,0);
        stoppedCommandSent = true;
      }
      if (!stoppedCommandSent) {
        if (joystickXValue > minValueModeDynamic) {
          carSpeedRight = (abs(carSpeedRight) - map(joystickXValue, minValueModeDynamic, speedMax, 200, speedMax) / 1.5) * sign(carSpeedRight);
          carSpeedLeft = 255 * sign(carSpeedLeft);
        } else if (joystickXValue < -minValueModeDynamic) {
          carSpeedLeft = (abs(carSpeedLeft) - map(joystickXValue, -minValueModeDynamic, -speedMax, 200, speedMax) / 1.5)  * sign(carSpeedLeft);
          carSpeedRight = 255 * sign(carSpeedRight);
        }
        sendEngineCommand(carSpeedLeft,carSpeedRight);
        delay(delayMeasurement);
      }
      break;

    case 3:
      // control mode 3: gyroscope control direct
      if (pitch > minValuePitchModeGyroscope && pitch <= maxValuePitchModeGyroscope && roll > -minValueRollModeGyroscope && roll < minValueRollModeGyroscope && enabled ) {
        carSpeedLeft = map(pitch, minValuePitchModeGyroscope, maxValuePitchModeGyroscope, 150, speedMax);
        carSpeedRight = carSpeedLeft;
        stoppedCommandSent = false;
      } else if (pitch < -minValuePitchModeGyroscope && pitch >= -maxValuePitchModeGyroscope && roll > -minValueRollModeGyroscope && roll < minValueRollModeGyroscope && enabled) {
        carSpeedLeft = map(pitch, -minValuePitchModeGyroscope, -maxValuePitchModeGyroscope, -150, -speedMax);
        carSpeedRight = carSpeedLeft;
        stoppedCommandSent = false;
      } else if (roll > minValueRollModeGyroscope && roll <= maxValueRollModeGyroscope && enabled) {
        carSpeedLeft = map(roll, minValueRollModeGyroscope, maxValueRollModeGyroscope, 200, speedMax);
        carSpeedRight = -carSpeedLeft;
        stoppedCommandSent = false;
      } else if (roll < -minValueRollModeGyroscope && roll >= -maxValueRollModeGyroscope && enabled) {
        carSpeedLeft = map(roll, -minValueRollModeGyroscope, -maxValueRollModeGyroscope, -200, -speedMax);
        carSpeedRight = -carSpeedLeft;
        stoppedCommandSent = false;
      } else if (!stoppedCommandSent) {
        sendEngineCommand(0,0);
        stoppedCommandSent = true;
      }
      if (!stoppedCommandSent) {
        sendEngineCommand(carSpeedLeft,carSpeedRight);
        delay(delayMeasurement);
      }
      break;

    case 4:
      // control mode 4: gyroscope control dynamic
      if (pitch > minValuePitchModeGyroscope && enabled) {
        carSpeedLeft = map(pitch, 15, 90, 150, speedMax);
        carSpeedRight = carSpeedLeft;
        stoppedCommandSent = false;
      } else if (pitch < -minValuePitchModeGyroscope && enabled) {
        carSpeedLeft = map(pitch, -15, -90, -150, -speedMax);
        carSpeedRight = carSpeedLeft;
        stoppedCommandSent = false;
      }  else if (!stoppedCommandSent) {
        sendEngineCommand(0,0);
        stoppedCommandSent = true;
      }
      if (!stoppedCommandSent) {
        if (roll > minValueRollModeGyroscope) {
          carSpeedRight = (abs(carSpeedRight) - map(roll, minValueRollModeGyroscope, 90, 200, speedMax) / 1.5) * sign(carSpeedRight);
          carSpeedLeft = 255 * sign(carSpeedLeft);
          stoppedCommandSent = false;
        } else if (roll < -minValueRollModeGyroscope) {
          carSpeedLeft = (abs(carSpeedLeft) - map(roll, -minValueRollModeGyroscope, -90, 200, speedMax) / 1.5)  * sign(carSpeedLeft);
          carSpeedRight = 255 * sign(carSpeedRight);
          stoppedCommandSent = false;
        }
        sendEngineCommand(carSpeedLeft,carSpeedRight);
        delay(delayMeasurement);
      }
      break;
  }
  // changing mode on c-button pressed
  if (cButtonState == HIGH && cButtonState != lastCButtonState) {
    lastCButtonState = cButtonState;
    controlMode++;
    if (controlMode > 4) {
      controlMode = 1;
    }
    for (int i = 0; i < controlMode; i++) {
      digitalWrite(RED_LED, HIGH);
      delay(125);
      digitalWrite(RED_LED, LOW);
      delay(125);
    }
  } else if (cButtonState == LOW) {
    lastCButtonState = cButtonState;
  }
  // send lights command/toggle lights on z-button pressed
  if (zButtonState == HIGH && zButtonState != lastZButtonState) {
    lastZButtonState = zButtonState;
    ESPserial.print(lightsOn ? lightsOffCommand : lightsOnCommand);
    lightsOn = !lightsOn;
  } else if (zButtonState == LOW) {
    lastZButtonState = zButtonState;
  }
  // listening for udp packets
  if (ESPserial.available()) {
    while (ESPserial.available() > 0) {
      incommingPacket += ((char)ESPserial.read());
      delay(1);
    }
    incommingPacket.replace("\n", "");
    if (incommingPacket.equals(serverDisconnectedMessage)) {
      for (int i = 255; i >= 0; i--) {
        analogWrite(GREEN_LED, i);
        delay(2);
      }
    } else if (incommingPacket.equals(serverConnectedMessage)) {
      for (int i = 0; i < 255; i++) {
        analogWrite(GREEN_LED, i);
        delay(2);
      }
    }
    incommingPacket = "";
  }
}

/////////////////// sign-method
int sign(int number) {
  if (number > 0) return 1;
  else if (number < 0) return -1;
  else return 0;
}

/////////////////// sendEngineCommand-method
void sendEngineCommand(int leftEngineValue, int rightEngineValue) {
  String engineCommand = driveCommandTemplate;
  engineCommand.replace("%1", String(leftEngineValue));
  engineCommand.replace("%2", String(rightEngineValue));
  ESPserial.print(engineCommand);
}
