#include <SoftwareSerial.h>

int ledPin = 13; //Built-in LED
int androidCmd; //Command message from Android
int sensorFlag = 0;
int press_init = 0;
int press_final = 0;
int result = 0;

SoftwareSerial bt (2, 3);

void setup() {
  // LED Setup
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW);

  //Bluetooth Setup
  Serial.begin(9600);
  bt.begin(9600);

}

void loop() {
   // Read incoming command from Android
  if (bt.available() > 0){
    androidCmd = bt.read();
    bt.println(androidCmd); // For debug purpose
  }

  // Translate Android command into Action
  if (androidCmd == 119 && sensorFlag != 1){ // Equivalent to the character "w"
    bt.println("SENSOR is ON/n"); // Send status message to Android
    digitalWrite(ledPin, HIGH); // Turn On LED
    sensorFlag = 1;
  } else {
    if (androidCmd == 115){ // Equivalent to the character "s"
      bt.println("SENSOR is OFF/n"); // Send status message to Android
      digitalWrite(ledPin, LOW); // Turn Off LED
      sensorFlag = 0;
    } else {
      if (androidCmd == 100 && sensorFlag != 1){ // Equivalent to the character "d"
        bt.println("Checking Leakage!/n"); // Send status message to Android
        delay(200);
        scan();
        bt.println("SCANNING COMPLETE!\n");
        androidCmd = 0;
      }
    }
  }
  if(sensorFlag == 1)
    showPot();
}

void scan(){
    for(int i = 0; i < 2500; ++i){
        if(i == 1)
          press_init = analogRead(A0);          
        else if(i == 2499)
          press_final = analogRead(A0);
        
        if(i % 250 == 0){
          if(digitalRead(ledPin) == LOW)
            digitalWrite(ledPin, HIGH);
          else
            digitalWrite(ledPin, LOW);
        }
        
        showPot();
        delay(1);
    }
    Serial.println(press_init);
    Serial.println(press_final);

    result = press_init*10000 + press_final;
    Serial.println(result);
  }

void showPot(){
  bt.println(analogRead(A0));
}
