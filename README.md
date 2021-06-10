# DashBoard
 Dashboard with Arduino and Android Phone via Bluetooth

This is an unfinished Project with the aim of creating a simple Data Recording Solution for Racing Motobikes.  
Data is acquired by an Arduino mounted somewhere on the Bike (e.g. below Tank or Seat), powered by an 9V block Battery.  
Currently, it can send Temerature Data from an DS18B20 via OneWire/Dallas Lib and Rev Count Data from a Hall Sensor mounted
near the Flywheel to an Android App on a Cockpit-mounted Android Phone.  
Data is sent via Bluetooth.  
The Arduino Project is in Folder arduino, teh Android App in Folder android.  
The latest Status is as follows:  
-   proved to work with live Temperature Data and artificial Rev Data from a commercial Signal Generator, everything in a Temperature / Frequency Range that can be expected under real-world Application.  
-   The Problem, however, was: too much noise on the Rev Signal under real Race Bike running Conditions, despite all Efforts with Shielding etc.  
-   Maybe there will be more Invertigation some time in the future...
