void setup()
{
  Serial.begin(9600);
}

void loop()
{
  int sensor = analogRead(A0);
  Serial.println(sensor);
  delay(1000);
}
