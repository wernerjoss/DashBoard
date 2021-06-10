/*
DashBoard.ino

Drehzahlmessung via Interrupt an Pin 2 DIG.IN
Prinzip Periodendauermessung zw. 2 Pulsen mit gleitender Mittelung bei Freq <= 300 Hz,
darüber Impulszaehlung während der Messzeit.

Temperaturmessung mit DS18B20 via OneWire/Dallas Lib 

Ausgabe der Daten an Android App DashBoard
*/

#include <SoftwareSerial.h> 
SoftwareSerial BTserial(10, 11); // RX | TX
// Connect the HC-05 TX to Arduino pin 10 RX. 
// Connect the HC-05 RX to Arduino pin 11 TX through a voltage divider.
// 

// Include the libraries we need
#include <OneWire.h>
#include <DallasTemperature.h>

// Data wire is plugged into port 3 on the Arduino
#define ONE_WIRE_BUS 3
// Setup a oneWire instance to communicate with any OneWire devices (not just Maxim/Dallas temperature ICs)
OneWire oneWire(ONE_WIRE_BUS);
// Pass our oneWire reference to Dallas Temperature. 
DallasTemperature sensors(&oneWire);

volatile unsigned long timer, timerOld, last_timer;
volatile unsigned long zaehler = 0;
unsigned long startzeit;
unsigned long messzeit = 250000;	// muesec
int Kf = 1000000 / messzeit;
int pin = 2;  // nur 2 oder 3 möglich bei UNO
long rpm;
float f, last_f;
int n = 4; // default: 4
int i = 0;	// f. Temperaturmessung alle 5 Sekunden (modulo 20)

void setup()
{
	Serial.begin(9600);
  BTserial.begin(9600);
  pinMode(pin, INPUT);           // set pin to input
	attachInterrupt(digitalPinToInterrupt(pin), ISR_2, RISING);
  // Start up Dallas library
  sensors.begin();
}

void ISR_2()	// Interrupt Service Routine
{
	zaehler++;
	timer = micros() - timerOld;
	timerOld = micros();
	timer = (last_timer * (n-1) + timer) / n;	// gleitende Mittelung
	last_timer = timer;
}

void loop()
{
	if ((micros() - startzeit) >= messzeit) {
		rpm = 0;
		f = timer; //Datentyp 'float', wegen untenstehender Division
		detachInterrupt(digitalPinToInterrupt(pin));  // Achtung: digitalPinToInterrupt(pin) geht erst mit neuerer (>1.5) IDE, direkt pin geht mit alter nicht !
		if (f > Kf) {
			f = Kf * messzeit / f; //Aus Periodendauer Frequenz berechnen, default
			if(f >= 300) {	// Periodendauer zu kurz -> Anzahl Pulse/messzeit verwenden!
				f = zaehler * Kf;
			}
			rpm = f * 60;
			Serial.print("Frequenz: ");
			Serial.print(f, 1);
			Serial.print(" Hz\t Drehzahl: ");
			Serial.println(rpm);
      //  BTserial.print("Drehzahl: ");
      //  dtostrf(rpm, 0, 1, RemoteXY.text_1); 
		}	else	{
			Serial.println("No Signal");
		}
    BTserial.print("D:"); // Kennzeichnung als Drehzahl f. DashBoard
    BTserial.println(rpm);
    i++;
    i = i % 10; // modulo 20 gibt 5 sec Periode f. Temp.messung, 10 = 2,5 sec
    if (i == 0) {
        sensors.requestTemperatures(); // Send the command to get temperatures
        // After we got the temperatures, we can print them here.
        // We use the function ByIndex, and as an example get the temperature from the first sensor only.
        Serial.print("Temperature for the device 1 (index 0) is: ");
        Serial.println(sensors.getTempCByIndex(0));
        BTserial.print("T:");
        BTserial.println(sensors.getTempCByIndex(0));
    }
    attachInterrupt(digitalPinToInterrupt(pin), ISR_2, RISING);
		zaehler = 0; //Frequenzzähler zurücksetzen
		startzeit = micros(); //Zeitpunkt der letzten Ausgabe speichern
	}
}
