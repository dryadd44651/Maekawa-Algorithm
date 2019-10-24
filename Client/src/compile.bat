taskkill /IM "java.exe" /F
set fileName=Client
javac -g %fileName%.java
jar cfm %fileName%.jar %fileName%.MF *.class
del *.class
timeout 5
exit
