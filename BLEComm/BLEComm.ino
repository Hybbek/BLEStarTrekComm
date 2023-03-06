#include <BLEDevice.h>
#include <BLEServer.h>
//#include <driver/adc.h>
// Working sketch for BLE Communication!

#define MESURE_PIN 27 // GIOP27 pin connected as mesure Pin

BLEServer *pServer = NULL;
BLEService *pService = NULL;
BLECharacteristic pCharacteristic("beb5483e-36e1-4688-b7f5-ea07361b26a8", BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor pDescriptor("00002902-0000-1000-8000-00805f9b34fb");

bool deviceConnected = false; //Connectionstate

bool emblemPressed = false; //Flag emblem pressed

int sensorValue; // the previous state from the input pin
float milliVoltage;     // the current reading from the input pin
int connection; // The state of the connection (on Phone/ not on phone)

//Setup callbacks onConnect and onDisconnect
class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
  };
  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
  }
};

void setup() {
// Start serial communication 
  Serial.begin(115200);

  connection = 0;

  pinMode(MESURE_PIN, INPUT); //Set MESURE_PIN as input

 /* // Setze ADC-Modus
  analogReadResolution(12); // 12-bit Auflösung
  analogSetAttenuation(ADC_11db); // Eingangsspannungsbereich: 0-3.6V 
*/

  // create the BLE Device
  BLEDevice::init("My ESP32 Device");

  // create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // create the BLE Service
  pService = pServer->createService("4fafc201-1fb5-459e-8fcc-c5c9c331914b");

  // create a BLE Characteristic
  pService->addCharacteristic(&pCharacteristic);
  pDescriptor.setValue("Charakteristik ESP");
  pCharacteristic.addDescriptor(&pDescriptor);

  // add data to the characteristic
  pCharacteristic.setValue("Hello World!");

  // start the service
  pService->start();

  // start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(pService->getUUID());
  pAdvertising->start();
  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection to notify...");

  BLECharacteristic *test = pService->getCharacteristic("beb5483e-36e1-4688-b7f5-ea07361b26a8");
  Serial.println(pDescriptor.getUUID().toString().c_str());  
}

void loop() {
 
  // Lese analogen Wert von GPIO27
  int analogValue = analogRead(MESURE_PIN); // Liest ADC-Wert (0-4095) vom Pin 27

  // Konvertiere ADC-Wert in Spannungswert
  float milliVoltage = analogValue * (3.3 / 4095.0) *1000; // ADC-Wert * (Eingangsspannungsbereich / ADC-Auflösung)

 /* // Gib den Spannungswert aus
  Serial.print("Analogwert: ");
  Serial.print(analogValue);
  Serial.print(", Spannung: ");
  Serial.print(voltage, 3); // Runde auf 3 Dezimalstellen
  Serial.println(" V");

  delay(1000); // Warte 1 Sekunde
*/
  sensorValue = analogRead(MESURE_PIN);//adc1_get_raw(ADC1_CHANNEL_0); //

  // Umrechnung des Analogwertes (welcher von 0 bis 4094 reicht) in eine Spannung von (0V bis 3.3V):
  milliVoltage = (sensorValue * 3.3) / 4095.0 *1000; //der esp gibt im vergrleich zum Arduino einen wert zwischen 0 und 4094 aus!
  
  // Ausgabe des Wertes über die serielle Schnittstelle 
  Serial.println(milliVoltage);
  Serial.println(pCharacteristic.getValue().c_str());
  
  if (milliVoltage > 25) { // den wert erst fixen, wenn der prototyp gemacht ist!!
    pCharacteristic.setValue("Emblem pressed");
    pCharacteristic.notify(true);
    Serial.println("pressed");
    delay(1000); // delay, damit nicht mehrmals die notification gesendet wird bei der berührung!
    pCharacteristic.setValue("Emblem not pressed");
    pCharacteristic.notify(true);
  }
  
}
