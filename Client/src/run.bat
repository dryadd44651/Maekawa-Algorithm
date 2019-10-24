set fileName=Client
start java -jar .\%fileName%.jar 0
start java -jar .\%fileName%.jar 1
start java -jar .\%fileName%.jar 2
start java -jar .\%fileName%.jar 3
start java -jar .\%fileName%.jar 4
timeout 5
exit
