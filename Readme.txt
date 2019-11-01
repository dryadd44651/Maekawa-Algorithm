start quorum0~quorum6 in dc01.utdallas.edu~dc07.utdallas.edu
start server0 in dc08.utdallas.edu
start client0~client4 in dc09.utdallas.edu~dc013.utdallas.edu

each client will enter critical section(server0) 20 times
each times client will send the request to quorums for token

without yield and fail, deadlock may happen.
without any waiting, the latency will be about 0.2
with waiting, the latency will be about 4~6

(before senting request)
reduce the first waiting time, the node will broadcast the message almost at the same time.
deadlock easily happen.

(during critical section)
reduce second waiting time, the waiting for quorum times and latency reduce significantly.
