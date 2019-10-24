set fileName=Quorum

start java -jar .\%fileName%.jar 0
start java -jar .\%fileName%.jar 1
start java -jar .\%fileName%.jar 2
start java -jar .\%fileName%.jar 3
start java -jar .\%fileName%.jar 4
start java -jar .\%fileName%.jar 5
start java -jar .\%fileName%.jar 6
timeout 5
exit
