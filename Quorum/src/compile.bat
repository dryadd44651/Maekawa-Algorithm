taskkill /IM "java.exe" /F
set fileName=Quorum

javac -g %fileName%.java

jar cfm Quorum.jar Quorum.MF *.class
del *.class
timeout 5
exit
