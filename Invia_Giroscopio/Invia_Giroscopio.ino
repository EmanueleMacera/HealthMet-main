#include <Wire.h>
#include <SoftwareSerial.h>

SoftwareSerial blue(2, 3); // modulo Bluetooth collegato qui
const int MPU = 0x68; // Indirizzo I2C del MPU-6050
int16_t GyX, GyY, GyZ;
bool debug = false;

void setup() {
  Wire.begin();
  Wire.beginTransmission(MPU);
  Wire.write(0x6B);  // registro PWR_MGMT_1
  Wire.write(0);     // impostato a zero (sveglia il MPU-6050)
  Wire.endTransmission(true);
  Serial.begin(9600);
  blue.begin(9600);
}

void loop() {
  Wire.beginTransmission(MPU);
  Wire.write(0x3B);  // inizio con il registro 0x3B (ACCEL_XOUT_H)
  Wire.endTransmission(false);
  Wire.requestFrom(MPU, 14, true); // richiede un totale di 14 registri
  GyX = Wire.read() << 8 | Wire.read(); // 0x43 (GYRO_XOUT_H) & 0x44 (GYRO_XOUT_L)
  GyY = Wire.read() << 8 | Wire.read(); // 0x45 (GYRO_YOUT_H) & 0x46 (GYRO_YOUT_L)
  GyZ = Wire.read() << 8 | Wire.read(); // 0x47 (GYRO_ZOUT_H) & 0x48 (GYRO_ZOUT_L)

  if (debug) {
    Serial.println("Giroscopio:");
    Serial.print("X = "); Serial.println(GyX);
    Serial.print("Y = "); Serial.println(GyY);
    Serial.print("Z = "); Serial.println(GyZ);
    Serial.println();
  }

  if (abs(GyZ) > 30000) {
    blue.println("1");
    Serial.println("Emergenza");
  }

  delay(1000);
}